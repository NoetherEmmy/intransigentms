package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.MapleReactor;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ReactorHitHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        // 8C 00 <int Reactor unique ID> <character relative position> 00 00 00 <character stance?>
        final int oid = slea.readInt();
        final int charPos = slea.readInt();
        final short stance = slea.readShort();
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (reactor != null && reactor.isAlive()) {
            reactor.hitReactor(charPos, stance, c);
        } else { // Player hit a destroyed reactor, likely due to lag
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }
}
