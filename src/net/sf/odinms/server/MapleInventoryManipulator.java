package net.sf.odinms.server;

import net.sf.odinms.client.*;
import net.sf.odinms.tools.MaplePacketCreator;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

public class MapleInventoryManipulator {
    private MapleInventoryManipulator() {
    }

    public static boolean addRing(final MapleCharacter chr, final int itemId, final int ringId) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleInventoryType type = ii.getInventoryType(itemId);
        final IItem nEquip = ii.getEquipById(itemId, ringId);

        final byte newSlot = chr.getInventory(type).addItem(nEquip);
        if (newSlot == -1) return false;
        chr.getClient().getSession().write(MaplePacketCreator.addInventorySlot(type, nEquip));
        return true;
    }

    public static boolean addById(final MapleClient c, final int itemId, final short quantity) {
        return addById(c, itemId, quantity, null);
    }

    public static boolean addById(final MapleClient c, final int itemId, final short quantity, final String owner) {
        return addById(c, itemId, quantity, owner, -1);
    }

    public static boolean addById(final MapleClient c, final int itemId, short quantity, final String owner, final int petid) {
        if (quantity >= 4000 || quantity < 0) {
            AutobanManager
                .getInstance()
                .autoban(
                    c,
                    "Packet edited item: " +
                        quantity +
                        "x " +
                        itemId
                );
            return false;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleInventoryType type = ii.getInventoryType(itemId);
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(c, itemId);
            final List<IItem> existing = c.getPlayer().getInventory(type).listById(itemId);
            if (!ii.isThrowingStar(itemId) && !ii.isBullet(itemId)) {
                if (!existing.isEmpty()) {
                    if (itemId == 3992027) return false;
                    final Iterator<IItem> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            final Item eItem = (Item) i.next();
                            final short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null)) {
                                final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.getSession().write(MaplePacketCreator.updateInventorySlot(type, eItem));
                            }
                        } else {
                            break;
                        }
                    }
                }
                while (quantity > 0 || ii.isThrowingStar(itemId) || ii.isBullet(itemId)) {
                    final short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        final Item nItem = new Item(itemId, (byte) 0, newQ, petid);
                        final byte newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1) {
                            c.getSession().write(MaplePacketCreator.getInventoryFull());
                            c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                            return false;
                        }
                        if (owner != null) nItem.setOwner(owner);
                        c.getSession().write(MaplePacketCreator.addInventorySlot(type, nItem));
                        if ((ii.isThrowingStar(itemId) || ii.isBullet(itemId)) && quantity == 0) break;
                    } else {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return false;
                    }
                }
            } else {
                final Item nItem = new Item(itemId, (byte) 0, quantity);
                final byte newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.getSession().write(MaplePacketCreator.addInventorySlot(type, nItem));
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else {
            if (quantity == 1) {
                final IItem nEquip = ii.getEquipById(itemId);
                if (owner != null) {
                    nEquip.setOwner(owner);
                }
                final byte newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.getSession().write(MaplePacketCreator.addInventorySlot(type, nEquip));
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        return true;
    }

    public static boolean addFromDrop(final MapleClient c, final IItem item) {
        return addFromDrop(c, item, true);
    }

    public static boolean addFromDrop(final MapleClient c, final IItem item, final boolean show) {
        return addFromDrop(c, item, show, null);
    }

    public static boolean addFromDrop(final MapleClient c, final IItem item, final boolean show, final String owner) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleInventoryType type = ii.getInventoryType(item.getItemId());
        if (!c.getChannelServer().allowMoreThanOne() && ii.isPickupRestricted(item.getItemId()) && c.getPlayer().haveItem(item.getItemId(), 1, true, false)) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            c.getSession().write(MaplePacketCreator.showItemUnavailable());
            return false;
        }
        short quantity = item.getQuantity();
        if (quantity >= 4000 || quantity < 0) {
            AutobanManager.getInstance().autoban(c.getPlayer().getClient(), "Packet edited item: " + quantity + "x " + item.getItemId());
            return false;
        }
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(c, item.getItemId());
            final List<IItem> existing = c.getPlayer().getInventory(type).listById(item.getItemId());
            if (!ii.isThrowingStar(item.getItemId()) && !ii.isBullet(item.getItemId())) {
                if (!existing.isEmpty()) {
                    final Iterator<IItem> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            final Item eItem = (Item) i.next();
                            final short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && item.getOwner().equals(eItem.getOwner())) {
                                final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.getSession().write(MaplePacketCreator.updateInventorySlot(type, eItem, true));
                            }
                        } else {
                            break;
                        }
                    }
                }
                while (quantity > 0 || ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId())) {
                    final short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    final Item nItem = new Item(item.getItemId(), (byte) 0, newQ);
                    nItem.setOwner(item.getOwner());
                    final byte newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                    if (newSlot == -1) {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.getSession().write(MaplePacketCreator.addInventorySlot(type, nItem, true));
                }
            } else {
                final Item nItem = new Item(item.getItemId(), (byte) 0, quantity);
                final byte newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.getSession().write(MaplePacketCreator.addInventorySlot(type, nItem));
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else {
            if (quantity == 1) {
                final byte newSlot = c.getPlayer().getInventory(type).addItem(item);
                if (newSlot == -1) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.getSession().write(MaplePacketCreator.addInventorySlot(type, item, true));
            } else {
                throw new RuntimeException("Trying to create equip with non-one quantity");
            }
        }
        if (owner != null) {
            item.setOwner(owner);
        }
        if (show) {
            c.getSession().write(MaplePacketCreator.getShowItemGain(item.getItemId(), item.getQuantity()));
        }
        return true;
    }

    public static boolean checkSpace(final MapleClient c, final int itemid, int quantity, final String owner) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleInventoryType type = ii.getInventoryType(itemid);
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(c, itemid);
            final List<IItem> existing = c.getPlayer().getInventory(type).listById(itemid);
            if (!ii.isThrowingStar(itemid) && !ii.isBullet(itemid)) {
                if (!existing.isEmpty()) {
                    for (final IItem eItem : existing) {
                        final short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner.equals(eItem.getOwner())) {
                            final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            final int numSlotsNeeded;
            if (slotMax > 0) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else if (ii.isThrowingStar(itemid) || ii.isBullet(itemid)) {
                numSlotsNeeded = 1;
            } else {
                numSlotsNeeded = 1;
                System.err.println("Error -- 0 slotMax.");
            }
            return !c.getPlayer().getInventory(type).isFull(numSlotsNeeded - 1);
        } else {
            return !c.getPlayer().getInventory(type).isFull();
        }
    }

    public static void removeFromSlot(final MapleClient c, final MapleInventoryType type, final byte slot, final short quantity, final boolean fromDrop) {
        removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static void removeFromSlot(final MapleClient c, final MapleInventoryType type, final byte slot, final short quantity, final boolean fromDrop, final boolean consume) {
        final IItem item = c.getPlayer().getInventory(type).getItem(slot);
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final boolean allowZero = consume && (ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId()));
        c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);
        if (item.getQuantity() == 0 && !allowZero) {
            c.getSession().write(MaplePacketCreator.clearInventoryItem(type, item.getPosition(), fromDrop));
        } else {
            c.getSession().write(MaplePacketCreator.updateInventorySlot(type, item, fromDrop));
        }
    }

    public static void removeById(final MapleClient c, final MapleInventoryType type, final int itemId, final int quantity, final boolean fromDrop, final boolean consume) {
        final List<IItem> items = c.getPlayer().getInventory(type).listById(itemId);
        int remremove = quantity;
        for (final IItem item : items) {
            if (remremove <= item.getQuantity()) {
                removeFromSlot(c, type, item.getPosition(), (short) remremove, fromDrop, consume);
                remremove = 0;
                break;
            } else {
                remremove -= item.getQuantity();
                removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume);
            }
        }
        if (remremove > 0) {
            throw new InventoryException("[h4x] Not enough cheese available (" + itemId + ", " + (quantity - remremove) + "/" + quantity + ")");
        }
    }

    public static void removeAllById(final MapleClient c, final int itemId, final boolean checkEquipped) {
        final MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemId);
        for (final IItem item : c.getPlayer().getInventory(type).listById(itemId)) {
            if (item != null) {
                removeFromSlot(c, type, item.getPosition(), item.getQuantity(), true, false);
            }
        }
        if (checkEquipped) {
            final IItem ii = c.getPlayer().getInventory(type).findById(itemId);
            if (ii != null) {
                c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeItem(ii.getPosition());
                c.getPlayer().equipChanged();
            }
        }
    }

    public static void move(final MapleClient c, final MapleInventoryType type, final byte src, final byte dst) {
        if (src < 0 || dst < 0) return;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final IItem source = c.getPlayer().getInventory(type).getItem(src);
        final IItem initialTarget = c.getPlayer().getInventory(type).getItem(dst);
        if (source == null) return;

        short olddstQ = -1;
        if (initialTarget != null) olddstQ = initialTarget.getQuantity();
        final short oldsrcQ = source.getQuantity();
        final short slotMax = ii.getSlotMax(c, source.getItemId());
        c.getPlayer().getInventory(type).move(src, dst, slotMax);
        if (
            !type.equals(MapleInventoryType.EQUIP) &&
            initialTarget != null &&
            initialTarget.getItemId() == source.getItemId() &&
            !ii.isThrowingStar(source.getItemId()) &&
            !ii.isBullet(source.getItemId())
        ) {
            if ((olddstQ + oldsrcQ) > slotMax) {
                c.getSession().write(MaplePacketCreator.moveAndMergeWithRestInventoryItem(type, src, dst, (short) ((olddstQ + oldsrcQ) - slotMax), slotMax));
            } else {
                c.getSession().write(MaplePacketCreator.moveAndMergeInventoryItem(type, src, dst, c.getPlayer().getInventory(type).getItem(dst).getQuantity()));
            }
        } else {
            c.getSession().write(MaplePacketCreator.moveInventoryItem(type, src, dst));
        }
    }

    public static void equip(final MapleClient c, final byte src, final byte dst) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(src);
        final Equip target;
        if (source == null) return;
        if (!c.getPlayer().isGM() && !c.getChannelServer().CanGMItem()) {
            switch (source.getItemId()) {
                case 1002140: // Wizet Invincible Hat
                case 1042003: // Wizet Plain Suit
                case 1062007: // Wizet Plain Suit Pants
                case 1322013: // Wizet Secret Agent Suitcase
                    removeAllById(c, source.getItemId(), false);
                    c.getPlayer().dropMessage(1, "You're not a GM.");
                    return;
            }
        }
        if (source.getItemId() == 1812006) {
            removeAllById(c, source.getItemId(), false);
            c.getPlayer().dropMessage(1, "Magic Scales have been blocked.");
            return;
        }
        if (dst == -6) {
            // Unequip the overall
            final IItem top = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
            if (top != null && ii.isOverall(top.getItemId())) {
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                unequip(c, (byte) -5, c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        } else if (dst == -5) {
            // Unequip the bottom and top
            final IItem top = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
            final IItem bottom = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -6);
            if (top != null && ii.isOverall(source.getItemId())) {
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull(bottom != null && ii.isOverall(source.getItemId()) ? 1 : 0)) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                unequip(c, (byte) -5, c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
            if (bottom != null && ii.isOverall(source.getItemId())) {
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                unequip(c, (byte) -6, c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        } else if (dst == -10) {
            // Check if weapon is two-handed
            final IItem weapon = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            if (weapon != null && ii.isTwoHanded(weapon.getItemId())) {
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                unequip(c, (byte) -11, c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        } else if (dst == -11) {
            final IItem shield = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
            if (shield != null && ii.isTwoHanded(source.getItemId())) {
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                unequip(c, (byte) -10, c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        } else if (dst == -18) {
            c.getPlayer().getMount().setItemId(source.getItemId());
        }
        source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(src);
        target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(src);
        if (target != null) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeSlot(dst);
        source.setPosition(dst);
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).addFromDB(target);
        }
        if (c.getPlayer().getBuffedValue(MapleBuffStat.BOOSTER) != null && ii.isWeapon(source.getItemId())) {
            c.getPlayer().cancelBuffStats(MapleBuffStat.BOOSTER);
        }
        c.getSession().write(MaplePacketCreator.moveInventoryItem(MapleInventoryType.EQUIP, src, dst, (byte) 2));
        c.getPlayer().equipChanged();
    }

    public static void unequip(final MapleClient c, final byte src, final byte dst) {
        final Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(src);
        final Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        if (dst < 0) {
            System.err.println(
                "Unequipping to negative slot. (" +
                    c.getPlayer().getName() +
                    ": " +
                    src +
                    " -> " +
                    dst +
                    ")"
            );
        }
        if (source == null) return;
        if (target != null && src <= 0) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeSlot(src);
        if (target != null) {
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getPlayer().getInventory(MapleInventoryType.EQUIP).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(target);
        }
        c.getSession().write(MaplePacketCreator.moveInventoryItem(MapleInventoryType.EQUIP, src, dst, (byte) 1));
        c.getPlayer().equipChanged();
    }

    public static void drop(final MapleClient c, MapleInventoryType type, final byte src, final short quantity) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (src < 0) type = MapleInventoryType.EQUIPPED;
        final IItem source = c.getPlayer().getInventory(type).getItem(src);
        final int itemId = source.getItemId();
        if (ii.isCash(itemId)) {
            c.getPlayer().dropMessage(1, "You cannot drop this item.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (c.getPlayer().getItemEffect() == itemId && source.getQuantity() == 1) {
            c.getPlayer().setItemEffect(0);
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.itemEffect(c.getPlayer().getId(), 0));
        } else if (itemId == 5370000 || itemId == 5370001) {
            if (c.getPlayer().getItemQuantity(itemId, false) == 1) {
                c.getPlayer().setChalkboard(null);
            }
        }
        if (quantity < 0 || quantity == 0 && !ii.isThrowingStar(itemId) && !ii.isBullet(itemId)) {
            System.err.println(
                c.getPlayer().getName() +
                    " dropping " +
                    quantity +
                    " " +
                    itemId +
                    " (" +
                    type.name() +
                    "/" +
                    src +
                    ")"
            );
            c.disconnect();
            return;
        }
        final Point dropPos = new Point(c.getPlayer().getPosition());
        if (quantity < source.getQuantity() && !ii.isThrowingStar(itemId) && !ii.isBullet(itemId)) {
            final IItem target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.getSession().write(MaplePacketCreator.dropInventoryItemUpdate(type, source));
            final boolean weddingRing =
                source.getItemId() == 1112803 ||
                source.getItemId() == 1112806 ||
                source.getItemId() == 1112807 ||
                source.getItemId() == 1112809;
            if (weddingRing) {
                c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
            } else if (c.getPlayer().getMap().getEverlast()) {
                if (!c.getChannelServer().allowUndroppablesDrop() && ii.isDropRestricted(target.getItemId())) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, false);
                }
            } else {
                if (!c.getChannelServer().allowUndroppablesDrop() && ii.isDropRestricted(target.getItemId())) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                }
            }
        } else {
            c.getPlayer().getInventory(type).removeSlot(src);
            c.getSession().write(
                MaplePacketCreator.dropInventoryItem(
                    src < 0 ? MapleInventoryType.EQUIP : type,
                    src
                )
            );
            if (src < 0) c.getPlayer().equipChanged();
            if (c.getPlayer().getMap().getEverlast()) {
                if (!c.getChannelServer().allowUndroppablesDrop() && ii.isDropRestricted(itemId)) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, false);
                }
            } else {
                if (!c.getChannelServer().allowUndroppablesDrop() && ii.isDropRestricted(itemId)) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                }
            }
        }
    }
}
