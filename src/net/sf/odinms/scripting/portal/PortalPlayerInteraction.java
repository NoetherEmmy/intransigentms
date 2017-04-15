package net.sf.odinms.scripting.portal;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.scripting.AbstractPlayerInteraction;
import net.sf.odinms.server.MaplePortal;

public class PortalPlayerInteraction extends AbstractPlayerInteraction {
    private final MaplePortal portal;

    public PortalPlayerInteraction(final MapleClient c, final MaplePortal portal) {
        super(c);
        this.portal = portal;
    }

    public MaplePortal getPortal() {
        return portal;
    }
}
