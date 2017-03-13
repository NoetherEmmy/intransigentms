package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.maps.MapleMapItem;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ItemPickupHandler extends AbstractMaplePacketHandler {
    /** Creates a new instance of ItemPickupHandler */
    public ItemPickupHandler() {
    }

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        player.resetAfkTime();
        @SuppressWarnings("unused")
        byte mode = slea.readByte();
        slea.readInt();
        slea.readInt();
        int oid = slea.readInt();
        final MapleMapObject ob = player.getMap().getMapObject(oid);
        if (ob == null) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            c.getSession().write(MaplePacketCreator.getShowInventoryFull());
            return;
        }
        if (ob instanceof MapleMapItem) {
            final MapleMapItem mapitem = (MapleMapItem) ob;
            synchronized (mapitem) {
                if (mapitem.isPickedUp()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                double distance = player.getPosition().distanceSq(mapitem.getPosition());
                // player.getCheatTracker().checkPickupAgain();
                if (mapitem.getMeso() > 0) {
                    if (player.getParty() != null) {
                        ChannelServer cserv = c.getChannelServer();
                        int mesosamm = mapitem.getMeso();
                        int partynum = 0;
                        for (MaplePartyCharacter partymem : player.getParty().getMembers()) {
                            if (partymem.isOnline() && partymem.getMapid() == player.getMap().getId() && partymem.getChannel() == c.getChannel()) {
                                partynum++;
                            }
                        }
                        int mesosgain = mesosamm / partynum;
                        for (MaplePartyCharacter partymem : player.getParty().getMembers()) {
                            if (partymem.isOnline() && partymem.getMapid() == player.getMap().getId()) {
                                MapleCharacter somecharacter = cserv.getPlayerStorage().getCharacterById(partymem.getId());
                                if (somecharacter != null) {
                                    somecharacter.gainMeso(mesosgain, true, true);
                                }
                            }
                        }
                    } else {
                        player.gainMeso(mapitem.getMeso(), true, true);
                    }
                    player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, player.getId()), mapitem.getPosition());
                    player.getCheatTracker().pickupComplete();
                    player.getMap().removeMapObject(ob);
                } else {
                    if (mapitem.getItem().getItemId() >= 5000000 && mapitem.getItem().getItemId() <= 5000100) {
                        int petId = MaplePet.createPet(mapitem.getItem().getItemId());
                        if (petId == -1) {
                            return;
                        }
                        MapleInventoryManipulator.addById(c, mapitem.getItem().getItemId(), mapitem.getItem().getQuantity(), null, petId);
                        player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, player.getId()), mapitem.getPosition());
                        // player.getCheatTracker().pickupComplete();
                        player.getMap().removeMapObject(ob);
                    } else {
                        if (mapitem.getItem().getItemId() == 3992027 && player.getItemQuantity(3992027, false) > 0) {
                            c.getSession().write(MaplePacketCreator.getInventoryFull());
                            c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                            return;
                        }
                        if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                            player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, player.getId()), mapitem.getPosition());
                            // player.getCheatTracker().pickupComplete();
                            player.getMap().removeMapObject(ob);
                        } else {
                            player.getCheatTracker().pickupComplete();
                            return;
                        }
                    }
                }
                mapitem.setPickedUp(true);
                if (player.isOnCQuest()) {
                    player.makeQuestProgress(0, mapitem.getItem().getItemId(), null);
                }
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
