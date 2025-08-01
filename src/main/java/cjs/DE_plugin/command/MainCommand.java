package cjs.DE_plugin.command;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import cjs.DE_plugin.settings.apply.WorldBorderManager;
import cjs.DE_plugin.settings.editor.SettingsEditorUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class MainCommand implements CommandExecutor {

    private final DE_plugin plugin;
    private final SettingsManager sm;
    private final SettingsEditorUI settingsEditorUI;

    // [추가] 설정 키를 카테고리별로 분류
    private static final List<String> GENERAL_SETTINGS_KEYS = Arrays.asList(
            SettingsManager.VILLAGER_TRADE_LIMIT,
            SettingsManager.POTION_LIMIT,
            SettingsManager.RESPAWNED_DRAGON_EXP_LEVEL,
            SettingsManager.EGG_FOOTPRINT_DURATION_DAYS,
            SettingsManager.GAME_PLAY_TIME_DAYS,
            SettingsManager.EXPLOSION_DAMAGE_MULTIPLIER,
            SettingsManager.PLAYER_EXP_DROP_MULTIPLIER,
            SettingsManager.ENDER_PEARL_BANNED,
            SettingsManager.ENDER_CHEST_BANNED,
            SettingsManager.SHIELD_BANNED,
            SettingsManager.TOTEM_BANNED,
            SettingsManager.HIDE_ADVANCEMENTS,
            SettingsManager.HIDE_COORDINATES,
            SettingsManager.HIDE_FOOTPRINTS_AT_NIGHT,
            SettingsManager.PREVENT_PORTAL_WITH_EGG,
            SettingsManager.CHAT_BANNED,
            SettingsManager.KILL_LOG_DISABLED
    );

    private static final List<String> BORDER_SETTINGS_KEYS = Arrays.asList(
            SettingsManager.WORLDBORDER_OVERWORLD_SIZE,
            SettingsManager.WORLDBORDER_NETHER_SCALE,
            SettingsManager.WORLDBORDER_END_ENABLED
    );

    private static final List<String> ENCHANT_SETTINGS_KEYS = Arrays.asList(
            SettingsManager.ENCHANT_PROTECTION_MAX_LEVEL,
            SettingsManager.ENCHANT_SHARPNESS_MAX_LEVEL
    );


    public MainCommand(DE_plugin plugin) {
        this.plugin = plugin;
        this.sm = plugin.getSettingsManager();
        this.settingsEditorUI = new SettingsEditorUI(sm);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§e[DE Plugin] §f/de help 로 도움말을 확인하세요.");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "settings":
                handleSettingsCommand(player, args);
                break;
            case "border":
                handleBorderCommand(player, args);
                break;
            case "set":
                handleSetCommand(player, args);
                break;
            case "start":
                if (player.hasPermission("de.admin.timer")) {
                    plugin.getGameTimeManager().startGame();
                } else {
                    player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
                }
                break;
            case "stop":
                if (player.hasPermission("de.admin.timer")) {
                    plugin.getGameTimeManager().stopGame();
                } else {
                    player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
                }
                break;
            case "status":
                player.sendMessage(plugin.getGameTimeManager().getStatus());
                break;
            default:
                player.sendMessage("§c알 수 없는 명령어입니다. /de help 로 도움말을 확인하세요.");
                break;
        }

        return true;
    }

    private void handleSettingsCommand(Player player, String[] args) {
        if (!player.hasPermission("de.admin.settings")) {
            player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length == 1) {
            // /de settings -> 메인 메뉴 열기
            settingsEditorUI.open(player);
        } else if (args.length == 2) {
            String category = args[1].toLowerCase();
            switch (category) {
                case "general":
                    settingsEditorUI.openGeneralSettings(player);
                    break;
                case "border":
                    settingsEditorUI.openBorderSettings(player);
                    break;
                case "enchant":
                    settingsEditorUI.openEnchantSettings(player);
                    break;
                default:
                    player.sendMessage("§c알 수 없는 설정 카테고리입니다. [general, border, enchant]");
                    settingsEditorUI.open(player); // 메뉴 다시 보여주기
                    break;
            }
        } else {
            player.sendMessage("§c사용법: /de settings [general|border|enchant]");
        }
    }

    private void handleBorderCommand(Player player, String[] args) {
        if (!player.hasPermission("de.admin.border")) {
            player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c사용법: /de border <subcommand>");
            player.sendMessage("§7  - setcenter: 현재 위치를 월드 보더의 새로운 중앙으로 설정합니다.");
            return;
        }

        String borderSubCommand = args[1].toLowerCase();
        if (borderSubCommand.equals("setcenter")) {
            WorldBorderManager wbm = plugin.getWorldBorderManager();
            if (wbm != null) {
                wbm.setCenter(player.getLocation(), player);
            } else {
                player.sendMessage("§cWorldBorderManager를 찾을 수 없습니다.");
            }
        } else {
            player.sendMessage("§c알 수 없는 보더 명령어입니다.");
        }
    }

    private void handleSetCommand(Player player, String[] args) {
        if (!player.hasPermission("de.admin.settings")) {
            player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§c사용법: /de set <설정키> <값>");
            return;
        }

        String key = args[1];
        String valueStr = args[2];
        Object currentValue = sm.get(key);

        if (currentValue == null) {
            player.sendMessage("§c'" + key + "'는 존재하지 않는 설정입니다.");
            return;
        }

        try {
            if (currentValue instanceof Integer) {
                sm.set(key, Integer.parseInt(valueStr));
            } else if (currentValue instanceof Double) {
                sm.set(key, Double.parseDouble(valueStr));
            } else if (currentValue instanceof Boolean) {
                sm.set(key, Boolean.parseBoolean(valueStr));
            } else {
                sm.set(key, valueStr);
            }
            player.sendMessage("§a설정 '" + key + "'을(를) '" + valueStr + "'(으)로 변경했습니다.");

            // [핵심 변경] 설정 변경 후, 해당 카테고리의 설정창을 다시 열어줍니다.
            if (GENERAL_SETTINGS_KEYS.contains(key)) {
                settingsEditorUI.openGeneralSettings(player);
            } else if (BORDER_SETTINGS_KEYS.contains(key)) {
                settingsEditorUI.openBorderSettings(player);
            } else if (ENCHANT_SETTINGS_KEYS.contains(key)) {
                settingsEditorUI.openEnchantSettings(player);
            } else {
                // 분류되지 않은 키의 경우, 메인 메뉴로 돌아갑니다.
                settingsEditorUI.open(player);
            }

        } catch (NumberFormatException e) {
            player.sendMessage("§c잘못된 숫자 형식입니다.");
        }
    }
}