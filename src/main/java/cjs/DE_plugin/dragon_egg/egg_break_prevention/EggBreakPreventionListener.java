package cjs.DE_plugin.dragon_egg.egg_break_prevention;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class EggBreakPreventionListener implements Listener {

    /**
     * 드래곤 알이 중력의 영향을 받지 않도록 막는 1차 방어선입니다.
     * @param event 블록 물리 이벤트
     */
    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
        }
    }

    /**
     * 블록이 FallingBlock 엔티티로 변하는 것을 막는, 더 확실한 중력 방지 메서드입니다.
     * @param event 엔티티-블록 변경 이벤트
     */
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        // 블록이 떨어지는 엔티티(FallingBlock)로 변하려고 하고, 그 블록이 드래곤 알인 경우
        if (event.getEntityType() == EntityType.FALLING_BLOCK && event.getBlock().getType() == Material.DRAGON_EGG) {
            // 이벤트를 취소하여 떨어지는 것을 막습니다.
            event.setCancelled(true);
            // 블록의 상태를 강제로 업데이트하여, 클라이언트 측의 깜빡임(시각적 버그)을 방지합니다.
            event.getBlock().getState().update(false, false);
        }
    }

    /**
     * 폭발로 인해 드래곤 알이 파괴되는 것을 막습니다.
     * @param event 엔티티 폭발 이벤트
     */
    @EventHandler(priority = EventPriority.HIGHEST) // 다른 플러그인과의 충돌을 막기 위해 우선순위를 높게 설정
    public void onEntityExplode(EntityExplodeEvent event) {
        // 폭발에 의해 파괴될 블록 목록에서 드래곤 알을 모두 제거합니다.
        event.blockList().removeIf(block -> block.getType() == Material.DRAGON_EGG);
    }

    /**
     * 피스톤이 드래곤 알을 미는 것을 방지합니다.
     * @param event 피스톤 확장 이벤트
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        // 피스톤이 밀어내는 블록 목록을 확인합니다.
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.DRAGON_EGG) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 점착 피스톤이 드래곤 알을 당기는 것을 방지합니다.
     * @param event 피스톤 수축 이벤트
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        // 점착 피스톤이 당기는 블록 목록을 확인합니다.
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.DRAGON_EGG) {
                event.setCancelled(true);
                return;
            }
        }
    }
}