package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.Map;

public class SkillBookHandler extends AbstractMaplePacketHandler {
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
        if (toUse != null && toUse.getQuantity() >= 1) {
            if (toUse.getItemId() != itemId) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            Map<String, Integer> skillData = ii.getSkillStats(toUse.getItemId(), player.getJob().getId());
            boolean canUse;
            boolean success = false;
            int skill = 0;
            int maxlevel = 0;
            if (skillData == null) { // Hacking or used an unknown item
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (skillData.get("skillid") == 0) { // Wrong job
                canUse = false;
            } else if (
                player.getMasterLevel(
                    SkillFactory.getSkill(skillData.get("skillid"))
                ) >= skillData.get("reqSkillLevel") ||
                skillData.get("reqSkillLevel") == 0
            ) {
                canUse = true;
                int random = (int) Math.floor(Math.random() * 100.0d) + 1;
                if (random <= skillData.get("success") && skillData.get("success") != 0) {
                    success = true;
                    ISkill skill2 = SkillFactory.getSkill(skillData.get("skillid"));
                    int curlevel = player.getSkillLevel(skill2);
                    if (skillData.get("masterLevel") > player.getMasterLevel(skill2)) {
                        player.changeSkillLevel(skill2, curlevel, skillData.get("masterLevel"));
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                    } else {
                        canUse = false;
                        success = false;
                    }
                } else {
                    success = false;
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                }
            } else { // Failed to meet skill requirements.
                canUse = false;
            }
            player
                .getClient()
                .getSession()
                .write(
                    MaplePacketCreator.skillBookSuccess(
                        player,
                        skill,
                        maxlevel,
                        canUse,
                        success
                    )
                );
        }
    }
}
