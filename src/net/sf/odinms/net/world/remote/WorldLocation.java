package net.sf.odinms.net.world.remote;

import java.io.Serializable;

public class WorldLocation implements Serializable {
    private static final long serialVersionUID = 2226165329466413678L;
    public final int map, channel;

    public WorldLocation(final int map, final int channel) {
        this.map = map;
        this.channel = channel;
    }
}
