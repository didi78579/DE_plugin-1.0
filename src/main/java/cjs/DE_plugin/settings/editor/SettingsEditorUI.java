package cjs.DE_plugin.settings.editor;

import cjs.DE_plugin.settings.SettingsManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SettingsEditorUI {

    private final SettingsManager sm;

    public SettingsEditorUI(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    /**
     * 설정 카테고리를 선택할 수 있는 메인 메뉴를 엽니다.
     * @param player 대상 플레이어
     */
    public void open(Player player) {
        player.sendMessage("§e======== §f[DE Plugin 설정] §e========");
        player.sendMessage("§7수정할 설정 카테고리를 선택하세요.");

        // 일반 설정 버튼
        TextComponent generalButton = new TextComponent("  §a▶ 일반 설정");
        generalButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/de settings general"));
        generalButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§e게임 플레이, 아이템, 규칙 등 일반 설정을 엽니다.").create()));
        player.spigot().sendMessage(generalButton);

        // 월드 보더 설정 버튼
        TextComponent borderButton = new TextComponent("  §b▶ 월드 보더 설정");
        borderButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/de settings border"));
        borderButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§e월드 보더 크기 및 배율 설정을 엽니다.").create()));
        player.spigot().sendMessage(borderButton);

        // 인챈트 설정 버튼
        TextComponent enchantButton = new TextComponent("  §d▶ 인챈트 설정");
        enchantButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/de settings enchant"));
        enchantButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§e인챈트 최대 레벨 설정을 엽니다.").create()));
        player.spigot().sendMessage(enchantButton);

        // [신규] 드래곤 알 설정 버튼
        TextComponent dragonEggButton = new TextComponent("  §5▶ 드래곤 알 설정");
        dragonEggButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/de settings dragon"));
        dragonEggButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§e드래곤 알 관련 설정을 엽니다.").create()));
        player.spigot().sendMessage(dragonEggButton);

        player.sendMessage("§e===================================");
    }

    /**
     * 일반 설정 UI를 엽니다.
     */
    public void openGeneralSettings(Player player) {
        player.sendMessage("§e======== §f[일반 설정] §e========");
        // 숫자 설정
        sendNumberSetting(player, "주민 거래 제한", SettingsManager.VILLAGER_TRADE_LIMIT, 1);
        sendNumberSetting(player, "포션 최대 소지 수", SettingsManager.POTION_LIMIT, 1);
        sendNumberSetting(player, "부활 드래곤 경험치(Lv)", SettingsManager.RESPAWNED_DRAGON_EXP_LEVEL, 5);
        sendNumberSetting(player, "황금사과 재생 시간(초)", SettingsManager.GOLDEN_APPLE_REGEN_DURATION_SECONDS, 1);
        sendNumberSetting(player, "게임 플레이 타임(일)", SettingsManager.GAME_PLAY_TIME_DAYS, 1);

        // 비율(소수) 설정
        sendDecimalSetting(player, "폭발 데미지 비율", SettingsManager.EXPLOSION_DAMAGE_MULTIPLIER, 0.05);
        sendDecimalSetting(player, "플레이어 경험치 드롭율", SettingsManager.PLAYER_EXP_DROP_MULTIPLIER, 0.1);

        // ON/OFF 설정
        sendBooleanSetting(player, "엔더 진주 금지", SettingsManager.ENDER_PEARL_BANNED);
        sendBooleanSetting(player, "엔더 상자 금지", SettingsManager.ENDER_CHEST_BANNED);
        sendBooleanSetting(player, "방패 금지", SettingsManager.SHIELD_BANNED);
        sendBooleanSetting(player, "불사의 토템 금지", SettingsManager.TOTEM_BANNED);
        sendBooleanSetting(player, "발전과제 숨기기", SettingsManager.HIDE_ADVANCEMENTS);
        sendBooleanSetting(player, "좌표 숨기기", SettingsManager.HIDE_COORDINATES);
        sendBooleanSetting(player, "밤에 발자국 숨기기", SettingsManager.HIDE_FOOTPRINTS_AT_NIGHT);
        sendBooleanSetting(player, "채팅 금지", SettingsManager.CHAT_BANNED);
        sendBooleanSetting(player, "킬로그 비활성화", SettingsManager.KILL_LOG_DISABLED);
        sendBooleanSetting(player, "네더라이트 형판 조합", SettingsManager.CRAFT_NETHERITE_TEMPLATE_ENABLED);

        player.sendMessage("§e===================================");
        sendBackButton(player); // 뒤로가기 버튼 추가
    }

    /**
     * 월드 보더 설정 UI를 엽니다.
     */
    public void openBorderSettings(Player player) {
        player.sendMessage("§e======== §f[월드 보더 설정] §e========");
        sendNumberSetting(player, "오버월드 크기", SettingsManager.WORLDBORDER_OVERWORLD_SIZE, 100);
        sendNumberSetting(player, "지옥 배율", SettingsManager.WORLDBORDER_NETHER_SCALE, 1);
        sendBooleanSetting(player, "엔더 보더 활성화", SettingsManager.WORLDBORDER_END_ENABLED);
        player.sendMessage("§e===================================");
        sendBackButton(player); // 뒤로가기 버튼 추가
    }

    /**
     * 인챈트 설정 UI를 엽니다.
     */
    public void openEnchantSettings(Player player) {
        player.sendMessage("§e======== §f[인챈트 설정] §e========");
        sendNumberSetting(player, "보호 최대 레벨", SettingsManager.ENCHANT_PROTECTION_MAX_LEVEL, 1);
        sendNumberSetting(player, "날카로움 최대 레벨", SettingsManager.ENCHANT_SHARPNESS_MAX_LEVEL, 1);
        sendNumberSetting(player, "초과 인챈트 비용(Lv)", SettingsManager.ENCHANT_OVER_LIMIT_COST, 5);
        sendBooleanSetting(player, "활 무한 금지", SettingsManager.ENCHANT_BOW_INFINITY_DISABLED);
        sendBooleanSetting(player, "갑옷 수선 금지", SettingsManager.ENCHANT_ARMOR_MENDING_DISABLED);
        player.sendMessage("§e===================================");
        sendBackButton(player); // 뒤로가기 버튼 추가
    }

    /**
     * [신규] 드래곤 알 설정 UI를 엽니다.
     */
    public void openDragonEggSettings(Player player) {
        player.sendMessage("§e======== §f[드래곤 알 설정] §e========");
        sendNumberSetting(player, "알 발자국 지속시간(일)", SettingsManager.EGG_FOOTPRINT_DURATION_DAYS, 1);
        sendBooleanSetting(player, "알 소지 시 차원이동 금지", SettingsManager.PREVENT_PORTAL_WITH_EGG);
        sendBooleanSetting(player, "알 우클릭으로 획득", SettingsManager.EGG_ACQUISITION_BY_CLICK_ENABLED);
        player.sendMessage("§e===================================");
        sendBackButton(player); // 뒤로가기 버튼 추가
    }


    /**
     * 정수(Integer) 타입의 설정을 채팅창에 표시합니다.
     */
    private void sendNumberSetting(Player player, String name, String key, int increment) {
        int value = sm.getInt(key);
        TextComponent message = new TextComponent("§7- " + name + ": ");

        TextComponent minusButton = new TextComponent("§c[-]");
        minusButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/de set " + key + " " + (value - increment)));
        minusButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§c-" + increment + " 감소").create()));
        message.addExtra(minusButton);

        message.addExtra(" ");

        TextComponent valueComponent = new TextComponent("§b[" + value + "]");
        valueComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/de set " + key + " "));
        valueComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§e클릭하여 직접 값 입력").create()));
        message.addExtra(valueComponent);

        message.addExtra(" ");

        TextComponent plusButton = new TextComponent("§a[+]");
        plusButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/de set " + key + " " + (value + increment)));
        plusButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§a+" + increment + " 증가").create()));
        message.addExtra(plusButton);

        player.spigot().sendMessage(message);
    }

    /**
     * 소수(Double) 타입의 설정을 채팅창에 표시합니다.
     */
    private void sendDecimalSetting(Player player, String name, String key, double increment) {
        double value = sm.getDouble(key);
        BigDecimal bdValue = BigDecimal.valueOf(value);
        BigDecimal bdIncrement = BigDecimal.valueOf(increment);

        String displayValue = bdValue.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String minusValue = bdValue.subtract(bdIncrement).setScale(2, RoundingMode.HALF_UP).toPlainString();
        String plusValue = bdValue.add(bdIncrement).setScale(2, RoundingMode.HALF_UP).toPlainString();

        TextComponent message = new TextComponent("§7- " + name + ": ");

        TextComponent minusButton = new TextComponent("§c[-]");
        minusButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/de set " + key + " " + minusValue));
        minusButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§c-" + increment + " 감소").create()));
        message.addExtra(minusButton);

        message.addExtra(" ");

        TextComponent valueComponent = new TextComponent("§b[" + displayValue + "]");
        valueComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/de set " + key + " "));
        valueComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§e클릭하여 직접 값 입력").create()));
        message.addExtra(valueComponent);

        message.addExtra(" ");

        TextComponent plusButton = new TextComponent("§a[+]");
        plusButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/de set " + key + " " + plusValue));
        plusButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§a+" + increment + " 증가").create()));
        message.addExtra(plusButton);

        player.spigot().sendMessage(message);
    }

    /**
     * 참/거짓(Boolean) 타입의 설정을 채팅창에 표시합니다.
     */
    private void sendBooleanSetting(Player player, String name, String key) {
        boolean value = sm.getBoolean(key);
        String displayValue = value ? "§a활성화" : "§c비활성화";
        String command = "/de set " + key + " " + !value;

        TextComponent message = new TextComponent("§7- " + name + ": ");
        TextComponent valueComponent = new TextComponent("[" + displayValue + "§r]");
        valueComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        valueComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§e클릭하여 토글").create()));
        message.addExtra(valueComponent);
        player.spigot().sendMessage(message);
    }

    /**
     * [신규] 뒤로가기 버튼을 생성하여 채팅창에 표시합니다.
     */
    private void sendBackButton(Player player) {
        TextComponent backButton = new TextComponent("§7« 뒤로가기");
        backButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/de settings"));
        backButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7설정 카테고리 선택으로 돌아갑니다.").create()));
        player.spigot().sendMessage(backButton);
    }
}