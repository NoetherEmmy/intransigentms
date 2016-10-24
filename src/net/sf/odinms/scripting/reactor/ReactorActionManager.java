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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ReactorActionManager extends AbstractPlayerInteraction {
    // private static final Logger log = LoggerFactory.getLogger(ReactorActionManager.class);

    private final MapleReactor reactor;

    public ReactorActionManager(MapleClient c, MapleReactor reactor) {
        super(c);
        this.reactor = reactor;
    }

    // Only used for meso = false, really. No minItems because meso is used to fill the gap.
    public void dropItems() {
        dropItems(false, 0, 0, 0, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
        dropItems(meso, mesoChance, minMeso, maxMeso, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        List<DropEntry> chances = getDropChances();
        List<DropEntry> items = new LinkedList<>();
        int numItems = 0;

        if (meso && Math.random() < (1 / (double) mesoChance)) {
            items.add(new DropEntry(0, mesoChance));
        }

        // narrow list down by chances
        Iterator<DropEntry> iter = chances.iterator();
        // for (DropEntry d : chances){
        while (iter.hasNext()) {
            DropEntry d = iter.next();
            if (Math.random() < (1 / (double) d.chance)) {
                numItems++;
                items.add(d);
            }
        }

        // if a minimum number of drops is required, add meso
        while (items.size() < minItems) {
            items.add(new DropEntry(0, mesoChance));
            numItems++;
        }

        // randomize drop order
        java.util.Collections.shuffle(items);

        final Point dropPos = reactor.getPosition();

        dropPos.x -= (12 * numItems);

        for (DropEntry d : items) {
            if (d.itemId == 0) {
                int range = maxMeso - minMeso;
                int displayDrop = (int) (Math.random() * range) + minMeso;
                int mesoDrop = displayDrop * ChannelServer.getInstance(getClient().getChannel()).getMesoRate();
                reactor.getMap().spawnMesoDrop(mesoDrop, displayDrop, dropPos, reactor, getPlayer(), meso);
            } else {
                IItem drop;
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
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

    private List<DropEntry> getDropChances() {
        return ReactorScriptManager.getInstance().getDrops(reactor.getId());
    }

    // Summon one monster on reactor location.
    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPosition());
    }

    // Summon one monster, remote location.
    public void spawnMonster(int id, int x, int y) {
        spawnMonster(id, 1, new Point(x, y));
    }

    // Multiple monsters, reactor location.
    public void spawnMonster(int id, int qty) {
        spawnMonster(id, qty, getPosition());
    }

    // Multiple monsters, remote location.
    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    // Handler for all spawnMonster.
    private void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; ++i) {
            MapleMonster mob = MapleLifeFactory.getMonster(id);
            reactor.getMap().spawnMonsterOnGroudBelow(mob, pos);
        }
    }

    // Returns slightly above the reactor's position for monster spawns.
    public Point getPosition() {
        Point pos = reactor.getPosition();
        pos.y -= 10;
        return pos;
    }

    /**
     * Spawns an NPC at the reactor's location
     * @param npcId
     */
    public void spawnNpc(int npcId) {
        spawnNpc(npcId, getPosition());
    }

    /**
     * Spawns an NPC at a custom position
     * @param npcId
     * @param x
     * @param y
     */
    public void spawnNpc(int npcId, int x, int y) {
        spawnNpc(npcId, new Point(x, y));
    }

    public MapleReactor getReactor() {
        return reactor;
    }

    public void spawnFakeMonster(int id) {
        spawnFakeMonster(id, 1, getPosition());
    }

    // Summon one monster, remote location.
    public void spawnFakeMonster(int id, int x, int y) {
        spawnFakeMonster(id, 1, new Point(x, y));
    }

    // Multiple monsters, reactor location.
    public void spawnFakeMonster(int id, int qty) {
        spawnFakeMonster(id, qty, getPosition());
    }

    // Multiple monsters, remote location.
    public void spawnFakeMonster(int id, int qty, int x, int y) {
        spawnFakeMonster(id, qty, new Point(x, y));
    }

    // Handler for all spawnFakeMonster.
    private void spawnFakeMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; ++i) {
            MapleMonster mob = MapleLifeFactory.getMonster(id);
            reactor.getMap().spawnFakeMonsterOnGroundBelow(mob, pos);
        }
    }

    public void killAll() {
        reactor.getMap().killAllMonsters(false);
    }

    public void killMonster(int monsId) {
        reactor.getMap().killMonster(monsId);
    }

    public void closePortal(int mapid, String pName) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).getPortal(pName).setPortalState(MaplePortal.CLOSE);
    }

    public void openPortal(int mapid, String pName) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).getPortal(pName).setPortalState(MaplePortal.OPEN);
    }

    public void closeDoor(int mapid) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).setReactorState();
    }

    public void openDoor(int mapid) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).resetReactors();
    }

    public void createMapMonitor(int pMapId, String pName) {
        MapleMap pMap = getClient().getChannelServer().getMapFactory().getMap(pMapId);
        MaplePortal portal = pMap.getPortal(pName);
        BossMapMonitor bmm = new BossMapMonitor(getPlayer().getMap(), pMap, portal);
    }
}
