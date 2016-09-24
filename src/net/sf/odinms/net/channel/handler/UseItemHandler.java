package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class UseItemHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        try {
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
                return;
            } else if (toUse != null && toUse.getQuantity() > 0) {
                if (toUse.getItemId() != itemId) {
                    return;
                }
                if (ii.isTownScroll(itemId)) {
                    if (ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer())) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                    } else {
                        switch (itemId) {
                            case 2030000: // Next town
                            case 2030008: // Next town (coffee-thingy)
                                c.getPlayer().changeMap(c.getPlayer().getMap().getReturnMapId(), 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030001: // Lith Harbor
                                c.getPlayer().changeMap(104000000, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030002: // Ellinia
                                c.getPlayer().changeMap(101000000, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030003: // Perion
                                c.getPlayer().changeMap(102000000, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030004: // Henesys
                                c.getPlayer().changeMap(100000000, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030005: // Kerning
                                c.getPlayer().changeMap(103000000, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030006: // Sleepywood
                                c.getPlayer().changeMap(105040300, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030007: // Dead Mine
                                c.getPlayer().changeMap(211041500, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030009: // Mushroom Shrine
                                c.getPlayer().changeMap(800000000, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030010: // Showa
                                c.getPlayer().changeMap(801000000, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030011: // Omega
                                c.getPlayer().changeMap(221000000, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030012: // Ludibrium
                                c.getPlayer().changeMap(220000000, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            case 2030019: // Nautilus Harbor
                                c.getPlayer().changeMap(120000000, 0);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                break;
                            default:
                                break;
                        }
                    }
                    c.getSession().write(MaplePacketCreator.enableActions());
                } else {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                    ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}