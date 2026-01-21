// 파일 경로: src/main/java/me/user/moc/game/ClearManager.java
package me.user.moc.game;

import me.user.moc.MocPlugin;
import me.user.moc.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

/**
 * ClearManager: 전장에 떨어진 아이템이나 소환된 몬스터들을 싹 치워주는 '청소 담당자' 클래스입니다.
 */
public class ClearManager {
    private final ConfigManager config = ConfigManager.getInstance(); // 설정 파일(config.yml) 정보를 가져옵니다.

    public ClearManager(MocPlugin plugin) {
    }

    /**
     * 월드 내의 아이템과 생명체(플레이어 제외)를 전부 제거하는 핵심 청소 기능입니다.
     */
    public void allCear() {
        // 1. 청소할 기준점을 정합니다. 설정된 스폰 지점을 먼저 찾습니다.
        Location center = config.spawn_point;

        // 만약 스폰 지점이 설정 안 되어 있다면, 현재 접속 중인 플레이어 중 한 명의 위치를 기준점으로 삼습니다.
        if (center == null)
            center = Bukkit.getOnlinePlayers().iterator().next().getLocation();

        // 2. 기준점이 속한 '월드(세계)' 정보를 가져옵니다.
        World world = center.getWorld();

        // 만약 월드 정보를 찾을 수 없다면 청소를 중단합니다.
        if (world == null) return;

        // 3. [아이템 청소] 바닥에 떨어져 있는 모든 아이템(Item 클래스)을 찾아서 제거합니다.
        world.getEntitiesByClass(Item.class).forEach(Entity::remove);

        // 4. [생명체 청소] 월드 내의 모든 살아있는 생명체(LivingEntity)를 하나씩 검사합니다.
        world.getLivingEntities().forEach(entity -> {

            // 5. [필터링] 만약 이 생명체가 '플레이어'가 아니라면 삭제합니다.
            // (사람까지 지워지면 안 되니까 "사람이 아닐 때만 삭제해!"라고 명령하는 것입니다.)
            if (!(entity instanceof org.bukkit.entity.Player)) {
                entity.remove(); // 좀비, 스켈레톤, 동물 등을 삭제함
            }
        });
    }
}