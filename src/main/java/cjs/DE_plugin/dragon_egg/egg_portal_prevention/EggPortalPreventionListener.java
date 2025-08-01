package cjs.DE_plugin.dragon_egg.egg_portal_prevention;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class EggPortalPreventionListener implements Listener {

    private final SettingsManager sm;

    public EggPortalPreventionListener(DE_plugin plugin) {
        this.sm = plugin.getSettingsManager();
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        // 설정이 비활성화되어 있으면 아무것도 하지 않음
        if (!sm.getBoolean(SettingsManager.PREVENT_PORTAL_WITH_EGG)) {
            return;
        }

        Player player = event.getPlayer();

        // 플레이어가 드래곤 알을 가지고 있는지 확인
        if (player.getInventory().contains(Material.DRAGON_EGG)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("§c드래곤 알을 소지한 상태에서는 차원을 이동할 수 없습니다."));
        }
    }
}