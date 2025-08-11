package cjs.DE_plugin.dragon_egg.egg_death_event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 드래곤 알이 지상에 놓였을 때 제단 생성 등 관련 이벤트를 처리하는 클래스
 */
public class EggDeathManager {

    /**
     * 플레이어가 드래곤 알을 놓았을 때 제단 생성 로직을 시작합니다.
     * @param player 알을 놓은 플레이어
     * @param eggBlock 알이 놓인 블록
     */
    public void handleEggPlace(Player player, Block eggBlock) {
        buildAltar(eggBlock.getLocation());
        player.sendMessage("§d드래곤 알이 대지에 안착하여 제단이 형성되었습니다.");
    }

    /**
     * 지정된 위치에 제단을 건설합니다.
     * @param center 제단의 중심 (알이 놓인 위치)
     */
    private void buildAltar(Location center) {
        Map<Location, BlockData> originalBlocks = new HashMap<>();
        Set<Location> altarShape = new HashSet<>();

        // --- Y-2 Layer: Beacon Base (3x3 Obsidian) ---
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                placeBlock(center.clone().add(x, -2, z), Material.OBSIDIAN, null, originalBlocks, altarShape);
            }
        }

        // --- Y-1 Layer: Beacon and surrounding blocks ---
        placeBlock(center.clone().add(0, -1, 0), Material.BEACON, null, originalBlocks, altarShape);
        placeBlock(center.clone().add(1, -1, 1), Material.CRYING_OBSIDIAN, null, originalBlocks, altarShape);
        placeBlock(center.clone().add(-1, -1, 1), Material.CRYING_OBSIDIAN, null, originalBlocks, altarShape);
        placeBlock(center.clone().add(1, -1, -1), Material.CRYING_OBSIDIAN, null, originalBlocks, altarShape);
        placeBlock(center.clone().add(-1, -1, -1), Material.CRYING_OBSIDIAN, null, originalBlocks, altarShape);
        placeBlock(center.clone().add(1, -1, 0), Material.QUARTZ_PILLAR, null, originalBlocks, altarShape);
        placeBlock(center.clone().add(-1, -1, 0), Material.QUARTZ_PILLAR, null, originalBlocks, altarShape);
        placeBlock(center.clone().add(0, -1, 1), Material.QUARTZ_PILLAR, null, originalBlocks, altarShape);
        placeBlock(center.clone().add(0, -1, -1), Material.QUARTZ_PILLAR, null, originalBlocks, altarShape);

        // --- Y=0 Layer: Stairs around the egg ---
        // 계단이 알을 바라보도록 설치합니다.
        placeStair(center.clone().add(1, 0, 0), BlockFace.WEST, originalBlocks, altarShape);  // 동쪽 계단 -> 서쪽 바라보기
        placeStair(center.clone().add(-1, 0, 0), BlockFace.EAST, originalBlocks, altarShape); // 서쪽 계단 -> 동쪽 바라보기
        placeStair(center.clone().add(0, 0, 1), BlockFace.NORTH, originalBlocks, altarShape); // 남쪽 계단 -> 북쪽 바라보기
        placeStair(center.clone().add(0, 0, -1), BlockFace.SOUTH, originalBlocks, altarShape); // 북쪽 계단 -> 남쪽 바라보기

        AltarManager.createAltar(center, altarShape, originalBlocks);
    }

    private void placeBlock(Location loc, Material material, BlockData data, Map<Location, BlockData> originalBlocks, Set<Location> altarShape) {
        Block block = loc.getBlock();
        originalBlocks.put(loc.clone(), block.getBlockData());
        altarShape.add(loc.clone());
        block.setType(material, false);
        if (data != null) block.setBlockData(data, false);
    }

    private void placeStair(Location loc, BlockFace facing, Map<Location, BlockData> originalBlocks, Set<Location> altarShape) {
        Stairs stairData = (Stairs) Material.QUARTZ_STAIRS.createBlockData();
        stairData.setFacing(facing);
        placeBlock(loc, Material.QUARTZ_STAIRS, stairData, originalBlocks, altarShape);
    }
}