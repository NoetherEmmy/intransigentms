package net.sf.odinms.server.life;

import net.sf.odinms.server.maps.MapleMap;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SpawnPoint {
    private final MapleMonster monster;
    private final Point pos;
    private long nextPossibleSpawn;
    private final int mobTime;
    private final AtomicInteger spawnedMonsters = new AtomicInteger();

    /**
     * Whether the spawned monster is immobile or not
     */
    private final boolean immobile;

    public SpawnPoint(final MapleMonster monster, final Point pos, final int mobTime) {
        super();
        this.monster = monster;
        this.pos = new Point(pos);
        this.mobTime = mobTime;
        immobile = !monster.isMobile();
        nextPossibleSpawn = System.currentTimeMillis();
    }

    public boolean shouldSpawn() {
        return shouldSpawn(System.currentTimeMillis());
    }

    // Intentionally package private
    boolean shouldSpawn(final long now) {
        if (mobTime < 0) return false;
        // Regular spawnpoints should spawn a maximum of 3 monsters: these are immobile spawnpoints
        // or spawnpoints with a maximum mobtime of 1
        return !(((mobTime != 0 || immobile) && spawnedMonsters.get() > 0) || spawnedMonsters.get() > 2) &&
               nextPossibleSpawn <= now;
    }

    /**
     * Spawns the monster for this spawn point. Creates a new MapleMonster instance for that and returns it.
     */
    public MapleMonster spawnMonster(final MapleMap mapleMap) {
        final MapleMonster mob = new MapleMonster(monster);
        mob.setPosition(new Point(pos));
        spawnedMonsters.incrementAndGet();
        mob.addListener(monster1 -> {
            nextPossibleSpawn = System.currentTimeMillis();
            if (mobTime > 0) {
                nextPossibleSpawn += mobTime * 1000L;
            } else {
                nextPossibleSpawn += monster1.getAnimationTime("die1");
            }
            spawnedMonsters.decrementAndGet();
        });
        mapleMap.spawnMonster(mob);
        if (mobTime == 0) {
            nextPossibleSpawn = System.currentTimeMillis() + 5000L;
        }

        // The conditional below is for events with monsters that spawn on all maps.
        /*
        if (Math.random() < 0.02d) {
            mapleMap.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9500196), pos);
        }
        */
        return mob;
    }
}
