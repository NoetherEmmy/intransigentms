package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.Element;
import net.sf.odinms.server.life.ElementalEffectiveness;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SummonDamageHandler extends AbstractMaplePacketHandler {
    public class SummonAttackEntry {
        private final int monsterOid;
        private final int damage;

        public SummonAttackEntry(int monsterOid, int damage) {
            super();
            this.monsterOid = monsterOid;
            this.damage = damage;
        }

        public int getMonsterOid() {
            return monsterOid;
        }

        public int getDamage() {
            return damage;
        }
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final MapleCharacter player = c.getPlayer();

        MapleSummon summon = null;
        synchronized (player.getSummons()) {
            for (MapleSummon sum : player.getSummons().values()) {
                if (sum != null && sum.getObjectId() == oid) {
                    summon = sum;
                }
            }
        }

        if (summon == null) {
            player.cleanNullSummons();
            player.getMap().removeMapObject(oid);
            return;
        }

        ISkill summonSkill = SkillFactory.getSkill(summon.getSkill());
        MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
        slea.skip(5);
        int numAttacked = slea.readByte();
        player.getCheatTracker().checkSummonAttack();

        List<SummonAttackEntry> allDamage = new ArrayList<>(numAttacked);
        for (int x = 0; x < numAttacked; ++x) {
            int monsterOid = slea.readInt(); // Attacked oid.
            slea.skip(14);
            int damage = slea.readInt();
            allDamage.add(new SummonAttackEntry(monsterOid, damage));
        }

        allDamage =
            allDamage
                .stream()
                .filter(d -> player.getMap().getMonsterByOid(d.getMonsterOid()) != null)
                .collect(Collectors.toCollection(ArrayList::new));

        if (player.getMap().isDamageMuted()) {
            for (SummonAttackEntry attackEntry : allDamage) {
                c.getSession().write(
                    MaplePacketCreator.damageMonster(
                        attackEntry.getMonsterOid(),
                        -attackEntry.getDamage()
                    )
                );
            }
            return;
        }

        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }

        final List<SummonAttackEntry> additionalDmg = new ArrayList<>(numAttacked);
        for (SummonAttackEntry attackEntry : allDamage) {
            additionalDmg.add(new SummonAttackEntry(attackEntry.getMonsterOid(), 0));
        }

        if (summonSkill.getId() == 2311006 || summonSkill.getId() == 2321003) { // Summon Dragon | Bahamut
            double percentPerLevel;
            if (summonSkill.getId() == 2311006) {
                percentPerLevel = 0.05d;
            } else {
                percentPerLevel = 0.07d;
            }
            final int min = player.calculateMinBaseDamage();
            final int max = player.calculateMaxBaseDamage();
            int baseDmg;
            double skillLevelMultiplier = 1.5d + player.getSkillLevel(summonSkill) * percentPerLevel;
            for (int i = 0; i < allDamage.size(); ++i) {
                final MapleMonster target = player.getMap().getMonsterByOid(allDamage.get(i).getMonsterOid());
                if (target == null || target.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) continue;
                baseDmg = min + (int) (Math.random() * (double) (max - min + 1));
                int thisDmg = allDamage.get(i).getDamage();
                int addedDmg = (int) (baseDmg * skillLevelMultiplier);
                thisDmg += addedDmg;
                additionalDmg.set(
                    i,
                    new SummonAttackEntry(
                        additionalDmg.get(i).getMonsterOid(),
                        additionalDmg.get(i).getDamage() + addedDmg
                    )
                );
                allDamage.set(i, new SummonAttackEntry(allDamage.get(i).getMonsterOid(), thisDmg));
            }
        } else if (summonSkill.getId() == 3111005 || summonSkill.getId() == 3121006) { // Silver Hawk | Phoenix
            int str = player.getTotalStr();
            int dex = player.getTotalDex();
            int pad = summonEffect.getWatk();
            double mastery = summonSkill.getId() == 3111005 ? 0.6d : 0.8d;
            double counterWeight = summonSkill.getId() == 3111005 ? 512.0d : 1280.0d;
            int min = (int) ((str * str * mastery + dex) * pad / counterWeight);
            int max = (int) ((double) (str * str + dex) * pad / counterWeight);
            for (int i = 0; i < allDamage.size(); ++i) {
                final MapleMonster target = player.getMap().getMonsterByOid(allDamage.get(i).getMonsterOid());
                if (target == null || target.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) continue;
                int addedDmg = min + (int) (Math.random() * (double) (max - min + 1));
                int thisDmg = allDamage.get(i).getDamage();
                thisDmg += addedDmg;
                additionalDmg.set(
                    i,
                    new SummonAttackEntry(
                        additionalDmg.get(i).getMonsterOid(),
                        additionalDmg.get(i).getDamage() + addedDmg
                    )
                );
                allDamage.set(i, new SummonAttackEntry(allDamage.get(i).getMonsterOid(), thisDmg));
                if (allDamage.get(i).getDamage() > 0) {
                    if (summonSkill.getId() == 3111005) {
                        int x = (int) Math.ceil(2.0d * Math.sqrt(Math.max(0, player.getTotalStr() - 400)) / 100.0d);
                        MonsterStatusEffect monsterStatusEffect =
                            new MonsterStatusEffect(
                                Collections.singletonMap(
                                    MonsterStatus.ACC,
                                    x
                                ),
                                SkillFactory.getSkill(3221006),
                                false
                            );
                        target.applyStatus(
                            player,
                            monsterStatusEffect,
                            false,
                            2L * 1000L
                        );
                    } else if (player.getStr() >= 512) {
                        long seconds = (long) Math.ceil((double) player.getSkillLevel(3121006) / 3.0d);
                        target.applyFlame(player, SkillFactory.getSkill(3121006), seconds * 1000L, false);
                    }
                }
            }
        }

        for (int i = 0; i < allDamage.size(); ++i) {
            SummonAttackEntry attackEntry = allDamage.get(i);
            MapleMonster target = player.getMap().getMonsterByOid(attackEntry.getMonsterOid());
            if (target == null) continue;

            ElementalEffectiveness ee = null;
            if (summonSkill.getElement() != Element.NEUTRAL) {
                ee = target.getAddedEffectiveness(summonSkill.getElement());
                if (
                    (ee == ElementalEffectiveness.WEAK || ee == ElementalEffectiveness.IMMUNE) &&
                    target.getEffectiveness(summonSkill.getElement()) == ElementalEffectiveness.WEAK
                ) {
                    ee = null;
                }
            }

            double multiplier = target.getVulnerability();
            if (ee != null) {
                switch (ee) {
                    case WEAK:
                        multiplier *= 1.5d;
                        break;
                    case STRONG:
                        multiplier *= 0.5d;
                        break;
                    case IMMUNE:
                        multiplier *= 0.0d;
                        break;
                }
            }

            if (multiplier != 1.0d) {
                int newDmg = (int) (attackEntry.getDamage() * multiplier);
                int addedDmg = newDmg - attackEntry.getDamage();
                additionalDmg.set(
                    i,
                    new SummonAttackEntry(
                        attackEntry.getMonsterOid(),
                        additionalDmg.get(i).getDamage() + addedDmg
                    )
                );
                attackEntry = new SummonAttackEntry(attackEntry.getMonsterOid(), newDmg);
                allDamage.set(i, attackEntry);
            }

            int damage = attackEntry.getDamage();
            if (damage >= 100000000) {
                AutobanManager
                    .getInstance()
                    .autoban(
                        player.getClient(),
                        player.getName() +
                            "'s summon dealt " +
                            damage +
                            " to monster " +
                            target.getId() +
                            "."
                    );
            }

            if (damage > 0 && !summonEffect.getMonsterStati().isEmpty()) {
                if (summonEffect.makeChanceResult()) {
                    MonsterStatusEffect monsterStatusEffect =
                        new MonsterStatusEffect(
                            summonEffect.getMonsterStati(),
                            summonSkill,
                            false
                        );
                    target.applyStatus(player, monsterStatusEffect, summonEffect.isPoison(), 4000);
                }
            }

            player.getMap().damageMonster(player, target, damage);
            player.checkMonsterAggro(target);
        }

        player
            .getMap()
            .broadcastMessage(
                player,
                MaplePacketCreator.summonAttack(
                    player.getId(),
                    summon.getSkill(),
                    summon.getStance(),
                    allDamage
                ),
                summon.getPosition()
            );

        additionalDmg.stream().filter(d -> d.getDamage() != 0).forEach(ad ->
            c.getSession().write(
                MaplePacketCreator.damageMonster(
                    ad.getMonsterOid(),
                    ad.getDamage()
                )
            )
        );
    }
}
