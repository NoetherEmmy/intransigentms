package net.sf.odinms.net.login.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ServerlistRequestHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getSession()
         .write(
             MaplePacketCreator.getServerList(
                 0,
                 LoginServer.getInstance().getServerName(),
                 LoginServer.getInstance().getLoad()
             )
         );
        if (LoginServer.getInstance().twoWorldsActive()) {
            c.getSession()
             .write(
                 MaplePacketCreator.getServerList(
                     1,
                     LoginServer.getInstance().getServerName(),
                     LoginServer.getInstance().getLoad()
                 )
             );
        }
        c.getSession().write(MaplePacketCreator.getEndOfServerList());
    }
}
