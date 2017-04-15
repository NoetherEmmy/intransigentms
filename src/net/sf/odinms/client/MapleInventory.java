package net.sf.odinms.client;

import net.sf.odinms.server.MapleItemInformationProvider;

import java.util.*;

public class MapleInventory implements Iterable<IItem>, InventoryContainer {
    private final Map<Byte, IItem> inventory;
    private byte slotLimit;
    private final MapleInventoryType type;

    /** Creates a new instance of MapleInventory */
    public MapleInventory(final MapleInventoryType type, final byte slotLimit) {
        this.inventory = new LinkedHashMap<>();
        this.slotLimit = slotLimit;
        this.type = type;
    }

    /** Returns the item with its slot ID if it exists within the inventory, otherwise {@code null} is returned. */
    public IItem findById(final int itemId) {
        for (final IItem item : inventory.values()) {
            if (item.getItemId() == itemId) return item;
        }
        return null;
    }

    public int countById(final int itemId) {
        int possesed = 0;
        for (final IItem item : inventory.values()) {
            if (item.getItemId() == itemId) possesed += item.getQuantity();
        }
        return possesed;
    }

    public List<IItem> listById(final int itemId) {
        final List<IItem> ret = new ArrayList<>();
        for (final IItem item : inventory.values()) {
            if (item.getItemId() == itemId) ret.add(item);
        }
        if (ret.size() > 1) Collections.sort(ret);
        return ret;
    }

    public Collection<IItem> list() {
        return inventory.values();
    }

    /** Adds the item to the inventory and returns the assigned slot ID. */
    public byte addItem(final IItem item) {
        final byte slotId = getNextFreeSlot();
        if (slotId < 0) return -1;
        inventory.put(slotId, item);
        item.setPosition(slotId);
        return slotId;
    }

    public void addFromDB(final IItem item) {
        if (item.getPosition() < 0 && !type.equals(MapleInventoryType.EQUIPPED)) {
            throw new RuntimeException("Item with negative position in non-equipped inv?");
        }
        inventory.put(item.getPosition(), item);
    }

    public void move(final byte sSlot, final byte dSlot, final short slotMax) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Item source = (Item) inventory.get(sSlot);
        final Item target = (Item) inventory.get(dSlot);
        if (source == null) throw new InventoryException("Trying to move empty slot");
        if (target == null) {
            source.setPosition(dSlot);
            inventory.put(dSlot, source);
            inventory.remove(sSlot);
        } else if (
            target.getItemId() == source.getItemId() &&
            !ii.isThrowingStar(source.getItemId()) &&
            !ii.isBullet(source.getItemId())
        ) {
            if (type.getType() == MapleInventoryType.EQUIP.getType()) {
                swap(target, source);
            }
            if (source.getQuantity() + target.getQuantity() > slotMax) {
                final short rest = (short) ((source.getQuantity() + target.getQuantity()) - slotMax);
                source.setQuantity(rest);
                target.setQuantity(slotMax);
            } else {
                target.setQuantity((short) (source.getQuantity() + target.getQuantity()));
                inventory.remove(sSlot);
            }
        } else {
            swap(target, source);
        }
    }

    private void swap(final IItem source, final IItem target) {
        inventory.remove(source.getPosition());
        inventory.remove(target.getPosition());
        final byte swapPos = source.getPosition();
        source.setPosition(target.getPosition());
        target.setPosition(swapPos);
        inventory.put(source.getPosition(), source);
        inventory.put(target.getPosition(), target);
    }

    public IItem getItem(final byte slot) {
        return inventory.get(slot);
    }

    public void removeItem(final byte slot) {
        removeItem(slot, (short) 1, false);
    }

    public void removeItem(final byte slot, final short quantity, final boolean allowZero) {
        final IItem item = inventory.get(slot);
        if (item == null) return;
        item.setQuantity((short) (item.getQuantity() - quantity));
        if (item.getQuantity() < 0) item.setQuantity((short) 0);
        if (item.getQuantity() == 0 && !allowZero) removeSlot(slot);
    }

    public void removeSlot(final byte slot) {
        inventory.remove(slot);
    }

    public byte getSlotLimit() {
        return slotLimit;
    }

    public int getSize() {
        return inventory.size();
    }

    public boolean isFull() {
        return inventory.size() >= slotLimit;
    }

    public boolean isFull(final int margin) {
        return inventory.size() + margin >= slotLimit;
    }

    /** Returns the next empty slot ID, or -1 if the inventory is full. */
    public byte getNextFreeSlot() {
        if (isFull()) return -1;
        for (byte i = 1; i <= slotLimit; ++i) {
            if (!inventory.containsKey(i)) return i;
        }
        return -1;
    }

    public MapleInventoryType getType() {
        return type;
    }

    @Override
    public Iterator<IItem> iterator() {
        return Collections.unmodifiableCollection(inventory.values()).iterator();
    }

    @Override
    public Collection<MapleInventory> allInventories() {
        return Collections.singletonList(this);
    }
}
