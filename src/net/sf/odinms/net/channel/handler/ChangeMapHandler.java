package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;

public class ChangeMapHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        player.setLastKillOnMap(0L);
        player.setLastDamageSource(null);
        player.resetAfkTime();
        player.setInvincible(false);
        if (player.cancelBossHpTask()) {
            player.dropMessage("@bosshp display has been stopped.");
        }
        if (slea.available() == 0) {
            final int channel = c.getChannel();
            final String ip = ChannelServer.getInstance(c.getChannel()).getIP(channel);
            final String[] socket = ip.split(":");
            if (player.inCS() || player.inMTS()) {
                player.saveToDB(true, true);
                player.setInCS(false);
                player.setInMTS(false);
            } else {
                player.saveToDB(true, false);
            }
            ChannelServer.getInstance(c.getChannel()).removePlayer(player);
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
            try {
                final MaplePacket packet = MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]));
                c.getSession().write(packet);
                c.getSession().close();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            slea.readByte();
            final int targetid = slea.readInt();
            final String startwp = slea.readMapleAsciiString();
            final MaplePortal portal = player.getMap().getPortal(startwp);
            if (player.getBuffedValue(MapleBuffStat.MORPH) != null && player.getBuffedValue(MapleBuffStat.COMBO) != null) {
                player.cancelEffectFromBuffStat(MapleBuffStat.MORPH);
                player.cancelEffectFromBuffStat(MapleBuffStat.COMBO);
            }
            if (player.getBuffedValue(MapleBuffStat.PUPPET) != null) {
                player.cancelBuffStats(MapleBuffStat.PUPPET);
            }
            if (targetid != -1 && !player.isAlive()) {
                /*
                boolean executeStandardPath = true;
                if (player.getEventInstance() != null) {
                    executeStandardPath = player.getEventInstance().revivePlayer(player);
                }
                if (executeStandardPath) {
                    player.cancelAllBuffs();
                    player.setHp(50);
                    MapleMap to = player.getMap().getReturnMap();
                    MaplePortal pto = to.getPortal(0);
                    player.setStance(0);
                    player.changeMap(to, pto);
                }
                */
                player.permadeath();
            } else if (targetid != -1 && player.isGM()) {
                final MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
                final MaplePortal pto = to.getPortal(0);
                player.changeMap(to, pto);
            } else if (targetid != -1 && !player.isGM()) {
                System.err.println("Player " + player.getName() + " attempted map jumping without being a GM.");
            } else {
                if (portal != null) {
                    portal.enterPortal(c);
                    /*
                    if (player.getClan() == -1 && !c.isGuest()) {
                        for (int i = 0; i < 3; ++i) {
                            player.dropMessage(6, "You have yet to join a clan! Type @clan to join a clan.");
                        }
                    }
                    */
                } else {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    System.err.println("Portal " + startwp + " not found on map " + player.getMap().getId());
                }
            }
        }
    }
}
