package cjs.DE_plugin.gametime;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.apply.WorldBorderManager;
import cjs.DE_plugin.settings.SettingsManager;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class GameTimeManager implements Listener {

    private final DE_plugin plugin;
    private final SettingsManager sm;
    private final File dataFile;
    private FileConfiguration dataConfig;

    private boolean isRunning = false;
    private long startTick = 0;
    private long gameEndTick = 0;

    private BukkitTask timerTask;

    // 조종 가능한 드래곤과 그 제어 태스크를 관리합니다.
    private final Map<UUID, BukkitTask> dragonControlTasks = new HashMap<>();
    private final Set<UUID> specialDragonIds = new HashSet<>();

    public GameTimeManager(DE_plugin plugin) {
        this.plugin = plugin;
        this.sm = plugin.getSettingsManager();
        this.dataFile = new File(plugin.getDataFolder(), "gametime.yml");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        load();

        if (isRunning) {
            if (Bukkit.getWorlds().get(0).getFullTime() < gameEndTick) {
                startTimerTask();
            } else {
                this.isRunning = false;
                save();
            }
        }
    }

    public boolean isRunning() { // [추가] FootprintTask에서 사용하기 위한 getter
        return isRunning;
    }

    public void load() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "gametime.yml 파일을 생성할 수 없습니다.", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        isRunning = dataConfig.getBoolean("is-running", false);
        startTick = dataConfig.getLong("start-tick", 0);

        long gameDays = sm.getInt(SettingsManager.GAME_PLAY_TIME_DAYS);
        if (gameDays > 0) {
            this.gameEndTick = startTick + ((gameDays - 1) * 24000L);
        } else {
            this.gameEndTick = startTick;
        }
    }

    public void save() {
        dataConfig.set("is-running", isRunning);
        dataConfig.set("start-tick", startTick);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "gametime.yml 파일을 저장할 수 없습니다.", e);
        }
    }

    public void startGame() {
        if (isRunning) {
            Bukkit.broadcastMessage("§c게임 타이머는 이미 실행 중입니다.");
            return;
        }

        // [핵심 변경] 게임 시작 전, 저장된 보더 설정을 적용합니다.
        WorldBorderManager wbm = plugin.getWorldBorderManager();
        if (wbm != null) {
            wbm.applyAllWorldBorders();
        } else {
            Bukkit.broadcastMessage("§cWorldBorderManager를 찾을 수 없어 보더를 적용할 수 없습니다.");
        }

        // 시간을 아침으로 설정
        World mainWorld = Bukkit.getWorlds().get(0);
        if (mainWorld != null) {
            mainWorld.setTime(0L); // 0L은 아침(일출) 시간입니다.
        }

        this.isRunning = true;
        this.startTick = Bukkit.getWorlds().get(0).getFullTime();
        long gameDays = sm.getInt(SettingsManager.GAME_PLAY_TIME_DAYS);
        this.gameEndTick = startTick + ((gameDays - 1) * 24000L);

        save();
        startTimerTask();

        Component startMessage = Component.text("§a§l" + gameDays + "일 간의 게임이 시작되었습니다!");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(startMessage);
        }
    }

    public void stopGame() {
        if (!isRunning) {
            Bukkit.broadcastMessage("§c게임 타이머가 실행 중이지 않습니다.");
            return;
        }
        this.isRunning = false;
        if (timerTask != null) {
            timerTask.cancel();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearScoreboard(player);
        }

        plugin.getFootprintManager().clearAllFootprints(); // [핵심 추가] 게임 중지 시 모든 발자국 제거
        cleanupControllableDragons();
        save();

        Component stopMessage = Component.text("§e게임 타이머가 관리자에 의해 중지되었습니다.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(stopMessage);
        }
    }

    private void endGame() {
        if (!isRunning) return;

        this.isRunning = false;
        if (timerTask != null) {
            timerTask.cancel();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearScoreboard(player);
        }

        plugin.getFootprintManager().clearAllFootprints(); // [핵심 추가] 게임 종료 시 모든 발자국 제거

        Player winner = findEggHolder();

        // [핵심 변경] 월드 보더의 중앙으로 텔레포트
        WorldBorderManager wbm = plugin.getWorldBorderManager();
        World overworld = Bukkit.getWorld("world");
        Location spawnLocation;

        if (wbm != null && overworld != null) {
            Location center = wbm.getOverworldCenter();
            spawnLocation = overworld.getHighestBlockAt(center).getLocation().add(0.5, 1, 0.5);
        } else {
            // Fallback to world spawn
            overworld = Bukkit.getWorlds().get(0);
            spawnLocation = overworld.getHighestBlockAt(0, 0).getLocation().add(0.5, 1, 0.5);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawnLocation);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
        }

        if (winner != null) {
            String title = "§f" + winner.getName() + " §6§l우승";

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(title, "", 10, 100, 20);
            }

            final Player finalWinner = winner;
            new BukkitRunnable() {
                @Override
                public void run() {
                    EnderDragon dragon = finalWinner.getWorld().spawn(finalWinner.getLocation(), EnderDragon.class);
                    dragon.setPhase(EnderDragon.Phase.LEAVE_PORTAL);
                    dragon.addPassenger(finalWinner);

                    makeDragonControllable(finalWinner, dragon);
                }
            }.runTaskLater(plugin, 40L);

        } else {
            String title = "§c무승부";
            String subtitle = "§7아무도 드래곤 알을 소유하지 못했습니다.";
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(title, subtitle, 10, 100, 20);
            }
        }
        save();
    }

    private Player findEggHolder() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getInventory().contains(Material.DRAGON_EGG)) {
                return player;
            }
        }
        return null;
    }

    private void startTimerTask() {
        this.timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    this.cancel();
                    return;
                }

                World mainWorld = Bukkit.getWorlds().get(0);
                if (mainWorld == null) return;

                long now = mainWorld.getFullTime();

                if (now >= gameEndTick) {
                    endGame();
                    this.cancel();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }

                long remainingTicks = gameEndTick - now;
                if (remainingTicks <= 1200) {
                    long countdownSeconds = (remainingTicks / 20) + 1;
                    Component actionBarComponent = Component.text("§c§l드래곤 알 부화까지: " + countdownSeconds + " 초");

                    float progress = (1200f - remainingTicks) / 1200f;
                    float volume = 0.5f + progress * 1.5f;
                    float pitch = 1.0f + progress;

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(actionBarComponent);
                        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, volume, pitch);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void updateScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = player.getScoreboard();

        if (board == manager.getMainScoreboard()) {
            board = manager.getNewScoreboard();
        }

        long elapsedTicks = Bukkit.getWorlds().get(0).getFullTime() - startTick;
        long currentDay = (elapsedTicks / 24000L) + 1;
        Component newTitle = Component.text("§fDay: " + currentDay);

        Objective objective = board.getObjective("de_sidebar");

        if (objective == null) {
            objective = board.registerNewObjective("de_sidebar", "dummy", newTitle);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.displayName(newTitle);
        }

        player.setScoreboard(board);
    }

    private void clearScoreboard(Player player) {
        if (player.getScoreboard() != Bukkit.getScoreboardManager().getMainScoreboard()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    // --- [신규] 드래곤 제어 관련 메소드들 ---

    /**
     * 특정 드래곤을 플레이어가 조종할 수 있도록 설정합니다.
     * @param player 조종할 플레이어
     * @param dragon 조종될 드래곤
     */
    private void makeDragonControllable(Player player, EnderDragon dragon) {
        specialDragonIds.add(dragon.getUniqueId());

        BukkitTask controlTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!dragon.isValid() || dragon.getPassengers().isEmpty() || !dragon.getPassengers().get(0).equals(player)) {
                    this.cancel();
                    dragonControlTasks.remove(player.getUniqueId());
                    if (dragon.isValid()) {
                        dragon.remove();
                        specialDragonIds.remove(dragon.getUniqueId());
                    }
                    return;
                }

                Vector direction = player.getEyeLocation().getDirection();
                dragon.setVelocity(direction.multiply(2.0)); // 속도 조절 가능
            }
        }.runTaskTimer(plugin, 0L, 1L);

        dragonControlTasks.put(player.getUniqueId(), controlTask);
    }

    /**
     * 플레이어가 탈것에서 내리는 이벤트를 감지합니다.
     */
    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getVehicle() instanceof EnderDragon && specialDragonIds.contains(event.getVehicle().getUniqueId())) {
            EnderDragon dragon = (EnderDragon) event.getVehicle();

            if (event.getExited() instanceof Player) {
                Player player = (Player) event.getExited();
                BukkitTask task = dragonControlTasks.remove(player.getUniqueId());
                if (task != null) {
                    task.cancel();
                }
            }

            dragon.remove();
            specialDragonIds.remove(dragon.getUniqueId());
        }
    }

    /**
     * [핵심 추가] 우승자가 탑승한 특별 드래곤으로부터 데미지를 받지 않도록 합니다.
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 공격자가 특별 드래곤인지 확인
        if (specialDragonIds.contains(event.getDamager().getUniqueId())) {
            EnderDragon dragon = (EnderDragon) event.getDamager();
            Entity damaged = event.getEntity();

            // 피격자가 드래곤에 탑승한 플레이어인지 확인
            if (!dragon.getPassengers().isEmpty() && dragon.getPassengers().get(0).equals(damaged)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 모든 특별 드래곤과 관련 태스크를 정리합니다.
     */
    private void cleanupControllableDragons() {
        dragonControlTasks.values().forEach(BukkitTask::cancel);
        dragonControlTasks.clear();

        for (UUID dragonId : new HashSet<>(specialDragonIds)) {
            Entity dragon = Bukkit.getEntity(dragonId);
            if (dragon != null) {
                dragon.remove();
            }
        }
        specialDragonIds.clear();
    }

    /**
     * 플러그인 비활성화 시 호출될 정리 메소드입니다.
     */
    public void shutdown() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        cleanupControllableDragons();
        save();
    }

    public String getStatus() {
        if (!isRunning) {
            return "§e타이머가 비활성화 상태입니다.";
        }
        long elapsedTicks = Bukkit.getWorlds().get(0).getFullTime() - startTick;
        long currentDay = (elapsedTicks / 24000L) + 1;
        return "§a타이머 실행 중. 현재 " + currentDay + "일차 입니다.";
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isRunning) {
            updateScoreboard(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearScoreboard(event.getPlayer());
    }
}