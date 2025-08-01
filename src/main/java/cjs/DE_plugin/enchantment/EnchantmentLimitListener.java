package cjs.DE_plugin.enchantment;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class EnchantmentLimitListener implements Listener {

    private final SettingsManager sm;

    public EnchantmentLimitListener(DE_plugin plugin) {
        this.sm = plugin.getSettingsManager();
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack firstItem = inventory.getItem(0);
        ItemStack secondItem = inventory.getItem(1);

        // 모루에 아이템이 2개 모두 있어야 함
        if (firstItem == null || secondItem == null) {
            return;
        }

        // 설정에서 최대 레벨 값을 가져옴
        int protectionMax = sm.getInt(SettingsManager.ENCHANT_PROTECTION_MAX_LEVEL);
        int sharpnessMax = sm.getInt(SettingsManager.ENCHANT_SHARPNESS_MAX_LEVEL);

        // 결과 아이템을 복제하여 작업 (원본을 직접 수정하지 않음)
        ItemStack result = event.getResult();
        if (result == null) {
            // 바닐라에서 조합이 불가능한 경우(예: 보호4+보호4), 첫 아이템을 기반으로 결과물을 생성
            result = firstItem.clone();
        } else {
            result = result.clone();
        }

        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta == null) return;

        boolean isChanged = false;

        // 보호 인챈트 처리
        isChanged |= handleEnchantment(resultMeta, firstItem, secondItem, Enchantment.PROTECTION_ENVIRONMENTAL, protectionMax);
        // 날카로움 인챈트 처리
        isChanged |= handleEnchantment(resultMeta, firstItem, secondItem, Enchantment.DAMAGE_ALL, sharpnessMax);

        if (isChanged) {
            result.setItemMeta(resultMeta);
            event.setResult(result);

            // 경험치 비용을 적절히 설정 (예시: 레벨당 4)
            // 이 부분은 게임 밸런스에 맞게 조절할 수 있습니다.
            int finalCost = inventory.getRepairCost() + 4 * (result.getEnchantments().values().stream().mapToInt(Integer::intValue).sum());
            inventory.setRepairCost(Math.min(finalCost, 39)); // 모루 최대 비용은 39
        }
    }

    /**
     * 특정 인챈트의 조합을 처리하고, 레벨이 확장되었는지 여부를 반환합니다.
     */
    private boolean handleEnchantment(ItemMeta resultMeta, ItemStack first, ItemStack second, Enchantment targetEnchant, int maxLevel) {
        Map<Enchantment, Integer> firstEnchants = getEnchantments(first);
        Map<Enchantment, Integer> secondEnchants = getEnchantments(second);

        int level1 = firstEnchants.getOrDefault(targetEnchant, 0);
        int level2 = secondEnchants.getOrDefault(targetEnchant, 0);

        if (level1 == 0 && level2 == 0) {
            return false; // 두 아이템 모두 해당 인챈트가 없음
        }

        int finalLevel;
        if (level1 == level2) {
            finalLevel = level1 + 1; // 같은 레벨이면 +1
        } else {
            finalLevel = Math.max(level1, level2); // 다른 레벨이면 더 높은 쪽으로
        }

        // 바닐라 최대 레벨을 초과하지만, 우리가 설정한 최대 레벨 이하인 경우
        if (finalLevel > targetEnchant.getMaxLevel() && finalLevel <= maxLevel) {
            applyEnchantment(resultMeta, targetEnchant, finalLevel);
            return true;
        }

        return false;
    }

    /**
     * 아이템 또는 마법이 부여된 책에서 인챈트 목록을 가져옵니다.
     */
    private Map<Enchantment, Integer> getEnchantments(ItemStack item) {
        if (item.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            return meta.getStoredEnchants();
        }
        return item.getEnchantments();
    }

    /**
     * 아이템 또는 마법이 부여된 책의 메타데이터에 인챈트를 적용합니다.
     */
    private void applyEnchantment(ItemMeta meta, Enchantment enchant, int level) {
        if (meta instanceof EnchantmentStorageMeta) {
            ((EnchantmentStorageMeta) meta).addStoredEnchant(enchant, level, true);
        } else {
            meta.addEnchant(enchant, level, true);
        }
    }
}