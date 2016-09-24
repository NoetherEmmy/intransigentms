package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ItemSortHandler2 extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.readInt();
        byte mode = slea.readByte();
        /*
        MapleInventoryType invType = MapleInventoryType.getByType(mode);
        MapleInventory Inv = c.getPlayer().getInventory(invType);

        List<IItem> itemMap = new LinkedList<IItem>();
        for (IItem item : Inv.list()) {
        if (item.getPetId() == -1) {
        itemMap.add(item.copy()); // clone all  items T___T.
        }
        }
        for (IItem itemStats : itemMap) {
        MapleInventoryManipulator.removeById(c, invType, itemStats.getItemId(), itemStats.getQuantity(), true, false);
        }

        LinkedList<IItem> sortedItems = sortItems(itemMap);
        for (IItem item : sortedItems) {
        MapleInventoryManipulator.addFromDrop(c, item, "stuff", false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
        itemMap.clear();
        sortedItems.clear();
         */
        c.getSession().write(MaplePacketCreator.finishedSort2(mode));
    }

//    private LinkedList<IItem> sortItems(List<IItem> passedMap) {
//        List<Integer> itemIds = new ArrayList<Integer>(); // empty list.
//        for (IItem item : passedMap) {
//            itemIds.add(item.getItemId()); // adds all item ids to the empty list to be sorted.
//        }
//        Collections.sort(itemIds); // sorts item ids
//
//        LinkedList<IItem> sortedList = new LinkedList<IItem>(); // ordered list pl0x <3.
//
//        for (Integer val : itemIds) {
//            for (IItem item : passedMap) {
//                if (val == item.getItemId()) { // Goes through every index and finds the first value that matches
//                    sortedList.add(item);
//                    passedMap.remove(item);
//                    break;
//                }
//            }
//        }
//        return sortedList;
//    }
}