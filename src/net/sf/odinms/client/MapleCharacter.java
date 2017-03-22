package net.sf.odinms.client;

import net.sf.odinms.client.anticheat.CheatTracker;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.database.DatabaseException;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.PartyQuest;
import net.sf.odinms.net.world.*;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.scripting.event.EventInstanceManager;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.*;
import net.sf.odinms.server.PlayerInteraction.HiredMerchant;
import net.sf.odinms.server.PlayerInteraction.IPlayerInteractionManager;
import net.sf.odinms.server.PlayerInteraction.MaplePlayerShop;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MobSkill;
import net.sf.odinms.server.life.MobSkillFactory;
import net.sf.odinms.server.maps.*;
import net.sf.odinms.server.quest.MapleCustomQuest;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class MapleCharacter extends AbstractAnimatedMapleMapObject implements InventoryContainer {
    public static final double MAX_VIEW_RANGE_SQ = 850.0d * 850.0d;
    private static final double READING_PRIZE_PROP = 0.017d;
    public static final int[] SKILL_IDS =
    {
        1000,    1001,    1002,    1000000, 1000001, 1000002, 1001003, 1001004, 1001005, 2000000, 2000001,
        2001002, 2001003, 2001004, 2001005, 3000000, 3000001, 3000002, 3001003, 3001004, 3001005, 4000000,
        4000001, 4001002, 4001003, 4001334, 4001344, 1100000, 1100001, 1100002, 1100003, 1101004, 1101005,
        1101006, 1101007, 1200000, 1200001, 1200002, 1200003, 1201004, 1201005, 1201006, 1201007, 1300000,
        1300001, 1300002, 1300003, 1301004, 1301005, 1301006, 1301007, 2100000, 2101001, 2101002, 2101003,
        2101004, 2101005, 2200000, 2201001, 2201002, 2201003, 2201004, 2201005, 2300000, 2301001, 2301002,
        2301003, 2301004, 2301005, 3100000, 3100001, 3101002, 3101003, 3101004, 3101005, 3200000, 3200001,
        3201002, 3201003, 3201004, 3201005, 4100000, 4100001, 4100002, 4101003, 4101004, 4101005, 4200000,
        4200001, 4201002, 4201003, 4201004, 4201005, 1110000, 1110001, 1111002, 1111003, 1111004, 1111005,
        1111006, 1111007, 1111008, 1210000, 1210001, 1211002, 1211003, 1211004, 1211005, 1211006, 1211007,
        1211008, 1211009, 1310000, 1311001, 1311002, 1311003, 1311004, 1311005, 1311006, 1311007, 1311008,
        2110000, 2110001, 2111002, 2111003, 2111004, 2111005, 2111006, 2210000, 2210001, 2211002, 2211003,
        2211004, 2211005, 2211006, 2310000, 2311001, 2311002, 2311003, 2311004, 2311005, 2311006, 3110000,
        3110001, 3111002, 3111003, 3111004, 3111005, 3111006, 3210000, 3210001, 3211002, 3211003, 3211004,
        3211005, 3211006, 4110000, 4111001, 4111002, 4111003, 4111004, 4111005, 4111006, 4210000, 4211001,
        4211002, 4211003, 4211004, 4211005, 4211006, 1120003, 1120004, 1120005, 1121000, 1121001, 1121002,
        1121006, 1121008, 1121010, 1121011, 1220005, 1220006, 1220010, 1221000, 1221001, 1221002, 1221003,
        1221004, 1221007, 1221009, 1221011, 1221012, 1320005, 1320006, 1320008, 1320009, 1321000, 1321001,
        1321002, 1321003, 1321007, 1321010, 2121000, 2121001, 2121002, 2121003, 2121004, 2121005, 2121006,
        2121007, 2121008, 2221000, 2221001, 2221002, 2221003, 2221004, 2221005, 2221006, 2221007, 2221008,
        2321000, 2321001, 2321002, 2321003, 2321004, 2321005, 2321006, 2321007, 2321008, 2321009, 3120005,
        3121000, 3121002, 3121003, 3121004, 3121006, 3121007, 3121008, 3121009, 3220004, 3221000, 3221001,
        3221002, 3221003, 3221005, 3221006, 3221007, 3221008, 4120002, 4120005, 4121000, 4121003, 4121004,
        4121006, 4121007, 4121008, 4121009, 4220002, 4220005, 4221000, 4221001, 4221003, 4221004, 4221006,
        4221007, 4221008, 5000000, 5001001, 5001002, 5001003, 5001005, 5100000, 5100001, 5101002, 5101003,
        5101004, 5101005, 5101006, 5101007, 5200000, 5201001, 5201002, 5201003, 5201004, 5201005, 5201006,
        5110000, 5110001, 5111002, 5111004, 5111005, 5111006, 5220011, 5221010, 5221009, 5221008, 5221007,
        5221006, 5221004, 5221003, 5220002, 5220001, 5221000, 5121010, 5121009, 5121008, 5121007, 5121005,
        5121004, 5121003, 5121002, 5121001, 5121000, 5211006, 5211005, 5211004, 5211002, 5211001, 5210000,
        9001000, 9001001, 9001002, 9101000, 9101001, 9101002, 9101003, 9101004, 9101005, 9101006, 9101007,
        9101008
    };
    private int world;
    private int accountid;
    private int rank, rankMove, jobRank, jobRankMove;
    private String name;
    private int level, reborns, levelAchieved;
    private MapleJob jobAchieved;
    private int str, dex, luk, int_;
    private final AtomicInteger exp = new AtomicInteger();
    private int hp, maxhp, mp, maxmp, mpApUsed, hpApUsed;
    private int hair, face;
    private final AtomicInteger meso = new AtomicInteger();
    private int remainingAp, remainingSp;
    private final int[] savedLocations;
    private int fame;
    private long lastFameTime;
    private List<Integer> lastMonthFameIds;
    private transient int localMaxHp, localMaxMp,   localStr, localDex,
                          localLuk,   localInt,     magic,    watk,
                          accuracy,   avoidability, localMaxBaseDamage;
    private transient double speedMod, jumpMod;
    private int id;
    private MapleClient client;
    private MapleMap map;
    private int initialSpawnPoint, mapid;
    private MapleShop shop;
    private IPlayerInteractionManager interaction;
    private MapleStorage storage;
    private final MaplePet[] pets = new MaplePet[3];
    private ScheduledFuture<?> fullnessSchedule;
    private ScheduledFuture<?> fullnessSchedule_1;
    private ScheduledFuture<?> fullnessSchedule_2;
    private final SkillMacro[] skillMacros = new SkillMacro[5];
    private MapleTrade trade;
    private MapleSkinColor skinColor = MapleSkinColor.NORMAL;
    private MapleJob job = MapleJob.BEGINNER;
    private int gender;
    private int gmLevel;
    private boolean hidden;
    private boolean canDoor = true;
    private int chair;
    private int itemEffect;
    private int APQScore;
    private MapleParty party;
    private EventInstanceManager eventInstance;
    private PartyQuest partyQuest;
    private final MapleInventory[] inventory;
    private final Map<MapleQuest, MapleQuestStatus> quests;
    private final Set<MapleMonster> controlled = new LinkedHashSet<>();
    private final Set<MapleMapObject> visibleMapObjects =
        Collections.synchronizedSet(
            new LinkedHashSet<MapleMapObject>()
        );
    private final Map<ISkill, SkillEntry> skills = new LinkedHashMap<>();
    private final Map<MapleBuffStat, MapleBuffStatValueHolder> effects = new ConcurrentHashMap<>(8, 0.8f, 2);
    private final HashMap<Integer, MapleKeyBinding> keymap = new LinkedHashMap<>();
    private final List<MapleDoor> doors = new ArrayList<>(2);
    private final Map<Integer, MapleSummon> summons = Collections.synchronizedMap(new LinkedHashMap<>(6, 0.7f));
    private BuddyList buddylist;
    private final Map<Integer, MapleCoolDownValueHolder> coolDowns = new LinkedHashMap<>();
    private final CheatTracker anticheat;
    private ScheduledFuture<?> dragonBloodSchedule;
    private ScheduledFuture<?> mapTimeLimitTask;
    private int guildid, guildrank, allianceRank;
    private MapleGuildCharacter mgc;
    private int paypalnx, maplepoints, cardnx;
    private boolean incs, inmts;
    private int currentPage = 0, currentType = 0, currentTab = 1;
    private MapleMessenger messenger;
    private int messengerposition = 4;
    private ScheduledFuture<?> hpDecreaseTask;
    private final List<MapleDisease> diseases = new ArrayList<>(3);
    private ScheduledFuture<?> beholderHealingSchedule;
    private ScheduledFuture<?> beholderBuffSchedule;
    private ScheduledFuture<?> BerserkSchedule;
    private boolean Berserk = false;
    public SummonMovementType getMovementType;
    private String chalktext;
    private int CP;
    private int totalCP;
    private int team;
    private boolean married = false;
    private int partnerid;
    private int marriageQuestLevel;
    private boolean canSmega = true;
    private boolean smegaEnabled = true;
    private boolean canTalk = true;
    private int zakumLvl;
    private final List<FakeCharacter> fakes = new ArrayList<>(2);
    private boolean isfake = false;
    private int clan;
    private int bombpoints;
    private int pvpkills;
    private int pvpdeaths;
    private int donatePoints;
    private MapleMount maplemount;
    private int gmtext;
    private int skill;
    private ISkill skil;
    private int maxDis;
    public int mpoints;
    private transient int wdef, mdef;
    private int energybar;
    private long afkTime;
    private long lastLogin;
    private int ringRequest;
    private boolean hasMerchant;
    //
    private int returnmap;
    private int trialreturnmap;
    private int monstertrialpoints;
    private int monstertrialtier;
    private long lasttrialtime;
    private Date lastdailyprize;
    private int votepoints;
    private List<List<Integer>> pastlives = new ArrayList<>(5);
    private final List<List<Integer>> newpastlives = new ArrayList<>(5);
    private int deathcount;
    private MapleMapObject lastdamagesource;
    private int highestlevelachieved = 1;
    private int suicides;
    private int paragonlevel;
    private int totalparagonlevel;
    private int bossreturnmap;
    private boolean expbonus = false;
    private int expbonusmulti = 1;
    private long expbonusend;
    private int eventpoints;
    private long lastelanrecharge;
    private long lastkillonmap;
    private long laststrengthening;
    private int deathpenalty;
    private int deathfactor;
    private boolean truedamage;
    private int battleshiphp;
    private final List<IItem> unclaimedItems = new ArrayList<>(4);
    private boolean hasMagicGuard = false;
    private ScheduledFuture<?> magicGuardCancelTask;
    private boolean scpqflag = false;
    private boolean showPqPoints = true;
    private int readingTime;
    ScheduledFuture<?> readingTask;
    private int pastLifeExp = 1;
    private boolean genderFilter = true;
    private ScheduledFuture<?> forcedWarp;
    private long overflowExp;
    private boolean invincible = false;
    private ScheduledFuture<?> bossHpTask;
    private ScheduledFuture<?> bossHpCancelTask;
    private int initialVotePoints, initialNx;
    private boolean zakDc = false;
    private final Map<IItem, Short> buyBacks = new LinkedHashMap<>();
    private boolean showDpm = false;
    private boolean showSnipeDmg = false;
    private int preEventMap;
    private long lastSamsara;
    public static final long SAMSARA_COOLDOWN = 3L * 60L * 60L * 1000L;
    // New quest apparatus:
    private byte questSlots = (byte) 1;
    private final List<CQuest> cQuests = new ArrayList<>(4);
    private final Map<Integer, CQuestStatus> completedCQuests = new LinkedHashMap<>();
    private final Set<Integer> feats = new TreeSet<>();
    private int questEffectiveLevel;

    public MapleCharacter() {
        setStance(0);
        inventory = new MapleInventory[MapleInventoryType.values().length];
        for (MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type, (byte) 100);
        }
        savedLocations = new int[SavedLocationType.values().length];
        for (int i = 0; i < SavedLocationType.values().length; ++i) {
            savedLocations[i] = -1;
        }
        quests = new LinkedHashMap<>();
        anticheat = new CheatTracker(this);
        afkTime = System.currentTimeMillis();
        setPosition(new Point());
    }

    public MapleCharacter getThis() {
        return this;
    }

    public static MapleCharacter loadCharFromDB(int charid,
                                                MapleClient client,
                                                boolean channelserver) throws SQLException {
        MapleCharacter ret = new MapleCharacter();
        ret.client = client;
        ret.id = charid;
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
        ps.setInt(1, charid);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            rs.close();
            ps.close();
            throw new RuntimeException("Loading the character failed (character not found).");
        }
        ret.name = rs.getString("name");
        ret.level = rs.getInt("level");
        ret.pvpdeaths = rs.getInt("pvpdeaths");
        ret.pvpkills = rs.getInt("pvpkills");
        ret.reborns = rs.getInt("reborns");
        ret.fame = rs.getInt("fame");
        ret.str = rs.getInt("str");
        ret.dex = rs.getInt("dex");
        ret.int_ = rs.getInt("int");
        ret.luk = rs.getInt("luk");
        ret.exp.set(rs.getInt("exp"));
        if (ret.exp.get() < 0) {
            ret.exp.set(0);
        }
        ret.hp = rs.getInt("hp");
        if (ret.hp < 50 && ret.hp > 0) {
            ret.hp = 50;
        }
        ret.maxhp = rs.getInt("maxhp");
        ret.mp = rs.getInt("mp");
        if (ret.mp < 50 && ret.mp > 0) {
            ret.mp = 50;
        }
        ret.maxmp = rs.getInt("maxmp");
        ret.hpApUsed = rs.getInt("hpApUsed");
        ret.mpApUsed = rs.getInt("mpApUsed");
        ret.hasMerchant = rs.getInt("HasMerchant") == 1;
        ret.remainingSp = rs.getInt("sp");
        ret.remainingAp = rs.getInt("ap");
        ret.meso.set(rs.getInt("meso"));
        ret.gmLevel = rs.getInt("gm");
        ret.clan = rs.getInt("clan");
        int mountexp = rs.getInt("mountexp");
        int mountlevel = rs.getInt("mountlevel");
        int mounttiredness = rs.getInt("mounttiredness");
        ret.married = rs.getInt("married") != 0;
        ret.partnerid = rs.getInt("partnerid");
        ret.marriageQuestLevel = rs.getInt("marriagequest");
        ret.zakumLvl = rs.getInt("zakumLvl");
        ret.skinColor = MapleSkinColor.getById(rs.getInt("skincolor"));
        ret.gender = rs.getInt("gender");
        ret.job = MapleJob.getById(rs.getInt("job"));
        ret.hair = rs.getInt("hair");
        ret.face = rs.getInt("face");
        ret.accountid = rs.getInt("accountid");
        ret.mapid = rs.getInt("map");
        ret.initialSpawnPoint = rs.getInt("spawnpoint");
        ret.world = rs.getInt("world");
        ret.rank = rs.getInt("rank");
        ret.rankMove = rs.getInt("rankMove");
        ret.jobRank = rs.getInt("jobRank");
        ret.jobRankMove = rs.getInt("jobRankMove");
        ret.guildid = rs.getInt("guildid");
        ret.guildrank = rs.getInt("guildrank");
        ret.allianceRank = rs.getInt("allianceRank");
        //
        ret.returnmap = rs.getInt("returnmap");
        ret.trialreturnmap = rs.getInt("trialreturnmap");
        ret.monstertrialpoints = rs.getInt("monstertrialpoints");
        ret.monstertrialtier = rs.getInt("monstertrialtier");
        ret.lasttrialtime = rs.getLong("lasttrialtime");
        ret.deathcount = rs.getInt("deathcount");
        ret.highestlevelachieved = rs.getInt("highestlevelachieved");
        ret.suicides = rs.getInt("suicides");
        ret.paragonlevel = rs.getInt("paragonlevel");
        ret.totalparagonlevel = rs.getInt("totalparagonlevel");
        if (ret.totalparagonlevel < ret.paragonlevel + ret.level) {
            ret.totalparagonlevel = ret.paragonlevel + ret.level;
        }
        ret.bossreturnmap = rs.getInt("bossreturnmap");
        ret.expbonusend = rs.getLong("expbonusend");
        ret.expbonus = System.currentTimeMillis() < ret.expbonusend;
        ret.expbonusmulti = rs.getInt("expmulti");
        ret.eventpoints = rs.getInt("eventpoints");
        ret.lastelanrecharge = rs.getLong("lastelanrecharge");
        ret.laststrengthening = rs.getLong("laststrengthening");
        ret.deathpenalty = rs.getInt("deathpenalty");
        ret.deathfactor = rs.getInt("deathfactor");
        ret.truedamage = rs.getInt("truedamage") != 0;
        ret.scpqflag = rs.getInt("scpqflag") != 0;
        ret.overflowExp = rs.getLong("overflowexp");
        ret.zakDc = rs.getInt("zakdc") == 1;
        ret.lastSamsara = rs.getLong("lastsamsara");
        ret.questSlots = (byte) rs.getInt("questslots");
        ret.battleshiphp = 0;
        //
        if (ret.guildid > 0) {
            ret.mgc = new MapleGuildCharacter(ret);
        }
        int buddyCapacity = rs.getInt("buddyCapacity");
        ret.buddylist = new BuddyList(buddyCapacity);
        ret.gmtext = rs.getInt("gmtext");
        if (ret.mapid >= 1000 && ret.mapid <= 1006) {
            ret.mapid = ret.trialreturnmap;
        } else if (ret.mapid == 3000) {
            ret.mapid = ret.bossreturnmap;
        } else if (ret.mapid == 240060200) {
            ret.mapid = 240040700;
        }
        if (channelserver) {
            MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
            ret.map = mapFactory.getMap(ret.mapid);
            if (ret.map == null) {
                ret.map = mapFactory.getMap(100000000);
            }
            MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
            if (portal == null) {
                portal = ret.map.getPortal(0);
                ret.initialSpawnPoint = 0;
            }
            ret.setPosition(portal.getPosition());
            int partyid = rs.getInt("party");
            if (partyid >= 0) {
                try {
                    MapleParty party = client.getChannelServer().getWorldInterface().getParty(partyid);
                    if (party != null && party.getMemberById(ret.id) != null) {
                        ret.party = party;
                    }
                } catch (RemoteException e) {
                    client.getChannelServer().reconnectWorld();
                }
            }
            int messengerid = rs.getInt("messengerid");
            int position = rs.getInt("messengerposition");
            if (messengerid > 0 && position < 4 && position > -1) {
                try {
                    WorldChannelInterface wci = ChannelServer.getInstance(client.getChannel()).getWorldInterface();
                    MapleMessenger messenger = wci.getMessenger(messengerid);
                    if (messenger != null) {
                        ret.messenger = messenger;
                        ret.messengerposition = position;
                    }
                } catch (RemoteException re) {
                    client.getChannelServer().reconnectWorld();
                }
            }
        }
        rs.close();
        ps.close();
        ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
        ps.setInt(1, ret.accountid);
        rs = ps.executeQuery();
        if (rs.next()) {
            ret.getClient().setAccountName(rs.getString("name"));
            ret.getClient().setAccountPass(rs.getString("password"));
            ret.getClient().setGuest(rs.getInt("guest") > 0);
            ret.donatePoints = rs.getInt("donorPoints");
            ret.lastLogin = rs.getLong("LastLoginInMilliseconds");
            ret.paypalnx = rs.getInt("paypalNX");
            ret.initialNx = ret.paypalnx;
            ret.maplepoints = rs.getInt("mPoints");
            ret.cardnx = rs.getInt("cardNX");
            ret.lastdailyprize = new Date(rs.getLong("lastdailyprize"));
            ret.votepoints = rs.getInt("votepoints");
            ret.initialVotePoints = ret.votepoints;
        }
        rs.close();
        ps.close();
        String sql =
            "SELECT * FROM inventoryitems LEFT JOIN inventoryequipment USING (inventoryitemid) WHERE characterid = ?";
        if (!channelserver) {
            sql += " AND inventorytype = " + MapleInventoryType.EQUIPPED.getType();
        }
        ps = con.prepareStatement(sql);
        ps.setInt(1, charid);
        rs = ps.executeQuery();
        while (rs.next()) {
            MapleInventoryType type = MapleInventoryType.getByType((byte) rs.getInt("inventorytype"));
            if (type.equals(MapleInventoryType.EQUIP) || type.equals(MapleInventoryType.EQUIPPED)) {
                int itemid = rs.getInt("itemid");
                Equip equip = new Equip(itemid, (byte) rs.getInt("position"), rs.getInt("ringid"));
                equip.setOwner(rs.getString("owner"));
                equip.setQuantity((short) rs.getInt("quantity"));
                equip.setAcc((short) rs.getInt("acc"));
                equip.setAvoid((short) rs.getInt("avoid"));
                equip.setDex((short) rs.getInt("dex"));
                equip.setHands((short) rs.getInt("hands"));
                equip.setHp((short) rs.getInt("hp"));
                equip.setInt((short) rs.getInt("int"));
                equip.setJump((short) rs.getInt("jump"));
                equip.setLuk((short) rs.getInt("luk"));
                equip.setMatk((short) rs.getInt("matk"));
                equip.setMdef((short) rs.getInt("mdef"));
                equip.setMp((short) rs.getInt("mp"));
                equip.setSpeed((short) rs.getInt("speed"));
                equip.setStr((short) rs.getInt("str"));
                equip.setWatk((short) rs.getInt("watk"));
                equip.setWdef((short) rs.getInt("wdef"));
                equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                equip.setLocked((byte) rs.getInt("locked"));
                equip.setLevel((byte) rs.getInt("level"));
                ret.getInventory(type).addFromDB(equip);
            } else {
                Item item = new Item(
                    rs.getInt("itemid"),
                    (byte) rs.getInt("position"),
                    (short) rs.getInt("quantity"),
                    rs.getInt("petid")
                );
                item.setOwner(rs.getString("owner"));
                ret.getInventory(type).addFromDB(item);
            }
        }
        rs.close();
        ps.close();
        if (channelserver) {
            ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            PreparedStatement pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");
            while (rs.next()) {
                MapleQuest q = MapleQuest.getInstance(rs.getInt("quest"));
                MapleQuestStatus status =
                    new MapleQuestStatus(q, MapleQuestStatus.Status.getById(rs.getInt("status")));
                long cTime = rs.getLong("time");
                if (cTime > -1) {
                    status.setCompletionTime(cTime * 1000);
                }
                status.setForfeited(rs.getInt("forfeited"));
                ret.quests.put(q, status);
                pse.setInt(1, rs.getInt("queststatusid"));
                ResultSet rsMobs = pse.executeQuery();
                while (rsMobs.next()) {
                    status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                }
                rsMobs.close();
            }
            rs.close();
            ps.close();
            pse.close();
            ps = con.prepareStatement("SELECT skillid,skilllevel,masterlevel FROM skills WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret.skills.put(
                    SkillFactory.getSkill(rs.getInt("skillid")),
                    new SkillEntry(
                        rs.getInt("skilllevel"),
                        rs.getInt("masterlevel")
                    )
                );
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            while (rs.next()) {
                int skill1 = rs.getInt("skill1");
                int skill2 = rs.getInt("skill2");
                int skill3 = rs.getInt("skill3");
                String name = rs.getString("name");
                int shout = rs.getInt("shout");
                int position = rs.getInt("position");
                SkillMacro macro = new SkillMacro(skill1, skill2, skill3, name, shout, position);
                ret.skillMacros[position] = macro;
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            while (rs.next()) {
                int key = rs.getInt("key");
                int type = rs.getInt("type");
                int action = rs.getInt("action");
                ret.keymap.put(key, new MapleKeyBinding(type, action));
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            while (rs.next()) {
                String locationType = rs.getString("locationtype");
                int mapid = rs.getInt("map");
                ret.savedLocations[SavedLocationType.valueOf(locationType).ordinal()] = mapid;
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement(
                "SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30"
            );
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            ret.lastFameTime = 0;
            ret.lastMonthFameIds = new ArrayList<>(31);
            while (rs.next()) {
                ret.lastFameTime = Math.max(ret.lastFameTime, rs.getTimestamp("when").getTime());
                ret.lastMonthFameIds.add(rs.getInt("characterid_to"));
            }
            rs.close();
            ps.close();
            ret.buddylist.loadFromDb(charid);
            ret.storage = MapleStorage.loadOrCreateFromDB(ret.accountid);

            //
            ps = con.prepareStatement("SELECT * FROM pastlives WHERE characterid = ? ORDER BY death DESC");
            ps.setInt(1, ret.id);
            rs = ps.executeQuery();
            ret.pastlives = new ArrayList<>(5);
            int pastlifecount = -1;
            while (rs.next()) {
                pastlifecount++;
                if (pastlifecount < 5) {
                    List<Integer> temppastlife = new ArrayList<>(3);
                    temppastlife.add(rs.getInt("level"));
                    temppastlife.add(rs.getInt("job"));
                    temppastlife.add(rs.getInt("lastdamagesource"));
                    ret.pastlives.add(temppastlife);
                } else {
                    break;
                }
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement(
                "SELECT questid, queststatus, effectivelevel " +
                    "FROM cqueststatus WHERE characterid = ? AND queststatus != -1"
            );
            ps.setInt(1, ret.id);
            rs = ps.executeQuery();
            int lowestEffectiveLevel = Integer.MAX_VALUE;
            while (rs.next()) {
                int questStatus = rs.getInt("queststatus");
                int questId = rs.getInt("questid");
                int effectiveLevel = rs.getInt("effectivelevel");
                if (questId == 0) continue;
                if (questStatus == 0) { // In progress
                    if (ret.cQuests.size() < ret.questSlots) {
                        CQuest cQuest = new CQuest(ret);
                        cQuest.loadQuest(questId, effectiveLevel);
                        if (effectiveLevel > 0 && effectiveLevel < lowestEffectiveLevel) {
                            lowestEffectiveLevel = effectiveLevel;
                        }
                        if (cQuest.getQuest().requiresMonsterTargets()) {
                            PreparedStatement ps1 =
                                con.prepareStatement(
                                    "SELECT monsterid, killcount FROM questkills " +
                                        "WHERE characterid = ? AND questid = ?"
                                );
                            ps1.setInt(1, ret.id);
                            ps1.setInt(2, questId);
                            ResultSet rs1 = ps1.executeQuery();
                            while (rs1.next()) {
                                int monsterId = rs1.getInt("monsterid");
                                int killCount = rs1.getInt("killcount");
                                cQuest.setQuestKill(monsterId, killCount);
                            }
                            rs1.close();
                            ps1.close();
                        }
                        if (cQuest.getQuest().requiresOtherObjectives()) {
                            PreparedStatement ps1 =
                                con.prepareStatement(
                                    "SELECT objname, completedcount FROM questobjs " +
                                        "WHERE characterid = ? AND questid = ?"
                                );
                            ps1.setInt(1, ret.id);
                            ps1.setInt(2, questId);
                            ResultSet rs1 = ps1.executeQuery();
                            while (rs1.next()) {
                                String objName = rs1.getString("objname");
                                int completedCount = rs1.getInt("completedcount");
                                cQuest.setObjectiveProgress(objName, completedCount);
                            }
                            rs1.close();
                            ps1.close();
                        }
                        ret.cQuests.add(cQuest);
                    }
                } else { // Completed
                    ret.completedCQuests.put(questId, CQuestStatus.getByValue(questStatus));
                }
            }
            rs.close();
            ps.close();
            while (ret.cQuests.size() < ret.questSlots) {
                CQuest newQuest = new CQuest(ret);
                newQuest.loadQuest(0);
                ret.cQuests.add(newQuest);
            }
            if (lowestEffectiveLevel < Integer.MAX_VALUE) {
                ret.setQuestEffectiveLevel(lowestEffectiveLevel);
            }

            ps = con.prepareStatement("SELECT featid FROM feats WHERE characterid = ?");
            ps.setInt(1, ret.id);
            rs = ps.executeQuery();
            while (rs.next()) {
                int featId = rs.getInt("featid");
                ret.feats.add(featId);
            }
            rs.close();
            ps.close();
            //
        }
        if (ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null) {
            ret.maplemount =
                new MapleMount(
                    ret,
                    ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18).getItemId(),
                    1004
                );
            ret.maplemount.setExp(mountexp);
            ret.maplemount.setLevel(mountlevel);
            ret.maplemount.setTiredness(mounttiredness);
            ret.maplemount.setActive(false);
        } else {
            ret.maplemount = new MapleMount(ret, 0, 1004);
            ret.maplemount.setExp(mountexp);
            ret.maplemount.setLevel(mountlevel);
            ret.maplemount.setTiredness(mounttiredness);
            ret.maplemount.setActive(false);
        }
        ret.lastkillonmap = 0L;
        ret.preEventMap = 0;
        ret.recalcLocalStats();
        ret.silentEnforceMaxHpMp();

        return ret;
    }

    public static MapleCharacter getDefault(MapleClient client, int chrid) {
        MapleCharacter ret = getDefault(client);
        ret.id = chrid;
        return ret;
    }

    public static MapleCharacter getDefault(MapleClient client) {
        MapleCharacter ret = new MapleCharacter();
        ret.client = client;
        ret.hp = 50;
        ret.maxhp = 50;
        ret.mp = 5;
        ret.maxmp = 5;
        ret.map = null;
        ret.exp.set(0);
        ret.gmLevel = 0;
        ret.clan = -1;
        ret.job = MapleJob.BEGINNER;
        ret.meso.set(0);
        ret.level = 1;
        ret.reborns = 0;
        ret.pvpdeaths = 0;
        ret.pvpkills = 0;
        ret.bombpoints = 0;
        ret.accountid = client.getAccID();
        ret.buddylist = new BuddyList(20);
        ret.CP = 0;
        ret.totalCP = 0;
        ret.team = -1;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            ResultSet rs = ps.executeQuery();
            //rs = ps.executeQuery();
            if (rs.next()) {
                ret.getClient().setAccountName(rs.getString("name"));
                ret.getClient().setAccountPass(rs.getString("password"));
                ret.getClient().setGuest(rs.getInt("guest") > 0);
                ret.donatePoints = rs.getInt("donorPoints");
                ret.paypalnx = rs.getInt("paypalNX");
                ret.initialNx = ret.paypalnx;
                ret.maplepoints = rs.getInt("mPoints");
                ret.cardnx = rs.getInt("cardNX");
                ret.lastLogin = rs.getLong("LastLoginInMilliseconds");
                ret.lastdailyprize = new Date(rs.getLong("lastdailyprize"));
                ret.votepoints = rs.getInt("votepoints");
                ret.initialVotePoints = ret.votepoints;
            }
            rs.close();
            ps.close();
        } catch (SQLException sqle) {
            System.err.println("Error getting default: " + sqle);
        }
        ret.incs = false;
        ret.inmts = false;
        ret.APQScore = 0;
        ret.maplemount = null;
        ret.scpqflag = false;
        ret.overflowExp = 0L;
        ret.lastSamsara = 0L;
        ret.preEventMap = 0;
        ret.setDefaultKeyMap();
        ret.recalcLocalStats();
        return ret;
    }

    public void saveToDB(boolean update, boolean full) {
        Connection con = DatabaseConnection.getConnection();
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);
            PreparedStatement ps;
            if (update) {
                ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpApUsed = ?, mpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, messengerid = ?, messengerposition = ?, reborns = ?, pvpkills = ?, pvpdeaths = ?, clan = ?, mountlevel = ?, mountexp = ?, mounttiredness = ?, married = ?, partnerid = ?, zakumlvl = ?, marriagequest = ?, returnmap = ?, trialreturnmap = ?, monstertrialpoints = ?, monstertrialtier = ?, lasttrialtime = ?, deathcount = ?, highestlevelachieved = ?, suicides = ?, paragonlevel = ?, bossreturnmap = ?, totalparagonlevel = ?, expbonusend = ?, eventpoints = ?, lastelanrecharge = ?, laststrengthening = ?, deathpenalty = ?, deathfactor = ?, truedamage = ?, expmulti = ?, scpqflag = ?, overflowexp = ?, zakdc = ?, lastsamsara = ?, questslots = ? WHERE id = ?");
            } else {
                ps = con.prepareStatement("INSERT INTO characters (level, fame, str, dex, luk, `int`, exp, hp, mp, maxhp, maxmp, sp, ap, gm, skincolor, gender, job, hair, face, map, meso, hpApUsed, mpApUsed, spawnpoint, party, buddyCapacity, messengerid, messengerposition, reborns, pvpkills, pvpdeaths, clan, mountlevel, mountexp, mounttiredness, married, partnerid, zakumlvl, marriagequest, returnmap, trialreturnmap, monstertrialpoints, monstertrialtier, lasttrialtime, deathcount, highestlevelachieved, suicides, paragonlevel, bossreturnmap, totalparagonlevel, expbonusend, eventpoints, lastelanrecharge, laststrengthening, deathpenalty, deathfactor, truedamage, expmulti, scpqflag, overflowexp, zakdc, lastsamsara, questslots, accountid, name, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            }
            ps.setInt(1, level);
            ps.setInt(2, fame);
            ps.setInt(3, str);
            ps.setInt(4, dex);
            ps.setInt(5, luk);
            ps.setInt(6, int_);
            ps.setInt(7, level < 250 ? exp.get() : 0);
            ps.setInt(8, hp);
            ps.setInt(9, mp);
            ps.setInt(10, maxhp);
            ps.setInt(11, maxmp);
            ps.setInt(12, remainingSp);
            ps.setInt(13, remainingAp);
            ps.setInt(14, gmLevel);
            ps.setInt(15, skinColor.getId());
            ps.setInt(16, gender);
            ps.setInt(17, job.getId());
            ps.setInt(18, hair);
            ps.setInt(19, face);
            if (map == null) {
                ps.setInt(20, 0);
            } else {
                if (map.getForcedReturnId() != 999999999) {
                    ps.setInt(20, map.getForcedReturnId());
                } else if (preEventMap > 0) {
                    ps.setInt(20, preEventMap);
                } else {
                    ps.setInt(20, map.getId());
                }
            }
            ps.setInt(21, meso.get());
            ps.setInt(22, hpApUsed);
            ps.setInt(23, mpApUsed);
            if (map == null || map.getId() == 610020000 || map.getId() == 610020001) {
                ps.setInt(24, 0);
            } else {
                MaplePortal closest = map.findClosestSpawnpoint(getPosition());
                if (closest != null) {
                    ps.setInt(24, closest.getId());
                } else {
                    ps.setInt(24, 0);
                }
            }
            if (party != null) {
                ps.setInt(25, party.getId());
            } else {
                ps.setInt(25, -1);
            }
            ps.setInt(26, buddylist.getCapacity());
            if (messenger != null) {
                ps.setInt(27, messenger.getId());
                ps.setInt(28, messengerposition);
            } else {
                ps.setInt(27, 0);
                ps.setInt(28, 4);
            }
            ps.setInt(29, reborns);
            ps.setInt(30, pvpkills);
            ps.setInt(31, pvpdeaths);
            ps.setInt(32, clan);
            if (maplemount != null) {
                ps.setInt(33, maplemount.getLevel());
                ps.setInt(34, maplemount.getExp());
                ps.setInt(35, maplemount.getTiredness());
            } else {
                ps.setInt(33, 1);
                ps.setInt(34, 0);
                ps.setInt(35, 0);
            }
            ps.setInt(36, married ? 1 : 0);
            ps.setInt(37, partnerid);
            ps.setInt(38, zakumLvl > 2 ? 2 : zakumLvl);
            ps.setInt(39, marriageQuestLevel);
            ps.setInt(40, returnmap);
            ps.setInt(41, trialreturnmap);
            ps.setInt(42, monstertrialpoints);
            ps.setInt(43, monstertrialtier);
            ps.setLong(44, lasttrialtime);
            ps.setInt(45, deathcount);
            ps.setInt(46, highestlevelachieved);
            ps.setInt(47, suicides);
            ps.setInt(48, paragonlevel);
            ps.setInt(49, bossreturnmap);
            ps.setInt(50, totalparagonlevel);
            ps.setLong(51, expbonusend);
            ps.setInt(52, eventpoints);
            ps.setLong(53, lastelanrecharge);
            ps.setLong(54, laststrengthening);
            ps.setInt(55, deathpenalty);
            ps.setInt(56, deathfactor);
            ps.setInt(57, truedamage ? 1 : 0);
            ps.setInt(58, expbonusmulti);
            ps.setInt(59, scpqflag ? 1 : 0);
            ps.setLong(60, overflowExp);
            if (map != null && map.getId() == 280030000) { // Zakum's Altar
                ps.setInt(61, 1);
            } else {
                ps.setInt(61, 0);
            }
            ps.setLong(62, lastSamsara);
            ps.setInt(63, questSlots);
            if (update) {
                ps.setInt(64, id);
            } else {
                ps.setInt(64, accountid);
                ps.setString(65, name);
                ps.setInt(66, world);
            }
            if (!full) {
                ps.executeUpdate();
                ps.close();
            } else {
                int updateRows = ps.executeUpdate();
                if (!update) {
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        id = rs.getInt(1);
                    } else {
                        throw new DatabaseException("Inserting char failed.");
                    }
                    rs.close();
                } else if (updateRows < 1) {
                    throw new DatabaseException("Character not in database (" + id + ")");
                }
                ps.close();
                for (int i = 0; i < 3; ++i) {
                    if (pets[i] != null) {
                        pets[i].saveToDb();
                    } else {
                        break;
                    }
                }
                deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
                for (int i = 0; i < 5; ++i) {
                    SkillMacro macro = skillMacros[i];
                    if (macro != null) {
                        ps = con.prepareStatement(
                            "INSERT INTO skillmacros " +
                                "(characterid, skill1, skill2, skill3, name, shout, position) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)"
                        );
                        ps.setInt(1, id);
                        ps.setInt(2, macro.getSkill1());
                        ps.setInt(3, macro.getSkill2());
                        ps.setInt(4, macro.getSkill3());
                        ps.setString(5, macro.getName());
                        ps.setInt(6, macro.getShout());
                        ps.setInt(7, i);
                        ps.executeUpdate();
                        ps.close();
                    }
                }
                deleteWhereCharacterId(con, "DELETE FROM inventoryitems WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO inventoryitems (characterid, itemid, inventorytype, position, quantity, owner, petid) VALUES (?, ?, ?, ?, ?, ?, ?)");
                PreparedStatement pse = con.prepareStatement("INSERT INTO inventoryequipment VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                for (MapleInventory iv : inventory) {
                    ps.setInt(3, iv.getType().getType());
                    for (IItem item : iv.list()) {
                        ps.setInt(1, id);
                        ps.setInt(2, item.getItemId());
                        ps.setInt(4, item.getPosition());
                        ps.setInt(5, item.getQuantity());
                        ps.setString(6, item.getOwner());
                        ps.setInt(7, item.getPetId());
                        ps.executeUpdate();
                        ResultSet rs = ps.getGeneratedKeys();
                        int itemid;
                        if (rs.next()) {
                            itemid = rs.getInt(1);
                        } else {
                            rs.close();
                            ps.close();
                            throw new DatabaseException("Inserting char failed.");
                        }
                        rs.close();
                        if (iv.getType().equals(MapleInventoryType.EQUIP) || iv.getType().equals(MapleInventoryType.EQUIPPED)) {
                            pse.setInt(1, itemid);
                            IEquip equip = (IEquip) item;
                            pse.setInt(2, equip.getUpgradeSlots());
                            pse.setInt(3, equip.getLevel());
                            pse.setInt(4, equip.getStr());
                            pse.setInt(5, equip.getDex());
                            pse.setInt(6, equip.getInt());
                            pse.setInt(7, equip.getLuk());
                            pse.setInt(8, equip.getHp());
                            pse.setInt(9, equip.getMp());
                            pse.setInt(10, equip.getWatk());
                            pse.setInt(11, equip.getMatk());
                            pse.setInt(12, equip.getWdef());
                            pse.setInt(13, equip.getMdef());
                            pse.setInt(14, equip.getAcc());
                            pse.setInt(15, equip.getAvoid());
                            pse.setInt(16, equip.getHands());
                            pse.setInt(17, equip.getSpeed());
                            pse.setInt(18, equip.getJump());
                            pse.setInt(19, equip.getRingId());
                            pse.setInt(20, equip.getLocked());
                            pse.executeUpdate();
                        }
                    }
                }
                ps.close();
                pse.close();
                deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`) VALUES (DEFAULT, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
                ps.setInt(1, id);
                for (MapleQuestStatus q : quests.values()) {
                    ps.setInt(2, q.getQuest().getId());
                    ps.setInt(3, q.getStatus().getId());
                    ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                    ps.setInt(5, q.getForfeited());
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.executeUpdate();
                    }
                    rs.close();
                }
                ps.close();
                pse.close();
                deleteWhereCharacterId(
                    con,
                    "DELETE FROM skills WHERE characterid = ?"
                );
                ps = con.prepareStatement(
                    "INSERT INTO skills (characterid, skillid, skilllevel, masterlevel) VALUES (?, ?, ?, ?)"
                );
                ps.setInt(1, id);
                for (Entry<ISkill, SkillEntry> skill_ : skills.entrySet()) {
                    ps.setInt(2, skill_.getKey().getId());
                    ps.setInt(3, skill_.getValue().skillevel);
                    ps.setInt(4, skill_.getValue().masterlevel);
                    ps.executeUpdate();
                }
                ps.close();
                deleteWhereCharacterId(con, "DELETE FROM keymap WHERE characterid = ?");
                ps = con.prepareStatement(
                    "INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)"
                );
                ps.setInt(1, id);
                for (Map.Entry<Integer, MapleKeyBinding> keybinding : keymap.entrySet()) {
                    ps.setInt(2, keybinding.getKey());
                    ps.setInt(3, keybinding.getValue().getType());
                    ps.setInt(4, keybinding.getValue().getAction());
                    ps.executeUpdate();
                }
                ps.close();
                deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
                ps = con.prepareStatement(
                    "INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)"
                );
                ps.setInt(1, id);
                for (SavedLocationType savedLocationType : SavedLocationType.values()) {
                    if (savedLocations[savedLocationType.ordinal()] != -1) {
                        ps.setString(2, savedLocationType.name());
                        ps.setInt(3, savedLocations[savedLocationType.ordinal()]);
                        ps.executeUpdate();
                    }
                }
                ps.close();
                deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ? AND pending = 0");
                ps = con.prepareStatement(
                    "INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 0)"
                );
                ps.setInt(1, id);
                for (BuddylistEntry entry : buddylist.getBuddies()) {
                    if (entry.isVisible()) {
                        ps.setInt(2, entry.getCharacterId());
                        ps.executeUpdate();
                    }
                }
                ps.close();
                // Checking DB for if votepoints/NX has been changed since the player logged in.
                // If so, the new votepoints/NX are factored into the new total.
                // This is to allow players to vote while online.
                ps = con.prepareStatement("SELECT paypalNX,votepoints FROM accounts WHERE id = ?");
                ps.setInt(1, client.getAccID());
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    rs.close();
                    ps.close();
                    throw new RuntimeException("Reading votepoints failed, acc ID: " + client.getAccID());
                }
                int dbNx = rs.getInt("paypalNX");
                int dbVotepoints = rs.getInt("votepoints");
                rs.close();
                ps.close();

                ps = con.prepareStatement(
                    "UPDATE accounts SET `paypalNX` = ?, `mPoints` = ?, `cardNX` = ?, " +
                        "`donorPoints` = ?, `lastdailyprize` = ?, `votepoints` = ? WHERE id = ?"
                );
                int newDbNx = paypalnx + (dbNx - initialNx);
                ps.setInt(1, paypalnx + (dbNx - initialNx));
                initialNx = newDbNx;
                paypalnx = newDbNx;

                ps.setInt(2, maplepoints);
                ps.setInt(3, cardnx);
                ps.setInt(4, donatePoints);
                ps.setLong(5, lastdailyprize.getTime());

                int newDbVotePoints = votepoints + (dbVotepoints - initialVotePoints);
                ps.setInt(6, newDbVotePoints);
                initialVotePoints = newDbVotePoints;
                votepoints = newDbVotePoints;

                ps.setInt(7, client.getAccID());
                ps.executeUpdate();
                ps.close();
                if (storage != null) {
                    storage.saveToDB();
                }

                ps = con.prepareStatement("SELECT * FROM pastlives WHERE characterid = ? ORDER BY death DESC");
                ps.setInt(1, id);
                rs = ps.executeQuery();
                int lastlifenotupdated = -1;
                if (rs.next()) {
                    lastlifenotupdated = rs.getInt("death");
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement(
                    "INSERT INTO pastlives " +
                        "(`characterid`, `death`, `level`, `job`, `lastdamagesource`) VALUES (?, ?, ?, ?, ?)"
                );
                ps.setInt(1, id);
                for (List<Integer> pastlife : newpastlives) {
                    lastlifenotupdated++;
                    ps.setInt(2, lastlifenotupdated);
                    ps.setInt(3, pastlife.get(0));
                    ps.setInt(4, pastlife.get(1));
                    ps.setInt(5, pastlife.get(2));
                    ps.executeUpdate();
                }
                ps.close();
                //
                deleteWhereCharacterId(con, "DELETE FROM cqueststatus WHERE characterid = ?");
                ps = con.prepareStatement(
                    "INSERT INTO cqueststatus " +
                        "(`characterid`, `questid`, `queststatus`, `effectivelevel`) VALUES (?, ?, ?, ?)"
                );
                ps.setInt(1, id);
                for (CQuest cQuest : cQuests) {
                    int qid = cQuest.getQuest().getId();
                    if (qid == 0) continue;
                    ps.setInt(2, qid);
                    ps.setInt(3, 0);
                    ps.setInt(4, cQuest.getEffectivePlayerLevel());
                    ps.executeUpdate();
                }
                for (Map.Entry<Integer, CQuestStatus> completed : completedCQuests.entrySet()) {
                    ps.setInt(2, completed.getKey());
                    ps.setInt(3, completed.getValue().getValue());
                    ps.setInt(4, 0);
                    ps.executeUpdate();
                }
                ps.close();
                deleteWhereCharacterId(con, "DELETE FROM questobjs WHERE characterid = ?");
                ps = con.prepareStatement(
                    "INSERT INTO questobjs " +
                        "(`characterid`, `questid`, `objname`, `completedcount`) VALUES (?, ?, ?, ?)"
                );
                ps.setInt(1, id);
                for (CQuest cQuest : cQuests) {
                    int qid = cQuest.getQuest().getId();
                    if (qid == 0) continue;
                    for (String objEntry : cQuest.getQuest().readOtherObjectives().keySet()) {
                        ps.setInt(2, qid);
                        ps.setString(3, objEntry);
                        ps.setInt(4, cQuest.getObjectiveProgress(objEntry));
                        ps.executeUpdate();
                    }
                }
                ps.close();
                deleteWhereCharacterId(con, "DELETE FROM questkills WHERE characterid = ?");
                ps = con.prepareStatement(
                    "INSERT INTO questkills (`characterid`, `questid`, `monsterid`, `killcount`) VALUES (?, ?, ?, ?)"
                );
                ps.setInt(1, id);
                for (CQuest cQuest : cQuests) {
                    int qid = cQuest.getQuest().getId();
                    if (qid == 0) continue;
                    for (Integer mobEntry : cQuest.getQuest().readMonsterTargets().keySet()) {
                        ps.setInt(2, qid);
                        ps.setInt(3, mobEntry);
                        ps.setInt(4, cQuest.getQuestKills(mobEntry));
                        ps.executeUpdate();
                    }
                }
                ps.close();
                deleteWhereCharacterId(con, "DELETE FROM feats WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO feats (`characterid`, `featid`) VALUES (?, ?)");
                ps.setInt(1, id);
                for (Integer featId : feats) {
                    ps.setInt(2, featId);
                    ps.executeUpdate();
                }
                ps.close();
                //
            }
            con.commit();
        } catch (Exception e) {
            System.err.println("[Saving] Error saving character data: " + e);
            try {
                con.rollback();
            } catch (SQLException sqle) {
                System.err.println("[Saving] Error rolling back: " + sqle);
            }
        } finally {
            try {
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException sqle) {
                System.err.println("[Saving] Error going back to autocommit mode: " + sqle);
            }
        }
    }
    //
    private void resetAllQuestProgress() {
        quests.clear();
    }

    public int getReturnMap() {
        return returnmap;
    }

    public void setReturnMap(int rm) {
        returnmap = rm;
    }

    public int getTrialReturnMap() {
        return trialreturnmap;
    }

    public void setTrialReturnMap(int trm) {
        trialreturnmap = trm;
    }

    public int getMonsterTrialPoints() {
        return monstertrialpoints;
    }

    public void setMonsterTrialPoints(int mtp) {
        monstertrialpoints = mtp;
    }

    public int getMonsterTrialTier() {
        return monstertrialtier;
    }

    public void setMonsterTrialTier(int mtt) {
        monstertrialtier = mtt;
    }

    public long getLastTrialTime() {
        return lasttrialtime;
    }

    public void setLastTrialTime(long ltt) {
        lasttrialtime = ltt;
    }

    public int getBossReturnMap() {
        return bossreturnmap;
    }

    public void setBossReturnMap(int brm) {
        bossreturnmap = brm;
    }

    public List<List<Integer>> getPastLives() {
        return pastlives;
    }

    public Optional<List<Integer>> getLastPastLife() {
        if (pastlives.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pastlives.get(0));
    }

    public void addNewPastLife(int level, int jobid, MapleMapObject lds) {
        List<Integer> newpastlife = new ArrayList<>(3);
        newpastlife.add(level);
        newpastlife.add(jobid);
        if (lds == null) {
            newpastlife.add(0);
            incrementSuicides();
        } else if (lds.getType() == MapleMapObjectType.MONSTER) {
            MapleMonster mm = (MapleMonster) lds;
            newpastlife.add(mm.getId());
        } else {
            newpastlife.add(0);
            incrementSuicides();
        }
        addToParagonLevel(level);
        setTotalParagonLevel(paragonlevel + getLevel());
        if (level > highestlevelachieved) {
            setHighestLevelAchieved(level);
        }
        pastlives.add(0, newpastlife);
        newpastlives.add(0, newpastlife);
    }

    public int getDeathCount() {
        return deathcount;
    }

    public void setDeathCount(int dc) {
        deathcount = dc;
    }

    public void incrementDeathCount() {
        deathcount++;
    }

    public int getHighestLevelAchieved() {
        return highestlevelachieved;
    }

    public void setHighestLevelAchieved(int hla) {
        highestlevelachieved = hla;
    }

    public int getSuicides() {
        return suicides;
    }

    public void setSuicides(int s) {
        suicides = s;
    }

    public void incrementSuicides() {
        suicides++;
    }

    public int getParagonLevel() {
        return paragonlevel;
    }

    public void setParagonLevel(int pl) {
        paragonlevel = pl;
    }

    public void addToParagonLevel(int levelcount) {
        paragonlevel += levelcount;
    }

    public int getTotalParagonLevel() {
        return totalparagonlevel;
    }

    public void setTotalParagonLevel(int tpl) {
        totalparagonlevel = tpl;
    }

    public void addToTotalParagonLevel(int levelcount) {
        totalparagonlevel += levelcount;
    }

    public MapleMapObject getLastDamageSource() {
        return lastdamagesource;
    }

    public void setLastDamageSource(MapleMapObject lds) {
        lastdamagesource = lds;
    }

    public boolean getExpBonus() {
        return expbonus;
    }

    public void setExpBonus(boolean eb) {
        expbonus = eb;
    }

    public int getExpBonusMulti() {
        return expbonusmulti;
    }

    public void setExpBonusMulti(int ebm) {
        expbonusmulti = ebm;
    }

    public long getExpBonusEnd() {
        return expbonusend;
    }

    public void setExpBonusEnd(long ebe) {
        expbonusend = ebe;
    }

    public void reactivateExpBonus() {
        if (System.currentTimeMillis() < expbonusend) {
            setExpBonus(true);
            TimerManager.getInstance().schedule(() -> setExpBonus(false), getExpBonusEnd() - System.currentTimeMillis());
        } else {
            setExpBonus(false);
        }
    }

    public void activateExpBonus(int timeinsec, int multi) {
        setExpBonus(true);
        setExpBonusMulti(multi);
        setExpBonusEnd(System.currentTimeMillis() + (long) timeinsec * 1000L);
        TimerManager.getInstance().schedule(() -> setExpBonus(false), (long) timeinsec * 1000L);
    }

    public int getEventPoints() {
        return eventpoints;
    }

    public void setEventPoints(int ep) {
        eventpoints = ep;
    }

    public long getLastElanRecharge() {
        return lastelanrecharge;
    }

    public void setLastElanRecharge(long ler) {
        lastelanrecharge = ler;
    }

    public void updateLastElanRecharge() {
        lastelanrecharge = System.currentTimeMillis();
    }

    public boolean canElanRecharge() {
        return System.currentTimeMillis() - getLastElanRecharge() >=
            5L * 24L * 60L * 60L * 1000L; // 5 days/120 hours
    }

    public String getElanRechargeTimeString() {
        long time = System.currentTimeMillis() - getLastElanRecharge();
        if (time < 5L * 24L * 60L * 60L * 1000L) {
            time = 5L * 24L * 60L * 60L * 1000L - time;
            long hours = time / 3600000L;
            time %= 3600000L;
            long minutes = time / 60000L;
            time %= 60000L;
            long seconds = time / 1000L;
            return
                "You must wait another " +
                    hours +
                    " hours, " +
                    minutes +
                    " minutes, and " +
                    seconds +
                    " seconds to recharge your Elans Vital.";
        } else {
            return "You may recharge your Elans Vital.";
        }
    }

    public long getLastStrengthening() {
        return laststrengthening;
    }

    public void setLastStrengthening(long ls) {
        this.laststrengthening = ls;
    }

    public void updateLastStrengthening() {
        this.laststrengthening = System.currentTimeMillis();
    }

    public boolean canStrengthen() {
        return System.currentTimeMillis() - getLastStrengthening() >= 24L * 60L * 60L * 1000L; // 1 day/24 hours
    }

    public String getStrengtheningTimeString() {
        long time = System.currentTimeMillis() - getLastStrengthening();
        if (time < 24L * 60L * 60L * 1000L) {
            time = 24L * 60L * 60L * 1000L - time;
            long hours = time / 3600000L;
            time %= 3600000L;
            long minutes = time / 60000L;
            time %= 60000L;
            long seconds = time / 1000L;
            return
                "You must wait another " +
                    hours +
                    " hours, " +
                    minutes +
                    " minutes, and " +
                    seconds +
                    " seconds before you can rest again.";
        } else {
            return "You may rest.";
        }
    }

    public int getDeathPenalty() {
        return deathpenalty;
    }

    public void setDeathPenalty(int dp) {
        deathpenalty = dp;
    }

    public int incrementDeathPenalty(int increment) {
        deathpenalty += increment;
        return deathpenalty;
    }

    public int getDeathFactor() {
        return deathfactor;
    }

    public void setDeathFactor(int df) {
        deathfactor = df;
    }

    public int incrementDeathFactor(int increment) {
        deathfactor += increment;
        return deathfactor;
    }

    public int incrementDeathPenaltyAndRecalc(int increment) {
        deathfactor++;

        int hppenalty, mppenalty;
        switch (getJob().getId() / 100) {
            case 0: // Beginner
                hppenalty = 95;
                mppenalty = 0;
                break;
            case 1: // Warrior
                hppenalty = 400;
                mppenalty = 60;
                break;
            case 2: // Mage
                hppenalty = 75;
                mppenalty = 400;
                break;
            case 3: // Archer
                hppenalty = 135;
                mppenalty = 70;
                break;
            case 4: // Rogue
                hppenalty = 145;
                mppenalty = 70;
                break;
            case 5: // Pirate
                hppenalty = 135;
                mppenalty = 70;
                break;
            default: // GM, or something went wrong
                hppenalty = 135;
                mppenalty = 75;
                break;
        }
        int olddeathpenalty = deathpenalty;

        int fakehp = getMaxHp() - hppenalty;
        int fakemp = getMaxMp() - mppenalty;
        while (fakehp > 50 && fakemp > 50 && increment > 0) {
            deathpenalty++;
            increment--;
            fakehp -= hppenalty;
            fakemp -= mppenalty;
        }

        int newhp = getMaxHp() - ((deathpenalty - olddeathpenalty) * hppenalty);
        if (getHp() > newhp) {
            setHp(newhp);
            updateSingleStat(MapleStat.HP, newhp);
        }
        setMaxHp(newhp);
        int newmp = getMaxMp() - ((deathpenalty - olddeathpenalty) * mppenalty);
        if (getMp() > newmp) {
            setMp(newmp);
            updateSingleStat(MapleStat.MP, newmp);
        }
        setMaxMp(newmp);

        List<Pair<MapleStat, Integer>> statup = new ArrayList<>(2);
        statup.add(new Pair<>(MapleStat.MAXHP, newhp));
        statup.add(new Pair<>(MapleStat.MAXMP, newmp));
        getClient().getSession().write(MaplePacketCreator.updatePlayerStats(statup));
        recalcLocalStats();
        enforceMaxHpMp();
        silentPartyUpdate();
        guildUpdate();
        updateLastStrengthening();

        return deathpenalty;
    }

    public int decrementDeathPenaltyAndRecalc(int decrement) {
        if (decrement > deathpenalty) {
            decrement = deathpenalty;
        }
        deathpenalty -= decrement;

        if (deathfactor > deathpenalty) {
            deathfactor = deathpenalty;
        }

        int hppenalty, mppenalty;
        switch (getJob().getId() / 100) {
            case 0: // Beginner
                hppenalty = 95;
                mppenalty = 0;
                break;
            case 1: // Warrior
                hppenalty = 400;
                mppenalty = 60;
                break;
            case 2: // Mage
                hppenalty = 75;
                mppenalty = 400;
                break;
            case 3: // Archer
                hppenalty = 135;
                mppenalty = 70;
                break;
            case 4: // Rogue
                hppenalty = 145;
                mppenalty = 70;
                break;
            case 5: // Pirate
                hppenalty = 135;
                mppenalty = 70;
                break;
            default: // GM, or something went wrong
                hppenalty = 135;
                mppenalty = 75;
                break;
        }
        int newhp = Math.min(getMaxHp() + (decrement * hppenalty), 30000);
        setMaxHp(newhp);
        int newmp = Math.min(getMaxMp() + (decrement * mppenalty), 30000);
        setMaxMp(newmp);
        List<Pair<MapleStat, Integer>> statup = new ArrayList<>(2);
        statup.add(new Pair<>(MapleStat.MAXHP, newhp));
        statup.add(new Pair<>(MapleStat.MAXMP, newmp));
        getClient().getSession().write(MaplePacketCreator.updatePlayerStats(statup));
        recalcLocalStats();
        enforceMaxHpMp();
        silentPartyUpdate();
        guildUpdate();

        return deathpenalty;
    }

    public long getLastKillOnMap() {
        return lastkillonmap;
    }

    public void setLastKillOnMap(long lkom) {
        lastkillonmap = lkom;
    }

    public boolean lastKillOnMapWithin(int seconds) {
        return System.currentTimeMillis() - getLastKillOnMap() < ((long) seconds * 1000L);
    }

    public void updateLastKillOnMap() {
        setLastKillOnMap(System.currentTimeMillis());
    }

    public boolean getTrueDamage() {
        return truedamage;
    }

    public void setTrueDamage(boolean td) {
        truedamage = td;
    }

    public void toggleTrueDamage() {
        truedamage = !truedamage;
    }

    public boolean showPqPoints() {
        return showPqPoints;
    }

    public void setShowPqPoints(boolean spqp) {
        showPqPoints = spqp;
    }

    public void toggleShowPqPoints() {
        showPqPoints = !showPqPoints;
    }

    public boolean isScpqFlagged() {
        return scpqflag;
    }

    public void setScpqFlag(boolean sf) {
        scpqflag = sf;
        silentPartyUpdate();
    }

    public boolean getZakDc() {
        return zakDc;
    }

    public void setZakDc(boolean zdc) {
        zakDc = zdc;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public void setInvincible(boolean invincible) {
        this.invincible = invincible;
    }

    public void setBattleshipHp(int bhp) {
        this.battleshiphp = bhp;
        MapleStatEffect battleShipEffect = this.getStatForBuff(MapleBuffStat.MONSTER_RIDING);
        if (this.battleshiphp < 1 && battleShipEffect != null) {
            this.cancelBuffsBySourceId(5221006);
            ISkill battleShip = SkillFactory.getSkill(5221006);
            long cooldownTime = (long) battleShip.getEffect(getSkillLevel(battleShip)).getCooldown() * 1000L;
            this.giveCoolDowns(5221006, System.currentTimeMillis(), cooldownTime, true);
        }
    }

    public int getBattleshipHp() {
        return this.battleshiphp;
    }

    public int getBattleshipMaxHp() {
        return (getSkillLevel(5221006) * 4000) + ((getLevel() - 120) * 2000);
    }

    public int decrementBattleshipHp(int decrement) {
        this.battleshiphp -= decrement;
        MapleStatEffect battleshipeffect = this.getStatForBuff(MapleBuffStat.MONSTER_RIDING);
        if (this.battleshiphp < 1 && battleshipeffect != null) {
            this.cancelBuffsBySourceId(5221006);
            ISkill battleship = SkillFactory.getSkill(5221006);
            long cooldowntime = (long) battleship.getEffect(getSkillLevel(battleship)).getCooldown() * 1000L;
            this.giveCoolDowns(5221006, System.currentTimeMillis(), cooldowntime, true);
        }
        return this.battleshiphp;
    }

    public void setMountActivity(boolean active) {
        if (this.maplemount != null) {
            this.maplemount.setActive(active);
        }
    }

    public void setMount(MapleMount m) {
        this.maplemount = m;
    }

    public MapleMount getMount() {
        return this.maplemount;
    }

    public void setPastLifeExp(int ple) {
        this.pastLifeExp = ple;
    }

    public int getPastLifeExp() {
        return this.pastLifeExp;
    }

    public void updatePastLifeExp() {
        int pastLifeLevel = 1;
        for (List<Integer> pastLife : pastlives) {
            if (pastLife.get(0) > 1) {
                pastLifeLevel = pastLife.get(0);
                break;
            }
        }
        pastLifeExp = Math.max((pastLifeLevel / 2 + pastLifeLevel % 2 + 9) / 10, 1);
    }

    public int getExpEffectiveLevel() {
        int pastLifeLevel = 1;
        for (List<Integer> pastLife : pastlives) {
            if (pastLife.get(0) > 1) {
                pastLifeLevel = pastLife.get(0);
                break;
            }
        }
        return Math.max(pastLifeLevel / 2 + pastLifeLevel % 2 + 9, getLevel());
    }

    public void toggleGenderFilter() {
        genderFilter = !genderFilter;
    }

    public boolean genderFilter() {
        return genderFilter;
    }

    public byte getQuestSlots() {
        return questSlots;
    }

    public void setQuestSlots(byte questSlots) {
        this.questSlots = questSlots;
    }

    public List<CQuest> getCQuests() {
        return new ArrayList<>(cQuests);
    }

    public void fillOutQuestSlots() {
        if (cQuests.size() < getQuestSlots()) {
            while (cQuests.size() < getQuestSlots()) {
                CQuest newQuest = new CQuest(this);
                newQuest.loadQuest(0);
                cQuests.add(newQuest);
            }
        }
    }

    public boolean hasOpenCQuestSlot() {
        fillOutQuestSlots();
        return cQuests.stream().anyMatch(cq -> cq.getQuest().getId() == 0);
    }

    public CQuest getCQuest(byte slot) {
        if (slot > getQuestSlots() || slot < 1) return null;
        if (slot > cQuests.size()) fillOutQuestSlots();
        if (slot > cQuests.size()) return null;
        return cQuests.get(slot - 1);
    }

    public CQuest getCQuestById(final int questId) {
        return cQuests.stream().filter(cq -> cq.getQuest().getId() == questId).findAny().orElse(null);
    }

    /**
     * NOTE: Only checks pre-reqs and that the player hasn't already
     * completed the quest if the quest isn't repeatable; does not
     * check to see that the player has a slot open.
     *
     * @throws NullPointerException When {@code questId} does not
     * correspond to a quest.
     */
    public boolean canBeginCQuest(int questId) {
        return canBeginCQuest(MapleCQuests.loadQuest(questId));
    }

    /**
     * NOTE: Only checks pre-reqs and that the player hasn't already
     * completed the quest if the quest isn't repeatable; does not
     * check to see that the player has a slot open.
     *
     * @throws NullPointerException When {@code q == null}.
     */
    public boolean canBeginCQuest(MapleCQuests q) {
        return
            !(!q.isRepeatable() && completedCQuest(q.getId())) &&
            q.meetsPrereqs(this);
    }

    public boolean loadCQuest(int questId) {
        fillOutQuestSlots();
        if (questId == 0 || !canBeginCQuest(questId)) return false;
        for (CQuest cQuest : cQuests) {
            if (cQuest.getQuest().getId() == 0) {
                cQuest.loadQuest(questId);
                sendHint("#eQuest start: " + MapleCQuests.loadQuest(questId).getTitle());
                return true;
            }
        }
        return false;
    }

    public void reloadCQuests() {
        cQuests.forEach(CQuest::reload);
    }

    /**
     * @return {@code true} if an active quest was successfully forfeited,
     *         {@code false} otherwise.
     */
    public boolean forfeitCQuest(byte slot) {
        CQuest cQuest = getCQuest(slot);
        if (cQuest == null || cQuest.getQuest().getId() == 0) return false;
        cQuest.closeQuest();
        return true;
    }

    /**
     * @return {@code true} if an active quest was successfully forfeited,
     *         {@code false} otherwise.
     */
    public boolean forfeitCQuestById(int questId) {
        if (questId == 0) return false;
        CQuest cQuest = getCQuestById(questId);
        if (cQuest == null) return false;
        cQuest.closeQuest();
        return true;
    }

    public boolean canCompleteCQuest(int questId) {
        CQuest cQuest = getCQuestById(questId);
        return cQuest != null && cQuest.canComplete();
    }

    /** Forces completion. */
    public void completeCQuest(int questId) {
        getCQuestById(questId).complete();
    }

    public boolean isOnCQuest() {
        fillOutQuestSlots();
        return cQuests.stream().anyMatch(cq -> cq.getQuest().getId() != 0);
    }

    public boolean isOnCQuest(final int questId) {
        return cQuests.stream().anyMatch(cq -> cq.getQuest().getId() == questId);
    }

    public void setCQuestCompleted(int questId, final CQuestStatus completionLevel) {
        completedCQuests.compute(
            questId,
            (qid, oldCL) ->
                oldCL == null ?
                    completionLevel :
                    CQuestStatus.max(oldCL, completionLevel)
        );
    }

    public boolean completedCQuest(int questId) {
        return completedCQuests.containsKey(questId);
    }

    public Map<Integer, CQuestStatus> readCompletedCQuests() {
        return Collections.unmodifiableMap(completedCQuests);
    }

    public CQuestStatus getCQuestStatus(int questId) {
        CQuestStatus completion = completedCQuests.get(questId);
        return
            completion != null ?
                completion :
                getCQuestById(questId) != null ?
                    CQuestStatus.IN_PROGRESS :
                    CQuestStatus.NONE;
    }

    public int getQuestCollected(int itemId) {
        return getItemQuantity(itemId, false);
    }

    public void makeQuestProgress(final int mobId, final int itemId, final String objective) {
        if (mobId > 0) {
            cQuests.stream().filter(CQuest::canAdvance).forEach(cq -> cq.doQuestKill(mobId));
        }
        if (objective != null && !objective.equals("")) {
            cQuests.stream().filter(CQuest::canAdvance).forEach(cq -> cq.doObjectiveProgress(objective));
        }
        if (itemId > 0) {
            cQuests
                .stream()
                .filter(cq ->
                    cq.getQuest().requiresItem(itemId) &&
                    getItemQuantity(itemId, false) <= cq.getQuest().getNumberToCollect(itemId)
                )
                .forEach(cq ->
                    sendHint(
                        "#e" +
                            cq.getQuest().getItemName(itemId) +
                            ": " +
                            (getItemQuantity(itemId, false) >=
                                cq.getQuest().getNumberToCollect(itemId) ? "#g" : "#r") +
                            getItemQuantity(itemId, false) +
                            " #k/ " +
                            cq.getQuest().getNumberToCollect(itemId)
                    )
                );
        }
    }

    public boolean hasCompletedCQuestGoal(int questId, int mobId, int itemId, String objective) {
        CQuest q = getCQuestById(questId);
        return q != null && q.hasCompletedGoal(mobId, itemId, objective);
    }

    public void updateQuestEffectiveLevel() {
        questEffectiveLevel =
            cQuests
                .stream()
                .filter(cq -> cq.getEffectivePlayerLevel() > 0)
                .mapToInt(CQuest::getEffectivePlayerLevel)
                .min()
                .orElse(0);
    }

    public void resetQuestEffectiveLevel() {
        questEffectiveLevel = 0;
    }

    public void setQuestEffectiveLevel(int level) {
        questEffectiveLevel = level;
    }

    public int getQuestEffectiveLevel() {
        return questEffectiveLevel;
    }

    public double getQuestEffectiveLevelDmgMulti() {
        if (getQuestEffectiveLevel() > 0 && getQuestEffectiveLevel() < getLevel()) {
            return Math.max(
                (100.0d - Math.sqrt((getLevel() - getQuestEffectiveLevel()) * Math.pow(getLevel(), 1.5d) / 64.0d)) / 100.0d,
                0.01d
            );
        }
        return 1.0d;
    }

    public Set<Integer> getFeats() {
        return feats;
    }

    public void addFeat(int featId) {
        feats.add(featId);
    }

    public boolean hasFeat(int featId) {
        return feats.contains(featId);
    }

    public void setPreEventMap(int pem) {
        preEventMap = pem;
    }

    public int getPreEventMap() {
        return preEventMap;
    }

    public void sendHint(String ms) {
        sendHint(ms, 275, 10);
    }

    public void sendHint(String msg, int x, int y) {
        getClient().getSession().write(MaplePacketCreator.sendHint(msg, x, y));
        getClient().getSession().write(MaplePacketCreator.enableActions());
    }

    public static String makeNumberReadable(int nr) {
        StringBuilder sb = new StringBuilder();
        String readable;
        String num = "" + nr;
        String first_num = "";
        String left_num = "";
        int repeat = 0;
        if (num.length() > 3) {
            first_num += num;
            while (first_num.length() > 3) {
                first_num = first_num.substring(0, first_num.length() - 3);
                repeat++;
            }
            sb.append(first_num).append(',');
            left_num += num.substring(first_num.length(), num.length());
            for (int x = 0; x < repeat; ++x) {
                sb.append(left_num.substring(3 * x, 3 * x + 3)).append(',');
            }
            readable = sb.toString().substring(0, sb.toString().length() - 1);
        } else {
            readable = num;
        }
        return readable;
    }

    public Date getLastDailyPrize() {
        return lastdailyprize;
    }

    public void setLastDailyPrize(Date ldp) {
        lastdailyprize = ldp;
    }

    public int getVotePoints() {
        return votepoints;
    }

    public void setVotePoints(int vp) {
        votepoints = vp;
    }

    public void voteUpdate() {
        Connection con = DatabaseConnection.getConnection();
        ResultSet rs = null;
        try (PreparedStatement ps = con.prepareStatement("SELECT paypalNX,votepoints FROM accounts WHERE id = ?")) {
            ps.setInt(1, getClient().getAccID());
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                dropMessage("Could not locate your NX/vote point totals!");
                return;
            }
            int dbNx = rs.getInt("paypalNX");
            int dbVotePoints = rs.getInt("votepoints");
            rs.close();
            modifyCSPoints(1, dbNx - initialNx);
            setVotePoints(getVotePoints() + (dbVotePoints - initialVotePoints));
            setInitialNx(dbNx);
            setInitialVotePoints(dbVotePoints);
            dropMessage(
                "Your vote points and NX have been updated. New vote point total: " +
                    getVotePoints() +
                    ", NX: " +
                    getCSPoints(1)
            );
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            dropMessage("There was an error retrieving your NX/vote point totals!");
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }
        }
    }

    public int getInitialVotePoints() {
        return initialVotePoints;
    }

    public void setInitialVotePoints(int ivp) {
        initialVotePoints = ivp;
    }

    public int getInitialNx() {
        return initialNx;
    }

    public void setInitialNx(int inx) {
        initialNx = inx;
    }

    public void dropVoteTime() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT date FROM bit_votingrecords WHERE account = ?");
            ps.setString(1, getClient().getAccountName());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                dropMessage("Could not locate your voting record. Remember to vote!");
                return;
            }
            long date = rs.getLong("date");
            date *= 1000;
            rs.close();
            ps.close();
            long timeleft = (24 * 60 * 60 * 1000) - (System.currentTimeMillis() - date);
            if (timeleft < 1) {
                dropMessage("You may vote right now! Remember to use @voteupdate if you want to spend your new vote point/NX without logging out!");
            } else {
                int hours = (int) ((timeleft - (timeleft % (1000 * 60 * 60))) / (1000 * 60 * 60));
                int remainder = (int) (timeleft - (hours * (1000 * 60 * 60)));
                int minutes = (remainder - (remainder % (1000 * 60))) / (1000 * 60);
                remainder -= minutes * (1000 * 60);
                int seconds = remainder / (1000);
                dropMessage("You may vote again in " + hours + " hours, " + minutes + " minutes, and " + seconds + " seconds.");
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            dropMessage("Error retrieving voting records!");
        }
    }

    public int getMorph() {
        List<MapleBuffStatValueHolder> allBuffs = new ArrayList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMorph() && mbsvh.effect.getSourceId() != 5111005 && mbsvh.effect.getSourceId() != 5121003) {
                return mbsvh.effect.getSourceId();
            }
        }
        return 0;
    }

    public void setReadingTime(int rt) {
        this.readingTime = rt;
    }

    public int getReadingTime() {
        return this.readingTime;
    }
    //

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    private static void sqlException(SQLException sqle) {
        System.err.println("SQL Error: " + sqle);
    }

    private static void sqlException(RemoteException re) {
        System.err.println("SQL Error: " + re);
    }

    public MapleQuestStatus getQuest(int questId) {
        return getQuest(MapleQuest.getInstance(questId));
    }

    public MapleQuestStatus getQuest(MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
        }
        return quests.get(quest);
    }

    public void updateQuest(MapleQuestStatus quest) {
        quests.put(quest.getQuest(), quest);
        if (!(quest.getQuest() instanceof MapleCustomQuest)) {
            if (quest.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
                client.getSession().write(MaplePacketCreator.startQuest((short) quest.getQuest().getId()));
                client.getSession().write(MaplePacketCreator.updateQuestInfo((short) quest.getQuest().getId(), quest.getNpc(), (byte) 8));
            } else if (quest.getStatus().equals(MapleQuestStatus.Status.COMPLETED)) {
                client.getSession().write(MaplePacketCreator.completeQuest((short) quest.getQuest().getId()));
            } else if (quest.getStatus().equals(MapleQuestStatus.Status.NOT_STARTED)) {
                client.getSession().write(MaplePacketCreator.forfeitQuest((short) quest.getQuest().getId()));
            }
        }
    }

    public static int getIdByName(String name, int world) {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        try {
            ps = con.prepareStatement("SELECT id FROM characters WHERE name = ? AND world = ?");
            ps.setString(1, name);
            ps.setInt(2, world);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            int id = rs.getInt("id");
            rs.close();
            ps.close();
            return id;
        } catch (SQLException e) {
            sqlException(e);
        }
        return -1;
    }

    public static String getNameById(int id, int world) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ? AND world = ?");
            ps.setInt(1, id);
            ps.setInt(2, world);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            String name = rs.getString("name");
            rs.close();
            ps.close();
            return name;
        } catch (SQLException e) {
            sqlException(e);
        }
        return null;
    }

    /** Nullable */
    public Integer getBuffedValue(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.value;
    }

    public boolean isBuffFrom(MapleBuffStat stat, ISkill skill) {
        MapleBuffStatValueHolder mbsvh = effects.get(stat);
        return mbsvh != null && mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
    }

    public int getBuffSource(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null) {
            return -1;
        }
        return mbsvh.effect.getSourceId();
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
        MapleInventory iv = inventory[type.ordinal()];
        int possesed = iv.countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    private void setBuffedValue(MapleBuffStat effect, int value) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.value = value;
    }

    private Long getBuffedStartTime(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.startTime;
    }

    public Long getBuffedRemainingTime(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.schedule.getDelay(TimeUnit.MILLISECONDS);
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect;
    }

    private void prepareDragonBlood(final MapleStatEffect bloodEffect) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        dragonBloodSchedule = TimerManager.getInstance().register(() -> {
            addHP(-bloodEffect.getX());
            getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
            getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBuffeffect(getId(), bloodEffect.getSourceId(), 5, (byte) 3), false);
        }, 4000, 4000);
    }

    public void startFullnessSchedule(final int decrease, final MaplePet pet, int petSlot) {
        ScheduledFuture<?> schedule = TimerManager.getInstance().register(() -> {
            if (pet != null) {
                int newFullness = pet.getFullness() - decrease;
                if (newFullness <= 5) {
                    pet.setFullness(15);
                    unequipPet(pet, true, true);
                } else {
                    pet.setFullness(newFullness);
                    getClient().getSession().write(MaplePacketCreator.updatePet(pet, true));
                }
            }
        }, 60L * 1000L, 60L * 1000L);
        switch (petSlot) {
            case 0:
                fullnessSchedule = schedule;
                break;
            case 1:
                fullnessSchedule_1 = schedule;
                break;
            case 2:
                fullnessSchedule_2 = schedule;
                break;
            default:
                break;
        }
    }

    private void cancelFullnessSchedule(int petSlot) {
        switch (petSlot) {
            case 0:
                if (fullnessSchedule != null) fullnessSchedule.cancel(false);
            case 1:
                if (fullnessSchedule_1 != null) fullnessSchedule_1.cancel(false);
            case 2:
                if (fullnessSchedule_2 != null) fullnessSchedule_2.cancel(false);
            default:
                break;
        }
    }

    public void startMapTimeLimitTask(final MapleMap from, final MapleMap to) {
        if (to.getTimeLimit() > 0 && from != null) {
            final MapleCharacter chr = this;
            mapTimeLimitTask = TimerManager.getInstance().register(() -> {
                MaplePortal pfrom;
                if (from.isMiniDungeonMap()) {
                    pfrom = from.getPortal("MD00");
                } else {
                    pfrom = from.getPortal(0);
                }
                if (pfrom != null) {
                    chr.changeMap(from, pfrom);
                }
            }, from.getTimeLimit() * 1000, from.getTimeLimit() * 1000);
        }
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
        }
    }

    public void registerEffect(final MapleStatEffect effect, long startTime, ScheduledFuture<?> schedule) {
        if (effect.isHide() && isGM()) {
            this.hidden = true;
            getMap().broadcastNONGMMessage(this, MaplePacketCreator.removePlayerFromMap(getId()), false);
        } else if (effect.isDragonBlood()) {
            prepareDragonBlood(effect);
        } else if (effect.isBerserk()) {
            checkBerserk();
        } else if (effect.isBeholder()) {
            prepareBeholderEffect();
        } else if (effect.isMagicGuard()) {
            registerMagicGuard();
        }
        for (int i = 0; i < effect.getStatups().size(); ++i) {
            Pair<MapleBuffStat, Integer> statup = effect.getStatups().get(i);
            effects.put(statup.getLeft(), new MapleBuffStatValueHolder(effect, startTime, schedule, statup.getRight()));
        }
        recalcLocalStats();
    }

    public void registerStatups(final MapleStatEffect effect, final List<Pair<MapleBuffStat, Integer>> statups, long startTime, ScheduledFuture<?> schedule) {
        for (int i = 0; i < statups.size(); ++i) {
            Pair<MapleBuffStat, Integer> statup = statups.get(i);
            effects.put(statup.getLeft(), new MapleBuffStatValueHolder(effect, startTime, schedule, statup.getRight()));
        }
        if (!statups.isEmpty()) {
            recalcLocalStats();
        }
    }

    private List<MapleBuffStat> getBuffStats(MapleStatEffect effect, long startTime) {
        List<MapleBuffStat> stats = new ArrayList<>(5);
        for (Map.Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : effects.entrySet()) {
            MapleBuffStatValueHolder mbsvh = stateffect.getValue();
            if (mbsvh.effect.sameSource(effect) && (startTime == -1 || startTime == mbsvh.startTime)) {
                stats.add(stateffect.getKey());
            }
        }
        return stats;
    }

    private void deregisterBuffStats(List<MapleBuffStat> stats) {
        List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<>(stats.size());
        for (MapleBuffStat stat : stats) {
            MapleBuffStatValueHolder mbsvh = effects.get(stat);
            if (mbsvh != null) {
                if (mbsvh.effect.getSourceId() == 2001002) {
                    this.setMagicGuard(false);
                    this.cancelMagicGuardCancelTask();
                }
                effects.remove(stat);
                boolean addMbsvh = true;
                for (MapleBuffStatValueHolder contained : effectsToCancel) {
                    if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
                        // Prevents duplicate mbsvh's
                        addMbsvh = false;
                    }
                }
                if (addMbsvh) {
                    effectsToCancel.add(mbsvh);
                }
                if (stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.PUPPET) {
                    int summonId = mbsvh.effect.getSourceId();
                    MapleSummon summon = summons.get(summonId);
                    if (summon != null) {
                        getMap().broadcastMessage(MaplePacketCreator.removeSpecialMapObject(summon, true));
                        getMap().removeMapObject(summon);
                        removeVisibleMapObject(summon);
                        summons.remove(summonId);
                        if (summon.getSkill() == 1321007) {
                            if (beholderHealingSchedule != null) {
                                beholderHealingSchedule.cancel(false);
                                beholderHealingSchedule = null;
                            }
                            if (beholderBuffSchedule != null) {
                                beholderBuffSchedule.cancel(false);
                                beholderBuffSchedule = null;
                            }
                        }
                    }
                } else if (stat == MapleBuffStat.DRAGONBLOOD) {
                    dragonBloodSchedule.cancel(false);
                    dragonBloodSchedule = null;
                }
            }
        }
        for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
            if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).isEmpty()) {
                cancelEffectCancelTasks.schedule.cancel(false);
            }
        }
    }

    public void cancelEffect(MapleStatEffect effect, boolean overwrite, long startTime) {
        List<MapleBuffStat> buffstats;
        if (!overwrite) {
            buffstats = getBuffStats(effect, startTime);
        } else {
            List<Pair<MapleBuffStat, Integer>> statups = effect.getStatups();
            buffstats = new ArrayList<>(statups.size());
            for (Pair<MapleBuffStat, Integer> statup : statups) {
                buffstats.add(statup.getLeft());
            }
        }
        deregisterBuffStats(buffstats);
        if (effect.isMagicDoor()) {
            if (!getDoors().isEmpty()) {
                MapleDoor door = getDoors().iterator().next();
                for (MapleCharacter chr : door.getTarget().getCharacters()) {
                    door.sendDestroyData(chr.getClient());
                }
                for (MapleCharacter chr : door.getTown().getCharacters()) {
                    door.sendDestroyData(chr.getClient());
                }
                for (MapleDoor destroyDoor : getDoors()) {
                    door.getTarget().removeMapObject(destroyDoor);
                    door.getTown().removeMapObject(destroyDoor);
                }
                clearDoors();
                silentPartyUpdate();
            }
        }
        if (effect.isMonsterRiding()) {
            //if (effect.getSourceId() != 5221006) {
                this.getMount().cancelSchedule();
                this.getMount().setActive(false);
            //}
        }
        if (effect.isMagicGuard()) {
            setMagicGuard(false);
            this.cancelMagicGuardCancelTask();
        }
        if (!overwrite) {
            cancelPlayerBuffs(buffstats);
            if (effect.isHide() && getMap().getMapObject(getObjectId()) != null) {
                this.hidden = false;
                getMap().broadcastNONGMMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);
                setOffOnline(true);
                for (int i = 0; i < 3; ++i) {
                    if (pets[i] != null) {
                        getMap().broadcastNONGMMessage(
                            this,
                            MaplePacketCreator.showPet(
                                this,
                                pets[i],
                                false,
                                false
                            ),
                            false
                        );
                    } else {
                        break;
                    }
                }
            }
        }
    }

    public void cancelBuffStats(MapleBuffStat stat) {
        List<MapleBuffStat> buffStatList = Collections.singletonList(stat);
        deregisterBuffStats(buffStatList);
        cancelPlayerBuffs(buffStatList);
    }

    public void cancelDarkSight() {
        MapleStatEffect dseffect;
        int darkSightSkillLevel = getSkillLevel(SkillFactory.getSkill(4001003));
        dseffect = SkillFactory.getSkill(1120003).getEffect(darkSightSkillLevel);

        cancelEffect(dseffect, false, -1);
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        cancelEffect(effects.get(stat).effect, false, -1);
    }

    private void cancelPlayerBuffs(List<MapleBuffStat> buffstats) {
        if (getClient().getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
            for (MapleBuffStat mbs : buffstats) {
                if (mbs == MapleBuffStat.WDEF) {
                    setMagicGuard(false);
                    cancelMagicGuardCancelTask();
                }
            }
            recalcLocalStats();
            enforceMaxHpMp();
            getClient().getSession().write(MaplePacketCreator.cancelBuff(buffstats));
            getMap().broadcastMessage(this, MaplePacketCreator.cancelForeignBuff(getId(), buffstats), false);
        }
    }

    public void dispel() {
        List<MapleBuffStatValueHolder> allBuffs = new ArrayList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSourceId() == 2001002) continue;
            if (mbsvh.effect.isSkill()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
        setMagicGuard(false);
        cancelMagicGuardCancelTask();
    }

    public void cancelAllBuffs() {
        List<MapleBuffStatValueHolder> allBuffs = new ArrayList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
        }
    }

    private void cancelBuffsBySourceId(int sourceid) {
        List<MapleBuffStatValueHolder> allBuffs = new ArrayList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (sourceid == mbsvh.effect.getSourceId()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public boolean isAffectedBySourceId(final int sourceId) {
        return effects.values().stream().anyMatch(mbsvh -> mbsvh.effect.getSourceId() == sourceId);
    }

    public void cancelMorphs() {
        List<MapleBuffStatValueHolder> allBuffs = new ArrayList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (
                mbsvh.effect.isMorph() &&
                mbsvh.effect.getSourceId() != 5111005 &&
                mbsvh.effect.getSourceId() != 5121003
            ) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        for (PlayerBuffValueHolder mbsvh : buffs) {
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime);
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        List<PlayerBuffValueHolder> ret = new ArrayList<>();
        for (MapleBuffStatValueHolder mbsvh : effects.values()) {
            ret.add(new PlayerBuffValueHolder(mbsvh.startTime, mbsvh.effect));
        }
        return ret;
    }

    public void cancelMagicDoor() {
        List<MapleBuffStatValueHolder> allBuffs = new ArrayList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void handleOrbgain() {
        MapleStatEffect ceffect;
        int advComboSkillLevel = getSkillLevel(SkillFactory.getSkill(1120003));
        if (advComboSkillLevel > 0) {
            ceffect = SkillFactory.getSkill(1120003).getEffect(advComboSkillLevel);
        } else {
            ceffect = SkillFactory.getSkill(1111002).getEffect(getSkillLevel(SkillFactory.getSkill(1111002)));
        }
        if (getBuffedValue(MapleBuffStat.COMBO) < ceffect.getX() + 1) {
            int neworbcount = getBuffedValue(MapleBuffStat.COMBO) + 1;
            if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
                if (neworbcount < ceffect.getX() + 1) {
                    neworbcount++;
                }
            }
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.COMBO, neworbcount));
            setBuffedValue(MapleBuffStat.COMBO, neworbcount);
            int duration = ceffect.getDuration();
            duration += (int) ((getBuffedStartTime(MapleBuffStat.COMBO) - System.currentTimeMillis()));
            getClient().getSession().write(MaplePacketCreator.giveBuff(1111002, duration, stat));
            getMap().broadcastMessage(this, MaplePacketCreator.giveForeignBuff(getId(), stat, ceffect), false);
        }
    }

    public void handleOrbconsume() {
        ISkill combo = SkillFactory.getSkill(1111002);
        MapleStatEffect ceffect = combo.getEffect(getSkillLevel(combo));
        List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.COMBO, 1));
        setBuffedValue(MapleBuffStat.COMBO, 1);
        int duration = ceffect.getDuration();
        duration += (int) ((getBuffedStartTime(MapleBuffStat.COMBO) - System.currentTimeMillis()));
        getClient().getSession().write(MaplePacketCreator.giveBuff(1111002, duration, stat));
        getMap().broadcastMessage(this, MaplePacketCreator.giveForeignBuff(getId(), stat, ceffect), false);
    }

    private void silentEnforceMaxHpMp() {
        setMp(getMp());
        setHp(getHp(), true);
    }

    private void enforceMaxHpMp() {
        List<Pair<MapleStat, Integer>> stats = new ArrayList<>(2);
        if (getMp() > getCurrentMaxMp()) {
            setMp(getMp());
            stats.add(new Pair<>(MapleStat.MP, getMp()));
        }
        if (getHp() > getCurrentMaxHp()) {
            setHp(getHp());
            stats.add(new Pair<>(MapleStat.HP, getHp()));
        }
        if (!stats.isEmpty()) {
            getClient().getSession().write(MaplePacketCreator.updatePlayerStats(stats));
        }
    }

    public boolean isBareHanded() {
        IItem weapon = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        return weapon != null &&
               weapon.getItemId() == 1482999 &&
               getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) == null;
    }

    public boolean isUnarmed() {
        return getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11) == null &&
               getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) == null;
    }

    public boolean showSnipeDmg() {
        return showSnipeDmg;
    }

    public void toggleShowSnipeDmg() {
        showSnipeDmg = !showSnipeDmg;
    }

    public MapleMap getMap() {
        return map;
    }

    public void setMap(MapleMap newMap) {
        map = newMap;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public int getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    public int getAPQScore() {
        return APQScore;
    }

    public int getFame() {
        return fame;
    }

    public int getCP() {
        return CP;
    }

    public int getTeam() {
        return team;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void setCP(int cp) {
        CP = cp;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public void setTotalCP(int totalcp) {
        totalCP = totalcp;
    }

    public void gainCP(int gain) {
        setCP(getCP() + gain);
        if (getCP() > getTotalCP()) {
            setTotalCP(getCP());
        }
        getClient().getSession().write(MaplePacketCreator.CPUpdate(false, getCP(), getTotalCP(), getTeam()));
        if (getParty() != null && getParty().getTeam() != -1) {
            getMap()
                .broadcastMessage(
                    MaplePacketCreator.CPUpdate(
                        true,
                        getParty().getCP(),
                        getParty().getTotalCP(),
                        getParty().getTeam()
                    )
                );
        }
    }

    public int getStr() {
        return str;
    }

    public int getDex() {
        return dex;
    }

    public int getLuk() {
        return luk;
    }

    public int getInt() {
        return int_;
    }

    public MapleClient getClient() {
        return client;
    }

    public int getExp() {
        return exp.get();
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxhp;
    }

    public int getMp() {
        return mp;
    }

    public int getMaxMp() {
        return maxmp;
    }

    public int getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp;
    }

    public int getMpApUsed() {
        return mpApUsed;
    }

    public void setMpApUsed(int mpApUsed) {
        this.mpApUsed = mpApUsed;
    }

    public int getHpApUsed() {
        return hpApUsed;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHpApUsed(int hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    public MapleSkinColor getSkinColor() {
        return skinColor;
    }

    public MapleJob getJob() {
        return job;
    }

    public int getGender() {
        return gender;
    }

    public int getHair() {
        return hair;
    }

    public int getFace() {
        return face;
    }

    public void setName(String name, boolean changeName) {
        if (!changeName) {
            this.name = name;
        } else {
            Connection con = DatabaseConnection.getConnection();
            try {
                con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                con.setAutoCommit(false);
                PreparedStatement sn =
                    con.prepareStatement(
                        "UPDATE characters SET name = ? WHERE id = ?"
                    );
                sn.setString(1, name);
                sn.setInt(2, id);
                sn.execute();
                con.commit();
                sn.close();
                final ChannelServer cserv = getClient().getChannelServer();
                cserv.removePlayer(this);
                this.name = name;
                cserv.addPlayer(this);
            } catch (SQLException e) {
                sqlException(e);
            }
        }
    }

    public void setStr(int str) {
        this.str = str;
        recalcLocalStats();
    }

    public void setDex(int dex) {
        this.dex = dex;
        recalcLocalStats();
    }

    public void setLuk(int luk) {
        this.luk = luk;
        recalcLocalStats();
    }

    public void setInt(int int_) {
        this.int_ = int_;
        recalcLocalStats();
    }

    public void setMaxHp(int hp) {
        this.maxhp = hp;
        recalcLocalStats();
    }

    public void setMaxMp(int mp) {
        this.maxmp = mp;
        recalcLocalStats();
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setAPQScore(int score) {
        this.APQScore = score;
    }

    public void setRemainingAp(int remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp = remainingSp;
    }

    public void setSkinColor(MapleSkinColor skinColor) {
        this.skinColor = skinColor;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public void setGM(int gmlevel) {
        this.gmLevel = gmlevel;
    }

    public CheatTracker getCheatTracker() {
        return anticheat;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public void addFame(int famechange) {
        this.fame += famechange;
    }

    public void changeMap(int map) {
        changeMap(map, 0);
    }

    public void changeMap(int map, int portal) {
        MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, warpMap.getPortal(portal));
    }

    public void changeMap(int map, String portal) {
        MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, warpMap.getPortal(portal));
    }

    public void changeMap(int map, MaplePortal portal) {
        MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, portal);
    }

    public void changeMap(final MapleMap to, final Point pos) {
        MaplePacket warpPacket = MaplePacketCreator.getWarpToMap(to, 0x80, this);
        changeMapInternal(to, pos, warpPacket);
    }

    public void changeMap(final MapleMap to, final MaplePortal pto) {
        if (to.getId() == 100000200 || to.getId() == 211000100 || to.getId() == 220000300) {
            MaplePacket warpPacket = MaplePacketCreator.getWarpToMap(to, pto.getId() - 2, this);
            changeMapInternal(to, pto.getPosition(), warpPacket);
        } else {
            MaplePacket warpPacket = MaplePacketCreator.getWarpToMap(to, pto.getId(), this);
            changeMapInternal(to, pto.getPosition(), warpPacket);
        }
    }

    private void changeMapInternal(final MapleMap to, final Point pos, MaplePacket warpPacket) {
        if (getCheatTracker().Spam(2000, 5)) {
            client.getSession().write(MaplePacketCreator.enableActions());
        } else {
            if (getPartyQuest() != null && !to.isPQMap()) {
                getPartyQuest().playerDisconnected(this);
            }
            warpPacket.setOnSend(() -> {
                IPlayerInteractionManager interaction1 = MapleCharacter.this.getInteraction();
                if (interaction1 != null) {
                    if (interaction1.isOwner(MapleCharacter.this)) {
                        if (interaction1.getShopType() == 2) {
                            interaction1.removeAllVisitors(3, 1);
                            interaction1.closeShop(((MaplePlayerShop) interaction1).returnItems(getClient()));
                        } else if (interaction1.getShopType() == 1) {
                            getClient().getSession().write(MaplePacketCreator.shopVisitorLeave(0));
                            if (interaction1.getItems().isEmpty()) {
                                interaction1.removeAllVisitors(3, 1);
                                interaction1.closeShop(((HiredMerchant) interaction1).returnItems(getClient()));
                            }
                        } else if (interaction1.getShopType() == 3 || interaction1.getShopType() == 4) {
                            interaction1.removeAllVisitors(3, 1);
                        }
                    } else {
                        interaction1.removeVisitor(MapleCharacter.this);
                    }
                }
                MapleCharacter.this.setInteraction(null);
                map.removePlayer(MapleCharacter.this);
                if (getClient().getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
                    map = to;
                    setPosition(pos);
                    to.addPlayer(MapleCharacter.this);
                    if (party != null) {
                        silentPartyUpdate();
                        getClient().getSession().write(MaplePacketCreator.updateParty(getClient().getChannel(), party, PartyOperation.SILENT_UPDATE, null));
                        updatePartyMemberHP();
                    }
                    if (getMap().getHPDec() > 0 && !inCS() && isAlive()) {
                        hpDecreaseTask = TimerManager.getInstance().schedule(this::doHurtHp, 10000);
                    }
                    if (to.getId() == 980000301) {
                        setTeam(MapleCharacter.rand(0, 1));
                        getClient().getSession().write(MaplePacketCreator.startMonsterCarnival(getTeam()));
                    }
                }
            });
            if (hasFakeChar()) {
                for (FakeCharacter ch : getFakeChars()) {
                    if (ch.follow()) {
                        ch.getFakeChar().getMap().removePlayer(ch.getFakeChar());
                    }
                }
            }
            getClient().getSession().write(warpPacket);
        }
    }

    public void leaveMap() {
        controlled.clear();
        //visibleMapObjects.clear();
        synchronized (visibleMapObjects) {
            Iterator<MapleMapObject> moiter = visibleMapObjects.iterator();
            while (moiter.hasNext()) {
                moiter.next();
                moiter.remove();
            }
        }
        if (chair != 0) {
            chair = 0;
        }
        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(false);
        }
    }

    private void doHurtHp() {
        if (getInventory(MapleInventoryType.EQUIPPED).findById(getMap().getHPDecProtect()) != null) {
            return;
        }
        addHP(-getMap().getHPDec());
        hpDecreaseTask = TimerManager.getInstance().schedule(this::doHurtHp, 10L * 1000L);
    }

    private void setDefaultCoreSkillMasterLevels() {
        int[] skillList;
        switch (getJob()) {
            case HERO:
                skillList = new int[] {1120003, 1120004, 1120005, 1121000, 1121001, 1121002, 1121006, 1121008, 1121010, 1121011};
                break;
            case PALADIN:
                skillList = new int[] {1220005, 1220006, 1220010, 1221000, 1221001, 1221002, 1221003, 1221004, 1221007, 1221009, 1221011, 1221012};
                break;
            case DARKKNIGHT:
                skillList = new int[] {1320005, 1320006, 1320008, 1320009, 1321000, 1321001, 1321002, 1321003, 1321007, 1321010};
                break;
            case FP_ARCHMAGE:
                skillList = new int[] {2121000, 2121001, 2121002, 2121003, 2121004, 2121005, 2121006, 2121007, 2121008};
                break;
            case IL_ARCHMAGE:
                skillList = new int[] {2221000, 2221001, 2221002, 2221003, 2221004, 2221005, 2221006, 2221007, 2221008};
                break;
            case BISHOP:
                // Does not include Resurrection or Genesis
                skillList = new int[] {2321000, 2321001, 2321002, 2321003, 2321004, 2321005, 2321007, 2321009};
                break;
            case BOWMASTER:
                skillList = new int[] {3120005, 3121000, 3121002, 3121003, 3121004, 3121006, 3121007, 3121008, 3121009};
                break;
            case CROSSBOWMASTER:
                skillList = new int[] {3220004, 3221000, 3221001, 3221002, 3221003, 3221005, 3221006, 3221007, 3221008};
                break;
            case NIGHTLORD:
                skillList = new int[] {4120002, 4120005, 4121000, 4121003, 4121004, 4121006, 4121007, 4121008, 4121009};
                break;
            case SHADOWER:
                skillList = new int[] {4220002, 4220005, 4221000, 4221001, 4221003, 4221004, 4221006, 4221007, 4221008};
                break;
            case BUCCANEER:
                skillList = new int[] {5121000, 5121001, 5121002, 5121003, 5121004, 5121005, 5121007, 5121008, 5121009, 5121010};
                break;
            case CORSAIR:
                skillList = new int[] {5220001, 5220002, 5220011, 5221000, 5221003, 5221004, 5221006, 5221007, 5221008, 5221009, 5221010};
                break;
            default:
                return;
        }
        for (int skillId : skillList) {
            ISkill theSkill = SkillFactory.getSkill(skillId);
            changeSkillLevel(theSkill, 0, Math.min(10, theSkill.getMaxLevel()));
        }
    }

    public void setMasterLevel(int skillid, int masterlevel) {
        ISkill s = SkillFactory.getSkill(skillid);
        int currentlevel = getSkillLevel(s);
        changeSkillLevel(SkillFactory.getSkill(skillid), currentlevel, masterlevel);
    }

    public void changeJob(MapleJob newJob, boolean announcement) {
        if (newJob == null) return;
        job = newJob;
        remainingSp++;
        if (newJob.getId() % 10 == 2) {
            remainingSp += 2;
        }
        updateSingleStat(MapleStat.AVAILABLESP, remainingSp);
        updateSingleStat(MapleStat.JOB, newJob.getId());
        if (job.getId() == 100) {
            maxhp += rand(200, 250);
        } else if (job.getId() == 200) {
            maxmp += rand(100, 150);
        } else if (job.getId() % 100 == 0) {
            maxhp += rand(100, 150);
            maxhp += rand(25, 50);
        } else if (job.getId() > 0 && job.getId() < 200) {
            maxhp += rand(300, 350);
        } else if (job.getId() < 300) {
            maxmp += rand(450, 500);
        } else if (job.getId() > 0) {
            maxhp += rand(300, 350);
            maxmp += rand(150, 200);
        }
        if (maxhp >= 30000) {
            maxhp = 30000;
        }
        if (maxmp >= 30000) {
            maxmp = 30000;
        }
        setHp(maxhp);
        setMp(maxmp);
        List<Pair<MapleStat, Integer>> statup = new ArrayList<>(2);
        statup.add(new Pair<>(MapleStat.MAXHP, maxhp));
        statup.add(new Pair<>(MapleStat.MAXMP, maxmp));
        if (getJob().getId() % 10 == 2) {
            setDefaultCoreSkillMasterLevels();
        }
        recalcLocalStats();
        getClient().getSession().write(MaplePacketCreator.updatePlayerStats(statup));
        silentPartyUpdate();
        guildUpdate();
        getMap().broadcastMessage(this, MaplePacketCreator.showJobChange(getId()), false);
        if (announcement) {
            String jobachieved = MapleJob.getJobName(job.getId());
            MaplePacket packet =
                MaplePacketCreator.serverNotice(
                    6,
                    "Congratulations to " +
                        getName() +
                        " on becoming a " +
                        jobachieved +
                        "!"
                );
            try {
                getClient()
                    .getChannelServer()
                    .getWorldInterface()
                    .broadcastMessage(
                        getName(),
                        packet.getBytes()
                    );
            } catch (RemoteException re) {
                getClient().getChannelServer().reconnectWorld();
            }
        }
    }

    public void gainAp(int ap) {
        remainingAp += ap;
        updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
    }

    public void changeSkillLevel(int skillId, int newLevel, int newMasterLevel) {
        changeSkillLevel(SkillFactory.getSkill(skillId), newLevel, newMasterLevel);
    }

    public void changeSkillLevel(ISkill skill, int newLevel, int newMasterlevel) {
        skills.put(skill, new SkillEntry(newLevel, newMasterlevel));
        getClient().getSession().write(MaplePacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel));
    }

    public void setHp(int newHp) {
        setHp(newHp, false);
    }

    public void setHp(int newHp, boolean silent) {
        int oldHp = hp;
        if (newHp < 0) {
            newHp = 0;
        } else if (newHp > localMaxHp) {
            newHp = localMaxHp;
        }
        hp = newHp;

        if (!silent) {
            updatePartyMemberHP();
        }
        if (oldHp > hp && !isAlive()) {
            playerDead();
        }
        checkBerserk();
    }

    public void addAP(MapleClient c, int stat, int amount) {
        MapleCharacter player = c.getPlayer();
        switch (stat) {
            case 1: // STR
                player.setStr(player.getStr() + amount);
                player.updateSingleStat(MapleStat.STR, player.getStr());
                break;
            case 2: // DEX
                player.setDex(player.getDex() + amount);
                player.updateSingleStat(MapleStat.DEX, player.getDex());
                break;
            case 3: // INT
                player.setInt(player.getInt() + amount);
                player.updateSingleStat(MapleStat.INT, player.getInt());
                break;
            case 4: // LUK
                player.setLuk(player.getLuk() + amount);
                player.updateSingleStat(MapleStat.LUK, player.getLuk());
                break;
            case 5: // HP
                player.setMaxHp(amount);
                player.updateSingleStat(MapleStat.MAXHP, player.getMaxHp());
                break;
            case 6: // MP
                player.setMaxMp(amount);
                player.updateSingleStat(MapleStat.MAXMP, player.getMaxMp());
                break;
        }
        if (!player.isGM()) {
            player.setRemainingAp(player.getRemainingAp() - amount);
        }
        player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
    }

    public void setLevelAchieved(int level) {
        levelAchieved = level;
    }

    public void setJobAchieved(MapleJob job) {
        jobAchieved = job;
    }

    public int getLevelAchieved() {
        return levelAchieved;
    }

    public MapleJob getJobAchieved() {
        return jobAchieved;
    }

    public synchronized void permadeath() {
        final MapleMapObject lds = getLastDamageSource();
        final int deathMap = getMap().getId();
        final int monsterTrialPoints = getMonsterTrialPoints();
        changeMap(100);
        setMap(100);
        cancelAllBuffs();
        cancelAllDebuffs();
        unequipEverything();
        setLevelAchieved(getLevel());
        setJobAchieved(getJob());
        setDeathPenalty(0);
        setDeathFactor(0);
        setLevel(1);
        setHp(1);
        setMp(1);
        setMaxHp(50);
        setMaxMp(5);
        setExp(0);
        resetQuestEffectiveLevel();
        updateSingleStat(MapleStat.EXP, 0);
        final StringBuilder questProgress = new StringBuilder();
        for (Map.Entry<Integer, CQuestStatus> completed : completedCQuests.entrySet()) {
            questProgress
                .append("    ")
                .append(completed.getKey())
                .append(" : ")
                .append(completed.getValue().getValue())
                .append('\n');
        }
        completedCQuests.clear();
        cQuests.clear();
        CQuest newQuest = new CQuest(this);
        newQuest.loadQuest(0);
        cQuests.add(newQuest);
        setQuestSlots((byte) 1);
        feats.clear();
        setScpqFlag(false);
        overflowExp = 0L;
        lastSamsara = 0L;
        setMonsterTrialPoints(0);
        setMonsterTrialTier(0);
        setLastTrialTime(0L);
        resetAllQuestProgress();
        final StringBuilder fourthJobSkills = new StringBuilder();
        if (getJob().getId() % 10 == 2) { // Fourth job
            for (int skillId : MapleCharacter.SKILL_IDS) {
                if (
                    skillId / 10000 == getJob().getId() &&
                    (getMasterLevelById(skillId) > 10 || skillId == 2321006 || skillId == 2321008)
                ) {
                    fourthJobSkills
                        .append(skillId)
                        .append(", ")
                        .append(getMasterLevelById(skillId))
                        .append("; ");
                }
            }
        }
        for (int s : SKILL_IDS) {
            changeSkillLevel(SkillFactory.getSkill(s), 0, 0);
        }
        setRemainingSp(0);
        setRemainingAp(9);
        setJob(MapleJob.BEGINNER);
        setStr(4);
        setDex(4);
        setInt(4);
        setLuk(4);
        updateSingleStat(MapleStat.STR, getStr());
        updateSingleStat(MapleStat.DEX, getDex());
        updateSingleStat(MapleStat.INT, getInt());
        updateSingleStat(MapleStat.LUK, getLuk());
        updateSingleStat(MapleStat.LEVEL, 1);
        updateSingleStat(MapleStat.HP, 1);
        updateSingleStat(MapleStat.MP, 1);
        updateSingleStat(MapleStat.MAXHP, 50);
        updateSingleStat(MapleStat.MAXMP, 5);
        updateSingleStat(MapleStat.EXP, 0);
        updateSingleStat(MapleStat.JOB, 0);
        setRemainingSp(0);
        setRemainingAp(9);
        updateSingleStat(MapleStat.AVAILABLESP, 0);
        updateSingleStat(MapleStat.AVAILABLEAP, 9);
        setHp(1);
        setMp(1);
        updateSingleStat(MapleStat.HP, 1);
        updateSingleStat(MapleStat.MP, 1);
        setMaxHp(50);
        setMaxMp(5);
        updateSingleStat(MapleStat.MAXHP, 50);
        updateSingleStat(MapleStat.MAXMP, 5);
        if (getPartyQuest() != null) getPartyQuest().playerDead(this);
        checkBerserk();
        addNewPastLife(levelAchieved, jobAchieved.getId(), lds);
        incrementDeathCount();
        new Thread(() -> {
            if (!DeathLogger.logDeath(this, deathMap, monsterTrialPoints, fourthJobSkills, questProgress)) {
                System.err.println("There was an error logging " + getName() + "'s death.");
            }
            /*
            if (!DeathBot.getInstance().announceDeath(this, deathMap)) {
                System.err.println("There was an error announcing " + getName() + "'s death.");
            }
            */
        }).start();
        updatePastLifeExp();
        String jobachieved = MapleJob.getJobName(getJobAchieved().getId());
        String causeofdeath;
        if (lastdamagesource == null) {
            causeofdeath = "by their own hand";
        } else if (lastdamagesource.getType() == MapleMapObjectType.MONSTER) {
            MapleMonster mm = (MapleMonster) lastdamagesource;
            causeofdeath = mm.getName();
            if (
                causeofdeath.charAt(0) == 'A' ||
                causeofdeath.charAt(0) == 'E' ||
                causeofdeath.charAt(0) == 'I' ||
                causeofdeath.charAt(0) == 'O' ||
                causeofdeath.charAt(0) == 'U'
            ) {
                causeofdeath = "at the hands of an " + causeofdeath;
            } else {
                causeofdeath = "at the hands of a " + causeofdeath;
            }
        } else {
            causeofdeath = "by their own hand";
        }
        MaplePacket packet = MaplePacketCreator.serverNotice(
           6,
           "[Graveyard] " +
            getName() +
            ", level " +
            getLevelAchieved() +
            " " +
            jobachieved +
            ", has just perished " +
            causeofdeath +
            ". May the gods let their soul rest until eternity."
        );
        try {
            getClient().getChannelServer().getWorldInterface().broadcastMessage(getName(), packet.getBytes());
        } catch (RemoteException re) {
            getClient().getChannelServer().reconnectWorld();
        }
        recalcLocalStats();
        NPCScriptManager npc = NPCScriptManager.getInstance();
        npc.start(getClient(), 1061000);
        getClient().getSession().write(MaplePacketCreator.enableActions());
    }

    private void playerDead() {
        NPCScriptManager npcsm = NPCScriptManager.getInstance();

        // Cancelling any interactions with NPCs
        if (npcsm != null) {
            if (npcsm.getCM(client) != null) {
                npcsm.getCM(client).dispose();
            }
            npcsm.dispose(client);
        }

        // Do cleanup if in a party quest map instance
        if (getPartyQuest() != null) {
            if (getPartyQuest().getMapInstance(getMap()) != null) {
                getPartyQuest().getMapInstance(getMap()).invokeMethod("playerDead", this);
            }
            getPartyQuest().playerDead(this);
        }

        // Do cleanup if in an event instance
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }

        // If they are 110+ they don't die immediately, but can be resurrected
        if (getLevel() >= 110) {
            cancelAllBuffs(); // Still get all buffs/debuffs removed
            cancelAllDebuffs();

            setExp(0); // Lose exp regardless
            updateSingleStat(MapleStat.EXP, 0);

            NPCScriptManager npc = NPCScriptManager.getInstance();
            npc.start(getClient(), 2041024); // Open up Tombstone, who asks if you want to use a candle
                                             // or instantly exits if you have no candles

            TimerManager tMan = TimerManager.getInstance();
            tMan.schedule(() -> {
                if (isDead()) {
                    ISkill resurrection = SkillFactory.getSkill(2321006);
                    int resurrectionlevel = getSkillLevel(resurrection);
                    if (resurrectionlevel > 0 && !skillIsCooling(2321006) && getItemQuantity(4031485, false) > 0) {
                        MapleInventoryManipulator.removeById(
                            getClient(),
                            MapleItemInformationProvider
                                .getInstance()
                                .getInventoryType(4031485),
                            4031485,
                            1,
                            true,
                            false
                        );
                        setHp(getMaxHp(), false);
                        setMp(getMaxMp());
                        updateSingleStat(MapleStat.HP, getMaxHp());
                        updateSingleStat(MapleStat.MP, getMaxMp());
                        long cooldowntime = 3600000L - (180000L * (long) resurrectionlevel);
                        giveCoolDowns(2321006, System.currentTimeMillis(), cooldowntime, true);
                        incrementDeathPenaltyAndRecalc(5);
                        setExp(0);
                        updateSingleStat(MapleStat.EXP, 0);
                    } else {
                        permadeath();
                    }
                }
            }, 120L * 1000L); // 120 seconds
            getClient().getSession().write(MaplePacketCreator.getClock(120));

            getClient().getSession().write(MaplePacketCreator.enableActions());
            // If they select OK to revive, ChangeMapHandler will call permadeath()
        } else {
            permadeath(); // Permadeath anyways if you are < 110
        }
    }

    public int getTierPoints(int tier) {
        if (tier < 1) {
            return 0;
        }
        return (int) (getTierPoints(tier - 1) + (10 * Math.pow(tier + 1, 2)) * (Math.floor(tier * 1.5d) + 3));
    }

    public void updatePartyMemberHP() {
        if (party != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    MapleCharacter other =
                        ChannelServer
                            .getInstance(channel)
                            .getPlayerStorage()
                            .getCharacterByName(partychar.getName());
                    if (other != null) {
                        other
                            .getClient()
                            .getSession()
                            .write(
                                MaplePacketCreator.updatePartyMemberHP(
                                    getId(),
                                    hp,
                                    localMaxHp
                                )
                            );
                    }
                }
            }
        }
    }

    public void receivePartyMemberHP() {
        if (party != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    MapleCharacter other =
                        ChannelServer
                            .getInstance(channel)
                            .getPlayerStorage()
                            .getCharacterByName(partychar.getName());
                    if (other != null) {
                        getClient()
                            .getSession()
                            .write(
                                MaplePacketCreator.updatePartyMemberHP(
                                    other.getId(),
                                    other.getHp(),
                                    other.getCurrentMaxHp()
                                )
                            );
                    }
                }
            }
        }
    }

    public void setMp(int newMp) {
        if (newMp < 0) {
            newMp = 0;
        } else if (newMp > localMaxMp) {
            newMp = localMaxMp;
        }
        mp = newMp;
    }

    public void addHP(int delta) {
        setHp(hp + delta);
        updateSingleStat(MapleStat.HP, hp);
    }

    public void addMP(int delta) {
        setMp(mp + delta);
        updateSingleStat(MapleStat.MP, mp);
    }

    public void addMPHP(int hpDiff, int mpDiff) {
        setHp(hp + hpDiff);
        setMp(mp + mpDiff);
        List<Pair<MapleStat, Integer>> stats = new ArrayList<>(2);
        stats.add(new Pair<>(MapleStat.HP, hp));
        stats.add(new Pair<>(MapleStat.MP, mp));
        MaplePacket updatePacket = MaplePacketCreator.updatePlayerStats(stats);
        client.getSession().write(updatePacket);
    }

    public void updateSingleStat(MapleStat stat, int newval, boolean itemReaction) {
        Pair<MapleStat, Integer> statpair = new Pair<>(stat, newval);
        MaplePacket updatePacket =
            MaplePacketCreator.updatePlayerStats(Collections.singletonList(statpair), itemReaction);
        client.getSession().write(updatePacket);
    }

    public void updateSingleStat(MapleStat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }

    public void gainExp(int gain, boolean show, boolean inChat) {
        gainExp(gain, show, inChat, true, true);
    }

    public void gainExp(int gain, boolean show, boolean inChat, boolean white) {
        gainExp(gain, show, inChat, white, true);
    }

    public void gainExp(int gain, boolean show, boolean inChat, boolean white, boolean etcLose) {
        int levelCap = getClient().getChannelServer().getLevelCap();
        if (!etcLose && gain < 0) {
            gain += Integer.MAX_VALUE;
            if (getLevel() < levelCap) levelUp();
            boolean overflowed = false;
            while (gain > 0) {
                if (getLevel() >= levelCap && !overflowed) {
                    addOverflowExp((long) gain);
                    overflowed = true;
                }
                gain -= (ExpTable.getExpNeededForLevel(level) - this.exp.get());
                if (getLevel() < levelCap) levelUp();
            }
            setExp(0);
            updateSingleStat(MapleStat.EXP, exp.get());
            client.getSession().write(MaplePacketCreator.getShowExpGain(Integer.MAX_VALUE, inChat, white));
            return;
        }
        if (getLevel() < levelCap) {
            if ((long) exp.get() + (long) gain > (long) Integer.MAX_VALUE) {
                int gainFirst = ExpTable.getExpNeededForLevel(level) - exp.get();
                gain -= gainFirst + 1;
                gainExp(gainFirst + 1, false, inChat, white);
            }
            updateSingleStat(MapleStat.EXP, exp.addAndGet(gain));
        } else {
            addOverflowExp((long) gain);
            if (show && gain != 0) {
                client.getSession().write(MaplePacketCreator.getShowExpGain(gain, inChat, white));
            }
            return;
        }
        if (show && gain != 0) {
            client.getSession().write(MaplePacketCreator.getShowExpGain(gain, inChat, white));
        }
        if (exp.get() >= ExpTable.getExpNeededForLevel(getLevel()) && getLevel() < levelCap) {
            if (getClient().getChannelServer().getMultiLevel()) {
                while (getLevel() < levelCap && exp.get() >= ExpTable.getExpNeededForLevel(getLevel())) {
                    levelUp();
                }
            } else {
                levelUp();
                int need = ExpTable.getExpNeededForLevel(getLevel());
                if (exp.get() >= need) {
                    setExp(need - 1);
                    updateSingleStat(MapleStat.EXP, exp.get());
                }
            }
        }
    }

    public void addOverflowExp(long gain) {
        overflowExp += gain;
    }

    public long getOverflowExp() {
        return overflowExp;
    }

    public void silentPartyUpdate() {
        if (party != null) {
            try {
                getClient().getChannelServer().getWorldInterface().updateParty(party.getId(), PartyOperation.SILENT_UPDATE, new MaplePartyCharacter(MapleCharacter.this));
            } catch (RemoteException e) {
                sqlException(e);
                getClient().getChannelServer().reconnectWorld();
            }
        }
    }

    public boolean isGM() {
        return gmLevel >= 3;
    }

    public int getGMLevel() {
        return gmLevel;
    }

    public boolean hasGmLevel(int level) {
        return gmLevel >= level;
    }

    public MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public MapleShop getShop() {
        return shop;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public int getMeso() {
        return meso.get();
    }

    public int getSavedLocation(SavedLocationType type) {
        return savedLocations[type.ordinal()];
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = getMapId();
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = -1;
    }

    public void setMeso(int set) {
        meso.set(set);
        updateSingleStat(MapleStat.MESO, set, false);
    }

    public void gainMeso(int gain) {
        gainMeso(gain, true, false, false);
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false, false);
    }

    public void gainMeso(int gain, boolean show, boolean enableActions) {
        gainMeso(gain, show, enableActions, false);
    }

    public void gainMeso(int gain, boolean show, boolean enableActions, boolean inChat) {
        int newVal;
        long total = (long) meso.get() + (long) gain;
        if (total >= Integer.MAX_VALUE) {
            meso.set(Integer.MAX_VALUE);
            newVal = Integer.MAX_VALUE;
        } else if (total < 0) {
            meso.set(0);
            newVal = 0;
        } else {
            newVal = meso.addAndGet(gain);
        }
        updateSingleStat(MapleStat.MESO, newVal, enableActions);
        if (show) {
            client.getSession().write(MaplePacketCreator.getShowMesoGain(gain, inChat));
        }
    }

    public void controlMonster(MapleMonster monster, boolean aggro) {
        monster.setController(this);
        controlled.add(monster);
        client.getSession().write(MaplePacketCreator.controlMonster(monster, false, aggro));
    }

    public void stopControllingMonster(MapleMonster monster) {
        controlled.remove(monster);
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (!monster.isControllerHasAggro()) {
            if (monster.getController() == this) {
                monster.setControllerHasAggro(true);
            } else {
                monster.switchController(this, true);
            }
        }
    }

    public Collection<MapleMonster> getControlledMonsters() {
        return Collections.unmodifiableCollection(controlled);
    }

    public int getNumControlledMonsters() {
        return controlled.size();
    }

    @Override
    public String toString() {
        return "Character: " + name;
    }

    public int getAccountID() {
        return accountid;
    }

    public void mobKilled(int mobId) {
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == MapleQuestStatus.Status.COMPLETED || q.getQuest().canComplete(this, null)) {
                continue;
            }
            if (q.mobKilled(mobId) && !(q.getQuest() instanceof MapleCustomQuest)) {
                client.getSession().write(MaplePacketCreator.updateQuestMobKills(q));
                if (q.getQuest().canComplete(this, null)) {
                    client.getSession().write(MaplePacketCreator.getShowQuestCompletion(q.getQuest().getId()));
                }
            }
        }
        makeQuestProgress(mobId, 0, null);
    }

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new ArrayList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus().equals(MapleQuestStatus.Status.STARTED) && !(q.getQuest() instanceof MapleCustomQuest)) {
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new ArrayList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (
                q.getStatus().equals(MapleQuestStatus.Status.COMPLETED) &&
                !(q.getQuest() instanceof MapleCustomQuest)
            ) {
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public IPlayerInteractionManager getInteraction() {
        return interaction;
    }

    public void setInteraction(IPlayerInteractionManager box) {
        interaction = box;
    }

    public Map<ISkill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public void dispelSkill(int skillId) {
        List<MapleBuffStatValueHolder> allBuffs = new ArrayList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (skillId == 0) {
                if (mbsvh.effect.isSkill()) {
                    switch (mbsvh.effect.getSourceId()) {
                        case 1004:
                        case 1321007:
                        case 2121005:
                        case 2221005:
                        case 2311006:
                        case 2321003:
                        case 3111002:
                        case 3111005:
                        case 3211002:
                        case 3211005:
                        case 4111002:
                            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                    }
                }
            } else {
                if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillId) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            }
        }
    }

    public boolean isActiveBuffedValue(int skillId) {
        List<MapleBuffStatValueHolder> allBuffs = new ArrayList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillId) {
                return true;
            }
        }
        return false;
    }

    public int getSkillLevel(int skillId) {
        return getSkillLevel(SkillFactory.getSkill(skillId));
    }

    public int getSkillLevel(ISkill skill) {
        SkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.skillevel;
    }

    public int getMasterLevel(ISkill skill) {
        SkillEntry ret = skills.get(skill);
        if (ret == null) return 0;
        return ret.masterlevel;
    }

    public int getMasterLevelById(int skillId) {
        ISkill s = SkillFactory.getSkill(skillId);
        SkillEntry ret = skills.get(s);
        if (ret == null) return 0;
        return ret.masterlevel;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public int getAvoidability() {
        return avoidability;
    }

    public int getTotalDex() {
        return localDex;
    }

    public int getTotalInt() {
        return localInt;
    }

    public int getTotalStr() {
        return localStr;
    }

    public int getTotalLuk() {
        return localLuk;
    }

    public int getTotalMagic() {
        return magic;
    }

    public double getSpeedMod() {
        return speedMod;
    }

    public double getJumpMod() {
        return jumpMod;
    }

    public int getTotalWatk() {
        return watk;
    }

    public static int rand(int lbound, int ubound) {
        return (int) ((Math.random() * (ubound - lbound + 1.0d)) + lbound);
    }

    public int getMaxDis(MapleCharacter player) {
        IItem weapon_item = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        if (weapon_item != null) {
            MapleWeaponType weapon = MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId());
            if (weapon == MapleWeaponType.SPEAR || weapon == MapleWeaponType.POLE_ARM) {
                maxDis = 106;
            }
            if (weapon == MapleWeaponType.DAGGER || weapon == MapleWeaponType.SWORD1H || weapon == MapleWeaponType.AXE1H || weapon == MapleWeaponType.BLUNT1H) {
                maxDis = 63;
            }
            if (weapon == MapleWeaponType.SWORD2H || weapon == MapleWeaponType.AXE1H || weapon == MapleWeaponType.BLUNT1H) {
                maxDis = 73;
            }
            if (weapon == MapleWeaponType.STAFF || weapon == MapleWeaponType.WAND) {
                maxDis = 51;
            }
            if (weapon == MapleWeaponType.CLAW) {
                skil = SkillFactory.getSkill(4000001);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    maxDis = (skil.getEffect(player.getSkillLevel(skil)).getRange()) + 205;
                } else {
                    maxDis = 205;
                }
            }
            if (weapon == MapleWeaponType.BOW || weapon == MapleWeaponType.CROSSBOW) {
                skil = SkillFactory.getSkill(3000002);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    maxDis = (skil.getEffect(player.getSkillLevel(skil)).getRange()) + 270;
                } else {
                    maxDis = 270;
                }
            }
        }
        return maxDis;
    }

    public int calculateMaxBaseDamage() {
        int maxbasedamage;
        if (getTotalWatk() < 1) {
            maxbasedamage = 1;
        } else {
            IItem weaponItem = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            if (weaponItem != null) {
                MapleWeaponType weapon =
                    MapleItemInformationProvider
                        .getInstance()
                        .getWeaponType(weaponItem.getItemId());
                int mainStat, secondaryStat;
                if (
                    weapon == MapleWeaponType.BOW ||
                    weapon == MapleWeaponType.CROSSBOW ||
                    weapon == MapleWeaponType.GUN
                ) {
                    mainStat = localDex;
                    secondaryStat = localStr;
                } else if (
                    getJob().isA(MapleJob.THIEF) &&
                    (weapon == MapleWeaponType.CLAW || weapon == MapleWeaponType.DAGGER)
                ) {
                    mainStat = localLuk;
                    secondaryStat = localDex + localStr;
                } else {
                    mainStat = localStr;
                    secondaryStat = localDex;
                }
                maxbasedamage =
                    (int) (((weapon.getMaxDamageMultiplier() * (double) mainStat + (double) secondaryStat) / 100.0d)
                        * (double) getTotalWatk());
                maxbasedamage += 10;
            } else {
                maxbasedamage = 0;
            }
        }
        return maxbasedamage;
    }

    public int calculateMinBaseDamage() {
        int minbasedamage = 0;
        final int atk = getTotalWatk();
        if (atk == 0) {
            minbasedamage = 1;
        } else {
            IItem weapon_item = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            if (weapon_item != null) {
                MapleWeaponType weapon =
                    MapleItemInformationProvider
                        .getInstance()
                        .getWeaponType(weapon_item.getItemId());
                double sword;
                if (getJob().isA(MapleJob.FIGHTER)) {
                    skil = SkillFactory.getSkill(1100000);
                    skill = getSkillLevel(skil);
                    if (skill > 0) {
                        sword = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                    } else {
                        sword = 0.1d;
                    }
                } else {
                    skil = SkillFactory.getSkill(1200000);
                    skill = getSkillLevel(skil);
                    if (skill > 0) {
                        sword = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                    } else {
                        sword = 0.1d;
                    }
                }
                skil = SkillFactory.getSkill(1100001);
                skill = getSkillLevel(skil);
                double axe;
                if (skill > 0) {
                    axe = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                } else {
                    axe = 0.1d;
                }
                skil = SkillFactory.getSkill(1200001);
                skill = getSkillLevel(skil);
                double blunt;
                if (skill > 0) {
                    blunt = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                } else {
                    blunt = 0.1d;
                }
                skil = SkillFactory.getSkill(1300000);
                skill = getSkillLevel(skil);
                double spear;
                if (skill > 0) {
                    spear = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                } else {
                    spear = 0.1d;
                }
                skil = SkillFactory.getSkill(1300001);
                skill = getSkillLevel(skil);
                double polearm;
                if (skill > 0) {
                    polearm = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                } else {
                    polearm = 0.1d;
                }
                skil = SkillFactory.getSkill(3200000);
                skill = getSkillLevel(skil);
                ISkill skil2 = SkillFactory.getSkill(3220004);
                int skill2 = getSkillLevel(skil2);
                double crossbow;
                if (skill > 0 || skill2 > 0) {
                    if (skill2 <= 0) {
                        crossbow = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                    } else {
                        crossbow = (skil2.getEffect(getSkillLevel(skil2)).getMastery() * 5.0d + 10.0d) / 100.0d;
                    }
                } else {
                    crossbow = 0.1d;
                }
                skil = SkillFactory.getSkill(3100000);
                skill = getSkillLevel(skil);
                skil2 = SkillFactory.getSkill(3120005);
                skill2 = getSkillLevel(skil2);
                double bow;
                if (skill > 0 || skill2 > 0) {
                    if (skill2 <= 0) {
                        bow = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                    } else {
                        bow = (skil2.getEffect(getSkillLevel(skil2)).getMastery() * 5.0d + 10.0d) / 100.0d;
                    }
                } else {
                    bow = 0.1d;
                }
                skil = SkillFactory.getSkill(4100000);
                skill = getSkillLevel(skil);
                double claw;
                if (skill > 0) {
                    claw = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                } else {
                    claw = 0.1d;
                }
                skil = SkillFactory.getSkill(4200000);
                skill = getSkillLevel(skil);
                double dagger;
                if (skill > 0) {
                    dagger = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                } else {
                    dagger = 0.1d;
                }
                skil = SkillFactory.getSkill(5100001);
                skill = getSkillLevel(skil);
                double knuckle;
                if (skill > 0) {
                    knuckle = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                } else {
                    knuckle = 0.1d;
                }
                skil = SkillFactory.getSkill(5200000);
                skill = getSkillLevel(skil);
                double gun;
                if (skill > 0) {
                    gun = (skil.getEffect(getSkillLevel(skil)).getMastery() * 5.0d + 10.0d) / 100.0d;
                } else {
                    gun = 0.1d;
                }

                if (weapon == MapleWeaponType.CROSSBOW) {
                    minbasedamage = (int) ((localDex * 0.9d * 3.6d * crossbow + localStr) / 100.0d * (atk + 15.0d));
                } else if (weapon == MapleWeaponType.BOW) {
                    minbasedamage = (int) ((localDex * 0.9d * 3.4d * bow + localStr) / 100.0d * (atk + 15.0d));
                } else if (getJob().isA(MapleJob.THIEF) && weapon == MapleWeaponType.DAGGER) {
                    minbasedamage = (int) ((localLuk * 0.9d * 3.6d * dagger + localStr + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.DAGGER) {
                    minbasedamage = (int) ((localStr * 0.9d * 4.0d * dagger + localDex) / 100.0d * atk);
                } else if (getJob().isA(MapleJob.THIEF) && weapon == MapleWeaponType.CLAW) {
                    minbasedamage = (int) ((localLuk * 0.9d * 3.6d * claw + localStr + localDex) / 100.0d * (atk + 15.0d));
                } else if (weapon == MapleWeaponType.CLAW) {
                    minbasedamage = (int) ((localStr * 0.9d * 3.6d * claw + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.SPEAR) {
                    minbasedamage = (int) ((localStr * 0.9d * 3.0d * spear + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.POLE_ARM) {
                    minbasedamage = (int) ((localStr * 0.9d * 3.0d * polearm + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.SWORD1H) {
                    minbasedamage = (int) ((localStr * 0.9d * 4.0d * sword + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.SWORD2H) {
                    minbasedamage = (int) ((localStr * 0.9d * 4.6d * sword + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.AXE1H) {
                    minbasedamage = (int) ((localStr * 0.9d * 3.2d * axe + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.BLUNT1H) {
                    minbasedamage = (int) ((localStr * 0.9d * 3.2d * blunt + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.AXE2H) {
                    minbasedamage = (int) ((localStr * 0.9d * 3.4d * axe + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.BLUNT2H) {
                    minbasedamage = (int) ((localStr * 0.9d * 3.4d * blunt + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.STAFF || weapon == MapleWeaponType.WAND) {
                    double staffwand = 0.1d;
                    minbasedamage = (int) ((localStr * 0.9d * 3.0d * staffwand + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.KNUCKLE) {
                    minbasedamage = (int) ((localStr * 0.9d * 4.8d * knuckle + localDex) / 100.0d * atk);
                } else if (weapon == MapleWeaponType.GUN) {
                    minbasedamage = (int) ((localDex * 0.9d * 3.6d * gun + localStr) / 100.0d * atk);
                }
            }
        }
        return minbasedamage;
    }

    public int getRandomage(MapleCharacter player) {
        int maxdamage = player.getCurrentMaxBaseDamage();
        int mindamage = player.calculateMinBaseDamage();
        return MapleCharacter.rand(mindamage, maxdamage);
    }

    public void levelUp() {
        ISkill improvingMaxHP = null;
        int improvingMaxHPLevel = 0;
        ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
        int improvingMaxMPLevel = getSkillLevel(improvingMaxMP);
        remainingAp += 5;
        if (job == MapleJob.BEGINNER) {
            maxhp += rand(28, 32);
            maxmp += rand(10, 12);
        } else if (job.isA(MapleJob.WARRIOR)) {
            improvingMaxHP = SkillFactory.getSkill(1000001);
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            maxhp += rand(24, 28);
            maxmp += rand(4, 6);
        } else if (job.isA(MapleJob.MAGICIAN)) {
            maxhp += rand(26, 30);
            maxmp += rand(22, 24);
        } else if (job.isA(MapleJob.BOWMAN) || job.isA(MapleJob.GM)) {
            maxhp += rand(42, 46);
            maxmp += rand(14, 16);
        } else if (job.isA(MapleJob.THIEF)) {
            if (job.isA(MapleJob.ASSASSIN)) {
                maxhp += rand(32, 36);
            } else {
                maxhp += rand(26, 30);
            }
            maxmp += rand(14, 16);
        } else if (job.isA(MapleJob.PIRATE)) {
            improvingMaxHP = SkillFactory.getSkill(5100000);
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            if (job.isA(MapleJob.BRAWLER)) {
                maxhp += rand(36, 40);
            } else {
                maxhp += rand(26, 30);
            }
            maxmp += rand(18, 23);
        }
        if (improvingMaxHPLevel > 0 && improvingMaxHP != null) {
            maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
        }
        if (improvingMaxMPLevel > 0) {
            maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
        }
        maxmp += getTotalInt() / 10;
        exp.addAndGet(-ExpTable.getExpNeededForLevel(level));
        level += 1;
        if (level == 30 && getItemQuantity(5030000, false) == 0) {
            MapleInventoryManipulator.addById(client, 5030000, (short) 1);
            sendHint(
                "Congrats on reaching #elevel 30#n for the first time!\r\n\r\n" +
                    "You've been given a #rspecial store#k in the cash tab of your inventory.\r\n\r\n" +
                    "You may set it up in the FM rooms to sell your items!"
            );
        } else if (level >= 120 && level % 10 == 0) {
            MaplePacket packet =
                MaplePacketCreator.serverNotice(
                    6,
                    "Congratulations to " +
                        getName() +
                        " for reaching level " +
                        level +
                        " as a " +
                        MapleJob.getJobName(getJob().getId()) +
                        "!"
                );
            try {
                getClient().getChannelServer().getWorldInterface().broadcastMessage(getName(), packet.getBytes());
            } catch (RemoteException e) {
                getClient().getChannelServer().reconnectWorld();
            }
        }
        if (level > highestlevelachieved) {
            setHighestLevelAchieved(level);
        }
        setTotalParagonLevel(paragonlevel + level);
        maxhp = Math.min(30000, maxhp);
        maxmp = Math.min(30000, maxmp);
        setHp(30000);
        setMp(30000);
        List<Pair<MapleStat, Integer>> statup = new ArrayList<>(8);
        statup.add(new Pair<>(MapleStat.AVAILABLEAP, remainingAp));
        statup.add(new Pair<>(MapleStat.MAXHP, maxhp));
        statup.add(new Pair<>(MapleStat.MAXMP, maxmp));
        statup.add(new Pair<>(MapleStat.HP, getHp()));
        statup.add(new Pair<>(MapleStat.MP, getMp()));
        statup.add(new Pair<>(MapleStat.EXP, exp.get()));
        statup.add(new Pair<>(MapleStat.LEVEL, level));
        if (job != MapleJob.BEGINNER) {
            remainingSp += 3;
            statup.add(new Pair<>(MapleStat.AVAILABLESP, remainingSp));
        }
        getClient().getSession().write(MaplePacketCreator.updatePlayerStats(statup));
        getMap().broadcastMessage(this, MaplePacketCreator.showLevelup(getId()), false);
        recalcLocalStats();
        silentPartyUpdate();
        guildUpdate();
    }

    public void changeKeybinding(int key, MapleKeyBinding keybinding) {
        if (keybinding.getType() != 0) {
            keymap.put(key, keybinding);
        } else {
            keymap.remove(key);
        }
    }

    public void sendKeymap() {
        getClient().getSession().write(MaplePacketCreator.getKeymap(keymap));
    }

    public void sendMacros() {
        boolean macros = false;
        for (int i = 0; i < 5; ++i) {
            if (skillMacros[i] != null) macros = true;
        }
        if (macros) {
            getClient().getSession().write(MaplePacketCreator.getMacros(skillMacros));
        }
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
    }

    public void tempban(String reason, Calendar duration, int greason) {
        tempban(reason, duration, greason, client.getAccID());
        client.getSession().write(MaplePacketCreator.sendGMPolice(greason, reason, (int) (duration.getTimeInMillis() / 1000L))); // Put duration as seconds
        TimerManager.getInstance().schedule(() -> client.getSession().close(), 10000);
    }

    public static boolean tempban(String reason, Calendar duration, int greason, int accountid) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setInt(4, accountid);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            sqlException(e);
        }
        return false;
    }

    public void ban(String reason, boolean permBan) {
        if (!client.isGuest()) {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps;
                if (permBan) {
                    getClient().banMacs();
                    ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                    String[] ipSplit = client.getSession().getRemoteAddress().toString().split(":");
                    ps.setString(1, ipSplit[0]);
                    ps.executeUpdate();
                    ps.close();
                }
                ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ?, greason = ? WHERE id = ?");
                ps.setInt(1, 1);
                ps.setString(2, reason);
                ps.setInt(3, 12);
                ps.setInt(4, accountid);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                sqlException(e);
            }
        }
        client.getSession().write(MaplePacketCreator.sendGMPolice(0, reason, 1000000));
        TimerManager.getInstance().schedule(() -> client.getSession().close(), 10000);
    }

    public static boolean ban(String id, String reason, boolean accountId) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (id.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, id);
                ps.executeUpdate();
                ps.close();
            }
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            boolean ret = false;
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
                ps.setString(1, reason);
                ps.setInt(2, rs.getInt(1));
                ps.executeUpdate();
                ret = true;
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException e) {
            sqlException(e);
        }
        return false;
    }

    public static int getAccIdFromCNAME(String name) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            int id_ = rs.getInt("accountid");
            rs.close();
            ps.close();
            return id_;
        } catch (SQLException e) {
            sqlException(e);
        }
        return -1;
    }

    @Override
    public int getObjectId() {
        return getId();
    }

    @Override
    public void setObjectId(int id) {
        throw new UnsupportedOperationException();
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public int getCurrentMaxHp() {
        return localMaxHp;
    }

    public int getCurrentMaxMp() {
        return localMaxMp;
    }

    public int getCurrentMaxBaseDamage() {
        return localMaxBaseDamage;
    }

    public int getTotalMdef() {
        return mdef;
    }

    public int getTotalWdef() {
        return wdef;
    }

    public void addVisibleMapObject(MapleMapObject mo) {
        synchronized (visibleMapObjects) {
            visibleMapObjects.add(mo);
        }
    }

    public void removeVisibleMapObject(MapleMapObject mo) {
        synchronized (visibleMapObjects) {
            Iterator<MapleMapObject> moiter = visibleMapObjects.iterator();
            while (moiter.hasNext()) {
                MapleMapObject mmo = moiter.next();
                if (mo == null ? mmo == null : mo.equals(mmo)) {
                    moiter.remove();
                    break;
                }
            }
            //visibleMapObjects.remove(mo);
        }
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        synchronized (visibleMapObjects) {
            Iterator<MapleMapObject> moiter = visibleMapObjects.iterator();
            while (moiter.hasNext()) {
                MapleMapObject mmo = moiter.next();
                if (mo == null ? mmo == null : mo.equals(mmo)) {
                    return true;
                }
            }
            return false;
        }
        //return visibleMapObjects.contains(mo);
    }

    public Set<MapleMapObject> getVisibleMapObjects() { // public Collection<MapleMapObject>
        //return Collections.unmodifiableCollection(visibleMapObjects);
        return visibleMapObjects;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.removePlayerFromMap(this.getObjectId()));
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (!isHidden() || client.getPlayer().isGM()) {
            client.getSession().write(MaplePacketCreator.spawnPlayerMapobject(this));
            for (int i = 0; i < 3; ++i) {
                if (pets[i] != null) {
                    client.getSession().write(MaplePacketCreator.showPet(this, pets[i], false, false));
                } else {
                    break;
                }
            }
        }
    }

    private void recalcLocalStats() {
        int oldmaxhp = localMaxHp;
        localMaxHp = getMaxHp();
        localMaxMp = getMaxMp();
        localDex = getDex();
        localInt = getInt();
        localStr = getStr();
        localLuk = getLuk();
        int speed = 100, jump = 100;
        magic = localInt;
        watk = 0;
        wdef = 0;
        mdef = 0;
        accuracy = 0;
        avoidability = 0;
        Integer mapleWarrior = getBuffedValue(MapleBuffStat.MAPLE_WARRIOR);
        double mapleWarriorMultiplier;
        if (mapleWarrior != null && mapleWarrior > 0) {
            mapleWarriorMultiplier = 1.0d + (double) getBuffedValue(MapleBuffStat.MAPLE_WARRIOR) / 100.0d;
        } else {
            mapleWarriorMultiplier = 1.0d;
        }
        double shieldMastery = 1.0d;
        if (job.getId() == 421 || job.getId() == 422) { // Chief Bandit / Shadower
            ISkill shieldMasterySkill = SkillFactory.getSkill(4210000);
            if (getSkillLevel(shieldMasterySkill) > 0) {
                shieldMastery = shieldMasterySkill.getEffect(getSkillLevel(shieldMasterySkill)).getX() / 100.0d;
            }
        } else if (job.getId() == 111 || job.getId() == 112) { // Crusader / Hero
            ISkill shieldMasterySkill = SkillFactory.getSkill(1110001);
            if (getSkillLevel(shieldMasterySkill) > 0) {
                shieldMastery = shieldMasterySkill.getEffect(getSkillLevel(shieldMasterySkill)).getX() / 100.0d;
            }
        } else if (job.getId() == 121 || job.getId() == 122) { // White Knight / Paladin
            ISkill shieldMasterySkill = SkillFactory.getSkill(1210001);
            if (getSkillLevel(shieldMasterySkill) > 0) {
                shieldMastery = shieldMasterySkill.getEffect(getSkillLevel(shieldMasterySkill)).getX() / 100.0d;
            }
        }
        for (IItem item : getInventory(MapleInventoryType.EQUIPPED)) {
            IEquip equip = (IEquip) item;
            localMaxHp += equip.getHp();
            localMaxMp += equip.getMp();
            localDex += equip.getDex();
            localInt += equip.getInt();
            localStr += equip.getStr();
            localLuk += equip.getLuk();
            magic += equip.getMatk() + equip.getInt();
            watk += equip.getWatk();
            speed += equip.getSpeed();
            jump += equip.getJump();
            if (equip.getItemId() / 10000 == 109 && shieldMastery > 1.0d) { // Shield
                wdef += (int) (equip.getWdef() * shieldMastery);
            } else {
                wdef += equip.getWdef();
            }
            mdef += equip.getMdef();
            accuracy += equip.getAcc();
            avoidability += equip.getAvoid();
        }
        if (mapleWarriorMultiplier > 1.0d) {
            magic += localInt * (mapleWarriorMultiplier - 1.0d);
            localDex *= mapleWarriorMultiplier;
            localInt *= mapleWarriorMultiplier;
            localStr *= mapleWarriorMultiplier;
            localLuk *= mapleWarriorMultiplier;
        }
        Integer hbhp = getBuffedValue(MapleBuffStat.HYPERBODYHP);
        if (hbhp != null) {
            localMaxHp += (int) ((hbhp.doubleValue() / 100.0d) * (double) localMaxHp);
        }
        Integer hbmp = getBuffedValue(MapleBuffStat.HYPERBODYMP);
        if (hbmp != null) {
            localMaxMp += (int) ((hbmp.doubleValue() / 100.0d) * (double) localMaxMp);
        }
        localMaxHp = Math.min(30000, localMaxHp);
        localMaxMp = Math.min(30000, localMaxMp);
        Integer watkbuff = getBuffedValue(MapleBuffStat.WATK);
        if (watkbuff != null) watk += watkbuff;
        if (job.isA(MapleJob.BOWMAN)) {
            ISkill expert = null;
            if (job.isA(MapleJob.CROSSBOWMASTER)) {
                expert = SkillFactory.getSkill(3220004);
            } else if (job.isA(MapleJob.BOWMASTER)) {
                expert = SkillFactory.getSkill(3120005);
            }
            if (expert != null) {
                int boostLevel = getSkillLevel(expert);
                if (boostLevel > 0) {
                    watk += expert.getEffect(boostLevel).getX();
                }
            }
        }

        ISkill bulletTime = SkillFactory.getSkill(5000000);
        ISkill nimbleBody = SkillFactory.getSkill(4000000);
        if (getSkillLevel(bulletTime) > 0) {
            avoidability += bulletTime.getEffect(getSkillLevel(bulletTime)).getY();
        }
        if (getSkillLevel(nimbleBody) > 0) {
            avoidability += nimbleBody.getEffect(getSkillLevel(nimbleBody)).getY();
        }

        if (getJob().isA(MapleJob.BRAWLER)) {
            avoidability += (int) (localDex * 1.5d + localLuk * 0.5d);
        } else if (getJob().isA(MapleJob.GUNSLINGER)) {
            avoidability += (int) (localDex * 0.125d + localLuk * 0.5d);
        } else {
            avoidability += (int) (localDex * 0.25d + localLuk * 0.5d);
        }

        Integer matkbuff = getBuffedValue(MapleBuffStat.MATK);
        if (matkbuff != null) magic += matkbuff;
        Integer speedbuff = getBuffedValue(MapleBuffStat.SPEED);
        if (speedbuff != null) speed += speedbuff;
        Integer jumpbuff = getBuffedValue(MapleBuffStat.JUMP);
        if (jumpbuff != null) jump += jumpbuff;
        if (speed > 140) speed = 140;
        if (jump > 123) jump = 123;
        speedMod = speed / 100.0d;
        jumpMod = jump / 100.0d;
        Integer mount = getBuffedValue(MapleBuffStat.MONSTER_RIDING);
        if (mount != null) {
            jumpMod = 1.23d;
            switch (mount) {
                case 1:
                    speedMod = 1.5d;
                    break;
                case 2:
                    speedMod = 1.7d;
                    break;
                case 3:
                    speedMod = 1.8d;
                    break;
                case 5:
                    speedMod = 1.0d;
                    jumpMod = 1.0d;
                    break;
                default:
                    speedMod = 2.0d;
            }
        }
        //
        if (job.isA(MapleJob.WARRIOR) || job.isA(MapleJob.MAGICIAN) || job.isA(MapleJob.BEGINNER)) {
            accuracy += (int) (localDex * 0.8d + localLuk * 0.5d);
        } else if (job.equals(MapleJob.BRAWLER) || job.equals(MapleJob.MARAUDER) || job.equals(MapleJob.BUCCANEER)) {
            accuracy += (int) (localDex * 0.9d + localLuk * 0.3d);
        } else {
            accuracy += (int) (localDex * 0.6d + localLuk * 0.3d);
        }

        Integer accbuff = getBuffedValue(MapleBuffStat.ACC);
        if (accbuff != null) accuracy += accbuff;

        Integer avoidbuff = getBuffedValue(MapleBuffStat.AVOID);
        if (avoidbuff != null) avoidability += avoidbuff;

        Integer wdefbuff = getBuffedValue(MapleBuffStat.WDEF);
        if (wdefbuff != null) wdef += wdefbuff;

        Integer mdefbuff = getBuffedValue(MapleBuffStat.MDEF);
        if (mdefbuff != null) mdef += mdefbuff;

        Integer echoBuff = getBuffedValue(MapleBuffStat.ECHO_OF_HERO);
        if (echoBuff != null) {
            double echoMulti = (double) (100 + echoBuff) / 100.0d;
            magic *= echoMulti;
            watk *= echoMulti;
        }

        mdef += localInt;

        // What follows is code for getting accuracy from passives
        IItem weapon_item = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        if (weapon_item != null) {
            MapleWeaponType weapon = MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId());
            ISkill accpassive;
            int acclevel, localsword, localaxe, localblunt, localspear, localpolearm, localclaw, localdagger, localknuckle, localgun, localbow, localcrossbow;
            localsword = localaxe = localblunt = localspear = localpolearm = localclaw = localdagger = localknuckle = localgun = localbow = localcrossbow = 0;
            /*
            if (getJob().isA(MapleJob.FIGHTER)) {
                accpassive = SkillFactory.getSkill(1100000);
                acclevel = getSkillLevel(accpassive);
                if (acclevel > 0) {
                    localsword = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                }
                accpassive = SkillFactory.getSkill(1100001);
                acclevel = getSkillLevel(accpassive);
                if (acclevel > 0) {
                    localaxe = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                }
            } else if (getJob().isA(MapleJob.PAGE)) {
                accpassive = SkillFactory.getSkill(1200000);
                acclevel = getSkillLevel(accpassive);
                if (acclevel > 0) {
                    localsword = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                }
                accpassive = SkillFactory.getSkill(1200001);
                acclevel = getSkillLevel(accpassive);
                if (acclevel > 0) {
                    localblunt = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                }
            } else if (getJob().isA(MapleJob.SPEARMAN)) {
                accpassive = SkillFactory.getSkill(1300000);
                acclevel = getSkillLevel(accpassive);
                if (acclevel > 0) {
                    localspear = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                }
                accpassive = SkillFactory.getSkill(1300001);
                acclevel = getSkillLevel(accpassive);
                if (acclevel > 0) {
                    localpolearm = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                }
            } else */
            if (getJob().isA(MapleJob.THIEF)) {
                accpassive = SkillFactory.getSkill(4000000);
                acclevel = getSkillLevel(accpassive);
                if (acclevel > 0) {
                    accuracy += accpassive.getEffect(getSkillLevel(accpassive)).getX();
                }
                if (getJob().isA(MapleJob.ASSASSIN)) {
                    accpassive = SkillFactory.getSkill(4100000);
                    acclevel = getSkillLevel(accpassive);
                    if (acclevel > 0) {
                        localclaw = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                    }
                } else if (getJob().isA(MapleJob.BANDIT)) {
                    accpassive = SkillFactory.getSkill(4200000);
                    acclevel = getSkillLevel(accpassive);
                    if (acclevel > 0) {
                        localdagger = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                    }
                }
            }/* else if (getJob().isA(MapleJob.PIRATE)) {
                accpassive = SkillFactory.getSkill(5000000);
                acclevel = getSkillLevel(accpassive);
                if (acclevel > 0) {
                    accuracy += accpassive.getEffect(getSkillLevel(accpassive)).getX();
                }
                if (getJob().isA(MapleJob.BRAWLER)) {
                    accpassive = SkillFactory.getSkill(5100001);
                    acclevel = getSkillLevel(accpassive);
                    if (acclevel > 0) {
                        localknuckle = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                    }
                } else if (getJob().isA(MapleJob.GUNSLINGER)) {
                    accpassive = SkillFactory.getSkill(5200000);
                    acclevel = getSkillLevel(accpassive);
                    if (acclevel > 0) {
                        localgun = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                    }
                }
            } else if (getJob().isA(MapleJob.BOWMAN)) {
                accpassive = SkillFactory.getSkill(3000000);
                acclevel = getSkillLevel(accpassive);
                if (acclevel > 0) {
                    accuracy += accpassive.getEffect(getSkillLevel(accpassive)).getX();
                }
                if (getJob().isA(MapleJob.HUNTER)) {
                    accpassive = SkillFactory.getSkill(3100000);
                    acclevel = getSkillLevel(accpassive);
                    if (acclevel > 0) {
                        localbow = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                    }
                } else if (getJob().isA(MapleJob.CROSSBOWMAN)) {
                    accpassive = SkillFactory.getSkill(3200000);
                    acclevel = getSkillLevel(accpassive);
                    if (acclevel > 0) {
                        localcrossbow = accpassive.getEffect(getSkillLevel(accpassive)).getX();
                    }
                }
            }
            */
            if (weapon != null) switch (weapon) {
                case CROSSBOW:
                    accuracy += localcrossbow;
                    break;
                case BOW:
                    accuracy += localbow;
                    break;
                case DAGGER:
                    accuracy += localdagger;
                    break;
                case CLAW:
                    accuracy += localclaw;
                    break;
                case SPEAR:
                    accuracy += localspear;
                    break;
                case POLE_ARM:
                    accuracy += localpolearm;
                    break;
                case SWORD1H:
                    accuracy += localsword;
                    break;
                case SWORD2H:
                    accuracy += localsword;
                    break;
                case AXE1H:
                    accuracy += localaxe;
                    break;
                case BLUNT1H:
                    accuracy += localblunt;
                    break;
                case AXE2H:
                    accuracy += localaxe;
                    break;
                case BLUNT2H:
                    accuracy += localblunt;
                    break;
                case KNUCKLE:
                    accuracy += localknuckle;
                    break;
                case GUN:
                    accuracy += localgun;
                    break;
                default:
                    break;
            }
        }

        localMaxBaseDamage = calculateMaxBaseDamage();
        if (oldmaxhp != 0 && oldmaxhp != localMaxHp) updatePartyMemberHP();
        checkBerserk();
    }

    public boolean canLearnSkill(int skillId) {
        return canLearnSkill(SkillFactory.getSkill(skillId));
    }

    public boolean canLearnSkill(ISkill skill) {
        if (!skill.canBeLearnedBy(getJob())) return false;

        if (
            skill
                .getRequirements()
                .entrySet()
                .stream()
                .anyMatch(req ->
                    getSkillLevel(req.getKey()) < req.getValue()
                )
        ) {
            return false;
        }

        final int skillJob = skill.getId() / 10000;
        if (skillJob < getJob().getId() || skill.getId() < 1000000 || skill.getId() / 10000 % 100 == 0) {
            return true;
        }

        int spSpentInCurrJob =
            skills
                .entrySet()
                .stream()
                .filter(s -> s.getKey().getId() / 10000 == skillJob)
                .mapToInt(s -> s.getValue().skillevel)
                .sum();

        int possibleSpInCurrJob = 1;
        if (skillJob % 10 == 2) {
            possibleSpInCurrJob += 2;
        }
        int minLvlReqForJob;
        if (skillJob % 10 == 0) {
            minLvlReqForJob = 30;
        } else if (skillJob % 10 == 1) {
            minLvlReqForJob = 70;
        } else {
            minLvlReqForJob = 120;
        }
        possibleSpInCurrJob += (getLevel() - minLvlReqForJob) * 3;

        return spSpentInCurrJob < possibleSpInCurrJob;
    }

    public void addUnclaimedItem(int itemId) {
        addUnclaimedItem(itemId, (short) 1);
    }

    public void addUnclaimedItem(int itemId, short quantity) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleInventoryType type = ii.getInventoryType(itemId);

        IItem newItem;
        if (type.equals(MapleInventoryType.EQUIP)) {
            newItem = ii.getEquipById(itemId);
            for (short i = 0; i < quantity; ++i) {
                unclaimedItems.add(newItem);
            }
        } else {
            newItem = new Item(itemId, (byte) 0, quantity, -1);
            unclaimedItems.add(newItem);
        }
    }

    public void addUnclaimedItem(IItem item) {
        unclaimedItems.add(item);
    }

    public boolean claimUnclaimedItem() {
        if (!unclaimedItems.isEmpty()) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            IItem item = unclaimedItems.get(unclaimedItems.size() - 1);
            MapleInventoryType type = ii.getInventoryType(item.getItemId());
            if (type.equals(MapleInventoryType.EQUIP) && !ii.isThrowingStar(item.getItemId()) && !ii.isBullet(item.getItemId())) {
                if (!getInventory(type).isFull()) {
                    MapleInventoryManipulator.addFromDrop(getClient(), item, false);
                } else {
                    return false;
                }
            } else if (MapleInventoryManipulator.checkSpace(getClient(), item.getItemId(), item.getQuantity(), "")) {
                if (item.getItemId() >= 5000000 && item.getItemId() <= 5000100) {
                    if (item.getQuantity() > 1) {
                        item.setQuantity((short) 1);
                    }
                    int petId = MaplePet.createPet(item.getItemId());
                    MapleInventoryManipulator.addById(getClient(), item.getItemId(), (short) 1, null, petId);
                    getClient().getSession().write(MaplePacketCreator.getShowItemGain(item.getItemId(), item.getQuantity()));
                } else {
                    MapleInventoryManipulator.addById(getClient(), item.getItemId(), item.getQuantity());
                }
            } else {
                return false;
            }
            getClient().getSession().write(MaplePacketCreator.getShowItemGain(item.getItemId(), item.getQuantity(), true));
        } else {
            return false;
        }

        unclaimedItems.remove(unclaimedItems.size() - 1);
        return true;
    }

    public List<IItem> getUnclaimedItems() {
        return unclaimedItems;
    }

    public IItem getLastUnclaimedItem() {
        return unclaimedItems.get(unclaimedItems.size() - 1);
    }

    public boolean hasUnclaimedItems() {
        return !unclaimedItems.isEmpty();
    }

    public void Mount(int id, int skillid) {
        maplemount = new MapleMount(this, id, skillid);
    }

    public void equipChanged() {
        getMap().broadcastMessage(this, MaplePacketCreator.updateCharLook(this), false);
        recalcLocalStats();
        enforceMaxHpMp();
        if (getClient().getPlayer().getMessenger() != null) {
            WorldChannelInterface wci = ChannelServer.getInstance(getClient().getChannel()).getWorldInterface();
            try {
                wci.updateMessenger(getClient().getPlayer().getMessenger().getId(), getClient().getPlayer().getName(), getClient().getChannel());
            } catch (RemoteException e) {
                getClient().getChannelServer().reconnectWorld();
            }
        }
    }

    public MaplePet getPet(int index) {
        return pets[index];
    }

    public void addPet(MaplePet pet) {
        for (int i = 0; i < 3; ++i) {
            if (pets[i] == null) {
                pets[i] = pet;
                return;
            }
        }
    }

    private void removePet(MaplePet pet, boolean shift_left) {
        int slot = -1;
        for (int i = 0; i < 3; ++i) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == pet.getUniqueId()) {
                    pets[i] = null;
                    slot = i;
                    break;
                }
            }
        }
        if (shift_left) {
            if (slot > -1) {
                for (int i = slot; i < 3; ++i) {
                    if (i != 2) {
                        pets[i] = pets[i + 1];
                    } else {
                        pets[i] = null;
                    }
                }
            }
        }
    }

    public int getNoPets() {
        int ret = 0;
        for (int i = 0; i < 3; ++i) {
            if (pets[i] != null) {
                ret++;
            } else {
                break;
            }
        }
        return ret;
    }

    public int getPetIndex(MaplePet pet) {
        if (pet == null) {
            return -1;
        }
        for (int i = 0; i < 3; ++i) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == pet.getUniqueId()) {
                    return i;
                }
            } else {
                break;
            }
        }
        return -1;
    }

    public int getPetIndex(int petId) {
        for (int i = 0; i < 3; ++i) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == petId) {
                    return i;
                }
            } else {
                break;
            }
        }
        return -1;
    }

    public int getNextEmptyPetIndex() {
        for (int i = 0; i < 3; ++i) {
            if (pets[i] == null) {
                return i;
            }
        }
        return 3;
    }

    public MaplePet[] getPets() {
        return pets;
    }

    public void unequipAllPets() {
        for (int i = 0; i < 3; ++i) {
            if (pets[i] != null) {
                unequipPet(pets[i], true);
                cancelFullnessSchedule(i);
            } else {
                break;
            }
        }
    }

    public void unequipPet(MaplePet pet, boolean shift_left) {
        unequipPet(pet, shift_left, false);
    }

    private void unequipPet(MaplePet pet, boolean shift_left, boolean hunger) {
        cancelFullnessSchedule(getPetIndex(pet));
        for (int i = 0; i < 3; ++i) {
            if (pets[i] != null) {
                pets[i].saveToDb();
            }
        }
        getMap().broadcastMessage(this, MaplePacketCreator.showPet(this, pet, true, hunger), true);
        getClient().getSession().write(MaplePacketCreator.petStatUpdate(this));
        getClient().getSession().write(MaplePacketCreator.enableActions());
        removePet(pet, shift_left);
    }

    public void shiftPetsRight() {
        if (pets[2] == null) {
            pets[2] = pets[1];
            pets[1] = pets[0];
            pets[0] = null;
        }
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        if (lastFameTime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
            return FameStatus.NOT_TODAY;
        } else if (lastMonthFameIds.contains(from.getId())) {
            return FameStatus.NOT_THIS_MONTH;
        } else {
            return FameStatus.OK;
        }
    }

    public void hasGivenFame(MapleCharacter to) {
        lastFameTime = System.currentTimeMillis();
        lastMonthFameIds.add(to.getId());
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)");
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            sqlException(e);
        }
    }

    public MapleParty getParty() {
        return party;
    }

    public int getPartyId() {
        return (party != null ? party.getId() : -1);
    }

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public PartyQuest getPartyQuest() {
        return this.partyQuest;
    }

    public void setPartyQuest(PartyQuest pq) {
        partyQuest = pq;
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    private void clearDoors() {
        doors.clear();
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<>(doors);
    }

    public boolean canDoor() {
        return canDoor;
    }

    public void disableDoor() {
        canDoor = false;
        TimerManager.getInstance().schedule(() -> canDoor = true, 5000);
    }

    public Map<Integer, MapleSummon> getSummons() {
        return summons;
    }

    public void cleanNullSummons() {
        List<Integer> keys = new LinkedList<>();
        synchronized (summons) {
            for (Map.Entry<Integer, MapleSummon> e : summons.entrySet()) {
                if (e.getValue() == null) keys.add(e.getKey());
            }

            for (Integer i : keys) {
                summons.remove(i);
            }
        }
    }

    public int getChair() {
        return chair;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public void setChair(int chair) {
        this.chair = chair;
        if (this.chair > 0 && getMapId() / 100 == 9100000) { // Reading Chair
            setReadingTime((int) (System.currentTimeMillis() / 1000));

            TimerManager tMan = TimerManager.getInstance();
            readingTask = tMan.register(() -> {
                if (this.chair > 0 && getMapId() / 100 == 9100000) {
                    if (Math.random() < MapleCharacter.READING_PRIZE_PROP) {
                        int rewardId = getReadingReward();
                        if (rewardId == 0) {
                            dropMessage("It looks like you don't have a book to read! Talk to Saeko to get one.");
                        } else {
                            MapleInventoryType type = MapleInventoryType.ETC;
                            if (MapleInventoryManipulator.checkSpace(getClient(), rewardId, 1, "")) {
                                MapleInventoryManipulator.addById(getClient(), rewardId, (short) 1);
                                getClient().getSession().write(
                                    MaplePacketCreator.getShowItemGain(
                                        rewardId,
                                        (short) 1,
                                        true
                                    )
                                );
                                sendHint("Nice reading!\r\nYou've just gained a\r\n#bknowledge essence#k!");
                            } else {
                                dropMessage(
                                    1,
                                    "Your inventory is full. Please remove an item from your " +
                                        type.name().toLowerCase() +
                                        " inventory, and then type @mapleadmin into chat to claim the item."
                                );
                                addUnclaimedItem(rewardId, (short) 1);
                            }
                        }
                    }
                } else {
                    cancelReadingTask();
                }
            }, 60L * 1000L, 60L * 1000L);
        } else { // No longer sitting in Reading Chair
            setReadingTime(0);
            cancelReadingTask();
        }
    }

    public void cancelReadingTask() {
        if (readingTask != null) {
            readingTask.cancel(false);
        }
        readingTask = null;
    }

    public int getReadingReward() {
        int ret = 0;
        int rewardSet[] = {4031762, 4031755, 4031750, 4031764, 4031753, 4031756};
        int tomeSet[]   = {4031056, 4161002, 4031157, 4031158, 4031159, 4031900};
        for (int i = 0; i < tomeSet.length; ++i) {
            int tomeId = tomeSet[i];
            if (getItemQuantity(tomeId, false) > 0) ret = rewardSet[i];
        }
        return ret;
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    @Override
    public Collection<MapleInventory> allInventories() {
        return Arrays.asList(inventory);
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public int getGuildId() {
        return guildid;
    }

    public int getGuildRank() {
        return guildrank;
    }

    public void setGuildId(int _id) {
        guildid = _id;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
        }
    }

    public void setGuildRank(int _rank) {
        guildrank = _rank;
        if (mgc != null) mgc.setGuildRank(_rank);
    }

    public void setAllianceRank(int rank) {
        allianceRank = rank;
        if (mgc != null) mgc.setAllianceRank(rank);
    }

    public int getAllianceRank() {
        return this.allianceRank;
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    private void guildUpdate() {
        if (guildid <= 0) return;
        mgc.setLevel(level);
        mgc.setJobId(job.getId());
        try {
            client.getChannelServer().getWorldInterface().memberLevelJobUpdate(mgc);
            int allianceId = getGuild().getAllianceId();
            if (allianceId > 0) {
                client
                    .getChannelServer()
                    .getWorldInterface()
                    .allianceMessage(
                        allianceId,
                        MaplePacketCreator.updateAllianceJobLevel(this),
                        getId(),
                        -1
                    );
            }
        } catch (RemoteException e) {
            sqlException(e);
        }
    }
    private final NumberFormat nf = new DecimalFormat("#,###,###,###");

    public String guildCost() {
        return nf.format(MapleGuild.CREATE_GUILD_COST);
    }

    public String emblemCost() {
        return nf.format(MapleGuild.CHANGE_EMBLEM_COST);
    }

    public String capacityCost() {
        return nf.format(MapleGuild.INCREASE_CAPACITY_COST);
    }

    public void genericGuildMessage(int code) {
        this.client.getSession().write(MaplePacketCreator.genericGuildMessage((byte) code));
    }

    public void disbandGuild() {
        if (guildid <= 0 || guildrank != 1) return;
        try {
            client.getChannelServer().getWorldInterface().disbandGuild(this.guildid);
        } catch (RemoteException e) {
            client.getChannelServer().reconnectWorld();
            sqlException(e);
        }
    }

    public void increaseGuildCapacity() {
        if (guildid <= 0) return;
        if (getMeso() < MapleGuild.INCREASE_CAPACITY_COST) {
            client.getSession().write(MaplePacketCreator.serverNotice(1, "You do not have enough mesos."));
            return;
        }
        try {
            client.getChannelServer().getWorldInterface().increaseGuildCapacity(guildid);
        } catch (RemoteException e) {
            client.getChannelServer().reconnectWorld();
            sqlException(e);
            return;
        }
        gainMeso(-MapleGuild.INCREASE_CAPACITY_COST, true, false, true);
    }

    public void saveGuildStatus() {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps =
                con.prepareStatement(
                    "UPDATE characters SET guildid = ?, guildrank = ?, allianceRank = ? WHERE id = ?"
                );
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, allianceRank);
            ps.setInt(4, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            sqlException(e);
        }
    }

    public void modifyCSPoints(int type, int quantity) {
        switch (type) {
            case 1:
                this.paypalnx += quantity;
                break;
            case 2:
                this.maplepoints += quantity;
                break;
            case 4:
                this.cardnx += quantity;
                break;
        }
    }

    public int getCSPoints(int type) {
        switch (type) {
            case 1:
                return this.paypalnx;
            case 2:
                return this.maplepoints;
            case 4:
                return this.cardnx;
            default:
                return 0;
        }
    }

    public boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        int possesed = getItemQuantity(itemid, checkEquipped);
        if (greaterOrEquals) {
            return possesed >= quantity;
        } else {
            return possesed == quantity;
        }
    }

    private void registerMagicGuard() {
        cancelMagicGuardCancelTask();

        ISkill magicGuard = SkillFactory.getSkill(2001002);
        MapleStatEffect magicGuardEffect = magicGuard.getEffect(getSkillLevel(magicGuard));
        setMagicGuard(true);
        TimerManager tMan = TimerManager.getInstance();
        final Runnable cancelTask = () -> setMagicGuard(false);

        setMagicGuardCancelTask(tMan.schedule(cancelTask, magicGuardEffect.getDuration()));
    }

    private void cancelMagicGuardCancelTask() {
        if (getMagicGuardCancelTask() != null) {
            magicGuardCancelTask.cancel(false);
        }
    }

    private ScheduledFuture<?> getMagicGuardCancelTask() {
        return magicGuardCancelTask;
    }

    public void setMagicGuardCancelTask(ScheduledFuture<?> mgct) {
        this.magicGuardCancelTask = mgct;
    }

    public void setMagicGuard(boolean hmg) {
        this.hasMagicGuard = hmg;
    }

    public boolean hasMagicGuard() {
        return this.hasMagicGuard;
    }

    public void setForcedWarp(final MapleCharacter to, long delay) {
        cancelForcedWarp();
        forcedWarp = TimerManager.getInstance().schedule(() ->
            changeMap(
                to.getMap(),
                to.getMap().findClosestSpawnpoint(to.getPosition())
            ),
            delay
        );
    }

    public void setForcedWarp(final MapleMap map, long delay) {
        cancelForcedWarp();
        forcedWarp = TimerManager.getInstance().schedule(() -> changeMap(map, map.getPortal(0)), delay);
    }

    public void setForcedWarp(final MapleMap map, final MaplePortal portal, long delay) {
        cancelForcedWarp();
        forcedWarp = TimerManager.getInstance().schedule(() -> changeMap(map, portal), delay);
    }

    public void setForcedWarp(final int mapId, long delay) {
        cancelForcedWarp();
        forcedWarp = TimerManager.getInstance().schedule(() -> changeMap(mapId), delay);
    }

    public void setForcedWarp(final int mapId, long delay, final Predicate<MapleCharacter> predicate) {
        cancelForcedWarp();
        forcedWarp = TimerManager.getInstance().schedule(() -> {
            if (predicate.test(this)) {
                changeMap(mapId);
            }
        }, delay);
    }

    public boolean hasForcedWarp() {
        return forcedWarp != null;
    }

    public void cancelForcedWarp() {
        if (forcedWarp != null) {
            forcedWarp.cancel(false);
            forcedWarp = null;
        }
    }

    public void addBuyBack(IItem item, short quantity) {
        if (buyBacks.size() > 100) {
            buyBacks.remove(buyBacks.keySet().iterator().next());
        }
        buyBacks.put(item, quantity);
    }

    public Map<IItem, Short> readBuyBacks() {
        return Collections.unmodifiableMap(buyBacks);
    }

    public boolean removeBuyBack(IItem item) {
        return buyBacks.remove(item) != null;
    }

    public boolean removeBuyBack(final int itemId) {
        IItem item =
            buyBacks
                .keySet()
                .stream()
                .filter(i -> i.getItemId() == itemId)
                .findAny()
                .orElse(null);
        return item != null && buyBacks.remove(item) != null;
    }

    public Pair<IItem, Short> getBuyBack(final int itemId) {
        Map.Entry<IItem, Short> bb =
            buyBacks
                .entrySet()
                .stream()
                .filter(b -> b.getKey().getItemId() == itemId)
                .findAny()
                .orElse(null);
        return bb != null ? new Pair<>(bb.getKey(), bb.getValue()) : null;
    }

    public void clearBuyBacks() {
        buyBacks.clear();
    }

    public void setBossHpTask(long repeatTime, long duration) {
        cancelBossHpTask();
        TimerManager tMan = TimerManager.getInstance();
        final DecimalFormat df = new DecimalFormat("#.00");
        bossHpTask = tMan.register(() ->
            getMap().getMapObjectsInRange(
                new Point(),
                Double.POSITIVE_INFINITY,
                MapleMapObjectType.MONSTER
            )
            .stream()
            .map(mmo -> (MapleMonster) mmo)
            .filter(MapleMonster::isBoss)
            .forEach(mob -> {
                double hpPercentage = (double) mob.getHp() / ((double) mob.getMaxHp()) * 100.0d;
                dropMessage("Monster: " + mob.getName() + ", HP: " + df.format(hpPercentage) + "%");
            }), repeatTime);

        bossHpCancelTask = tMan.schedule(() -> bossHpTask.cancel(false), duration);
    }

    public boolean hasBossHpTask() {
        return bossHpTask != null;
    }

    public boolean cancelBossHpTask() {
        boolean didCancel = false;
        if (bossHpTask != null) {
            bossHpTask.cancel(false);
            didCancel = true;
        }
        if (bossHpCancelTask != null) {
            bossHpCancelTask.cancel(false);
        }
        bossHpTask = null;
        bossHpCancelTask = null;
        return didCancel;
    }

    public void toggleDpm() {
        showDpm = !showDpm;
    }

    public boolean doShowDpm() {
        return showDpm;
    }

    public void absorbDamage(float dmg, int damageFrom) {
        if (isHidden() || !isAlive()) return;
        boolean belowLevelLimit = getMap().getPartyQuestInstance() != null &&
                                  getMap().getPartyQuestInstance().getLevelLimit() > getLevel();
        int damage = (int) (dmg * getDamageScale());
        int removedDamage = 0;
        boolean dodge = false;
        boolean advaita = false;

        if (belowLevelLimit) damage = 1;
        if (isInvincible()) {
            removedDamage += damage;
            damage = 0;
        }

        if (getTotalWdef() > 1999 && damageFrom == -1) {
            int oldDamage = damage;
            damage = Math.max(damage - (getTotalWdef() - 1999), 1);

            double dodgeChance = (Math.log1p(getTotalWdef() - 1999) / Math.log(2.0d)) / 25.0d;
            if (Math.random() < dodgeChance) {
                damage = 0;
                dodge = true;
            }
            removedDamage += oldDamage - damage;
        }

        if (getTotalMdef() > 1999 && (damageFrom == 0 || damageFrom == 1)) {
            int oldDamage = damage;
            damage = (int) Math.max(damage - Math.pow(getTotalMdef() - 1999.0d, 1.2d), 1);
            removedDamage += oldDamage - damage;
        }

        int advaitaLevel = getSkillLevel(5121004);
        if (advaitaLevel > 10 && (damageFrom == 0 || damageFrom == 1)) {
            double advaitaChance = (double) (advaitaLevel - 10) / 100.0d;
            int oldDamage = damage;
            if (Math.random() < advaitaChance) {
                damage = 0;
                advaita = true;
            }
            removedDamage += oldDamage - damage;
        }

        if (getBuffedValue(MapleBuffStat.MORPH) != null) cancelMorphs();

        if (damage > 1 && getSkillLevel(5100000) > 0) {
            int mpLoss;
            if (getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
                mpLoss = (int) (damage * (getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0d));
            } else {
                mpLoss = (int) (damage * ((2.0d * getSkillLevel(5100000) + 5.0d) / 100.0d));
            }
            int hpLoss = damage - mpLoss;
            int hypotheticalMpLoss = 0;
            if (getBuffedValue(MapleBuffStat.INFINITY) != null) {
                hypotheticalMpLoss = mpLoss;
                mpLoss = 0;
            }
            if (mpLoss > getMp()) {
                hpLoss += mpLoss - getMp();
                mpLoss = getMp();
            }
            addMP(-mpLoss);
            damage = hpLoss;
            if (hypotheticalMpLoss > 0) {
                mpLoss = hypotheticalMpLoss;
            }
            removedDamage += mpLoss;
        }

        if (damage > 0) addHP(-damage);

        if (getTrueDamage()) {
            sendHint(
                "Absorbed: #e" +
                    (dodge ? "MISS! " : (advaita ? "Advaita: " : "")) +
                    "#r" +
                    damage +
                    "#k#n" +
                    (removedDamage > 0 ? " #e#b(" + removedDamage + ")#k#n" : ""),
                0,
                0
            );
        }
    }

    public void setLastSamsara(long time) {
        lastSamsara = time;
    }

    public long getLastSamsara() {
        return lastSamsara;
    }

    public boolean canSamsara() {
        return System.currentTimeMillis() - getLastSamsara() >= SAMSARA_COOLDOWN;
    }

    public void dropDailyPrizeTime(boolean checkOtherChars) {
        if (getLastDailyPrize() == null || getLastDailyPrize().getTime() <= 1) {
            dropMessage("IT LOOKS LIKE YOU HAVEN'T STARTED YOUR DAILY PRIZES. TALK TO T-1337 IN THE FM!");
            return;
        }

        boolean canGetDailyPrizes = true;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement charps = con.prepareStatement("SELECT id FROM characters WHERE accountid = ?");
            charps.setInt(1, getAccountID());
            ResultSet charrs = charps.executeQuery();
            while (charrs.next()) {
                int charid = charrs.getInt("id");
                if (charid == getId()) {
                    continue;
                }
                PreparedStatement invps = con.prepareStatement(
                    "SELECT itemid FROM inventoryitems WHERE characterid = ?"
                );
                invps.setInt(1, charid);
                ResultSet invrs = invps.executeQuery();
                while (invrs.next()) {
                    int itemid = invrs.getInt("itemid");
                    if (itemid >= 3990010 && itemid <= 3990016) {
                        canGetDailyPrizes = false;
                        break;
                    }
                }
                invrs.close();
                invps.close();
            }
            charrs.close();
            charps.close();
        } catch (SQLException e) {
            e.printStackTrace();
            dropMessage("THERE WAS AN ERROR GETTING YOUR DAILY PRIZE INFO.");
            return;
        }
        if (!canGetDailyPrizes) {
            if (checkOtherChars) {
                dropMessage("YOU MAY ONLY PARTICIPATE IN DAILY PRIZES WITH ONE CHARACTER AT A TIME.");
            }
            return;
        }

        int dailyPrizeStatus;
        Calendar validTime = Calendar.getInstance();
        validTime.setTime(getLastDailyPrize());
        validTime.add(Calendar.DATE, 1);
        Calendar currenttime = Calendar.getInstance();
        currenttime.setTimeInMillis(System.currentTimeMillis());
        if (validTime.compareTo(currenttime) > 0) {
            // Valid time is in the future
            if (
                validTime.get(Calendar.DAY_OF_MONTH) == currenttime.get(Calendar.DAY_OF_MONTH) &&
                validTime.get(Calendar.MONTH) == currenttime.get(Calendar.MONTH)
            ) {
                dailyPrizeStatus = 0;
            } else {
                dailyPrizeStatus = 1;
            }
        } else if (validTime.compareTo(currenttime) < 0) {
            // Valid time is in the past
            if (
                validTime.get(Calendar.DAY_OF_MONTH) == currenttime.get(Calendar.DAY_OF_MONTH) &&
                validTime.get(Calendar.MONTH) == currenttime.get(Calendar.MONTH)
            ) {
                dailyPrizeStatus = 0;
            } else {
                dailyPrizeStatus = -1;
            }
        } else {
            dailyPrizeStatus = 0;
        }

        if (dailyPrizeStatus == 1) {
            Calendar validtime = Calendar.getInstance();
            validtime.setTime(getLastDailyPrize());
            validtime.add(Calendar.DATE, 1);
            validtime.set(Calendar.HOUR_OF_DAY, 0);
            validtime.set(Calendar.MINUTE, 0);
            validtime.set(Calendar.SECOND, 0);
            validtime.set(Calendar.MILLISECOND, 0);
            long time = validtime.getTimeInMillis() - System.currentTimeMillis();

            long hours = time / 3600000L;
            time %= 3600000L;
            long minutes = time / 60000L;
            time %= 60000L;
            long seconds = time / 1000L;

            dropMessage(
                "YOU MUST WAIT ANOTHER " +
                    hours +
                    " HOURS, " +
                    minutes +
                    " MINUTES, AND " +
                    seconds +
                    " SECONDS TO GET YOUR NEXT PRIZE."
            );
        } else if (dailyPrizeStatus == -1) {
            dropMessage("BUMMER. LOOKS LIKE YOU WAITED TOO LONG TO GET YOUR NEXT PRIZE, HUMAN! GO BACK TO T-1337!");
        } else {
            dropMessage("LOOKS LIKE YOU'RE READY TO GET YOUR NEXT PRIZE FROM T-1337!");
        }
    }

    private static class MapleBuffStatValueHolder {
        public final MapleStatEffect effect;
        public final long startTime;
        public int value;
        public final ScheduledFuture<?> schedule;
        public MapleBuffStatValueHolder(MapleStatEffect effect, long startTime, ScheduledFuture<?> schedule, int value) {
            super();
            this.effect = effect;
            this.startTime = startTime;
            this.schedule = schedule;
            this.value = value;
        }
    }

    public static class MapleCoolDownValueHolder {
        public final int skillId;
        public final long startTime;
        public final long length;
        public final ScheduledFuture<?> timer;

        public MapleCoolDownValueHolder(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
            super();
            this.skillId = skillId;
            this.startTime = startTime;
            this.length = length;
            this.timer = timer;
        }
    }

    public static class SkillEntry {
        public final int skillevel;
        public final int masterlevel;
        public SkillEntry(int skillevel, int masterlevel) {
            this.skillevel = skillevel;
            this.masterlevel = masterlevel;
        }
        @Override
        public String toString() {
            return skillevel + ":" + masterlevel;
        }
    }

    public enum FameStatus {
        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public int getBuddyCapacity() {
        return buddylist.getCapacity();
    }

    public void setBuddyCapacity(int capacity) {
        buddylist.setCapacity(capacity);
        client.getSession().write(MaplePacketCreator.updateBuddyCapacity(capacity));
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public void checkMessenger() {
        if (messenger != null && messengerposition < 4 && messengerposition > -1) {
            try {
                WorldChannelInterface wci = ChannelServer.getInstance(client.getChannel()).getWorldInterface();
                MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(client.getPlayer(), messengerposition);
                wci.silentJoinMessenger(messenger.getId(), messengerplayer, messengerposition);
                wci.updateMessenger(getClient().getPlayer().getMessenger().getId(), getClient().getPlayer().getName(), getClient().getChannel());
            } catch (RemoteException e) {
                client.getChannelServer().reconnectWorld();
            }
        }
    }

    public int getMessengerPosition() {
        return messengerposition;
    }

    public void setMessengerPosition(int position) {
        this.messengerposition = position;
    }

    public int hasEXPCard() {
        return 1;
    }

    public void setInCS(boolean yesno) {
        this.incs = yesno;
    }

    public boolean inCS() {
        return this.incs;
    }

    public void setInMTS(boolean yesno) {
        this.inmts = yesno;
    }

    public boolean inMTS() {
        return this.inmts;
    }

    public void addCooldown(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
        addCooldown(skillId, startTime, length, timer, false);
    }

    private void addCooldown(int skillId, long startTime, long length, ScheduledFuture<?> timer, boolean sendpacket) {
        if (coolDowns.containsKey(skillId)) {
            coolDowns.remove(skillId);
        }
        coolDowns.put(skillId, new MapleCoolDownValueHolder(skillId, startTime, length, timer));
        if (sendpacket) {
            client.getSession().write(MaplePacketCreator.skillCooldown(skillId, (int) length / 1000));
        }
    }

    public void removeCooldown(int skillId) {
        if (coolDowns.containsKey(skillId)) {
            coolDowns.remove(skillId);
            client.getSession().write(MaplePacketCreator.skillCooldown(skillId, 0));
        }
    }

    public boolean skillIsCooling(int skillId) {
        return this.coolDowns.containsKey(skillId);
    }

    public void giveCoolDowns(final List<PlayerCoolDownValueHolder> cooldowns) {
        for (PlayerCoolDownValueHolder cooldown : cooldowns) {
            int time = (int) ((cooldown.length + cooldown.startTime) - System.currentTimeMillis());
            ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, cooldown.skillId), time);
            addCooldown(cooldown.skillId, System.currentTimeMillis(), time, timer);
        }
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        giveCoolDowns(skillid, starttime, length, false);
    }

    public void giveCoolDowns(final int skillid, long starttime, long length, boolean sendpacket) {
        int time = (int) ((length + starttime) - System.currentTimeMillis());
        ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, skillid), time);
        addCooldown(skillid, System.currentTimeMillis(), time, timer, sendpacket);
    }

    public List<PlayerCoolDownValueHolder> getAllCooldowns() {
        List<PlayerCoolDownValueHolder> ret = new ArrayList<>();
        for (MapleCoolDownValueHolder mcdvh : coolDowns.values()) {
            ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
        }
        return ret;
    }

    public static class CancelCooldownAction implements Runnable {
        private final int skillId;
        private final WeakReference<MapleCharacter> target;
        public CancelCooldownAction(MapleCharacter target, int skillId) {
            this.target = new WeakReference<>(target);
            this.skillId = skillId;
        }

        @Override
        public void run() {
            MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.removeCooldown(skillId);
            }
        }
    }

    public void giveDebuff(MapleDisease disease, MobSkill mobSkill) {
        int stillMindLevel = getSkillLevel(5110000);
        if (stillMindLevel > 0) {
            double stillMindProp = (double) ((stillMindLevel * getTotalInt() - 500) / 250) / 100.0d;
            if (Math.random() < stillMindProp) {
                if (getTrueDamage()) {
                    String diseaseName = disease.toString().toLowerCase();
                    sendHint("#bStill Mind: Negated #e" + diseaseName + "#n#k");
                }
                return;
            }
        }

        synchronized (diseases) {
            if (isAlive() && !isActiveBuffedValue(2321005) && !diseases.contains(disease) && diseases.size() < 2) {
                diseases.add(disease);
                List<Pair<MapleDisease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, mobSkill.getX()));
                long mask = 0;
                for (Pair<MapleDisease, Integer> statup : debuff) {
                    mask |= statup.getLeft().getValue();
                }
                getClient().getSession().write(MaplePacketCreator.giveDebuff(mask, debuff, mobSkill));
                if (disease != MapleDisease.POISON) {
                    getMap().broadcastMessage(this, MaplePacketCreator.giveForeignDebuff(id, mask, mobSkill), false);
                }
                if (isAlive()) {
                    final MapleCharacter character = this;
                    final MapleDisease disease_ = disease;
                    TimerManager.getInstance().schedule(() -> {
                        if (character.diseases.contains(disease_)) {
                            dispelDebuff(disease_);
                        }
                    }, mobSkill.getDuration());
                }
            }
        }
    }

    public void forciblyGiveDebuff(MapleDisease disease, MobSkill mobSkill) {
        synchronized (diseases) {
            if (isAlive() && !isActiveBuffedValue(2321005) && !diseases.contains(disease) && diseases.size() < 2) {
                diseases.add(disease);
                List<Pair<MapleDisease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, mobSkill.getX()));
                long mask = 0;
                for (Pair<MapleDisease, Integer> statup : debuff) {
                    mask |= statup.getLeft().getValue();
                }
                getClient().getSession().write(MaplePacketCreator.giveDebuff(mask, debuff, mobSkill));
                if (disease != MapleDisease.POISON) {
                    getMap().broadcastMessage(this, MaplePacketCreator.giveForeignDebuff(id, mask, mobSkill), false);
                }
                if (isAlive()) {
                    final MapleCharacter character = this;
                    final MapleDisease disease_ = disease;
                    TimerManager.getInstance().schedule(() -> {
                        if (character.diseases.contains(disease_)) {
                            dispelDebuff(disease_);
                        }
                    }, mobSkill.getDuration());
                }
            }
        }
    }

    public void dispelDebuff(MapleDisease debuff) {
        if (diseases.contains(debuff)) {
            diseases.remove(debuff);
            long mask = debuff.getValue();
            getClient().getSession().write(MaplePacketCreator.cancelDebuff(mask));
            getMap().broadcastMessage(this, MaplePacketCreator.cancelForeignDebuff(id, mask), false);
        }
    }

    public MapleCharacter getPartner() {
        return
            ChannelServer
                .getAllInstances()
                .stream()
                .map(cs -> cs.getPlayerStorage().getCharacterById(getPartnerId()))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    public void dispelDebuffs() {
        MapleDisease[] disease =
        {
            MapleDisease.POISON, MapleDisease.SLOW,
            MapleDisease.SEAL,   MapleDisease.DARKNESS,
            MapleDisease.WEAKEN, MapleDisease.CURSE,
            MapleDisease.SEDUCE
        };
        for (int i = 0; i < disease.length; ++i) {
            dispelDebuff(disease[i]);
        }
        /*
        MapleDisease[] disease =
        {
            MapleDisease.POISON, MapleDisease.SLOW,
            MapleDisease.SEAL,   MapleDisease.DARKNESS,
            MapleDisease.WEAKEN, MapleDisease.CURSE,
            MapleDisease.SEDUCE
        };
        for (int i = 0; i < diseases.size(); ++i) {
            if (diseases.contains(disease[i])) {
                diseases.remove(disease[i]);
                long mask = 0;
                for (MapleDisease statup : diseases) {
                    mask |= statup.getValue();
                }
                getClient().getSession().write(MaplePacketCreator.cancelDebuff(mask));
                getMap().broadcastMessage(this, MaplePacketCreator.cancelForeignDebuff(id, mask), false);
            }
        }
        */
    }

    public void setLevel(int level) {
        if (level <= 0) level = 1;
        this.level = level;
    }

    private void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public List<Integer> getQuestItemsToShow() {
        Set<Integer> delta = new LinkedHashSet<>();
        for (Map.Entry<MapleQuest, MapleQuestStatus> questEntry : quests.entrySet()) {
            if (questEntry.getValue().getStatus() != MapleQuestStatus.Status.STARTED) {
                delta.addAll(questEntry.getKey().getQuestItemsToShowOnlyIfQuestIsActivated());
            }
        }
        List<Integer> returnThis = new ArrayList<>(delta);
        return Collections.unmodifiableList(returnThis);
    }

    public void sendNote(int to, String msg) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps =
            con.prepareStatement(
                "INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)"
            );
        ps.setInt(1, to);
        ps.setString(2, getName());
        ps.setString(3, msg);
        ps.setLong(4, System.currentTimeMillis());
        ps.executeUpdate();
        ps.close();
    }

    public void showNote() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps =
            con.prepareStatement(
                "SELECT * FROM notes WHERE `to` = ?",
                ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE
            );
        ps.setInt(1, getId());
        ResultSet rs = ps.executeQuery();
        rs.last();
        int count = rs.getRow();
        rs.first();
        client.getSession().write(MaplePacketCreator.showNotes(rs, count));
        ps.close();
    }

    public void giveDebuff(int debuff, int level) {
        giveDebuff(debuff, level, -1L);
    }

    public void giveDebuff(int debuff, int level, long time) {
        final MobSkill ms = MobSkillFactory.getMobSkill(debuff, level);
        MapleDisease disease;
        switch (debuff) {
            case 120:
                disease = MapleDisease.SEAL;
                break;
            case 121:
                disease = MapleDisease.DARKNESS;
                break;
            case 122:
                disease = MapleDisease.WEAKEN;
                break;
            case 123:
                disease = MapleDisease.STUN;
                break;
            case 124:
                disease = MapleDisease.CURSE;
                break;
            case 125:
                disease = MapleDisease.POISON;
                break;
            case 126:
                disease = MapleDisease.SLOW;
                break;
            case 128:
                disease = MapleDisease.SEDUCE;
                break;
            default:
                System.err.println(
                    "Failed to apply debuff of skill ID " +
                        debuff +
                        " and skill level " +
                        level +
                        " to player " +
                        getName() +
                        ". Function: MapleCharacter#giveDebuff"
                );
                return;
        }
        if (time > 0L) {
            ms.setDuration(time);
        }
        giveDebuff(disease, ms);
    }

    public void forciblyGiveDebuff(int debuff, int level) {
        forciblyGiveDebuff(debuff, level, -1L);
    }

    public void forciblyGiveDebuff(int debuff, int level, long time) {
        final MobSkill ms = MobSkillFactory.getMobSkill(debuff, level);
        MapleDisease disease;
        switch (debuff) {
            case 120:
                disease = MapleDisease.SEAL;
                break;
            case 121:
                disease = MapleDisease.DARKNESS;
                break;
            case 122:
                disease = MapleDisease.WEAKEN;
                break;
            case 123:
                disease = MapleDisease.STUN;
                break;
            case 124:
                disease = MapleDisease.CURSE;
                break;
            case 125:
                disease = MapleDisease.POISON;
                break;
            case 126:
                disease = MapleDisease.SLOW;
                break;
            case 128:
                disease = MapleDisease.SEDUCE;
                break;
            default:
                System.err.println(
                    "Failed to apply debuff of skill ID " +
                        debuff +
                        " and skill level " +
                        level +
                        " to player " +
                        getName() +
                        ". Method: MapleCharacter#giveDebuff"
                );
                return;
        }
        if (time > 0L) {
            ms.setDuration(time);
        }
        forciblyGiveDebuff(disease, ms);
    }

    public boolean isMarried() {
        return married;
    }

    public void setMarried(boolean status) {
        married = status;
    }

    public int getMarriageQuestLevel() {
        return marriageQuestLevel;
    }

    public void setMarriageQuestLevel(int nf) {
        marriageQuestLevel = nf;
    }

    public void addMarriageQuestLevel() {
        marriageQuestLevel++;
    }

    public void subtractMarriageQuestLevel() {
        marriageQuestLevel -= 1;
    }

    public void setZakumLevel(int level) {
        zakumLvl = level;
    }

    public int getZakumLevel() {
        return zakumLvl;
    }

    public void addZakumLevel() {
        zakumLvl += 1;
    }

    public void subtractZakumLevel() {
        zakumLvl -= 1;
    }

    public void setPartnerId(int pem) {
        partnerid = pem;
    }

    public int getPartnerId() {
        return partnerid;
    }

    public void checkBerserk() {
        if (BerserkSchedule != null) {
            BerserkSchedule.cancel(false);
        }
        final MapleCharacter chr = this;
        ISkill BerserkX = SkillFactory.getSkill(1320006);
        final int skilllevel = getSkillLevel(BerserkX);
        if (chr.getJob().equals(MapleJob.DARKKNIGHT) && skilllevel >= 1) {
            MapleStatEffect ampStat = BerserkX.getEffect(skilllevel);
            int x = ampStat.getX();
            int HP = chr.getHp();
            int MHP = chr.getMaxHp();
            int ratio = HP * 100 / MHP;
            Berserk = ratio <= x;
            BerserkSchedule = TimerManager.getInstance().register(() -> {
                getClient().getSession().write(MaplePacketCreator.showOwnBerserk(skilllevel, Berserk));
                getMap().broadcastMessage(
                    this,
                    MaplePacketCreator.showBerserk(
                        getId(),
                        skilllevel,
                        Berserk
                    ),
                    false
                );
            }, 5L * 1000L, 3L * 1000L);
        }
    }

    private void prepareBeholderEffect() {
        if (beholderHealingSchedule != null) {
            beholderHealingSchedule.cancel(false);
        }
        if (beholderBuffSchedule != null) {
            beholderBuffSchedule.cancel(false);
        }

        ISkill bHealing = SkillFactory.getSkill(1320008);
        if (getSkillLevel(bHealing) > 0) {
            final MapleStatEffect healEffect = bHealing.getEffect(getSkillLevel(bHealing));
            beholderHealingSchedule = TimerManager.getInstance().register(() -> {
                addHP(healEffect.getHp());
                getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(1321007, 2));
                getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.summonSkill(getId(), 1321007, 5), true);
                getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBuffeffect(getId(), 1321007, 2, (byte) 3), false);
            }, healEffect.getX() * 1000, healEffect.getX() * 1000);
        }
        ISkill bBuffing = SkillFactory.getSkill(1320009);
        if (getSkillLevel(bBuffing) > 0) {
            final MapleStatEffect buffEffect = bBuffing.getEffect(getSkillLevel(bBuffing));
            beholderBuffSchedule = TimerManager.getInstance().register(() -> {
                buffEffect.applyTo(MapleCharacter.this);
                getClient().getSession().write(MaplePacketCreator.beholderAnimation(getId(), 1320009));
                getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.summonSkill(getId(), 1321007, (int) (Math.random() * 3) + 6), true);
                getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBuffeffect(getId(), 1321007, 2, (byte) 3), false);
            }, buffEffect.getX() * 1000, buffEffect.getX() * 1000);
        }
    }

    public void setChalkboard(String text) {
        if (interaction != null) {
            return;
        }
        this.chalktext = text;
        for (FakeCharacter ch : fakes) {
            ch.getFakeChar().setChalkboard(text);
        }
        if (chalktext == null) {
            getMap().broadcastMessage(MaplePacketCreator.useChalkboard(this, true));
        } else {
            getMap().broadcastMessage(MaplePacketCreator.useChalkboard(this, false));
        }
    }

    public String getChalkboard() {
        return this.chalktext;
    }

    public void setDefaultKeyMap() {
        keymap.clear();
        int[] num1 = {2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 23, 25, 26, 27, 29, 31, 34, 35, 37, 38, 40, 41, 43, 44, 45, 46, 48, 50, 56, 57, 59, 60, 61, 62, 63, 64, 65};
        int[] num2 = {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 5, 5, 6, 6, 6, 6, 6, 6, 6};
        int[] num3 = {10, 12, 13, 18, 24, 21, 8, 5, 0, 4, 1, 19, 14, 15, 52, 2, 17, 11, 3, 20, 16, 23, 9, 50, 51, 6, 22, 7, 53, 54, 100, 101, 102, 103, 104, 105, 106};
        for (int i = 0; i < num1.length; ++i) {
            keymap.put(num1[i], new MapleKeyBinding(num2[i], num3[i]));
        }
        sendKeymap();
    }

    public void setReborns(int amt) {
        reborns = amt;
    }

    public int getReborns() {
        return reborns;
    }

    public void doReborn() {
        unequipEverything();
        setReborns(getReborns() + 1);
        setLevel(1);
        setHp(1000);
        setMp(1000);
        setMaxHp(1000);
        setMaxMp(1000);
        setExp(0);
        setRemainingSp(0);
        setJob(MapleJob.BEGINNER);
        updateSingleStat(MapleStat.LEVEL, 1);
        updateSingleStat(MapleStat.HP, 1000);
        updateSingleStat(MapleStat.MP, 1000);
        updateSingleStat(MapleStat.MAXHP, 1000);
        updateSingleStat(MapleStat.MAXMP, 1000);
        updateSingleStat(MapleStat.EXP, 0);
        updateSingleStat(MapleStat.AVAILABLESP, 0);
        updateSingleStat(MapleStat.JOB, 0);
        MaplePacket packet =
            MaplePacketCreator.serverNotice(
                6,
                "[Congrats] (" +
                    getReborns() +
                    ") " +
                    getName() +
                    " has reached rebirth! Congratulations, " +
                    getName() +
                    ", on your achievement!"
            );
        try {
            getClient().getChannelServer().getWorldInterface().broadcastMessage(getName(), packet.getBytes());
        } catch (RemoteException e) {
            getClient().getChannelServer().reconnectWorld();
        }
    }

    public void setPvpDeaths(int amount) {
        pvpdeaths = amount;
    }

    public void setPvpKills(int amount) {
        pvpkills = amount;
    }

    public void gainPvpDeath() {
        pvpdeaths += 1;
    }

    public void gainPvpKill() {
        pvpkills += 1;
    }

    public boolean getCanSmega() {
        return canSmega;
    }

    public void setCanSmega(boolean yn) {
        canSmega = yn;
    }

    public boolean getSmegaEnabled() {
        return smegaEnabled;
    }

    public void setSmegaEnabled(boolean yn) {
        smegaEnabled = yn;
    }

    public boolean getCanTalk() {
        return canTalk;
    }

    public boolean canTalk(boolean yn) {
        return canTalk = yn;
    }

    public int getPvpKills() {
        return pvpkills;
    }

    public int getPvpDeaths() {
        return pvpdeaths;
    }

    public int getAbsoluteXp() {
        int absLevelMultiplier = Math.max(getLevel() / 10, pastLifeExp);
        int currentExpBonus = getExpBonus() ? getExpBonusMulti() : 1;
        if (absLevelMultiplier >= 1) {
            return absLevelMultiplier * currentExpBonus;
        } else {
            return currentExpBonus;
        }
    }

    public double getRelativeXp(int monsterLevel) {
        double relativeLevelMultiplier;
        if (getLevel() < 70) {
            if (monsterLevel - getLevel() >= 0) {
                relativeLevelMultiplier = 1.0d + 0.1d * (double) (monsterLevel - getLevel());
            } else {
                relativeLevelMultiplier = 1.0d + 0.07d * (double) (monsterLevel - getLevel());
            }
        } else if (getLevel() >= 70 && getLevel() < 95) {
            if (monsterLevel - getLevel() >= 0) {
                relativeLevelMultiplier = 1.0d + 0.1d * (double) (monsterLevel - getLevel());
            } else {
                relativeLevelMultiplier = 1.0d + 0.05d * (double) (monsterLevel - getLevel());
            }
        } else if (getLevel() >= 95 && getLevel() < 110) {
            if (monsterLevel - getLevel() >= 0) {
                relativeLevelMultiplier = 1.0d + 0.1d * (double) (monsterLevel - getLevel());
            } else {
                relativeLevelMultiplier = 1.0d + 0.03d * (double) (monsterLevel - getLevel());
            }
        } else {
            if (monsterLevel - getLevel() >= 0) {
                relativeLevelMultiplier = 1.0d + 0.1d * (double) (monsterLevel - getLevel());
            } else {
                relativeLevelMultiplier = 1.0d;
            }
        }
        if (relativeLevelMultiplier < 0.0d) {
            relativeLevelMultiplier = 0.0d;
        }
        return relativeLevelMultiplier;
    }

    public double getTotalMonsterXp(int monsterlevel) {
        return (double) getClient().getChannelServer().getExpRate() *
               (double) getAbsoluteXp() * getRelativeXp(monsterlevel);
    }

    public float getDamageScale() {
        float scale;
        if (getLevel() < 30) {
            scale = 2.8f;
        } else if (getLevel() < 40) {
            scale = 2.6f;
        } else if (getLevel() < 50) {
            scale = 2.4f;
        } else if (getLevel() < 60) {
            scale = 2.2f;
        } else if (getLevel() < 70) {
            scale = 2.0f;
        } else if (getLevel() < 80) {
            scale = 1.8f;
        } else if (getLevel() < 90) {
            scale = 1.6f;
        } else if (getLevel() < 100) {
            scale = 1.4f;
        } else if (getLevel() < 110) {
            scale = 1.2f;
        } else {
            scale = 1.0f;
        }
        if (getQuestEffectiveLevel() > 0 && getQuestEffectiveLevel() < getLevel()) {
            scale *= 1.0f + (float) (getLevel() - getQuestEffectiveLevel()) / 80.0f;
        }
        return scale;
    }

    public MapleGuild getGuild() {
        try {
            return getClient().getChannelServer().getWorldInterface().getGuild(getGuildId(), null);
        } catch (RemoteException e) {
            client.getChannelServer().reconnectWorld();
        }
        return null;
    }

    public void gainGP(int amount) {
        getGuild().gainGP(amount);
    }

    public void addBuddyCapacity(int capacity) {
        buddylist.addCapacity(capacity);
        client.getSession().write(MaplePacketCreator.updateBuddyCapacity(getBuddyCapacity()));
    }

    public void maxSkillLevel(int skillid) {
        int maxlevel = SkillFactory.getSkill(skillid).getMaxLevel();
        changeSkillLevel(SkillFactory.getSkill(skillid), maxlevel, maxlevel);
    }

    public void resetSkillLevel(int skillid) {
        changeSkillLevel(SkillFactory.getSkill(skillid), 0, 0);
    }

    public void maxAllSkills() {
        for (int s : SKILL_IDS) {
            maxSkillLevel(s);
        }
        if (isGM()) {
            int[] skillgm =
            {
                9001000, 9001001, 9001002, 9101000,
                9101001, 9101002, 9101003, 9101004,
                9101005, 9101006, 9101007, 9101008
            };
            for (int s : skillgm) {
                maxSkillLevel(s);
            }
        }
    }

    public void resetAllSkills() {
        for (int s : SKILL_IDS) {
            resetSkillLevel(s);
        }
        if (isGM()) {
            int[] skillgm =
            {
                9001000, 9001001, 9001002, 9101000,
                9101001, 9101002, 9101003, 9101004,
                9101005, 9101006, 9101007, 9101008
            };
            for (int s : skillgm) {
                resetSkillLevel(s);
            }
        }
    }

    public void unequipEverything() {
        MapleInventory equipped = getInventory(MapleInventoryType.EQUIPPED);
        List<Byte> position = new ArrayList<>();

        MapleInventory equip = getInventory(MapleInventoryType.EQUIP);

        if (equip.getSlotLimit() - equip.getSize() < equipped.getSize()) {
            int numtodelete = equipped.getSize() - (equip.getSlotLimit() - equip.getSize());
            byte[] equipslots = new byte[equip.list().size()];
            int i = 0;
            for (IItem equipitem : equip.list()) {
                equipslots[i] = equipitem.getPosition();
                i++;
            }
            Arrays.sort(equipslots);
            for (int j = equipslots.length - 1; j > equipslots.length - 1 - numtodelete; --j) {
                MapleInventoryManipulator.removeFromSlot(
                    getClient(),
                    MapleItemInformationProvider
                        .getInstance()
                        .getInventoryType(equip.getItem(equipslots[j]).getItemId()),
                    equipslots[j],
                    (short) 1,
                    false
                );
            }
        }

        for (IItem item : equipped.list()) {
            position.add(item.getPosition());
        }
        for (byte pos : position) {
            MapleInventoryManipulator.unequip(
                client,
                pos,
                getInventory(MapleInventoryType.EQUIP).getNextFreeSlot()
            );
        }
    }

    private void setOffOnline(boolean online) {
        try {
            WorldChannelInterface wci = client.getChannelServer().getWorldInterface();
            if (online) {
                wci.loggedOn(getName(), getId(), client.getChannel(), getBuddylist().getBuddyIds());
            } else {
                wci.loggedOff(getName(), getId(), client.getChannel(), getBuddylist().getBuddyIds());
            }
        } catch (RemoteException e) {
            client.getChannelServer().reconnectWorld();
        }
    }

    public static boolean unban(String name) {
        try {
            int accountid = -1;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) accountid = rs.getInt("accountid");
            ps.close();
            rs.close();
            if (accountid == -1) return false;
            ps = con.prepareStatement("UPDATE accounts SET banned = -1 WHERE id = ?");
            ps.setInt(1, accountid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            sqlException(e);
            return false;
        }
        return true;
    }

    public void dropMessage(String message) {
        dropMessage(6, message);
    }

    public void dropMessage(int type, String message) {
        client.getSession().write(MaplePacketCreator.serverNotice(type, message));
    }

    public void setClan(int num) {
        clan = num;
    }

    public int getClan() {
        return clan;
    }

    public void setBombPoints(int bombpoints) {
        this.bombpoints = bombpoints;
    }

    public void setJob(int job) {
        if (isfake) {
            this.job = MapleJob.getById(job);
        } else {
            changeJob(MapleJob.getById(job), false);
        }
    }

    public void setFake() {
        isfake = true;
    }

    private void setJob(MapleJob job) {
        changeJob(job, false);
    }

    public int isDonator() {
        return donatePoints;
    }

    public void setDonator(int set) {
        donatePoints = set;
    }

    public int getBombPoints() {
        return bombpoints;
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setInventory(MapleInventoryType type, MapleInventory inv) {
        inventory[type.ordinal()] = inv;
    }

    public boolean hasFakeChar() {
        return !fakes.isEmpty();
    }

    public List<FakeCharacter> getFakeChars() {
        return fakes;
    }

    public void setGMText(int text) {
        gmtext = text;
    }

    public int getGMText() {
        return gmtext;
    }

    public void setExp(int set) {
        exp.set(set);
        if (exp.get() < 0) exp.set(0);
    }

    public void giveItemBuff(int itemID) {
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        MapleStatEffect statEffect = mii.getItemEffect(itemID);
        statEffect.applyTo(this);
    }

    public void cancelAllDebuffs() {
        for (int i = 0; i < diseases.size(); ++i) {
            diseases.remove(i);
            long mask = 0L;
            for (MapleDisease statup : diseases) {
                mask |= statup.getValue();
            }
            getClient().getSession().write(MaplePacketCreator.cancelDebuff(mask));
            getMap().broadcastMessage(this, MaplePacketCreator.cancelForeignDebuff(id, mask), false);
        }
    }

    public List<MapleDisease> getDiseases() {
        return diseases;
    }

    @SuppressWarnings("unchecked")
    public void removeJobSkills() {
        HashMap<Integer, MapleKeyBinding> keymapCloned = (HashMap<Integer, MapleKeyBinding>) keymap.clone();
        for (Integer keys : keymapCloned.keySet()) {
            if (SkillFactory.getSkillName(keys) != null) {
                if (keymapCloned.get(keys).getAction() >= 1000000) {
                    keymap.remove(keys);
                }
            }
        }
        sendKeymap();
    }

    public void changePage(int page) {
        this.currentPage = page;
    }

    public void changeTab(int tab) {
        this.currentTab = tab;
    }

    public void changeType(int type) {
        this.currentType = type;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getCurrentTab() {
        return currentTab;
    }

    public int getCurrentType() {
        return currentType;
    }

    public void handleEnergyChargeGain() {
        handleEnergyChargeGain(1.0d);
    }

    public void handleEnergyChargeGain(double multiplier) {
        ISkill energyCharge = SkillFactory.getSkill(5110001);
        int energyChargeSkillLevel = getSkillLevel(energyCharge);
        MapleStatEffect ceffect = energyCharge.getEffect(energyChargeSkillLevel);
        int gain = rand((int) ceffect.getProp(), (int) ceffect.getProp() * 2);
        if (multiplier != 1.0d) gain = (int) (multiplier * (double) gain);
        if (energybar < 10000) {
            energybar += gain;
            if (energybar > 10000) energybar = 10000;
            getClient().getSession().write(MaplePacketCreator.giveEnergyCharge(energybar));
        }/*else {
            TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    getClient().getSession().write(MaplePacketCreator.giveEnergyCharge(0));
                    energybar = 0;
                }
            }, ceffect.getDuration());
        }*/
    }

    public int getEnergyBar() {
        return energybar;
    }

    public void setEnergyBar(int set) {
        energybar = set;
    }

    public long getAfkTime() {
        return afkTime;
    }

    public void resetAfkTime() {
        if (chalktext != null && chalktext.equals("I'm afk! Drop me a message <3")) {
            setChalkboard(null);
        }
        afkTime = System.currentTimeMillis();
    }

    public void setClient(MapleClient c) {
        client = c;
    }

    public boolean isFake() {
        return this.isfake;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public int getRingRequested() {
        return this.ringRequest;
    }

    public void setRingRequested(int set) {
        ringRequest = set;
    }

    public boolean hasMerchant() {
        return hasMerchant;
    }

    public boolean tempHasItems() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps =
                con.prepareStatement(
                    "SELECT ownerid FROM hiredmerchanttemp WHERE ownerid = ?"
                );
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                rs.close();
                ps.close();
                return true;
            }
            rs.close();
            ps.close();
        } catch (SQLException sqle) {
            sqlException(sqle);
        }
        return false;
    }

    public void setHasMerchant(boolean set) {
        setHasMerchant(set, true);
    }

    public void setHasMerchant(boolean set, boolean doSql) {
        if (doSql) {
            try {
                PreparedStatement ps =
                    DatabaseConnection
                        .getConnection()
                        .prepareStatement(
                            "UPDATE characters SET HasMerchant = ? WHERE id = ?"
                        );
                ps.setInt(1, set ? 1 : 0);
                ps.setInt(2, getId());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException sqle) {
                sqlException(sqle);
            }
        }
        hasMerchant = set;
    }

    public List<Integer> getVIPRockMaps(int type) {
        List<Integer> rockmaps = new ArrayList<>();
        try {
            PreparedStatement ps =
                DatabaseConnection
                    .getConnection()
                    .prepareStatement(
                        "SELECT mapid FROM viprockmaps WHERE cid = ? AND type = ?"
                    );
            ps.setInt(1, id);
            ps.setInt(2, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rockmaps.add(rs.getInt("mapid"));
            }
            rs.close();
            ps.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return null;
        }
        return rockmaps;
    }

    public void leaveParty() {
        WorldChannelInterface wci = ChannelServer.getInstance(getClient().getChannel()).getWorldInterface();
        MaplePartyCharacter partyplayer = new MaplePartyCharacter(this);
        if (party != null) {
            try {
                if (partyplayer.equals(party.getLeader())) {
                    wci.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                    if (getEventInstance() != null) {
                        getEventInstance().disbandParty();
                    }
                    if (getPartyQuest() != null) {
                        getPartyQuest().disbandParty();
                    }
                } else {
                    wci.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                    if (getEventInstance() != null) {
                        getEventInstance().leftParty(this);
                    }
                    if (getPartyQuest() != null) {
                        getPartyQuest().leftParty(this);
                    }
                }
            } catch (RemoteException re) {
                getClient().getChannelServer().reconnectWorld();
            }
            setParty(null);
        }
    }
}
