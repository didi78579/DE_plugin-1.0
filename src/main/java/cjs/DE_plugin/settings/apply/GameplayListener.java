package cjs.DE_plugin.settings.apply;

import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class GameplayListener implements Listener {

    private final SettingsManager sm;

    public GameplayListener(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    // 플레이어 경험치 드롭 배율
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getKeepLevel()) return;

        double multiplier = sm.getDouble(SettingsManager.PLAYER_EXP_DROP_MULTIPLIER);
        int originalExp = event.getDroppedExp();
        int newExp = (int) (originalExp * multiplier);
        event.setDroppedExp(newExp);
    }

    // 부활 드래곤 경험치 & 폭발 데미지
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // 부활 드래곤 경험치
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            // DragonBattle 객체가 null이 아니고, 엔드 포탈이 이미 존재하는지(두 번째 드래곤 이후인지) 확인
            if (event.getEntity().getWorld().getEnderDragonBattle() != null &&
                    event.getEntity().getWorld().getEnderDragonBattle().getEndPortalLocation() != null) {

                int level = sm.getInt(SettingsManager.RESPAWNED_DRAGON_EXP_LEVEL);
                // 마인크래프트 레벨을 경험치 총량으로 변환하는 공식
                int exp;
                if (level >= 32) {
                    exp = (int) (4.5 * level * level - 162.5 * level + 2220);
                } else if (level >= 17) {
                    exp = (int) (2.5 * level * level - 40.5 * level + 360);
                } else if (level > 0) {
                    exp = level * level + 6 * level;
                } else {
                    exp = 0;
                }
                event.setDroppedExp(exp);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        // 폭발 데미지 비율
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            double multiplier = sm.getDouble(SettingsManager.EXPLOSION_DAMAGE_MULTIPLIER);
            double newDamage = event.getDamage() * multiplier;
            event.setDamage(newDamage);
        }
    }
}