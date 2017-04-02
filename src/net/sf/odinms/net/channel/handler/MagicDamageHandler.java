package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.Element;
import net.sf.odinms.server.life.ElementalEffectiveness;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class MagicDamageHandler extends AbstractDealDamageHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        AttackInfo attack = parseDamage(slea, false);
        final MapleCharacter player = c.getPlayer();

        if (!player.canQuestEffectivelyUseSkill(attack.skill) || player.getMap().isDamageMuted()) {
            for (int i = 0; i < attack.allDamage.size(); ++i) {
                Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                MapleMonster monster = null;
                if (dmg != null) monster = player.getMap().getMonsterByOid(dmg.getLeft());
                if (monster == null) continue;
                List<Integer> additionalDmg = new ArrayList<>(dmg.getRight().size());
                for (Integer dmgNumber : dmg.getRight()) {
                    additionalDmg.add(-dmgNumber);
                }
                for (Integer additionald : additionalDmg) {
                    c.getSession().write(MaplePacketCreator.damageMonster(dmg.getLeft(), additionald));
                }
            }
            return;
        }

        int totalMagic = player.getTotalMagic();
        final ISkill skillUsed = SkillFactory.getSkill(attack.skill);
        if (totalMagic > 1999) {
            int skillLevel = player.getSkillLevel(skillUsed);
            boolean isHeal = skillUsed.getId() == 2301002;
            double mastery = ((double) skillUsed.getEffect(skillLevel).getMastery() * 5.0d + 10.0d) / 100.0d;
            double spellAttack = (double) skillUsed.getEffect(skillLevel).getMatk();
            ISkill eleAmp = SkillFactory.getSkill(2110001);
            int eleAmpLevel;
            if (player.getSkillLevel(eleAmp) > 0) {
                eleAmpLevel = player.getSkillLevel(eleAmp);
            } else {
                eleAmp = SkillFactory.getSkill(2210001);
                eleAmpLevel = player.getSkillLevel(eleAmp);
            }
            double eleAmpMulti;
            if (eleAmpLevel > 0) {
                eleAmpMulti = (double) eleAmp.getEffect(player.getSkillLevel(eleAmp)).getY() / 100.0d;
            } else {
                eleAmpMulti = 1.0d;
            }

            int baseMin, baseMax, min, max;
            double baseRange;
            if (isHeal) {
                baseMin = (int) (((double) player.getTotalInt() * 0.3d + (double) player.getTotalLuk()) * 1999.0d / 1000.0d * (1.5d + 5.0d / (double) (attack.allDamage.size() + 1)));
                baseMax = (int) (((double) player.getTotalInt() * 1.2d + (double) player.getTotalLuk()) * 1999.0d / 1000.0d * (1.5d + 5.0d / (double) (attack.allDamage.size() + 1)));
                baseRange = (double) (baseMax - baseMin);
                min = (int) (((double) player.getTotalInt() * 0.3d + (double) player.getTotalLuk()) * (double) totalMagic / 1000.0d * (1.5d + 5.0d / (double) (attack.allDamage.size() + 1)));
                max = (int) (((double) player.getTotalInt() * 1.2d + (double) player.getTotalLuk()) * (double) totalMagic / 1000.0d * (1.5d + 5.0d / (double) (attack.allDamage.size() + 1)));
            } else {
                baseMin = (int) (((1999.0d * 1999.0d / 1000.0d + 1999.0d * mastery * 0.9d) / 30.0d + player.getTotalInt() / 200.0d) * spellAttack * eleAmpMulti);
                baseMax = (int) (((1999.0d * 1999.0d / 1000.0d + 1999.0d) / 30.0d + player.getTotalInt() / 200.0d) * spellAttack * eleAmpMulti);
                baseRange = (double) (baseMax - baseMin);
                min = (int) ((((double) (totalMagic * totalMagic) / 1000.0d + (double) totalMagic * mastery * 0.9d) / 30.0d + player.getTotalInt() / 200.0d) * spellAttack * eleAmpMulti);
                max = (int) ((((double) (totalMagic * totalMagic) / 1000.0d + (double) totalMagic) / 30.0d + player.getTotalInt() / 200.0d) * spellAttack * eleAmpMulti);
            }
            double range = (double) (max - min);

            for (int i = 0; i < attack.allDamage.size(); ++i) {
                Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                final MapleMonster m = dmg == null ? null : player.getMap().getMonsterByOid(dmg.getLeft());
                if (m == null || m.isBuffed(MonsterStatus.MAGIC_IMMUNITY) || dmg.getRight() == null) {
                    continue;
                }
                List<Integer> additionalDmg = new ArrayList<>(dmg.getRight().size());
                List<Integer> newDmg = new ArrayList<>(dmg.getRight().size());
                for (Integer dmgNumber : dmg.getRight()) {
                    if (dmgNumber == null) continue;
                    double dmgPercentile = (double) (dmgNumber - baseMin) / baseRange;
                    int newDmgNumber = min + (int) (range * dmgPercentile);
                    additionalDmg.add(newDmgNumber - dmgNumber);
                    newDmg.add(newDmgNumber);
                }
                attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newDmg));
                for (Integer additionald : additionalDmg) {
                    player.getMap().broadcastMessage(
                        player,
                        MaplePacketCreator.damageMonster(dmg.getLeft(), additionald),
                        true
                    );
                }
            }
        }

        for (int i = 0; i < attack.allDamage.size(); ++i) {
            Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
            MapleMonster monster = null;
            if (dmg != null) monster = player.getMap().getMonsterByOid(dmg.getLeft());
            if (monster == null) continue;
            if (monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) continue;
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
            List<Integer> additionalDmg = new ArrayList<>(dmg.getRight().size());
            List<Integer> newDmg = new ArrayList<>(dmg.getRight().size());
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
            for (Integer dmgNumber : dmg.getRight()) {
                additionalDmg.add((int) (dmgNumber * (multiplier - 1.0d)));
                newDmg.add((int) (dmgNumber * multiplier));
            }
            attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newDmg));
            for (Integer additionald : additionalDmg) {
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

        if ((player.getDeathPenalty() > 0 || player.getQuestEffectiveLevel() > 0) && attack.allDamage != null) {
            double dpMultiplier = 1.0d;
            double qeMultiplier = player.getQuestEffectiveLevelDmgMulti();
            if (player.getDeathPenalty() > 0) {
                dpMultiplier = Math.max(
                    1.0d - (double) player.getDeathPenalty() * 0.03d,
                    0.0d
                );
            }
            double totalMultiplier = dpMultiplier * qeMultiplier;

            for (int i = 0; i < attack.allDamage.size(); ++i) {
                Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                if (dmg == null || dmg.getLeft() == null || dmg.getRight() == null) continue;
                List<Integer> additionaldmg = new ArrayList<>(dmg.getRight().size());
                List<Integer> newdmg = new ArrayList<>(dmg.getRight().size());
                for (Integer dmgnumber : dmg.getRight()) {
                    if (dmgnumber == null) continue;
                    additionaldmg.add((int) (dmgnumber * (totalMultiplier - 1.0d)));
                    newdmg.add((int) (dmgnumber * totalMultiplier));
                }
                attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                for (Integer additionald : additionaldmg) {
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

        MaplePacket packet;
        if (attack.skill == 2121001 || attack.skill == 2221001 || attack.skill == 2321001) {
            packet =
                MaplePacketCreator.magicAttack(
                    player.getId(),
                    attack.skill,
                    attack.stance,
                    attack.numAttackedAndDamage,
                    attack.allDamage,
                    attack.charge,
                    attack.speed
                );
        } else {
            packet =
                MaplePacketCreator.magicAttack(
                    player.getId(),
                    attack.skill,
                    attack.stance,
                    attack.numAttackedAndDamage,
                    attack.allDamage,
                    -1,
                    attack.speed
                );
        }
        player.getMap().broadcastMessage(player, packet, false, true);
        MapleStatEffect effect = attack.getAttackEffect(c.getPlayer());
        //int maxdamage;
        // TODO: Fix magic damage calculation
        //maxdamage = 999999;
        ISkill skill = SkillFactory.getSkill(attack.skill);
        MapleStatEffect effect_ = null;
        if (skill != null) {
            int skillLevel = c.getPlayer().getSkillLevel(skill);
            if (skillLevel < 1) {
                System.err.println(
                    c.getPlayer().getName() +
                        " is using a magic skill they don't have: " +
                        skill.getId()
                );
                return;
            }
            effect_ = skill.getEffect(skillLevel);
        }
        if (effect_ != null && effect_.getCooldown() > 0) {
            if (player.skillIsCooling(attack.skill)) {
                //player.getCheatTracker().registerOffense(CheatingOffense.COOLDOWN_HACK);
                return;
            }
            c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect_.getCooldown()));
            ScheduledFuture<?> timer =
                TimerManager.getInstance().schedule(
                    new CancelCooldownAction(
                        c.getPlayer(),
                        attack.skill
                    ),
                    effect_.getCooldown() * 1000L
                );
            player.addCooldown(
                attack.skill,
                System.currentTimeMillis(),
                effect_.getCooldown() * 1000L,
                timer
            );
        }
        applyAttack(attack, player, effect.getAttackCount());
        // MP Eater
        for (int i = 1; i <= 3; ++i) {
            ISkill eaterSkill = SkillFactory.getSkill(2000000 + i * 100000);
            int eaterLevel = 0;
            if (eaterSkill != null) eaterLevel = player.getSkillLevel(eaterSkill);
            if (eaterLevel > 0 && attack.allDamage != null) {
                for (Pair<Integer, List<Integer>> singleDamage : attack.allDamage) {
                    if (singleDamage == null || singleDamage.getLeft() == null) continue;
                    eaterSkill
                        .getEffect(eaterLevel)
                        .applyPassive(
                            player,
                            player
                                .getMap()
                                .getMapObject(
                                    singleDamage.getLeft()
                                )
                        );
                }
                break;
            }
        }
    }
}
