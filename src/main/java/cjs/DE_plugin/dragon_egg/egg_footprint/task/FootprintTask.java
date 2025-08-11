package cjs.DE_plugin.dragon_egg.egg_footprint.task;

import cjs.DE_plugin.dragon_egg.egg_footprint.Footprint;
import cjs.DE_plugin.dragon_egg.egg_footprint.FootprintManager;
import cjs.DE_plugin.gametime.GameTimeManager; // [추가]
import cjs.DE_plugin.settings.SettingsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.*;
import java.util.stream.Collectors;

public class FootprintTask extends BukkitRunnable {

    private final SettingsManager sm;
    private final GameTimeManager gameTimeManager; // [추가]
    private final FootprintManager footprintManager;

    // [수정] 생성자에 GameTimeManager 추가
    public FootprintTask(SettingsManager settingsManager, GameTimeManager gameTimeManager, FootprintManager footprintManager) {
        this.sm = settingsManager;
        this.gameTimeManager = gameTimeManager;
        this.footprintManager = footprintManager;
    }

    @Override
    public void run() {
        // [핵심 추가] 게임이 실행 중이 아닐 때는 발자국 관련 로직을 모두 건너뜁니다.
        if (!gameTimeManager.isRunning()) {
            return;
        }

        checkPlayerInventories();
        updateExistingFootprints();
        createNewFootprints();
    }

    // ... (이하 모든 코드는 변경 없음) ...

    private void checkPlayerInventories() {
        Set<UUID> playersWithEgg = Bukkit.getOnlinePlayers().stream()
                // [수정] 주 인벤토리뿐만 아니라 왼손(off-hand)에 드래곤 알을 들고 있는 경우도 확인합니다.
                .filter(p -> p.getInventory().contains(Material.DRAGON_EGG) || p.getInventory().getItemInOffHand().getType() == Material.DRAGON_EGG)
                .map(Player::getUniqueId)
                .collect(Collectors.toSet());

        // 새로 알을 가진 플레이어 추적 시작
        playersWithEgg.stream()
                .filter(id -> !footprintManager.isTracking(id))
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(footprintManager::startTracking);

        // 알을 잃은 플레이어 추적 중지
        new HashSet<>(footprintManager.getTrackedPlayerIds()).stream()
                .filter(trackedId -> !playersWithEgg.contains(trackedId))
                .forEach(footprintManager::stopTracking);
    }

    private void updateExistingFootprints() {
        if (Bukkit.getWorlds().isEmpty()) return;
        World mainWorld = Bukkit.getWorlds().get(0);
        boolean isDay = mainWorld.getTime() >= 0 && mainWorld.getTime() < 13000;

        // [복원] 시간 기반 만료 로직
        long durationDays = sm.getInt(SettingsManager.EGG_FOOTPRINT_DURATION_DAYS);
        // 마인크래프트 하루는 20분입니다. (20분 * 60초 * 1000밀리초)
        long durationMs = durationDays * 20 * 60 * 1000;

        // Concurrent-safe 컬렉션(예: CopyOnWriteArraySet)을 사용하면 반복 중 수정이 안전합니다.
        for (Footprint fp : footprintManager.getAllFootprints()) {
            if (System.currentTimeMillis() - fp.getCreationTime() > durationMs) {
                // FootprintManager를 통해 발자국을 제거합니다.
                // 이 방식은 Manager가 내부 컬렉션과 파일 저장을 모두 처리한다고 가정합니다.
                footprintManager.removeFootprint(fp);
            } else {
                // 만료되지 않은 발자국의 가시성을 업데이트합니다.
                toggleVisibility(fp, isDay);
            }
        }
    }

    private void createNewFootprints() {
        footprintManager.getTrackedPlayerIds().stream()
                .map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline())
                .forEach(player -> {
                    // [핵심 추가] 알 소유자 주변에 파티클 생성
                    spawnHoldingParticles(player);

                    Location lastLocation = footprintManager.getLastLocation(player.getUniqueId());
                    Location currentLocation = player.getLocation();

                    if (lastLocation != null && lastLocation.getWorld().equals(currentLocation.getWorld())) {
                        createDoubleSidedFootprint(lastLocation, currentLocation, player.getUniqueId());
                    }

                    footprintManager.updateLastLocation(player.getUniqueId(), currentLocation);
                });
    }

    /**
     * [신규 메소드] 드래곤 알을 소유한 플레이어 주변에 보라색 파티클을 생성합니다.
     * @param player 파티클을 생성할 플레이어
     */
    private void spawnHoldingParticles(Player player) {
        if (player.getWorld() == null) return;

        // 포탈 파티클을 플레이어 주변에 생성합니다.
        player.getWorld().spawnParticle(
                Particle.PORTAL,          // 파티클 종류
                player.getLocation().add(0, 1, 0), // 생성 위치 (플레이어 몸 중앙)
                20,                       // 파티클 개수
                0.5,                      // X축 퍼짐 범위
                0.5,                      // Y축 퍼짐 범위
                0.5,                      // Z축 퍼짐 범위
                0.01                      // 파티클 속도
        );
    }

    private void toggleVisibility(Footprint fp, boolean isDay) {
        Entity front = Bukkit.getEntity(fp.getFrontEntityId());
        Entity back = Bukkit.getEntity(fp.getBackEntityId());
        boolean shouldHideAtNight = sm.getBoolean(SettingsManager.HIDE_FOOTPRINTS_AT_NIGHT);

        // 숨기기 옵션이 꺼져있거나, 낮일 경우 텍스트를 보여줍니다.
        if (!shouldHideAtNight || isDay) {
            if (front instanceof TextDisplay) {
                TextDisplay display = (TextDisplay) front;
                // 텍스트가 비어있다면, 원래 텍스트('<')로 복원
                if (display.text().equals(Component.empty())) {
                    display.text(Component.text("<", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
                }
            }
            if (back instanceof TextDisplay) {
                TextDisplay display = (TextDisplay) back;
                // 텍스트가 비어있다면, 원래 텍스트('>')로 복원
                if (display.text().equals(Component.empty())) {
                    display.text(Component.text(">", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
                }
            }
        } else {
            // 숨기기 옵션이 켜져있고 밤일 경우, 텍스트를 지워서 숨깁니다.
            if (front instanceof TextDisplay) {
                TextDisplay display = (TextDisplay) front;
                // 텍스트가 있다면, 빈 텍스트로 변경
                if (!display.text().equals(Component.empty())) {
                    display.text(Component.empty());
                }
            }
            if (back instanceof TextDisplay) {
                TextDisplay display = (TextDisplay) back;
                // 텍스트가 있다면, 빈 텍스트로 변경
                if (!display.text().equals(Component.empty())) {
                    display.text(Component.empty());
                }
            }
        }
    }

    private void createDoubleSidedFootprint(Location from, Location to, UUID ownerId) {
        // 1. 앞면('<')을 생성하고 올바른 방향으로 회전시킵니다.
        TextDisplay frontDisplay = createSingleDisplay(from, to, "<");
        if (frontDisplay == null) return;

        // 2. 뒷면('>')을 생성합니다. 이 단계에서는 뒷면도 앞면과 같은 방향을 봅니다.
        TextDisplay backDisplay = createSingleDisplay(from, to, ">");
        if (backDisplay == null) {
            frontDisplay.remove(); // 뒷면 생성 실패 시 앞면도 제거
            return;
        }

        // 3. (핵심) 뒷면만 뒤집어 완벽한 반대면을 만듭니다.
        Transformation backTransform = backDisplay.getTransformation();
        Quaternionf baseRotation = backTransform.getLeftRotation();

        // 목표 방향 벡터를 다시 계산합니다. 이 벡터가 뒤집기 회전의 '축'이 됩니다.
        Vector targetDirection = to.toVector().subtract(from.toVector());
        targetDirection.setY(0);
        if (targetDirection.lengthSquared() < 1.0E-6) {
            targetDirection = new Vector(0, 0, -1);
        }
        targetDirection.normalize();

        // 화살표가 가리키는 방향(targetDirection)을 축으로 180도 회전(수직 뒤집기)하는 회전값을 만듭니다.
        Quaternionf flipRotation = new Quaternionf().fromAxisAngleRad(
                (float) targetDirection.getX(),
                (float) targetDirection.getY(),
                (float) targetDirection.getZ(),
                (float) Math.toRadians(180)
        );

        // 최종 뒷면 회전을 계산합니다: (앞을 보도록 만든 회전)을 적용한 후, 그 결과를 축을 중심으로 뒤집습니다.
        Quaternionf finalBackRotation = new Quaternionf(flipRotation).mul(baseRotation);

        // 최종 회전값을 설정하고 엔티티에 적용합니다.
        backTransform.getLeftRotation().set(finalBackRotation);
        backDisplay.setTransformation(backTransform);

        // 생성된 발자국 정보를 Manager에 등록합니다.
        Location actualFootprintLocation = from.clone().add(0, 0.01, 0);
        Footprint footprint = new Footprint(frontDisplay.getUniqueId(), backDisplay.getUniqueId(), ownerId, actualFootprintLocation, System.currentTimeMillis());
        footprintManager.addFootprint(footprint);
    }
    /**
     * [최종 완성] 발자국 엔티티를 생성하고 초기 상태를 설정합니다.
     */
    private TextDisplay createSingleDisplay(Location from, Location to, String text) {
        World world = from.getWorld();
        if (world == null) return null;

        // 발자국은 항상 실제 위치에 생성합니다.
        Location displayLocation = from.clone().add(0, 0.01, 0);
        TextDisplay display = (TextDisplay) world.spawnEntity(displayLocation, EntityType.TEXT_DISPLAY);

        // --- 기본 속성 설정 ---
        boolean isDay = world.getTime() >= 0 && world.getTime() < 13000;
        boolean shouldHideAtNight = sm.getBoolean(SettingsManager.HIDE_FOOTPRINTS_AT_NIGHT);

        // 숨기기 옵션이 꺼져있거나, 낮일 경우에만 텍스트를 보여줍니다.
        if (!shouldHideAtNight || isDay) {
            display.text(Component.text(text, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        } else {
            // 숨기기 옵션이 켜져있고 밤일 경우, 빈 텍스트로 생성
            display.text(Component.empty());
        }

        display.setShadowed(false);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setBillboard(Display.Billboard.FIXED);
        display.setRotation(0, 0);
        // 투명도는 항상 60%로 고정 (밤에는 텍스트가 없으므로 의미 없음)
        display.setTextOpacity((byte) (255 * 0.6));

        // --- 최종 회전 계산 ---
        Vector targetDirection = to.toVector().subtract(from.toVector());
        targetDirection.setY(0); // 수직 움직임 무시하여 안정성 확보
        if (targetDirection.lengthSquared() < 1.0E-6) {
            targetDirection = new Vector(0, 0, -1); // 움직임이 없으면 북쪽을 기본값으로 사용
        }
        targetDirection.normalize();

        // 2. 바닥에 눕히는 '수직 회전'을 정의합니다. (X축 기준 -90도)
        Quaternionf pitchRotation = new Quaternionf().fromAxisAngleRad(1, 0, 0, (float) Math.toRadians(-90));

        // 3. 눕혀진 상태에서, 기호의 꼭짓점이 가리키는 기본 방향을 정의합니다.
        Vector initialFlatTipDirection;
        if (text.equals("<")) {
            initialFlatTipDirection = new Vector(-1, 0, 0); // '<' 기호는 서쪽을 향함
        } else { // ">"
            initialFlatTipDirection = new Vector(1, 0, 0);  // '>' 기호는 동쪽을 향함
        }

        // 4. 눕혀진 기호의 꼭짓점을 목표 방향으로 보내는 '방향 회전(Yaw)'을 계산합니다.
        Quaternionf yawRotation = new Quaternionf().rotationTo(initialFlatTipDirection.toVector3f(), targetDirection.toVector3f());

        // 5. 최종 회전을 결합합니다: 먼저 눕히고(pitch), 그 다음에 방향을 돌립니다(yaw).
        Quaternionf finalRotation = new Quaternionf(yawRotation).mul(pitchRotation);

        // --- 변환 적용 ---
        Transformation transformation = display.getTransformation();
        final float scale = 4.0f;
        transformation.getScale().set(scale);
        transformation.getLeftRotation().set(finalRotation);
        display.setTransformation(transformation);

        return display;
    }
}