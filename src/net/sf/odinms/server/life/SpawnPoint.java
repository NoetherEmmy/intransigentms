package net.sf.odinms.server.life;

import net.sf.odinms.server.maps.MapleMap;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SpawnPoint {
    private final MapleMonster monster;
    private final Point pos;
    private long nextPossibleSpawn;
    private final int mobTime;
    private final AtomicInteger spawnedMonsters = new AtomicInteger(0);

    /**
     * Whether the spawned monster is immobile
     */
    private final boolean immobile;

    public SpawnPoint(MapleMonster monster, Point pos, int mobTime) {
        super();
        this.monster = monster;
        this.pos = new Point(pos);
        this.mobTime = mobTime;
        this.immobile = !monster.isMobile();
        this.nextPossibleSpawn = System.currentTimeMillis();
    }

    public boolean shouldSpawn() {
        return shouldSpawn(System.currentTimeMillis());
    }

    // Intentionally package private
    boolean shouldSpawn(long now) {
        if (mobTime < 0) {
            return false;
        }
        // Regular spawnpoints should spawn a maximum of 3 monsters: these are immobile spawnpoints
        // or spawnpoints with a maximum mobtime of 1
        if (((mobTime != 0 || immobile) && spawnedMonsters.get() > 0) || spawnedMonsters.get() > 2) {
            return false;
        }
        return nextPossibleSpawn <= now;
    }

    /**
     * Spawns the monster for this spawnpoint. Creates a new MapleMonster instance for that and returns it.
     *
     * @param mapleMap
     * @return
     */
    public MapleMonster spawnMonster(MapleMap mapleMap) {
        MapleMonster mob = new MapleMonster(monster);
        mob.setPosition(new Point(pos));
        spawnedMonsters.incrementAndGet();
        mob.addListener(monster1 -> {
            nextPossibleSpawn = System.currentTimeMillis();
            if (mobTime > 0) {
                nextPossibleSpawn += mobTime * 1000;
            } else {
                nextPossibleSpawn += monster1.getAnimationTime("die1");
            }
            spawnedMonsters.decrementAndGet();
        });
        mapleMap.spawnMonster(mob);
        if (mobTime == 0) {
            nextPossibleSpawn = System.currentTimeMillis() + 5000;
        }

        // The conditional below is for events with monsters that spawn on all maps.
        if (Math.random() < 0.009d) {
            mapleMap.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9500196), pos);
        }

        return mob;
    }
}
