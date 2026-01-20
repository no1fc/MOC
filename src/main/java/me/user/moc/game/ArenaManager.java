package me.user.moc.game;

import me.user.moc.MocPlugin;
import me.user.moc.config.ConfigManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class ArenaManager {
    private final MocPlugin plugin;
    private final ConfigManager config = ConfigManager.getInstance(); // 콘피그 싱글톤
    private Location gameCenter;
    private BukkitTask borderShrinkTask;
    private BukkitTask borderDamageTask;

    public ArenaManager(MocPlugin plugin) {
        this.plugin = plugin;
    }

    public Location getGameCenter() {
        return gameCenter;
    }

    public void setGameCenter(Location center) {
        this.gameCenter = center;
    }

    /**
     * 경기장 바닥을 생성하고 플레이어들을 텔레포트시킵니다.
     *
     * @param center  경기장 중심
     * @param radius  반지름
     * @param targetY 바닥 높이
     */
    public void generateCircleFloor(Location center, int radius, int targetY) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        long radiusSq = (long) radius * radius;

        // 텔레포트 목적지 (중심)
        Location teleportDest = center.clone();

        new BukkitRunnable() {
            int x = cx - radius;

            @Override
            public void run() {
                for (int i = 0; i < 20; i++) {
                    if (x > cx + radius) {
                        // 생성 완료 후 아이템 제거 및 플레이어 텔레포트
                        world.getEntitiesByClass(Item.class).forEach(Entity::remove);
                        Bukkit.getOnlinePlayers().forEach(p -> p.teleport(teleportDest.clone().add(0.5, 1.0, 0.5)));
                        this.cancel();
                        return;
                    }
                    long dx = (long) (x - cx) * (x - cx);
                    for (int z = cz - radius; z <= cz + radius; z++) {
                        if (dx + (long) (z - cz) * (z - cz) <= radiusSq) {
                            for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                                Block b = world.getBlockAt(x, y, z);
                                if (y == targetY) {
                                    // 텔레포트 위치의 바로 아래(바닥)은 비우지 않음 (혹시 모를 안전장치)
                                    // 원본 코드에서도 비슷하게 동작했으나, 여기서는 단순화하여 바닥 생성
                                    b.setType(Material.BEDROCK, false);
                                } else if (b.getType() != Material.AIR) {
                                    b.setType(Material.AIR, false);
                                }
                            }
                        }
                    }
                    x++;
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /**
     * 자기장 축소를 시작합니다.
     */
    /**
     * 자기장 축소를 시작합니다.
     * 주기적으로 방벽의 크기를 줄여서 플레이어들을 중앙으로 몰아넣습니다.
     */
    public void startBorderShrink() {
        // 1. 게임의 중심 좌표(gameCenter)가 설정되어 있지 않으면 실행하지 않고 돌아갑니다.
        if (gameCenter == null) return;

        // 2. 게임이 진행 중인 월드(세계) 정보를 가져옵니다.
        World world = gameCenter.getWorld();

        // 3. 주기적으로 실행될 '반복 작업'을 만듭니다. (컴퓨터에게 시키는 알람 같은 것)
        borderShrinkTask = new BukkitRunnable() {
            @Override
            public void run() {
//                // 4. 현재 이 월드에 설정된 장벽(방벽)의 실제 크기를 가져옵니다.
//                double size = world.getWorldBorder().getSize();
//                // 5. 만약 장벽 크기가 1칸 이하로 줄어들었다면, 더 이상 줄일 수 없으므로 작업을 멈춥니다.
//                if (size <= 1) {
//                    this.cancel(); // 반복 작업을 종료합니다.
//                    return;
//                }
//                // 6. [핵심 로직] 장벽의 크기를 현재보다 2칸 줄입니다.
//                // 첫 번째 인자(size - 2) : 변경될 목표 크기 (현재 크기에서 2만큼 뺀 값)
//                // 두 번째 인자(1) : 크기가 줄어드는 데 걸리는 시간 (1초 동안 서서히 줄어듦)
//                world.getWorldBorder().setSize(size - 2, 1);

                // ArenaManager에게 자기장 수축 명령
                if (config.spawn_point != null) {
                    // 5초에 걸쳐 줄어들거나 천천히 줄어들게 설정
                    // 여기서는 예시로 월드보더를 사용
                    WorldBorder wb = config.spawn_point.getWorld().getWorldBorder();
                    wb.setCenter(config.spawn_point);
                    wb.setSize(config.map_size); // 초기 크기
                    // [수정된 코드] 다시 long 타입을 사용합니다.
                    // 120L은 120초를 의미합니다.
                    wb.setSize(10, 120); // 120초 동안 10칸 크기로 서서히 줄어듭니다.
                }
            }

            // 7. 이 작업을 언제 얼마나 자주 실행할지 정합니다.
            // runTaskTimer(플러그인, 시작지연시간, 반복주기)
            // 0 : 명령을 내리자마자 바로 시작합니다.
            // 10초 : ( 마인크래프트 시간으로 20틱 = 1초)마다 위 작업을 반복합니다.
        }.runTaskTimer(plugin, 0, 200L);
    }


    /**
     * 자기장 밖 대미지 처리를 시작합니다.
     */
    public void startBorderDamage() {
        if (gameCenter == null) return;
        World world = gameCenter.getWorld();

        borderDamageTask = new BukkitRunnable() {
            @Override
            public void run() {
                WorldBorder b = world.getWorldBorder();
                double s = b.getSize() / 2.0;
                Location c = b.getCenter();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(world)) {
                        Location l = p.getLocation();
                        if (Math.abs(l.getX() - c.getX()) > s || Math.abs(l.getZ() - c.getZ()) > s) {
                            p.damage(6.0);
                            p.sendMessage("§c§l[자기장 밖 대미지]");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20L);
    }

    // 장벽 멈추기.
    public void stopTasks() {
        if (borderShrinkTask != null && !borderShrinkTask.isCancelled()) borderShrinkTask.cancel();
        if (borderDamageTask != null && !borderDamageTask.isCancelled()) borderDamageTask.cancel();
        config.spawn_point.getWorld().getWorldBorder().setSize(30000000);
    }
}
