package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ChangeMapSpecialHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        try {
            c.getPlayer().setLastKillOnMap(0L);
            c.getPlayer().setLastDamageSource(null);
            c.getPlayer().resetAfkTime();
            slea.readByte();
            final String startwp = slea.readMapleAsciiString();
            slea.readByte();
            //byte sourcefm = slea.readByte();
            slea.readByte();
            final MapleCharacter player = c.getPlayer();
            if (player.getBuffedValue(MapleBuffStat.MORPH) != null && player.getBuffedValue(MapleBuffStat.COMBO) != null) {
                player.cancelEffectFromBuffStat(MapleBuffStat.MORPH);
                player.cancelEffectFromBuffStat(MapleBuffStat.COMBO);
            }
            if (player.getBuffedValue(MapleBuffStat.PUPPET) != null) {
                player.cancelBuffStats(MapleBuffStat.PUPPET);
            }
            final MaplePortal portal = c.getPlayer().getMap().getPortal(startwp);
            if (portal != null) {
                if (c.getPlayer().getMapId() == 222020200 && "elevator".equals(portal.getScriptName())) {
                    c.getPlayer().changeMap(222020100);
                } else if (c.getPlayer().getMapId() == 222020100 && "elevator".equals(portal.getScriptName())) {
                    c.getPlayer().changeMap(222020200);
                } else {
                    portal.enterPortal(c);
                }
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
