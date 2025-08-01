package cjs.DE_plugin.settings.apply;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GoldenAppleListener implements Listener {

    private final DE_plugin plugin;
    private final SettingsManager sm;

    public GoldenAppleListener(DE_plugin plugin) {
        this.plugin = plugin;
        this.sm = plugin.getSettingsManager();
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.GOLDEN_APPLE) {
            Player player = event.getPlayer();
            int durationSeconds = sm.getInt(SettingsManager.GOLDEN_APPLE_REGEN_DURATION_SECONDS);
            int durationTicks = durationSeconds * 20;

            plugin.getServer().getScheduler().runTask(plugin, () -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1, false, true), true));
        }
    }
}