package net.sf.odinms.net;

import net.sf.odinms.client.MapleClient;

public abstract class AbstractMaplePacketHandler implements MaplePacketHandler {
    @Override
    public boolean validateState(final MapleClient c) {
        return c.isLoggedIn();
    }
}
