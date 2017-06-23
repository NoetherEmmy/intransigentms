package net.sf.odinms.net.channel.handler;

//import net.sf.odinms.client.IItem;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

//import net.sf.odinms.client.MapleInventoryType;
//import net.sf.odinms.server.MapleInventoryManipulator;
//import net.sf.odinms.server.MapleItemInformationProvider;
//import net.sf.odinms.server.life.MapleLifeFactory;
//import net.sf.odinms.server.life.MapleMonster;

public class UseSummonBag extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        c.getPlayer().dropMessage(1, "Summoning bags may not be used.");
        /*
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemId && c.getPlayer().isAlive()) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            int[][] toSpawn = ii.getSummonMobs(itemId);
            for (int z = 0; z < toSpawn.length; ++z) {
                int[] toSpawnChild = toSpawn[z];
                if ((int) Math.ceil(Math.random() * 100) <= toSpawnChild[1]) {
                    MapleMonster ht = MapleLifeFactory.getMonster(toSpawnChild[0]);
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(ht, c.getPlayer().getPosition());
                }
            }
        } else {
            c.getPlayer().ban("Trying to use a summonbag not in item inventory.", true);
            return;
        }
        */
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
