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
import java.util.logging.Level;

public class FootprintManager {

    private final DE_plugin plugin;
    private final Set<Footprint> footprints = new HashSet<>();
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

    public void saveFootprints() {
        footprintConfig = new YamlConfiguration();
        int i = 0;
        for (Footprint fp : footprints) {
            String path = "footprints." + i;
            footprintConfig.set(path + ".front", fp.getFrontEntityId().toString());
            footprintConfig.set(path + ".back", fp.getBackEntityId().toString());
            footprintConfig.set(path + ".owner", fp.getOwnerId().toString());
            footprintConfig.set(path + ".location", fp.getLocation());
            footprintConfig.set(path + ".time", fp.getCreationTime());
            i++;
        }
        try {
            footprintConfig.save(footprintFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save footprints to " + footprintFile, e);
        }
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
    }

    public void removeFootprintEntity(Footprint footprint) {
        if (footprint == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
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
        new HashSet<>(footprints).forEach(this::removeFootprintEntity);
        footprints.clear();
        trackedPlayers.clear();
        lastLocations.clear();
        if (footprintFile.exists()) {
            footprintFile.delete();
        }
    }
}