package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ItemSortHandler2 extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.readInt();
        byte mode = slea.readByte();

        final MapleInventoryType invType = MapleInventoryType.getByType(mode);
        MapleInventory inv = c.getPlayer().getInventory(invType);

        List<IItem> itemStatMap = inv.list()
                                     .stream()
                                     .filter(i -> i.getPetId() == -1)
                                     .map(IItem::copy)
                                     .collect(Collectors.toList());

        itemStatMap.forEach(itemStats ->
            MapleInventoryManipulator.removeById(
                c,
                invType,
                itemStats.getItemId(),
                itemStats.getQuantity(),
                true,
                false
            )
        );

        itemStatMap.sort(Comparator.comparingInt(IItem::getItemId));

        itemStatMap.forEach(item -> MapleInventoryManipulator.addFromDrop(c, item, false, item.getOwner()));

        c.getSession().write(MaplePacketCreator.finishedSort2(mode));
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
