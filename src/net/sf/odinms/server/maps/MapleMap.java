package net.sf.odinms.server.maps;

import net.sf.odinms.client.*;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.PartyQuestMapInstance;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.*;
import net.sf.odinms.server.maps.pvp.PvPLibrary;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.Direction;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.Vect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapleMap {
    private static final int MAX_OID = 20000;
    private static final List<MapleMapObjectType> rangedMapObjectTypes =
        Arrays.asList(
            MapleMapObjectType.ITEM,
            MapleMapObjectType.MONSTER,
            MapleMapObjectType.DOOR,
            MapleMapObjectType.SUMMON,
            MapleMapObjectType.REACTOR
        );
    private final Map<Integer, MapleMapObject> mapObjects = new ConcurrentHashMap<>(15, 0.7f, 2);
    private final Collection<SpawnPoint> monsterSpawn = new ArrayList<>();
    private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private final Collection<MapleCharacter> characters = new LinkedHashSet<>();
    private final Map<Integer, MaplePortal> portals = new LinkedHashMap<>();
    private final List<Rectangle> areas = new ArrayList<>();
    private MapleFootholdTree footholds = null;
    private final int mapid;
    private int runningOid = 100;
    private final int returnMapId;
    private final int channel;
    private float monsterRate;
    private boolean dropsDisabled = false;
    private boolean clock;
    private boolean boat;
    private boolean docked;
    private String mapName;
    private String streetName;
    private MapleMapEffect mapEffect = null;
    private boolean everlast = false;
    private int forcedReturnMap = 999999999;
    private int timeLimit;
    private int fieldLimit;
    private static final Logger log = LoggerFactory.getLogger(MapleMap.class);
    private MapleMapTimer mapTimer = null;
    private final int dropLife = 180000;
    private int decHP = 0;
    private int protectItem = 0;
    private boolean town;
    private boolean showGate = false;
    private final List<Pair<PeriodicMonsterDrop, ScheduledFuture<?>>> periodicMonsterDrops = new ArrayList<>(3);
    private final List<DynamicSpawnWorker> dynamicSpawnWorkers = new ArrayList<>(2);
    private static final Map<Integer, Integer> lastLatanicaTimes = new ConcurrentHashMap<>(5, 0.85f, 1);
    private PartyQuestMapInstance partyQuestInstance = null;
    private ScheduledFuture<?> respawnWorker = null;
    private Set<FieldLimit> fieldLimits;
    private boolean damageMuted = false;
    private ScheduledFuture<?> damageMuteCancelTask = null;
    private ScheduledFuture<?> damageMuteHintTask = null;

    public MapleMap(int mapid, int channel, int returnMapId, float monsterRate) {
        this.mapid = mapid;
        this.channel = channel;
        this.returnMapId = returnMapId;
        if (monsterRate > 0.0f) {
            this.monsterRate = monsterRate;
            boolean greater1 = monsterRate > 1.0f;
            this.monsterRate = Math.abs(1.0f - this.monsterRate);
            this.monsterRate = this.monsterRate / 2.0f;
            if (greater1) {
                this.monsterRate = 1.0f + this.monsterRate;
            } else {
                this.monsterRate = 1.0f - this.monsterRate;
            }
            respawnWorker = TimerManager.getInstance().register(new RespawnWorker(), 5000);
        }
    }

    public static int timeSinceLastLatanica(int channel) {
        synchronized (lastLatanicaTimes) {
            return (int) (System.currentTimeMillis() / 1000 - (lastLatanicaTimes.getOrDefault(channel, 0)));
        }
    }

    public static void updateLastLatanica(int channel) {
        synchronized (lastLatanicaTimes) {
            lastLatanicaTimes.put(channel, (int) (System.currentTimeMillis() / 1000L));
        }
    }

    public enum FieldLimit {
        MOVE_LIMIT(0x01),
        SKILL_LIMIT(0x02),
        SUMMON_LIMIT(0x04),
        MYSTIC_DOOR_LIMIT(0x08),
        MIGRATE_LIMIT(0x10),
        PORTAL_SCROLL_LIMIT(0x40),
        MINIGAME_LIMIT(0x80),
        SPECIFIC_PORTAL_SCROLL_LIMIT(0x100),
        TAMING_MOB_LIMIT(0x200),
        STAT_CHANGE_ITEM_CONSUME_LIMIT(0x400),
        PARTY_BOSS_CHANGE_LIMIT(0x800),
        NO_MOB_CAPACITY_LIMIT(0x1000),
        WEDDING_INVITATION_LIMIT(0x2000),
        CASH_WEATHER_CONSUME_LIMIT(0x4000),
        NO_PET(0x8000),
        ANTI_MACRO_LIMIT(0x10000),
        FALL_DOWN_LIMIT(0x20000),
        SUMMON_NPC_LIMIT(0x40000),
        NO_EXP_DECREASE(0x80000),
        NO_DAMAGE_ON_FALLING(0x100000),
        PARCEL_OPEN_LIMIT(0x200000),
        DROP_LIMIT(0x400000),
        ROCKETBOOSTER_LIMIT(0x800000); // No mechanics for this
        private final int i;

        FieldLimit(int i) {
            this.i = i;
        }

        public int getValue() {
            return i;
        }

        public static FieldLimit getByValue(int value) {
            for (FieldLimit fl : FieldLimit.values()) {
                if (fl.getValue() == value) {
                    return fl;
                }
            }
            return null;
        }
    }

    public void setFieldLimit(int fieldLimit) {
        this.fieldLimit = fieldLimit;
        if (fieldLimit == 0) {
            fieldLimits = Collections.emptySet();
            return;
        }
        fieldLimits = Stream.of(FieldLimit.values())
                            .filter(fl -> (fieldLimit & fl.getValue()) > 0)
                            .collect(Collectors.toSet());
    }

    public int getFieldLimit() {
        return fieldLimit;
    }

    public Collection<FieldLimit> getFieldLimits() {
        return Collections.unmodifiableSet(fieldLimits);
    }

    public boolean hasFieldLimit(FieldLimit fl) {
        return fieldLimits.contains(fl);
    }

    public void setDamageMuted(boolean muted, final long duration) {
        if (damageMuteCancelTask != null && !damageMuteCancelTask.isDone()) {
            damageMuteCancelTask.cancel(false);
            damageMuteCancelTask = null;
        }
        if (damageMuteHintTask != null && !damageMuteHintTask.isDone()) {
            damageMuteHintTask.cancel(false);
            damageMuteHintTask = null;
        }
        TimerManager tMan = TimerManager.getInstance();
        damageMuted = muted;

        if (duration > 0L) {
            if (muted) {
                final long startTime = System.currentTimeMillis();
                damageMuteHintTask = tMan.register(() -> {
                    long remainingTime = duration - (System.currentTimeMillis() - startTime);
                    final int remainingSeconds = (int) (remainingTime / 1000);
                    getCharacters().forEach(p ->
                        p.sendHint("All damage muted: #b#e" + remainingSeconds + "#n sec.#k")
                    );
                }, 1000L, 0L);
            }

            damageMuteCancelTask = tMan.schedule(() -> setDamageMuted(false, -1L), duration);
        }
    }

    public boolean isDamageMuted() {
        return damageMuted;
    }

    public void restartRespawnWorker() {
        if (respawnWorker != null) respawnWorker.cancel(false);
        respawnWorker = TimerManager.getInstance().register(new RespawnWorker(), 5000);
    }

    public void toggleDrops() {
        dropsDisabled = !dropsDisabled;
    }

    public void setDropsDisabled(boolean dd) {
        dropsDisabled = dd;
    }

    public int getId() {
        return mapid;
    }

    public MapleMap getReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(returnMapId);
    }

    public int getReturnMapId() {
        return returnMapId;
    }

    public int getForcedReturnId() {
        return forcedReturnMap;
    }

    public MapleMap getForcedReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(forcedReturnMap);
    }

    public void setForcedReturnMap(int map) {
        this.forcedReturnMap = map;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getCurrentPartyId() {
        for (MapleCharacter chr : this.getCharacters()) {
            if (chr.getPartyId() != -1) {
                return chr.getPartyId();
            }
        }
        return -1;
    }

    public void addMapObject(MapleMapObject mapobject) {
        synchronized (this.mapObjects) {
            mapobject.setObjectId(runningOid);
            this.mapObjects.put(runningOid, mapobject);
            incrementRunningOid();
        }
    }

    private void spawnAndAddRangedMapObject(MapleMapObject mapobject, DelayedPacketCreation packetbakery, SpawnCondition condition) {
        synchronized (mapObjects) {
            mapobject.setObjectId(runningOid);
            synchronized (characters) {
                for (MapleCharacter chr : characters) {
                    if (condition == null || condition.canSpawn(chr)) {
                        if (chr.getPosition().distanceSq(mapobject.getPosition()) <= MapleCharacter.MAX_VIEW_RANGE_SQ && !chr.isFake()) {
                            packetbakery.sendPackets(chr.getClient());
                            chr.addVisibleMapObject(mapobject);
                        }
                    }
                }
            }
            mapObjects.put(runningOid, mapobject);
            incrementRunningOid();
        }
    }

    private void incrementRunningOid() {
        runningOid++;
        synchronized(this.mapObjects) {
            for (int numIncrements = 1; numIncrements < MAX_OID; ++numIncrements) {
                if (runningOid > MAX_OID) {
                    runningOid = 100;
                }
                if (this.mapObjects.containsKey(runningOid)) {
                    runningOid++;
                } else {
                    return;
                }
            }
        }
        throw new RuntimeException("Out of OIDs on map " + mapid + " (channel: " + channel + ")");
    }

    public void removeMapObject(int num) {
        synchronized (this.mapObjects) {
            this.mapObjects.remove(num);
        }
    }

    public void removeMapObject(MapleMapObject obj) {
        removeMapObject(obj.getObjectId());
    }

    private Point calcPointBelow(Point initial) {
        MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            double s1 = Math.abs(fh.getY2() - fh.getY1());
            double s2 = Math.abs(fh.getX2() - fh.getX1());
            double s4 = Math.abs(initial.x - fh.getX1());
            double alpha = Math.atan(s2 / s1);
            double beta = Math.atan(s1 / s2);
            double s5 = Math.cos(alpha) * (s4 / Math.cos(beta));
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) s5;
            } else {
                dropY = fh.getY1() + (int) s5;
            }
        }
        return new Point(initial.x, dropY);
    }

    private Point calcDropPos(Point initial, Point fallback) {
        Point ret = calcPointBelow(new Point(initial.x, initial.y - 99));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    private void dropFromMonster(MapleCharacter dropOwner, MapleMonster monster) {
        if (dropsDisabled || monster.dropsDisabled()) {
            return;
        }
        boolean partyevent = false;
        int maxDrops;
        final boolean explosive = monster.isExplosive();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ChannelServer cserv = ChannelServer.getInstance(channel);
        if (explosive) {
            maxDrops = 10 * cserv.getBossDropRate();
        } else {
            maxDrops = 4 * cserv.getDropRate();
        }
        List<Integer> toDrop = new ArrayList<>();
        for (int i = 0; i < maxDrops; ++i) {
            toDrop.add(monster.getDrop());
        }
        //
        if (dropOwner != null && dropOwner.getEventInstance() == null && dropOwner.getPartyQuest() == null) {
            int chance = (int) (Math.random() * 150.0d); // 1/150 droprate
            if (chance < 1) {
                toDrop.add(4001126); // Maple leaf
            }
            chance = (int) (Math.random() * 150.0d); // 1/150 droprate
            if (chance < 1) {
                toDrop.add(5072000); // Super megaphone
            }
            if (dropOwner.getMapId() == 2000) { // Pirate 2nd job adv. map
                chance = (int) (Math.random() * 3.0d); // 1/3 droprate
                if (chance < 1) {
                    toDrop.add(4031013); // Dark marble
                }
            } else if (dropOwner.getQuest(MapleQuest.getInstance(7104 /* A Piece of Crack */)).getStatus() == MapleQuestStatus.Status.STARTED && (monster.getId() == 8141100 || monster.getId() == 8143000)) {
                chance = (int) (Math.random() * 180.0d); // 1/180 droprate
                switch (chance) {
                    case 1:
                        toDrop.add(4031176); // Piece of Cracked Dimension A
                        break;
                    case 2:
                        toDrop.add(4031177); // Piece of Cracked Dimension B
                        break;
                    case 3:
                        toDrop.add(4031178); // Piece of Cracked Dimension C
                        break;
                    default:
                        break;
                }
            } else if (dropOwner.getMapId() == 4000 && monster.getId() == 9300101) { // Taming Hog map
                if (Math.random() < 0.5d) {
                    toDrop.add(4031507);
                } else {
                    toDrop.add(4031508);
                }
            }
            if (dropOwner.getMapId() >= 1000 && dropOwner.getMapId() <= 1006 && monster.isBoss()) {
                int timelessItems[] =
                {
                    1302081, 1312037, 1322060, 1332073, 1332074, 1372044, 1382057, 1402046,
                    1412033, 1422037, 1432047, 1442063, 1452057, 1462050, 1472068, 1482023,
                    1492023
                };
                if (Math.random() < 0.15d) {
                    toDrop.add(timelessItems[(int) (Math.random() * timelessItems.length)]);
                }
            } else if (dropOwner.getMapId() >= 5000 && monster.getId() == 8200000) {
                toDrop.removeIf(i -> i.equals(4031303));
            }
            if (partyevent && dropOwner.getParty() != null) {
                chance = (int) (Math.random() * 112.0d); // 1/112 droprate
                if (chance == 61) { // Arbitrary
                    switch (Math.min(monster.dropShareCount.get(), 6)) {
                        case 1:
                            toDrop.add(4031439);
                            break;
                        case 2:
                            toDrop.add(4031440);
                            break;
                        case 3:
                            toDrop.add(4031441);
                            break;
                        case 4:
                            toDrop.add(4031442);
                            break;
                        case 5:
                            toDrop.add(4031443);
                            break;
                        case 6:
                            toDrop.add(4031443);
                            toDrop.add(4031439);
                            break;
                        default:
                            break;
                    }
                }
            }
            if (monster.getId() == 9500196) {
                chance = (int) (Math.random() * 5.0d); // 1/5 droprate
                if (chance == 2) { // Arbitrary
                    toDrop.add(4031203);
                }
            }
        }
        //
        Set<Integer> alreadyDropped = new HashSet<>();
        byte htpendants = 0, htstones = 0, mesos = 0;
        for (int i = 0; i < toDrop.size(); ++i) {
            if (toDrop.get(i) == -1) {
                if (!isPQMap()) {
                    if (alreadyDropped.contains(-1)) {
                        if (!explosive) {
                            toDrop.remove(i);
                            i--;
                        } else {
                            if (mesos < 9) {
                                mesos++;
                            } else {
                                toDrop.remove(i);
                                i--;
                            }
                        }
                    } else {
                        alreadyDropped.add(-1);
                    }
                }
            } else {
                if (alreadyDropped.contains(toDrop.get(i)) && !explosive) {
                    toDrop.remove(i);
                    i--;
                } else {
                    if (toDrop.get(i) == 2041200) { // Stone
                        if (htstones > 2) {
                            toDrop.remove(i);
                            i--;
                            continue;
                        } else {
                            htstones++;
                        }
                    } else if (toDrop.get(i) == 1122000) { // Pendant
                        if (htstones > 2) {
                            toDrop.remove(i);
                            i--;
                            continue;
                        } else {
                            htpendants++;
                        }
                    }
                    alreadyDropped.add(toDrop.get(i));
                }
            }
        }
        if (toDrop.size() > maxDrops) {
            toDrop = toDrop.subList(0, maxDrops);
        }
        if (mesos < 7 && explosive) {
            for (int i = mesos; i < 7; ++i) {
                toDrop.add(-1);
            }
        }
        int shiftDirection = 0;
        int shiftCount = 0;
        int curX = Math.min(Math.max(monster.getPosition().x - 25 * (toDrop.size() / 2), footholds.getMinDropX() + 25), footholds.getMaxDropX() - toDrop.size() * 25);
        int curY = Math.max(monster.getPosition().y, footholds.getY1());
        while (shiftDirection < 3 && shiftCount < 1000) {
            if (shiftDirection == 1) {
                curX += 25;
            } else if (shiftDirection == 2) {
                curX -= 25;
            }
            for (int i = 0; i < toDrop.size(); ++i) {
                MapleFoothold wall = footholds.findWall(new Point(curX, curY), new Point(curX + toDrop.size() * 25, curY));
                if (wall != null) {
                    if (wall.getX1() < curX) {
                        shiftDirection = 1;
                        shiftCount++;
                        break;
                    } else if (wall.getX1() == curX) {
                        if (shiftDirection == 0) {
                            shiftDirection = 1;
                        }
                        shiftCount++;
                        break;
                    } else {
                        shiftDirection = 2;
                        shiftCount++;
                        break;
                    }
                } else if (i == toDrop.size() - 1) {
                    shiftDirection = 3;
                }
                final Point dropPos = calcDropPos(new Point(curX + i * 25, curY), new Point(monster.getPosition()));
                final int drop = toDrop.get(i);
                if (drop == -1) {
                    if (monster.isBoss()) {
                        final int cc = cserv.getMesoRate() + 25;
                        final MapleMonster dropMonster = monster;
                        final Random r = new Random();
                        double mesoDecrease = Math.pow(0.93d, (double) monster.getExp() / 300.0d);
                        if (mesoDecrease > 1.0d) {
                            mesoDecrease = 1.0d;
                        } else if (mesoDecrease < 0.001d) {
                            mesoDecrease = 0.005d;
                        }
                        int tempmeso = Math.min(30000, (int) (mesoDecrease * (double) monster.getExp() * (1.0d + (double) r.nextInt(20)) / 10.0d));
                        if (dropOwner != null && dropOwner.getBuffedValue(MapleBuffStat.MESOUP) != null) {
                            tempmeso = (int) ((double) tempmeso * dropOwner.getBuffedValue(MapleBuffStat.MESOUP).doubleValue() / 100.0d);
                        }
                        final int dmesos = tempmeso;
                        if (dmesos > 0) {
                            final MapleCharacter dropChar = dropOwner;
                            final boolean publicLoot = this.isPQMap();
                            TimerManager.getInstance().schedule(() ->
                                spawnMesoDrop(
                                    dmesos * cc,
                                    dmesos,
                                    dropPos,
                                    dropMonster,
                                    dropChar,
                                    explosive || publicLoot || dropChar == null
                                ), monster.getAnimationTime("die1")
                            );
                        }
                    } else {
                        final int mesoRate = cserv.getMesoRate();
                        final Random r = new Random();
                        double mesoDecrease = Math.pow(0.93d, monster.getExp() / 300.0d);
                        if (mesoDecrease > 1.0d) {
                            mesoDecrease = 1.0d;
                        }
                        int tempmeso = Math.min(30000, (int) (mesoDecrease * monster.getExp() * (1.0d + r.nextInt(20)) / 10.0d));
                        if (dropOwner != null && dropOwner.getBuffedValue(MapleBuffStat.MESOUP) != null) {
                            tempmeso = (int) (tempmeso * dropOwner.getBuffedValue(MapleBuffStat.MESOUP).doubleValue() / 100.0d);
                        }
                        final int meso = tempmeso;
                        if (meso > 0) {
                            final MapleMonster dropMonster = monster;
                            final MapleCharacter dropChar = dropOwner;
                            final boolean publicLoot = isPQMap();
                            TimerManager.getInstance().schedule(() ->
                                spawnMesoDrop(
                                    meso * mesoRate,
                                    meso,
                                    dropPos,
                                    dropMonster,
                                    dropChar,
                                    explosive || publicLoot || dropChar == null
                                ), monster.getAnimationTime("die1")
                            );
                        }
                    }
                } else {
                    IItem idrop;
                    MapleInventoryType type = ii.getInventoryType(drop);
                    if (type.equals(MapleInventoryType.EQUIP)) {
                        MapleClient c = null;
                        if (dropOwner == null && playerCount() > 0) {
                            c = ((MapleCharacter) getAllPlayers().get(0)).getClient();
                        } else if (dropOwner != null) {
                            c = dropOwner.getClient();
                        }
                        if (c != null) {
                            idrop = ii.randomizeStats(c, (Equip) ii.getEquipById(drop));
                        } else {
                            idrop = ii.getEquipById(drop);
                        }
                    } else {
                        idrop = new Item(drop, (byte) 0, (short) 1);
                        if ((ii.isArrowForBow(drop) || ii.isArrowForCrossBow(drop)) && dropOwner != null) {
                            if (dropOwner.getJob().getId() / 100 == 3) {
                                idrop.setQuantity((short) (1.0d + 100.0d * Math.random()));
                            }
                        } else if (ii.isThrowingStar(drop) || ii.isBullet(drop)) {
                            idrop.setQuantity((short) 1);
                        }
                    }
                    final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, monster, dropOwner);
                    final MapleMapObject dropMonster = monster;
                    final MapleCharacter dropChar = dropOwner;
                    final TimerManager tMan = TimerManager.getInstance();
                    tMan.schedule(() -> {
                        spawnAndAddRangedMapObject(mdrop, c ->
                            c.getSession()
                             .write(MaplePacketCreator.dropItemFromMapObject(
                                 drop,
                                 mdrop.getObjectId(),
                                 dropMonster.getObjectId(),
                                 (explosive || dropOwner == null) ? 0 : dropChar.getId(),
                                 dropMonster.getPosition(),
                                 dropPos,
                                 (byte) 1)
                             ), null);
                        tMan.schedule(new ExpireMapItemJob(mdrop), dropLife);
                    }, monster.getAnimationTime("die1"));
                }
            }
        }
    }

    public void startPeriodicMonsterDrop(MapleMonster monster, long period, long duration) {
        if (playerCount() > 0) {
            startPeriodicMonsterDrop((MapleCharacter) getAllPlayers().get(0), monster, period, duration);
        } else {
            startPeriodicMonsterDrop(null, monster, period, duration);
        }
    }

    public void startPeriodicMonsterDrop(MapleCharacter chr, MapleMonster monster, long period, long duration) {
        TimerManager timerManager = TimerManager.getInstance();

        final PeriodicMonsterDrop pmd = new PeriodicMonsterDrop(chr, monster);
        final ScheduledFuture<?> dropTask = timerManager.register(pmd, period, period);
        pmd.setTask(dropTask);
        final Runnable cancelTask = () -> dropTask.cancel(false);
        final ScheduledFuture<?> schedule = timerManager.schedule(cancelTask, duration);
        addPeriodicMonsterDrop(pmd, schedule);
    }

    private void addPeriodicMonsterDrop(PeriodicMonsterDrop pmd, ScheduledFuture<?> cancelTask) {
        periodicMonsterDrops.add(new Pair<>(pmd, cancelTask));
    }

    public DynamicSpawnWorker registerDynamicSpawnWorker(int monsterId, Point spawnPoint, int period) {
        final DynamicSpawnWorker dsw = new DynamicSpawnWorker(monsterId, spawnPoint, period);
        dynamicSpawnWorkers.add(dsw);
        return dsw;
    }

    public DynamicSpawnWorker registerDynamicSpawnWorker(int monsterId, Rectangle spawnArea, int period) {
        final DynamicSpawnWorker dsw = new DynamicSpawnWorker(monsterId, spawnArea, period);
        dynamicSpawnWorkers.add(dsw);
        return dsw;
    }

    public DynamicSpawnWorker registerDynamicSpawnWorker(int monsterId, Point spawnPoint, int period, int duration, boolean putPeriodicMonsterDrops, int monsterDropPeriod) {
        final DynamicSpawnWorker dsw = new DynamicSpawnWorker(monsterId, spawnPoint, period, duration, putPeriodicMonsterDrops, monsterDropPeriod);
        dynamicSpawnWorkers.add(dsw);
        return dsw;
    }

    public DynamicSpawnWorker registerDynamicSpawnWorker(int monsterId, Rectangle spawnArea, int period, int duration, boolean putPeriodicMonsterDrops, int monsterDropPeriod) {
        final DynamicSpawnWorker dsw = new DynamicSpawnWorker(monsterId, spawnArea, period, duration, putPeriodicMonsterDrops, monsterDropPeriod);
        dynamicSpawnWorkers.add(dsw);
        return dsw;
    }

    public DynamicSpawnWorker registerDynamicSpawnWorker(int monsterId, Rectangle spawnArea, int period, int duration, boolean putPeriodicMonsterDrops, int monsterDropPeriod, MapleMonsterStats overrideStats) {
        final DynamicSpawnWorker dsw = new DynamicSpawnWorker(monsterId, spawnArea, period, duration, putPeriodicMonsterDrops, monsterDropPeriod, overrideStats);
        dynamicSpawnWorkers.add(dsw);
        return dsw;
    }

    public DynamicSpawnWorker getDynamicSpawnWorker(int index) {
        return dynamicSpawnWorkers.get(index);
    }

    public Set<DynamicSpawnWorker> getDynamicSpawnWorkersByMobId(int mobId) {
        return dynamicSpawnWorkers.stream().filter(dsw -> dsw.getMonsterId() == mobId).collect(Collectors.toSet());
    }

    public void disposeDynamicSpawnWorker(int index) {
        final DynamicSpawnWorker dsw = dynamicSpawnWorkers.get(index);
        if (dsw != null) {
            dsw.dispose();
            dynamicSpawnWorkers.remove(index);
        }
    }

    public void disposeDynamicSpawnWorker(DynamicSpawnWorker dsw) {
        dsw.dispose();
        dynamicSpawnWorkers.remove(dsw);
    }

    public void disposeAllDynamicSpawnWorkers() {
        dynamicSpawnWorkers.forEach(DynamicSpawnWorker::dispose);
        dynamicSpawnWorkers.clear();
    }

    public int dynamicSpawnWorkerCount() {
        return dynamicSpawnWorkers.size();
    }

    public List<DynamicSpawnWorker> readDynamicSpawnWorkers() {
        return new ArrayList<>(dynamicSpawnWorkers);
    }

    public boolean damageMonster(MapleCharacter chr, final MapleMonster monster, int damage) {
        boolean withDrops = true;
        if (monster.getId() == 9500196) { // Ghost
            damage = 1;
            withDrops = false;
        }
        if (monster.getId() == 8800000) {
            Collection<MapleMapObject> objects = chr.getMap().getMapObjects();
            for (MapleMapObject object : objects) {
                if (object == null) {
                    continue;
                }
                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        return true;
                    }
                }
            }
        }
        if (monster.isAlive()) {
            synchronized (monster) {
                if (!monster.isAlive()) {
                    return false;
                }
                if (damage > 0) {
                    int monsterhp = monster.getHp();
                    monster.damage(chr, damage, true);
                    if (!monster.isAlive()) {
                        killMonster(monster, chr, withDrops);
                        if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {
                            Collection<MapleMapObject> objects = chr.getMap().getMapObjects();
                            for (MapleMapObject object : objects) {
                                if (object == null) {
                                    continue;
                                }
                                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                                if (mons != null) {
                                    if (mons.getId() == 8810018) {
                                        damageMonster(chr, mons, monsterhp);
                                    }
                                }
                            }
                        }
                    } else {
                        if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {
                            Collection<MapleMapObject> objects = chr.getMap().getMapObjects();
                            for (MapleMapObject object : objects) {
                                if (object == null) {
                                    continue;
                                }
                                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                                if (mons != null) {
                                    if (mons.getId() == 8810018) {
                                        damageMonster(chr, mons, damage);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops) {
        killMonster(monster, chr, withDrops, false, 1);
    }

    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean secondTime) {
        killMonster(monster, chr, withDrops, secondTime, 1);
    }

    @SuppressWarnings("static-access")
    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean secondTime, int animation) {
        monster.stopOtherMobHitChecking();
        monster.getMap()
               .getAllPlayers()
               .stream()
               .map(mmo -> (MapleCharacter) mmo)
               .filter(p -> p.isGM() && p.doShowDpm())
               .forEach(p -> {
                   DecimalFormat df = new DecimalFormat("#.000");
                   p.dropMessage(
                       monster.getName() +
                           ", oid: " +
                           monster.getObjectId() +
                           ", death DPM: " +
                           df.format(monster.avgIncomingDpm())
                   );
               });
        if (monster.getId() == 8810018 && !secondTime) {
            TimerManager.getInstance().schedule(() -> {
                killMonster(monster, chr, withDrops, true, 1);
                killAllMonsters(false);
            }, 3000);
            return;
        }
        if (monster.getBuffToGive() > -1) {
            broadcastMessage(MaplePacketCreator.showOwnBuffEffect(monster.getBuffToGive(), 11));
            MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
            MapleStatEffect statEffect = mii.getItemEffect(monster.getBuffToGive());
            synchronized (characters) {
                for (MapleCharacter character : characters) {
                    if (character.isAlive()) {
                        statEffect.applyTo(character);
                        broadcastMessage(
                            MaplePacketCreator.showBuffeffect(
                                character.getId(),
                                monster.getBuffToGive(),
                                11,
                                (byte) 1
                            )
                        );
                    }
                }
            }
        }
        if (monster.getId() == 8810018) {
            try {
                chr.getClient()
                   .getChannelServer()
                   .getWorldInterface()
                   .broadcastMessage(
                       chr.getName(),
                       MaplePacketCreator.serverNotice(
                           6,
                           "To those of the crew that have finally conquered Horned Tail after numerous attempts, " +
                               "I salute thee! You are the true heroes of Leafre!"
                       ).getBytes()
                   );
            } catch (RemoteException e) {
                chr.getClient().getChannelServer().reconnectWorld();
            }
        }
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), animation), monster.getPosition());
        removeMapObject(monster);
        if (monster.getId() >= 8800003 && monster.getId() <= 8800010) {
            boolean makeZakReal = true;
            Collection<MapleMapObject> objects = getMapObjects();
            for (MapleMapObject object : objects) {
                MapleMonster mons = getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        makeZakReal = false;
                    }
                }
            }
            if (makeZakReal) {
                for (MapleMapObject object : objects) {
                    MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                    if (mons != null) {
                        if (mons.getId() == 8800000) {
                            makeMonsterReal(mons);
                            updateMonsterController(mons);
                        }
                    }
                }
            }
        }
        MapleCharacter dropOwner = monster.killBy(chr);
        if (withDrops && !monster.dropsDisabled()) {
            if (dropOwner == null) {
                dropOwner = chr;
            }
            dropFromMonster(dropOwner, monster);
        }
        if (hasPeriodicMonsterDrop(monster.getObjectId())) {
            cancelPeriodicMonsterDrops(monster.getObjectId());
        }
    }

    public void killAllMonsters(boolean drop) {
        List<MapleMapObject> players = null;
        if (drop) {
            players = getAllPlayers();
        }
        List<MapleMapObject> monsters =
            getMapObjectsInRange(
                new Point(0, 0),
                Double.POSITIVE_INFINITY,
                Collections.singletonList(MapleMapObjectType.MONSTER)
            );
        for (MapleMapObject monstermo : monsters) {
            MapleMonster monster = (MapleMonster) monstermo;
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
            removeMapObject(monster);
            if (drop && players != null) {
                int random = (int) (Math.random() * players.size());
                dropFromMonster((MapleCharacter) players.get(random), monster);
            }
        }
    }

    public void killMonster(int monsId) {
        for (MapleMapObject mmo : getMapObjects()) {
            if (mmo instanceof MapleMonster) {
                if (((MapleMonster) mmo).getId() == monsId) {
                    this.killMonster((MapleMonster) mmo, (MapleCharacter) getAllPlayers().get(0), false);
                }
            }
        }
    }

    public void silentKillMonster(int oid) {
        MapleMonster monster = getMonsterByOid(oid);
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
        removeMapObject(monster);
    }

    public List<MapleMapObject> getAllPlayers() {
        return getMapObjectsInRange(
            new Point(0, 0),
            Double.POSITIVE_INFINITY,
            Collections.singletonList(MapleMapObjectType.PLAYER)
        );
    }

    public List<MapleMonster> getAllMonsters() {
        return getMapObjectsInRange(
            new Point(0, 0),
            Double.POSITIVE_INFINITY,
            Collections.singletonList(MapleMapObjectType.MONSTER)
        )
        .stream()
        .map(mmo -> (MapleMonster) mmo)
        .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<MapleReactor> getAllReactors() {
        return getMapObjectsInRange(
            new Point(0, 0),
            Double.POSITIVE_INFINITY,
            Collections.singletonList(MapleMapObjectType.REACTOR)
        )
        .stream()
        .map(mmo -> (MapleReactor) mmo)
        .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<MapleNPC> getAllNPCs() {
        return getMapObjectsInRange(
            new Point(0, 0),
            Double.POSITIVE_INFINITY,
            Collections.singletonList(MapleMapObjectType.NPC)
        )
        .stream()
        .map(mmo -> (MapleNPC) mmo)
        .collect(Collectors.toCollection(ArrayList::new));
    }

    public MapleNPC getNPCById(int npcId) {
        return getMapObjectsInRange(
            new Point(0, 0),
            Double.POSITIVE_INFINITY,
            Collections.singletonList(MapleMapObjectType.NPC)
        )
        .stream()
        .map(mmo -> (MapleNPC) mmo)
        .filter(npc -> npc.getId() == npcId)
        .findAny()
        .orElse(null);
    }

    public void destroyReactor(int oid) {
        synchronized (mapObjects) {
            final MapleReactor reactor = getReactorByOid(oid);
            if (reactor == null) return;
            TimerManager tMan = TimerManager.getInstance();
            broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
            reactor.setAlive(false);
            removeMapObject(reactor);
            reactor.setTimerActive(false);
            if (reactor.getDelay() > 0) {
                tMan.schedule(() -> respawnReactor(reactor), reactor.getDelay());
            }
        }
    }

    public void removeReactor(int oid) {
        synchronized (mapObjects) {
            final MapleReactor reactor = getReactorByOid(oid);
            if (reactor == null) return;
            broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
            reactor.setAlive(false);
            removeMapObject(reactor);
            reactor.setTimerActive(false);
        }
    }

    public void resetReactors() {
        synchronized (mapObjects) {
            Iterator<MapleMapObject> mmoiter = this.mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject o = mmoiter.next();
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    ((MapleReactor) o).setState((byte) 0);
                    ((MapleReactor) o).setTimerActive(false);
                    broadcastMessage(MaplePacketCreator.triggerReactor((MapleReactor) o, 0));
                }
            }
        }
    }

    public void shuffleReactors() {
        List<Point> points = new ArrayList<>();
        synchronized (mapObjects) {
            Iterator<MapleMapObject> mmoiter1 = mapObjects.values().iterator();
            while (mmoiter1.hasNext()) {
                MapleMapObject o = mmoiter1.next();
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    points.add(o.getPosition());
                }
            }
            Collections.shuffle(points);
            Iterator<MapleMapObject> mmoiter2 = mapObjects.values().iterator();
            while (mmoiter2.hasNext()) {
                MapleMapObject o = mmoiter2.next();
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    o.setPosition(points.remove(points.size() - 1));
                }
            }
        }
    }

    public void updateMonsterController(final MapleMonster monster) {
        synchronized (monster) {
            if (!monster.isAlive()) {
                return;
            }

            if (monster.getController() != null) {
                if (monster.getController().getMap() != this) {
                    log.warn("Monstercontroller wasn't on same map");
                    monster.getController().stopControllingMonster(monster);
                } else {
                    return;
                }
            }

            int mincontrolled = -1;
            MapleCharacter newController = null;
            synchronized (characters) {
                for (MapleCharacter chr : characters) {
                    if (!chr.isHidden() && (chr.getControlledMonsters().size() < mincontrolled || mincontrolled == -1)) {
                        mincontrolled = chr.getControlledMonsters().size();
                        newController = chr;
                    }
                }
            }

            if (newController != null) {
                if (monster.isFirstAttack()) {
                    newController.controlMonster(monster, true);
                    monster.setControllerHasAggro(true);
                    monster.setControllerKnowsAboutAggro(true);
                } else {
                    newController.controlMonster(monster, false);
                }
            }
        }
    }

    public Collection<MapleMapObject> getMapObjects() {
        List<MapleMapObject> ret = new ArrayList<>();
        synchronized (mapObjects) {
            Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                ret.add(mmoiter.next());
            }
        }
        return ret;
    }

    public boolean containsNPC(int npcid) {
        synchronized (mapObjects) {
            Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject obj = mmoiter.next();
                if (obj.getType() == MapleMapObjectType.NPC) {
                    if (((MapleNPC) obj).getId() == npcid) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public MapleMapObject getMapObject(int oid) {
        synchronized (mapObjects) {
            return mapObjects.get(oid);
        }
    }

    public MapleMonster getMonsterByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid);
        if (mmo == null) {
            return null;
        }
        if (mmo.getType() == MapleMapObjectType.MONSTER) {
            return (MapleMonster) mmo;
        }
        return null;
    }

    public MapleReactor getReactorByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid);
        if (mmo == null) {
            return null;
        }
        if (mmo.getType() == MapleMapObjectType.REACTOR) {
            return (MapleReactor) mmo;
        }
        return null;
    }

    public MapleReactor getReactorByName(String name) {
        synchronized (mapObjects) {
            Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject obj = mmoiter.next();
                if (obj.getType() == MapleMapObjectType.REACTOR) {
                    if (((MapleReactor) obj).getName().equals(name)) {
                        return (MapleReactor) obj;
                    }
                }
            }
        }
        return null;
    }

    public void spawnMonsterOnGroudBelow(MapleMonster mob, Point pos) {
        spawnMonsterOnGroundBelow(mob, pos);
    }

    public void spawnMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = getGroundBelow(pos);
        int dy = 1;
        int tries = 0;
        while (spos == null && tries < 20) {
            pos.translate(0, dy);
            spos = getGroundBelow(pos);
            dy *= -2;
            tries++;
        }
        mob.setPosition(spos);
        spawnMonster(mob);
    }

    public void spawnFakeMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = getGroundBelow(pos);
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    public Point getGroundBelow(Point pos) {
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        if (spos != null) {
            spos.y -= 1;
        }
        return spos;
    }

    public void spawnRevives(final MapleMonster monster) {
        monster.setMap(this);
        synchronized (this.mapObjects) {
            spawnAndAddRangedMapObject(monster, c -> c.getSession().write(MaplePacketCreator.spawnMonster(monster, false)), null);
            updateMonsterController(monster);
        }
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnMonster(final MapleMonster monster) {
        monster.setMap(this);
        synchronized (mapObjects) {
            spawnAndAddRangedMapObject(monster, c -> {
                c.getSession().write(MaplePacketCreator.spawnMonster(monster, true));
                if (monster.getId() == 9300166) {
                    TimerManager.getInstance().schedule(() ->
                        killMonster(
                            monster,
                            (MapleCharacter) getAllPlayers().get(0),
                            false,
                            false,
                            3
                        ), new Random().nextInt(5000)
                    );
                } else if (monster.getId() == 9500196) {
                    monster.startOtherMobHitChecking(() -> {
                        final MapleMap map = monster.getMap();
                        MapleCharacter damager = monster.getController();
                        if (damager == null && map.playerCount() > 0) {
                            damager = (MapleCharacter) map.getAllPlayers().get(0);
                        }
                        if (damager != null) {
                            map.broadcastMessage(MaplePacketCreator.damageMonster(monster.getObjectId(), 1));
                            map.damageMonster(damager, monster, 1);
                        }
                    }, 800, 750);
                    startPeriodicMonsterDrop(monster.getController() != null ? monster.getController()
                                           : monster.getMap().playerCount() > 0 ? (MapleCharacter) getAllPlayers().get(0)
                                           : null, monster, 2500, 125000);
                }
            }, null);
            updateMonsterController(monster);
        }
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos) {
        try {
            monster.setMap(this);
            Point spos = new Point(pos.x, pos.y - 1);
            spos = calcPointBelow(spos);
            spos.y -= 1;
            monster.setPosition(spos);
            monster.disableDrops();
            synchronized (this.mapObjects) {
                spawnAndAddRangedMapObject(monster, c ->
                    c.getSession().write(
                        MaplePacketCreator.spawnMonster(
                            monster,
                            true,
                            effect
                        )
                    ), null
                );
                updateMonsterController(monster);
            }
            spawnedMonstersOnMap.incrementAndGet();
        } catch (Exception ignored) {
        }
    }

    public void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);
        synchronized (this.mapObjects) {
            spawnAndAddRangedMapObject(monster, c -> c.getSession().write(MaplePacketCreator.spawnFakeMonster(monster, 0)), null);
        }
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void makeMonsterReal(final MapleMonster monster) {
        monster.setFake(false);
        broadcastMessage(MaplePacketCreator.makeMonsterReal(monster));
    }

    public void spawnReactor(final MapleReactor reactor) {
        reactor.setMap(this);
        synchronized (this.mapObjects) {
            spawnAndAddRangedMapObject(reactor, c -> c.getSession().write(reactor.makeSpawnData()), null);
        }
    }

    private void respawnReactor(final MapleReactor reactor) {
        reactor.setState((byte) 0);
        reactor.setAlive(true);
        spawnReactor(reactor);
    }

    public void spawnDoor(final MapleDoor door) {
        synchronized (this.mapObjects) {
            spawnAndAddRangedMapObject(door, c -> {
                c.getSession().write(MaplePacketCreator.spawnDoor(door.getOwner().getId(), door.getTargetPosition(), false));
                if (door.getOwner().getParty() != null && (door.getOwner() == c.getPlayer() || door.getOwner().getParty().containsMembers(new MaplePartyCharacter(c.getPlayer())))) {
                    c.getSession().write(MaplePacketCreator.partyPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                }
                c.getSession().write(MaplePacketCreator.spawnPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                c.getSession().write(MaplePacketCreator.enableActions());
            }, chr -> chr.getMapId() == door.getTarget().getId() ||
               chr == door.getOwner() && chr.getParty() == null
            );
        }
    }

    public void spawnSummon(final MapleSummon summon) {
        spawnAndAddRangedMapObject(summon, c -> {
            int skillLevel = summon.getOwner().getSkillLevel(SkillFactory.getSkill(summon.getSkill()));
            c.getSession().write(MaplePacketCreator.spawnSpecialMapObject(summon, skillLevel, true));
        }, null);
    }

    public void spawnMist(final MapleMist mist, final int duration, boolean poison, boolean fake) {
        addMapObject(mist);
        broadcastMessage(fake ? mist.makeFakeSpawnData(30) : mist.makeSpawnData());
        TimerManager tMan = TimerManager.getInstance();
        final ScheduledFuture<?> poisonSchedule;
        if (poison) {
            Runnable poisonTask = () -> {
                List<MapleMapObject> affectedMonsters = getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.MONSTER));
                for (MapleMapObject mo : affectedMonsters) {
                    if (mist.makeChanceResult()) {
                        MonsterStatusEffect poisonEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist.getSourceSkill(), false);
                        ((MapleMonster) mo).applyStatus(mist.getOwner(), poisonEffect, true, duration);
                    }
                }
            };
            poisonSchedule = tMan.register(poisonTask, 2000, 2500);
        } else {
            poisonSchedule = null;
        }
        tMan.schedule(() -> {
            removeMapObject(mist);
            if (poisonSchedule != null) {
                poisonSchedule.cancel(false);
            }
            broadcastMessage(mist.makeDestroyData());
        }, duration);
    }

    public void disappearingItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final IItem item, Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(item.getItemId(), drop.getObjectId(), 0, 0, dropper.getPosition(), droppos, (byte) 3), drop.getPosition());
    }

    public void spawnItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final IItem item, Point pos, final boolean ffaDrop, final boolean expire) {
        TimerManager tMan = TimerManager.getInstance();
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner);
        spawnAndAddRangedMapObject(drop, c ->
            c.getSession()
             .write(MaplePacketCreator.dropItemFromMapObject(
                 item.getItemId(),
                 drop.getObjectId(),
                 0,
                 ffaDrop ? 0 : owner.getId(),
                 dropper.getPosition(),
                 droppos,
                 (byte) 1)
             ), null);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(
            item.getItemId(),
            drop.getObjectId(),
            0,
            ffaDrop ? 0 : owner.getId(),
            dropper.getPosition(),
            droppos,
            (byte) 0
        ), drop.getPosition());
        if (expire) {
            tMan.schedule(new ExpireMapItemJob(drop), dropLife);
        }
        activateItemReactors(drop);
    }

    public boolean hasPeriodicMonsterDrop(int monsterOid) {
        return periodicMonsterDrops.stream().anyMatch((pmd) -> pmd.getLeft().getMonsterOid() == monsterOid);
    }

    private void cancelCancelPeriodicMonsterDrop(final int monsterOid) {
        synchronized (periodicMonsterDrops) {
            periodicMonsterDrops.forEach((pmdh) -> {
                if (pmdh.getLeft().getMonsterOid() == monsterOid) {
                    pmdh.getRight().cancel(false);
                }
            });
        }
    }

    public void cancelPeriodicMonsterDrops(final int monsterOid) {
        synchronized (periodicMonsterDrops) {
            for (int i = 0; i < periodicMonsterDrops.size() && i >= 0; ++i) {
                if (periodicMonsterDrops.get(i).getLeft().getMonsterOid() == monsterOid) {
                    periodicMonsterDrops.get(i--).getLeft().selfCancel();
                }
            }
        }
    }

    public void cancelAllPeriodicMonsterDrops() {
        synchronized (periodicMonsterDrops) {
            for (int i = 0; i < periodicMonsterDrops.size() && i >= 0; ++i) {
                periodicMonsterDrops.get(i--).getLeft().selfCancel();
            }
            periodicMonsterDrops.clear();
        }
    }

    private void cancelPeriodicMonsterDrop(final PeriodicMonsterDrop pmd) {
        synchronized (periodicMonsterDrops) {
            Pair<PeriodicMonsterDrop, ScheduledFuture<?>> pmdh = null;
            for (Pair<PeriodicMonsterDrop, ScheduledFuture<?>> _pmdh : periodicMonsterDrops) {
                if (_pmdh.getLeft().equals(pmd)) {
                    pmdh = _pmdh;
                    break;
                }
            }
            if (pmdh != null) {
                pmdh.getRight().cancel(false);
            }
            periodicMonsterDrops.remove(pmdh);
        }
    }

    public PartyQuestMapInstance getPartyQuestInstance() {
        return partyQuestInstance;
    }

    public void registerPartyQuestInstance(PartyQuestMapInstance newInstance) {
        if (this.partyQuestInstance != null) {
            this.partyQuestInstance.dispose();
        }
        this.partyQuestInstance = newInstance;
    }

    public void unregisterPartyQuestInstance() {
        this.partyQuestInstance = null;
    }

    private class TimerDestroyWorker implements Runnable {
        @Override
        public void run() {
            if (mapTimer != null) {
                int warpMap = mapTimer.warpToMap();
                int minWarp = mapTimer.minLevelToWarp();
                int maxWarp = mapTimer.maxLevelToWarp();
                mapTimer = null;
                if (warpMap != -1) {
                    MapleMap map2wa2 = ChannelServer.getInstance(channel).getMapFactory().getMap(warpMap);
                    String warpmsg = "You will now be warped to " + map2wa2.getStreetName() + " : " + map2wa2.getMapName() + ".";
                    broadcastMessage(MaplePacketCreator.serverNotice(6, warpmsg));
                    for (MapleCharacter chr : getCharacters()) {
                        try {
                            if (chr.getLevel() >= minWarp && chr.getLevel() <= maxWarp) {
                                chr.changeMap(map2wa2, map2wa2.getPortal(0));
                            } else {
                                chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "You are not at yet level " + minWarp + ", or you are higher than level " + maxWarp + "."));
                            }
                        } catch (Exception ex) {
                            String errormsg = "There was a problem warping you. Please contact a GM.";
                            chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, errormsg));
                        }
                    }
                }
            }
        }
    }

    public void addMapTimer(int duration) {
        ScheduledFuture<?> sf0f = TimerManager.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new MapleMapTimer(sf0f, duration, -1, -1, -1);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void addMapTimer(int duration, int mapToWarpTo) {
        ScheduledFuture<?> sf0f = TimerManager.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new MapleMapTimer(sf0f, duration, mapToWarpTo, 0, 256);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void addMapTimer(int duration, int mapToWarpTo, int minLevelToWarp) {
        ScheduledFuture<?> sf0f = TimerManager.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new MapleMapTimer(sf0f, duration, mapToWarpTo, minLevelToWarp, 256);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void addMapTimer(int duration, int mapToWarpTo, int minLevelToWarp, int maxLevelToWarp) {
        ScheduledFuture<?> sf0f = TimerManager.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new MapleMapTimer(sf0f, duration, mapToWarpTo, minLevelToWarp, maxLevelToWarp);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void clearMapTimer() {
        if (mapTimer != null) {
            mapTimer.getSF0F().cancel(true);
        }
        mapTimer = null;
    }

    public int clearDrops() {
        double range = Double.POSITIVE_INFINITY;
        List<MapleMapObject> items = getMapObjectsInRange(new Point(0, 0), range, Collections.singletonList(MapleMapObjectType.ITEM));
        for (MapleMapObject itemmo : items) {
            removeMapObject(itemmo);
            broadcastMessage(MaplePacketCreator.removeItemFromMap(itemmo.getObjectId(), 0, 0));
        }
        return items.size();
    }

    private void activateItemReactors(MapleMapItem drop) {
        IItem item = drop.getItem();
        final TimerManager tMan = TimerManager.getInstance();
        Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
        while (mmoiter.hasNext()) {
            MapleMapObject o = mmoiter.next();
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (((MapleReactor) o).getReactorType() == 100) {
                    if (((MapleReactor) o).getReactItem().getLeft() == item.getItemId() && ((MapleReactor) o).getReactItem().getRight() <= item.getQuantity()) {
                        Rectangle area = ((MapleReactor) o).getArea();

                        if (area.contains(drop.getPosition())) {
                            MapleClient ownerClient = null;
                            if (drop.getOwner() != null) {
                                ownerClient = drop.getOwner().getClient();
                            }
                            MapleReactor reactor = (MapleReactor) o;
                            if (!reactor.isTimerActive()) {
                                tMan.schedule(new ActivateItemReactor(drop, reactor, ownerClient), 5000);
                                reactor.setTimerActive(true);
                            }
                        }
                    }
                }
            }
        }
    }

    public void AriantPQStart() {
        int i = 1;
        for (MapleCharacter chars2 : this.getCharacters()) {
            broadcastMessage(MaplePacketCreator.updateAriantPQRanking(chars2.getName(), 0, false));
            broadcastMessage(MaplePacketCreator.serverNotice(0, MaplePacketCreator.updateAriantPQRanking(chars2.getName(), 0, false).toString()));
            if (this.getCharacters().size() > i) {
                broadcastMessage(MaplePacketCreator.updateAriantPQRanking(null, 0, true));
                broadcastMessage(MaplePacketCreator.serverNotice(0, MaplePacketCreator.updateAriantPQRanking(chars2.getName(), 0, true).toString()));
            }
            i++;
        }
    }

    public void spawnMesoDrop(final int meso, final int displayMeso, Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean ffaLoot) {
        TimerManager tMan = TimerManager.getInstance();
        final Point droppos = calcDropPos(position, position);
        final MapleMapItem mdrop = new MapleMapItem(meso, displayMeso, droppos, dropper, owner);
        spawnAndAddRangedMapObject(mdrop, c ->
            c.getSession().write(
                MaplePacketCreator.dropMesoFromMapObject(
                    displayMeso,
                    mdrop.getObjectId(),
                    dropper.getObjectId(),
                    ffaLoot ? 0 : owner.getId(),
                    dropper.getPosition(),
                    droppos,
                    (byte) 1
                )
            ), null);
        tMan.schedule(new ExpireMapItemJob(mdrop), dropLife);
    }

    public void startMapEffect(String msg, int itemId) {
        if (mapEffect != null) return;
        mapEffect = new MapleMapEffect(msg, itemId);
        broadcastMessage(mapEffect.makeStartData());
        TimerManager tMan = TimerManager.getInstance();
        tMan.schedule(() -> {
            broadcastMessage(mapEffect.makeDestroyData());
            mapEffect = null;
        }, 30000);
    }

    public void addPlayer(MapleCharacter chr) {
        synchronized (characters) {
            this.characters.add(chr);
        }
        synchronized (this.mapObjects) {
            if (!chr.isHidden()) {
                broadcastMessage(chr, (MaplePacketCreator.spawnPlayerMapobject(chr)), false);
                MaplePet[] pets = chr.getPets();
                for (int i = 0; i < 3; ++i) {
                    if (pets[i] != null) {
                        pets[i].setPos(getGroundBelow(chr.getPosition()));
                        broadcastMessage(chr, MaplePacketCreator.showPet(chr, pets[i], false, false), false);
                    } else {
                        break;
                    }
                }
                if (chr.getChalkboard() != null) {
                    broadcastMessage(chr, (MaplePacketCreator.useChalkboard(chr, false)), false);
                }
            } else {
                broadcastGMMessage(chr, (MaplePacketCreator.spawnPlayerMapobject(chr)), false);
                MaplePet[] pets = chr.getPets();
                for (int i = 0; i < 3; ++i) {
                    if (pets[i] != null) {
                        pets[i].setPos(getGroundBelow(chr.getPosition()));
                        broadcastGMMessage(chr, MaplePacketCreator.showPet(chr, pets[i], false, false), false);
                    } else {
                        break;
                    }
                }
                if (chr.getChalkboard() != null) {
                    broadcastGMMessage(chr, (MaplePacketCreator.useChalkboard(chr, false)), false);
                }
            }
            sendObjectPlacement(chr.getClient());
            switch (getId()) {
                case 1:
                case 2:
                case 809000101:
                case 809000201:
                    chr.getClient().getSession().write(MaplePacketCreator.showEquipEffect());
            }
            MaplePet[] pets = chr.getPets();
            for (int i = 0; i < 3; ++i) {
                if (pets[i] != null) {
                    pets[i].setPos(getGroundBelow(chr.getPosition()));
                    chr.getClient().getSession().write(MaplePacketCreator.showPet(chr, pets[i], false, false));
                }
            }
            if (chr.getChalkboard() != null) {
                chr.getClient().getSession().write((MaplePacketCreator.useChalkboard(chr, false)));
            }
            this.mapObjects.put(chr.getObjectId(), chr);
        }
        MapleStatEffect summonStat = chr.getStatForBuff(MapleBuffStat.SUMMON);
        if (summonStat != null) {
            MapleSummon summon = chr.getSummons().get(summonStat.getSourceId());
            summon.setPosition(getGroundBelow(chr.getPosition()));
            chr.getMap().spawnSummon(summon);
            updateMapObjectVisibility(chr, summon);
        }
        MapleStatEffect morphStat = chr.getStatForBuff(MapleBuffStat.MORPH);
        if (morphStat != null && morphStat.isPirateMorph()) {
            List<Pair<MapleBuffStat, Integer>> pmorphstatup = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, morphStat.getMorph(chr)));
            chr.getClient().getSession().write(MaplePacketCreator.giveBuff(morphStat.getSourceId(), 100, pmorphstatup));
        }
        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        if (MapleTVEffect.active) {
            if (hasMapleTV() && MapleTVEffect.packet != null) {
                chr.getClient().getSession().write(MapleTVEffect.packet);
            }
        }
        if (getTimeLimit() > 0 && getForcedReturnMap() != null) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock(getTimeLimit()));
            chr.startMapTimeLimitTask(this, this.getForcedReturnMap());
        }
        if (chr.getEventInstance() != null && chr.getEventInstance().isTimerStarted()) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (chr.getEventInstance().getTimeLeft() / 1000)));
        }
        if (chr.getPartyQuest() != null && chr.getPartyQuest().isTimerStarted()) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (chr.getPartyQuest().getTimeLeft() / 1000)));
        }
        if (hasClock()) {
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int min = cal.get(Calendar.MINUTE);
            int second = cal.get(Calendar.SECOND);
            chr.getClient().getSession().write((MaplePacketCreator.getClockTime(hour, min, second)));
        } else if (getPartyQuestInstance() != null && getPartyQuestInstance().getPartyQuest().isTimerStarted()) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (getPartyQuestInstance().getPartyQuest().getTimeLeft() / 1000)));
        }
        if (hasBoat() == 2) {
            chr.getClient().getSession().write((MaplePacketCreator.boatPacket(true)));
        } else if (hasBoat() == 1 && (chr.getMapId() != 200090000 || chr.getMapId() != 200090010)) {
            chr.getClient().getSession().write(MaplePacketCreator.boatPacket(false));
        }
        chr.receivePartyMemberHP();
    }

    public void removePlayer(MapleCharacter chr) {
        synchronized (characters) {
            characters.remove(chr);
        }
        removeMapObject(chr.getObjectId());
        broadcastMessage(MaplePacketCreator.removePlayerFromMap(chr.getId()));
        for (MapleMonster monster : chr.getControlledMonsters()) {
            monster.setController(null);
            monster.setControllerHasAggro(false);
            monster.setControllerKnowsAboutAggro(false);
            updateMonsterController(monster);
        }
        chr.leaveMap();
        chr.cancelMapTimeLimitTask();
        for (MapleSummon summon : chr.getSummons().values()) {
            if (summon.isPuppet()) {
                chr.cancelBuffStats(MapleBuffStat.PUPPET);
            } else {
                removeMapObject(summon);
            }
        }
    }

    public void broadcastMessage(MaplePacket packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    public void broadcastMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource, boolean ranged) {
        broadcastMessage(repeatToSource ? null : source, packet, ranged ? MapleCharacter.MAX_VIEW_RANGE_SQ : Double.POSITIVE_INFINITY, source.getPosition());
    }

    public void broadcastMessage(MaplePacket packet, Point rangedFrom) {
        broadcastMessage(null, packet, MapleCharacter.MAX_VIEW_RANGE_SQ, rangedFrom);
    }

    public void broadcastMessage(MapleCharacter source, MaplePacket packet, Point rangedFrom) {
        broadcastMessage(source, packet, MapleCharacter.MAX_VIEW_RANGE_SQ, rangedFrom);
    }

    private void broadcastMessage(MapleCharacter source, MaplePacket packet, double rangeSq, Point rangedFrom) {
        synchronized (characters) {
            for (MapleCharacter chr : characters) {
                if (chr != source && !chr.isFake()) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.getClient().getSession().write(packet);
                        }
                    } else {
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        }
    }

    public void broadcastGMMessage(MaplePacket packet) {
        broadcastGMMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastGMMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    private void broadcastGMMessage(MapleCharacter source, MaplePacket packet, double rangeSq, Point rangedFrom) {
        synchronized (characters) {
            for (MapleCharacter chr : characters) {
                if (chr != source && !chr.isFake() && chr.isGM()) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.getClient().getSession().write(packet);
                        }
                    } else {
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        }
    }

    public void broadcastNONGMMessage(MaplePacket packet) {
        broadcastNONGMMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastNONGMMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource) {
        broadcastNONGMMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    private void broadcastNONGMMessage(MapleCharacter source, MaplePacket packet, double rangeSq, Point rangedFrom) {
        synchronized (characters) {
            for (MapleCharacter chr : characters) {
                if (chr != source && !chr.isFake() && !chr.isGM()) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.getClient().getSession().write(packet);
                        }
                    } else {
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        }
    }

    private boolean isNonRangedType(MapleMapObjectType type) {
        switch (type) {
            case NPC:
            case PLAYER:
            case HIRED_MERCHANT:
            case MIST:
            case PLAYER_NPC:
                return true;
        }
        return false;
    }

    private void sendObjectPlacement(MapleClient mapleClient) {
        Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
        while (mmoiter.hasNext()) {
            MapleMapObject o = mmoiter.next();
            if (isNonRangedType(o.getType())) {
                o.sendSpawnData(mapleClient);
            } else if (o.getType() == MapleMapObjectType.MONSTER) {
                updateMonsterController((MapleMonster) o);
            }
        }
        MapleCharacter chr = mapleClient.getPlayer();

        if (chr != null) {
            for (MapleMapObject o : getMapObjectsInRange(chr.getPosition(), MapleCharacter.MAX_VIEW_RANGE_SQ, rangedMapObjectTypes)) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    if (((MapleReactor) o).isAlive()) {
                        o.sendSpawnData(chr.getClient());
                        chr.addVisibleMapObject(o);
                    }
                } else {
                    o.sendSpawnData(chr.getClient());
                    chr.addVisibleMapObject(o);
                }
            }
        } else {
            log.info("sendObjectPlacement invoked with null char");
        }
    }

    public List<MapleMapObject> getMapObjectsInRange(Point from, double rangeSq, List<MapleMapObjectType> types) {
        List<MapleMapObject> ret = new ArrayList<>();
        synchronized (mapObjects) {
            Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject l = mmoiter.next();
                if (types.contains(l.getType())) {
                    if (from.distanceSq(l.getPosition()) <= rangeSq) {
                        ret.add(l);
                    }
                }
            }
        }
        return ret;
    }

    public List<MapleMapObject> getMapObjectsInRange(Point from, double rangeSq, MapleMapObjectType type) {
        List<MapleMapObject> ret = new ArrayList<>();
        synchronized (mapObjects) {
            Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject l = mmoiter.next();
                if (type.equals(l.getType())) {
                    if (from.distanceSq(l.getPosition()) <= rangeSq) {
                        ret.add(l);
                    }
                }
            }
        }
        return ret;
    }

    public List<MapleMapObject> getItemsInRange(Point from, double rangeSq) {
        List<MapleMapObject> ret = new ArrayList<>();
        synchronized (mapObjects) {
            Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject l = mmoiter.next();
                if (l.getType() == MapleMapObjectType.ITEM) {
                    if (from.distanceSq(l.getPosition()) <= rangeSq) {
                        ret.add(l);
                    }
                }
            }
        }
        return ret;
    }

    public List<MapleMapObject> getMapObjectsInRect(Rectangle box, List<MapleMapObjectType> types) {
        List<MapleMapObject> ret = new ArrayList<>();
        synchronized (mapObjects) {
            Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject l = mmoiter.next();
                if (types.contains(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
        }
        return ret;
    }

    public List<MapleMapObject> getMapObjectsInRect(Rectangle box, MapleMapObjectType type) {
        List<MapleMapObject> ret = new ArrayList<>();
        synchronized (mapObjects) {
            Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject l = mmoiter.next();
                if (type.equals(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
        }
        return ret;
    }

    public List<MapleCharacter> getPlayersInRect(Rectangle box, List<MapleCharacter> chr) {
        List<MapleCharacter> character = new ArrayList<>();
        synchronized (characters) {
            for (MapleCharacter a : characters) {
                if (chr.contains(a.getClient().getPlayer())) {
                    if (box.contains(a.getPosition())) {
                        character.add(a);
                    }
                }
            }
        }
        return character;
    }

    public void addPortal(MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public MaplePortal getPortal(String portalname) {
        for (MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public MaplePortal getPortal(int portalid) {
        return portals.get(portalid);
    }

    public MaplePortal getRandomPortal() {
        List<MaplePortal> portalList = new ArrayList<>(portals.values());
        return portalList.get((int) (Math.random() * portalList.size()));
    }

    public void addMapleArea(Rectangle rec) {
        areas.add(rec);
    }

    public List<Rectangle> getAreas() {
        return new ArrayList<>(areas);
    }

    public Rectangle getArea(int index) {
        return areas.get(index);
    }

    public void setFootholds(MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public MapleFootholdTree getFootholds() {
        return footholds;
    }

    public void addMonsterSpawn(MapleMonster monster, int mobTime) {
        if (!((monster.getId() == 9400014 || monster.getId() == 9400575) && this.getId() > 5000)) {
            Point newpos = calcPointBelow(monster.getPosition());
            if (newpos == null) {
                Point adjustedpos = monster.getPosition();
                adjustedpos.translate(0, -4);
                newpos = calcPointBelow(adjustedpos);
            }
            if (newpos == null) {
                System.err.println("Could not generate mob spawn position. MapleMap#addMonsterSpawn");
                return;
            }
            newpos.y -= 1;
            SpawnPoint sp = new SpawnPoint(monster, newpos, mobTime);
            monsterSpawn.add(sp);
            if (sp.shouldSpawn() || mobTime == -1) {
                sp.spawnMonster(this);
            }
        }
    }

    public float getMonsterRate() {
        return monsterRate;
    }

    public Collection<MapleCharacter> getCharacters() {
        return Collections.unmodifiableCollection(this.characters);
    }

    public MapleCharacter getCharacterById(int id) {
        for (MapleCharacter c : this.characters) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    private void updateMapObjectVisibility(MapleCharacter chr, MapleMapObject mo) {
        if (chr.isFake()) {
            return;
        }
        if (!chr.isMapObjectVisible(mo)) {
            if (mo.getType() == MapleMapObjectType.SUMMON || mo.getPosition().distanceSq(chr.getPosition()) <= MapleCharacter.MAX_VIEW_RANGE_SQ) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else {
            if (mo.getType() != MapleMapObjectType.SUMMON && mo.getPosition().distanceSq(chr.getPosition()) > MapleCharacter.MAX_VIEW_RANGE_SQ) {
                chr.removeVisibleMapObject(mo);
                mo.sendDestroyData(chr.getClient());
            }
        }
    }

    public void moveMonster(MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);
        synchronized (characters) {
            for (MapleCharacter chr : characters) {
                updateMapObjectVisibility(chr, monster);
            }
        }
    }

    public void movePlayer(final MapleCharacter player, Point newPosition) {
        if (player.isFake()) {
            return;
        }
        player.setPosition(newPosition);

        List<MapleMapObject> visibleObjectsNow = new ArrayList<>();
        //Set<MapleMapObject> visibleObjects = player.getVisibleMapObjects();
        synchronized (player.getVisibleMapObjects()) {
            Iterator<MapleMapObject> mmoiter = player.getVisibleMapObjects().iterator();
            while (mmoiter.hasNext()) {
                visibleObjectsNow.add(mmoiter.next());
            }
        }
        //MapleMapObject[] visibleObjectsNow = visibleObjects.toArray(new MapleMapObject[visibleObjects.size()]);

        /*
        synchronized (player.getVisibleMapObjects()) {
            visibleObjectsNow = visibleObjects.toArray(new MapleMapObject[visibleObjects.size()]);
        }
        */
        synchronized (mapObjects) {
            for (MapleMapObject mmo : visibleObjectsNow) {
                if (mapObjects.get(mmo.getObjectId()) == mmo) {
                    updateMapObjectVisibility(player, mmo);
                } else {
                    player.removeVisibleMapObject(mmo);
                }
            }
            getMapObjectsInRange(player.getPosition(), MapleCharacter.MAX_VIEW_RANGE_SQ, rangedMapObjectTypes)
                .stream()
                .filter(mmo -> !player.isMapObjectVisible(mmo))
                .forEach(mmo -> {
                    mmo.sendSpawnData(player.getClient());
                    player.addVisibleMapObject(mmo);
                });
        }
    }

    public MaplePortal findClosestSpawnpoint(final Point from) {
        return portals.values()
                      .stream()
                      .filter(p -> p.getType() >= 0 && p.getType() <= 2 && p.getTargetMapId() == 999999999)
                      .reduce(null, (b, p) -> {
                          if (b == null || p.getPosition().distanceSq(from) < b.getPosition().distanceSq(from)) {
                              return p;
                          }
                          return b;
                      });
    }

    public MaplePortal findClosestSpawnpointInDirection(final Point from, Direction dir) {
        return findClosestSpawnpointInDirection(from, Collections.singleton(dir));
    }

    public MaplePortal findClosestSpawnpointInDirection(final Point from, final Set<Direction> dirs) {
        if (dirs.isEmpty()) {
            return null;
        }
        return portals.values()
                      .stream()
                      .filter(p ->
                          p.getType() >= 0 &&
                          p.getType() <= 2 &&
                          p.getTargetMapId() == 999999999 &&
                          Direction.directionsOf(Vect.point(p.getPosition()).subtract(Vect.point(from)))
                                   .stream()
                                   .anyMatch(dirs::contains)
                      )
                      .reduce(null, (b, p) -> {
                          if (b == null || p.getPosition().distanceSq(from) < b.getPosition().distanceSq(from)) {
                              return p;
                          }
                          return b;
                      });
    }

    public MaplePortal findClosestSpawnpointInDirection(final Point from,
                                                        Direction dir,
                                                        final Set<Direction> excludedDirs) {
        return findClosestSpawnpointInDirection(from, Collections.singleton(dir), excludedDirs);
    }

    public MaplePortal findClosestSpawnpointInDirection(final Point from,
                                                        final Set<Direction> dirs,
                                                        final Set<Direction> excludedDirs) {
        if (dirs.isEmpty()) {
            return null;
        }
        return portals.values()
                      .stream()
                      .filter(p -> {
                          Set<Direction> locationDirs =
                              Direction.directionsOf(Vect.point(p.getPosition()).subtract(Vect.point(from)));
                          Set<Direction> intersection = new HashSet<>(locationDirs);
                          intersection.retainAll(excludedDirs);
                          return
                              intersection.isEmpty() &&
                              p.getType() >= 0 &&
                              p.getType() <= 2 &&
                              p.getTargetMapId() == 999999999 &&
                              locationDirs
                                  .stream()
                                  .anyMatch(dirs::contains);
                      })
                      .reduce(null, (b, p) -> {
                          if (b == null || p.getPosition().distanceSq(from) < b.getPosition().distanceSq(from)) {
                              return p;
                          }
                          return b;
                      });
    }

    public MaplePortal findClosestSpawnpointInDirection(final Point from,
                                                        final Direction dir,
                                                        final int yMin,
                                                        final int yMax) {
        if (dir == null) {
            return null;
        }
        return portals.values()
                      .stream()
                      .filter(p ->
                          p.getType() >= 0 &&
                          p.getType() <= 2 &&
                          p.getTargetMapId() == 999999999 &&
                          p.getPosition().y >= yMin &&
                          p.getPosition().y <= yMax &&
                          Direction.directionsOf(Vect.point(p.getPosition()).subtract(Vect.point(from))).contains(dir)
                      )
                      .reduce(null, (b, p) -> {
                          if (b == null || p.getPosition().distanceSq(from) < b.getPosition().distanceSq(from)) {
                              return p;
                          }
                          return b;
                      });
    }

    public void spawnDebug(MessageCallback mc) {
        mc.dropMessage("Spawndebug...");
        synchronized (mapObjects) {
            mc.dropMessage("Mapobjects in map: " + mapObjects.size() + " \"spawnedMonstersOnMap\": " +
                    spawnedMonstersOnMap + " spawnpoints: " + monsterSpawn.size() +
                    " maxRegularSpawn: " + getMaxRegularSpawn());
            int numMonsters = 0;
            Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject mo = mmoiter.next();
                if (mo instanceof MapleMonster) {
                    numMonsters++;
                }
            }
            mc.dropMessage("Actual monsters: " + numMonsters);
        }
    }

    private int getMaxRegularSpawn() {
        return (int) (monsterSpawn.size() / monsterRate);
    }

    public Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setClock(boolean hasClock) {
        this.clock = hasClock;
    }

    public boolean hasClock() {
        return clock;
    }

    public void setTown(boolean isTown) {
        this.town = isTown;
    }

    public boolean isTown() {
        return town;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public void setEverlast(boolean everlast) {
        this.everlast = everlast;
    }

    public boolean getEverlast() {
        return everlast;
    }

    public int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }

    public Collection<MapleCharacter> getNearestPvpChar(Point attacker, double maxRange, double maxHeight, Collection<MapleCharacter> chr) {
        Collection<MapleCharacter> character = new ArrayList<>();
        for (MapleCharacter a : characters) {
            if (chr.contains(a.getClient().getPlayer())) {
                Point attackedPlayer = a.getPosition();
                MaplePortal Port = a.getMap().findClosestSpawnpoint(a.getPosition());
                Point nearestPort = Port.getPosition();
                double safeDis = attackedPlayer.distance(nearestPort);
                double distanceX = attacker.distance(attackedPlayer.getX(), attackedPlayer.getY());
                if (PvPLibrary.isLeft) {
                    if (attacker.x > attackedPlayer.x && distanceX < maxRange && distanceX > 2 &&
                            attackedPlayer.y >= attacker.y - maxHeight && attackedPlayer.y <= attacker.y + maxHeight && safeDis > 2) {
                        character.add(a);
                    }
                }
                if (PvPLibrary.isRight) {
                    if (attacker.x < attackedPlayer.x && distanceX < maxRange && distanceX > 2 &&
                            attackedPlayer.y >= attacker.y - maxHeight && attackedPlayer.y <= attacker.y + maxHeight && safeDis > 2) {
                        character.add(a);
                    }
                }
            }
        }
        return character;
    }

    private class ExpireMapItemJob implements Runnable {
        private final MapleMapItem mapitem;
        public ExpireMapItemJob(MapleMapItem mapitem) {
            this.mapitem = mapitem;
        }
        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
                synchronized (mapitem) {
                    if (mapitem.isPickedUp()) {
                        return;
                    }
                    MapleMap.this.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
                    MapleMap.this.removeMapObject(mapitem);
                    mapitem.setPickedUp(true);
                }
            }
        }
    }

    private class ActivateItemReactor implements Runnable {
        private final MapleMapItem mapitem;
        private final MapleReactor reactor;
        private final MapleClient c;

        public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor, MapleClient c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
                synchronized (mapitem) {
                    TimerManager tMan = TimerManager.getInstance();
                    if (mapitem.isPickedUp()) {
                        return;
                    }
                    MapleMap.this.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
                    MapleMap.this.removeMapObject(mapitem);
                    reactor.hitReactor(c);
                    reactor.setTimerActive(false);
                    if (reactor.getDelay() > 0) {
                        tMan.schedule(() -> {
                            reactor.setState((byte) 0);
                            broadcastMessage(MaplePacketCreator.triggerReactor(reactor, 0));
                        }, reactor.getDelay());
                    }
                }
            }
        }
    }

    private class RespawnWorker implements Runnable {
        @Override
        public void run() {
            int playersOnMap = characters.size();
            if (playersOnMap == 0) return;

            int ispawnedMonstersOnMap = spawnedMonstersOnMap.get();
            int getMaxSpawn = (int) (getMaxRegularSpawn() * 1.6d);
            int numShouldSpawn = (int) Math.ceil(Math.random() * (playersOnMap / 1.5d + (getMaxSpawn - ispawnedMonstersOnMap)));
            // int numShouldSpawn = (int) Math.round(Math.random() * (2 + playersOnMap / 1.5 + (getMaxRegularSpawn() - ispawnedMonstersOnMap) / 4.0));
            if (numShouldSpawn + ispawnedMonstersOnMap > getMaxSpawn) {
                numShouldSpawn = getMaxSpawn - ispawnedMonstersOnMap;
            }
            if (numShouldSpawn <= 0) return;

            List<SpawnPoint> randomSpawn = new ArrayList<>(monsterSpawn);
            Collections.shuffle(randomSpawn);
            int spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                if (spawnPoint.shouldSpawn()) {
                    spawnPoint.spawnMonster(MapleMap.this);
                    spawned++;
                }
                if (spawned >= numShouldSpawn) break;
            }
        }
    }

    private class PeriodicMonsterDrop implements Runnable {
        private final MapleCharacter chr;
        private final MapleMonster monster;
        private ScheduledFuture<?> task = null;

        public PeriodicMonsterDrop(MapleCharacter chr, MapleMonster monster) {
            this.chr = chr;
            this.monster = monster;
        }

        @Override
        public void run() {
            if (chr != null && monster != null) {
                if (monster.isAlive()) {
                    monster.getMap().dropFromMonster(chr, monster);
                } else {
                    selfCancel();
                }
            }
        }

        public void setTask(ScheduledFuture<?> task) {
            this.task = task;
        }

        public int getMonsterOid() {
            return monster.getObjectId();
        }

        public void selfCancel() {
            if (task != null) {
                MapleMap.this.cancelPeriodicMonsterDrop(this);
                task.cancel(false);
            }
        }
    }

    public class DynamicSpawnWorker {
        private final Rectangle spawnArea;
        private final Point spawnPoint;
        private final int period;
        private final int duration;
        private final int monsterId;
        private boolean putPeriodicMonsterDrops;
        private int monsterDropPeriod;
        private final MapleMonsterStats overrideStats;
        private ScheduledFuture<?> spawnTask = null;
        private ScheduledFuture<?> cancelTask = null;

        public DynamicSpawnWorker(int monsterId, Point spawnPoint, int period) {
            this(monsterId, spawnPoint, period, 0, false, 0);
        }

        public DynamicSpawnWorker(int monsterId, Point spawnPoint, int period, int duration, boolean putPeriodicMonsterDrops, int monsterDropPeriod) {
            if (MapleLifeFactory.getMonster(monsterId) == null) {
                throw new IllegalArgumentException("Monster ID for DynamicSpawnWorker must be a valid monster ID!");
            }
            spawnArea = null;
            this.spawnPoint = spawnPoint;
            this.period = period;
            this.duration = duration;
            this.monsterId = monsterId;
            this.putPeriodicMonsterDrops = putPeriodicMonsterDrops;
            this.monsterDropPeriod = monsterDropPeriod;
            this.overrideStats = null;
        }

        public DynamicSpawnWorker(int monsterId, Rectangle spawnArea, int period) {
            this(monsterId, spawnArea, period, 0, false, 0);
        }

        public DynamicSpawnWorker(int monsterId, Rectangle spawnArea, int period, int duration, boolean putPeriodicMonsterDrops, int monsterDropPeriod) {
            this(monsterId, spawnArea, period, duration, putPeriodicMonsterDrops, monsterDropPeriod, null);
        }

        public DynamicSpawnWorker(int monsterId, Rectangle spawnArea, int period, int duration, boolean putPeriodicMonsterDrops, int monsterDropPeriod, MapleMonsterStats overrideStats) {
            if (MapleLifeFactory.getMonster(monsterId) == null) {
                throw new IllegalArgumentException("Monster ID for DynamicSpawnWorker must be a valid monster ID!");
            }
            this.spawnArea = spawnArea;
            spawnPoint = null;
            this.period = period;
            this.duration = duration;
            this.monsterId = monsterId;
            this.putPeriodicMonsterDrops = putPeriodicMonsterDrops;
            this.monsterDropPeriod = monsterDropPeriod;
            this.overrideStats = overrideStats;
        }

        public void start() {
            if (spawnTask == null) {
                TimerManager tMan = TimerManager.getInstance();
                if (spawnArea != null) {
                    spawnTask = tMan.register(() -> {
                        final MapleMonster toSpawn = MapleLifeFactory.getMonster(monsterId);
                        if (overrideStats != null) {
                            toSpawn.setOverrideStats(overrideStats);
                            if (overrideStats.getHp() > 0) {
                                toSpawn.setHp(overrideStats.getHp());
                            }
                            if (overrideStats.getMp() > 0) {
                                toSpawn.setMp(overrideStats.getMp());
                            }
                        }

                        for (int i = 0; i < 10; ++i) {
                            try {
                                int x = (int) (spawnArea.x + Math.random() * (spawnArea.getWidth() + 1));
                                int y = (int) (spawnArea.y + Math.random() * (spawnArea.getHeight() + 1));
                                toSpawn.setPosition(new Point(x, y));
                                MapleMap.this.spawnMonster(toSpawn);

                                if (putPeriodicMonsterDrops) {
                                    MapleMap.this.startPeriodicMonsterDrop(toSpawn, monsterDropPeriod, 3000000);
                                }
                                break;
                            } catch (Exception ignored) {
                            }
                        }
                    }, period);
                } else {
                    spawnTask = tMan.register(() -> {
                        final MapleMonster toSpawn = MapleLifeFactory.getMonster(monsterId);
                        if (overrideStats != null) {
                            toSpawn.setOverrideStats(overrideStats);
                            if (overrideStats.getHp() > 0) {
                                toSpawn.setHp(overrideStats.getHp());
                            }
                            if (overrideStats.getMp() > 0) {
                                toSpawn.setMp(overrideStats.getMp());
                            }
                        }

                        toSpawn.setPosition(spawnPoint);
                        MapleMap.this.spawnMonster(toSpawn);

                        if (putPeriodicMonsterDrops) {
                            MapleMap.this.startPeriodicMonsterDrop(toSpawn, monsterDropPeriod, 3000000);
                        }
                    }, period);
                }

                if (duration > 0) {
                    cancelTask = tMan.schedule(() -> MapleMap.this.disposeDynamicSpawnWorker(this), duration);
                }
            }
        }

        public Point getSpawnPoint() {
            if (spawnPoint == null) return null;
            return new Point(spawnPoint);
        }

        public Rectangle getSpawnArea() {
            if (spawnArea == null) return null;
            return new Rectangle(spawnArea);
        }

        public int getMonsterId() {
            return monsterId;
        }

        public int getPeriod() {
            return period;
        }

        public MapleMonsterStats getOverrideStats() {
            return overrideStats;
        }

        public void turnOffPeriodicMonsterDrops() {
            setPeriodicMonsterDrops(false, 0);
        }

        public void setPeriodicMonsterDrops(boolean on, int monsterDropPeriod) {
            if (on) {
                this.monsterDropPeriod = monsterDropPeriod;
                putPeriodicMonsterDrops = true;
            } else {
                putPeriodicMonsterDrops = false;
            }
        }

        public void dispose() {
            if (spawnTask != null) spawnTask.cancel(false);
            if (cancelTask != null) cancelTask.cancel(false);
            cancelTask = null;
            spawnTask = null;
        }
    }

    private interface DelayedPacketCreation {
        void sendPackets(MapleClient c);
    }

    private interface SpawnCondition {
        boolean canSpawn(MapleCharacter chr);
    }

    public int getHPDec() {
        return decHP;
    }

    public void setHPDec(int delta) {
        decHP = delta;
    }

    public int getHPDecProtect() {
        return this.protectItem;
    }

    public void setHPDecProtect(int delta) {
        this.protectItem = delta;
    }

    public int hasBoat() {
        if (boat && docked) {
            return 2;
        } else if (boat) {
            return 1;
        } else {
            return 0;
        }
    }

    public void setBoat(boolean hasBoat) {
        this.boat = hasBoat;
    }

    public void setDocked(boolean isDocked) {
        this.docked = isDocked;
    }

    public void addBotPlayer(MapleCharacter chr) {
        synchronized (characters) {
            this.characters.add(chr);
        }
        synchronized (this.mapObjects) {
            if (!chr.isHidden()) {
                broadcastMessage(chr, (MaplePacketCreator.spawnPlayerMapobject(chr)), false);
            } else {
                broadcastGMMessage(chr, (MaplePacketCreator.spawnPlayerMapobject(chr)), false);
            }
            this.mapObjects.put(chr.getObjectId(), chr);
        }
    }

    public int playerCount() {
        return
            getMapObjectsInRange(
                new Point(0, 0),
                Double.POSITIVE_INFINITY,
                Collections.singletonList(MapleMapObjectType.PLAYER)
            ).size();
    }

    public int mobCount() {
        return
            getMapObjectsInRange(
                new Point(0, 0),
                Double.POSITIVE_INFINITY,
                Collections.singletonList(MapleMapObjectType.MONSTER)
            ).size();
    }

    public int reactorCount() {
        return
            getMapObjectsInRange(
                new Point(0, 0),
                Double.POSITIVE_INFINITY,
                Collections.singletonList(MapleMapObjectType.REACTOR)
            ).size();
    }

    public void setReactorState() {
        synchronized (this.mapObjects) {
            Iterator<MapleMapObject> mmoiter = mapObjects.values().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject o = mmoiter.next();
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    ((MapleReactor) o).setState((byte) 1);
                    broadcastMessage(MaplePacketCreator.triggerReactor((MapleReactor) o, 1));
                }
            }
        }
    }

    public void setShowGate(boolean gate) {
        this.showGate = gate;
    }

    public boolean hasShowGate() {
        return showGate;
    }

    public boolean hasMapleTV() {
        int tvIds[] =
        {
            9250042, 9250043, 9250025, 9250045, 9250044, 9270001, 9270002, 9250023, 9250024, 9270003,
            9270004, 9250026, 9270006, 9270007, 9250046, 9270000, 9201066, 9270005, 9270008, 9270009,
            9270010, 9270011, 9270012, 9270013, 9270014, 9270015, 9270016, 9270040
        };
        for (int id : tvIds) {
            if (containsNPC(id)) return true;
        }
        return false;
    }

    public void removeMonster(MapleMonster mons) {
        spawnedMonstersOnMap.decrementAndGet();
        broadcastMessage(MaplePacketCreator.killMonster(mons.getObjectId(), true), mons.getPosition());
        removeMapObject(mons);
    }

    public boolean isPQMap() {
        switch (getId()) {
            case 103000800:
            case 103000804:
            case 922010100:
            case 922010200:
            case 922010201:
            case 922010300:
            case 922010400:
            case 922010401:
            case 922010402:
            case 922010403:
            case 922010404:
            case 922010405:
            case 922010500:
            case 922010600:
            case 922010700:
            case 922010800:
                return true;
            default:
                return getId() / 1000 == 5;
        }
    }

    public boolean isMiniDungeonMap() {
        switch (mapid) {
            case 100020000:
            case 105040304:
            case 105050100:
            case 221023400:
                return true;
            default:
                return false;
        }
    }
}
