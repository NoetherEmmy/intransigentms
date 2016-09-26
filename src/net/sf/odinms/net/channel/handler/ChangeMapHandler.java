package net.sf.odinms.net.channel.handler;

import java.net.InetAddress;
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

public class ChangeMapHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().setLastKillOnMap((long) 0);
        c.getPlayer().setLastDamageSource(null);
        c.getPlayer().resetAfkTime();
        if (slea.available() == 0) {
            int channel = c.getChannel();
            String ip = ChannelServer.getInstance(c.getChannel()).getIP(channel);
            String[] socket = ip.split(":");
            if (c.getPlayer().inCS() || c.getPlayer().inMTS()) {
                c.getPlayer().saveToDB(true, true);
                c.getPlayer().setInCS(false);
                c.getPlayer().setInMTS(false);
            } else {
                c.getPlayer().saveToDB(true, false);
            }
            ChannelServer.getInstance(c.getChannel()).removePlayer(c.getPlayer());
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
            try {
                MaplePacket packet = MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]));
                c.getSession().write(packet);
                c.getSession().close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            slea.readByte();
            int targetid = slea.readInt();
            String startwp = slea.readMapleAsciiString();
            MaplePortal portal = c.getPlayer().getMap().getPortal(startwp);
            MapleCharacter player = c.getPlayer();
            if (player.getBuffedValue(MapleBuffStat.MORPH) != null && player.getBuffedValue(MapleBuffStat.COMBO) != null) {
                player.cancelEffectFromBuffStat(MapleBuffStat.MORPH);
                player.cancelEffectFromBuffStat(MapleBuffStat.COMBO);
            }
            if (player.getBuffedValue(MapleBuffStat.PUPPET) != null) {
                player.cancelBuffStats(MapleBuffStat.PUPPET);
            }
            if (targetid != -1 && !c.getPlayer().isAlive()) {
                boolean executeStandardPath = true;
                if (player.getEventInstance() != null) {
                    executeStandardPath = player.getEventInstance().revivePlayer(player);
                }
                if (executeStandardPath) {
                    /*
                    player.cancelAllBuffs();
                    player.setHp(50);
                    MapleMap to = player.getMap().getReturnMap();
                    MaplePortal pto = to.getPortal(0);
                    player.setStance(0);
                    player.changeMap(to, pto);
                    */
                    player.permadeath();
                }
            } else if (targetid != -1 && player.isGM()) {
                MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
                MaplePortal pto = to.getPortal(0);
                player.changeMap(to, pto);
            } else if (targetid != -1 && !player.isGM()) {
                System.out.println("Player " + player.getName() + " attempted Map jumping without being a GM");
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
                    System.out.println("Portal " + startwp + " not found on map " + player.getMap().getId());
                }
            }
        }
    }
}