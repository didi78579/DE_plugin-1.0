package cjs.DE_plugin.dragon_egg.egg_acquisition;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import cjs.DE_plugin.dragon_egg.egg_death_event.AltarManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class EggAcquisitionListener implements Listener {

    private final SettingsManager sm;

    public EggAcquisitionListener(DE_plugin plugin) {
        this.sm = plugin.getSettingsManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 설정에서 비활성화된 경우, 아무것도 하지 않음
        if (!sm.getBoolean(SettingsManager.EGG_ACQUISITION_BY_CLICK_ENABLED)) {
            return;
        }
        // 이벤트가 블록 우클릭이 아니면 무시
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.DRAGON_EGG) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();

        if (inventory.firstEmpty() == -1) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c인벤토리가 가득 차서 알을 획득할 수 없습니다."));
        } else {
            // --- 수정/추가된 부분 시작 ---
            Location eggLocation = clickedBlock.getLocation();

            // AltarManager를 통해 제단인지 확인하고, 맞다면 제거 및 복구
            if (AltarManager.isAltarBlock(eggLocation)) {
                AltarManager.removeAltar(eggLocation);
            } else {
                // 제단이 아닌 일반 드래곤 알인 경우, 그냥 공기로 바꿈
                clickedBlock.setType(Material.AIR);
            }
            // --- 수정/추가된 부분 끝 ---

            inventory.addItem(new ItemStack(Material.DRAGON_EGG));
        }
    }
}