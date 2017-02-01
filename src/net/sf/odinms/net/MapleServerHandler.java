package net.sf.odinms.net;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.login.LoginWorker;
import net.sf.odinms.tools.MapleAESOFB;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.ByteArrayByteStream;
import net.sf.odinms.tools.data.input.GenericSeekableLittleEndianAccessor;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

public class MapleServerHandler extends IoHandlerAdapter {
    private static final short MAPLE_VERSION = 62;
    private final PacketProcessor processor;
    private int channel = -1;

    public MapleServerHandler(PacketProcessor processor) {
        this.processor = processor;
    }

    public MapleServerHandler(PacketProcessor processor, int channel) {
        this.processor = processor;
        this.channel = channel;
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        Runnable r = ((MaplePacket) message).getOnSend();
        if (r != null) {
            r.run();
        }
        super.messageSent(session, message);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        //cause.printStackTrace();
        /*
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        log.error(MapleClient.getLogMessage(client, cause.getMessage()), cause);
        ChannelServer.getInstance(1).broadcastPacket(MaplePacketCreator.getChatText(30000, "Exception: " + cause.getClass().getName() + ": " + cause.getMessage()));
        for (int i = 0; i < cause.getStackTrace().length; ++i) {
            StackTraceElement ste = cause.getStackTrace()[i];
            ChannelServer.getInstance(1).broadcastPacket(MaplePacketCreator.getChatText(30000, ste.toString()));
            if (i > 2) {
                break;
            }
        }
        */
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        //log.info("IoSession with {} opened", session.getRemoteAddress());
        if (channel > -1) {
            if (ChannelServer.getInstance(channel).isShutdown()) {
                session.close();
                return;
            }
        }
        byte key[] = {
            0x13, 0x00, 0x00, 0x00,
            0x08, 0x00, 0x00, 0x00,
            0x06, 0x00, 0x00, 0x00,
            (byte) 0xB4, 0x00, 0x00,
            0x00, 0x1B, 0x00, 0x00,
            0x00, 0x0F, 0x00, 0x00,
            0x00, 0x33, 0x00, 0x00,
            0x00, 0x52, 0x00, 0x00,
            0x00
        };
        byte ivRecv[] = {70, 114, 122, 82};
        byte ivSend[] = {82, 48, 120, 115};
        ivRecv[3] = (byte) (Math.random() * 255);
        ivSend[3] = (byte) (Math.random() * 255);
        MapleAESOFB sendCypher = new MapleAESOFB(key, ivSend, (short) (0xFFFF - MAPLE_VERSION));
        MapleAESOFB recvCypher = new MapleAESOFB(key, ivRecv, MAPLE_VERSION);
        MapleClient client = new MapleClient(sendCypher, recvCypher, session);
        client.setChannel(channel);
        session.write(MaplePacketCreator.getHello(MAPLE_VERSION, ivSend, ivRecv, false));
        session.setAttribute(MapleClient.CLIENT_KEY, client);
        session.setIdleTime(IdleStatus.READER_IDLE, 30);
        session.setIdleTime(IdleStatus.WRITER_IDLE, 30);
    }

    @Override
    public void sessionClosed(final IoSession session) throws Exception {
        synchronized (session) {
            MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
            if (client != null) {
                client.disconnect();
                LoginWorker.getInstance().deregisterClient(client);
                session.removeAttribute(MapleClient.CLIENT_KEY);
            }
        }
        super.sessionClosed(session);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        byte[] content = (byte[]) message;
        SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream(content));
        short packetId = slea.readShort();
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        MaplePacketHandler packetHandler = processor.getHandler(packetId);
        //
        client.pongReceived();
        //
        //if (log.isTraceEnabled() || log.isInfoEnabled()) {
            //String from = "";
            //if (client.getPlayer() != null) {
                //from = "from " + client.getPlayer().getName() + " ";
            //}
            //if (packetHandler == null) {
                //log.info("Got unhandled Message {} ({}) {}\n{}", new Object[]{from, content.length, HexTool.toString(content), HexTool.toStringFromAscii(content)});
            //}
        //}
        if (packetHandler != null && packetHandler.validateState(client)) {
            try {
                //if (trace) {
                    //String from = "";
                    //if (client.getPlayer() != null) {
                        //from = "from " + client.getPlayer().getName() + " ";
                    //}
                //log.info("Got Message {}handled by {} ({}) {}\n{}", new Object[] { from, packetHandler.getClass().getSimpleName(), content.length, HexTool.toString(content), HexTool.toStringFromAscii(content) });
                //}
                packetHandler.handlePacket(slea, client);
            } catch (Throwable t) {
                if (!packetHandler.getClass().getName().contains("ItemPickupHandler")) {
                    System.err.println(
                        "Exception during processing packet: " +
                            packetHandler.getClass().getName() +
                            ":"
                    );
                    t.printStackTrace();
                }
            }
        }
    }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        /*
        if (client != null && client.getPlayer() != null) {
            System.out.println("Player " + client.getPlayer().getName() + " went idle.");
        }
        */
        if (client != null) {
            client.sendPing();
        }
        super.sessionIdle(session, status);
    }
}
