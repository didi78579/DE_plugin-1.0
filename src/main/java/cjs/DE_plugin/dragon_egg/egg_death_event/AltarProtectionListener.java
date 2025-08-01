package cjs.DE_plugin.dragon_egg.egg_death_event;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class AltarProtectionListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // 파괴하려는 블록이 제단의 일부인지 확인
        if (AltarManager.isAltarBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c이 제단은 파괴할 수 없습니다.");
        }
    }
}