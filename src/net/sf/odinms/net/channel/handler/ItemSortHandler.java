package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ItemSortHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.readInt();
        final byte mode = slea.readByte();

        final MapleInventoryType pInvType = MapleInventoryType.getByType(mode);
        final MapleInventory pInv = c.getPlayer().getInventory(pInvType);

        for (;;) {
            final byte freeSlot = pInv.getNextFreeSlot();
            if (freeSlot != -1) {
                byte itemSlot = -1;
                for (byte i = (byte) (freeSlot + 1); i <= 100; ++i) {
                    if (pInv.getItem(i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot <= 100 && itemSlot > 0) {
                    MapleInventoryManipulator.move(c, pInvType, itemSlot, freeSlot);
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        c.getSession().write(MaplePacketCreator.finishedSort(mode));
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
