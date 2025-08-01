package cjs.DE_plugin.dragon_egg.placed_egg_effect;

import cjs.DE_plugin.DE_plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class PlacedEggManager {

    private final DE_plugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Set<Location> eggLocations = new HashSet<>();
    private BukkitTask soundTask;

    public PlacedEggManager(DE_plugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "placed-eggs.yml");
        load();
        startSoundTask();
    }

    public void load() {
        if (!dataFile.exists()) {
            return;
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection eggsSection = dataConfig.getConfigurationSection("eggs");
        if (eggsSection != null) {
            for (String key : eggsSection.getKeys(false)) {
                String worldName = eggsSection.getString(key + ".world");
                if (worldName != null && Bukkit.getWorld(worldName) != null) {
                    double x = eggsSection.getDouble(key + ".x");
                    double y = eggsSection.getDouble(key + ".y");
                    double z = eggsSection.getDouble(key + ".z");
                    eggLocations.add(new Location(Bukkit.getWorld(worldName), x, y, z));
                }
            }
        }
    }

    public void save() {
        dataConfig = new YamlConfiguration();
        Set<Location> locationsToSave = new HashSet<>(eggLocations);
        int i = 0;
        for (Location loc : locationsToSave) {
            if (loc.getWorld() == null) continue;
            String path = "eggs." + i;
            dataConfig.set(path + ".world", loc.getWorld().getName());
            dataConfig.set(path + ".x", loc.getX());
            dataConfig.set(path + ".y", loc.getY());
            dataConfig.set(path + ".z", loc.getZ());
            i++;
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "placed-eggs.yml 파일을 저장할 수 없습니다.", e);
        }
    }

    public void addEggLocation(Location location) {
        // 블록 좌표의 정중앙이 아닌, 블록 자체의 정수 좌표를 저장하여 일관성 유지
        eggLocations.add(location.getBlock().getLocation());
    }

    public void removeEggLocation(Location location) {
        eggLocations.remove(location.getBlock().getLocation());
    }

    private void startSoundTask() {
        this.soundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            new HashSet<>(eggLocations).forEach(loc -> {
                // 알이 있던 월드가 비활성화되었거나, 해당 위치의 블록이 더 이상 드래곤 알이 아니면 목록에서 제거
                if (loc.getWorld() == null || !loc.isWorldLoaded() || loc.getBlock().getType() != org.bukkit.Material.DRAGON_EGG) {
                    eggLocations.remove(loc);
                    return;
                }

                // 알 주변 16블록 반경 내의 플레이어에게 소리 재생
                loc.getWorld().getPlayers().stream()
                        .filter(p -> p.getLocation().distanceSquared(loc) < 256) // 16*16
                        .forEach(p -> p.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, SoundCategory.BLOCKS, 0.3f, 0.5f));
            });
        }, 0L, 40L); // 2초(40틱)마다 소리 재생
    }

    public void cancelTask() {
        if (soundTask != null && !soundTask.isCancelled()) {
            soundTask.cancel();
        }
    }
}