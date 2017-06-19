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

    public AbstractPlayerInteraction(final MapleClient c) {
        this.c = c;
    }

    public MapleClient getClient() {
        return c;
    }

    public MapleCharacter getPlayer() {
        return c.getPlayer();
    }

    public void warp(final int map) {
        try {
            getPlayer().changeMap(map);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void warp(final int map, final int portal) {
        getPlayer().changeMap(map, portal);
    }

    public void warp(final int map, final String portal) {
        getPlayer().changeMap(map, portal);
    }

    public boolean warp(final int map, final String curPortal, final String nextPortal) {
        if (getPlayer().getMap().getPortal(curPortal).getPortalState()) {
            getPlayer().changeMap(map, nextPortal);
            return true;
        } else {
            c.getPlayer().dropMessage(5, "The battle against the boss has begun... please come back later.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return false;
        }
    }

    public void warpRandom(final int mapId) {
        final MapleMap target = c.getChannelServer().getMapFactory().getMap(mapId);
        getPlayer().changeMap(target, target.getRandomPortal());
    }

    private MapleMap getWarpMap(final int map) {
        if (getPlayer().getEventInstance() == null) {
            return ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(map);
        }
        return getPlayer().getEventInstance().getMapInstance(map);
    }

    public MapleMap getMap(final int map) {
        return getWarpMap(map);
    }

    public boolean haveItem(final int itemid) {
        return haveItem(itemid, 1);
    }

    public boolean haveItem(final int itemid, final int quantity) {
        return haveItem(itemid, quantity, false, true);
    }

    public boolean haveItem(final int itemid, final int quantity, final boolean checkEquipped, final boolean greaterOrEquals) {
        return getPlayer().haveItem(itemid, quantity, checkEquipped, greaterOrEquals);
    }

    public boolean canHold(final int itemid) {
        final MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
        final MapleInventory iv = getPlayer().getInventory(type);

        return iv.getNextFreeSlot() > -1;
    }

    public MapleQuestStatus.Status getQuestStatus(final int id) {
        return getPlayer().getQuest(MapleQuest.getInstance(id)).getStatus();
    }

    public boolean gainItem(final int id) {
        return gainItem(id, (short) 1);
    }

    public boolean gainItem(final int id, final short quantity) {
        return gainItem(id, quantity, false, true);
    }

    public boolean gainItem(final int id, final short quantity, final boolean show) {
        return gainItem(id, quantity, false, show);
    }

    /**
     * Gives item with the specified ID, or takes it if the quantity is negative.
     * Note that this does NOT take items from the equipped inventory.
     *
     * @param randomStats Give random stats to the generated equip.
     */
    public boolean gainItem(final int id, short quantity, final boolean randomStats, final boolean show) {
        if (quantity >= 0) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final IItem item = ii.getEquipById(id);
            final MapleInventoryType type = ii.getInventoryType(id);
            if (
                type.equals(MapleInventoryType.EQUIP) &&
                !ii.isThrowingStar(item.getItemId()) &&
                !ii.isBullet(item.getItemId())
            ) {
                if (!getPlayer().getInventory(type).isFull()) {
                    if (randomStats) {
                        MapleInventoryManipulator.addFromDrop(
                            c,
                            ii.randomizeStats(c, (Equip) item),
                            false
                        );
                    } else {
                        MapleInventoryManipulator.addFromDrop(c, item, false);
                    }
                } else {
                    c.getPlayer().dropMessage(
                        1,
                        "Your inventory is full. Please remove an item from your " +
                            type.name().toLowerCase() +
                            " inventory, and then type @mapleadmin into chat to claim the item."
                    );
                    c.getPlayer().addUnclaimedItem(id, quantity);
                    return false;
                }
            } else if (MapleInventoryManipulator.checkSpace(c, id, quantity, "")) {
                if (id >= 5000000 && id <= 5000100) {
                    if (quantity > 1) quantity = 1;
                    final int petId = MaplePet.createPet(id);
                    MapleInventoryManipulator.addById(c, id, (short) 1, null, petId);
                    if (show) c.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity));
                } else {
                    MapleInventoryManipulator.addById(c, id, quantity);
                }
            } else {
                c.getPlayer().dropMessage(
                    1,
                    "Your inventory is full. Please remove an item from your " +
                        type.name().toLowerCase() +
                        " inventory, and then type @mapleadmin into chat to claim the item."
                );
                c.getPlayer().addUnclaimedItem(id, quantity);
                return false;
            }
            if (show) c.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
        } else {
            MapleInventoryManipulator.removeById(
                c,
                MapleItemInformationProvider.getInstance().getInventoryType(id),
                id,
                -quantity,
                true,
                false
            );
        }
        return true;
    }

    public void changeMusic(final String songName) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(songName));
    }

    // Default playerMessage and mapMessage to use type 5
    public void playerMessage(final String message) {
        playerMessage(5, message);
    }

    public void mapMessage(final String message) {
        mapMessage(5, message);
    }

    public void guildMessage(final String message) {
        guildMessage(5, message);
    }

    public void playerMessage(final int type, final String message) {
        getPlayer().dropMessage(type, message);
    }

    public void mapMessage(final int type, final String message) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    public void guildMessage(final int type, final String message) {
        final MapleGuild guild = getGuild();
        if (guild != null) {
            guild.guildMessage(MaplePacketCreator.serverNotice(type, message));
        }
    }

    public MapleGuild getGuild() {
        try {
            return c.getChannelServer().getWorldInterface().getGuild(getPlayer().getGuildId(), null);
        } catch (final RemoteException ex) {
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

    /**
     * PQ method: Gives items/exp to all party members.
     */
    public void givePartyItems(final int id, final short quantity, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            final MapleClient cl = chr.getClient();
            if (quantity >= 0) {
                MapleInventoryManipulator.addById(cl, id, quantity);
            } else {
                MapleInventoryManipulator.removeById(cl, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
            }
            cl.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
        }
    }

    /**
     * PQ gain EXP: Multiplied by channel rate and absolute EXP multiplier here
     * to allow global values to be input direct into NPCs.
     */
    public void givePartyExp(final int amount, final List<MapleCharacter> party) {
        party.forEach(chr ->
            chr.gainExp(amount * c.getChannelServer().getExpRate() * chr.getAbsoluteXp(), true, true)
        );
    }

    /**
     * Remove all items of type from party.
     * Combination of {@code haveItem} and {@code gainItem}.
     */
    public void removeFromParty(final int id, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            final int possesed = chr.getItemQuantity(id, false);

            if (possesed > 0) {
                MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, possesed, true, false);
                chr.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -possesed, true));
            }
        }
    }

    public void removeAll(final int id) {
        removeAll(id, false);
    }

    /**
     * <p>
     * Remove all items of specified ID from character.
     * </p>
     *
     * <p>
     * Used to replace a combination of
     * {@link AbstractPlayerInteraction#haveItem} and
     * {@link AbstractPlayerInteraction#gainItem}.
     * </p>
     */
    public void removeAll(final int id, final boolean checkEquipped) {
        MapleInventoryManipulator.removeAllById(c, id, checkEquipped);
    }

    public void gainCloseness(final int closeness, final int index) {
        final MaplePet pet = getPlayer().getPet(index);
        if (pet != null) {
            pet.setCloseness(pet.getCloseness() + closeness);
            c.getSession().write(MaplePacketCreator.updatePet(pet, true));
        }
    }

    public void gainClosenessAll(final int closeness) {
        for (final MaplePet pet : getPlayer().getPets()) {
            if (pet != null) {
                pet.setCloseness(pet.getCloseness() + closeness);
                c.getSession().write(MaplePacketCreator.updatePet(pet, true));
            }
        }
    }

    public int getMapId() {
        return getPlayer().getMap().getId();
    }

    public int getPlayerCount(final int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getCharacters().size();
    }

    public int getCurrentPartyId(final int mapId) {
        return getMap(mapId).getCurrentPartyId();
    }

    public void showInstruction(final String msg, final int width, final int height) {
        c.getSession().write(MaplePacketCreator.sendHint(msg, width, height));
    }

    public void openNpc(final int npcid) {
        NPCScriptManager.getInstance().dispose(c);
        NPCScriptManager.getInstance().start(c, npcid);
    }

    public String serverName() {
        return c.getChannelServer().getServerName();
    }

    public void startMapEffect(final String msg) {
        getPlayer().getMap().startMapEffect(msg, 5120008); // Might work?
    }

    /**
     * Spawns an NPC at a custom position.
     */
    public void spawnNpc(final int npcId, final Point pos) {
        final MapleNPC npc = MapleLifeFactory.getNPC(npcId);
        if (!npc.getName().equals("MISSINGNO")) {
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
