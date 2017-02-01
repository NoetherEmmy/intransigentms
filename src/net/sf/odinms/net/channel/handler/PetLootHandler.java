package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.maps.MapleMapItem;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class PetLootHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter p = c.getPlayer();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        if (p.getNoPets() < 1) return;

        final int petIndex = p.getPetIndex(slea.readInt());
        if (petIndex < 0) return;
        MaplePet pet = p.getPet(petIndex);
        slea.skip(13);
        int oid = slea.readInt();
        final MapleMapObject ob = p.getMap().getMapObject(oid);
        if (ob == null || pet == null) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        if (ob instanceof MapleMapItem) {
            final MapleMapItem mapItem = (MapleMapItem) ob;
            synchronized (mapItem) {
                if (mapItem.isPickedUp()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    return;
                }
                double distance = pet.getPos().distanceSq(mapItem.getPosition());
                p.getCheatTracker().checkPickupAgain();
                if (distance > 90000.0d) { // 300^2, 550 is approximately the range of ultimates
                    p.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC);
                } else if (distance > 22500.0d) {
                    p.getCheatTracker().registerOffense(CheatingOffense.SHORT_ITEMVAC);
                }
                if (mapItem.getMeso() > 0) {
                    // Hack fix in the absence of the actual packet
                    if (p.getInventory(MapleInventoryType.EQUIPPED).findById(1812000) != null) {
                        p.gainMeso(mapItem.getMeso(), true, true);
                        p.getMap().broadcastMessage(
                            MaplePacketCreator.removeItemFromMap(
                                mapItem.getObjectId(),
                                5,
                                p.getId(),
                                true,
                                p.getPetIndex(pet)
                            ),
                            mapItem.getPosition()
                        );
                        p.getCheatTracker().pickupComplete();
                        p.getMap().removeMapObject(ob);
                    } else {
                        p.getCheatTracker().pickupComplete();
                        mapItem.setPickedUp(false);
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                } else {
                    if (ii.isPet(mapItem.getItem().getItemId())) {
                        if (
                            MapleInventoryManipulator.addById(
                                c,
                                mapItem.getItem().getItemId(),
                                mapItem.getItem().getQuantity(),
                                null
                            )
                        ) {
                            p.getMap().broadcastMessage(
                                MaplePacketCreator.removeItemFromMap(
                                    mapItem.getObjectId(),
                                    5,
                                    p.getId(),
                                    true,
                                    p.getPetIndex(pet)
                                ),
                                mapItem.getPosition()
                            );
                            p.getCheatTracker().pickupComplete();
                            p.getMap().removeMapObject(ob);
                        } else {
                            p.getCheatTracker().pickupComplete();
                            return;
                        }
                    } else {
                        if (MapleInventoryManipulator.addFromDrop(c, mapItem.getItem(), true)) {
                            p.getMap().broadcastMessage(
                                MaplePacketCreator.removeItemFromMap(
                                    mapItem.getObjectId(),
                                    5,
                                    p.getId(),
                                    true,
                                    p.getPetIndex(pet)
                                ),
                                mapItem.getPosition()
                            );
                            p.getCheatTracker().pickupComplete();
                            p.getMap().removeMapObject(ob);
                        } else {
                            p.getCheatTracker().pickupComplete();
                            return;
                        }
                    }
                }
                mapItem.setPickedUp(true);
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
