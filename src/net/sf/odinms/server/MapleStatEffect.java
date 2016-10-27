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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class MapleStatEffect implements Serializable {

    static final long serialVersionUID = 9179541993413738569L;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MapleStatEffect.class);
    private short watk,  matk,  wdef,  mdef,  acc,  avoid,  hands,  speed,  jump;
    private short hp,  mp;
    private double hpR,  mpR;
    private short mpCon,  hpCon;
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
                case 2001002: // magic guard
                    statups.add(new Pair<>(MapleBuffStat.MAGIC_GUARD, x));
                    break;
                case 2301003: // invincible
                    //
                    statups.add(new Pair<>(MapleBuffStat.BOOSTER, -2));
                    //
                    statups.add(new Pair<>(MapleBuffStat.INVINCIBLE, x));
                    break;
                case 9101004: // hide
                    ret.duration = 60 * 120 * 1000;
                    ret.overTime = true;
                case 4001003: // darksight
                    statups.add(new Pair<>(MapleBuffStat.DARKSIGHT, x));
                    break;
                case 4211003: // pickpocket
                    statups.add(new Pair<>(MapleBuffStat.PICKPOCKET, x));
                    break;
                case 4211005: // mesoguard
                    statups.add(new Pair<>(MapleBuffStat.MESOGUARD, x));
                    break;
                case 4111001: // mesoup
                    statups.add(new Pair<>(MapleBuffStat.MESOUP, x));
                    break;
                case 4111002: // shadowpartner
                    statups.add(new Pair<>(MapleBuffStat.SHADOWPARTNER, x));
                    break;
                case 3101004: // soul arrow
                case 3201004:
                case 2311002: // mystic door
                    statups.add(new Pair<>(MapleBuffStat.SOULARROW, x));
                    break;
                case 1211003:
                case 1211004:
                case 1211005:
                case 1211006: // wk charges
                case 1211007:
                case 1211008:
                case 1221003:
                case 1221004:
                    statups.add(new Pair<>(MapleBuffStat.WK_CHARGE, x));
                    break;
                case 1101004:
                case 1101005: // booster
                case 1201004:
                case 1201005:
                case 1301004:
                case 1301005:
                case 2111005: // spell booster, do these work the same?
                case 2211005:
                case 3101002:
                case 3201002:
                case 4101003:
                case 4201002:
                case 5101006:
                case 5201003:
                //case 5121009: // speed infusion (was 5221010)
                    statups.add(new Pair<>(MapleBuffStat.BOOSTER, x));
                    break;
                case 5121009:
                    statups.add(new Pair<>(MapleBuffStat.SPEED_INFUSION, -4));
                    break;
                case 5111005: // transformation
                case 5121003: // super transformation
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                    statups.add(new Pair<>(MapleBuffStat.MDEF, (int) ret.mdef));
                    statups.add(new Pair<>(MapleBuffStat.SPEED, (int) ret.speed));
                    statups.add(new Pair<>(MapleBuffStat.JUMP, (int) ret.jump));
                    break;
                case 1101006: // rage
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                case 1121010: // enrage
                    statups.add(new Pair<>(MapleBuffStat.WATK, (int) ret.watk));
                    break;
                case 1301006: // iron will
                    statups.add(new Pair<>(MapleBuffStat.MDEF, (int) ret.mdef));
                case 1001003: // iron body
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                    break;
                case 2001003: // magic armor
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                    break;
                case 2101001: // meditation
                case 2201001: // meditation
                    statups.add(new Pair<>(MapleBuffStat.MATK, (int) ret.matk));
                    break;
                case 4101004: // haste
                case 4201003: // haste
                case 9101001: // gm haste
                    statups.add(new Pair<>(MapleBuffStat.SPEED, (int) ret.speed));
                    statups.add(new Pair<>(MapleBuffStat.JUMP, (int) ret.jump));
                    break;
                case 2301004: // bless
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                    statups.add(new Pair<>(MapleBuffStat.MDEF, (int) ret.mdef));
                case 3001003: // focus
                    statups.add(new Pair<>(MapleBuffStat.ACC, (int) ret.acc));
                    statups.add(new Pair<>(MapleBuffStat.AVOID, (int) ret.avoid));
                    break;
                case 9101003: // gm bless
                    statups.add(new Pair<>(MapleBuffStat.MATK, (int) ret.matk));
                case 3121008: // concentrate
                    statups.add(new Pair<>(MapleBuffStat.WATK, (int) ret.watk));
                    break;
                case 5001005: // Dash
                    statups.add(new Pair<>(MapleBuffStat.DASH, 1));
                    break;
                case 1101007: // pguard
                case 1201007:
                    statups.add(new Pair<>(MapleBuffStat.POWERGUARD, x));
                    break;
                case 1301007:
                case 9101008:
                    statups.add(new Pair<>(MapleBuffStat.HYPERBODYHP, x));
                    statups.add(new Pair<>(MapleBuffStat.HYPERBODYMP, ret.y));
                    break;
                case 1001: // recovery
                    statups.add(new Pair<>(MapleBuffStat.RECOVERY, x));
                    break;
                case 1111002: // combo
                    statups.add(new Pair<>(MapleBuffStat.COMBO, 1));
                    break;
                case 1004: // monster riding
                    statups.add(new Pair<>(MapleBuffStat.MONSTER_RIDING, 1));
                    break;
                case 5221006: // 4th Job - Pirate riding
                    statups.add(new Pair<>(MapleBuffStat.MONSTER_RIDING, 1));
                    statups.add(new Pair<>(MapleBuffStat.WDEF, (int) ret.wdef));
                    statups.add(new Pair<>(MapleBuffStat.MDEF, (int) ret.mdef));
                    //System.out.print("statups WDEF: " + ret.wdef + "\n");
                    //System.out.print("statups MDEF: " + ret.mdef + "\n");
                    break;
                case 1311006: //dragon roar
                    ret.hpR = -x / 100.0;
                    break;
                case 1311008: // dragon blood
                    statups.add(new Pair<>(MapleBuffStat.DRAGONBLOOD, ret.x));
                    break;
                case 1121000: // maple warrior, all classes
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
                case 3121002: // sharp eyes bow master
                case 3221002: // sharp eyes marksmen
                    // hack much (TODO is the order correct?)
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
                    //ret.duration *= 2; // freezing skills are a little strange
                    break;
                case 5211005: // Ice Splitter
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 2221003: // Ice Demon
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    //System.out.print("ret.duration, ret.x: " + ret.duration + ", " + ret.x + "\n");
                    int tempduration = ret.duration;
                    ret.duration = ret.x * 1000;
                    ret.x = tempduration;
                    //ret.duration *= 2; // freezing skills are a little strange
                    //System.out.print("ret.duration, ret.x: " + ret.duration + ", " + ret.x + "\n");
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

    /**
     * @param applyto
     * @param obj
     */
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

    private boolean applyTo(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary, Point pos) {
        int hpchange = calcHPChange(applyfrom, primary);
        int mpchange = calcMPChange(applyfrom, primary);

        if (primary) {
            if (itemConNo != 0) {
                if (sourceid != 2321006) {
                    MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemCon);
                    MapleInventoryManipulator.removeById(applyto.getClient(), type, itemCon, itemConNo, false, true);
                } else {
                    MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemCon);
                    if (applyfrom.getItemQuantity(itemCon, false) >= itemConNo) {
                        MapleInventoryManipulator.removeById(applyto.getClient(), type, itemCon, itemConNo, false, true);
                    } else {
                        applyfrom.dropMessage("You do not have enough Elans Vital to use the Resurrection skill.");
                        applyfrom.removeCooldown(sourceid);
                        applyfrom.addMPHP(0, mpCon);
                        applyfrom.getClient().getSession().write(MaplePacketCreator.enableActions());
                        return false;
                    }
                }
            }
            if (sourceid == 5101005) {
                hpchange = (int) ((double) applyfrom.getMaxHp() * (-x / 100.0));
                if (-hpchange >= applyto.getHp()) {
                    applyfrom.dropMessage("You do not have enough HP to use the MP Recovery skill.");
                    applyfrom.removeCooldown(sourceid);
                    applyfrom.getClient().getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }
                mpchange = (int) ((double) applyfrom.getMaxMp() * (y / 100.0));
            }
        }
        if (!cureDebuffs.isEmpty()) {
            for (MapleDisease debuff : cureDebuffs) {
                applyfrom.dispelDebuff(debuff);
            }
        }
        List<Pair<MapleStat, Integer>> hpmpupdate = new ArrayList<>(2);
        if (!primary && isResurrection()) {
            hpchange = applyto.getMaxHp();
            applyto.setStance(0); // TODO fix death bug, player doesnt spawn on other screen
        }
        if (isDispel() && makeChanceResult()) {
            MonsterStatus[] remove = {MonsterStatus.ACC, MonsterStatus.AVOID, MonsterStatus.WEAPON_IMMUNITY, MonsterStatus.MAGIC_IMMUNITY, MonsterStatus.SPEED};
            for (MapleMapObject _mob : applyfrom.getMap().getMapObjectsInRange(applyfrom.getPosition(), 30000 + applyfrom.getSkillLevel(SkillFactory.getSkill(sourceid)) * 1000, Collections.singletonList(MapleMapObjectType.MONSTER))) {
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
            applyto.dispelDebuffs();
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
                default: // ???!!!
                    remove = MonsterStatus.STUN;
                    break;
            }
            for (MapleMapObject _mob : applyfrom.getMap().getMapObjectsInRange(applyfrom.getPosition(), 30000 + applyfrom.getSkillLevel(SkillFactory.getSkill(sourceid)) * 3000, Collections.singletonList(MapleMapObjectType.MONSTER))) {
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
            //applyto.dispelDebuff(MapleDisease.SEDUCE);
            applyto.dispelDebuffs();
        }
        if (hpchange != 0) {
            if (hpchange < 0 && (-hpchange) > applyto.getHp()) {
                if (applyfrom != null) {
                    applyfrom.getClient().getSession().write(MaplePacketCreator.enableActions());
                }
                return false;
            }
            int newHp = applyto.getHp() + hpchange;
            if (newHp < 1) {
                newHp = 1;
            }
            applyto.setHp(newHp);
            hpmpupdate.add(new Pair<>(MapleStat.HP, applyto.getHp()));
        }
        if (mpchange != 0) {
            if (mpchange < 0 && (-mpchange) > applyto.getMp()) {
                if (applyfrom != null) {
                    applyfrom.getClient().getSession().write(MaplePacketCreator.enableActions());
                }
                return false;
            }
            applyto.setMp(applyto.getMp() + mpchange);
            hpmpupdate.add(new Pair<>(MapleStat.MP, applyto.getMp()));
        }
        applyto.getClient().getSession().write(MaplePacketCreator.updatePlayerStats(hpmpupdate, true));
        if (isResurrection() && !primary) {
            applyto.incrementDeathPenaltyAndRecalc(5);
            applyto.setExp(0);
            applyto.updateSingleStat(MapleStat.EXP, 0);
            applyto.getClient().getSession().write(MaplePacketCreator.getClock(0));
        }
        if (moveTo != -1) {
            if (applyto.getMap().getReturnMapId() != applyto.getMapId()) {
                MapleMap target;
                if (moveTo == 999999999) {
                    target = applyto.getMap().getReturnMap();
                } else {
                    target = ChannelServer.getInstance(applyto.getClient().getChannel()).getMapFactory().getMap(moveTo);
                    if (target.getId() / 10000000 != 60 && applyto.getMapId() / 10000000 != 61) {
                        if (target.getId() / 10000000 != 21 && applyto.getMapId() / 10000000 != 20) {
                            if (target.getId() / 10000000 != applyto.getMapId() / 10000000) {
                                log.info("Player {} is trying to use a return scroll to an illegal location ({}->{})", new Object[]{applyto.getName(), applyto.getMapId(), target.getId()});
                                //applyto.getClient().disconnect();
                                //return false;
                            }
                        }
                    }
                }
                applyto.changeMap(target, target.getPortal(0));
            } else {
                return false;
            }
        }
        if (isShadowClaw()) {
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
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
                MapleInventoryManipulator.removeById(applyto.getClient(), MapleInventoryType.USE, projectile, 200, false, true);
            }
        }
        if (overTime) {
            applyBuffEffect(applyfrom, applyto, primary);
        }
        if (primary && (overTime || isHeal())) {
            applyBuff(applyfrom);
        }
        if (primary && isMonsterBuff()) {
            applyMonsterBuff(applyfrom);
        }

        SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null && pos != null) {
            final MapleSummon tosummon = new MapleSummon(applyfrom, sourceid, pos, summonMovementType);
            if (!tosummon.isPuppet()) {
                applyfrom.getCheatTracker().resetSummonAttack();
            }
            applyfrom.getMap().spawnSummon(tosummon);
            applyfrom.getSummons().put(sourceid, tosummon);
            tosummon.addHP(x);
            if (isBeholder()) {
                tosummon.addHP(1);
            }
        }

        // Magic Door
        if (isMagicDoor()) {
            //applyto.cancelMagicDoor();
            Point doorPosition = new Point(applyto.getPosition());
            //doorPosition.y -= 280;
            MapleDoor door = new MapleDoor(applyto, doorPosition);
            applyto.getMap().spawnDoor(door);
            applyto.addDoor(door);
            door = new MapleDoor(door);
            applyto.addDoor(door);
            door.getTown().spawnDoor(door);
            if (applyto.getParty() != null) {
                // update town doors
                applyto.silentPartyUpdate();
            }
            applyto.disableDoor();
        } else if (isMist()) {
            Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
            MapleMist mist = new MapleMist(bounds, applyfrom, this);
            applyfrom.getMap().spawnMist(mist, getDuration(), sourceid == 2111003, false);
        }
        // Time Leap
        if (isTimeLeap()) {
            for (PlayerCoolDownValueHolder i : applyto.getAllCooldowns()) {
                if (i.skillId != 5121010) {
                    applyto.removeCooldown(i.skillId);
                }
            }
        }
        if (sourceid == 5111005) {
            List<Pair<MapleBuffStat, Integer>> pmorphstatup = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, getMorph(applyto)));
            applyto.getClient().getSession().write(MaplePacketCreator.giveBuff(sourceid, getDuration(), pmorphstatup));
        }
        return true;
    }

    private void applyBuff(MapleCharacter applyfrom) {
        if (isPartyBuff() && (applyfrom.getParty() != null || isGmBuff())) {
            Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
            List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.PLAYER));
            List<MapleCharacter> affectedp = new ArrayList<>(affecteds.size());
            MapleCharacter closestplayer = null;
            double closestdistance = Double.MAX_VALUE;
            for (MapleMapObject affectedmo : affecteds) {
                MapleCharacter affected = (MapleCharacter) affectedmo;
                //this is new and weird...
                //System.out.print("affected: " + (affected == null ? "null" : affected.getName()) + ", applyfrom: " + applyfrom.getName() + ", affected.getParty(): " + (affected == null ? "N/A" : affected.getParty().getId()) + ", applyfrom.getParty(): " + applyfrom.getParty().getId() + "\n");
                if (affected != null && isHeal() && affected != applyfrom && affected.getParty() != null && affected.getParty() == applyfrom.getParty() && affected.isAlive()) {
                    int expadd = (int) ((((double) calcHPChange(applyfrom, true) / 10.0) * ((double) applyfrom.getAbsoluteXp() * (double) applyfrom.getClient().getChannelServer().getExpRate() + ((Math.random() * 10.0) + 30.0)) * ((Math.random() * (double) applyfrom.getSkillLevel(SkillFactory.getSkill(2301002)) / 100.0) * ((double) applyfrom.getLevel() / 30.0))) / 4.0);
                    //System.out.print("applyfrom.getAbsoluteXp(), applyfrom.getClient().getChannelServer().getExpRate(), applyfrom.getSkillLevel(SkillFactory.getSkill(2301002)), applyfrom.getLevel(): " + applyfrom.getAbsoluteXp() + ", " + applyfrom.getClient().getChannelServer().getExpRate() + ", " + applyfrom.getSkillLevel(SkillFactory.getSkill(2301002)) + ", " + applyfrom.getLevel() + "\n");
                    //System.out.print("expadd: " + expadd + "\n");
                    if (affected.getHp() < affected.getMaxHp() - affected.getMaxHp() / 20) {
                        //System.out.print("ooga" + "\n");
                        applyfrom.gainExp(expadd, true, false, false);
                    }
                }
                if (affected != applyfrom && (isGmBuff() || applyfrom.getParty().equals(affected.getParty()))) {
                    boolean isResurrection = isResurrection();
                    if ((isResurrection && !affected.isAlive()) || (!isResurrection && affected.isAlive())) {
                        if (isResurrection) {
                            //System.out.print("Name: " + affected.getName() + ", distancesq: " + applyfrom.getPosition().distanceSq(affected.getPosition()) + "\n");
                            if (applyfrom.getPosition().distanceSq(affected.getPosition()) < closestdistance) {
                                closestdistance = applyfrom.getPosition().distanceSq(affected.getPosition());
                                closestplayer = affected;
                                //System.out.print("closest player name: " + affected.getName() + "\n");
                            }
                        } else {
                            affectedp.add(affected);
                        }
                    }
                    boolean isTimeLeap = isTimeLeap();
                    if (isTimeLeap) {
                        for (PlayerCoolDownValueHolder i : affected.getAllCooldowns()) {
                            if (i.skillId != 5121010) {
                                affected.removeCooldown(i.skillId);
                            }
                        }
                    }
                }
            }
            if (closestplayer != null) {
                //System.out.print("closestplayer != null, closestplayer.getName(): " + closestplayer.getName() + "\n");
                affectedp.add(closestplayer);
            } else {
                //System.out.print("closestplayer == null\n");
            }
            for (MapleCharacter affected : affectedp) {
                // TODO actually heal (and others) shouldn't recalculate everything
                // for heal this is an actual bug since heal hp is decreased with the number
                // of affected players
                //System.out.print("affected.getName(): " + affected.getName() + "\n");
                applyTo(applyfrom, affected, false, null);
                affected.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 2));
                affected.getMap().broadcastMessage(affected, MaplePacketCreator.showBuffeffect(affected.getId(), sourceid, 2, (byte) 3), false);
            }
        }
    }

    private void applyMonsterBuff(MapleCharacter applyfrom) {
        Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
        List<MapleMapObject> affected = applyfrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.MONSTER));
        ISkill skill_ = SkillFactory.getSkill(sourceid);
        Map<MonsterStatus, Integer> localmonsterstatus = getMonsterStati();
        // /*
        if (skill_.getId() == 1201006) { // Handing Threaten's scaling with level
            int basethreatendebuff = skill_.getEffect(applyfrom.getSkillLevel(skill_)).getX();
            int threatendebuffscale = applyfrom.getLevel() - 30;
            if (localmonsterstatus.containsKey(MonsterStatus.WATK)) {
                //System.out.print("(w) basethreatendebuff, threatendebuffscale: " + basethreatendebuff + ", " + threatendebuffscale + "\n");
                localmonsterstatus.put(MonsterStatus.WATK, basethreatendebuff - threatendebuffscale);
            }
            if (localmonsterstatus.containsKey(MonsterStatus.MATK)) {
                //System.out.print("(m) basethreatendebuff, threatendebuffscale: " + basethreatendebuff + ", " + threatendebuffscale + "\n");
                localmonsterstatus.put(MonsterStatus.MATK, basethreatendebuff - threatendebuffscale);
            }
        }
        // */
        int i = 0;
        for (MapleMapObject mo : affected) {
            MapleMonster monster = (MapleMonster) mo;
            if (makeChanceResult()) {
                //System.out.print("MapleStatEffect.applyMonsterBuff()\n");
                monster.applyStatus(applyfrom, new MonsterStatusEffect(localmonsterstatus, skill_, false), isPoison(), getDuration());
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

    public void silentApplyBuff(MapleCharacter chr, long starttime) {
        int localDuration = duration;
        localDuration = alchemistModifyVal(chr, localDuration, false);
        CancelEffectAction cancelAction = new CancelEffectAction(chr, this, starttime);
        ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + localDuration) - System.currentTimeMillis()));
        chr.registerEffect(this, starttime, schedule);

        SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null) {
            final MapleSummon tosummon = new MapleSummon(chr, sourceid, chr.getPosition(), summonMovementType);
            if (!tosummon.isPuppet()) {
                chr.getMap().spawnSummon(tosummon);
                chr.getCheatTracker().resetSummonAttack();
                chr.getSummons().put(sourceid, tosummon);
                tosummon.addHP(x);
            }
        }
    }

    private void applyBuffEffect(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary) {
        boolean wasship = false;
        if (sourceid != 5221006) {
            if (!this.isMonsterRiding()) {
                applyto.cancelEffect(this, true, -1);
            }
        } else {
            if (applyto.getStatForBuff(MapleBuffStat.MONSTER_RIDING) != null) {
                applyto.cancelEffect(this, true, -1);
                wasship = true;
            }
        }
        List<Pair<MapleBuffStat, Integer>> localstatups = statups;
        int localDuration = duration;
        int localsourceid = sourceid;
        int localX = x;
        int localY = y;
        int seconds = localDuration / 1000;
        MapleMount givemount = null;
        if (isHolySymbol()) {
            //System.out.print("applyfrom.getName(), applyto.getName(): " + applyfrom.getName() + ", " + applyto.getName() + "\n");
            //System.out.print("applyfrom.getId(), applyto.getId(): " + applyfrom.getId() + ", " + applyto.getId() + "\n");
            if (applyfrom.getId() != applyto.getId()) { // Making sure that other players don't get the watk buff.
                for (int i = 0; i < localstatups.size(); ++i) {
                    Pair<MapleBuffStat, Integer> statup = localstatups.get(i);
                    if (statup.getLeft() == MapleBuffStat.WATK) {
                        localstatups.remove(i);
                        break;
                    }
                }
            } else { // Setting the actual watk value for Holy Symbol
                boolean alreadyhaswatk = false;
                for (int i = 0; i < localstatups.size(); ++i) {
                    Pair<MapleBuffStat, Integer> statup = localstatups.get(i);
                    if (statup.getLeft() == MapleBuffStat.WATK) {
                        alreadyhaswatk = true;
                        // 3 * skillLevel + casterLevel / 1.5
                        //System.out.print("statup.getLeft() == MapleBuffStat.WATK\n");
                        int watt = (int) ((3 * applyfrom.getSkillLevel(SkillFactory.getSkill(localsourceid))) + (applyfrom.getLevel() / 1.5));
                        localstatups.set(i, new Pair<>(MapleBuffStat.WATK, watt));
                        //System.out.print("localstatups.get(i): " + localstatups.get(i) + "\n");
                        break;
                    }
                }
                if (!alreadyhaswatk) {
                    int watt = (int) ((3 * applyfrom.getSkillLevel(SkillFactory.getSkill(localsourceid))) + (applyfrom.getLevel() / 1.5));
                    localstatups.add(0, new Pair<>(MapleBuffStat.WATK, watt));
                }
            }
        }
        if (getSourceId() == 2301003) {
            IItem weapon_item = applyto.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) - 11);
            MapleWeaponType weapon = null;
            if (weapon_item != null) {
                weapon = MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId());
            }
            //System.out.print("weapon: " + (weapon != null ? weapon.toString() : "null") + "\n");
            if (!(weapon == MapleWeaponType.BLUNT1H || weapon == MapleWeaponType.BLUNT2H)) {
                for (int i = 0; i < localstatups.size(); ++i) {
                    Pair<MapleBuffStat, Integer> localstatup = localstatups.get(i);
                    if (localstatup.getLeft() == MapleBuffStat.BOOSTER) {
                        localstatups.remove(i);
                    }
                }
            }
        }
        if (isMonsterRiding()) {
            int ridingLevel = 0; // Mount ID.
            IItem mount = applyfrom.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
            if (mount != null) {
                ridingLevel = mount.getItemId();
            }
            if (sourceid == 5221006) {
                ridingLevel = 1932000;
                givemount = new MapleMount(applyto, ridingLevel, 5221006);
                if (wasship) {
                    givemount.setActive(false);
                } else {
                    applyto.Mount(ridingLevel, 5221006);
                    givemount.setActive(true);
                }
                if (applyto.getBattleshipHp() < 1) {
                    applyto.setBattleshipHp(applyto.getBattleshipMaxHp());
                }
            } else {
                if (applyto.getMount() == null) {
                    applyto.Mount(ridingLevel, sourceid);
                }
                givemount = applyto.getMount();
                givemount.startSchedule();
                givemount.setActive(true);
            }
            localDuration = sourceid;
            localsourceid = ridingLevel;
            if (sourceid != 5221006) {
                localstatups = Collections.singletonList(new Pair<>(MapleBuffStat.MONSTER_RIDING, 0));
            }
        }
        if (isPirateMorph()) {
            //localstatups = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MORPH, getMorph(applyto)));
            //localstatups.addAll(statups);
            List<Pair<MapleBuffStat, Integer>> pmorphstatup = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, getMorph(applyto)));
            applyto.getClient().getSession().write(MaplePacketCreator.giveBuff(localsourceid, localDuration, pmorphstatup));
        }
        if (isRecovery()) {
            applyto.setHp(applyto.getHp() + localstatups.get(0).getRight() * 6);
            applyto.updateSingleStat(MapleStat.HP, applyto.getHp());
        }
        if (primary) {
            localDuration = alchemistModifyVal(applyfrom, localDuration, false);
        }
        if (!localstatups.isEmpty()) {
            if (isDash()) {
                if (!applyto.getJob().isA(MapleJob.PIRATE)) {
                    applyto.changeSkillLevel(SkillFactory.getSkill(sourceid), 0, 10);
                } else {
                    localstatups = Collections.singletonList(new Pair<>(MapleBuffStat.DASH, 1));
                    applyto.getClient().getSession().write(MaplePacketCreator.giveDash(localstatups, localX, localY, seconds));
                }
            } else if (isInfusion()) {
                applyto.getClient().getSession().write(MaplePacketCreator.giveInfusion(seconds, x));
            } else {
                applyto.getClient().getSession().write(MaplePacketCreator.giveBuff((skill ? localsourceid : -localsourceid), localDuration, localstatups));
            }
            /*
            if (sourceid == 5221006) { // Battleship
                List<Pair<MapleBuffStat, Integer>> relocalstatups = statups;
                List<Pair<MapleBuffStat, Integer>> battleshipdefstatups = new ArrayList<>(2);
                for (int i = 0; i < relocalstatups.size(); ++i) {
                    Pair<MapleBuffStat, Integer> localstatup = relocalstatups.get(i);
                    if (localstatup.getLeft() == MapleBuffStat.WDEF || localstatup.getLeft() == MapleBuffStat.MDEF) {
                        battleshipdefstatups.add(localstatup);
                    }
                }
                applyto.getClient().getSession().write(MaplePacketCreator.giveBuff((skill ? localsourceid : -localsourceid), localDuration, battleshipdefstatups));
            }
            */
        } else {
            // Apply empty buff icon.
            applyto.getClient().getSession().write(MaplePacketCreator.giveBuffTest((skill ? localsourceid : -localsourceid), localDuration, 0));
        }
        if (isMonsterRiding()) {
            if (givemount != null && givemount.getItemId() != 0) {
                applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showMonsterRiding(applyto.getId(), Collections.singletonList(new Pair<>(MapleBuffStat.MONSTER_RIDING, 1)), givemount), false);
            }
            localDuration = duration;
        }
        if (isDs()) {
            List<Pair<MapleBuffStat, Integer>> dsstat = Collections.singletonList(new Pair<>(MapleBuffStat.DARKSIGHT, 0));
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.giveForeignBuff(applyto.getId(), dsstat, this), false);
        }
        if (isCombo()) {
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.COMBO, 1));
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.giveForeignBuff(applyto.getId(), stat, this), false);
        }
        if (isShadowPartner()) {
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.SHADOWPARTNER, 0));
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.giveForeignBuff(applyto.getId(), stat, this), false);
        }
        if (isSoulArrow()) {
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.SOULARROW, 0));
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.giveForeignBuff(applyto.getId(), stat, this), false);
        }
        if (isEnrage()) {
            applyto.handleOrbconsume();
        }
        if (isMorph()) {
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, getMorph(applyto)));
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.giveForeignBuff(applyto.getId(), stat, this), false);
        }
        if (isOakBarrel()) {
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, 1002));
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.giveForeignBuff(applyto.getId(), stat, this), false);
        }
        if (isTimeLeap()) {
            for (PlayerCoolDownValueHolder i : applyto.getAllCooldowns()) {
                if (i.skillId != 5121010) {
                    applyto.removeCooldown(i.skillId);
                }
            }
        }
        if (!localstatups.isEmpty()) {
            long starttime = System.currentTimeMillis();
            CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
            ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, localDuration);
            applyto.registerEffect(this, starttime, schedule);
        }
        if (primary) {
            if (isDash()) {
                applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showDashEffecttoOthers(applyto.getId(), localX, localY, seconds), false);
            } else if (isInfusion()) {
                applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.giveForeignInfusion(applyto.getId(), x, seconds), false);
            } else {
                applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showBuffeffect(applyto.getId(), sourceid, 1, (byte) 3), false);
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
        // actually receivers probably never get any hp when it's not heal but whatever
        if (primary) {
            if (hpCon != 0) {
                hpchange -= hpCon;
            }
        }
        if (isChakra()) {
            hpchange += makeHealHP(getY() / 100.0, applyfrom.getTotalLuk(), 2.3, 3.5);
        }
        //System.out.print("hpchange: " + hpchange + "\n");
        return hpchange;
    }

    private int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        int maxHeal = (int) (stat * upperfactor * rate);
        int minHeal = (int) (stat * lowerfactor * rate);
        //System.out.print("maxHeal, minHeal: " + maxHeal + ", " + minHeal + "\n");
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
        if (lt == null || rb == null) {
            return false;
        }
        return !((sourceid >= 1211003 && sourceid <= 1211008) || sourceid == 1221003 || sourceid == 1221004);
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
