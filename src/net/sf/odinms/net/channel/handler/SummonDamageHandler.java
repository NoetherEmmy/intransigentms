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
        private final int monsterOid, damage;

        public SummonAttackEntry(final int monsterOid, final int damage) {
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
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final MapleCharacter player = c.getPlayer();

        MapleSummon summon = null;
        synchronized (player.getSummons()) {
            for (final MapleSummon sum : player.getSummons().values()) {
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

        final ISkill summonSkill = SkillFactory.getSkill(summon.getSkill());
        final MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
        slea.skip(5);
        final int numAttacked = slea.readByte();
        player.getCheatTracker().checkSummonAttack();

        List<SummonAttackEntry> allDamage = new ArrayList<>(numAttacked);
        for (int x = 0; x < numAttacked; ++x) {
            final int monsterOid = slea.readInt(); // Attacked oid.
            slea.skip(14);
            final int damage = slea.readInt();
            allDamage.add(new SummonAttackEntry(monsterOid, damage));
        }

        allDamage =
            allDamage
                .stream()
                .filter(d -> player.getMap().getMonsterByOid(d.getMonsterOid()) != null)
                .collect(Collectors.toCollection(ArrayList::new));

        final boolean questEffectiveBlock = !player.canQuestEffectivelyUseSkill(summon.getSkill());
        if (questEffectiveBlock || player.getMap().isDamageMuted()) {
            allDamage.forEach(attackEntry ->
                c.getSession().write(
                    MaplePacketCreator.damageMonster(
                        attackEntry.getMonsterOid(),
                        -attackEntry.getDamage()
                    )
                )
            );
            if (questEffectiveBlock) {
                player.dropMessage(
                    5,
                    "Your quest effective level (" +
                        player.getQuestEffectiveLevel() +
                        ") is too low to use " +
                        SkillFactory.getSkillName(summon.getSkill()) +
                        "."
                );
            }
            return;
        }

        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }

        final List<SummonAttackEntry> additionalDmg =
            allDamage
                .stream()
                .map(attackEntry -> new SummonAttackEntry(attackEntry.getMonsterOid(), 0))
                .collect(Collectors.toCollection(ArrayList::new));

        if (summonSkill.getId() == 2311006 || summonSkill.getId() == 2321003) { // Summon Dragon | Bahamut
            final double percentPerLevel;
            if (summonSkill.getId() == 2311006) {
                percentPerLevel = 0.05d;
            } else {
                percentPerLevel = 0.07d;
            }
            final int min = player.calculateMinBaseDamage();
            final int max = player.calculateMaxBaseDamage();
            final double skillLevelMultiplier = 1.5d + player.getSkillLevel(summonSkill) * percentPerLevel;
            for (int i = 0; i < allDamage.size(); ++i) {
                if (allDamage.get(i).getDamage() < 1) continue;
                final MapleMonster target = player.getMap().getMonsterByOid(allDamage.get(i).getMonsterOid());
                if (target == null || target.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) continue;
                final int baseDmg = min + (int) (Math.random() * (double) (max - min + 1));
                int thisDmg = allDamage.get(i).getDamage();
                final int addedDmg = (int) (baseDmg * skillLevelMultiplier);
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
            final int str = player.getTotalStr();
            final int dex = player.getTotalDex();
            final int pad = summonEffect.getWatk();
            final double mastery = summonSkill.getId() == 3111005 ? 0.6d : 0.8d;
            final double counterWeight = summonSkill.getId() == 3111005 ? 4.0d : 12.0d;
            final int min = (int) ((str * Math.log(str) * mastery + dex) * pad / counterWeight);
            final int max = (int) ((str * Math.log(str) + dex) * pad / counterWeight);
            for (int i = 0; i < allDamage.size(); ++i) {
                if (allDamage.get(i).getDamage() < 1) continue;
                final MapleMonster target = player.getMap().getMonsterByOid(allDamage.get(i).getMonsterOid());
                if (target == null || target.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) continue;
                final int addedDmg = min + (int) (Math.random() * (double) (max - min + 1));
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
                        final int x = (int) Math.ceil(2.0d * Math.sqrt(Math.max(0, player.getTotalStr() - 400)) / 100.0d);
                        final MonsterStatusEffect monsterStatusEffect =
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
                        final long seconds = (long) Math.ceil((double) player.getSkillLevel(3121006) / 3.0d);
                        target.applyFlame(player, SkillFactory.getSkill(3121006), seconds * 1000L, false);
                    }
                }
            }
        }

        for (int i = 0; i < allDamage.size(); ++i) {
            SummonAttackEntry attackEntry = allDamage.get(i);
            final MapleMonster target = player.getMap().getMonsterByOid(attackEntry.getMonsterOid());
            if (target == null) continue;

            final double multiplier = AbstractDealDamageHandler.getMobDamageMulti(target, summonSkill.getElement());
            final double dpMultiplier;
            final double qeMultiplier = player.getQuestEffectiveLevelDmgMulti();
            if (player.getDeathPenalty() > 0) {
                dpMultiplier = Math.max(
                    1.0d - (double) player.getDeathPenalty() * 0.03d,
                    0.0d
                );
            } else {
                dpMultiplier = 1.0d;
            }
            final double totalMultiplier = multiplier * dpMultiplier * qeMultiplier;

            if (multiplier != 1.0d) {
                final int newDmg = (int) (attackEntry.getDamage() * multiplier);
                final int addedDmg = newDmg - attackEntry.getDamage();
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

            final int damage = attackEntry.getDamage();
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
                    final MonsterStatusEffect monsterStatusEffect =
                        new MonsterStatusEffect(
                            summonEffect.getMonsterStati(),
                            summonSkill,
                            false
                        );
                    target.applyStatus(player, monsterStatusEffect, summonEffect.isPoison(), 4L * 1000L);
                }
            }
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

        allDamage.forEach(attackEntry -> {
            final MapleMonster target = player.getMap().getMonsterByOid(attackEntry.getMonsterOid());
            if (target == null) return;
            player.getMap().damageMonster(player, target, attackEntry.getDamage());
            player.checkMonsterAggro(target);
        });
    }
}
