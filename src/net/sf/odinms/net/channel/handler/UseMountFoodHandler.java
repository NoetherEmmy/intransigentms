package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ExpTable;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class UseMountFoodHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter p = c.getPlayer();
        p.resetAfkTime();
        slea.readInt();
        slea.readShort();
        int itemId = slea.readInt();

        if (p.getInventory(MapleInventoryType.USE).findById(itemId) != null) {
            if (p.getMount() != null) {
                p.getMount().setTiredness(p.getMount().getTiredness() - 30);
                p.getMount().setExp((int) ((Math.random() * 26) + 12) + p.getMount().getExp());
                int level = p.getMount().getLevel();
                boolean levelup =
                    p.getMount().getExp() >= ExpTable.getMountExpNeededForLevel(level) &&
                    level < 31 && p.getMount().getTiredness() != 0;
                if (levelup) {
                    p.getMount().setLevel(level + 1);
                }
                p.getMap().broadcastMessage(MaplePacketCreator.updateMount(p.getId(), p.getMount(), levelup));
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemId, 1, true, false);
            } else {
                p.dropMessage(5, "Please get on your mount first before using the mount food.");
            }
        }
    }
}
