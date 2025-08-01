package cjs.DE_plugin.dragon_egg.egg_storage_prevention;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 드래곤 알을 버리거나 다른 보관함에 넣는 것을 방지하는 리스너
 */
public class EggStoragePreventionListener implements Listener {

    private void sendCantStoreMessage(Player player) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c드래곤 알은 보관함에 넣을 수 없습니다."));
    }

    private void sendCantDropMessage(Player player) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c드래곤 알은 버릴 수 없습니다."));
    }

    /**
     * 플레이어가 드래곤 알을 버리는 것을 막습니다.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropEgg(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
            sendCantDropMessage(event.getPlayer());
        }
    }

    /**
     * 플레이어가 드래곤 알을 드래그하여 보관함에 넣는 것을 막습니다.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        // 드래그하는 아이템이 드래곤 알이 아니면 무시
        if (event.getOldCursor() == null || event.getOldCursor().getType() != Material.DRAGON_EGG) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        // 플레이어 인벤토리만 열려있으면(보관함 GUI가 아니면) 무시
        if (topInventory.getType() == InventoryType.PLAYER || topInventory.getType() == InventoryType.CRAFTING) {
            return;
        }

        // 드래그된 슬롯 중 하나라도 상단 인벤토리(보관함)에 포함되면 취소
        int topInventorySize = topInventory.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topInventorySize) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    sendCantStoreMessage((Player) event.getWhoClicked());
                }
                return;
            }
        }
    }

    /**
     * 플레이어가 클릭을 통해 드래곤 알을 보관함에 넣는 모든 시도를 막습니다.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // 상단 인벤토리(보관함)가 없으면 무시
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getType() == InventoryType.PLAYER || topInventory.getType() == InventoryType.CRAFTING) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        Inventory clickedInventory = event.getClickedInventory();

        // 1. 커서에 알을 들고 보관함을 클릭하는 경우
        if (cursorItem != null && cursorItem.getType() == Material.DRAGON_EGG) {
            if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                event.setCancelled(true);
                sendCantStoreMessage(player);
                return;
            }
        }

        // 2. Shift-Click으로 알을 보관함으로 옮기려는 경우
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (currentItem != null && currentItem.getType() == Material.DRAGON_EGG) {
                // 클릭된 인벤토리가 플레이어 인벤토리라면, 알은 상단 인벤토리로 이동하려고 함
                if (clickedInventory != null && clickedInventory.getType() == InventoryType.PLAYER) {
                    event.setCancelled(true);
                    sendCantStoreMessage(player);
                    return;
                }
            }
        }

        // 3. 숫자 키로 핫바의 아이템과 보관함의 아이템을 바꾸려는데, 핫바에 알이 있는 경우
        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (hotbarItem != null && hotbarItem.getType() == Material.DRAGON_EGG) {
                // 클릭된 슬롯이 보관함 안이라면 취소
                if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                    event.setCancelled(true);
                    sendCantStoreMessage(player);
                }
            }
        }
    }
}