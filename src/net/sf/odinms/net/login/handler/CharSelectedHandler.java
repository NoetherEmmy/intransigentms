package net.sf.odinms.net.login.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CharSelectedHandler extends AbstractMaplePacketHandler {
    private static final Logger log = LoggerFactory.getLogger(CharSelectedHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //String channelHost = System.getProperty("net.sf.odinms.channelserver.host");
        int charId = slea.readInt();
        String macs;
        try {
            macs = slea.readMapleAsciiString();
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            System.err.println(
                c.getSession().getRemoteAddress() +
                    " sending bad packets at CharSelectedHandler#handlePacket"
            );
            return;
        }
        c.updateMacs(macs);
        if (c.hasBannedMac()) {
            c.getSession().close();
            return;
        }
        try {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            //c.getSession().write(MaplePacketCreator.getServerIP(InetAddress.getByName("127.0.0.1"), 7575, charId));
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
            String channelServerIP =
                MapleClient
                    .getChannelServerIPFromSubnet(
                        c.getSession().getRemoteAddress().toString().replace("/", "").split(":")[0],
                        c.getChannel()
                    );
            if (channelServerIP.equals("0.0.0.0")) {
                String[] socket =
                    LoginServer
                        .getInstance()
                        .getIP(c.getChannel())
                        .split(":");
                c.getSession()
                 .write(
                     MaplePacketCreator
                         .getServerIP(
                             InetAddress.getByName(socket[0]),
                             Integer.parseInt(socket[1]),
                             charId
                         )
                 );
            } else {
                String[] socket =
                    LoginServer
                        .getInstance()
                        .getIP(c.getChannel())
                        .split(":");
                c.getSession()
                 .write(
                     MaplePacketCreator
                         .getServerIP(
                             InetAddress.getByName(channelServerIP),
                             Integer.parseInt(socket[1]),
                             charId
                         )
                 );
            }
        } catch (UnknownHostException uhe) {
            log.error("Host not found. ", uhe);
        }
    }
}
