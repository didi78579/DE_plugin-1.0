package cjs.DE_plugin.dragon_egg.egg_footprint;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.Chunk;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class FootprintManager {

    private final DE_plugin plugin;
    private final SettingsManager sm;
    // [최적화] 발자국을 월드와 청크별로 관리하여 조회를 빠르게 합니다.
    private final Map<UUID, Map<Long, Set<Footprint>>> footprintsByChunk = new ConcurrentHashMap<>();
    private final Set<UUID> trackedPlayers = new HashSet<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final File footprintFile;

    private boolean isDirty = false;
    private BukkitTask saveTask;

    public FootprintManager(DE_plugin plugin) {
        this.plugin = plugin;
        this.sm = plugin.getSettingsManager();
        this.footprintFile = new File(plugin.getDataFolder(), "footprints.yml");
        loadFootprints();
        startAutoSaveTask(); // [신규] 자동 저장 시작
    }

    private long getChunkKey(Chunk chunk) {
        return (long) chunk.getX() << 32 | (chunk.getZ() & 0xFFFFFFFFL);
    }

    public void loadFootprints() {
        if (!footprintFile.exists()) {
            return;
        }
        FileConfiguration footprintConfig = YamlConfiguration.loadConfiguration(footprintFile);
        ConfigurationSection section = footprintConfig.getConfigurationSection("footprints");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                Location location = section.getLocation(key + ".location");
                if (location == null || location.getWorld() == null) continue;

                UUID frontId = UUID.fromString(section.getString(key + ".front"));
                UUID backId = UUID.fromString(section.getString(key + ".back"));
                UUID ownerId = UUID.fromString(section.getString(key + ".owner"));
                long creationTime = section.getLong(key + ".time");

                Footprint footprint = new Footprint(frontId, backId, ownerId, location, creationTime);
                addFootprintToMap(footprint);

            } catch (Exception e) {
                plugin.getLogger().warning("Could not load footprint " + key + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info(getAllFootprints().size() + " footprints loaded.");
    }

    /**
     * [신규] 주기적으로 변경 사항을 파일에 저장하는 태스크를 시작합니다.
     */
    private void startAutoSaveTask() {
        // 5분(6000틱)마다 저장
        long saveInterval = 20L * 60 * 5;
        this.saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 변경 사항이 있을 때만 저장합니다.
                if (isDirty) {
                    rewriteFootprintFile();
                }
            }
        }.runTaskTimerAsynchronously(plugin, saveInterval, saveInterval);
    }

    /**
     * [신규] 플러그인 비활성화 시 태스크를 중지하고 최종 저장을 수행합니다.
     */
    public void shutdown() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        // 마지막으로 변경사항 저장
        if (isDirty) {
            saveFootprints();
        }
    }

    /**
     * [수정] 현재 메모리에 있는 발자국 목록을 파일에 즉시 덮어쓰는 내부 메소드입니다. 비동기적으로 호출될 수 있습니다.
     */
    private synchronized void rewriteFootprintFile() {
        if (!isDirty) {
            return; // 변경사항 없으면 스킵
        }
        FileConfiguration newConfig = new YamlConfiguration();
        int i = 0;
        for (Footprint fp : getAllFootprints()) {
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
            isDirty = false; // [수정] 저장이 완료되면 dirty 플래그를 리셋합니다.
            plugin.getLogger().info("Successfully saved " + i + " footprints.");
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
        return footprintsByChunk.values().stream()
                .flatMap(worldMap -> worldMap.values().stream())
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private void addFootprintToMap(Footprint footprint) {
        Location loc = footprint.getLocation();
        if (loc == null || loc.getWorld() == null) return;

        UUID worldId = loc.getWorld().getUID();
        long chunkKey = getChunkKey(loc.getChunk());

        footprintsByChunk
                .computeIfAbsent(worldId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> new CopyOnWriteArraySet<>())
                .add(footprint);
    }

    public void addFootprint(Footprint footprint) {
        addFootprintToMap(footprint);
        isDirty = true;
    }

    private void removeFootprintFromMap(Footprint footprint) {
        Location loc = footprint.getLocation();
        if (loc == null || loc.getWorld() == null) return;

        UUID worldId = loc.getWorld().getUID();
        long chunkKey = getChunkKey(loc.getChunk());

        Map<Long, Set<Footprint>> worldMap = footprintsByChunk.get(worldId);
        if (worldMap != null) {
            Set<Footprint> chunkSet = worldMap.get(chunkKey);
            if (chunkSet != null) {
                chunkSet.remove(footprint);
                if (chunkSet.isEmpty()) {
                    worldMap.remove(chunkKey);
                }
            }
            if (worldMap.isEmpty()) {
                footprintsByChunk.remove(worldId);
            }
        }
    }

    public void removeFootprint(Footprint footprint) {
        removeFootprintFromMap(footprint);
        removeFootprintEntity(footprint);
        isDirty = true;
    }

    public void removeFootprintEntity(Footprint footprint) {
        if (footprint == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {

                Location loc = footprint.getLocation();
                if (loc == null || loc.getWorld() == null) return;
                
                if (!loc.getChunk().isLoaded()) {
                    // [최적화] 청크가 로드되어 있지 않으면 제거하지 않습니다. ChunkLoadEvent에서 처리됩니다.
                    return;
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
        getAllFootprints().forEach(this::removeFootprintEntity);
        footprintsByChunk.clear();
        trackedPlayers.clear();
        lastLocations.clear();
        isDirty = true; // 파일도 비워야 하므로 dirty 플래그 설정
        rewriteFootprintFile(); // 게임 종료 시에는 즉시 파일에 반영합니다.
    }

    public Set<Footprint> getFootprintsInChunk(Chunk chunk) {
        Map<Long, Set<Footprint>> worldFootprints = footprintsByChunk.get(chunk.getWorld().getUID());
        if (worldFootprints == null) {
            return Collections.emptySet();
        }
        return worldFootprints.getOrDefault(getChunkKey(chunk), Collections.emptySet());
    }

    public void checkAndRemoveExpiredFootprintsInChunk(Chunk chunk) {
        long durationDays = sm.getInt(SettingsManager.EGG_FOOTPRINT_DURATION_DAYS);
        long durationMs = durationDays * 20L * 60L * 1000L;

        Set<Footprint> toCheck = getFootprintsInChunk(chunk);
        if (toCheck.isEmpty()) return;

        List<Footprint> toRemove = toCheck.stream()
                .filter(fp -> System.currentTimeMillis() - fp.getCreationTime() > durationMs)
                .collect(Collectors.toList());

        if (!toRemove.isEmpty()) {
            toRemove.forEach(this::removeFootprint);
        }
    }
}