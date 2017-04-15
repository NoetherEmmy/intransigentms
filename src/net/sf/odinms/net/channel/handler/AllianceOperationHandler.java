package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.world.guild.MapleAlliance;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.util.stream.IntStream;

public class AllianceOperationHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        //System.out.println(slea.toString());

        MapleAlliance alliance = null;
        if (c.getPlayer().getGuild() != null && c.getPlayer().getGuild().getAllianceId() > 0) {
            try {
                alliance = c.getChannelServer().getWorldInterface().getAlliance(c.getPlayer().getGuild().getAllianceId());
            } catch (final RemoteException re) {
                c.getChannelServer().reconnectWorld();
            }
        }
        if (alliance == null) {
            c.getPlayer().dropMessage("System error!");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        } else if (c.getPlayer().getMGC().getAllianceRank() > 2 || !alliance.getGuilds().contains(c.getPlayer().getGuildId())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        try {
            switch (slea.readByte()) {
                case 0x0A:
                    final String notice = slea.readMapleAsciiString();
                    c.getChannelServer().getWorldInterface().setAllianceNotice(alliance.getId(), notice);
                    c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), MaplePacketCreator.allianceNotice(alliance.getId(), notice), -1, -1);
                    break;
                case 0x08:
                    final String[] ranks =
                        IntStream
                            .range(0, 5)
                            .mapToObj(i -> slea.readMapleAsciiString())
                            .toArray(String[]::new);
                    c.getChannelServer().getWorldInterface().setAllianceRanks(alliance.getId(), ranks);
                    c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), MaplePacketCreator.changeAllianceRankTitle(alliance.getId(), ranks), -1, -1);
                    break;
                case 0x03: // fall through to default.
                /*
                String charName = slea.readMapleAsciiString();
                int channel = c.getChannelServer().getWorldInterface().find(charName);
                if (channel == -1) {
                c.getPlayer().dropMessage("The c.getPlayer() is not online");
                } else {
                MapleCharacter victim = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(charName);
                if (victim.getGuildId() == 0) {
                c.getPlayer().dropMessage("c.getPlayer() does not have a guild");
                } else if (victim.getGuildRank() != 1) {
                c.getPlayer().dropMessage("c.getPlayer() is not the leader of his/her guild.");
                }
                // UGH T___T No alliance packet.
                }*/
                default:
                    //c.getPlayer().dropMessage("Feature not available");
            }
            alliance.saveToDB();
        } catch (final RemoteException re) {
            c.getChannelServer().reconnectWorld();
        }
    }
}
