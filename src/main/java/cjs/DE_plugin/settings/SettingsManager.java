package cjs.DE_plugin.settings;

import cjs.DE_plugin.DE_plugin;
import org.bukkit.configuration.file.FileConfiguration;

public class SettingsManager {

    private final DE_plugin plugin;
    private FileConfiguration config;

    // 설정 키 (config.yml에 저장될 이름)
    public static final String VILLAGER_TRADE_LIMIT = "villager-trade-limit";
    public static final String ENDER_PEARL_BANNED = "ban.ender-pearl";
    public static final String ENDER_CHEST_BANNED = "ban.ender-chest";
    public static final String SHIELD_BANNED = "ban.shield";
    public static final String TOTEM_BANNED = "ban.totem";
    public static final String EXPLOSION_DAMAGE_MULTIPLIER = "explosion-damage-multiplier";
    public static final String HIDE_ADVANCEMENTS = "world-rules.hide-advancements";
    public static final String HIDE_COORDINATES = "world-rules.hide-coordinates";
    public static final String HIDE_FOOTPRINTS_AT_NIGHT = "world-rules.hide-footprints-at-night";
    public static final String PLAYER_EXP_DROP_MULTIPLIER = "player-exp-drop-multiplier";
    public static final String POTION_LIMIT = "potion-limit"; // [수정] 드래곤 경험치 설정 키 이름 변경
    public static final String RESPAWNED_DRAGON_EXP_LEVEL = "respawned-dragon-exp-level";
    public static final String EGG_FOOTPRINT_DURATION_DAYS = "dragon-egg-footprint-duration-days";
    public static final String GAME_PLAY_TIME_DAYS = "game-play-time-days";
    public static final String WORLDBORDER_OVERWORLD_SIZE = "worldborder.overworld-size";
    public static final String WORLDBORDER_NETHER_SCALE = "worldborder.nether-scale";
    public static final String WORLDBORDER_END_ENABLED = "worldborder.end-enabled";
    public static final String ENCHANT_PROTECTION_MAX_LEVEL = "enchant-protection-max-level";
    public static final String ENCHANT_SHARPNESS_MAX_LEVEL = "enchant-sharpness-max-level";
    public static final String ENCHANT_OVER_LIMIT_COST = "enchant-over-limit-cost";
    // [신규] 활 무한, 갑옷 수선 비활성화 설정
    public static final String ENCHANT_BOW_INFINITY_DISABLED = "enchant-bow-infinity-disabled";
    public static final String ENCHANT_ARMOR_MENDING_DISABLED = "enchant-armor-mending-disabled";
    // [신규] 황금사과 및 형판 레시피 설정
    public static final String GOLDEN_APPLE_REGEN_DURATION_SECONDS = "golden-apple-regen-duration-seconds";
    public static final String CRAFT_NETHERITE_TEMPLATE_ENABLED = "craft-netherite-template-enabled";
    // [핵심 추가] 새로운 설정 키
    public static final String PREVENT_PORTAL_WITH_EGG = "dragon-egg.prevent-portal-travel";
    // [신규] 채팅 및 킬로그 설정 키
    public static final String CHAT_BANNED = "game-rules.chat-banned";
    public static final String KILL_LOG_DISABLED = "game-rules.kill-log-disabled";


    public SettingsManager(DE_plugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadDefaultSettings();
    }

    // 플러그인 시작 시 기본 설정값을 config.yml에 생성
    private void loadDefaultSettings() {
        config.addDefault(VILLAGER_TRADE_LIMIT, 20);
        config.addDefault(ENDER_PEARL_BANNED, true);
        config.addDefault(ENDER_CHEST_BANNED, true);
        config.addDefault(SHIELD_BANNED, true);
        config.addDefault(TOTEM_BANNED, true);
        config.addDefault(EXPLOSION_DAMAGE_MULTIPLIER, 0.25); // 1/4
        config.addDefault(HIDE_ADVANCEMENTS, true);
        config.addDefault(HIDE_COORDINATES, true);
        config.addDefault(HIDE_FOOTPRINTS_AT_NIGHT, true);
        config.addDefault(PLAYER_EXP_DROP_MULTIPLIER, 1.0); // 100%
        config.addDefault(POTION_LIMIT, 2);
        config.addDefault(RESPAWNED_DRAGON_EXP_LEVEL, 30); // [수정] 부활 드래곤 처치 시 획득할 경험치 '레벨'
        config.addDefault(EGG_FOOTPRINT_DURATION_DAYS, 5);
        config.addDefault(GAME_PLAY_TIME_DAYS, 100);
        config.addDefault(WORLDBORDER_OVERWORLD_SIZE, 1000);
        config.addDefault(WORLDBORDER_NETHER_SCALE, 4);
        config.addDefault(WORLDBORDER_END_ENABLED, true);
        config.addDefault(ENCHANT_PROTECTION_MAX_LEVEL, 7);
        config.addDefault(ENCHANT_SHARPNESS_MAX_LEVEL, 10);
        config.addDefault(ENCHANT_OVER_LIMIT_COST, 35);
        // [신규] 활 무한, 갑옷 수선 비활성화 설정 기본값
        config.addDefault(ENCHANT_BOW_INFINITY_DISABLED, true);
        config.addDefault(ENCHANT_ARMOR_MENDING_DISABLED, true);
        // [신규] 황금사과 및 형판 레시피 설정 기본값
        config.addDefault(GOLDEN_APPLE_REGEN_DURATION_SECONDS, 2);
        config.addDefault(CRAFT_NETHERITE_TEMPLATE_ENABLED, true);
        // [핵심 추가] 새로운 설정의 기본값
        config.addDefault(PREVENT_PORTAL_WITH_EGG, true);
        // [신규] 채팅 및 킬로그 설정 기본값
        config.addDefault(CHAT_BANNED, true);
        config.addDefault(KILL_LOG_DISABLED, true);


        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    // 설정 값을 가져오는 메소드들
    public int getInt(String path) {
        return config.getInt(path);
    }

    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    public double getDouble(String path) {
        return config.getDouble(path);
    }

    public Object get(String path) {
        return config.get(path);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // 설정 값을 변경하고 저장하는 메소드
    public void set(String path, Object value) {
        config.set(path, value);
        plugin.saveConfig();
        // 변경된 설정을 즉시 적용하기 위해 리스너나 다른 모듈에 알리는 로직을 추가할 수 있습니다.
        // 예: BannedItemsListener.updateBannedItems();

        // --- 실시간 설정 적용 ---
        // 좌표 숨기기 설정이 변경된 경우, 월드 규칙을 다시 적용합니다.
        if (path.equals(HIDE_COORDINATES) && plugin.getWorldRuleListener() != null) {
            plugin.getWorldRuleListener().applyAllWorldRules();
        }

        // 월드 보더 관련 설정이 변경된 경우, 월드 보더를 다시 적용합니다.
        if ((path.equals(WORLDBORDER_OVERWORLD_SIZE) || path.equals(WORLDBORDER_NETHER_SCALE) || path.equals(WORLDBORDER_END_ENABLED)) && plugin.getWorldBorderManager() != null) {
            plugin.getWorldBorderManager().applyAllWorldBorders();
        }
    }
}