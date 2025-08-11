package cjs.DE_plugin.dragon_egg.egg_death_event;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EggDeathListener implements Listener {

    /**
     * 드래곤 알을 가진 플레이어가 사망했을 때 제단을 생성합니다.
     * @param event 플레이어 사망 이벤트
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // 사망 시 드롭될 아이템 목록에서 드래곤 알이 있는지 확인
        boolean hasDragonEgg = event.getDrops().stream().anyMatch(item -> item.getType() == Material.DRAGON_EGG);

        if (hasDragonEgg) {
            // 1. 드롭 목록에서 드래곤 알 아이템을 제거
            event.getDrops().removeIf(item -> item.getType() == Material.DRAGON_EGG);

            // 2. 제단 생성
            buildAltar(player.getLocation());
        }
    }

    private void buildAltar(Location deathLocation) {
        // 제단의 기준점(알 위치)을 사망 위치보다 한 칸 위로 설정합니다.
        Location eggLocation = deathLocation.getBlock().getLocation().add(0, 1, 0);
        Map<Location, BlockData> originalBlockData = new HashMap<>();
        Set<Location> altarBlockLocations = new HashSet<>();

        // 제단 구조의 기준이 되는 신호기 위치 (알 위치로부터 y-2)
        Location beaconLocation = eggLocation.clone().add(0, -2, 0);

        // --- 1. 제단이 지어질 모든 위치의 '원래' 블록 데이터를 미리 저장 ---

        // 네더라이트 층 (알 위치 -3)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location loc = beaconLocation.clone().add(x, -1, z);
                originalBlockData.put(loc, loc.getBlock().getBlockData());
                altarBlockLocations.add(loc);
            }
        }
        // 신호기/유리 층 (알 위치 -2)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location loc = beaconLocation.clone().add(x, 0, z);
                originalBlockData.put(loc, loc.getBlock().getBlockData());
                altarBlockLocations.add(loc);
            }
        }
        // 계단/중앙유리 층 (알 위치 -1)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location loc = eggLocation.clone().add(x, -1, z);
                originalBlockData.put(loc, loc.getBlock().getBlockData());
                altarBlockLocations.add(loc);
            }
        }
        // 알 위치
        originalBlockData.put(eggLocation, eggLocation.getBlock().getBlockData());
        altarBlockLocations.add(eggLocation);

        // 알 위쪽부터 월드 최대 높이까지의 블록 정보를 저장합니다.
        int worldMaxHeight = eggLocation.getWorld().getMaxHeight();
        for (int y = eggLocation.getBlockY() + 1; y < worldMaxHeight; y++) {
            Location locAbove = new Location(eggLocation.getWorld(), eggLocation.getX(), y, eggLocation.getZ());
            originalBlockData.put(locAbove, locAbove.getBlock().getBlockData());
            altarBlockLocations.add(locAbove);
        }


        // --- 2. 실제 제단 건설 ---

        // 1. 네더라이트 3x3 플랫폼 (알 위치 -3)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                beaconLocation.clone().add(x, -1, z).getBlock().setType(Material.NETHERITE_BLOCK);
            }
        }

        // 2. 신호기 (알 위치 -2 중앙)
        beaconLocation.getBlock().setType(Material.BEACON);

        // 3. 보라색 유리 (알 위치 -2 주변부)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // 중앙(신호기)은 건너뜀

                beaconLocation.clone().add(x, 0, z).getBlock().setType(Material.PURPLE_STAINED_GLASS);
            }
        }

        // 4. 알 바로 아래 보라색 유리 (알 위치 -1 중앙)
        eggLocation.clone().add(0, -1, 0).getBlock().setType(Material.PURPLE_STAINED_GLASS);

        // 5. 이끼 낀 조약돌 계단 (알 위치 -1 주변부)
        // 요청에 따라 모서리를 먼저 생성하고, 그 다음 십자 방향을 생성합니다.

        // 모서리 (Corners)
        for (int x = -1; x <= 1; x += 2) { // x는 -1, 1
            for (int z = -1; z <= 1; z += 2) { // z는 -1, 1
                Block stairBlock = eggLocation.clone().add(x, -1, z).getBlock();
                stairBlock.setType(Material.MOSSY_COBBLESTONE_STAIRS);
                Stairs stairData = (Stairs) stairBlock.getBlockData();
                // 모서리 계단은 남/북 방향을 우선으로 하여 대칭적인 모양을 만듭니다.
                stairData.setFacing(z == 1 ? BlockFace.NORTH : BlockFace.SOUTH);
                stairBlock.setBlockData(stairData);
            }
        }

        // 십자 (Cardinal directions)
        // 북/남 방향
        for (int z = -1; z <= 1; z += 2) { // z는 -1, 1
            Block stairBlock = eggLocation.clone().add(0, -1, z).getBlock();
            stairBlock.setType(Material.MOSSY_COBBLESTONE_STAIRS);
            Stairs stairData = (Stairs) stairBlock.getBlockData();
            stairData.setFacing(z == 1 ? BlockFace.NORTH : BlockFace.SOUTH);
            stairBlock.setBlockData(stairData);
        }
        // 동/서 방향
        for (int x = -1; x <= 1; x += 2) { // x는 -1, 1
            Block stairBlock = eggLocation.clone().add(x, -1, 0).getBlock();
            stairBlock.setType(Material.MOSSY_COBBLESTONE_STAIRS);
            Stairs stairData = (Stairs) stairBlock.getBlockData();
            stairData.setFacing(x == 1 ? BlockFace.WEST : BlockFace.EAST);
            stairBlock.setBlockData(stairData);
        }

        // 6. 드래곤 알 설치
        eggLocation.getBlock().setType(Material.DRAGON_EGG);

        // 알 위쪽을 공기로 변경합니다.
        for (int y = eggLocation.getBlockY() + 1; y < worldMaxHeight; y++) {
            Location locAbove = new Location(eggLocation.getWorld(), eggLocation.getX(), y, eggLocation.getZ());
            locAbove.getBlock().setType(Material.AIR, false); // false 옵션으로 불필요한 물리 업데이트 방지
        }

        // 7. AltarManager에 제단 정보 등록
        AltarManager.createAltar(eggLocation, altarBlockLocations, originalBlockData);
    }
}