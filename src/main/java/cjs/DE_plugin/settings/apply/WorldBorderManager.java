package cjs.DE_plugin.settings.apply;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class WorldBorderManager {

    private final DE_plugin plugin;
    private final SettingsManager sm;
    private final File borderDataFile;
    private FileConfiguration borderDataConfig;

    private Location overworldCenter;

    public WorldBorderManager(DE_plugin plugin) {
        this.plugin = plugin;
        this.sm = plugin.getSettingsManager();
        this.borderDataFile = new File(plugin.getDataFolder(), "border_data.yml");
        loadBorderData();
    }

    public void loadBorderData() {
        if (!borderDataFile.exists()) {
            World overworld = Bukkit.getWorlds().get(0);
            this.overworldCenter = new Location(overworld, 0.5, 0, 0.5);
            saveBorderData();
        }
        this.borderDataConfig = YamlConfiguration.loadConfiguration(borderDataFile);
        World overworld = Bukkit.getWorld("world");
        if (overworld != null) {
            double x = borderDataConfig.getDouble("overworld-center-x", 0.5);
            double z = borderDataConfig.getDouble("overworld-center-z", 0.5);
            this.overworldCenter = new Location(overworld, x, 0, z);
        } else {
            // 'world' 월드를 찾을 수 없을 경우의 대체 처리
            this.overworldCenter = new Location(Bukkit.getWorlds().get(0), 0.5, 0, 0.5);
        }
    }

    public void saveBorderData() {
        if (this.borderDataConfig == null) {
            this.borderDataConfig = new YamlConfiguration();
        }
        if (overworldCenter != null) {
            borderDataConfig.set("overworld-center-x", overworldCenter.getX());
            borderDataConfig.set("overworld-center-z", overworldCenter.getZ());
        }
        try {
            borderDataConfig.save(borderDataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "border_data.yml 파일을 저장할 수 없습니다.", e);
        }
    }

    public Location getOverworldCenter() {
        return this.overworldCenter.clone();
    }

    public void setCenter(Location newCenter, CommandSender requester) {
        World overworld = Bukkit.getWorld("world");
        if (overworld == null) {
            requester.sendMessage("§c오버월드를 찾을 수 없습니다.");
            return;
        }

        // 1. 메모리에 새로운 중앙 위치를 설정합니다.
        this.overworldCenter = new Location(overworld, newCenter.getX(), 0, newCenter.getZ());

        // 2. 새로운 중앙 좌표를 파일에 저장합니다.
        saveBorderData();

        // 3. 모든 월드에 변경된 보더 설정을 적용합니다.
        applyAllWorldBorders();

        // 4. 사용자에게 상세한 피드백을 제공합니다.
        requester.sendMessage("§a월드 보더 중앙을 성공적으로 설정하고 적용했습니다.");
        requester.sendMessage(String.format("§7- 오버월드 중앙: X: %.1f, Z: %.1f", this.overworldCenter.getX(), this.overworldCenter.getZ()));

        World netherWorld = Bukkit.getWorld(overworld.getName() + "_nether");
        if (netherWorld != null) {
            double netherScale = sm.getDouble(SettingsManager.WORLDBORDER_NETHER_SCALE);
            if (netherScale > 0) {
                double netherX = this.overworldCenter.getX() / netherScale;
                double netherZ = this.overworldCenter.getZ() / netherScale;
                requester.sendMessage(String.format("§7- 지옥 중앙 (배율 %.1f 적용): X: %.1f, Z: %.1f", netherScale, netherX, netherZ));
            }
        }
    }

    public void applyAllWorldBorders() {
        // 오버월드 보더 설정
        World overworld = Bukkit.getWorld("world");
        if (overworld != null) {
            WorldBorder border = overworld.getWorldBorder();
            border.setCenter(this.overworldCenter);
            border.setSize(sm.getInt(SettingsManager.WORLDBORDER_OVERWORLD_SIZE));
            border.setDamageAmount(1.0);
            border.setDamageBuffer(5.0);
            border.setWarningDistance(16);
            border.setWarningTime(15);
        }

        // 네더 보더 설정
        World nether = Bukkit.getWorld("world_nether");
        if (nether != null) {
            WorldBorder border = nether.getWorldBorder();
            double scale = sm.getDouble(SettingsManager.WORLDBORDER_NETHER_SCALE);
            if (scale <= 0) scale = 8.0; // 기본값/대체 배율
            double overworldSize = sm.getInt(SettingsManager.WORLDBORDER_OVERWORLD_SIZE);

            border.setCenter(overworldCenter.getX() / scale, overworldCenter.getZ() / scale);
            border.setSize(overworldSize / scale);
            border.setDamageAmount(1.0);
            border.setDamageBuffer(5.0);
            border.setWarningDistance(16);
            border.setWarningTime(15);
        }

        // 엔더 보더 설정
        World end = Bukkit.getWorld("world_the_end");
        if (end != null) {
            WorldBorder border = end.getWorldBorder();
            if (sm.getBoolean(SettingsManager.WORLDBORDER_END_ENABLED)) {
                border.setCenter(0, 0);
                border.setSize(sm.getInt(SettingsManager.WORLDBORDER_OVERWORLD_SIZE));
            } else {
                // 비활성화 시, 사실상 보더가 없는 것과 같도록 매우 크게 설정
                border.setSize(60000000);
            }
        }
    }
}