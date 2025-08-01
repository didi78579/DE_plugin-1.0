package cjs.DE_plugin.dragon_egg.egg_footprint;

import org.bukkit.Location;
import java.util.UUID;

/**
 * 개별 발자국의 모든 정보를 저장하는 데이터 클래스
 */
public class Footprint {
    private final UUID frontEntityId;
    private final UUID backEntityId;
    private final UUID ownerId;
    private final Location location;
    private final long creationTime;

    public Footprint(UUID frontEntityId, UUID backEntityId, UUID ownerId, Location location, long creationTime) {
        this.frontEntityId = frontEntityId;
        this.backEntityId = backEntityId;
        this.ownerId = ownerId;
        this.location = location;
        this.creationTime = creationTime; // 불러온 생성 시간을 적용
    }

    public UUID getFrontEntityId() {
        return frontEntityId;
    }

    public UUID getBackEntityId() {
        return backEntityId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Location getLocation() {
        return location;
    }

    public long getCreationTime() {
        return creationTime;
    }
}