package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class PetAutoPotHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final byte type = slea.readByte();
        slea.skip(12);
        final byte slot = slea.readByte();
        slea.readByte();
        final int itemId = slea.readInt();
        final IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (toUse != null && toUse.getQuantity() > 0) {
            if (toUse.getItemId() != itemId) {
                return;
            }
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer());
        }
    }
}
