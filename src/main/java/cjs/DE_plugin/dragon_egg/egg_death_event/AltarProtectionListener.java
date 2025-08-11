package cjs.DE_plugin.dragon_egg.egg_death_event;

import cjs.DE_plugin.DE_plugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class AltarProtectionListener implements Listener {

    private final DE_plugin plugin;

    public AltarProtectionListener(DE_plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleAltarInteraction(event.getBlock(), event);
    }

    /**
     * 피스톤이 제단 블록을 미는 것을 방지합니다.
     */
    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (AltarManager.isAltarBlock(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 점착 피스톤이 제단 블록을 당기는 것을 방지합니다.
     */
    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (AltarManager.isAltarBlock(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 플레이어가 제단에 있는 드래곤 알을 클릭(펀치)하는 것을 감지합니다.
     * 바닐라에서는 이 행동으로 알이 텔레포트됩니다.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        // 드래곤 알을 클릭했을 때의 상호작용을 처리합니다.
        if (event.getClickedBlock().getType() == Material.DRAGON_EGG) {
            handleAltarInteraction(event.getClickedBlock(), event);
        }
    }

    /**
     * 제단 블록과의 상호작용(파괴, 클릭)을 처리하는 공통 로직입니다.
     * @param block 상호작용이 일어난 블록
     * @param event 관련된 이벤트 (취소 가능 여부 판단용)
     */
    private void handleAltarInteraction(Block block, Event event) {
        Location blockLocation = block.getLocation();

        // 블록이 제단의 일부가 아니면 무시
        if (!AltarManager.isAltarBlock(blockLocation)) {
            return;
        }

        // 상호작용한 블록이 드래곤 알인 경우
        if (block.getType() == Material.DRAGON_EGG) {
            // 1틱 후에 알이 사라졌는지 확인하고 제단을 제거합니다.
            // 알의 텔레포트 또는 파괴가 일어날 시간을 줍니다.
            new BukkitRunnable() {
                @Override
                public void run() {
                    // 알이 있던 위치를 다시 확인하여, 알이 없어졌다면(AIR), 제단을 제거합니다.
                    if (block.getType() != Material.DRAGON_EGG) {
                        AltarManager.removeAltar(blockLocation);
                    }
                }
            }.runTaskLater(plugin, 1L);
        } else { // 알이 아닌 다른 제단 블록인 경우
            if (event instanceof BlockBreakEvent) {
                ((BlockBreakEvent) event).setCancelled(true);
                ((BlockBreakEvent) event).getPlayer().sendMessage("§c이 제단은 파괴할 수 없습니다.");
            }
        }
    }
}