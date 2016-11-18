package net.sf.odinms.server;

import net.sf.odinms.client.*;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.PlayerCoolDownValueHolder;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.*;
import net.sf.odinms.tools.ArrayMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;

import java.awt.*;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class MapleStatEffect implements Serializable {
    static final long serialVersionUID = 9179541993413738569L;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MapleStatEffect.class);
    private short watk, matk, wdef, mdef, acc, avoid, hands, speed, jump;
    private short hp, mp;
    private double hpR, mpR;
    private short mpCon, hpCon;
    private int duration;
    private boolean overTime;
    private int sourceid;
    private int moveTo;
    private boolean skill;
    private List<Pair<MapleBuffStat, Integer>> statups;
    private Map<MonsterStatus, Integer> monsterStatus;
    private int x, y, z;
    private double prop;
    private int iProp;
    private int itemCon, itemConNo;
    private int damage, attackCount, bulletCount, bulletConsume;
    private Point lt, rb;
    private int mobCount;
    private int moneyCon;
    private int cooldown;
    private boolean isMorph = false;
    private int morphId = 0;
    private List<MapleDisease> cureDebuffs;
    private int mastery, range;

    public MapleStatEffect() {
    }
    
    private MapleStatEffect(MapleStatEffect mse) {
        this.watk = mse.getWatk();
        this.matk = mse.getMatk();
        this.wdef = mse.getWdef();
        this.mdef = mse.getMdef();
        this.acc = mse.getAcc();
        this.avoid = mse.getAvoid();
        this.hands = mse.getHands();
        this.speed = mse.getSpeed();
        this.jump = mse.getJump();
        this.hp = mse.getHp();
        this.mp = mse.getMp();
        this.hpR = mse.getHpR();
        this.mpR = mse.getMpR();
        this.mpCon = mse.getMpCon();
        this.hpCon = mse.getHpCon();
        this.duration = mse.getDuration();
        this.overTime = mse.isOverTime();
        this.sourceid = mse.getSourceId();
        this.moveTo = mse.getMoveTo();
        this.skill = mse.isSkill();
        this.statups = new ArrayList<>(mse.getStatups());
        this.monsterStatus = new HashMap<>(mse.getMonsterStati());
        this.x = mse.getX();
        this.y = mse.getY();
        this.z = mse.getZ();
        this.prop = mse.getProp();
        this.iProp = mse.getIProp();
        this.itemCon = mse.getItemCon();
        this.itemConNo = mse.getItemConNo();
        this.damage = mse.getDamage();
        this.attackCount = mse.getAttackCount();
        this.bulletCount = mse.getBulletCount();
        this.bulletConsume = mse.getBulletConsume();
        this.lt = mse.getLt();
        this.rb = mse.getRb();
        this.mobCount = mse.getMobCount();
        this.moneyCon = mse.getMoneyCon();
        this.cooldown = mse.getCooldown();
        this.isMorph = mse.isMorph();
        this.morphId = mse.getMorph();
        this.cureDebuffs = mse.getCureDebuffs();
        this.mastery = mse.getMastery();
        this.range = mse.getRange();
    }

    public static MapleStatEffect loadSkillEffectFromData(MapleData source, int skillid, boolean overtime) {
        return loadFromData(source, skillid, true, overtime);
    }

    public static MapleStatEffect loadItemEffectFromData(MapleData source, int itemid) {
        return loadFromData(source, itemid, false, false);
    }

    private static void addBuffStatPairToListIfNotZero(List<Pair<MapleBuffStat, Integer>> list, MapleBuffStat buffstat, Integer val) {
        if (val != 0) {
            list.add(new Pair<>(buffstat, val));
        }
    }

    private static MapleStatEffect loadFromData(MapleData source, int sourceid, boolean skill, boolean overTime) {
        MapleStatEffect ret = new MapleStatEffect();
        ret.duration = MapleDataTool.getIntConvert("time", source, -1);
        ret.hp = (short) MapleDataTool.getInt("hp", source, 0);
        ret.hpR = MapleDataTool.getInt("hpR", source, 0) / 100.0;
        ret.mp = (short) MapleDataTool.getInt("mp", source, 0);
        ret.mpR = MapleDataTool.getInt("mpR", source, 0) / 100.0;
        ret.mpCon = (short) MapleDataTool.getInt("mpCon", source, 0);
        ret.hpCon = (short) MapleDataTool.getInt("hpCon", source, 0);
        ret.iProp = MapleDataTool.getInt("prop", source, 100);
        ret.prop = ret.iProp / 100.0;
        ret.mobCount = MapleDataTool.getInt("mobCount", source, 1);
        ret.cooldown = MapleDataTool.getInt("cooltime", source, 0);
        ret.morphId = MapleDataTool.getInt("morph", source, 0);
        ret.isMorph = ret.morphId > 0;

        ret.sourceid = sourceid;
        ret.skill = skill;
        
        //
        if (ret.skill && ret.sourceid == 1201006) {
            ret.mobCount *= 2; // Doubling the number of mobs potentially affected by Threaten
        }
        //
        
        if (!ret.skill && ret.duration > -1) {
            ret.overTime = true;
        } else {
            ret.duration *= 1000; // Items have their times stored in ms, of course
            ret.overTime = overTime;
        }
        ArrayList<Pair<MapleBuffStat, Integer>> statups = new ArrayList<>();

        ret.watk = (short) MapleDataTool.getInt("pad", source, 0);
        ret.wdef = (short) MapleDataTool.getInt("pdd", source, 0);
        ret.matk = (short) MapleDataTool.getInt("mad", source, 0);
        ret.mdef = (short) MapleDataTool.getInt("mdd", source, 0);
        ret.acc = (short) MapleDataTool.getIntConvert("acc", source, 0);
        ret.avoid = (short) MapleDataTool.getInt("eva", source, 0);
        ret.speed = (short) MapleDataTool.getInt("speed", source, 0);
        ret.jump = (short) MapleDataTool.getInt("jump", source, 0);
        if (ret.overTime && ret.getSummonMovementType() == null) {
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WATK, (int) ret.watk);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WDEF, (int) ret.wdef);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MATK, (int) ret.matk);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MDEF, (int) ret.mdef);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.ACC, (int) ret.acc);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.AVOID, (int) ret.avoid);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.SPEED, (int) ret.speed);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.JUMP, (int) ret.jump);
        }

        MapleData ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.lt = (Point) ltd.getData();
            ret.rb = (Point) source.getChildByPath("rb").getData();
        }
        int x = MapleDataTool.getInt("x", source, 0);
        ret.x = x;
        ret.y = MapleDataTool.getInt("y", source, 0);
        ret.z = MapleDataTool.getInt("z", source, 0);
        ret.damage = MapleDataTool.getIntConvert("damage", source, 100);
        ret.attackCount = MapleDataTool.getIntConvert("attackCount", source, 1);
        ret.bulletCount = MapleDataTool.getIntConvert("bulletCount", source, 1);
        ret.bulletConsume = MapleDataTool.getIntConvert("bulletConsume", source, 0);
        ret.moneyCon = MapleDataTool.getIntConvert("moneyCon", source, 0);
        ret.mastery = MapleDataTool.getIntConvert("mastery", source, 0);
        ret.range = MapleDataTool.getIntConvert("range", source, 0);
        ret.itemCon = MapleDataTool.getInt("itemCon", source, 0);
        ret.itemConNo = MapleDataTool.getInt("itemConNo", source, 0);
        ret.moveTo = MapleDataTool.getInt("moveTo", source, -1);
        
        if (ret.sourceid == 2321006) { // Making Resurrection consume 1 Elan Vital
            ret.itemCon = 4031485;
            ret.itemConNo = 1;
        }

        List<MapleDisease> localCureDebuffs = new ArrayList<>();
        if (MapleDataTool.getInt("poison", source, 0) > 0) {
            localCureDebuffs.add(MapleDisease.POISON);
        }
        if (MapleDataTool.getInt("seal", source, 0) > 0) {
            localCureDebuffs.add(MapleDisease.SEAL);
        }
        if (MapleDataTool.getInt("darkness", source, 0) > 0) {
            localCureDebuffs.add(MapleDisease.DARKNESS);
        }
        if (MapleDataTool.getInt("weakness", source, 0) > 0) {
            localCureDebuffs.add(MapleDisease.WEAKEN);
        }
        if (MapleDataTool.getInt("curse", source, 0) > 0) {
            localCureDebuffs.add(MapleDisease.CURSE);
        }
        ret.cureDebuffs = localCureDebuffs;

        Map<MonsterStatus, Integer> monsterStatus = new ArrayMap<>();

        if (skill) {
            switch (sourceid) {
                case 2001002: // Magic Guard
                    statups.add(new Pair<>(MapleBuffStat.MAGIC_GUARD, x));
                    break;
                case 2301003: // Invincible
                    //
                    statups.add(new Pair<>(MapleBuffStat.BOOSTER, -2));
                    //
                    statups.add(new Pair<>(MapleBuffStat.INVINCIBLE, x));
                    break;
                case 9101004: // Hide
                    ret.duration = 60 * 120 * 1000;
                    ret.overTime = true;
                case 4001003: // Dark Sight
                    statups.add(new Pair<>(MapleBuffStat.DARKSIGHT, x));
                    break;
                case 4211003: // Pickpocket
                    statups.add(new Pair<>(MapleBuffStat.PICKPOCKET, x));
                    break;
                case 4211005: // Meso Guard
                    statups.add(new Pair<>(MapleBuffStat.MESOGUARD, x));
                    break;
                case 4111001: // Meso Up
                    statups.add(new Pair<>(MapleBuffStat.MESOUP, x));
                    break;
                case 4111002: // Shadow Partner
                    statups.add(new Pair<>(MapleBuffStat.SHADOWPARTNER, x));
                    break;
                case 3101004: // Soul Arrow
                case 3201004:
                case 2311002: // Mystic Door
                    statups.add(new Pair<>(MapleBuffStat.SOULARROW, x));
                    break;
                case 1211003:
                case 1211004:
                case 1211005:
                case 1211006: // White Knight Charges
                case 1211007:
                case 1211008:
                case 1221003:
                case 1221004:
                    statups.add(new Pair<>(MapleBuffStat.WK_CHARGE, x));
                    break;
                case 1101004:
                case 1101005: // Booster
                case 1201004:
                case 1201005:
                case 1301004:
                case 1301005:
                case 2111005: // Spell Booster; do these work the same?
                case 2211005:
                case 3101002:
                case 3201002:
                case 4101003:
                case 4201002:
                case 5101006:
                case 5201003:
                //case 5121009: // Speed Infusion (was 5221010)
                    statups.add(new Pair<>(MapleBuffStat.BOOSTER, x));
                    break;
                case 5121009:
                    statups.add(new Pair<>(MapleBuffStat.SPEED_INFUSION, -4));
                    break;
                case 5111005: // Transformation
                case 5121003: // Super Transformation
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                    statups.add(new Pair<>(MapleBuffStat.MDEF, (int) ret.mdef));
                    statups.add(new Pair<>(MapleBuffStat.SPEED, (int) ret.speed));
                    statups.add(new Pair<>(MapleBuffStat.JUMP, (int) ret.jump));
                    break;
                case 1101006: // Rage
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                case 1121010: // Enrage
                    statups.add(new Pair<>(MapleBuffStat.WATK, (int) ret.watk));
                    break;
                case 1301006: // Iron Will
                    statups.add(new Pair<>(MapleBuffStat.MDEF, (int) ret.mdef));
                case 1001003: // Iron Body
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                    break;
                case 2001003: // Magic Armor
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                    //
                    statups.add(new Pair<>(MapleBuffStat.MDEF, (int) ret.wdef));
                    //
                    break;
                case 2101001: // Meditation
                case 2201001: // Meditation
                    statups.add(new Pair<>(MapleBuffStat.MATK, (int) ret.matk));
                    break;
                case 4101004: // Haste
                case 4201003: // Haste
                case 9101001: // GM Haste
                    statups.add(new Pair<>(MapleBuffStat.SPEED, (int) ret.speed));
                    statups.add(new Pair<>(MapleBuffStat.JUMP, (int) ret.jump));
                    break;
                case 2301004: // Bless
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                    statups.add(new Pair<>(MapleBuffStat.MDEF, (int) ret.mdef));
                case 3001003: // Focus
                    statups.add(new Pair<>(MapleBuffStat.ACC, (int) ret.acc));
                    statups.add(new Pair<>(MapleBuffStat.AVOID, (int) ret.avoid));
                    break;
                case 9101003: // GM Bless
                    statups.add(new Pair<>(MapleBuffStat.MATK, (int) ret.matk));
                case 3121008: // Concentrate
                    statups.add(new Pair<>(MapleBuffStat.WATK, (int) ret.watk));
                    break;
                case 5001005: // Dash
                    statups.add(new Pair<>(MapleBuffStat.DASH, 1));
                    break;
                case 1101007: // Power Guard
                case 1201007:
                    statups.add(new Pair<>(MapleBuffStat.POWERGUARD, x));
                    break;
                case 1301007:
                case 9101008:
                    statups.add(new Pair<>(MapleBuffStat.HYPERBODYHP, x));
                    statups.add(new Pair<>(MapleBuffStat.HYPERBODYMP, ret.y));
                    break;
                case 1001:    // Recovery
                    statups.add(new Pair<>(MapleBuffStat.RECOVERY, x));
                    break;
                case 1111002: // Combo
                    statups.add(new Pair<>(MapleBuffStat.COMBO, 1));
                    break;
                case 1004:    // Monster Riding
                    statups.add(new Pair<>(MapleBuffStat.MONSTER_RIDING, 1));
                    break;
                case 5221006: // Battleship
                    statups.add(new Pair<>(MapleBuffStat.MONSTER_RIDING, 1));
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                    statups.add(new Pair<>(MapleBuffStat.MDEF, (int) ret.mdef));
                    break;
                case 1311006: // Dragon Roar
                    ret.hpR = -x / 100.0;
                    break;
                case 1311008: // Dragon Blood
                    statups.add(new Pair<>(MapleBuffStat.DRAGONBLOOD, ret.x));
                    break;
                case 1121000: // Maple Warrior (all 4th job classes)
                case 1221000:
                case 1321000:
                case 2121000:
                case 2221000:
                case 2321000:
                case 3121000:
                case 3221000:
                case 4121000:
                case 4221000:
                case 5121000:
                case 5221000:
                    statups.add(new Pair<>(MapleBuffStat.MAPLE_WARRIOR, ret.x));
                    break;
                case 3121002: // Sharp Eyes (Bowmaster)
                case 3221002: // Sharp Eyes (Marksman)
                    // Hack much (TODO: is the order correct?)
                    statups.add(new Pair<>(MapleBuffStat.SHARP_EYES, ret.x << 8 | ret.y));
                    break;
                case 1321007: // Beholder
                case 2221005: // Ifrit
                case 2311006: // Summon Dragon
                case 2321003: // Bahamut
                case 3121006: // Phoenix
                case 5211001: // Octopus
                case 5211002: // Gaviota
                case 5220002: // Wrath of the Octopi
                    statups.add(new Pair<>(MapleBuffStat.SUMMON, 1));
                    break;
                case 2311003: // Holy Symbol
                case 9101002: // Holy Symbol (GM)
                    statups.add(new Pair<>(MapleBuffStat.WATK, 1)); // This gets multiplied later
                    statups.add(new Pair<>(MapleBuffStat.HOLY_SYMBOL, x));
                    break;
                case 4121006: // Shadow Claw
                    statups.add(new Pair<>(MapleBuffStat.SHADOW_CLAW, 0));
                    break;
                case 2121004:
                case 2221004:
                case 2321004: // Infinity
                    statups.add(new Pair<>(MapleBuffStat.INFINITY, x));
                    break;
                case 1121002:
                case 1221002:
                case 1321002: // Stance
                    statups.add(new Pair<>(MapleBuffStat.STANCE, ret.iProp));
                    break;
                case 1005: // Echo of Hero
                    statups.add(new Pair<>(MapleBuffStat.ECHO_OF_HERO, ret.x));
                    break;
                case 2121002: // Mana Reflection
                case 2221002:
                case 2321002: // Mana Reflection (Battle Priest)
                    statups.add(new Pair<>(MapleBuffStat.MANA_REFLECTION, 1));
                    break;
                case 2321005: // Holy Shield
                    statups.add(new Pair<>(MapleBuffStat.HOLY_SHIELD, x));
                    break;
                case 3111002: // Puppet (Ranger)
                case 3211002: // Puppet (Sniper)
                    statups.add(new Pair<>(MapleBuffStat.PUPPET, 1));
                    break;

                // ----------------------------- MONSTER STATUS PUT! ----------------------------- //

                case 4001002: // Disorder
                    monsterStatus.put(MonsterStatus.WATK, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.y);
                    break;
                case 1201006: // Threaten
                    monsterStatus.put(MonsterStatus.WATK, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.y);
                    monsterStatus.put(MonsterStatus.MATK, ret.x);
                    break;
                case 1111005: // Coma: Sword
                case 1111006: // Coma: Axe
                case 1111008: // Shout
                case 1211002: // Charged Blow
                case 3101005: // Arrow Bomb
                case 4211002: // Assaulter
                case 4221007: // Boomerang Step
                case 5101002: // Backspin Blow
                case 5101003: // Double Uppercut
                case 5121004: // Demolition
                case 5121005: // Snatch
                case 5121007: // Barrage
                case 5201004: // Blank Shot
                    monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 4121003:
                case 4221003:
                    monsterStatus.put(MonsterStatus.SHOWDOWN, 1);
                    break;
                case 2201004: // Cold Beam
                case 2211002: // Ice Strike
                case 2211006: // I/L Elemental Composition
                case 2221007: // Blizzard
                case 3211003: // Blizzard
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    //ret.duration *= 2; // Freezing skills are a little strange
                    break;
                case 5211005: // Ice Splitter
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 2221003: // Ice Demon
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    int tempduration = ret.duration;
                    ret.duration = ret.x * 1000;
                    ret.x = tempduration;
                    break;
                case 2121006: // Paralyze
                case 2101003: // F/P Slow
                case 2201003: // I/L Slow
                    monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case 2101005: // Poison Breath
                case 2111006: // F/P Elemental Composition
                    monsterStatus.put(MonsterStatus.POISON, 1);
                    break;
                case 2311005:
                    monsterStatus.put(MonsterStatus.DOOM, 1);
                    break;
                case 3111005: // Golden Hawk
                case 3211005: // Golden Eagle
                    statups.add(new Pair<>(MapleBuffStat.SUMMON, 1));
                    monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 2121005: // Elquines
                case 3221005: // Frostprey
                    statups.add(new Pair<>(MapleBuffStat.SUMMON, 1));
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 2111004: // F/P Seal
                case 2211004: // I/L Seal
                    monsterStatus.put(MonsterStatus.SEAL, 1);
                    break;
                case 4111003: // Shadow Web
                    monsterStatus.put(MonsterStatus.SHADOW_WEB, 1);
                    break;
                case 3121007: // Hamstring
                    statups.add(new Pair<>(MapleBuffStat.HAMSTRING, x));
                    monsterStatus.put(MonsterStatus.SPEED, x);
                    break;
                case 3221006: // Blind
                    statups.add(new Pair<>(MapleBuffStat.BLIND, x));
                    monsterStatus.put(MonsterStatus.ACC, x);
                    break;
                default:
                    break;
            }
        }

        if (ret.isMorph()) {
            statups.add(new Pair<>(MapleBuffStat.MORPH, ret.getMorph()));
        }

        ret.monsterStatus = monsterStatus;

        statups.trimToSize();
        ret.statups = statups;

        return ret;
    }
    
    public void applyPassive(MapleCharacter applyto, MapleMapObject obj) {
        if (makeChanceResult()) {
            switch (sourceid) {
                // MP eater
                case 2100000:
                case 2200000:
                case 2300000:
                    if (obj == null || obj.getType() != MapleMapObjectType.MONSTER) {
                        return;
                    }
                    MapleMonster mob = (MapleMonster) obj;
                    // x is absorb percentage
                    if (!mob.isBoss()) {
                        int absorbMp = Math.min((int) (mob.getMaxMp() * (getX() / 100.0)), mob.getMp());
                        if (absorbMp > 0) {
                            mob.setMp(mob.getMp() - absorbMp);
                            applyto.addMP(absorbMp);
                            applyto.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 1));
                            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showBuffeffect(applyto.getId(), sourceid, 1, (byte) 3), false);
                        }
                    }
                    break;
            }
        }
    }

    public boolean applyTo(MapleCharacter chr) {
        return applyTo(chr, chr, true, null);
    }

    public boolean applyTo(MapleCharacter chr, Point pos) {
        return applyTo(chr, chr, true, pos);
    }

    private boolean applyTo(MapleCharacter applyFrom, MapleCharacter applyTo, boolean primary, Point pos) {
        int hpChange = calcHPChange(applyFrom, primary);
        int mpChange = calcMPChange(applyFrom, primary);

        if (primary) {
            if (itemConNo != 0) {
                if (sourceid != 2321006) {
                    MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemCon);
                    MapleInventoryManipulator.removeById(applyTo.getClient(), type, itemCon, itemConNo, false, true);
                } else {
                    MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemCon);
                    if (applyFrom.getItemQuantity(itemCon, false) >= itemConNo) {
                        MapleInventoryManipulator.removeById(applyTo.getClient(), type, itemCon, itemConNo, false, true);
                    } else {
                        applyFrom.dropMessage("You do not have enough Elans Vital to use the Resurrection skill.");
                        applyFrom.removeCooldown(sourceid);
                        applyFrom.addMPHP(0, mpCon);
                        applyFrom.getClient().getSession().write(MaplePacketCreator.enableActions());
                        return false;
                    }
                }
            }
            if (sourceid == 5101005) {
                hpChange = (int) ((double) applyFrom.getMaxHp() * (-x / 100.0));
                if (-hpChange >= applyTo.getHp()) {
                    applyFrom.dropMessage(5, "You do not have enough HP to use the MP Recovery skill.");
                    applyFrom.removeCooldown(sourceid);
                    applyFrom.getClient().getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }
                mpChange = (int) ((double) applyFrom.getMaxMp() * (y / 100.0));
            }
        }
        if (!cureDebuffs.isEmpty()) {
            cureDebuffs.forEach(applyFrom::dispelDebuff);
        }
        List<Pair<MapleStat, Integer>> hpMpUpdate = new ArrayList<>(2);
        if (!primary && isResurrection()) {
            hpChange = applyTo.getMaxHp();
            applyTo.setStance(0); // TODO: Fix death bug, player doesn't spawn on other screen
        }
        if (isDispel() && makeChanceResult()) {
            MonsterStatus[] remove = {MonsterStatus.ACC, MonsterStatus.AVOID, MonsterStatus.WEAPON_IMMUNITY, MonsterStatus.MAGIC_IMMUNITY, MonsterStatus.SPEED};
            for (MapleMapObject _mob : applyFrom.getMap().getMapObjectsInRange(applyFrom.getPosition(), 30000 + applyFrom.getSkillLevel(SkillFactory.getSkill(sourceid)) * 1000, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                MapleMonster mob = (MapleMonster) _mob;
                if (mob != null && mob.isAlive() && !mob.getMonsterBuffs().isEmpty()) {
                    for (int i = 0; i < remove.length; ++i) {
                        if (mob.getMonsterBuffs().contains(remove[i])) {
                            mob.getMonsterBuffs().remove(remove[i]);
                            MaplePacket packet = MaplePacketCreator.cancelMonsterStatus(mob.getObjectId(), Collections.singletonMap(remove[i], 1));
                            mob.getMap().broadcastMessage(packet, mob.getPosition());
                            if (mob.getController() != null && !mob.getController().isMapObjectVisible(mob)) {
                                mob.getController().getClient().getSession().write(packet);
                            }
                        }
                    }
                }
            }
            applyTo.dispelDebuffs();
        }
        if (isCrash() && makeChanceResult()) {
            MonsterStatus remove;
            switch (sourceid) {
                case 1311007: // Power Crash
                    remove = MonsterStatus.WEAPON_ATTACK_UP;
                    break;
                case 1211009: // Magic Crash
                    remove = MonsterStatus.MAGIC_DEFENSE_UP;
                    break;
                case 1111007: // Armor Crash
                    remove = MonsterStatus.WEAPON_DEFENSE_UP;
                    break;
                default:      // ???!!!
                    remove = MonsterStatus.STUN;
                    break;
            }
            for (MapleMapObject _mob : applyFrom.getMap().getMapObjectsInRange(applyFrom.getPosition(), 30000 + applyFrom.getSkillLevel(SkillFactory.getSkill(sourceid)) * 3000, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                MapleMonster mob = (MapleMonster) _mob;
                if (mob != null && mob.isAlive() && !mob.getMonsterBuffs().isEmpty()) {
                    if (mob.getMonsterBuffs().contains(remove)) {
                        mob.getMonsterBuffs().remove(remove);
                        MaplePacket packet = MaplePacketCreator.cancelMonsterStatus(mob.getObjectId(), Collections.singletonMap(remove, 1));
                        mob.getMap().broadcastMessage(packet, mob.getPosition());
                        if (mob.getController() != null && !mob.getController().isMapObjectVisible(mob)) {
                            mob.getController().getClient().getSession().write(packet);
                        }
                    }
                }
            }
        }
        if (isHeroWill()) {
            //applyTo.dispelDebuff(MapleDisease.SEDUCE);
            applyTo.dispelDebuffs();
        }
        if (hpChange != 0) {
            if (hpChange < 0 && (-hpChange) > applyTo.getHp()) {
                if (applyFrom != null) {
                    applyFrom.getClient().getSession().write(MaplePacketCreator.enableActions());
                }
                return false;
            }
            int newHp = applyTo.getHp() + hpChange;
            if (newHp < 1) {
                newHp = 1;
            }
            applyTo.setHp(newHp);
            hpMpUpdate.add(new Pair<>(MapleStat.HP, applyTo.getHp()));
        }
        if (mpChange != 0) {
            if (mpChange < 0 && (-mpChange) > applyTo.getMp()) {
                if (applyFrom != null) {
                    applyFrom.getClient().getSession().write(MaplePacketCreator.enableActions());
                }
                return false;
            }
            applyTo.setMp(applyTo.getMp() + mpChange);
            hpMpUpdate.add(new Pair<>(MapleStat.MP, applyTo.getMp()));
        }
        applyTo.getClient().getSession().write(MaplePacketCreator.updatePlayerStats(hpMpUpdate, true));
        if (isResurrection() && !primary) {
            applyTo.incrementDeathPenaltyAndRecalc(5);
            applyTo.setExp(0);
            applyTo.updateSingleStat(MapleStat.EXP, 0);
            applyTo.getClient().getSession().write(MaplePacketCreator.getClock(0));
        }
        if (moveTo != -1) {
            if (applyTo.getMap().getReturnMapId() != applyTo.getMapId()) {
                MapleMap target;
                if (moveTo == 999999999) {
                    target = applyTo.getMap().getReturnMap();
                } else {
                    target = ChannelServer.getInstance(applyTo.getClient().getChannel()).getMapFactory().getMap(moveTo);
                    if (target.getId() / 10000000 != 60 && applyTo.getMapId() / 10000000 != 61) {
                        if (target.getId() / 10000000 != 21 && applyTo.getMapId() / 10000000 != 20) {
                            if (target.getId() / 10000000 != applyTo.getMapId() / 10000000) {
                                log.info("Player {} is trying to use a return scroll to an illegal location ({}->{})", new Object[]{applyTo.getName(), applyTo.getMapId(), target.getId()});
                                //applyTo.getClient().disconnect();
                                //return false;
                            }
                        }
                    }
                }
                applyTo.changeMap(target, target.getPortal(0));
            } else {
                return false;
            }
        }
        if (isShadowClaw()) {
            MapleInventory use = applyTo.getInventory(MapleInventoryType.USE);
            MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
            int projectile = 0;
            for (int i = 0; i < 255; ++i) {
                IItem item = use.getItem((byte) i);
                if (item != null) {
                    boolean isStar = mii.isThrowingStar(item.getItemId());
                    if (isStar && item.getQuantity() >= 200) {
                        projectile = item.getItemId();
                        break;
                    }
                }
            }
            if (projectile == 0) {
                return false;
            } else {
                MapleInventoryManipulator.removeById(applyTo.getClient(), MapleInventoryType.USE, projectile, 200, false, true);
            }
        }
        if (overTime) {
            applyBuffEffect(applyFrom, applyTo, primary);
        }
        if (primary && (overTime || isHeal())) {
            applyBuff(applyFrom);
        }
        if (primary && isMonsterBuff()) {
            applyMonsterBuff(applyFrom);
        }

        SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null && pos != null) {
            final MapleSummon toSummon = new MapleSummon(applyFrom, sourceid, pos, summonMovementType);
            if (!toSummon.isPuppet()) {
                applyFrom.getCheatTracker().resetSummonAttack();
            }
            applyFrom.getMap().spawnSummon(toSummon);
            applyFrom.getSummons().put(sourceid, toSummon);
            toSummon.addHP(x);
            if (isBeholder()) {
                toSummon.addHP(1);
            }
        }

        // Magic Door
        if (isMagicDoor()) {
            //applyTo.cancelMagicDoor();
            Point doorPosition = new Point(applyTo.getPosition());
            //doorPosition.y -= 280;
            MapleDoor door = new MapleDoor(applyTo, doorPosition);
            applyTo.getMap().spawnDoor(door);
            applyTo.addDoor(door);
            door = new MapleDoor(door);
            applyTo.addDoor(door);
            door.getTown().spawnDoor(door);
            if (applyTo.getParty() != null) {
                // update town doors
                applyTo.silentPartyUpdate();
            }
            applyTo.disableDoor();
        } else if (isMist()) {
            Rectangle bounds = calculateBoundingBox(applyFrom.getPosition(), applyFrom.isFacingLeft());
            MapleMist mist = new MapleMist(bounds, applyFrom, this);
            applyFrom.getMap().spawnMist(mist, getDuration(), sourceid == 2111003, false);
        }
        // Time Leap
        if (isTimeLeap()) {
            Set<Integer> disabledSkills = null;
            if (applyTo.getPartyQuest() != null) {
                if (applyTo.getPartyQuest().getMapInstance(applyTo.getMap()) != null) {
                    disabledSkills = applyTo.getPartyQuest().getMapInstance(applyTo.getMap()).readDisabledSkills();
                }
            }
            for (PlayerCoolDownValueHolder i : applyTo.getAllCooldowns()) {
                if (i.skillId != 5121010 && (disabledSkills == null || !disabledSkills.contains(i.skillId))) {
                    applyTo.removeCooldown(i.skillId);
                }
            }
        }
        if (sourceid == 5111005) {
            List<Pair<MapleBuffStat, Integer>> pMorphStatup = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, getMorph(applyTo)));
            applyTo.getClient().getSession().write(MaplePacketCreator.giveBuff(sourceid, getDuration(), pMorphStatup));
        }
        return true;
    }

    private void applyBuff(final MapleCharacter applyFrom) {
        if (isPartyBuff() && (applyFrom.getParty() != null || isGmBuff())) {
            Rectangle bounds = calculateBoundingBox(applyFrom.getPosition(), applyFrom.isFacingLeft());
            List<MapleMapObject> affecteds = applyFrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.PLAYER));
            List<MapleCharacter> affectedp = new ArrayList<>(affecteds.size());
            MapleCharacter closestplayer = null;
            double closestdistance = Double.MAX_VALUE;
            for (MapleMapObject affectedmo : affecteds) {
                MapleCharacter affected = (MapleCharacter) affectedmo;
                if (affected != null && isHeal() && affected != applyFrom && affected.getParty() != null && affected.getParty() == applyFrom.getParty() && affected.isAlive()) {
                    int expadd = (int) ((((double) calcHPChange(applyFrom, true) / 10.0) * ((double) applyFrom.getAbsoluteXp() * (double) applyFrom.getClient().getChannelServer().getExpRate() + ((Math.random() * 10.0) + 30.0)) * ((Math.random() * (double) applyFrom.getSkillLevel(SkillFactory.getSkill(2301002)) / 100.0) * ((double) applyFrom.getLevel() / 30.0))) / 4.0);
                    if (affected.getHp() < affected.getMaxHp() - affected.getMaxHp() / 20) {
                        applyFrom.gainExp(expadd, true, false, false);
                    }
                }
                if (affected != null && affected != applyFrom && (isGmBuff() || applyFrom.getParty().equals(affected.getParty()))) {
                    boolean isResurrection = isResurrection();
                    if ((isResurrection && !affected.isAlive()) || (!isResurrection && affected.isAlive())) {
                        if (isResurrection) {
                            if (applyFrom.getPosition().distanceSq(affected.getPosition()) < closestdistance) {
                                closestdistance = applyFrom.getPosition().distanceSq(affected.getPosition());
                                closestplayer = affected;
                            }
                        } else {
                            affectedp.add(affected);
                        }
                    }
                    if (isTimeLeap()) {
                        Set<Integer> disabledSkills = null;
                        if (affected.getPartyQuest() != null) {
                            if (affected.getPartyQuest().getMapInstance(affected.getMap()) != null) {
                                disabledSkills = affected.getPartyQuest().getMapInstance(affected.getMap()).readDisabledSkills();
                            }
                        }
                        for (PlayerCoolDownValueHolder i : affected.getAllCooldowns()) {
                            if (i.skillId != 5121010 && (disabledSkills == null || !disabledSkills.contains(i.skillId))) {
                                affected.removeCooldown(i.skillId);
                            }
                        }
                    }
                }
            }
            if (closestplayer != null) {
                affectedp.add(closestplayer);
            }
            for (MapleCharacter affected : affectedp) {
                // TODO actually heal (and others) shouldn't recalculate everything
                // For heal, this is an actual bug since heal HP is decreased with the number
                // of affected players.
                applyTo(applyFrom, affected, false, null);
                affected.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 2));
                affected.getMap().broadcastMessage(affected, MaplePacketCreator.showBuffeffect(affected.getId(), sourceid, 2, (byte) 3), false);
            }
        }
    }

    private void applyMonsterBuff(MapleCharacter applyFrom) {
        Rectangle bounds = calculateBoundingBox(applyFrom.getPosition(), applyFrom.isFacingLeft());
        List<MapleMapObject> affected = applyFrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.MONSTER));
        final ISkill skill_ = SkillFactory.getSkill(sourceid);
        Map<MonsterStatus, Integer> localMonsterStatus = getMonsterStati();
        if (skill_.getId() == 1201006) { // Handing Threaten's scaling with level
            int baseThreatenDebuff = skill_.getEffect(applyFrom.getSkillLevel(skill_)).getX();
            int threatenDebuffScale = applyFrom.getLevel() - 30;
            if (localMonsterStatus.containsKey(MonsterStatus.WATK)) {
                localMonsterStatus.put(MonsterStatus.WATK, baseThreatenDebuff - threatenDebuffScale);
            }
            if (localMonsterStatus.containsKey(MonsterStatus.MATK)) {
                localMonsterStatus.put(MonsterStatus.MATK, baseThreatenDebuff - threatenDebuffScale);
            }
        }
        int i = 0;
        for (MapleMapObject mo : affected) {
            MapleMonster monster = (MapleMonster) mo;
            if (makeChanceResult()) {
                monster.applyStatus(applyFrom, new MonsterStatusEffect(localMonsterStatus, skill_, false), isPoison(), getDuration());
            }
            i++;
            if (i >= mobCount) {
                break;
            }
        }
    }

    private Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(lt.x * -1 + posFrom.x, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }

    public void silentApplyBuff(final MapleCharacter chr, final long startTime) {
        final List<Pair<MapleBuffStat, Integer>> localStatups = new ArrayList<>(getStatups());
        boolean buffPrioritized = false;
        for (int i = 0; i < localStatups.size(); ++i) {
            final Pair<MapleBuffStat, Integer> localStatup = localStatups.get(i);
            if (chr.getBuffedValue(localStatup.getLeft()) != null) {
                if (chr.getBuffedValue(localStatup.getLeft()) > localStatup.getRight()) {
                    // Player already has buff of this type that is better.
                    if (getDuration() > chr.getBuffedRemainingTime(localStatup.getLeft())) {
                        // The better buff isn't going to last as long as this buff would,
                        // so we will give it when the better one wears off.
                        final MapleStatEffect lingeringInferiorBuff = new MapleStatEffect(this);
                        lingeringInferiorBuff.statups = Collections.singletonList(localStatup);
                        final int finalSourceId = getSourceId();
                        final int finalDuration = getDuration();
                        TimerManager.getInstance().schedule(() -> {
                            chr.getClient().getSession().write(MaplePacketCreator.giveBuff((lingeringInferiorBuff.isSkill() ? finalSourceId : -finalSourceId), finalDuration, lingeringInferiorBuff.getStatups()));
                            final CancelEffectAction cancelAction = new CancelEffectAction(chr, lingeringInferiorBuff, startTime);
                            final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, finalDuration);
                            chr.registerEffect(lingeringInferiorBuff, startTime, schedule);
                        }, chr.getBuffedRemainingTime(localStatup.getLeft()) + 10);
                    }
                    // We remove the inferior buff (for now, possibly).
                    localStatups.remove(i--);
                    buffPrioritized = true;
                } else if (getDuration() < (chr.getBuffedRemainingTime(localStatup.getLeft()) == null ? (long) -1 : chr.getBuffedRemainingTime(localStatup.getLeft()))) {
                    // This buff is as good or better, but doesn't last as long so we will reinstate
                    // the inferior one once this one wears off.
                    final MapleStatEffect currentInferiorBuff = new MapleStatEffect(chr.getStatForBuff(localStatup.getLeft()));
                    currentInferiorBuff.statups = Collections.singletonList(localStatup);
                    final long inferiorDuration = chr.getBuffedRemainingTime(localStatup.getLeft()) - getDuration();
                    TimerManager.getInstance().schedule(() -> {
                        chr.getClient().getSession().write(MaplePacketCreator.giveBuff((currentInferiorBuff.isSkill() ? currentInferiorBuff.getSourceId() : -currentInferiorBuff.getSourceId()), currentInferiorBuff.getDuration(), currentInferiorBuff.getStatups()));
                        final CancelEffectAction cancelAction = new CancelEffectAction(chr, currentInferiorBuff, System.currentTimeMillis());
                        final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, inferiorDuration);
                        chr.registerEffect(currentInferiorBuff, System.currentTimeMillis(), schedule);
                    }, getDuration());
                }
            }
        }
        if (buffPrioritized && !localStatups.isEmpty()) {
            final MapleStatEffect prioritizedStatEffect = new MapleStatEffect(this);
            prioritizedStatEffect.statups = localStatups;
            int localDuration = prioritizedStatEffect.getDuration();
            localDuration = alchemistModifyVal(chr, localDuration, false);
            final CancelEffectAction cancelAction = new CancelEffectAction(chr, prioritizedStatEffect, startTime);
            ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((startTime + localDuration) - System.currentTimeMillis()));
            chr.registerEffect(prioritizedStatEffect, startTime, schedule);
        } else {
            int localDuration = duration;
            localDuration = alchemistModifyVal(chr, localDuration, false);
            final CancelEffectAction cancelAction = new CancelEffectAction(chr, this, startTime);
            ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((startTime + localDuration) - System.currentTimeMillis()));
            chr.registerEffect(this, startTime, schedule);
        }

        SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null) {
            final MapleSummon toSummon = new MapleSummon(chr, sourceid, chr.getPosition(), summonMovementType);
            if (!toSummon.isPuppet()) {
                chr.getMap().spawnSummon(toSummon);
                chr.getCheatTracker().resetSummonAttack();
                chr.getSummons().put(sourceid, toSummon);
                toSummon.addHP(x);
            }
        }
    }

    private void applyBuffEffect(MapleCharacter applyFrom, final MapleCharacter applyTo, boolean primary) {
        boolean wasShip = false;
        if (sourceid != 5221006) {
            if (!this.isMonsterRiding()) {
                applyTo.cancelEffect(this, true, -1);
            }
        } else {
            if (applyTo.getStatForBuff(MapleBuffStat.MONSTER_RIDING) != null) {
                applyTo.cancelEffect(this, true, -1);
                wasShip = true;
            }
        }
        List<Pair<MapleBuffStat, Integer>> localStatups = new ArrayList<>(getStatups());
        int localDuration = duration;
        int localSourceId = sourceid;
        int localX = x;
        int localY = y;
        int seconds = localDuration / 1000;
        MapleMount givemount = null;
        if (isHolySymbol()) {
            if (applyFrom.getId() != applyTo.getId()) { // Making sure that other players don't get the watk buff.
                for (int i = 0; i < localStatups.size(); ++i) {
                    Pair<MapleBuffStat, Integer> statup = localStatups.get(i);
                    if (statup.getLeft() == MapleBuffStat.WATK) {
                        localStatups.remove(i);
                        break;
                    }
                }
            } else { // Setting the actual watk value for Holy Symbol
                boolean alreadyHasWatk = false;
                for (int i = 0; i < localStatups.size(); ++i) {
                    Pair<MapleBuffStat, Integer> statup = localStatups.get(i);
                    if (statup.getLeft() == MapleBuffStat.WATK) {
                        alreadyHasWatk = true;
                        // 3 * skillLevel + casterLevel / 1.5
                        int watt = (int) ((3.0d * applyFrom.getSkillLevel(SkillFactory.getSkill(localSourceId))) + (applyFrom.getLevel() / 1.3d));
                        localStatups.set(i, new Pair<>(MapleBuffStat.WATK, watt));
                        break;
                    }
                }
                if (!alreadyHasWatk) {
                    int watt = (int) ((3.0d * applyFrom.getSkillLevel(SkillFactory.getSkill(localSourceId))) + (applyFrom.getLevel() / 1.3d));
                    localStatups.add(0, new Pair<>(MapleBuffStat.WATK, watt));
                }
            }
        } else if (getSourceId() == 2001003 && isSkill() && applyFrom.getLevel() > 100) { // Making Magic Armor scale with level
            for (int i = 0; i < localStatups.size(); ++i) {
                Pair<MapleBuffStat, Integer> localStatup = localStatups.get(i);
                if (localStatup.getLeft() == MapleBuffStat.WDEF) {
                    int baseDef = SkillFactory.getSkill(getSourceId()).getEffect(applyFrom.getSkillLevel(SkillFactory.getSkill(getSourceId()))).getWdef();
                    int newDef = (int) (baseDef * (1.0d + (applyFrom.getLevel() - 100.0d) / 60.0d));
                    localStatups.set(i, new Pair<>(localStatup.getLeft(), newDef));
                } else if (localStatup.getLeft() == MapleBuffStat.MDEF) {
                    int baseDef = SkillFactory.getSkill(getSourceId()).getEffect(applyFrom.getSkillLevel(SkillFactory.getSkill(getSourceId()))).getMdef();
                    int newDef = (int) (baseDef * (1.0d + (applyFrom.getLevel() - 100.0d) / 60.0d));
                    localStatups.set(i, new Pair<>(localStatup.getLeft(), newDef));
                }
            }
        } else if (getSourceId() == 2301004 && isSkill() && applyFrom.getLevel() > 100) { // Making Bless defense scale with level
            for (int i = 0; i < localStatups.size(); ++i) {
                Pair<MapleBuffStat, Integer> localStatup = localStatups.get(i);
                if (localStatup.getLeft() == MapleBuffStat.WDEF) {
                    int baseDef = SkillFactory.getSkill(getSourceId()).getEffect(applyFrom.getSkillLevel(SkillFactory.getSkill(getSourceId()))).getWdef();
                    int newDef = (int) (baseDef * (1.0d + (applyFrom.getLevel() - 100.0d) / 80.0d));
                    localStatups.set(i, new Pair<>(localStatup.getLeft(), newDef));
                } else if (localStatup.getLeft() == MapleBuffStat.MDEF) {
                    int baseDef = SkillFactory.getSkill(getSourceId()).getEffect(applyFrom.getSkillLevel(SkillFactory.getSkill(getSourceId()))).getMdef();
                    int newDef = (int) (baseDef * (1.0d + (applyFrom.getLevel() - 100.0d) / 80.0d));
                    localStatups.set(i, new Pair<>(localStatup.getLeft(), newDef));
                }
            }
        } else if ((getSourceId() == 5111005 || getSourceId() == 5121003 || getSourceId() == 1301006) && isSkill() && applyFrom.getLevel() > 100) {
            // Making Transformation, Super Transformation, and Iron Will defense scale with level
            for (int i = 0; i < localStatups.size(); ++i) {
                Pair<MapleBuffStat, Integer> localStatup = localStatups.get(i);
                if (localStatup.getLeft() == MapleBuffStat.WDEF) {
                    int baseDef = SkillFactory.getSkill(getSourceId()).getEffect(applyFrom.getSkillLevel(SkillFactory.getSkill(getSourceId()))).getWdef();
                    int newDef = (int) (baseDef * (1.0d + (applyFrom.getLevel() - 100.0d) / 200.0d));
                    localStatups.set(i, new Pair<>(localStatup.getLeft(), newDef));
                } else if (localStatup.getLeft() == MapleBuffStat.MDEF) {
                    int baseDef = SkillFactory.getSkill(getSourceId()).getEffect(applyFrom.getSkillLevel(SkillFactory.getSkill(getSourceId()))).getMdef();
                    int newDef = (int) (baseDef * (1.0d + (applyFrom.getLevel() - 100.0d) / 200.0d));
                    localStatups.set(i, new Pair<>(localStatup.getLeft(), newDef));
                }
            }
        } else if (getSourceId() == 1320009 && isSkill()) { // Making Beholder hex defense scale with level
            for (int i = 0; i < localStatups.size(); ++i) {
                Pair<MapleBuffStat, Integer> localStatup = localStatups.get(i);
                if (localStatup.getLeft() == MapleBuffStat.WDEF) {
                    int baseDef = SkillFactory.getSkill(getSourceId()).getEffect(applyFrom.getSkillLevel(SkillFactory.getSkill(getSourceId()))).getWdef();
                    int newDef = (int) (baseDef * (1.0d + (applyFrom.getLevel() - 100.0d) / 250.0d));
                    localStatups.set(i, new Pair<>(localStatup.getLeft(), newDef));
                } else if (localStatup.getLeft() == MapleBuffStat.MDEF) {
                    int baseDef = SkillFactory.getSkill(getSourceId()).getEffect(applyFrom.getSkillLevel(SkillFactory.getSkill(getSourceId()))).getMdef();
                    int newDef = (int) (baseDef * (1.0d + (applyFrom.getLevel() - 100.0d) / 250.0d));
                    localStatups.set(i, new Pair<>(localStatup.getLeft(), newDef));
                }
            }
        } else if (getSourceId() == 2301003 && isSkill()) { // Removing attack speed buff from Invincible if the player isn't wielding a BW
            IItem weaponItem = applyTo.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) - 11);
            MapleWeaponType weapon = null;
            if (weaponItem != null) {
                weapon = MapleItemInformationProvider.getInstance().getWeaponType(weaponItem.getItemId());
            }
            if (!(weapon == MapleWeaponType.BLUNT1H || weapon == MapleWeaponType.BLUNT2H)) {
                for (int i = 0; i < localStatups.size(); ++i) {
                    Pair<MapleBuffStat, Integer> localStatup = localStatups.get(i);
                    if (localStatup.getLeft() == MapleBuffStat.BOOSTER) {
                        localStatups.remove(i);
                        i--;
                    }
                }
            }
        } else if (getSourceId() == 2321002 && isSkill() && applyFrom.getTotalStr() >= 200) { // Adding magic defense for Battle Priest's Mana Reflection
            if (!localStatups.stream().anyMatch(p -> p.getLeft() == MapleBuffStat.MDEF)) {
                localStatups.add(new Pair<>(MapleBuffStat.MDEF, applyFrom.getTotalStr()));
            }
        }
        if (isMonsterRiding()) {
            int ridingLevel = 0; // Mount ID.
            IItem mount = applyFrom.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
            if (mount != null) {
                ridingLevel = mount.getItemId();
            }
            if (sourceid == 5221006) {
                ridingLevel = 1932000;
                givemount = new MapleMount(applyTo, ridingLevel, 5221006);
                if (wasShip) {
                    givemount.setActive(false);
                } else {
                    applyTo.Mount(ridingLevel, 5221006);
                    givemount.setActive(true);
                }
                if (applyTo.getBattleshipHp() < 1) {
                    applyTo.setBattleshipHp(applyTo.getBattleshipMaxHp());
                }
            } else {
                if (applyTo.getMount() == null) {
                    applyTo.Mount(ridingLevel, sourceid);
                }
                givemount = applyTo.getMount();
                givemount.startSchedule();
                givemount.setActive(true);
            }
            localDuration = sourceid;
            localSourceId = ridingLevel;
            if (sourceid != 5221006) {
                localStatups = Collections.singletonList(new Pair<>(MapleBuffStat.MONSTER_RIDING, 0));
            }
        }
        if (isPirateMorph()) {
            List<Pair<MapleBuffStat, Integer>> pMorphStatup = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, getMorph(applyTo)));
            applyTo.getClient().getSession().write(MaplePacketCreator.giveBuff(localSourceId, localDuration, pMorphStatup));
        }
        if (isRecovery()) {
            applyTo.setHp(applyTo.getHp() + localStatups.get(0).getRight() * 6);
            applyTo.updateSingleStat(MapleStat.HP, applyTo.getHp());
        }
        if (primary) {
            localDuration = alchemistModifyVal(applyFrom, localDuration, false);
        }
        boolean buffPrioritized = false;
        if (!localStatups.isEmpty()) {
            if (isDash()) {
                if (!applyTo.getJob().isA(MapleJob.PIRATE)) {
                    applyTo.changeSkillLevel(SkillFactory.getSkill(sourceid), 0, 10);
                } else {
                    localStatups = Collections.singletonList(new Pair<>(MapleBuffStat.DASH, 1));
                    applyTo.getClient().getSession().write(MaplePacketCreator.giveDash(localStatups, localX, localY, seconds));
                }
            } else if (isInfusion()) {
                applyTo.getClient().getSession().write(MaplePacketCreator.giveInfusion(seconds, x));
            } else {
                for (int i = 0; i < localStatups.size(); ++i) {
                    final Pair<MapleBuffStat, Integer> localStatup = localStatups.get(i);
                    if (applyTo.getBuffedValue(localStatup.getLeft()) != null) {
                        if (applyTo.getBuffedValue(localStatup.getLeft()) > localStatup.getRight()) {
                            if (localDuration > applyTo.getBuffedRemainingTime(localStatup.getLeft())) {
                                // The better buff isn't going to last as long as this buff would,
                                // so we will give it when the better one wears off.
                                final MapleStatEffect lingeringInferiorBuff = new MapleStatEffect(this);
                                lingeringInferiorBuff.statups = Collections.singletonList(localStatup);
                                final int finalSourceId = localSourceId;
                                final int finalDuration = localDuration;
                                TimerManager.getInstance().schedule(() -> {
                                    applyTo.getClient().getSession().write(MaplePacketCreator.giveBuff((lingeringInferiorBuff.isSkill() ? finalSourceId : -finalSourceId), finalDuration, lingeringInferiorBuff.getStatups()));
                                    final long startTime = System.currentTimeMillis();
                                    final CancelEffectAction cancelAction = new CancelEffectAction(applyTo, lingeringInferiorBuff, startTime);
                                    final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, finalDuration);
                                    applyTo.registerEffect(lingeringInferiorBuff, startTime, schedule);
                                }, applyTo.getBuffedRemainingTime(localStatup.getLeft()) + 10);
                            }
                            // We remove the inferior buff (for now, possibly).
                            localStatups.remove(i--);
                            buffPrioritized = true;
                        } else if (getDuration() < applyTo.getBuffedRemainingTime(localStatup.getLeft())) {
                            // This buff is as good or better, but doesn't last as long so we will reinstate
                            // the inferior one once this one wears off.
                            final MapleStatEffect currentInferiorBuff = new MapleStatEffect(applyTo.getStatForBuff(localStatup.getLeft()));
                            currentInferiorBuff.statups = Collections.singletonList(localStatup);
                            final long inferiorDuration = applyTo.getBuffedRemainingTime(localStatup.getLeft()) - getDuration();
                            TimerManager.getInstance().schedule(() -> {
                                applyTo.getClient().getSession().write(MaplePacketCreator.giveBuff((currentInferiorBuff.isSkill() ? currentInferiorBuff.getSourceId() : -currentInferiorBuff.getSourceId()), currentInferiorBuff.getDuration(), currentInferiorBuff.getStatups()));
                                final CancelEffectAction cancelAction = new CancelEffectAction(applyTo, currentInferiorBuff, System.currentTimeMillis());
                                final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, inferiorDuration);
                                applyTo.registerEffect(currentInferiorBuff, System.currentTimeMillis(), schedule);
                            }, getDuration());
                        }
                    }
                }
                applyTo.getClient().getSession().write(MaplePacketCreator.giveBuff((skill ? localSourceId : -localSourceId), localDuration, localStatups));
            }
            /*if (sourceid == 5221006) { // Battleship
                List<Pair<MapleBuffStat, Integer>> relocalstatups = statups;
                List<Pair<MapleBuffStat, Integer>> battleshipdefstatups = new ArrayList<>(2);
                for (int i = 0; i < relocalstatups.size(); ++i) {
                    Pair<MapleBuffStat, Integer> localstatup = relocalstatups.get(i);
                    if (localstatup.getLeft() == MapleBuffStat.WDEF || localstatup.getLeft() == MapleBuffStat.MDEF) {
                        battleshipdefstatups.add(localstatup);
                    }
                }
                applyTo.getClient().getSession().write(MaplePacketCreator.giveBuff((skill ? localSourceId : -localSourceId), localDuration, battleshipdefstatups));
            }*/
        } else {
            // Apply empty buff icon.
            applyTo.getClient().getSession().write(MaplePacketCreator.giveBuffTest((skill ? localSourceId : -localSourceId), localDuration, 0));
        }
        if (isMonsterRiding()) {
            if (givemount != null && givemount.getItemId() != 0) {
                applyTo.getMap().broadcastMessage(applyTo, MaplePacketCreator.showMonsterRiding(applyTo.getId(), Collections.singletonList(new Pair<>(MapleBuffStat.MONSTER_RIDING, 1)), givemount), false);
            }
            localDuration = duration;
        }
        if (isDs()) {
            List<Pair<MapleBuffStat, Integer>> dsStat = Collections.singletonList(new Pair<>(MapleBuffStat.DARKSIGHT, 0));
            applyTo.getMap().broadcastMessage(applyTo, MaplePacketCreator.giveForeignBuff(applyTo.getId(), dsStat, this), false);
        }
        if (isCombo()) {
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.COMBO, 1));
            applyTo.getMap().broadcastMessage(applyTo, MaplePacketCreator.giveForeignBuff(applyTo.getId(), stat, this), false);
        }
        if (isShadowPartner()) {
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.SHADOWPARTNER, 0));
            applyTo.getMap().broadcastMessage(applyTo, MaplePacketCreator.giveForeignBuff(applyTo.getId(), stat, this), false);
        }
        if (isSoulArrow()) {
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.SOULARROW, 0));
            applyTo.getMap().broadcastMessage(applyTo, MaplePacketCreator.giveForeignBuff(applyTo.getId(), stat, this), false);
        }
        if (isEnrage()) {
            applyTo.handleOrbconsume();
        }
        if (isMorph()) {
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, getMorph(applyTo)));
            applyTo.getMap().broadcastMessage(applyTo, MaplePacketCreator.giveForeignBuff(applyTo.getId(), stat, this), false);
        }
        if (isOakBarrel()) {
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, 1002));
            applyTo.getMap().broadcastMessage(applyTo, MaplePacketCreator.giveForeignBuff(applyTo.getId(), stat, this), false);
        }
        if (isTimeLeap()) {
            Set<Integer> disabledSkills = null;
            if (applyTo.getPartyQuest() != null) {
                if (applyTo.getPartyQuest().getMapInstance(applyTo.getMap()) != null) {
                    disabledSkills = applyTo.getPartyQuest().getMapInstance(applyTo.getMap()).readDisabledSkills();
                }
            }
            for (PlayerCoolDownValueHolder i : applyTo.getAllCooldowns()) {
                if (i.skillId != 5121010 && (disabledSkills == null || !disabledSkills.contains(i.skillId))) {
                    applyTo.removeCooldown(i.skillId);
                }
            }
        }
        if (!localStatups.isEmpty()) {
            if (buffPrioritized) {
                final MapleStatEffect prioritizedStatEffect = new MapleStatEffect(this);
                prioritizedStatEffect.statups = localStatups;
                final long startTime = System.currentTimeMillis();
                final CancelEffectAction cancelAction = new CancelEffectAction(applyTo, prioritizedStatEffect, startTime);
                final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, localDuration);
                applyTo.registerEffect(prioritizedStatEffect, startTime, schedule);
            } else {
                final long startTime = System.currentTimeMillis();
                final CancelEffectAction cancelAction = new CancelEffectAction(applyTo, this, startTime);
                final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, localDuration);
                applyTo.registerEffect(this, startTime, schedule);
            }
        }
        if (primary) {
            if (isDash()) {
                applyTo.getMap().broadcastMessage(applyTo, MaplePacketCreator.showDashEffectToOthers(applyTo.getId(), localX, localY, seconds), false);
            } else if (isInfusion()) {
                applyTo.getMap().broadcastMessage(applyTo, MaplePacketCreator.giveForeignInfusion(applyTo.getId(), x, seconds), false);
            } else {
                applyTo.getMap().broadcastMessage(applyTo, MaplePacketCreator.showBuffeffect(applyTo.getId(), sourceid, 1, (byte) 3), false);
            }
        }
    }

    private int calcHPChange(MapleCharacter applyfrom, boolean primary) {
        int hpchange = 0;
        if (hp != 0) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, hp, true);
                } else {
                    hpchange += hp;
                }
            } else { // Assumption: this is heal.
                hpchange += makeHealHP((double) hp / 100.0, (double) applyfrom.getTotalMagic(), 3.0, 5.0);
            }
        }
        if (hpR != 0) {
            hpchange += (int) (applyfrom.getCurrentMaxHp() * hpR);
        }
        // Actually receivers probably never get any hp when it's not heal but whatever
        if (primary) {
            if (hpCon != 0) {
                hpchange -= hpCon;
            }
        }
        if (isChakra()) {
            hpchange += makeHealHP(getY() / 100.0, applyfrom.getTotalLuk(), 2.3, 3.5);
        }
        return hpchange;
    }

    private int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        int maxHeal = (int) (stat * upperfactor * rate);
        int minHeal = (int) (stat * lowerfactor * rate);
        return (int) ((Math.random() * (maxHeal - minHeal + 1)) + minHeal);
    }

    private int calcMPChange(MapleCharacter applyfrom, boolean primary) {
        int mpchange = 0;
        if (mp != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, mp, true);
            } else {
                mpchange += mp;
            }
        }
        if (mpR != 0) {
            mpchange += (int) (applyfrom.getCurrentMaxMp() * mpR);
        }
        if (primary) {
            if (mpCon != 0) {
                double mod = 1.0;
                boolean isAFpMage = applyfrom.getJob().isA(MapleJob.FP_MAGE);
                if (isAFpMage || applyfrom.getJob().isA(MapleJob.IL_MAGE)) {
                    ISkill amp;
                    if (isAFpMage) {
                        amp = SkillFactory.getSkill(2110001);
                    } else {
                        amp = SkillFactory.getSkill(2210001);
                    }
                    int ampLevel = applyfrom.getSkillLevel(amp);
                    if (ampLevel > 0) {
                        MapleStatEffect ampStat = amp.getEffect(ampLevel);
                        mod = ampStat.getX() / 100.0;
                    }
                }
                mpchange -= mpCon * mod;
                if (applyfrom.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                    mpchange = 0;
                }
            }
        }
        return mpchange;
    }

    private int alchemistModifyVal(MapleCharacter chr, int val, boolean withX) {
        if (!skill && chr.getJob().isA(MapleJob.HERMIT)) {
            MapleStatEffect alchemistEffect = getAlchemistEffect(chr);
            if (alchemistEffect != null) {
                return (int) (val * ((withX ? alchemistEffect.getX() : alchemistEffect.getY()) / 100.0));
            }
        }
        return val;
    }

    private MapleStatEffect getAlchemistEffect(MapleCharacter chr) {
        ISkill alchemist = SkillFactory.getSkill(4110000);
        int alchemistLevel = chr.getSkillLevel(alchemist);
        if (alchemistLevel == 0) {
            return null;
        }
        return alchemist.getEffect(alchemistLevel);
    }

    public void setSourceId(int newid) {
        sourceid = newid;
    }

    private boolean isGmBuff() {
        switch (sourceid) {
            case 1005: // Echo of hero acts like a GM buff.
            case 9101000:
            case 9101001:
            case 9101002:
            case 9101003:
            case 9101005:
            case 9101008:
                return true;
            default:
                return false;
        }
    }

    private boolean isMonsterBuff() {
        if (!skill) {
            return false;
        }
        switch (sourceid) {
            case 1201006: // threaten
            case 2101003: // fp slow
            case 2201003: // il slow
            case 2211004: // il seal
            case 2111004: // fp seal
            case 2311005: // doom
            case 4111003: // shadow web
                return true;
        }
        return false;
    }

    private boolean isPartyBuff() {
        return !(lt == null || rb == null) && !((sourceid >= 1211003 && sourceid <= 1211008) || sourceid == 1221003 || sourceid == 1221004);
    }

    public boolean isHeal() {
        return sourceid == 2301002 || sourceid == 9101000;
    }

    public boolean isResurrection() {
        return sourceid == 9101005 || sourceid == 2321006;
    }

    public boolean isTimeLeap() {
        return sourceid == 5121010;
    }

    public short getHp() {
        return hp;
    }

    public short getMp() {
        return mp;
    }
    
    public double getHpR() {
        return this.hpR;
    }

    public double getMpR() {
        return this.mpR;
    }
    
    public short getHpCon() {
        return this.hpCon;
    }

    public short getMpCon() {
        return this.mpCon;
    }
    
    public int getMoveTo() {
        return this.moveTo;
    }
    
    public int getIProp() {
        return this.iProp;
    }
    
    public int getItemCon() {
        return this.itemCon;
    }
    
    public int getItemConNo() {
        return this.itemConNo;
    }
    
    public Point getLt() {
        return this.lt;
    }

    public Point getRb() {
        return this.rb;
    }

    public int getMobCount() {
        return this.mobCount;
    }
    
    public List<MapleDisease> getCureDebuffs() {
        return this.cureDebuffs;
    }
    
    public short getWatk() {
        return watk;
    }

    public short getMatk() {
        return matk;
    }

    public short getWdef() {
        return wdef;
    }

    public short getMdef() {
        return mdef;
    }

    public short getAcc() {
        return acc;
    }

    public short getAvoid() {
        return avoid;
    }

    public short getHands() {
        return hands;
    }

    public short getSpeed() {
        return speed;
    }

    public short getJump() {
        return jump;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isOverTime() {
        return overTime;
    }

    public List<Pair<MapleBuffStat, Integer>> getStatups() {
        return statups;
    }

    public boolean sameSource(MapleStatEffect effect) {
        return this.sourceid == effect.sourceid && this.skill == effect.skill;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getDamage() {
        return damage;
    }

    public int getAttackCount() {
        return attackCount;
    }

    public int getBulletCount() {
        return bulletCount;
    }

    public int getBulletConsume() {
        return bulletConsume;
    }

    public int getMoneyCon() {
        return moneyCon;
    }

    public int getCooldown() {
        return cooldown;
    }

    public Map<MonsterStatus, Integer> getMonsterStati() {
        return monsterStatus;
    }

    public boolean isHide() {
        return skill && sourceid == 9101004;
    }

    public boolean isDragonBlood() {
        return skill && sourceid == 1311008;
    }

    public boolean isBerserk() {
        return skill && sourceid == 1320006;
    }

    private boolean isDs() {
        return skill && sourceid == 4001003;
    }

    private boolean isCombo() {
        return skill && sourceid == 1111002;
    }

    private boolean isEnrage() {
        return skill && sourceid == 1121010;
    }

    public boolean isBeholder() {
        return skill && sourceid == 1321007;
    }

    private boolean isShadowPartner() {
        return skill && sourceid == 4111002;
    }

    private boolean isChakra() {
        return skill && sourceid == 4211001;
    }

    public boolean isMonsterRiding() {
        return skill && (sourceid == 1004 || sourceid == 5221006);
    }

    public boolean isMagicDoor() {
        return skill && sourceid == 2311002;
    }

    public boolean isMesoGuard() {
        return skill && sourceid == 4211005;
    }

    public boolean isCharge() {
        return skill && sourceid >= 1211003 && sourceid <= 1211008;
    }

    public boolean isPoison() {
        return skill && (sourceid == 2111003 || sourceid == 2101005 || sourceid == 2111006);
    }

    private boolean isMist() {
        return skill && (sourceid == 2111003 || sourceid == 4221006); // poison mist and smokescreen
    }

    private boolean isSoulArrow() {
        return skill && (sourceid == 3101004 || sourceid == 3201004); // bow and crossbow
    }

    private boolean isShadowClaw() {
        return skill && sourceid == 4121006;
    }

    private boolean isDispel() {
        return skill && (sourceid == 2311001 || sourceid == 9101000);
    }

    private boolean isHeroWill() {
        return skill && (sourceid == 1121011 || sourceid == 1221012 || sourceid == 1321010 || sourceid == 2121008 || sourceid == 2221008 || sourceid == 2321009 || sourceid == 3121009 || sourceid == 3221008 || sourceid == 4121009 || sourceid == 4221008 || sourceid == 5121008 || sourceid == 5221010);
    }
    
    private boolean isHolySymbol() {
        return skill && sourceid == 2311003;
    }

    private boolean isDash() {
        return skill && sourceid == 5001005;
    }

    public boolean isPirateMorph() {
        return skill && (sourceid == 5111005 || sourceid == 5121003);
    }
    
    //
    public boolean isRecovery() {
        return skill && sourceid == 1001;
    }
    
    public boolean isMagicArmor() {
        return skill && sourceid == 2001003;
    }
    //

    public boolean isInfusion() {
        return skill && sourceid == 0;
    }

    public boolean isMorph() {
        return morphId > 0;
    }

    public int getMorph() {
        return morphId;
    }

    public int getMorph(MapleCharacter chr) {
        if (morphId == 1000) { // check for gender on pirate morph
            return 1000 + chr.getGender();
        } else if (morphId == 1100) {
            return 1100 + chr.getGender();
        }
        return morphId;
    }

    public SummonMovementType getSummonMovementType() {
        if (!skill) {
            return null;
        }
        switch (sourceid) {
            case 3211002: // puppet sniper
            case 3111002: // puppet ranger
            case 5211001: // octopus - pirate
            case 5220002: // advanced octopus - pirate
                return SummonMovementType.STATIONARY;
            case 3211005: // golden eagle
            case 3111005: // golden hawk
            case 2311006: // summon dragon
            case 3221005: // frostprey
            case 3121006: // phoenix
            case 5211002: // bird - pirate
                return SummonMovementType.CIRCLE_FOLLOW;
            case 1321007: // beholder
            case 2121005: // elquines
            case 2221005: // ifrit
            case 2321003: // bahamut
                return SummonMovementType.FOLLOW;
        }
        return null;
    }

    public boolean isSkill() {
        return skill;
    }

    public int getSourceId() {
        return sourceid;
    }

    /**
     *
     * @return true if the effect should happen based on its probability, false otherwise
     */
    public boolean makeChanceResult() {
        return prop == 1.0 || Math.random() < prop;
    }

    private boolean isCrash() {
        return skill && (sourceid == 1311007 || sourceid == 1211009 || sourceid == 1111007);
    }

    private boolean isOakBarrel() {
        return skill && sourceid == 5101007;
    }

    public static class CancelEffectAction implements Runnable {
        private final MapleStatEffect effect;
        private final WeakReference<MapleCharacter> target;
        private final long startTime;

        public CancelEffectAction(MapleCharacter target, MapleStatEffect effect, long startTime) {
            this.effect = effect;
            this.target = new WeakReference<>(target);
            this.startTime = startTime;
        }

        @Override
        public void run() {
            MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.cancelEffect(effect, false, startTime);
            }
        }
    }

    public int getMastery() {
        return mastery;
    }

    public int getRange() {
        return range;
    }

    public double getProp() {
        return iProp;
    }
    
    public void doubleDuration() {
        this.duration *= 2;
    }
}
