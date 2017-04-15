package net.sf.odinms.net.login.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public class PickCharHandler extends AbstractMaplePacketHandler {
    private static final Logger log = LoggerFactory.getLogger(PickCharHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int charId = slea.readInt();
        final int world = slea.readInt();
        c.setWorld(world);
        try {
            c.setChannel(new Random().nextInt(ChannelServer.getAllInstances().size()));
        } catch (final Exception e) {
            c.setChannel(1);
        }
        try {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);

            final String channelServerIP =
                MapleClient.getChannelServerIPFromSubnet(
                    c.getSession().getRemoteAddress().toString().replace("/", "").split(":")[0],
                    c.getChannel()
                );
            if (channelServerIP.equals("0.0.0.0")) {
                final String[] socket = LoginServer.getInstance().getIP(c.getChannel()).split(":");
                c.getSession()
                 .write(
                     MaplePacketCreator.getServerIP(
                         InetAddress.getByName(socket[0]),
                         Integer.parseInt(socket[1]),
                         charId
                     )
                 );
            } else {
                final String[] socket = LoginServer.getInstance().getIP(c.getChannel()).split(":");
                c.getSession()
                 .write(
                     MaplePacketCreator.getServerIP(
                         InetAddress.getByName(channelServerIP),
                         Integer.parseInt(socket[1]),
                         charId
                     )
                 );
            }
        } catch (final UnknownHostException uhe) {
            log.error("Host not found. ", uhe);
        }
    }
}
