package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.Element;
import net.sf.odinms.server.life.ElementalEffectiveness;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class CloseRangeDamageHandler extends AbstractDealDamageHandler {

    private boolean isFinisher(int skillId) {
        return skillId >= 1111003 && skillId <= 1111006;
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        AttackInfo attack = parseDamage(slea, false);
        MapleCharacter player = c.getPlayer();
        player.resetAfkTime();
        MaplePacket packet = MaplePacketCreator.closeRangeAttack(player.getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.speed);
        player.getMap().broadcastMessage(player, packet, false, true);
        int numFinisherOrbs = 0;
        Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO);
        if (isFinisher(attack.skill)) {
            if (comboBuff != null) {
                numFinisherOrbs = comboBuff - 1;
            }
            player.handleOrbconsume();
        } else if (attack.numAttacked > 0) {
            // Handle combo orb gain.
            if (comboBuff != null) {
                if (attack.skill != 1111008) { // Shout should not give orbs.
                    player.handleOrbgain();
                }
            } else if ((player.getJob().equals(MapleJob.BUCCANEER) || player.getJob().equals(MapleJob.MARAUDER)) && player.getSkillLevel(SkillFactory.getSkill(5110001)) > 0) {
                for (int i = 0; i < attack.numAttacked; ++i) {
                    player.handleEnergyChargeGain();
                }
            }
        }

        IItem weaponItem = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        MapleWeaponType weapon = null;
        if (weaponItem != null) {
            weapon = MapleItemInformationProvider.getInstance().getWeaponType(weaponItem.getItemId());
        }
        if (weapon == MapleWeaponType.BLUNT1H || weapon == MapleWeaponType.BLUNT2H) {
            if (player.getBuffedValue(MapleBuffStat.MANA_REFLECTION) != null && player.getSkillLevel(SkillFactory.getSkill(2321002)) > 0) {
                double mrmultiplier = 1.0 + (double) player.getSkillLevel(SkillFactory.getSkill(2321002)) * 0.05;
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    List<Integer> additionaldmg = new ArrayList<>();
                    List<Integer> newdmg = new ArrayList<>();
                    for (Integer dmgnumber : dmg.getRight()) {
                        additionaldmg.add((int) (dmgnumber * (mrmultiplier - 1.0)));
                        newdmg.add((int) (dmgnumber * mrmultiplier));
                    }
                    attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                    for (Integer additionald : additionaldmg) {
                        player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(dmg.getLeft(), additionald), true);
                    }
                }
            }

            // Handing crits from Battle Priest's MP Eater passive
            boolean iscrit;
            int mpeaterlevel = player.getSkillLevel(SkillFactory.getSkill(2300000));
            if (mpeaterlevel > 0) {
                double chance = (20.0d + 2.0d * (double) mpeaterlevel) / 100.0d;
                iscrit = Math.random() < chance;
            } else {
                iscrit = false;
            }

            if (iscrit) {
                double critmultiplier = 1.5d + 0.08d * (double) mpeaterlevel;
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    List<Integer> additionaldmg = new ArrayList<>();
                    List<Integer> newdmg = new ArrayList<>();
                    for (Integer dmgnumber : dmg.getRight()) {
                        additionaldmg.add((int) (dmgnumber * (critmultiplier - 1.0d)));
                        newdmg.add((int) (dmgnumber * critmultiplier));
                    }
                    attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                    for (Integer additionald : additionaldmg) {
                        player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(dmg.getLeft(), additionald), true);
                    }
                }
            }
        }

        ISkill skillused = SkillFactory.getSkill(attack.skill);
        if (skillused != null && skillused.getElement() != Element.NEUTRAL) {
            for (int i = 0; i < attack.allDamage.size(); ++i) {
                Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                MapleMonster monster = null;
                if (dmg != null) {
                    monster = player.getMap().getMonsterByOid(dmg.getLeft());
                }
                if (monster != null) {
                    ElementalEffectiveness ee = monster.getAddedEffectiveness(skillused.getElement());
                    if ((ee == ElementalEffectiveness.WEAK || ee == ElementalEffectiveness.IMMUNE) && monster.getEffectiveness(skillused.getElement()) == ElementalEffectiveness.WEAK) {
                        continue;
                    }
                    double multiplier;
                    List<Integer> additionaldmg = new ArrayList<>();
                    List<Integer> newdmg = new ArrayList<>();
                    switch (ee) {
                        case WEAK:
                            multiplier = 1.5d;
                            break;
                        case STRONG:
                            multiplier = 0.5d;
                            break;
                        case IMMUNE:
                            multiplier = 0.0d;
                            break;
                        default:
                            multiplier = 1.0d;
                            break;
                    }
                    if (multiplier != 1.0d) {
                        for (Integer dmgnumber : dmg.getRight()) {
                            additionaldmg.add((int) (dmgnumber * (multiplier - 1.0)));
                            newdmg.add((int) (dmgnumber * multiplier));
                        }
                        attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                        for (Integer additionald : additionaldmg) {
                            player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(dmg.getLeft(), additionald), true);
                        }
                    }
                }
            }
        }

        if (weapon == MapleWeaponType.BLUNT1H || weapon == MapleWeaponType.BLUNT2H) {
            if (player.hasMagicArmor()) { // If player has Magic Armor on them currently.
                final double magicarmorradius = 70000.0; // Squared distance
                int magicarmorlevel = player.getSkillLevel(SkillFactory.getSkill(2001003));
                if (magicarmorlevel > 0) { // Their Magic Armor level should be greater than 0 if they have the buff on, but this is to make absolutely sure.
                    int splashedmonstercount = (magicarmorlevel / 10) + 1; // Magic Armor strikes 1 additional mob at lvls 1 - 9, 2 at lvls 10 - 19, and 3 at lvl 20.
                    double maxdmgmulti = ((double) magicarmorlevel * 0.01) + 0.8; // The maximum damage (% of original strike) that splash damage can do.
                                                                                  // This value scales with skill level, and the % dealt is only this % if the monster is
                                                                                  // close to 0.0 distance from the originally struck monster. Otherwise it scales down
                                                                                  // with distance to this % - 30%.
                    List<Pair<Integer, List<Integer>>> additionaldmgs = new ArrayList<>(); // Stores additional monster ID/dmg line(s) pairs from splash dmg.
                    for (int i = 0; i < attack.allDamage.size(); ++i) { // For each instance of damage lines dealt to a monster:
                        Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i); // This pair has the monster's identifier for the map, and a list of damage lines it takes.
                        if (dmg != null) { // Checking for null.
                            MapleMonster struckmonster = player.getMap().getMonsterByOid(dmg.getLeft()); // Getting the MapleMonster obj associated with the identifier.
                            if (struckmonster == null) {
                                continue;
                            }
                            List<MapleMonster> splashedmonsters = new ArrayList<>(); // This will store all monsters that are affected by splash damage.
                            // The following for loop gets all the map objects of type MONSTER within a squared distance of 100,000 from the initially struck monster (~316.23 linear distance).
                            for (MapleMapObject _mob : player.getMap().getMapObjectsInRange(struckmonster.getPosition(), magicarmorradius, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                                MapleMonster mob = (MapleMonster) _mob; // Casting to MapleMonster since we know we are only getting objs of type MONSTER.
                                if (mob.getObjectId() != struckmonster.getObjectId()) { // Making sure the map object isn't the initially struck monster, since they get no splash damage.
                                    if (splashedmonsters.size() < splashedmonstercount) { // If we haven't yet gathered as many monsters as can be splashed, just add it in to the list.
                                        splashedmonsters.add(mob);
                                    } else { // Looks like there are more monsters in range than can be splashed.
                                        double furthestdistance = -1.0; // Arbitrary negative value so that any squared distance is further than this.
                                        MapleMonster furthestmonster = null; // This stores the monster that, so far, is in the splashed monster list, but is furthest from the init strike.
                                        for (MapleMonster splashed : splashedmonsters) { // This for loop gets the monster in splashedmonsters furthest from the init strike.
                                            if (struckmonster.getPosition().distanceSq(splashed.getPosition()) > furthestdistance) {
                                                furthestdistance = struckmonster.getPosition().distanceSq(splashed.getPosition());
                                                furthestmonster = splashed;
                                            }
                                        }
                                        if (struckmonster.getPosition().distanceSq(mob.getPosition()) < furthestdistance) {
                                            // If our new monster we are trying to add in to the 'splashed' list is closer than the furthest already in the list,
                                            // that furthest one is replaced:
                                            splashedmonsters.set(splashedmonsters.indexOf(furthestmonster), mob);
                                        }
                                    }
                                }
                            }
                            // Now we have our list of splashed monsters.
                            for (MapleMonster splashedmonster : splashedmonsters) { // For each monster that is splashed, create for it one of these pairs.
                                Integer splashedoid = splashedmonster.getObjectId(); // Object ID, for the left side of the pair.
                                List<Integer> splashdamage = new ArrayList<>(); // Stores dmg line(s) for splash dmg.
                                double distancesq = struckmonster.getPosition().distanceSq(splashedmonster.getPosition()); // Getting the squared distance of this monster from the
                                                                                                                           // init strike for purposes of scaling the dmg % by distance.
                                for (Integer dmgline : dmg.getRight()) { // For each dmg line done in the init strike, we scale the dmg line by the %, and add it to out new splash dmg.
                                    double chancetohit = (double) player.getAccuracy() / ((1.84 + 0.07 * Math.max((double) splashedmonster.getLevel() - (double) player.getLevel(), 0.0)) * (double) splashedmonster.getAvoid()) - 1.0;
                                    if (Math.random() < chancetohit) {
                                        splashdamage.add((int) (dmgline * (maxdmgmulti - 0.3 * (distancesq / magicarmorradius)))); // distancesq / radius is small when the monster is close to
                                    } else {                                                                                       // the init struck monster, so 0.3 is multiplied by a small
                                        splashdamage.add(0);                                                                       // number and the dmg multiplier (%) is closer to the max %.
                                    }
                                }
                                additionaldmgs.add(new Pair<>(splashedoid, splashdamage)); // The additional splash dmg pairs are stored in their own container to be used later.
                            }
                        }
                    }
                    attack.allDamage.addAll(additionaldmgs); // The separate container is added to the allDamage, so that the allDamage is processed normally but with the new
                                                             // splash dmg.
                    for (Pair<Integer, List<Integer>> additionaldmg : additionaldmgs) {
                        for (Integer dmgline : additionaldmg.getRight()) { // For each dmg line in the new splash dmg, we send a packet to everyone (incl. attacker) that
                                                                           // the monster was struck for that much dmg.
                            player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(additionaldmg.getLeft(), dmgline), true);
                        }
                    }
                }
            }
        }
            
            if (player.getDeathPenalty() > 0) {
                double dpmultiplier = Math.max(1.0 - (double) player.getDeathPenalty() * 0.03, 0.0);
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    if (dmg != null) {
                        List<Integer> additionaldmg = new ArrayList<>();
                        List<Integer> newdmg = new ArrayList<>();
                        for (Integer dmgnumber : dmg.getRight()) {
                            additionaldmg.add((int) (dmgnumber * (dpmultiplier - 1.0)));
                            newdmg.add((int) (dmgnumber * dpmultiplier));
                        }
                        attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                        for (Integer additionald : additionaldmg) {
                            player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(dmg.getLeft(), additionald), true);
                        }
                    }
                }
            }

        // Handle sacrifice hp loss.
        if (attack.numAttacked > 0 && attack.skill == 1311005) {
            int totDamageToOneMonster = attack.allDamage.get(0).getRight().get(0); // sacrifice attacks only 1 mob with 1 attack
            int remainingHP = player.getHp() - totDamageToOneMonster * attack.getAttackEffect(player).getX() / 100;
            if (remainingHP > 1) {
                player.setHp(remainingHP);
            } else {
                player.setHp(1);
            }
            player.updateSingleStat(MapleStat.HP, player.getHp());
        }
        // Handle charged blow.
        if (attack.numAttacked > 0 && attack.skill == 1211002) {
            boolean advcharge_prob = false;
            int advcharge_level = player.getSkillLevel(SkillFactory.getSkill(1220010));
            if (advcharge_level > 0) {
                MapleStatEffect advcharge_effect = SkillFactory.getSkill(1220010).getEffect(advcharge_level);
                advcharge_prob = advcharge_effect != null && advcharge_effect.makeChanceResult();
            }
            if (!advcharge_prob) {
                player.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            }
        }
        //int maxdamage = c.getPlayer().getCurrentMaxBaseDamage();
        int attackCount = 1;
        if (attack.skill != 0) {
            MapleStatEffect effect = attack.getAttackEffect(c.getPlayer());
            if (effect != null) {
                attackCount = effect.getAttackCount();
            }
            //maxdamage *= effect.getDamage() / 100;
            //maxdamage *= attackCount;
        }
        /*
        maxdamage = Math.min(maxdamage, 999999 * attackCount);
        if (attack.skill == 4211006) {
            maxdamage = 999999;
        } else if (numFinisherOrbs > 0) {
            maxdamage *= numFinisherOrbs;
        } else if (comboBuff != null) {
            ISkill combo = SkillFactory.getSkill(1111002);
            int comboLevel = player.getSkillLevel(combo);
            MapleStatEffect comboEffect = combo.getEffect(comboLevel);
            double comboMod = 1.0 + (comboEffect.getDamage() / 100.0 - 1.0) * (comboBuff - 1);
            maxdamage *= comboMod;
        }
        */
        if (numFinisherOrbs == 0 && isFinisher(attack.skill)) {
            return; // Can only happen when lagging.
        }
        /*
        if (isFinisher(attack.skill)) {
            maxdamage = 999999;
        }
        */
        if (attack.skill > 0) {
            ISkill skill = SkillFactory.getSkill(attack.skill);
            int skillLevel = c.getPlayer().getSkillLevel(skill);
            MapleStatEffect effect_ = skill.getEffect(skillLevel);
            if (effect_.getCooldown() > 0) {
                if (player.skillIsCooling(attack.skill)) {
                    //player.getCheatTracker().registerOffense(CheatingOffense.COOLDOWN_HACK);
                    return;
                } else {
                    c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect_.getCooldown()));
                    ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(c.getPlayer(), attack.skill), effect_.getCooldown() * 1000);
                    player.addCooldown(attack.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, timer);
                }
            }
        }
        applyAttack(attack, player, attackCount);
        if (c.getPlayer().hasFakeChar()) {
            for (FakeCharacter ch : c.getPlayer().getFakeChars()) {
                player.getMap().broadcastMessage(ch.getFakeChar(), MaplePacketCreator.closeRangeAttack(ch.getFakeChar().getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.speed), false, true);
                applyAttack(attack, ch.getFakeChar(), attackCount);
            }
        }
    }
}
