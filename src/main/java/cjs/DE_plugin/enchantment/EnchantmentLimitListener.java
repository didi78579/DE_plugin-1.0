package cjs.DE_plugin.enchantment;

import org.bukkit.GameMode;
import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnchantmentLimitListener implements Listener {

    private final DE_plugin plugin;
    private final SettingsManager sm;

    // [추가] 자주 사용하는 인챈트를 상수로 정의하여 중복 조회를 피하고 가독성을 높입니다.
    private static final Enchantment INFINITY = Enchantment.getByKey(NamespacedKey.minecraft("infinity"));
    private static final Enchantment MENDING = Enchantment.getByKey(NamespacedKey.minecraft("mending"));
    private static final Enchantment PROTECTION = Enchantment.getByKey(NamespacedKey.minecraft("protection"));
    private static final Enchantment SHARPNESS = Enchantment.getByKey(NamespacedKey.minecraft("sharpness"));

    public EnchantmentLimitListener(DE_plugin plugin) {
        this.plugin = plugin;
        this.sm = plugin.getSettingsManager();
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        // 활 무한 비활성화
        if (INFINITY != null && sm.getBoolean(SettingsManager.ENCHANT_BOW_INFINITY_DISABLED) && event.getItem().getType() == Material.BOW) {
            event.getEnchantsToAdd().remove(INFINITY);
        }

        // 갑옷 수선 비활성화
        if (MENDING != null && sm.getBoolean(SettingsManager.ENCHANT_ARMOR_MENDING_DISABLED) && isArmor(event.getItem())) {
            event.getEnchantsToAdd().remove(MENDING);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack first = inventory.getItem(0);
        ItemStack second = inventory.getItem(1);

        // 모루에 아이템이 2개 모두 있어야 함
        if (first == null || second == null) {
            return;
        }

        // [신규] 활 무한 조합 비활성화
        if (sm.getBoolean(SettingsManager.ENCHANT_BOW_INFINITY_DISABLED) && first.getType() == Material.BOW && isEnchantedBookWith(second, INFINITY)) {
            event.setResult(null);
            return;
        }

        // [신규] 갑옷 수선 조합 비활성화
        if (sm.getBoolean(SettingsManager.ENCHANT_ARMOR_MENDING_DISABLED) && isArmor(first) && isEnchantedBookWith(second, MENDING)) {
            event.setResult(null);
            return;
        }

        // 1. 최종 인챈트 목록을 계산하기 위한 맵을 준비합니다. (첫 번째 아이템 기준)
        Map<Enchantment, Integer> finalEnchants = new HashMap<>(getEnchantments(first));
        boolean customUpgradeOccurred = false;

        // 2. 두 번째 아이템의 인챈트를 순회하며 병합 로직을 수행합니다.
        Map<Enchantment, Integer> secondEnchants = getEnchantments(second);
        int protectionMax = sm.getInt(SettingsManager.ENCHANT_PROTECTION_MAX_LEVEL);
        int sharpnessMax = sm.getInt(SettingsManager.ENCHANT_SHARPNESS_MAX_LEVEL);

        for (Map.Entry<Enchantment, Integer> entry : secondEnchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level2 = entry.getValue();

            // 다른 인챈트와 충돌하는지 확인합니다.
            boolean conflicts = false;
            for (Enchantment existingEnchant : finalEnchants.keySet()) {
                if (enchant != existingEnchant && enchant.conflictsWith(existingEnchant)) {
                    conflicts = true;
                    break;
                }
            }
            if (conflicts) continue;

            // 충돌하지 않으면, 최종 레벨을 계산합니다.
            int level1 = finalEnchants.getOrDefault(enchant, 0);
            int finalLevel = (level1 == level2) ? level1 + 1 : Math.max(level1, level2);

            // 3. 계산된 레벨이 커스텀 레벨업 조건에 맞는지 확인합니다.
            boolean isCustomLevel = false;
            int vanillaMax = enchant.getMaxLevel();
            int customMax = 0;

            if (enchant.equals(PROTECTION)) {
                customMax = protectionMax;
            } else if (enchant.equals(SHARPNESS)) {
                customMax = sharpnessMax;
            }

            // 해당 인챈트가 플러그인 관리 대상이고, 결과 레벨이 커스텀 범위에 속하는지 확인
            if (customMax > 0 && finalLevel > vanillaMax && finalLevel <= customMax) {
                // [수정] 커스텀 레벨업은 동일한 레벨의 아이템 두 개를 조합할 때만 허용합니다 (N + N -> N+1).
                if (level1 == level2 && finalLevel == level1 + 1) {
                    isCustomLevel = true;
                }
            }

            if (isCustomLevel) {
                finalEnchants.put(enchant, finalLevel);
                customUpgradeOccurred = true;
            } else if (finalLevel <= enchant.getMaxLevel()) {
                // 바닐라 레벨 범위 내의 유효한 조합이면 최종 목록에 추가합니다.
                finalEnchants.put(enchant, finalLevel);
            }
        }

        // 4. [수정] 커스텀 레벨업이 발생한 경우, 바닐라 결과와 상관없이 플러그인이 직접 결과물을 생성하고 비용을 설정합니다.
        // 이렇게 해야 '아이템+책' 조합 시 바닐라가 낮은 레벨의 결과물을 만드는 것을 덮어쓸 수 있습니다.
        if (customUpgradeOccurred) {
            ItemStack result = first.clone();
            ItemMeta meta = result.getItemMeta();

            // 모든 인챈트를 지우고 계산된 최종 인챈트 목록을 새로 적용합니다.
            clearEnchantments(meta);
            for (Map.Entry<Enchantment, Integer> entry : finalEnchants.entrySet()) {
                applyEnchantment(meta, entry.getKey(), entry.getValue());
            }
            result.setItemMeta(meta);
            event.setResult(result);

            int cost = sm.getInt(SettingsManager.ENCHANT_OVER_LIMIT_COST);
            plugin.getServer().getScheduler().runTask(plugin, () -> inventory.setRepairCost(cost));
        }
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

    /**
     * [신규] 아이템 메타에서 모든 인챈트를 제거합니다.
     */
    private void clearEnchantments(ItemMeta meta) {
        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) meta;
            // 복사본을 만들어 ConcurrentModificationException 방지
            new HashMap<>(bookMeta.getStoredEnchants()).keySet().forEach(bookMeta::removeStoredEnchant);
        } else {
            new HashMap<>(meta.getEnchants()).keySet().forEach(meta::removeEnchant);
        }
    }

    /**
     * 아이템이 갑옷인지 확인합니다.
     */
    private boolean isArmor(ItemStack item) {
        if (item == null) return false;
        String typeName = item.getType().name();
        return typeName.endsWith("_HELMET") || typeName.endsWith("_CHESTPLATE") || typeName.endsWith("_LEGGINGS") || typeName.endsWith("_BOOTS");
    }

    /**
     * 아이템이 특정 인챈트가 부여된 책인지 확인합니다.
     */
    private boolean isEnchantedBookWith(ItemStack item, Enchantment enchant) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK || enchant == null) return false;
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        return meta != null && meta.hasStoredEnchant(enchant);
    }
}