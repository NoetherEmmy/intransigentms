package net.sf.odinms.net.login.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class CharSelectedHandler extends AbstractMaplePacketHandler {
    //private static final Logger log = LoggerFactory.getLogger(CharSelectedHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        //String channelHost = System.getProperty("net.sf.odinms.channelserver.host");
        final int charId = slea.readInt();
        final String macs;
        try {
            macs = slea.readMapleAsciiString();
        } catch (final ArrayIndexOutOfBoundsException aioobe) {
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
            final String channelServerIP =
                MapleClient
                    .getChannelServerIPFromSubnet(
                        c.getSession().getRemoteAddress().toString().replace("/", "").split(":")[0],
                        c.getChannel()
                    );
            if (channelServerIP.equals("0.0.0.0")) {
                final String[] socket =
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
                final String[] socket =
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
        } catch (final UnknownHostException uhe) {
            System.err.println("Host not found. ");
            uhe.printStackTrace();
        }
    }
}
