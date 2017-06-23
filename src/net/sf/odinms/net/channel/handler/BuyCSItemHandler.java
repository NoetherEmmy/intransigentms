package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.CashItemFactory;
import net.sf.odinms.server.CashItemInfo;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BuyCSItemHandler extends AbstractMaplePacketHandler {
    private void updateInformation(final MapleClient c, final int item) {
        final CashItemInfo Item = CashItemFactory.getItem(item);
        c.getSession().write(MaplePacketCreator.showBoughtCSItem(Item.getId()));
        updateInformation(c);
    }

    private void updateInformation(final MapleClient c) {
        c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
        c.getSession().write(MaplePacketCreator.enableCSUse0());
        c.getSession().write(MaplePacketCreator.enableCSUse1());
        c.getSession().write(MaplePacketCreator.enableCSUse2());
        c.getSession().write(MaplePacketCreator.enableCSUse3());
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int action = slea.readByte();
        if (action == 3) {
            slea.skip(1);
            final int useNX = slea.readInt();
            final int snCS = slea.readInt();
            final CashItemInfo item = CashItemFactory.getItem(snCS);
            final int itemID = item.getId();
            if (itemID == 5120015 || (itemID >= 5390000 && itemID <= 5430000) || itemID == 1812002 || itemID == 1812003 || (itemID >= 5030000 && itemID <= 5076000 && itemID != 5060000) || (itemID >= 5130000 && itemID <= 5154000) || (itemID >= 5210000 && itemID <= 5230000)) {
                c.getPlayer().dropMessage(1, "You may not purchase this item.");
                updateInformation(c);
                return;
            }
            if (c.getPlayer().getCSPoints(useNX) >= item.getPrice()) {
                c.getPlayer().modifyCSPoints(useNX, -item.getPrice());
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                AutobanManager.getInstance().autoban(c, "Trying to purchase from the CS when they have no NX.");
                return;
            }
            if (itemID >= 5000000 && itemID <= 5000100) {
                final int petId = MaplePet.createPet(itemID);
                if (petId == -1) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                MapleInventoryManipulator.addById(c, itemID, (short) 1, null, petId);
            } else {
                MapleInventoryManipulator.addById(c, itemID, (short) item.getCount());
            }
            updateInformation(c, snCS);
        } else if (action == 5) {
            try {
                final Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("DELETE FROM wishlist WHERE charid = ?");
                ps.setInt(1, c.getPlayer().getId());
                ps.executeUpdate();
                ps.close();

                int i = 10;
                while (i > 0) {
                    final int sn = slea.readInt();
                    if (sn != 0) {
                        ps = con.prepareStatement("INSERT INTO wishlist(charid, sn) VALUES(?, ?) ");
                        ps.setInt(1, c.getPlayer().getId());
                        ps.setInt(2, sn);
                        ps.executeUpdate();
                        ps.close();
                    }
                    i--;
                }
            } catch (final SQLException ignored) {
            }
            c.getSession().write(MaplePacketCreator.sendWishList(c.getPlayer().getId(), true));
        } else if (action == 7) {
            slea.skip(1);
            final byte toCharge = slea.readByte();
            final int toIncrease = slea.readInt();
            if (c.getPlayer().getCSPoints(toCharge) >= 4000 && c.getPlayer().getStorage().getSlots() < 48) {
                c.getPlayer().modifyCSPoints(toCharge, -4000);
                if (toIncrease == 0) {
                    c.getPlayer().getStorage().gainSlots(4);
                }
                updateInformation(c);
            }
        } else if (action == 28) { // Package
            slea.skip(1);
            final int useNX = slea.readInt();
            final int snCS = slea.readInt();
            final CashItemInfo item = CashItemFactory.getItem(snCS);
            if (c.getPlayer().getCSPoints(useNX) >= item.getPrice()) {
                c.getPlayer().modifyCSPoints(useNX, -item.getPrice());
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                AutobanManager.getInstance().autoban(c, "Trying to purchase from the CS when they have no NX.");
                return;
            }
            for (final int i : CashItemFactory.getPackageItems(item.getId())) {
                if (i >= 5000000 && i <= 5000100) {
                    final int petId = MaplePet.createPet(i);
                    if (petId == -1) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    MapleInventoryManipulator.addById(c, i, (short) 1, null, petId);
                } else {
                    MapleInventoryManipulator.addById(c, i, (short) item.getCount());
                }
            }
            updateInformation(c, snCS);
        } else if (action == 30) {
            final int snCS = slea.readInt();
            final CashItemInfo item = CashItemFactory.getItem(snCS);
            if (c.getPlayer().getMeso() >= item.getPrice()) {
                c.getPlayer().gainMeso(-item.getPrice(), false);
                MapleInventoryManipulator.addById(c, item.getId(), (short) item.getCount());
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                AutobanManager.getInstance().autoban(c, "Trying to purchase from the CS with an insufficient amount.");
            }
        }
    }
}
