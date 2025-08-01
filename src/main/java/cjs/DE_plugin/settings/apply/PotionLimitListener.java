package cjs.DE_plugin.settings.apply;

import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PotionLimitListener implements Listener {

    private final SettingsManager sm;

    public PotionLimitListener(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    /**
     * 아이템이 일반, 투척, 잔류형 포션인지 확인합니다.
     * @param item 확인할 아이템
     * @return 포션이면 true
     */
    private boolean isPotion(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION;
    }

    /**
     * 플레이어 인벤토리의 포션 개수를 셉니다.
     * @param inventory 확인할 플레이어의 인벤토리
     * @return 포션의 총 개수
     */
    private int countPotions(PlayerInventory inventory) {
        int amount = 0;
        // 메인 인벤토리 + 핫바
        for (ItemStack item : inventory.getStorageContents()) {
            if (isPotion(item)) {
                amount++;
            }
        }
        // 왼손(Off-hand) 슬롯 포함
        if (isPotion(inventory.getItemInOffHand())) {
            amount++;
        }
        return amount;
    }

    /**
     * 플레이어에게 알림 메시지와 효과음을 보냅니다.
     * @param player 알림을 받을 플레이어
     */
    private void notify(Player player) {
        player.sendMessage("§c포션은 " + sm.getInt(SettingsManager.POTION_LIMIT) + "개까지만 소지할 수 있습니다.");
        // 주민이 거절하는 소리를 사용하여 명확한 피드백을 제공합니다.
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    /**
     * 인벤토리 클릭으로 포션을 획득하는 모든 경우를 처리합니다. (상자 등 외부 GUI 포함)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.isCancelled()) return;

        Player player = (Player) event.getWhoClicked();
        int limit = sm.getInt(SettingsManager.POTION_LIMIT);
        if (limit < 0) return;

        // 현재 포션 개수가 한도 미만이면, 굳이 복잡한 검사를 할 필요가 없습니다.
        if (countPotions(player.getInventory()) < limit) {
            return;
        }

        // --- 이 시점부터 플레이어는 포션 소지 한도에 도달한 상태입니다. ---
        // 따라서, 포션을 '추가'하려는 모든 시도를 막아야 합니다.

        InventoryAction action = event.getAction();
        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();

        // [상황 1] 커서에 있는 포션을 플레이어 인벤토리로 옮기려는 경우
        if (isPotion(cursorItem) && event.getClickedInventory() == player.getInventory()) {
            // 예외: 커서의 포션과 인벤토리의 포션을 맞바꾸는 것은 허용 (개수 변화 없음)
            if (action == InventoryAction.SWAP_WITH_CURSOR && isPotion(currentItem)) {
                return;
            }
            event.setCancelled(true);
            notify(player);
            return;
        }

        // [상황 2] 외부 인벤토리(상자 등)에서 Shift-Click으로 포션을 가져오려는 경우
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (isPotion(currentItem) && event.getClickedInventory() != player.getInventory()) {
                event.setCancelled(true);
                notify(player);
                return;
            }
        }

        // [상황 3] 외부 인벤토리의 포션을 핫바 키(1-9)로 가져오려는 경우
        if (action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD) {
            if (isPotion(currentItem) && event.getClickedInventory() != player.getInventory()) {
                // 예외: 핫바의 아이템도 포션이면 맞바꾸기이므로 허용 (개수 변화 없음)
                if (isPotion(player.getInventory().getItem(event.getHotbarButton()))) {
                    return;
                }
                event.setCancelled(true);
                notify(player);
            }
        }
    }

    /**
     * 드래그를 통해 포션을 획득하는 경우를 처리합니다.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        // 드래그하는 아이템이 포션이 아니면 무시
        if (!isPotion(event.getOldCursor())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int limit = sm.getInt(SettingsManager.POTION_LIMIT);
        if (limit < 0) return;

        int potionsBefore = countPotions(player.getInventory());
        int potionsAdded = 0;

        // 드래그로 인해 플레이어 인벤토리에 '새로' 추가되는 포션의 개수만 계산
        for (int slot : event.getRawSlots()) {
            if (event.getView().getInventory(slot) == player.getInventory()) {
                // 원래 비어있거나 포션이 아니었던 슬롯에 포션이 놓이면 '추가'로 간주
                if (!isPotion(player.getInventory().getItem(slot))) {
                    potionsAdded++;
                }
            }
        }

        // 최종 개수가 한도를 초과하면 이벤트 취소
        if (potionsBefore + potionsAdded > limit) {
            event.setCancelled(true);
            notify(player);
        }
    }

    /**
     * 땅에 떨어진 포션을 줍는 경우를 처리합니다.
     */
    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!isPotion(event.getItem().getItemStack())) return;

        Player player = (Player) event.getEntity();
        if (countPotions(player.getInventory()) >= sm.getInt(SettingsManager.POTION_LIMIT)) {
            event.setCancelled(true);
            // 아이템이 사라지지 않도록 줍는 딜레이를 다시 설정
            event.getItem().setPickupDelay(40);
            notify(player);
        }
    }

    /**
     * 인벤토리를 닫을 때 포션 소지 한도를 초과했는지 최종적으로 확인하고, 초과분을 땅에 떨어뜨립니다.
     * 이는 모든 획득 경로를 막지 못했을 경우를 대비한 최종 안전장치입니다.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        int limit = sm.getInt(SettingsManager.POTION_LIMIT);
        if (limit < 0) return;

        int currentAmount = countPotions(inventory);
        if (currentAmount <= limit) return;

        int excessAmount = currentAmount - limit;
        player.sendMessage("§c포션 소지 한도를 초과하여 " + excessAmount + "개의 포션을 땅에 떨어뜨립니다.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);

        // 초과분을 인벤토리 뒤에서부터(핫바 -> 주 인벤토리 -> 왼손 순) 제거하고 땅에 드랍
        for (int i = inventory.getSize() - 1; i >= 0; i--) {
            if (excessAmount <= 0) break;

            ItemStack item = inventory.getItem(i);
            if (isPotion(item)) {
                // 아이템을 땅에 떨어뜨립니다.
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                // 인벤토리에서 해당 아이템을 제거합니다.
                inventory.setItem(i, null);
                excessAmount--;
            }
        }
    }
}