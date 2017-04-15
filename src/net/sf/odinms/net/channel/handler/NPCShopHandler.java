package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class NPCShopHandler extends AbstractMaplePacketHandler {
    /** Creates a new instance of NPCShopHandler */
    public NPCShopHandler() {
    }

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final byte bmode = slea.readByte();
        if (bmode == 0) { // Buy
            slea.readShort();
            final int itemId = slea.readInt();
            final short quantity = slea.readShort();
            c.getPlayer().getShop().buy(c, itemId, quantity);
        } else if (bmode == 1) { // Sell
            final byte slot = (byte) slea.readShort();
            final int itemId = slea.readInt();
            final MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemId);
            final short quantity = slea.readShort();
            c.getPlayer().getShop().sell(c, type, slot, quantity);
        } else if (bmode == 2) { // Recharge
            final byte slot = (byte) slea.readShort();
            c.getPlayer().getShop().recharge(c, slot);
        }
    }
}
