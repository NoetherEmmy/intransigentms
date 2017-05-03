package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CloseRangeDamageHandler extends AbstractDealDamageHandler {
    static final double MAGIC_GUARD_RADIUS = 70000.0d;

    private boolean isFinisher(final int skillId) {
        return skillId >= 1111003 && skillId <= 1111006;
    }

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final AttackInfo attack = parseDamage(slea, false);
        final MapleCharacter player = c.getPlayer();
        player.resetAfkTime();

        if (SkillFactory.getSkill(attack.skill) == null) {
            System.err.println(player.getName() + " is using nonexistent skill: " + attack.skill);
            return;
        }

        final boolean questEffectiveBlock = !player.canQuestEffectivelyUseSkill(attack.skill);
        if (questEffectiveBlock || player.getMap().isDamageMuted()) {
            AbstractDealDamageHandler.cancelDamage(attack, c, questEffectiveBlock);
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

        boolean perfectStrike = false;
        final IItem weaponItem = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        MapleWeaponType weapon = null;
        if (weaponItem != null) {
            weapon = MapleItemInformationProvider.getInstance().getWeaponType(weaponItem.getItemId());
        }
        //<editor-fold desc="Bare-handed; Monk stuff goes here">
        if (player.isBareHanded()) {
            final int perfectStrikeLevel = player.getSkillLevel(5000000);
            if (perfectStrikeLevel > 0) {
                perfectStrike = true;
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
                            if (additionald == 0) continue;
                            final int additionald_ = additionald;
                            TimerManager.getInstance().schedule(() ->
                                map.broadcastMessage(
                                    player,
                                    MaplePacketCreator.damageMonster(dmg.getLeft(), additionald),
                                    true
                                ),
                                delay
                            );
                            delay += 300;
                        }
                    } else {
                        additionalDmg.forEach(additionald -> {
                            if (additionald == 0) return;
                            map.broadcastMessage(
                                player,
                                MaplePacketCreator.damageMonster(dmg.getLeft(), additionald),
                                true
                            );
                        });
                    }
                }

                removedDmg.forEach(rd ->
                    rd.getRight().stream().filter(num -> num != 0).forEach(num ->
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
                                    if (inflicted < 1) continue;
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
                AbstractDealDamageHandler.multDamage(attack, c, mrmultiplier, 0);
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
                AbstractDealDamageHandler.multDamage(attack, c, critMultiplier, 0);
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
                AbstractDealDamageHandler.multDamage(attack, c, critMultiplier, 0);
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
        AbstractDealDamageHandler.baseMultDamage(attack, c, skillUsed, perfectStrike ? 1 : 0);

        //<editor-fold desc="Battle Priest Magic Guard">
        final int magicGuardLevel = player.getSkillLevel(2001002);
        if (
            (weapon == MapleWeaponType.BLUNT1H || weapon == MapleWeaponType.BLUNT2H) &&
            player.hasMagicGuard() &&
            magicGuardLevel > 0
        ) {
            final int splashedMonsterCount = magicGuardLevel / 10 + 1;
            final double maxDmgMulti = ((double) magicGuardLevel * 0.01d) + 0.8d;
            final List<Pair<Integer, List<Integer>>> additionalDmgs = new ArrayList<>(splashedMonsterCount);
            attack.allDamage.forEach(dmg -> {
                if (dmg == null) return;
                final MapleMonster struckMonster = player.getMap().getMonsterByOid(dmg.getLeft());
                if (struckMonster == null) return;
                player
                    .getMap()
                    .getMapObjectsInRange(
                        struckMonster.getPosition(),
                        MAGIC_GUARD_RADIUS,
                        MapleMapObjectType.MONSTER
                    )
                    .stream()
                    .filter(mmo -> mmo.getObjectId() != struckMonster.getObjectId())
                    .map(mmo -> (MapleMonster) mmo)
                    .sorted(
                        Comparator.comparingDouble(
                            mob -> struckMonster.getPosition().distanceSq(mob.getPosition())
                        )
                    )
                    .limit(splashedMonsterCount)
                    .forEach(splashedMonster -> {
                        final Integer splashedOid = splashedMonster.getObjectId();
                        final List<Integer> splashDamage = new ArrayList<>(1);
                        final double distanceSq =
                            struckMonster
                                .getPosition()
                                .distanceSq(splashedMonster.getPosition());
                        dmg.getRight().forEach(dmgLine -> {
                            final double chanceToHit =
                                (double) player.getAccuracy() / ((1.84d + 0.07d * Math.max((double) splashedMonster.getLevel() - (double) player.getLevel(), 0.0d)) * (double) splashedMonster.getAvoid()) - 1.0d;
                            if (Math.random() < chanceToHit) {
                                if (!splashedMonster.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) {
                                    splashDamage.add(
                                        (int) (dmgLine * (maxDmgMulti - 0.3d * (distanceSq / MAGIC_GUARD_RADIUS)))
                                    );
                                } else {
                                    splashDamage.add(Math.min(1, dmgLine));
                                }
                            } else {
                                splashDamage.add(0);
                            }
                        });
                        additionalDmgs.add(new Pair<>(splashedOid, splashDamage));
                    });
            });
            attack.allDamage.addAll(additionalDmgs);
            additionalDmgs.forEach(additionalDmg ->
                additionalDmg
                    .getRight()
                    .forEach(dmgLine ->
                        player
                            .getMap()
                            .broadcastMessage(
                                player,
                                MaplePacketCreator.damageMonster(
                                    additionalDmg.getLeft(),
                                    dmgLine
                                ),
                                true
                            )
                    )
            );
        }
        //</editor-fold>

        if (player.getDeathPenalty() > 0 || player.getQuestEffectiveLevel() > 0) {
            AbstractDealDamageHandler.applyDamagePenalty(attack, c);
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
            final boolean advchargeProb;
            final int advchargeLevel = player.getSkillLevel(1220010);
            if (advchargeLevel > 0) {
                final MapleStatEffect advchargeEffect = SkillFactory.getSkill(1220010).getEffect(advchargeLevel);
                advchargeProb = advchargeEffect != null && advchargeEffect.makeChanceResult();
            } else {
                advchargeProb = false;
            }
            if (!advchargeProb) {
                try {
                    player.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
                } catch (final NullPointerException npe) {
                    return; // Player did not have the buff stat somehow.
                }
            }
        }
        //int maxdamage = c.getPlayer().getCurrentMaxBaseDamage();
        final int attackCount;
        if (attack.skill != 0) {
            final MapleStatEffect effect = attack.getAttackEffect(c.getPlayer());
            if (effect != null) {
                attackCount = effect.getAttackCount();
            } else {
                attackCount = 1;
            }
            //maxdamage *= effect.getDamage() / 100;
            //maxdamage *= attackCount;
        } else {
            attackCount = 1;
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
            if (!AbstractDealDamageHandler.processSkill(attack, c)) return;
        }

        applyAttack(attack, player, attackCount);

        if (c.getPlayer().hasFakeChar()) {
            c.getPlayer().getFakeChars().forEach(ch -> {
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
            });
        }
    }
}
