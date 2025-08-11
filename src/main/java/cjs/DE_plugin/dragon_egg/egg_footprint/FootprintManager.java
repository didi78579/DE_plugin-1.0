package cjs.DE_plugin.dragon_egg.egg_footprint;

import cjs.DE_plugin.DE_plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

public class FootprintManager {

    private final DE_plugin plugin;
    // [수정] 동시성 문제를 예방하기 위해 스레드에 안전한 Set으로 변경합니다.
    private final Set<Footprint> footprints = new CopyOnWriteArraySet<>();
    private final Set<UUID> trackedPlayers = new HashSet<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final File footprintFile;
    private FileConfiguration footprintConfig;

    public FootprintManager(DE_plugin plugin) {
        this.plugin = plugin;
        this.footprintFile = new File(plugin.getDataFolder(), "footprints.yml");
        loadFootprints();
    }

    public void loadFootprints() {
        if (!footprintFile.exists()) {
            return;
        }
        footprintConfig = YamlConfiguration.loadConfiguration(footprintFile);
        ConfigurationSection section = footprintConfig.getConfigurationSection("footprints");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID frontId = UUID.fromString(section.getString(key + ".front"));
                UUID backId = UUID.fromString(section.getString(key + ".back"));
                UUID ownerId = UUID.fromString(section.getString(key + ".owner"));
                Location location = section.getLocation(key + ".location");
                long creationTime = section.getLong(key + ".time");

                if (location != null) {
                    footprints.add(new Footprint(frontId, backId, ownerId, location, creationTime));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load footprint " + key + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info(footprints.size() + " footprints loaded.");
    }

    /**
     * [수정] 현재 메모리에 있는 발자국 목록을 파일에 즉시 덮어쓰는 내부 메소드입니다.
     * 파일 I/O를 유발하므로 잦은 호출은 성능 저하의 원인이 될 수 있습니다.
     */
    private synchronized void rewriteFootprintFile() {
        FileConfiguration newConfig = new YamlConfiguration();
        int i = 0;
        for (Footprint fp : footprints) {
            if (fp.getLocation() == null) {
                continue;
            }
            String path = "footprints." + i;
            newConfig.set(path + ".front", fp.getFrontEntityId().toString());
            newConfig.set(path + ".back", fp.getBackEntityId().toString());
            newConfig.set(path + ".owner", fp.getOwnerId().toString());
            newConfig.set(path + ".location", fp.getLocation());
            newConfig.set(path + ".time", fp.getCreationTime());
            i++;
        }
        try {
            newConfig.save(footprintFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save footprints to " + footprintFile, e);
        }
    }

    public void saveFootprints() {
        // 이제 내부 헬퍼 메소드를 호출합니다.
        rewriteFootprintFile();
    }

    public boolean isTracking(UUID uuid) {
        return trackedPlayers.contains(uuid);
    }

    public void startTracking(Player player) {
        trackedPlayers.add(player.getUniqueId());
        lastLocations.put(player.getUniqueId(), player.getLocation());
    }

    public void stopTracking(UUID uuid) {
        trackedPlayers.remove(uuid);
        lastLocations.remove(uuid);
    }

    public Set<UUID> getTrackedPlayerIds() {
        return Collections.unmodifiableSet(trackedPlayers);
    }

    public Set<Footprint> getAllFootprints() {
        return footprints;
    }

    public void addFootprint(Footprint footprint) {
        footprints.add(footprint);
        rewriteFootprintFile();
    }

    public void removeFootprint(Footprint footprint) {
        if (footprints.remove(footprint)) {
            removeFootprintEntity(footprint);
            rewriteFootprintFile();
        }
    }

    public void removeFootprintEntity(Footprint footprint) {
        if (footprint == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {

                Location loc = footprint.getLocation();
                if (loc == null || loc.getWorld() == null) return;
                
                if (!loc.getChunk().isLoaded()) {
                    loc.getChunk().load();
                }

                Entity front = Bukkit.getEntity(footprint.getFrontEntityId());
                if (front != null) front.remove();
                Entity back = Bukkit.getEntity(footprint.getBackEntityId());
                if (back != null) back.remove();
            }
        }.runTask(plugin);
    }

    public Location getLastLocation(UUID uuid) {
        return lastLocations.get(uuid);
    }

    public void updateLastLocation(UUID uuid, Location location) {
        lastLocations.put(uuid, location);
    }

    public void clearAllFootprints() {
        // [수정] 게임 중지/종료 시 호출되며, 모든 발자국 엔티티와 데이터를 제거합니다.
        new HashSet<>(footprints).forEach(this::removeFootprintEntity);
        footprints.clear();
        trackedPlayers.clear();
        lastLocations.clear();
        rewriteFootprintFile(); // 파일도 비웁니다.
    }
}