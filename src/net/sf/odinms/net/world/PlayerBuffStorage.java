package net.sf.odinms.net.world;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class PlayerBuffStorage implements Serializable {
    private static int runningId = (int) (Math.random() * 127.0d);
    private final Map<Integer, Set<PlayerBuffValueHolder>> buffs = new HashMap<>();
    private final Map<Integer, Set<PlayerCoolDownValueHolder>> coolDowns = new HashMap<>();
    private final int id;
    @SuppressWarnings("unused")

    public PlayerBuffStorage() {
        runningId += 7;
        id = runningId;
    }

    public void addBuffsToStorage(int cid, Set<PlayerBuffValueHolder> toStore) {
        buffs.put(cid, toStore);
    }

    public void addCooldownsToStorage(int cid, Set<PlayerCoolDownValueHolder> toStore) {
        coolDowns.put(cid, toStore);
    }

    public Set<PlayerBuffValueHolder> getBuffsFromStorage(int cid) {
        return buffs.remove(cid);
    }

    public Set<PlayerCoolDownValueHolder> getCooldownsFromStorage(int cid) {
        return coolDowns.remove(cid);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final PlayerBuffStorage other = (PlayerBuffStorage) obj;
        return id == other.id;
    }
}
