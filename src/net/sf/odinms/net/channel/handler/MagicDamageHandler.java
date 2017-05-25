package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;
import java.util.List;

public class MagicDamageHandler extends AbstractDealDamageHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final AttackInfo attack = parseDamage(slea, false);
        final MapleCharacter player = c.getPlayer();

        if (attack.skill > 0 && SkillFactory.getSkill(attack.skill) == null) {
            System.err.println(player.getName() + " is using nonexistent skill: " + attack.skill);
            return;
        }

        final boolean questEffectiveBlock = !player.canQuestEffectivelyUseSkill(attack.skill);
        if (questEffectiveBlock || player.getMap().isDamageMuted()) {
            AbstractDealDamageHandler.cancelDamage(attack, c, questEffectiveBlock);
            return;
        }

        final int totalMagic = player.getTotalMagic();
        final ISkill skillUsed = SkillFactory.getSkill(attack.skill);
        if (totalMagic > 1999) {
            final int skillLevel = player.getSkillLevel(skillUsed);
            final boolean isHeal = skillUsed.getId() == 2301002;
            final double mastery = ((double) skillUsed.getEffect(skillLevel).getMastery() * 5.0d + 10.0d) / 100.0d;
            final double spellAttack = (double) skillUsed.getEffect(skillLevel).getMatk();
            ISkill eleAmp = SkillFactory.getSkill(2110001);
            final int eleAmpLevel;
            if (player.getSkillLevel(eleAmp) > 0) {
                eleAmpLevel = player.getSkillLevel(eleAmp);
            } else {
                eleAmp = SkillFactory.getSkill(2210001);
                eleAmpLevel = player.getSkillLevel(eleAmp);
            }
            final double eleAmpMulti;
            if (eleAmpLevel > 0) {
                eleAmpMulti = (double) eleAmp.getEffect(player.getSkillLevel(eleAmp)).getY() / 100.0d;
            } else {
                eleAmpMulti = 1.0d;
            }

            final int baseMin, baseMax,
                      min,     max;
            final double baseRange;
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
            final double range = (double) (max - min);

            for (int i = 0; i < attack.allDamage.size(); ++i) {
                final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                final MapleMonster m = dmg == null ? null : player.getMap().getMonsterByOid(dmg.getLeft());
                if (m == null || m.isBuffed(MonsterStatus.MAGIC_IMMUNITY) || dmg.getRight() == null) {
                    continue;
                }
                final List<Integer> additionalDmg = new ArrayList<>(dmg.getRight().size());
                final List<Integer> newDmg = new ArrayList<>(dmg.getRight().size());
                dmg.getRight().forEach(dmgNumber -> {
                    if (dmgNumber == null) return;
                    final double dmgPercentile = (double) (dmgNumber - baseMin) / baseRange;
                    final int newDmgNumber = min + (int) (range * dmgPercentile);
                    additionalDmg.add(newDmgNumber - dmgNumber);
                    newDmg.add(newDmgNumber);
                });
                attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newDmg));
                additionalDmg.stream().filter(d -> d != 0).forEach(additionald ->
                    player.getMap().broadcastMessage(
                        player,
                        MaplePacketCreator.damageMonster(dmg.getLeft(), additionald),
                        true
                    )
                );
            }
        }

        AbstractDealDamageHandler.baseMultDamage(attack, c, skillUsed, 1);

        if ((player.getDeathPenalty() > 0 || player.getQuestEffectiveLevel() > 0) && attack.allDamage != null) {
            AbstractDealDamageHandler.applyDamagePenalty(attack, c);
        }

        final MaplePacket packet;
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

        final MapleStatEffect effect = attack.getAttackEffect(c.getPlayer());

        //int maxdamage;
        // TODO: Fix magic damage calculation
        //maxdamage = 999999;

        if (skillUsed != null) {
            if (!AbstractDealDamageHandler.processSkill(attack, c)) return;
        }

        applyAttack(attack, player, effect.getAttackCount());

        // MP Eater
        for (int i = 1; i < 4; ++i) {
            final ISkill eaterSkill = SkillFactory.getSkill(2000000 + i * 100000);
            final int eaterLevel = eaterSkill != null ? player.getSkillLevel(eaterSkill) : 0;
            if (eaterLevel > 0 && attack.allDamage != null) {
                attack.allDamage.forEach(singleDamage -> {
                    if (singleDamage == null || singleDamage.getLeft() == null) return;
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
                });
                break;
            }
        }
    }
}
