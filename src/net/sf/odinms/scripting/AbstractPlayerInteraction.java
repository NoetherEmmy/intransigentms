package net.sf.odinms.scripting;

import net.sf.odinms.client.*;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.MaplePacketCreator;

import java.awt.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractPlayerInteraction {
    private final MapleClient c;

    public AbstractPlayerInteraction(MapleClient c) {
        this.c = c;
    }

    public MapleClient getClient() {
        return c;
    }

    public MapleCharacter getPlayer() {
        return c.getPlayer();
    }

    public void warp(int map) {
        try {
            getPlayer().changeMap(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void warp(int map, int portal) {
        getPlayer().changeMap(map, portal);
    }

    public void warp(int map, String portal) {
        getPlayer().changeMap(map, portal);
    }

    public boolean warp(int map, String curPortal, String nextPortal) {
        if (getPlayer().getMap().getPortal(curPortal).getPortalState()) {
            getPlayer().changeMap(map, nextPortal);
            return true;
        } else {
            c.getPlayer().dropMessage(5, "The battle against the boss has begun... please come back later.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return false;
        }
    }

    public void warpRandom(int mapId) {
        MapleMap target = c.getChannelServer().getMapFactory().getMap(mapId);
        getPlayer().changeMap(target, target.getRandomPortal());
    }

    private MapleMap getWarpMap(int map) {
        MapleMap target;
        if (getPlayer().getEventInstance() == null) {
            target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(map);
        } else {
            target = getPlayer().getEventInstance().getMapInstance(map);
        }
        return target;
    }

    public MapleMap getMap(int map) { // WTF IS THIS.
        return getWarpMap(map);
    }

    public boolean haveItem(int itemid) {
        return haveItem(itemid, 1);
    }

    public boolean haveItem(int itemid, int quantity) {
        return haveItem(itemid, quantity, false, true);
    }

    public boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        return getPlayer().haveItem(itemid, quantity, checkEquipped, greaterOrEquals);
    }

    public boolean canHold(int itemid) {
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
        MapleInventory iv = getPlayer().getInventory(type);

        return iv.getNextFreeSlot() > -1;
    }

    public MapleQuestStatus.Status getQuestStatus(int id) {
        return getPlayer().getQuest(MapleQuest.getInstance(id)).getStatus();
    }

    public boolean gainItem(int id) {
        return gainItem(id, (short) 1);
    }

    public boolean gainItem(int id, short quantity) {
        return gainItem(id, quantity, false, true);
    }

    public boolean gainItem(int id, short quantity, boolean show) {
        return gainItem(id, quantity, false, show);
    }

    /** Gives item with the specified id or takes it if the quantity is negative.
     ** Note that this does NOT take items from the equipped inventory.
     ** randomStats for generating random stats on the generated equip. */
    public boolean gainItem(int id, short quantity, boolean randomStats, boolean show) {
        if (quantity >= 0) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            IItem item = ii.getEquipById(id);
            MapleInventoryType type = ii.getInventoryType(id);
            if (type.equals(MapleInventoryType.EQUIP) && !ii.isThrowingStar(item.getItemId()) && !ii.isBullet(item.getItemId())) {
                if (!getPlayer().getInventory(type).isFull()) {
                    if (randomStats) {
                        MapleInventoryManipulator.addFromDrop(c, ii.randomizeStats(getClient(), (Equip) item), false);
                    } else {
                        MapleInventoryManipulator.addFromDrop(c, item, false);
                    }
                } else {
                    c.getPlayer().dropMessage(1, "Your inventory is full. Please remove an item from your " + type.name().toLowerCase() + " inventory, and then type @mapleadmin into chat to claim the item.");
                    c.getPlayer().setUnclaimedItem(id, quantity);
                    return false;
                }
            } else if (MapleInventoryManipulator.checkSpace(c, id, quantity, "")) {
                if (id >= 5000000 && id <= 5000100) {
                    if (quantity > 1) {
                        quantity = 1;
                    }
                    int petId = MaplePet.createPet(id);
                    MapleInventoryManipulator.addById(c, id, (short) 1, null, petId);
                    if (show) {
                        c.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity));
                    }
                } else {
                    MapleInventoryManipulator.addById(c, id, quantity);
                }
            } else {
                c.getPlayer().dropMessage(1, "Your inventory is full. Please remove an item from your " + type.name().toLowerCase() + " inventory, and then type @mapleadmin into chat to claim the item.");
                c.getPlayer().setUnclaimedItem(id, quantity);
                return false;
            }
            if (show) {
                c.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
            }
        } else {
            MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
        }
        return true;
    }

    public void changeMusic(String songName) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(songName));
    }

    // default playerMessage and mapMessage to use type 5
    public void playerMessage(String message) {
        playerMessage(5, message);
    }

    public void mapMessage(String message) {
        mapMessage(5, message);
    }

    public void guildMessage(String message) {
        guildMessage(5, message);
    }

    public void playerMessage(int type, String message) {
        getPlayer().dropMessage(type, message);
    }

    public void mapMessage(int type, String message) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    public void guildMessage(int type, String message) {
        MapleGuild guild = getGuild();
        if (guild != null) {
            guild.guildMessage(MaplePacketCreator.serverNotice(type, message));
        }
    }

    public MapleGuild getGuild() {
        try {
            return c.getChannelServer().getWorldInterface().getGuild(getPlayer().getGuildId(), null);
        } catch (RemoteException ex) {
            Logger.getLogger(AbstractPlayerInteraction.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public MapleParty getParty() {
        return getPlayer().getParty();
    }

    public boolean isLeader() {
        return getParty().getLeader().equals(new MaplePartyCharacter(getPlayer()));
    }
    //PQ methods: give items/exp to all party members

    public void givePartyItems(int id, short quantity, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            MapleClient cl = chr.getClient();
            if (quantity >= 0) {
                MapleInventoryManipulator.addById(cl, id, quantity);
            } else {
                MapleInventoryManipulator.removeById(cl, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
            }
            cl.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
        }
    }
    //PQ gain EXP: Multiplied by channel rate here to allow global values to be input direct into NPCs

    public void givePartyExp(int amount, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.gainExp(amount * c.getChannelServer().getExpRate(), true, true);
        }
    }
    //remove all items of type from party
    //combination of haveItem and gainItem

    public void removeFromParty(int id, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            int possesed = chr.getItemQuantity(id, false);

            if (possesed > 0) {
                MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, possesed, true, false);
                chr.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -possesed, true));
            }
        }
    }

    public void removeAll(int id) {
        removeAll(id, false);
    }

    //remove all items of type from character
    //combination of haveItem and gainItem
    public void removeAll(int id, boolean checkEquipped) {
        MapleInventoryManipulator.removeAllById(c, id, checkEquipped);
    }

    public void gainCloseness(int closeness, int index) {
        MaplePet pet = getPlayer().getPet(index);
        if (pet != null) {
            pet.setCloseness(pet.getCloseness() + closeness);
            getClient().getSession().write(MaplePacketCreator.updatePet(pet, true));
        }
    }

    public void gainClosenessAll(int closeness) {
        for (MaplePet pet : getPlayer().getPets()) {
            if (pet != null) {
                pet.setCloseness(pet.getCloseness() + closeness);
                getClient().getSession().write(MaplePacketCreator.updatePet(pet, true));
            }
        }
    }

    public int getMapId() {
        return getPlayer().getMap().getId();
    }

    public int getPlayerCount(int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getCharacters().size();
    }

    public int getCurrentPartyId(int mapid) {
        return getMap(mapid).getCurrentPartyId();
    }

    public void showInstruction(String msg, int width, int height) {
        c.getSession().write(MaplePacketCreator.sendHint(msg, width, height));
    }

    public void openNpc(int npcid) {
        NPCScriptManager.getInstance().dispose(c);
        NPCScriptManager.getInstance().start(getClient(), npcid);
    }

    public String serverName() {
        return c.getChannelServer().getServerName();
    }

    public void startMapEffect(String msg) {
        getPlayer().getMap().startMapEffect(msg, 5120008); // might work ?
    }

    /**
     * Spawns an NPC at a custom position
     */
    public void spawnNpc(int npcId, Point pos) {
        MapleNPC npc = MapleLifeFactory.getNPC(npcId);
        if (npc != null && !npc.getName().equals("MISSINGNO")) {
            npc.setPosition(pos);
            npc.setCy(pos.y);
            npc.setRx0(pos.x + 50);
            npc.setRx1(pos.x - 50);
            npc.setFh(getPlayer().getMap().getFootholds().findBelow(pos).getId());
            npc.setCustom(true);
            getPlayer().getMap().addMapObject(npc);
            getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
        }
    }
}
