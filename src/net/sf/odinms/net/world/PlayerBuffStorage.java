package net.sf.odinms.net.world;

import net.sf.odinms.tools.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class PlayerBuffStorage implements Serializable {
    private final List<Pair<Integer, List<PlayerBuffValueHolder>>> buffs = new ArrayList<>();
    private final List<Pair<Integer, List<PlayerCoolDownValueHolder>>> coolDowns = new ArrayList<>();
    private final int id = (int) (Math.random() * 100);
    @SuppressWarnings("unused")

    public PlayerBuffStorage() {
        // Empty constructor
    }

    public void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) {
        for (Pair<Integer, List<PlayerBuffValueHolder>> stored : buffs) {
            if (stored.getLeft() == chrid) buffs.remove(stored);
        }
        buffs.add(new Pair<>(chrid, toStore));
    }

    public void addCooldownsToStorage(int chrid, List<PlayerCoolDownValueHolder> toStore) {
        for (Pair<Integer, List<PlayerCoolDownValueHolder>> stored : coolDowns) {
            if (stored.getLeft() == chrid) coolDowns.remove(stored);
        }
        coolDowns.add(new Pair<>(chrid, toStore));
    }

    public List<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) {
        List<PlayerBuffValueHolder> ret = null;
        Pair<Integer, List<PlayerBuffValueHolder>> stored;
        for (int i = 0; i < buffs.size(); ++i) {
            stored = buffs.get(i);
            if (stored.getLeft().equals(chrid)) {
                ret = stored.getRight();
                buffs.remove(stored);
            }
        }
        return ret;
    }

    public List<PlayerCoolDownValueHolder> getCooldownsFromStorage(int chrid) {
        List<PlayerCoolDownValueHolder> ret = null;
        Pair<Integer, List<PlayerCoolDownValueHolder>> stored;
        for (int i = 0; i < coolDowns.size(); ++i) {
            stored = coolDowns.get(i);
            if (stored.getLeft().equals(chrid)) {
                ret = stored.getRight();
                coolDowns.remove(stored);
            }
        }
        return ret;
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
