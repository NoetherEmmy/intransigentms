package net.sf.odinms.server.maps;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.MaplePacketCreator;

public class MapleMapEffect {
    private final String msg;
    private final int itemId;
    private boolean active = true;

    public MapleMapEffect(final String msg, final int itemId) {
        this.msg = msg;
        this.itemId = itemId;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public MaplePacket makeDestroyData() {
        return MaplePacketCreator.removeMapEffect();
    }

    public MaplePacket makeStartData() {
        return MaplePacketCreator.startMapEffect(msg, itemId, active);
    }

    public void sendStartData(final MapleClient client) {
        client.getSession().write(makeStartData());
    }
}
