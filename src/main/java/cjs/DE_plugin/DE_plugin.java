package cjs.DE_plugin;

import org.bukkit.inventory.ShapedRecipe;
// 필요한 import 문을 추가합니다.
import cjs.DE_plugin.dragon_egg.egg_portal_prevention.EggPortalPreventionListener;
import cjs.DE_plugin.dragon_egg.placed_egg_effect.PlacedEggListener;
import cjs.DE_plugin.dragon_egg.placed_egg_effect.PlacedEggManager;
// --- 기존 import 문 ---
import cjs.DE_plugin.command.MainCommand;
import cjs.DE_plugin.command.MainTabCompleter;
import cjs.DE_plugin.enchantment.EnchantmentLimitListener;
import cjs.DE_plugin.gametime.GameTimeManager;
import cjs.DE_plugin.settings.SettingsManager;
import cjs.DE_plugin.settings.apply.*;
import cjs.DE_plugin.dragon_egg.egg_acquisition.EggAcquisitionListener;
import cjs.DE_plugin.dragon_egg.egg_break_prevention.EggBreakPreventionListener;
import cjs.DE_plugin.dragon_egg.egg_death_event.AltarProtectionListener;
import cjs.DE_plugin.dragon_egg.egg_death_event.EggDeathListener;
import cjs.DE_plugin.dragon_egg.egg_storage_prevention.EggStoragePreventionListener;
import cjs.DE_plugin.dragon_egg.egg_footprint.FootprintChunkListener;
import cjs.DE_plugin.dragon_egg.egg_footprint.FootprintManager;
import cjs.DE_plugin.dragon_egg.egg_footprint.task.FootprintTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


public final class DE_plugin extends JavaPlugin {

    private BukkitTask footprintTask;
    private SettingsManager settingsManager;
    private WorldRuleListener worldRuleListener;
    private WorldBorderManager worldBorderManager;
    private GameTimeManager gameTimeManager;
    private FootprintManager footprintManager;
    private PlacedEggManager placedEggManager; // [추가]

    @Override
    public void onEnable() {
        getLogger().info("DE_plugin이 활성화되었습니다.");

        // --- 시스템 초기화 (리스너 및 명령어 등록 전) ---
        this.settingsManager = new SettingsManager(this);
        this.worldRuleListener = new WorldRuleListener(this);
        this.worldBorderManager = new WorldBorderManager(this);
        this.gameTimeManager = new GameTimeManager(this);
        this.footprintManager = new FootprintManager(this);
        this.placedEggManager = new PlacedEggManager(this); // [추가]

        // --- 명령어 등록 ---
        getCommand("de").setExecutor(new MainCommand(this));
        getCommand("de").setTabCompleter(new MainTabCompleter(this.settingsManager));

        // --- 리스너 등록 ---
        // 드래곤 알
        getServer().getPluginManager().registerEvents(new EggAcquisitionListener(this), this);
        getServer().getPluginManager().registerEvents(new EggBreakPreventionListener(), this);
        getServer().getPluginManager().registerEvents(new EggDeathListener(), this);
        getServer().getPluginManager().registerEvents(new AltarProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new EggStoragePreventionListener(), this);
        getServer().getPluginManager().registerEvents(new FootprintChunkListener(this.footprintManager), this); // [신규] 청크 로드 시 만료된 발자국 제거
        getServer().getPluginManager().registerEvents(new PlacedEggListener(this.placedEggManager), this); // [추가]
        // 설정 적용
        getServer().getPluginManager().registerEvents(new BannedItemsListener(this.settingsManager), this);
        getServer().getPluginManager().registerEvents(new GameplayListener(this.settingsManager), this);
        getServer().getPluginManager().registerEvents(new PotionLimitListener(this.settingsManager), this);
        getServer().getPluginManager().registerEvents(new VillagerTradeLimitListener(this), this);
        getServer().getPluginManager().registerEvents(this.worldRuleListener, this);
        getServer().getPluginManager().registerEvents(new EnchantmentLimitListener(this), this); // [이 줄 추가]
        getServer().getPluginManager().registerEvents(new EggPortalPreventionListener(this), this);
        getServer().getPluginManager().registerEvents(new GoldenAppleListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalTravelListener(this), this); // [신규] 지옥문 이동 리스너
        getServer().getPluginManager().registerEvents(this.gameTimeManager, this);

        // --- 작업(Task) 시작 ---
        // [핵심 변경] FootprintTask 생성 시 gameTimeManager를 전달합니다.
        this.footprintTask = new FootprintTask(this.settingsManager, this.gameTimeManager, this.footprintManager).runTaskTimer(this, 0L, 10L);

        // [신규] 커스텀 레시피 등록
        registerCustomRecipes();
    }

    @Override
    public void onDisable() {
        getLogger().info("DE_plugin이 비활성화되었습니다.");

        // [핵심 변경] gameTimeManager.save() 대신 shutdown()을 호출하여
        // 드래곤 정리와 데이터 저장을 모두 처리합니다.
        if (this.gameTimeManager != null) {
            this.gameTimeManager.shutdown();
        }

        if (this.placedEggManager != null) {
            this.placedEggManager.save();
            this.placedEggManager.cancelTask();
        }
        if (this.footprintManager != null) {
            // [수정] 자동 저장 태스크를 중지하고 최종 데이터를 저장하기 위해 shutdown()을 호출합니다.
            this.footprintManager.shutdown();
        }
        if (this.footprintTask != null && !this.footprintTask.isCancelled()) {
            this.footprintTask.cancel();
        }
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public WorldRuleListener getWorldRuleListener() {
        return worldRuleListener;
    }

    public WorldBorderManager getWorldBorderManager() {
        return worldBorderManager;
    }
    public GameTimeManager getGameTimeManager() {
        return gameTimeManager;
    }

    public FootprintManager getFootprintManager() {
        return footprintManager;
    }

    /**
     * 플러그인에서 사용할 커스텀 조합법을 등록합니다.
     */
    private void registerCustomRecipes() {
        // 네더라이트 강화 대장장이 형판 커스텀 조합법
        if (settingsManager.getBoolean(SettingsManager.CRAFT_NETHERITE_TEMPLATE_ENABLED)) {
            ItemStack result = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
            NamespacedKey key = new NamespacedKey(this, "custom_netherite_upgrade_template");

            // 서버 리로드 시 레시피가 중복 등록되는 것을 방지
            if (Bukkit.getRecipe(key) == null) {
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                recipe.shape(
                        "DND",
                        "DRD",
                        "DDD"
                );
                recipe.setIngredient('D', Material.DIAMOND);
                recipe.setIngredient('N', Material.NETHERITE_INGOT);
                recipe.setIngredient('R', Material.NETHERRACK);

                Bukkit.addRecipe(recipe);
            }
        }
    }
}