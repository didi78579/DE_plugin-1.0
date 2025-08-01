package cjs.DE_plugin.command;

import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainTabCompleter implements TabCompleter {

    private final SettingsManager sm;
    private final List<String> mainSubCommands = Arrays.asList("settings", "set", "start", "stop", "status", "border");
    private final List<String> settingsCategories = Arrays.asList("general", "border", "enchant");
    private final List<String> borderSubCommands = Arrays.asList("setcenter");

    public MainTabCompleter(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], mainSubCommands, completions);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("settings")) {
                StringUtil.copyPartialMatches(args[1], settingsCategories, completions);
            } else if (args[0].equalsIgnoreCase("set")) {
                // 모든 설정 키 목록을 자동완성으로 제공
                StringUtil.copyPartialMatches(args[1], sm.getConfig().getKeys(true), completions);
            } else if (args[0].equalsIgnoreCase("border")) {
                StringUtil.copyPartialMatches(args[1], borderSubCommands, completions);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
                String key = args[1];
                Object value = sm.get(key);
                if (value instanceof Boolean) {
                    StringUtil.copyPartialMatches(args[2], Arrays.asList("true", "false"), completions);
                }
            }
        }

        return completions.stream().sorted().collect(Collectors.toList());
    }
}