package net.sf.odinms.server.maps;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;

import java.rmi.RemoteException;
import java.util.List;

public class MapleTVEffect {
    public static MaplePacket packet;
    public static boolean active;
    private final ChannelServer cserv;

    public MapleTVEffect(final MapleCharacter user, final MapleCharacter partner, final List<String> msg, final int type) {
        cserv = user.getClient().getChannelServer();
        packet = MaplePacketCreator.sendTV(user, msg, type <= 2 ? type : type - 3, partner);
        broadCastTV(true);
        scheduleCancel(type);
    }

    private void broadCastTV(final boolean active) {
        MapleTVEffect.active = active;
        try {
            if (active) {
                cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.enableTV().getBytes());
                cserv.getWorldInterface().broadcastMessage(null, packet.getBytes());
            } else {
                cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.removeTV().getBytes());
                packet = null;
            }
        } catch (final RemoteException noob) {
            cserv.reconnectWorld();
        }
    }

    public static int getMapleTVDuration(final int type) {
        switch (type) {
            case 1:
            case 4:
                return 30000;
            case 2:
            case 5:
                return 60000;
            default:
                return 15000;
        }
    }

    private void scheduleCancel(final int type) {
        TimerManager.getInstance().schedule(() -> broadCastTV(false), getMapleTVDuration(type));
    }
}
