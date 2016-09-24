package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class HealOvertimeHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        slea.readShort();
        slea.readByte();
        int healHP = slea.readShort();
        if (healHP != 0) {
            if (healHP > 1000) {
                AutobanManager.getInstance().autoban
                (c.getPlayer().getClient(), "XSource| " + c.getPlayer().getName() + " healed for " + healHP + "/HP in map: " + c.getPlayer().getMapId() + ".");
                return;
            }
            c.getPlayer().addHP(healHP);
        }
        int healMP = slea.readShort();
        if (healMP != 0) {
            if (healMP > 1000) {
                AutobanManager.getInstance().autoban
                (c.getPlayer().getClient(), "XSource| " + c.getPlayer().getName() + " healed for " + healMP + "/MP in map: " + c.getPlayer().getMapId() + ".");
                return;
            }
            c.getPlayer().addMP(healMP);
        }
    }
}