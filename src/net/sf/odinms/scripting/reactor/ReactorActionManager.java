package net.sf.odinms.scripting.reactor;

import net.sf.odinms.client.*;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.scripting.AbstractPlayerInteraction;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleMonsterInformationProvider.DropEntry;
import net.sf.odinms.server.maps.BossMapMonitor;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleReactor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ReactorActionManager extends AbstractPlayerInteraction {
    private final MapleReactor reactor;
    //private static final Logger log = LoggerFactory.getLogger(ReactorActionManager.class);

    public ReactorActionManager(final MapleClient c, final MapleReactor reactor) {
        super(c);
        this.reactor = reactor;
    }

    // Only used for meso = false, really. No minItems because meso is used to fill the gap.
    public void dropItems() {
        dropItems(false, 0, 0, 0, 0);
    }

    public void dropItems(final boolean meso, final int mesoChance, final int minMeso, final int maxMeso) {
        dropItems(meso, mesoChance, minMeso, maxMeso, 0);
    }

    public void dropItems(final boolean meso, final int mesoChance, final int minMeso, final int maxMeso, final int minItems) {
        final List<DropEntry> chances = getDropChances();
        final List<DropEntry> items = new ArrayList<>();
        int numItems = 0;

        if (meso && Math.random() < (1.0d / (double) mesoChance)) {
            items.add(new DropEntry(0, mesoChance));
        }

        // Narrow list down by chances.
        final Iterator<DropEntry> iter = chances.iterator();
        //for (DropEntry d : chances) {
        while (iter.hasNext()) {
            final DropEntry d = iter.next();
            if (Math.random() < (1.0d / (double) d.chance)) {
                numItems++;
                items.add(d);
            }
        }

        // If a minimum number of drops is required, add mesos.
        while (items.size() < minItems) {
            items.add(new DropEntry(0, mesoChance));
            numItems++;
        }

        // Randomize drop order.
        Collections.shuffle(items);

        final Point dropPos = reactor.getPosition();

        dropPos.x -= 12 * numItems;

        for (final DropEntry d : items) {
            if (d.itemId == 0) {
                final int range = maxMeso - minMeso;
                final int displayDrop = (int) (Math.random() * range) + minMeso;
                final int mesoDrop = displayDrop * ChannelServer.getInstance(getClient().getChannel()).getMesoRate();
                reactor.getMap().spawnMesoDrop(mesoDrop, displayDrop, dropPos, reactor, getPlayer(), meso);
            } else {
                final IItem drop;
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (ii.getInventoryType(d.itemId) != MapleInventoryType.EQUIP) {
                    drop = new Item(d.itemId, (byte) 0, (short) 1);
                } else {
                    drop = ii.randomizeStats(getClient(), (Equip) ii.getEquipById(d.itemId));
                }
                reactor.getMap().spawnItemDrop(reactor, getPlayer(), drop, dropPos, false, true);
            }
            dropPos.x += 25;
        }
    }

    public void dropItem(final int itemId) {
        dropItem(itemId, 1);
    }

    public void dropItem(final int itemId, final int qty) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Point dropPos = new Point(reactor.getPosition().x, reactor.getPosition().y);

        dropPos.x -= 12 * qty;

        for (int i = 0; i < qty; ++i) {
            if (itemId == 0) {
                final int mesoDrop = qty * ChannelServer.getInstance(getClient().getChannel()).getMesoRate();
                reactor.getMap().spawnMesoDrop(mesoDrop, qty, dropPos, reactor, getPlayer(), true);
            } else {
                final IItem drop;
                if (ii.getInventoryType(itemId) != MapleInventoryType.EQUIP) {
                    drop = new Item(itemId, (byte) 0, (short) 1);
                } else {
                    drop = ii.randomizeStats(getClient(), ii.getEquipByIdAsEquip(itemId));
                }
                reactor.getMap().spawnItemDrop(reactor, getPlayer(), drop, dropPos, false, true);
            }
            dropPos.x += 25;
        }
    }

    private List<DropEntry> getDropChances() {
        return ReactorScriptManager.getInstance().getDrops(reactor.getId());
    }

    // Summon one monster on reactor location
    public void spawnMonster(final int id) {
        spawnMonster(id, 1, getPosition());
    }

    // Summon one monster, remote location
    public void spawnMonster(final int id, final int x, final int y) {
        spawnMonster(id, 1, new Point(x, y));
    }

    // Multiple monsters, reactor location
    public void spawnMonster(final int id, final int qty) {
        spawnMonster(id, qty, getPosition());
    }

    // Multiple monsters, remote location
    public void spawnMonster(final int id, final int qty, final int x, final int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    // Handler for all spawnMonster
    private void spawnMonster(final int id, final int qty, final Point pos) {
        for (int i = 0; i < qty; ++i) {
            final MapleMonster mob = MapleLifeFactory.getMonster(id);
            reactor.getMap().spawnMonsterOnGroundBelow(mob, pos);
        }
    }

    // Returns slightly above the reactor's position for monster spawns
    public Point getPosition() {
        final Point pos = reactor.getPosition();
        pos.y -= 10;
        return pos;
    }

    /**
     * Spawns an NPC at the reactor's location
     */
    public void spawnNpc(final int npcId) {
        spawnNpc(npcId, getPosition());
    }

    /**
     * Spawns an NPC at a custom position
     */
    public void spawnNpc(final int npcId, final int x, final int y) {
        spawnNpc(npcId, new Point(x, y));
    }

    public MapleReactor getReactor() {
        return reactor;
    }

    public void spawnFakeMonster(final int id) {
        spawnFakeMonster(id, 1, getPosition());
    }

    // Summon one monster, remote location
    public void spawnFakeMonster(final int id, final int x, final int y) {
        spawnFakeMonster(id, 1, new Point(x, y));
    }

    // Multiple monsters, reactor location
    public void spawnFakeMonster(final int id, final int qty) {
        spawnFakeMonster(id, qty, getPosition());
    }

    // Multiple monsters, remote location
    public void spawnFakeMonster(final int id, final int qty, final int x, final int y) {
        spawnFakeMonster(id, qty, new Point(x, y));
    }

    // Handler for all spawnFakeMonster
    private void spawnFakeMonster(final int id, final int qty, final Point pos) {
        for (int i = 0; i < qty; ++i) {
            final MapleMonster mob = MapleLifeFactory.getMonster(id);
            reactor.getMap().spawnFakeMonsterOnGroundBelow(mob, pos);
        }
    }

    public void killAll() {
        reactor.getMap().killAllMonsters(false);
    }

    public void killMonster(final int monsId) {
        reactor.getMap().killMonster(monsId);
    }

    public void closePortal(final int mapid, final String pName) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).getPortal(pName).setPortalState(MaplePortal.CLOSE);
    }

    public void openPortal(final int mapid, final String pName) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).getPortal(pName).setPortalState(MaplePortal.OPEN);
    }

    public void closeDoor(final int mapid) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).setReactorState();
    }

    public void openDoor(final int mapid) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).resetReactors();
    }

    public void createMapMonitor(final int pMapId, final String pName) {
        final MapleMap pMap = getClient().getChannelServer().getMapFactory().getMap(pMapId);
        final MaplePortal portal = pMap.getPortal(pName);
        final BossMapMonitor bmm = new BossMapMonitor(getPlayer().getMap(), pMap, portal);
    }
}
