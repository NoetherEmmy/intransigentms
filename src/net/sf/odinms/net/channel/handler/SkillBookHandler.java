package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.Map;

public class SkillBookHandler extends AbstractMaplePacketHandler {
    public SkillBookHandler() {
    }

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        player.resetAfkTime();
        if (!player.isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        IItem toUse = player.getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() == 1) {
            if (toUse.getItemId() != itemId) return;
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            Map<String, Integer> skilldata = ii.getSkillStats(toUse.getItemId(), player.getJob().getId());
            boolean canuse;
            boolean success = false;
            int skill = 0;
            int maxlevel = 0;
            if (skilldata == null) { // Hacking or used an unknown item
                return;
            }
            if (skilldata.get("skillid") == 0) { // Wrong job
                canuse = false;
            } else if (player.getMasterLevel(SkillFactory.getSkill(skilldata.get("skillid"))) >= skilldata.get("reqSkillLevel") || skilldata.get("reqSkillLevel") == 0) {
                canuse = true;
                int random = (int) Math.floor(Math.random() * 100.0d) + 1;
                if (random <= skilldata.get("success") && skilldata.get("success") != 0) {
                    success = true;
                    ISkill skill2 = SkillFactory.getSkill(skilldata.get("skillid"));
                    int curlevel = player.getSkillLevel(skill2);
                    if (skilldata.get("masterLevel") > player.getMasterLevel(skill2)) {
                        player.changeSkillLevel(skill2, curlevel, skilldata.get("masterLevel"));
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                    } else {
                        canuse = false;
                        success = false;
                    }
                } else {
                    success = false;
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                }
            } else { // Failed to meet skill requirements.
                canuse = false;
            }
            player.getClient().getSession().write(MaplePacketCreator.skillBookSuccess(player, skill, maxlevel, canuse, success));
        }
    }
}
