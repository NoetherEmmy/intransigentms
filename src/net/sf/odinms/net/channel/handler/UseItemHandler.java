package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class UseItemHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (itemId == 2022118) {
            c.getPlayer().dropMessage(1, "Please keep this item for scrolling purposes. Gives you 100% scroll rate.");
            c.getSession().write(MaplePacketCreator.enableActions());
        } else if (itemId == 2022065) {
            NPCScriptManager.getInstance().start(c, 9010000, "JobChanger", null);
        } else if (itemId >= 2100000 && itemId < 2120000) {
            c.getPlayer().dropMessage(1, "Summoning bags may not be used.");
            c.getSession().write(MaplePacketCreator.enableActions());
        } else if (toUse != null && toUse.getQuantity() > 0) {
            if (toUse.getItemId() != itemId) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (ii.isTownScroll(itemId)) {
                if (ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer())) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                } else {
                    switch (itemId) {
                        case 2030000: // Next town
                        case 2030008: // Next town (coffee-thingy)
                            final int returnMapId = c.getPlayer().getMap().getReturnMapId();
                            if (returnMapId < 999999999) {
                                warpRandom(c, returnMapId);
                                MapleInventoryManipulator.removeFromSlot(
                                    c,
                                    MapleInventoryType.USE,
                                    slot,
                                    (short) 1,
                                    false
                                );
                            } else {
                                c.getPlayer().dropMessage(5, "You can't use that here.");
                            }
                            break;
                        case 2030001: // Lith Harbor
                            warpRandom(c, 104000000);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030002: // Ellinia
                            warpRandom(c, 101000000);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030003: // Perion
                            warpRandom(c, 102000000);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030004: // Henesys
                            warpRandom(c, 100000000);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030005: // Kerning
                            warpRandom(c, 103000000);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030006: // Sleepywood
                            warpRandom(c, 105040300);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030007: // Dead Mine
                            warpRandom(c, 211041500);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030009: // Mushroom Shrine
                            warpRandom(c, 800000000);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030010: // Showa
                            warpRandom(c, 801000000);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030011: // Omega
                            warpRandom(c, 221000000);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030012: // Ludibrium
                            warpRandom(c, 220000000);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        case 2030019: // Nautilus Harbor
                            warpRandom(c, 120000000);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                    }
                }
                c.getSession().write(MaplePacketCreator.enableActions());
            } else {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer());
            }
        }
    }

    private void warpRandom(MapleClient c, int mapId) {
        MapleMap target = c.getChannelServer().getMapFactory().getMap(mapId);
        c.getPlayer().changeMap(target, target.getRandomPortal());
    }
}
