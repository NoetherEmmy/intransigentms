package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class useSolomonHandler extends AbstractMaplePacketHandler {
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        slea.readInt();
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final IItem item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (item == null || item.getItemId() != itemid) {
            c.disconnect();
            return;
        }
        c.getPlayer().gainExp(ii.getExpCache(itemid), true, false);
        c.getSession().write(MaplePacketCreator.enableActions());
        MapleInventoryManipulator.removeFromSlot(
            c,
            MapleInventoryType.USE,
            slot,
            (short) 1,
            false
        );
    }
}
