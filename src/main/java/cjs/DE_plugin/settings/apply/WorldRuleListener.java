package cjs.DE_plugin.settings.apply;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import io.papermc.paper.advancement.AdvancementDisplay;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class WorldRuleListener implements Listener {

    private final SettingsManager sm;

    public WorldRuleListener(DE_plugin plugin) {
        this.sm = plugin.getSettingsManager();
        // 플러그인 시작 시 모든 월드에 규칙 적용
        applyAllWorldRules();
    }

    // 발전과제 숨기기
    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (sm.getBoolean(SettingsManager.HIDE_ADVANCEMENTS)) {
            // [수정] AdvancementDisplay가 null이 아니고, 채팅에 알림을 보내는 유형일 때만 메시지를 숨깁니다.
            // 복잡한 리플렉션 대신, 안정적인 API를 직접 사용하도록 변경했습니다.
            io.papermc.paper.advancement.AdvancementDisplay display = event.getAdvancement().getDisplay();
            // [수정] shouldAnnounceToChat() 메소드 대신, display 객체의 존재 여부만으로 알림을 확인합니다.
            if (display != null) {
                event.message(null);
            }
        }
    }

    // [신규] 채팅 금지
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) { // [수정] 최신 Paper API 이벤트로 변경
        if (sm.getBoolean(SettingsManager.CHAT_BANNED)) {
            // 관리자 권한이 있는 플레이어는 채팅 금지를 우회할 수 있습니다.
            if (!event.getPlayer().hasPermission("de.admin.chat.bypass")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("§c채팅이 금지되어 있습니다.", NamedTextColor.RED));
            }
        }
    }

    // [신규] 킬로그 비활성화
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (sm.getBoolean(SettingsManager.KILL_LOG_DISABLED)) {
            event.deathMessage(null);
        }
    }


    // 좌표 숨기기 등 월드 규칙 적용
    public void applyAllWorldRules() {
        boolean hideCoords = sm.getBoolean(SettingsManager.HIDE_COORDINATES);
        for (World world : Bukkit.getServer().getWorlds()) {
            // F3 디버그 화면에서 좌표를 숨기는 게임 규칙 설정
            world.setGameRule(GameRule.REDUCED_DEBUG_INFO, hideCoords);
        }
        Bukkit.getLogger().info("[DE_plugin] 월드 게임 규칙을 설정값에 따라 적용했습니다.");
    }
}