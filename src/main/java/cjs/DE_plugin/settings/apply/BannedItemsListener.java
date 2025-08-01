package cjs.DE_plugin.settings.apply;

import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BannedItemsListener implements Listener {

    private final SettingsManager sm;

    public BannedItemsListener(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    private void sendBannedMessage(Player player, Material material) {
        player.sendMessage("§c" + material.name() + " 아이템은 현재 금지되어 있습니다.");
    }

    // 아이템 사용/설치 방지
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        Material itemType = item.getType();

        if ((itemType == Material.ENDER_PEARL && sm.getBoolean(SettingsManager.ENDER_PEARL_BANNED)) ||
                (itemType == Material.ENDER_CHEST && sm.getBoolean(SettingsManager.ENDER_CHEST_BANNED)) ||
                (itemType == Material.SHIELD && sm.getBoolean(SettingsManager.SHIELD_BANNED))) {
            event.setCancelled(true);
            sendBannedMessage(player, itemType);
        }
    }

    // 아이템 제작 방지
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (event.getRecipe() == null) return;

        Material resultType = event.getRecipe().getResult().getType();
        if ((resultType == Material.ENDER_CHEST && sm.getBoolean(SettingsManager.ENDER_CHEST_BANNED)) ||
                (resultType == Material.SHIELD && sm.getBoolean(SettingsManager.SHIELD_BANNED))) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                sendBannedMessage((Player) event.getWhoClicked(), resultType);
            }
        }
    }

    // 불사의 토템 발동 방지
    @EventHandler
    public void onPlayerResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        if (sm.getBoolean(SettingsManager.TOTEM_BANNED)) {
            event.setCancelled(true);
        }
    }
}