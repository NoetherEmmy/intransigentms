package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleStorage;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class StorageHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte mode = slea.readByte();
        final MapleStorage storage = c.getPlayer().getStorage();
        if (mode == 4) { // Take out
            byte type = slea.readByte();
            byte slot = slea.readByte();
            slot = storage.getSlot(MapleInventoryType.getByType(type), slot);
            IItem item = storage.takeOut(slot);
            if (item != null) {
                if (
                    MapleInventoryManipulator.checkSpace(
                        c,
                        item.getItemId(),
                        item.getQuantity(),
                        item.getOwner()
                    )
                ) {
                    MapleInventoryManipulator.addFromDrop(c, item, false);
                } else {
                    storage.store(item);
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "Your inventory is full."));
                }
                storage.sendTakenOut(c, ii.getInventoryType(item.getItemId()));
            } else {
                AutobanManager
                    .getInstance()
                    .autoban(
                        c,
                        "Trying to take out item from storage which does not exist."
                    );
            }
        } else if (mode == 5) { // Store
            byte slot = (byte) slea.readShort();
            int itemId = slea.readInt();
            short quantity = slea.readShort();
            if (quantity < 1) {
                AutobanManager.getInstance().autoban(c, "Trying to store " + quantity + " of " + itemId);
                return;
            }
            if (storage.isFull()) {
                c.getSession().write(MaplePacketCreator.getStorageFull());
                return;
            }
            if (c.getPlayer().getMeso() < 100) {
                c.getSession()
                 .write(
                     MaplePacketCreator.serverNotice(
                         1,
                         "You don't have enough mesos to store the item."
                     )
                 );
            } else {
                MapleInventoryType type = ii.getInventoryType(itemId);
                IItem item = c.getPlayer().getInventory(type).getItem(slot).copy();
                if (
                    item.getItemId() == itemId &&
                    (item.getQuantity() >= quantity || ii.isThrowingStar(itemId) || ii.isBullet(itemId))
                ) {
                    if (ii.isThrowingStar(itemId) || ii.isBullet(itemId)) {
                        quantity = item.getQuantity();
                    }
                    c.getPlayer().gainMeso(-100, false, true, false);
                    MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                    item.setQuantity(quantity);
                    storage.store(item);
                } else {
                    AutobanManager
                        .getInstance()
                        .addPoints(
                            c,
                            1000,
                            0,
                            "Trying to store non-matching itemid (" +
                                itemId +
                                "/" +
                                item.getItemId() +
                                ") or quantity not in posession (" +
                                quantity +
                                "/" +
                                item.getQuantity() +
                                ")"
                        );
                    return;
                }
            }
            storage.sendStored(c, ii.getInventoryType(itemId));
        } else if (mode == 6) {
            c.getPlayer().dropMessage(1, "Sorry, storage sorting is unavailable.");
        } else if (mode == 7) { // Meso.
            int meso = slea.readInt();
            int storageMesos = storage.getMeso();
            int playerMesos = c.getPlayer().getMeso();
            if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                if (meso < 0 && storageMesos - meso < 0) { // Storing with overflow
                    meso = -(Integer.MAX_VALUE - storageMesos);
                    if ((-meso) > playerMesos) { // Should never happen, just a failsafe
                        throw new RuntimeException();
                    }
                } else if (meso > 0 && (playerMesos + meso) < 0) { // Taking out with overflow
                    meso = (Integer.MAX_VALUE - playerMesos);
                    if ((meso) > storageMesos) { // Should never happen, just a failsafe
                        throw new RuntimeException();
                    }
                }
                storage.setMeso(storageMesos - meso);
                c.getPlayer().gainMeso(meso, false, true, false);
            } else {
                AutobanManager
                    .getInstance()
                    .addPoints(
                        c,
                        1000,
                        0,
                        "Trying to store or take out unavailable amount of mesos (" +
                            meso +
                            "/" +
                            storage.getMeso() +
                            "/" +
                            c.getPlayer().getMeso() +
                            ")"
                    );
                return;
            }
            storage.sendMeso(c);
        } else if (mode == 8) { // Close
            storage.close();
        }
    }
}
