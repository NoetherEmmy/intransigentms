package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class HealOvertimeHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter p = c.getPlayer();
        slea.readByte();
        slea.readShort();
        slea.readByte();
        int healHP = slea.readShort();
        if (healHP != 0) {
            if (p.isDead()) return;
            if (healHP > 150 && p.getChair() < 1) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (healHP > 5000) {
                AutobanManager
                    .getInstance()
                    .autoban(
                        p.getClient(),
                        p.getName() +
                            " healed for " +
                            healHP +
                            "/HP in map: " +
                            p.getMapId() +
                            "."
                    );
                return;
            }
            p.addHP(healHP);
        }
        int healMP = slea.readShort();
        if (healMP != 0) {
            if (p.isDead()) return;
            if (healMP > 200 && p.getChair() < 1) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (healMP > 5000) {
                AutobanManager
                    .getInstance()
                    .autoban(
                        p.getClient(),
                        p.getName() +
                            " healed for " +
                            healMP +
                            "/MP in map: " +
                            p.getMapId() +
                            "."
                    );
                return;
            }
            p.addMP(healMP);
        }
    }
}
