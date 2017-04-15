package net.sf.odinms.net.world;

import net.sf.odinms.server.MapleStatEffect;

import java.io.Serializable;

public class PlayerBuffValueHolder implements Serializable {
    static final long serialVersionUID = 9179541993413738569L;
    public final long startTime;
    public final MapleStatEffect effect;
    private final int id;

    public PlayerBuffValueHolder(final long startTime, final MapleStatEffect effect) {
        this.startTime = startTime;
        this.effect = effect;
        this.id = (int) (Math.random() * 100);
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
        final PlayerBuffValueHolder other = (PlayerBuffValueHolder) obj;
        return id == other.id;
    }
}
