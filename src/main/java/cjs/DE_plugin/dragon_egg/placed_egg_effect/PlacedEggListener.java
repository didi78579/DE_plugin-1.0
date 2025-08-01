package cjs.DE_plugin.dragon_egg.placed_egg_effect;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlacedEggListener implements Listener {

    private final PlacedEggManager placedEggManager;

    public PlacedEggListener(PlacedEggManager placedEggManager) {
        this.placedEggManager = placedEggManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEggPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            placedEggManager.addEggLocation(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEggBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            placedEggManager.removeEggLocation(event.getBlock().getLocation());
        }
    }
}