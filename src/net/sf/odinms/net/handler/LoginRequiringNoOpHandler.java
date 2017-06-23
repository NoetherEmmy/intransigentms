package net.sf.odinms.net.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public final class LoginRequiringNoOpHandler implements MaplePacketHandler {
    private static final LoginRequiringNoOpHandler instance = new LoginRequiringNoOpHandler();

    /** Singleton class */
    private LoginRequiringNoOpHandler() {
    }

    public static LoginRequiringNoOpHandler getInstance() {
        return instance;
    }

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
    }

    @Override
    public boolean validateState(final MapleClient c) {
        return c.isLoggedIn();
    }
}
