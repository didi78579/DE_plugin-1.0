package cjs.DE_plugin.settings.apply;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.Material;
import org.bukkit.Sound; // Sound 임포트 추가
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerTradeLimitListener implements Listener {

    private final DE_plugin plugin;
    private final SettingsManager sm;
    private final Map<UUID, Integer> tradeCounts = new ConcurrentHashMap<>();

    public VillagerTradeLimitListener(DE_plugin plugin) {
        this.plugin = plugin;
        this.sm = plugin.getSettingsManager();
        startDailyResetTask();
    }

    @EventHandler
    public void onTrade(InventoryClickEvent event) {
        // 1. 상인 인벤토리인지, 클릭한 사람이 플레이어인지 확인
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        // 2. 결과 슬롯(slot 2)을 클릭했는지 확인
        if (event.getSlot() != 2) return;

        // 3. [개선] 결과 슬롯에 아이템이 있는지 확인 (비어있으면 무시)
        ItemStack resultItem = event.getCurrentItem();
        if (resultItem == null || resultItem.getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // 4. [개선] Shift-Click으로 거래하는 경우, 재고 소모 문제를 해결하며 거래를 차단
        if (event.isShiftClick()) {
            // --- 재고 소모 문제 해결을 위한 새로운 접근 ---
            if (event.getInventory().getHolder() instanceof Merchant) {
                Merchant merchant = (Merchant) event.getInventory().getHolder();

                // 1. 현재 레시피의 사용 횟수를 미리 기록합니다.
                int recipeIndex = -1;
                int originalUses = -1;

                // Merchant#getRecipe(int)는 Paper API에만 존재할 수 있으므로, 루프를 사용합니다.
                for (int i = 0; i < merchant.getRecipes().size(); i++) {
                    MerchantRecipe recipe = merchant.getRecipes().get(i);
                    if (recipe != null && recipe.getResult().equals(resultItem)) {
                        recipeIndex = i;
                        originalUses = recipe.getUses();
                        break;
                    }
                }

                if (recipeIndex != -1) {
                    final int finalRecipeIndex = recipeIndex;
                    final int finalOriginalUses = originalUses;

                    // 2. 이벤트를 취소하고, 다음 틱으로 넘겨 서버의 재고 변경을 기다립니다.
                    event.setCancelled(true);
                    player.sendMessage("§cShift-Click으로 거래할 수 없습니다. 아이템을 직접 클릭해주세요.");
                    playFailSound(player); // 거래 실패 사운드 재생

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // 3. 다음 틱에서, 변경된 레시피를 가져와 사용 횟수를 원래대로 되돌립니다.
                            MerchantRecipe recipe = merchant.getRecipe(finalRecipeIndex);
                            if (recipe != null && recipe.getUses() != finalOriginalUses) {
                                recipe.setUses(finalOriginalUses);
                            }
                            // 4. 플레이어의 인벤토리를 업데이트하여 시각적 오류를 수정합니다.
                            player.updateInventory();
                        }
                    }.runTask(plugin);
                }
            } else {
                // Villager가 아닌 다른 상인
                event.setCancelled(true);
                player.sendMessage("§cShift-Click으로 거래할 수 없습니다. 아이템을 직접 클릭해주세요.");
                playFailSound(player); // 거래 실패 사운드 재생
            }
            return; // Shift-Click 처리는 여기서 종료
        }

        // 5. 거래 횟수 제한 로직 (일반 클릭에만 적용)
        UUID playerId = player.getUniqueId();
        int limit = sm.getInt(SettingsManager.VILLAGER_TRADE_LIMIT);
        if (limit < 0) return; // 제한이 없으면 통과

        int currentTrades = tradeCounts.getOrDefault(playerId, 0);

        if (currentTrades >= limit) {
            event.setCancelled(true);
            player.sendMessage("§c하루에 " + limit + "번만 거래할 수 있습니다.");
            playFailSound(player); // 거래 실패 사운드 재생
            player.closeInventory();
        } else {
            // 정상적인 거래로 간주하여 횟수 증가
            tradeCounts.put(playerId, currentTrades + 1);
            player.sendMessage("§a거래 횟수: " + (currentTrades + 1) + "/" + limit);
        }
    }

    /**
     * 거래 실패 시 플레이어에게 효과음을 들려줍니다.
     * @param player 효과음을 들을 플레이어
     */
    private void playFailSound(Player player) {
        // 모루가 땅에 떨어지는 소리를 사용하여 '깡' 소리를 표현하고, 피치를 높여 효과를 강조합니다.
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 1.8f);
    }

    private void startDailyResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // 매일 자정(월드 시간 기준)에 초기화
                long time = plugin.getServer().getWorlds().get(0).getTime();
                // 자정부터 1분(1200틱) 사이에 한 번만 실행되도록 조건 추가
                if (time >= 0 && time < 1200) {
                    if (!tradeCounts.isEmpty()) {
                        plugin.getLogger().info("주민 거래 횟수를 초기화합니다.");
                        tradeCounts.clear();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 1200L); // 1분마다 체크
    }
}