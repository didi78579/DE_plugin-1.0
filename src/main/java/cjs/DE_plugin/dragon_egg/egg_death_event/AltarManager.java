package cjs.DE_plugin.dragon_egg.egg_death_event;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 드래곤 알 제단의 생성, 제거, 상태를 관리하는 클래스
 */
public class AltarManager {

    // 활성화된 모든 제단의 블록 위치를 저장합니다. Key: 제단 블록의 Location, Value: 제단의 기준점(알 위치)
    private static final Map<Location, Location> altarBlocks = new HashMap<>();
    // 각 제단의 원상 복구를 위한 원래 블록 데이터를 저장합니다. Key: 제단의 기준점(알 위치), Value: 원래 블록 데이터 맵
    private static final Map<Location, Map<Location, BlockData>> originalBlocks = new HashMap<>();

    /**
     * 새로운 제단을 등록합니다.
     * @param altarCenterLocation 제단의 기준점 (알이 놓일 위치)
     * @param blocksInAltar 제단을 구성하는 모든 블록의 위치 Set
     * @param originalBlockData 복원을 위해 저장할 원래 블록들의 데이터 Map
     */
    public static void createAltar(Location altarCenterLocation, Set<Location> blocksInAltar, Map<Location, BlockData> originalBlockData) {
        originalBlocks.put(altarCenterLocation, originalBlockData);
        for (Location blockLoc : blocksInAltar) {
            altarBlocks.put(blockLoc, altarCenterLocation);
        }
        // 알 위치도 제단 블록에 포함시킵니다.
        altarBlocks.put(altarCenterLocation, altarCenterLocation);
    }

    /**
     * 특정 위치의 블록이 제단의 일부인지 확인합니다.
     * @param location 확인할 블록의 위치
     * @return 제단의 일부이면 true
     */
    public static boolean isAltarBlock(Location location) {
        return altarBlocks.containsKey(location);
    }

    /**
     * 제단을 제거하고 원래 블록으로 복원합니다.
     * @param altarCenterLocation 제거할 제단의 기준점 (알이 있던 위치)
     */
    public static void removeAltar(Location altarCenterLocation) {
        if (!originalBlocks.containsKey(altarCenterLocation)) {
            return;
        }

        // get()으로 가져온 후 바로 remove()하여 맵에서 제단 정보를 먼저 제거합니다.
        Map<Location, BlockData> blocksToRestore = originalBlocks.remove(altarCenterLocation);
        if (blocksToRestore == null) return; // 만약의 경우를 대비한 null 체크

        for (Map.Entry<Location, BlockData> entry : blocksToRestore.entrySet()) {
            // 블록을 원래 데이터로 복원합니다. false 옵션은 불필요한 물리 업데이트를 방지하여 성능을 향상시킵니다.
            entry.getKey().getBlock().setBlockData(entry.getValue(), false);
        }

        // 보호되던 블록 목록(altarBlocks)에서도 해당 제단의 모든 블록을 제거합니다.
        // 복원된 블록들의 위치(keySet)를 직접 사용하여 제거하므로, 이전보다 훨씬 효율적이고 정확합니다.
        for (Location loc : blocksToRestore.keySet()) {
            altarBlocks.remove(loc);
        }
        // 제단 중심(알 위치)도 보호 목록에서 제거합니다.
        altarBlocks.remove(altarCenterLocation);
    }

    /**
     * [신규] 활성화된 제단이 하나라도 있는지 확인합니다.
     * @return 제단이 하나 이상 존재하면 true
     */
    public static boolean isAnyAltarActive() {
        return !originalBlocks.isEmpty();
    }
}