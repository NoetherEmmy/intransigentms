package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.Element;
import net.sf.odinms.server.life.ElementalEffectiveness;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

public class CloseRangeDamageHandler extends AbstractDealDamageHandler {
    private boolean isFinisher(final int skillId) {
        return skillId >= 1111003 && skillId <= 1111006;
    }

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final AttackInfo attack = parseDamage(slea, false);
        final MapleCharacter player = c.getPlayer();
        player.resetAfkTime();

        final boolean questEffectiveBlock = !player.canQuestEffectivelyUseSkill(attack.skill);
        if (questEffectiveBlock || player.getMap().isDamageMuted()) {
            for (int i = 0; i < attack.allDamage.size(); ++i) {
                final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                MapleMonster monster = null;
                if (dmg != null) monster = player.getMap().getMonsterByOid(dmg.getLeft());
                if (monster == null) continue;
                final List<Integer> additionalDmg = new ArrayList<>(dmg.getRight().size());
                for (final Integer dmgNumber : dmg.getRight()) {
                    additionalDmg.add(-dmgNumber);
                }
                for (final Integer additionald : additionalDmg) {
                    c.getSession().write(MaplePacketCreator.damageMonster(dmg.getLeft(), additionald));
                }
            }
            if (questEffectiveBlock) {
                player.dropMessage(
                    5,
                    "Your quest effective level (" +
                        player.getQuestEffectiveLevel() +
                        ") is too low to use " +
                        SkillFactory.getSkillName(attack.skill) +
                        "."
                );
            }
            return;
        }

        final MaplePacket packet = MaplePacketCreator.closeRangeAttack(
            player.getId(),
            attack.skill,
            attack.stance,
            attack.numAttackedAndDamage,
            attack.allDamage,
            attack.speed
        );

        player.getMap().broadcastMessage(player, packet, false, true);
        int numFinisherOrbs = 0;
        final Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO);
        if (isFinisher(attack.skill)) {
            if (comboBuff != null) numFinisherOrbs = comboBuff - 1;
            player.handleOrbconsume();
        } else if (attack.numAttacked > 0) {
            // Handle combo orb gain.
            if (comboBuff != null) {
                if (attack.skill != 1111008) { // Shout should not give orbs.
                    player.handleOrbgain();
                }
            } else if (
                (player.getJob().equals(MapleJob.BUCCANEER) || player.getJob().equals(MapleJob.MARAUDER)) &&
                player.getSkillLevel(5110001) > 0
            ) {
                for (int i = 0; i < attack.numAttacked; ++i) {
                    player.handleEnergyChargeGain();
                }
            }
        }

        final IItem weaponItem = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        MapleWeaponType weapon = null;
        if (weaponItem != null) {
            weapon = MapleItemInformationProvider.getInstance().getWeaponType(weaponItem.getItemId());
        }
        //<editor-fold desc="Bare-handed; Monk stuff goes here">
        if (player.isBareHanded()) {
            final int perfectStrikeLevel = player.getSkillLevel(5000000);
            if (perfectStrikeLevel > 0) {
                int mobsHit = 1;

                final ISkill fistMastery = SkillFactory.getSkill(5100001);
                final int fistMasteryLevel = player.getSkillLevel(fistMastery);
                if (fistMasteryLevel > 10) {
                    mobsHit++;
                }
                final int stunMasteryLevel = player.getSkillLevel(5110000);
                if (stunMasteryLevel > 10) {
                    mobsHit++;
                }
                final Integer flurryOfBlowsBuff = player.getBuffedValue(MapleBuffStat.SPEED_INFUSION);
                if (flurryOfBlowsBuff != null && flurryOfBlowsBuff != 0) {
                    mobsHit++;
                }

                final MapleMap map = player.getMap();
                attack.allDamage.sort(Comparator.comparingInt(d -> {
                    final MapleMapObject m = map.getMapObject(d.getLeft());
                    if (m == null) return Integer.MAX_VALUE;
                    return (int) m.getPosition().distanceSq(player.getPosition());
                }));

                final List<Pair<Integer, List<Integer>>> removedDmg = new ArrayList<>(attack.allDamage.size());
                if (attack.allDamage.size() > mobsHit) {
                    for (int i = mobsHit; i < attack.allDamage.size(); ++i) {
                        removedDmg.add(attack.allDamage.get(i));
                    }
                    attack.allDamage.subList(mobsHit, attack.allDamage.size()).clear();
                }

                double critRate = 0.0d, critDamage = 0.0d;
                final Integer sharpEyesBuff_ = player.getBuffedValue(MapleBuffStat.SHARP_EYES);
                if (sharpEyesBuff_ != null) {
                    final int sharpEyesBuff = sharpEyesBuff_;
                    final int x = (sharpEyesBuff & ~0xFF) >> 8;
                    final int y = sharpEyesBuff & 0xFF;
                    critRate = (double) x / 100.0d;
                    critDamage = ((double) y - 100.0d) / 100.0d;
                }
                final double effectiveMagic = (double) (perfectStrikeLevel * player.getTotalMagic()) * 0.05d;
                final double mastery = fistMasteryLevel > 0 ? ((double) fistMastery.getEffect(fistMasteryLevel).getMastery() * 5.0d + 10.0d) / 100.0d : 0.1d;
                final double skillDamage;
                if (attack.skill > 0) {
                    final double chargeMulti;
                    if (attack.charge > 0) {
                        chargeMulti = (double) attack.charge / 1000.0d;
                    } else {
                        chargeMulti = 1.0d;
                    }
                    skillDamage = chargeMulti * (double) SkillFactory.getSkill(attack.skill).getEffect(player.getSkillLevel(attack.skill)).getDamage() / 100.0d;
                } else {
                    skillDamage = 1.0d;
                }
                final int minDmg = (int) ((Math.log(player.getTotalInt()) * 0.9d * mastery + 2.0d * (double) player.getTotalDex()) * effectiveMagic / 32.0d);
                final int maxDmg = (int) ((Math.log(player.getTotalInt()) + 2.0d * (double) player.getTotalDex()) * effectiveMagic / 32.0d);

                final Random rand = new Random();
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    final List<Integer> additionalDmg = new ArrayList<>(dmg.getRight().size());
                    final List<Integer> newDmg = new ArrayList<>(dmg.getRight().size());
                    final MapleMonster m = map.getMonsterByOid(dmg.getLeft());
                    double critMulti = 1.0d;
                    if (dmg.getRight() == null) continue;
                    if (m == null) continue;
                    if (stunMasteryLevel > 0) {
                        final boolean stunned = m.isBuffed(MonsterStatus.STUN);
                        if (stunned) {
                            final MapleStatEffect stunMasteryEffect =
                                SkillFactory
                                    .getSkill(5110000)
                                    .getEffect(stunMasteryLevel);
                            final double stunCritChance = stunMasteryEffect.getProp() / 100.0d + critRate;
                            if (stunCritChance > Math.random()) {
                                critMulti = (double) stunMasteryEffect.getDamage() / 100.0d + critDamage;
                            }
                        } else if (critRate > 0.0d) {
                            final MapleStatEffect stunMasteryEffect =
                                SkillFactory
                                    .getSkill(5110000)
                                    .getEffect(stunMasteryLevel);
                            if (critRate > Math.random()) {
                                critMulti = (double) stunMasteryEffect.getDamage() / 100.0d + critDamage;
                            }
                        }
                    } else if (critRate > 0.0d && critRate > Math.random()) {
                        critMulti = 1.0d + critDamage;
                    }
                    final boolean immune = m.isBuffed(MonsterStatus.MAGIC_IMMUNITY);
                    int hitIndex = 0;
                    int localMinDmg = immune ? 1 : (int) (minDmg * skillDamage * critMulti - m.getMdef() * 0.6d * (1.0d + 0.01d * Math.max(m.getLevel() - player.getLevel(), 0.0d)));
                    localMinDmg = Math.max(1, localMinDmg);
                    int localMaxDmg = immune ? 1 : (int) (maxDmg * skillDamage * critMulti - m.getMdef() * 0.5d * (1.0d + 0.01d * Math.max(m.getLevel() - player.getLevel(), 0.0d)));
                    localMaxDmg = Math.max(1, localMaxDmg);
                    for (final Integer dmgNumber : dmg.getRight()) {
                        if (dmgNumber == null || dmgNumber < 1) continue;
                        final int magicDmgNumber;
                        if (localMaxDmg > 1 && attack.skill == 5121007 && hitIndex >= 4 && hitIndex <= 5) {
                            // Barrage's last two strikes do additional damage.
                            magicDmgNumber = (localMinDmg + rand.nextInt(localMaxDmg - localMinDmg + 1)) * (hitIndex - 3) * 2;
                        } else {
                            magicDmgNumber = localMinDmg + rand.nextInt(localMaxDmg - localMinDmg + 1);
                        }
                        additionalDmg.add(magicDmgNumber);
                        newDmg.add(dmgNumber + magicDmgNumber);
                        hitIndex++;
                    }
                    attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newDmg));
                    if (attack.skill == 5121004 || attack.skill == 5121007 || attack.skill == 5101003) {
                        // Demolition, Barrage, Double Uppercut
                        // Skills that hit multiple numbers per mob.
                        int delay = 0;
                        for (final Integer additionald : additionalDmg) {
                            final int additionald_ = additionald;
                            TimerManager.getInstance().schedule(() ->
                                map.broadcastMessage(
                                    player,
                                    MaplePacketCreator.damageMonster(dmg.getLeft(), additionald),
                                    true
                                ), delay);
                            delay += 300;
                        }
                    } else {
                        for (final Integer additionald : additionalDmg) {
                            map.broadcastMessage(
                                player,
                                MaplePacketCreator.damageMonster(dmg.getLeft(), additionald),
                                true
                            );
                        }
                    }
                }

                removedDmg.forEach(rd ->
                    rd.getRight().forEach(num ->
                        map.broadcastMessage(
                            player,
                            MaplePacketCreator.damageMonster(rd.getLeft(), -num),
                            true
                        )
                    )
                );
            }
            if (player.getEnergyBar() >= 10000) {
                final double radiusSq = 500000.0d;

                final boolean someHit =
                    !attack.allDamage.isEmpty() &&
                    attack.allDamage
                          .stream()
                          .allMatch(dmg ->
                              dmg.getRight()
                                 .stream()
                                 .anyMatch(num ->
                                     num > 0
                                 )
                          );
                if (player.getTotalInt() >= 400 && attack.skill == 5111002) {
                    // Abhayamudra
                    c.getSession().write(MaplePacketCreator.giveEnergyCharge(0));
                    player.setEnergyBar(0);
                    if (someHit) {
                        player.getMap()
                              .getMapObjectsInRange(
                                  player.getPosition(),
                                  radiusSq,
                                  MapleMapObjectType.MONSTER
                              )
                              .stream()
                              .map(mmo -> (MapleMonster) mmo)
                              .forEach(mob -> {
                                  mob.applyStatus(
                                      player,
                                      new MonsterStatusEffect(
                                          Collections.singletonMap(
                                              MonsterStatus.STUN,
                                              1
                                          ),
                                          SkillFactory.getSkill(5101003),
                                          false
                                      ),
                                      false,
                                      5L * 1000L,
                                      false
                                  );
                                  mob.applyVulnerability(1.5d, 5L * 1000L);
                              });
                    }
                } else if (player.getTotalInt() >= 400 && attack.skill == 5111004) {
                    // Nirvana
                    c.getSession().write(MaplePacketCreator.giveEnergyCharge(0));
                    player.setEnergyBar(0);
                    if (someHit) {
                        player.setInvincible(true);
                        TimerManager.getInstance().schedule(() -> player.setInvincible(false), 5L * 1000L);
                    }
                } else if (player.getTotalInt() >= 750 && attack.skill == 5121007) {
                    // Despondency
                    if (someHit) {
                        Pair<Integer, List<Integer>> strike = null;
                        int i = 0;
                        while (i < attack.allDamage.size() && strike == null) {
                            strike = attack.allDamage.get(i);
                            i++;
                        }
                        if (strike == null) return;

                        final MapleMonster struckMob = player.getMap().getMonsterByOid(strike.getLeft());
                        if (struckMob != null) {
                            c.getSession().write(MaplePacketCreator.giveEnergyCharge(0));
                            player.setEnergyBar(0);

                            final Point struckMobPos = struckMob.getPosition();
                            final double struckMobHpPercentage = (double) struckMob.getHp() / (double) struckMob.getMaxHp();
                            final double multiplier =
                                (double) player.getSkillLevel(5121007) *
                                    (Math.sqrt(1.0d / (struckMobHpPercentage + 0.01d)) - 0.9d);
                            final long animationInterval = 300L; //SkillFactory.getSkill(5121007).getAnimationTime() / 5L;
                            final List<MapleMonster> targets =
                                player.getMap()
                                      .getMapObjectsInRange(
                                          struckMobPos,
                                          radiusSq,
                                          MapleMapObjectType.MONSTER
                                      )
                                      .stream()
                                      .map(mmo -> (MapleMonster) mmo)
                                      .sorted(Comparator.comparingDouble(m -> struckMobPos.distanceSq(m.getPosition())))
                                      .collect(Collectors.toCollection(ArrayList::new));

                            i = 0;
                            while (i < targets.size() && i < 8) {
                                final MapleMonster target = targets.get(i);
                                final boolean immune = target.isBuffed(MonsterStatus.MAGIC_IMMUNITY);
                                for (final Integer dmgNumber : strike.getRight()) {
                                    if (dmgNumber == null || dmgNumber < 1) continue;
                                    final int inflicted = immune ? 1 : (int) (dmgNumber * multiplier * target.getVulnerability());
                                    TimerManager.getInstance().schedule(
                                        () -> {
                                            player
                                                .getMap()
                                                .broadcastMessage(
                                                    player,
                                                    MaplePacketCreator.damageMonster(
                                                        target.getObjectId(),
                                                        inflicted
                                                    ),
                                                    true,
                                                    true
                                                );
                                            player.getMap().damageMonster(player, target, inflicted);
                                        },
                                        i * animationInterval
                                    );
                                }
                                player.checkMonsterAggro(target);
                                i++;
                            }
                        }
                    } else {
                        c.getSession().write(MaplePacketCreator.giveEnergyCharge(0));
                        player.setEnergyBar(0);
                    }
                }
            }
        }
        //</editor-fold>

        if (weapon == MapleWeaponType.BLUNT1H || weapon == MapleWeaponType.BLUNT2H) {
            final int mrSkillLevel = player.getSkillLevel(2321002);
            if (player.getBuffedValue(MapleBuffStat.MANA_REFLECTION) != null && mrSkillLevel > 0) {
                final double mrmultiplier = 2.0d + (double) mrSkillLevel * 0.05d;
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    final MapleMonster m = player.getMap().getMonsterByOid(dmg.getLeft());
                    if (m == null || m.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) continue;
                    final List<Integer> additionalDmg = new ArrayList<>(1);
                    final List<Integer> newDmg = new ArrayList<>(1);
                    for (final Integer dmgNumber : dmg.getRight()) {
                        additionalDmg.add((int) ((double) dmgNumber * (mrmultiplier - 1.0d)));
                        newDmg.add((int) ((double) dmgNumber * mrmultiplier));
                    }
                    attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newDmg));
                    for (final Integer additionald : additionalDmg) {
                        player
                            .getMap()
                            .broadcastMessage(
                                player,
                                MaplePacketCreator.damageMonster(dmg.getLeft(), additionald),
                                true
                            );
                    }
                }
            }

            // Handing crits from Battle Priest's MP Eater passive
            final boolean isCrit;
            final int mpEaterLevel = player.getSkillLevel(2300000);
            if (mpEaterLevel > 0) {
                final double chance = (20.0d + 2.0d * (double) mpEaterLevel) / 100.0d;
                isCrit = Math.random() < chance;
            } else {
                isCrit = false;
            }

            if (isCrit) {
                final double critMultiplier = 1.5d + 0.08d * (double) mpEaterLevel;
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    final MapleMonster m = player.getMap().getMonsterByOid(dmg.getLeft());
                    if (m == null || m.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) continue;
                    final List<Integer> additionalDmg = new ArrayList<>(dmg.getRight().size());
                    final List<Integer> newDmg = new ArrayList<>(dmg.getRight().size());
                    for (final Integer dmgNumber : dmg.getRight()) {
                        additionalDmg.add((int) (dmgNumber * (critMultiplier - 1.0d)));
                        newDmg.add((int) (dmgNumber * critMultiplier));
                    }
                    attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newDmg));
                    for (final Integer additionald : additionalDmg) {
                        player
                            .getMap()
                            .broadcastMessage(
                                player,
                                MaplePacketCreator.damageMonster(
                                    dmg.getLeft(),
                                    additionald
                                ),
                                true
                            );
                    }
                }
            }
        } else if ((weapon == MapleWeaponType.DAGGER || weapon == MapleWeaponType.SPEAR) && player.isUnshielded()) {
            // Handing crits from Orion's Critical Shot
            final int critShotLevel = player.getSkillLevel(3000001);
            MapleStatEffect critShot = null;
            if (critShotLevel > 0) {
                critShot = SkillFactory.getSkill(3000001).getEffect(critShotLevel);
            }
            if (critShot != null && critShot.makeChanceResult()) {
                final double critMultiplier = (double) critShot.getDamage() / 100.0d;
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    final MapleMonster m = player.getMap().getMonsterByOid(dmg.getLeft());
                    if (m == null || m.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) continue;
                    final List<Integer> additionalDmg = new ArrayList<>(dmg.getRight().size());
                    final List<Integer> newDmg = new ArrayList<>(dmg.getRight().size());
                    for (final Integer dmgNumber : dmg.getRight()) {
                        additionalDmg.add((int) (dmgNumber * (critMultiplier - 1.0d)));
                        newDmg.add((int) (dmgNumber * critMultiplier));
                    }
                    attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newDmg));
                    for (final Integer additionald : additionalDmg) {
                        player
                            .getMap()
                            .broadcastMessage(
                                player,
                                MaplePacketCreator.damageMonster(
                                    dmg.getLeft(),
                                    additionald
                                ),
                                true
                            );
                    }
                }
            }

            // Anatomical Adept/Final Attack: Bow
            final int anatomicalAdeptLevel = player.getSkillLevel(3100001);
            if (anatomicalAdeptLevel > 0) {
                final double proc = (double) (2 * anatomicalAdeptLevel + 40) / 100.0d;
                if (Math.random() < proc) {
                    for (int i = 0; i < attack.allDamage.size(); ++i) {
                        final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                        final MapleMonster m = player.getMap().getMonsterByOid(dmg.getLeft());
                        if (
                            m == null ||
                            dmg.getRight().stream().mapToInt(Integer::intValue).sum() < 1
                        ) {
                            continue;
                        }
                        m.anatomicalStrike(player.getId());
                    }
                }
            }
        }

        final ISkill skillUsed = SkillFactory.getSkill(attack.skill);
        for (int i = 0; i < attack.allDamage.size(); ++i) {
            final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
            MapleMonster monster = null;
            if (dmg != null) monster = player.getMap().getMonsterByOid(dmg.getLeft());
            if (monster == null || monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) continue;
            ElementalEffectiveness ee = null;
            if (skillUsed != null && skillUsed.getElement() != Element.NEUTRAL) {
                ee = monster.getAddedEffectiveness(skillUsed.getElement());
                if (
                    (ee == ElementalEffectiveness.WEAK || ee == ElementalEffectiveness.IMMUNE) &&
                    monster.getEffectiveness(skillUsed.getElement()) == ElementalEffectiveness.WEAK
                ) {
                    ee = null;
                }
            }
            double multiplier = monster.getVulnerability();
            final List<Integer> additionalDmg = new ArrayList<>(dmg.getRight().size());
            final List<Integer> newDmg = new ArrayList<>(dmg.getRight().size());
            if (ee != null) {
                switch (ee) {
                    case WEAK:
                        multiplier *= 1.5d;
                        break;
                    case STRONG:
                        multiplier *= 0.5d;
                        break;
                    case IMMUNE:
                        multiplier = 0.0d;
                        break;
                }
            }
            if (multiplier == 1.0d) continue;
            for (final Integer dmgNumber : dmg.getRight()) {
                additionalDmg.add((int) (dmgNumber * (multiplier - 1.0d)));
                newDmg.add((int) (dmgNumber * multiplier));
            }
            attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newDmg));
            for (final Integer additionald : additionalDmg) {
                player.getMap().broadcastMessage(
                    player,
                    MaplePacketCreator.damageMonster(dmg.getLeft(), additionald),
                    true
                );
            }
        }

        if (weapon == MapleWeaponType.BLUNT1H || weapon == MapleWeaponType.BLUNT2H) {
            if (player.hasMagicGuard()) { // If player has Magic Guard on them currently.
                final double magicGuardRadius = 70000.0d; // Squared distance
                final int magicGuardLevel = player.getSkillLevel(2001002);
                if (magicGuardLevel > 0) { // Their Magic Guard level should be greater than 0 if they have the buff on, but this is to make absolutely sure.
                    final int splashedMonsterCount = magicGuardLevel / 10 + 1; // Magic Guard strikes 1 additional mob at lvls 1 - 9, 2 at lvls 10 - 19, and 3 at lvl 20.
                    final double maxDmgMulti = ((double) magicGuardLevel * 0.01d) + 0.8d; // The maximum damage (% of original strike) that splash damage can do.
                                                                                    // This value scales with skill level, and the % dealt is only this % if the monster is
                                                                                    // close to 0.0 distance from the originally struck monster. Otherwise it scales down
                                                                                    // with distance to this % - 30%.
                    final List<Pair<Integer, List<Integer>>> additionalDmgs = new ArrayList<>(splashedMonsterCount); // Stores additional monster ID/dmg line(s) pairs from splash dmg.
                    for (int i = 0; i < attack.allDamage.size(); ++i) { // For each instance of damage lines dealt to a monster:
                        final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i); // This pair has the monster's identifier for the map, and a list of damage lines it takes.
                        if (dmg == null) continue; // Checking for null.
                        final MapleMonster struckMonster = player.getMap().getMonsterByOid(dmg.getLeft()); // Getting the MapleMonster obj associated with the identifier.
                        if (struckMonster == null) continue;
                        final List<MapleMonster> splashedMonsters = new ArrayList<>(splashedMonsterCount); // This will store all monsters that are affected by splash damage.
                        // The following for loop gets all the map objects of type MONSTER within a squared distance of 100,000 from the initially struck monster (~316.23 linear distance).
                        for (final MapleMapObject _mob : player.getMap().getMapObjectsInRange(struckMonster.getPosition(), magicGuardRadius, MapleMapObjectType.MONSTER)) {
                            final MapleMonster mob = (MapleMonster) _mob; // Casting to MapleMonster since we know we are only getting objs of type MONSTER.
                            if (mob.getObjectId() != struckMonster.getObjectId()) { // Making sure the map object isn't the initially struck monster, since they get no splash damage.
                                if (splashedMonsters.size() < splashedMonsterCount) { // If we haven't yet gathered as many monsters as can be splashed, just add it in to the list.
                                    splashedMonsters.add(mob);
                                } else { // Looks like there are more monsters in range than can be splashed.
                                    double furthestDistance = -1.0d; // Arbitrary negative value so that any squared distance is further than this.
                                    MapleMonster furthestMonster = null; // This stores the monster that, so far, is in the splashed monster list, but is furthest from the init strike.
                                    for (final MapleMonster splashed : splashedMonsters) { // This for loop gets the monster in splashedMonsters furthest from the init strike.
                                        if (struckMonster.getPosition().distanceSq(splashed.getPosition()) <= furthestDistance) {
                                            continue;
                                        }
                                        furthestDistance = struckMonster.getPosition().distanceSq(splashed.getPosition());
                                        furthestMonster = splashed;
                                    }
                                    if (struckMonster.getPosition().distanceSq(mob.getPosition()) < furthestDistance) {
                                        // If our new monster we are trying to add in to the 'splashed' list is closer than the furthest already in the list,
                                        // that furthest one is replaced:
                                        splashedMonsters.set(splashedMonsters.indexOf(furthestMonster), mob);
                                    }
                                }
                            }
                        }
                        // Now we have our list of splashed monsters.
                        for (final MapleMonster splashedMonster : splashedMonsters) { // For each monster that is splashed, create for it one of these pairs.
                            final Integer splashedOid = splashedMonster.getObjectId(); // Object ID, for the left side of the pair.
                            final List<Integer> splashDamage = new ArrayList<>(1); // Stores dmg line(s) for splash dmg.
                            final double distanceSq = struckMonster.getPosition().distanceSq(splashedMonster.getPosition()); // Getting the squared distance of this monster from the
                                                                                                                       // init strike for purposes of scaling the dmg % by distance.
                            for (final Integer dmgLine : dmg.getRight()) { // For each dmg line done in the init strike, we scale the dmg line by the %, and add it to out new splash dmg.
                                final double chanceToHit = (double) player.getAccuracy() / ((1.84d + 0.07d * Math.max((double) splashedMonster.getLevel() - (double) player.getLevel(), 0.0d)) * (double) splashedMonster.getAvoid()) - 1.0d;
                                if (Math.random() < chanceToHit) {
                                    if (!splashedMonster.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) {
                                        splashDamage.add((int) (dmgLine * (maxDmgMulti - 0.3d * (distanceSq / magicGuardRadius))));
                                    } else {
                                        splashDamage.add(Math.min(1, dmgLine));
                                    }
                                } else {
                                    splashDamage.add(0);
                                }
                            }
                            additionalDmgs.add(new Pair<>(splashedOid, splashDamage)); // The additional splash dmg pairs are stored in their own container to be used later.
                        }
                    }
                    attack.allDamage.addAll(additionalDmgs); // The separate container is added to the allDamage, so that the allDamage is processed normally but with the new
                                                             // splash dmg.
                    for (final Pair<Integer, List<Integer>> additionalDmg : additionalDmgs) {
                        for (final Integer dmgLine : additionalDmg.getRight()) { // For each dmg line in the new splash dmg, we send a packet to everyone (incl. attacker) that
                                                                           // the monster was struck for that much dmg.
                            player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(additionalDmg.getLeft(), dmgLine), true);
                        }
                    }
                }
            }
        }

        if (player.getDeathPenalty() > 0 || player.getQuestEffectiveLevel() > 0) {
            double dpMultiplier = 1.0d;
            final double qeMultiplier = player.getQuestEffectiveLevelDmgMulti();
            if (player.getDeathPenalty() > 0) {
                dpMultiplier = Math.max(
                    1.0d - (double) player.getDeathPenalty() * 0.03d,
                    0.0d
                );
            }
            final double totalMultiplier = dpMultiplier * qeMultiplier;

            for (int i = 0; i < attack.allDamage.size(); ++i) {
                final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                if (dmg == null || dmg.getRight() == null) continue;
                final List<Integer> additionaldmg = new ArrayList<>(dmg.getRight().size());
                final List<Integer> newdmg = new ArrayList<>(dmg.getRight().size());
                for (final Integer dmgnumber : dmg.getRight()) {
                    additionaldmg.add((int) (dmgnumber * (totalMultiplier - 1.0d)));
                    newdmg.add((int) (dmgnumber * totalMultiplier));
                }
                attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                for (final Integer additionald : additionaldmg) {
                    player
                        .getMap()
                        .broadcastMessage(
                            player,
                            MaplePacketCreator.damageMonster(
                                dmg.getLeft(),
                                additionald
                            ),
                            true
                        );
                }
            }
        }

        // Handle Sacrifice HP loss.
        if (attack.numAttacked > 0 && attack.skill == 1311005) {
            // Sacrifice attacks only 1 mob with 1 attack.
            final int totDamageToOneMonster = attack.allDamage.get(0).getRight().get(0);
            final int remainingHP =
                player.getHp() -
                    Math.min(
                        totDamageToOneMonster * attack.getAttackEffect(player).getX() / 100,
                        4 * player.getCurrentMaxHp() / 10
                    );
            if (remainingHP > 1) {
                player.setHp(remainingHP);
            } else {
                player.setHp(1);
            }
            player.updateSingleStat(MapleStat.HP, player.getHp());
        }
        // Handle charged blow.
        if (attack.numAttacked > 0 && attack.skill == 1211002) {
            boolean advchargeProb = false;
            final int advchargeLevel = player.getSkillLevel(1220010);
            if (advchargeLevel > 0) {
                final MapleStatEffect advchargeEffect = SkillFactory.getSkill(1220010).getEffect(advchargeLevel);
                advchargeProb = advchargeEffect != null && advchargeEffect.makeChanceResult();
            }
            if (!advchargeProb) {
                try {
                    player.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
                } catch (final NullPointerException npe) {
                    return; // Player did not have the buff stat somehow
                }
            }
        }
        //int maxdamage = c.getPlayer().getCurrentMaxBaseDamage();
        int attackCount = 1;
        if (attack.skill != 0) {
            final MapleStatEffect effect = attack.getAttackEffect(c.getPlayer());
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
            double comboMod = 1.0d + (comboEffect.getDamage() / 100.0d - 1.0d) * (comboBuff - 1);
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
            final ISkill skill = SkillFactory.getSkill(attack.skill);
            final int skillLevel = c.getPlayer().getSkillLevel(skill);
            if (skillLevel < 1) return;
            final MapleStatEffect effect_ = skill.getEffect(skillLevel);
            if (effect_.getCooldown() > 0) {
                if (player.skillIsCooling(attack.skill)) {
                    //player.getCheatTracker().registerOffense(CheatingOffense.COOLDOWN_HACK);
                    return;
                }
                c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect_.getCooldown()));
                final ScheduledFuture<?> timer =
                    TimerManager
                        .getInstance()
                        .schedule(
                            new CancelCooldownAction(
                                c.getPlayer(),
                                attack.skill
                            ),
                            effect_.getCooldown() * 1000L
                        );
                player.addCooldown(attack.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000L, timer);
            }
        }
        applyAttack(attack, player, attackCount);
        if (c.getPlayer().hasFakeChar()) {
            for (final FakeCharacter ch : c.getPlayer().getFakeChars()) {
                player
                    .getMap()
                    .broadcastMessage(
                        ch.getFakeChar(),
                        MaplePacketCreator.closeRangeAttack(
                            ch.getFakeChar().getId(),
                            attack.skill,
                            attack.stance,
                            attack.numAttackedAndDamage,
                            attack.allDamage,
                            attack.speed
                        ),
                        false,
                        true
                    );
                applyAttack(attack, ch.getFakeChar(), attackCount);
            }
        }
    }
}
