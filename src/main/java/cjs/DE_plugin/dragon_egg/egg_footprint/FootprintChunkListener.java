package cjs.DE_plugin.dragon_egg.egg_footprint;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class FootprintChunkListener implements Listener {

    private final FootprintManager footprintManager;

    public FootprintChunkListener(FootprintManager footprintManager) {
        this.footprintManager = footprintManager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // 청크가 로드될 때, 해당 청크 내의 만료된 발자국을 확인하고 제거합니다.
        footprintManager.checkAndRemoveExpiredFootprintsInChunk(event.getChunk());
    }
}