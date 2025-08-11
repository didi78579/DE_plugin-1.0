package cjs.DE_plugin.settings.apply;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
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
            // To completely override the default effects, we cancel the vanilla event.
            event.setCancelled(true);

            Player player = event.getPlayer();

            // Manually consume one item from the correct hand if the player is not in creative mode.
            // event.getItem() returns a copy, so we must modify the item in the player's inventory directly.
            if (player.getGameMode() != GameMode.CREATIVE) {
                ItemStack mainHandItem = player.getInventory().getItemInMainHand();
                ItemStack offHandItem = player.getInventory().getItemInOffHand();

                // Check which hand holds the golden apple and reduce its amount.
                if (mainHandItem.getType() == Material.GOLDEN_APPLE) {
                    mainHandItem.setAmount(mainHandItem.getAmount() - 1);
                } else if (offHandItem.getType() == Material.GOLDEN_APPLE) {
                    offHandItem.setAmount(offHandItem.getAmount() - 1);
                }
            }

            // Re-apply the standard Absorption effect that a golden apple provides.
            // Absorption I for 2 minutes (2400 ticks).
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0, false, true), true);

            // Apply our custom Regeneration effect from the settings, instead of the vanilla one.
            int durationSeconds = sm.getInt(SettingsManager.GOLDEN_APPLE_REGEN_DURATION_SECONDS);
            if (durationSeconds > 0) {
                int durationTicks = durationSeconds * 20;
                // Regeneration II (amplifier 1) for the custom duration.
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1, false, true), true);
            }
        }
    }
}