package net.sf.odinms.net.world;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerBuffStorage implements Serializable {
    private static final AtomicInteger runningId = new AtomicInteger((int) (Math.random() * 127.0d));
    private final Map<Integer, Set<PlayerBuffValueHolder>> buffs = new HashMap<>();
    private final Map<Integer, Set<PlayerCoolDownValueHolder>> coolDowns = new HashMap<>();
    private final int id;

    public PlayerBuffStorage() {
        id = runningId.addAndGet(7);
    }

    public void addBuffsToStorage(final int cid, final Set<PlayerBuffValueHolder> toStore) {
        buffs.put(cid, toStore);
    }

    public void addCooldownsToStorage(final int cid, final Set<PlayerCoolDownValueHolder> toStore) {
        coolDowns.put(cid, toStore);
    }

    public Set<PlayerBuffValueHolder> getBuffsFromStorage(final int cid) {
        return buffs.remove(cid);
    }

    public Set<PlayerCoolDownValueHolder> getCooldownsFromStorage(final int cid) {
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
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final PlayerBuffStorage other = (PlayerBuffStorage) obj;
        return id == other.id;
    }
}
