package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.Element;
import net.sf.odinms.server.life.ElementalEffectiveness;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapItem;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.LittleEndianAccessor;

import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class AbstractDealDamageHandler extends AbstractMaplePacketHandler {
    //private static Logger log = LoggerFactory.getLogger(AbstractDealDamageHandler.class);

    public class AttackInfo {
        public int numAttacked, numDamage, numAttackedAndDamage;
        public int skill, stance, direction, charge;
        public List<Pair<Integer, List<Integer>>> allDamage;
        public boolean isHH = false;
        public int speed = 4;

        private MapleStatEffect getAttackEffect(MapleCharacter chr, ISkill theSkill) {
            ISkill mySkill = theSkill;
            if (mySkill == null) {
                mySkill = SkillFactory.getSkill(skill);
            }
            int skillLevel = chr.getSkillLevel(mySkill);
            if (skillLevel == 0) {
                return null;
            }
            return mySkill.getEffect(skillLevel);
        }

        public MapleStatEffect getAttackEffect(MapleCharacter chr) {
            return getAttackEffect(chr, null);
        }
    }

    protected synchronized void applyAttack(AttackInfo attack, MapleCharacter player, int attackCount) {
        player.getCheatTracker().resetHPRegen();
        //player.getCheatTracker().checkAttack(attack.skill);
        
        ISkill theSkill = null;
        MapleStatEffect attackEffect = null;
        if (attack.skill != 0) {
            theSkill = SkillFactory.getSkill(attack.skill);
            if (theSkill != null) {
                attackEffect = attack.getAttackEffect(player, theSkill);
            }
            if (attackEffect == null) {
                player.getClient().getSession().write(MaplePacketCreator.enableActions());
                return;
            } else if (attackEffect.getSourceId() == 5211005 && attack.charge == 1) { // Ice Splitter
                attackEffect.doubleDuration();
            }
            if (attack.skill != 2301002) {
                if (player.isAlive()) {
                    attackEffect.applyTo(player);
                } else {
                    player.getClient().getSession().write(MaplePacketCreator.enableActions());
                }
            } else if (theSkill.isGMSkill() && !player.isGM()) {
                player.getClient().getSession().close();
                return;
            }
        }
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        // Meso explosion has a variable bullet count
        if (attackCount != attack.numDamage && attack.skill != 4211006) {
            player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT, attack.numDamage + "/" + attackCount);
            return;
        }
        int totDamage = 0;
        final MapleMap map = player.getMap();

        if (attack.skill == 4211006 && attack.allDamage != null) { // Meso explosion
            int delay = 0;
            for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
                if (oned != null && oned.getLeft() != null) {
                    MapleMapObject mapObject = map.getMapObject(oned.getLeft());
                    if (mapObject != null && mapObject.getType() == MapleMapObjectType.ITEM) {
                        final MapleMapItem mapItem = (MapleMapItem) mapObject;
                        if (mapItem != null && mapItem.getMeso() >= 10) {
                            synchronized (mapItem) {
                                if (mapItem.isPickedUp()) {
                                    player.getClient().getSession().write(MaplePacketCreator.enableActions());
                                    return;
                                }
                                TimerManager.getInstance().schedule(() -> {
                                    map.removeMapObject(mapItem);
                                    map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapItem.getObjectId(), 4, 0), mapItem.getPosition());
                                    mapItem.setPickedUp(true);
                                }, delay);
                                delay += 100;
                            }
                        } else if (mapItem != null && mapItem.getMeso() == 0) {
                            player.getCheatTracker().registerOffense(CheatingOffense.ETC_EXPLOSION);
                            return;
                        }
                    } else if (mapObject != null && mapObject.getType() != MapleMapObjectType.MONSTER) {
                        player.getCheatTracker().registerOffense(CheatingOffense.EXPLODING_NONEXISTANT);
                        return;
                    }
                }
            }
        }

        if (attack.allDamage == null) {
            player.getClient().getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
            if (oned == null || oned.getLeft() == null) {
                continue;
            }
            MapleMonster monster = map.getMonsterByOid(oned.getLeft());

            if (monster != null && oned.getRight() != null) {
                int totDamageToOneMonster = 0;
                for (Integer eachd : oned.getRight()) {
                    if (eachd != null) totDamageToOneMonster += eachd;
                }
                totDamage += totDamageToOneMonster;

                player.checkMonsterAggro(monster);

                // Antihack
                if (totDamageToOneMonster > attack.numDamage + 1) {
                    int dmgCheck = player.getCheatTracker().checkDamage(totDamageToOneMonster);
                    if (dmgCheck > 5 && totDamageToOneMonster < 999999 && monster.getId() < 9500317 && monster.getId() > 9500319) {
                        player.getCheatTracker().registerOffense(CheatingOffense.SAME_DAMAGE, dmgCheck + " times: " + totDamageToOneMonster);
                    }
                }
                if (totDamageToOneMonster >= 12000000) {
                    AutobanManager.getInstance().autoban
                    (player.getClient(),"XSource| " + player.getName() + " dealt " + totDamageToOneMonster + " to monster " + monster.getId() + ".");
                }

                double distance = player.getPosition().distanceSq(monster.getPosition());
                if (distance > 400000.0) { // 600^2, 550 is approximately the range of ultimates
                    player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, Double.toString(Math.sqrt(distance)));
                }

                if (attack.skill == 2301002 && !monster.getUndead()) {
                    player.getCheatTracker().registerOffense(CheatingOffense.HEAL_ATTACKING_UNDEAD);
                    return;
                }

                // Pickpocket
                if (player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null) {
                    switch (attack.skill) {
                        case 0:
                        case 4001334:
                        case 4201005:
                        case 4211002:
                        case 4211004:
                        case 4211001:
                        case 4221003:
                        case 4221007:
                            handlePickPocket(player, monster, oned);
                            break;
                    }
                }

                // Effects
                switch (attack.skill) {
                    case 1221011: // Sanctuary
                        if (attack.isHH) {
                            // TODO: Min damage still needs calculated. Using -20% as minimum damage in the meantime... seems to work.
                            int HHDmg = player.calculateMaxBaseDamage(player.getTotalWatk()) * (theSkill.getEffect(player.getSkillLevel(theSkill)).getDamage() / 100);
                            HHDmg = (int) (Math.floor(Math.random() * (HHDmg - HHDmg * .80) + HHDmg * .80));
                            map.damageMonster(player, monster, HHDmg);
                        }
                        break;
                    case 3221007: // Snipe
                        //totDamageToOneMonster = (int) (95000 + Math.random() * 5000);
                        int upperRange = player.getCurrentMaxBaseDamage();
                        int lowerRange = player.calculateMinBaseDamage(player);
                        totDamageToOneMonster = (int) ((lowerRange + Math.random() * (upperRange - lowerRange + 1.0d)) * 45.0d);
                        break;
                    case 4101005: // Drain
                    case 5111004: // Energy drain
                        int gainhp = (int) ((double) totDamageToOneMonster * (double) SkillFactory.getSkill(attack.skill).getEffect(player.getSkillLevel(SkillFactory.getSkill(attack.skill))).getX() / 100.0);
                        gainhp = Math.min(monster.getMaxHp(), Math.min(gainhp, player.getMaxHp() / 2));
                        player.addHP(gainhp);
                        break;
                    case 2121003: // Fire Demon
                        {
                            ISkill fireDemon = SkillFactory.getSkill(2121003);
                            MapleStatEffect fireDemonEffect = fireDemon.getEffect(player.getSkillLevel(fireDemon));
                            monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK, fireDemonEffect.getDuration());
                            monster.applyFlame(player, fireDemon, fireDemonEffect.getDuration(), false);
                        }
                        break;
                    case 2221003: // Ice Demon
                        {
                            ISkill iceDemon = SkillFactory.getSkill(2221003);
                            MapleStatEffect iceDemonEffect = iceDemon.getEffect(player.getSkillLevel(iceDemon));
                            monster.setTempEffectiveness(Element.FIRE, ElementalEffectiveness.WEAK, iceDemonEffect.getX());
                        }
                        break;
                    case 5211004: // Flamethrower
                        ISkill flamethrower = SkillFactory.getSkill(5211004);
                        MapleStatEffect flameEffect = flamethrower.getEffect(player.getSkillLevel(flamethrower));
                        for (int i = 0; i < attackCount; ++i) {
                            monster.applyFlame(player, flamethrower, flameEffect.getDuration(), attack.charge == 1);
                        }
                        break;
                    default:
                        // Passives' attack bonuses
                        if (totDamageToOneMonster > 0 && monster.isAlive()) {
                            if (player.getBuffedValue(MapleBuffStat.BLIND) != null) {
                                if (SkillFactory.getSkill(3221006).getEffect(player.getSkillLevel(SkillFactory.getSkill(3221006))).makeChanceResult()) {
                                    MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, SkillFactory.getSkill(3221006).getEffect(player.getSkillLevel(SkillFactory.getSkill(3221006))).getX()), SkillFactory.getSkill(3221006), false);
                                    monster.applyStatus(player, monsterStatusEffect, false, SkillFactory.getSkill(3221006).getEffect(player.getSkillLevel(SkillFactory.getSkill(3221006))).getY() * 1000);
                                }
                            }
                            if (player.getBuffedValue(MapleBuffStat.HAMSTRING) != null) {
                                if (SkillFactory.getSkill(3121007).getEffect(player.getSkillLevel(SkillFactory.getSkill(3121007))).makeChanceResult()) {
                                    MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, SkillFactory.getSkill(3121007).getEffect(player.getSkillLevel(SkillFactory.getSkill(3121007))).getX()), SkillFactory.getSkill(3121007), false);
                                    monster.applyStatus(player, monsterStatusEffect, false, SkillFactory.getSkill(3121007).getEffect(player.getSkillLevel(SkillFactory.getSkill(3121007))).getY() * 1000);
                                }
                            }
                            if (player.getJob().isA(MapleJob.WHITEKNIGHT)) {
                                int[] charges = {1211005, 1211006};
                                for (int charge : charges) {
                                    if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, SkillFactory.getSkill(charge))) {
                                        final ElementalEffectiveness iceEffectiveness = monster.getEffectiveness(Element.ICE);
                                        if (iceEffectiveness == ElementalEffectiveness.NORMAL || iceEffectiveness == ElementalEffectiveness.WEAK) {
                                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.FREEZE, 1), SkillFactory.getSkill(charge), false);
                                            monster.applyStatus(player, monsterStatusEffect, false, SkillFactory.getSkill(charge).getEffect(player.getSkillLevel(SkillFactory.getSkill(charge))).getY() * 2000);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                }
                
                // Venom
                if (player.getSkillLevel(SkillFactory.getSkill(4120005)) > 0) {
                    MapleStatEffect venomEffect = SkillFactory.getSkill(4120005).getEffect(player.getSkillLevel(SkillFactory.getSkill(4120005)));
                    for (int i = 0; i < attackCount; ++i) {
                        if (venomEffect.makeChanceResult()) {
                            if (monster.getVenomMulti() < 3) {
                                monster.setVenomMulti((monster.getVenomMulti() + 1));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), SkillFactory.getSkill(4120005), false);
                                monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
                            }
                        }
                    }
                } else if (player.getSkillLevel(SkillFactory.getSkill(4220005)) > 0) {
                    MapleStatEffect venomEffect = SkillFactory.getSkill(4220005).getEffect(player.getSkillLevel(SkillFactory.getSkill(4220005)));
                    for (int i = 0; i < attackCount; ++i) {
                        if (venomEffect.makeChanceResult()) {
                            if (monster.getVenomMulti() < 3) {
                                monster.setVenomMulti((monster.getVenomMulti() + 1));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), SkillFactory.getSkill(4220005), false);
                                monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
                            }
                        }
                    }
                }
                if (totDamageToOneMonster > 0 && attackEffect != null && !attackEffect.getMonsterStati().isEmpty()) {
                    if (attackEffect.makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect;
                        switch (attack.skill) {
                            case 4121003:
                            case 4221003:
                                // Fixing Taunt skill
                                Map<MonsterStatus, Integer> tauntstati = new LinkedHashMap<>(3);
                                tauntstati.put(MonsterStatus.WDEF, 300);
                                tauntstati.put(MonsterStatus.MDEF, 300);
                                tauntstati.put(MonsterStatus.SHOWDOWN, player.getSkillLevel(SkillFactory.getSkill(attack.skill)));
                                monsterStatusEffect = new MonsterStatusEffect(tauntstati, SkillFactory.getSkill(attack.skill), false);
                                break;
                            case 4001002:
                                // Handle Disorder's scaling with player level
                                Map<MonsterStatus, Integer> disorderstati = attackEffect.getMonsterStati();
                                if (disorderstati.containsKey(MonsterStatus.WATK)) {
                                    disorderstati.put(MonsterStatus.WATK, disorderstati.get(MonsterStatus.WATK) - (player.getLevel() / 2));
                                }
                                if (disorderstati.containsKey(MonsterStatus.WDEF)) {
                                    disorderstati.put(MonsterStatus.WDEF, disorderstati.get(MonsterStatus.WDEF) - (player.getLevel() / 2));
                                }
                                monsterStatusEffect = new MonsterStatusEffect(disorderstati, theSkill, false);
                                break;
                            default:
                                monsterStatusEffect = new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, false);
                                break;
                        }
                        monster.applyStatus(player, monsterStatusEffect, attackEffect.isPoison(), attackEffect.getDuration());
                    }
                }

                // Apply attack
                if (!attack.isHH) {
                    map.damageMonster(player, monster, totDamageToOneMonster);
                }
            }
        }
        if (totDamage > 1) {
            player.getCheatTracker().setAttacksWithoutHit(player.getCheatTracker().getAttacksWithoutHit() + 1);
            final int offenseLimit;
            switch (attack.skill) {
                case 3121004:
                case 5221004:
                    offenseLimit = 100;
                    break;
                default:
                    offenseLimit = 500;
                    break;
            }
            if (player.getCheatTracker().getAttacksWithoutHit() > offenseLimit) {
                player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, Integer.toString(player.getCheatTracker().getAttacksWithoutHit()));
            }
        }
    }
	
    private void handlePickPocket(MapleCharacter player, MapleMonster monster, Pair<Integer, List<Integer>> oned) {
        int delay = 0;
        int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET);
        int reqdamage = 20000;
        Point monsterPosition = monster.getPosition();
        ISkill pickpocket = SkillFactory.getSkill(4211003);

        for (Integer eachd : oned.getRight()) {
            if (pickpocket.getEffect(player.getSkillLevel(pickpocket)).makeChanceResult()) {
                double perc = (double) eachd / (double) reqdamage;

                final int todrop = Math.min((int) Math.max(perc * (double) maxmeso, (double) 1), maxmeso);
                final MapleMap tdmap = player.getMap();
                final Point tdpos = new Point(
                    (int) (monsterPosition.getX() + (Math.random() * 100) - 50),
                    (int) (monsterPosition.getY())
                );
                final MapleMonster tdmob = monster;
                final MapleCharacter tdchar = player;

                TimerManager.getInstance().schedule(() ->
                    tdmap.spawnMesoDrop(todrop, todrop, tdpos, tdmob, tdchar, false),
                    delay
                );

                delay += 100;
            }
        }
    }
    
    public AttackInfo parseDamage(LittleEndianAccessor lea, boolean ranged) {
        AttackInfo ret = new AttackInfo();
        lea.readByte();
        ret.numAttackedAndDamage = lea.readByte();
        ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
        ret.numDamage = ret.numAttackedAndDamage & 0xF;
        ret.allDamage = new ArrayList<>(6);
        ret.skill = lea.readInt();
        switch (ret.skill) {
            case 2121001:
            case 2221001:
            case 2321001:
            case 5101004:
            case 5201002:
                ret.charge = lea.readInt();
                break;
            default:
                ret.charge = 0;
                break;
        }
        if (ret.skill == 1221011) {
            ret.isHH = true;
        }
        lea.readByte();
        ret.stance = lea.readByte();
        if (ret.skill == 4211006) {
            return parseMesoExplosion(lea, ret);
        }
        if (ranged) {
            lea.readByte();
            ret.speed = lea.readByte();
            lea.readByte();
            ret.direction = lea.readByte(); // Contains direction on some 4th job skills
            lea.skip(7);
            // Hurricane and pierce have extra 4 bytes
            switch (ret.skill) {
                case 3121004:
                case 3221001:
                case 5221004:
                    lea.skip(4);
                    break;
                default:
                    break;
            }
        } else {
            lea.readByte();
            ret.speed = lea.readByte();
            lea.skip(4);
        //  if (ret.skill == 5201002) {
        //      lea.skip(4);
        //  }
        }
        for (int i = 0; i < ret.numAttacked; ++i) {
            int oid = lea.readInt();
            // System.out.println("Unk2: " + HexTool.toString(lea.read(14)));
            lea.skip(14);
            List<Integer> allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < ret.numDamage; ++j) {
                int damage = lea.readInt();
                if (ret.skill == 3221007) {
                    damage += 0x80000000; // Critical damage = 0x80000000 + damage
                }
                allDamageNumbers.add(damage);
            }
            if (ret.skill != 5221004) {
                lea.skip(4);
            }
            ret.allDamage.add(new Pair<>(oid, allDamageNumbers));
        }
        return ret;
    }

    public AttackInfo parseMesoExplosion(LittleEndianAccessor lea, AttackInfo ret) {
        if (ret.numAttackedAndDamage == 0) {
            lea.skip(10);
            int bullets = lea.readByte();
            for (int j = 0; j < bullets; ++j) {
                int mesoid = lea.readInt();
                lea.skip(1);
                ret.allDamage.add(new Pair<>(mesoid, null));
            }
            return ret;
        } else {
            lea.skip(6);
        }
        for (int i = 0; i < ret.numAttacked + 1; ++i) {
            int oid = lea.readInt();
            if (i < ret.numAttacked) {
                lea.skip(12);
                int bullets = lea.readByte();
                List<Integer> allDamageNumbers = new ArrayList<>();
                for (int j = 0; j < bullets; ++j) {
                    int damage = lea.readInt();
                    allDamageNumbers.add(damage);
                }
                ret.allDamage.add(new Pair<>(oid, allDamageNumbers));
                lea.skip(4);

            } else {
                int bullets = lea.readByte();
                for (int j = 0; j < bullets; ++j) {
                    int mesoid = lea.readInt();
                    lea.skip(1);
                    ret.allDamage.add(new Pair<>(mesoid, null));
                }
            }
        }
        return ret;
    }
}
