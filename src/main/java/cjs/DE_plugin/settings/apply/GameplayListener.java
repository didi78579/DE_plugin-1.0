package cjs.DE_plugin.settings.apply;

import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventPriority;
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
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // keepInventory gamerule이 켜져있으면 경험치를 드롭하지 않으므로, 아무것도 하지 않습니다.
        if (event.getKeepLevel()) {
            return;
        }

        Player player = event.getEntity();
        double multiplier = sm.getDouble(SettingsManager.PLAYER_EXP_DROP_MULTIPLIER);

        // multiplier가 1.0이면, 사망 전 총 경험치를 드롭합니다.
        if (multiplier == 1.0) {
            // PlayerDeathEvent에서는 플레이어의 경험치가 아직 초기화되지 않았으므로,
            // getTotalExperience()를 통해 사망 직전의 총 경험치를 가져올 수 있습니다.
            event.setDroppedExp(player.getTotalExperience());
        } else {
            // multiplier가 1.0이 아니면, 기존 로직대로 바닐라 드롭량에 배율을 적용합니다.
            int originalExp = event.getDroppedExp();
            if (originalExp == 0) return;
            int newExp = (int) Math.round(originalExp * multiplier);
            event.setDroppedExp(newExp);
        }
    }

    // 부활 드래곤 경험치 & 폭발 데미지
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // 부활 드래곤 경험치
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            // DragonBattle 객체가 null이 아니고, 엔드 포탈이 이미 존재하는지(두 번째 드래곤 이후인지) 확인
            if (event.getEntity().getWorld().getEnderDragonBattle() != null &&
                    event.getEntity().getWorld().getEnderDragonBattle().getEndPortalLocation() != null) {

                // [수정] 설정된 '레벨'에 도달하는데 필요한 총 경험치를 계산하여 드롭합니다.
                int targetLevel = sm.getInt(SettingsManager.RESPAWNED_DRAGON_EXP_LEVEL);
                int expToDrop = getExpForLevel(targetLevel);
                event.setDroppedExp(expToDrop);
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

    /**
     * [신규] 목표 레벨에 도달하기 위해 필요한 총 경험치 양을 계산합니다.
     * @param level 목표 레벨
     * @return 0레벨부터 목표 레벨까지 필요한 총 경험치
     */
    private int getExpForLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level <= 16) {
            return (int) (Math.pow(level, 2) + 6 * level);
        } else if (level <= 31) {
            return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        } else { // level >= 32
            return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
        }
    }
}