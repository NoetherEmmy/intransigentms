package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ItemMoveHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.readInt();
        final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
        final byte src = (byte) slea.readShort();
        final byte dst = (byte) slea.readShort();
        final long checkq = slea.readShort();
        final short quantity = (short) (int) checkq;
        if (src < 0 && dst > 0) {
            MapleInventoryManipulator.unequip(c, src, dst);
        } else if (dst < 0) {
            MapleInventoryManipulator.equip(c, src, dst);
        } else if (dst == 0) {
            if (c.getPlayer().getInventory(type).getItem(src) == null) {
                return;
            }
            if (checkq > 4000 || checkq < 1) {
                AutobanManager.getInstance().autoban(
                    c,
                    "Drop-dupe attempt, item: " +
                        c.getPlayer().getInventory(type).getItem(src).getItemId()
                );
                return;
            }
            MapleInventoryManipulator.drop(c, type, src, quantity);
        } else {
            MapleInventoryManipulator.move(c, type, src, dst);
        }
    }
}
