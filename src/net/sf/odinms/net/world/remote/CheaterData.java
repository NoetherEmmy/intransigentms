package net.sf.odinms.net.world.remote;

import java.io.Serializable;

public class CheaterData implements Serializable, Comparable<CheaterData> {
    private static final long serialVersionUID = -8733673311051249885L;
    private final int points;
    private final String info;

    public CheaterData(final int points, final String info) {
        this.points = points;
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    public int getPoints() {
        return points;
    }

    public int compareTo(final CheaterData o) {
        final int thisVal = points;
        final int anotherVal = o.getPoints();
        return (thisVal<anotherVal ? 1 : (thisVal==anotherVal ? 0 : -1));
    }
}
