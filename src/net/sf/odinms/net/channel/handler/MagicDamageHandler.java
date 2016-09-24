package net.sf.odinms.net.channel.handler;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.Element;
import net.sf.odinms.server.life.ElementalEffectiveness;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class MagicDamageHandler extends AbstractDealDamageHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        AttackInfo attack = parseDamage(slea, false);
        MapleCharacter player = c.getPlayer();
        //
        try {
            ISkill skillused = SkillFactory.getSkill(attack.skill);
            if (skillused != null && skillused.getElement() != Element.NEUTRAL) {
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    MapleMonster monster = null;
                    if (dmg != null) {
                        monster = player.getMap().getMonsterByOid(dmg.getLeft());
                    }
                    if (monster != null && dmg != null) {
                        ElementalEffectiveness ee = monster.getAddedEffectiveness(skillused.getElement());
                        if ((ee == ElementalEffectiveness.WEAK || ee == ElementalEffectiveness.IMMUNE) && monster.getEffectiveness(skillused.getElement()) == ElementalEffectiveness.WEAK) {
                            continue;
                        }
                        double multiplier;
                        List<Integer> additionaldmg = new LinkedList<>();
                        List<Integer> newdmg = new LinkedList<>();
                        switch (ee) {
                            case WEAK:
                                multiplier = 1.5;
                                for (Integer dmgnumber : dmg.getRight()) {
                                    additionaldmg.add((int) (dmgnumber * (multiplier - 1.0)));
                                    newdmg.add((int) (dmgnumber * multiplier));
                                }
                                attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                                for (Integer additionald : additionaldmg) {
                                    player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(dmg.getLeft(), additionald), true);
                                }
                                break;
                            case STRONG:
                                multiplier = 0.5;
                                for (Integer dmgnumber : dmg.getRight()) {
                                    additionaldmg.add((int) (dmgnumber * (multiplier - 1.0)));
                                    newdmg.add((int) (dmgnumber * multiplier));
                                }
                                attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                                for (Integer additionald : additionaldmg) {
                                    player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(dmg.getLeft(), additionald), true);
                                }
                                break;
                            case IMMUNE:
                                multiplier = 0.0;
                                for (Integer dmgnumber : dmg.getRight()) {
                                    additionaldmg.add((int) (dmgnumber * (multiplier - 1.0)));
                                    newdmg.add((int) (dmgnumber * multiplier));
                                }
                                attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                                for (Integer additionald : additionaldmg) {
                                    player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(dmg.getLeft(), additionald), true);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
            
            if (player.getDeathPenalty() > 0) {
                double dpmultiplier = Math.max((double) 1.0 - (double) player.getDeathPenalty() * 0.04, (double) 0.0);
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    if (dmg != null) {
                        List<Integer> additionaldmg = new LinkedList<>();
                        List<Integer> newdmg = new LinkedList<>();
                        for (Integer dmgnumber : dmg.getRight()) {
                            additionaldmg.add((int) (dmgnumber * (dpmultiplier - 1.0)));
                            newdmg.add((int) (dmgnumber * dpmultiplier));
                        }
                        attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                        for (Integer additionald : additionaldmg) {
                            //c.getSession().write(MaplePacketCreator.damageMonster(dmg.getLeft(), additionald));
                            player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(dmg.getLeft(), additionald), true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //
        MaplePacket packet = MaplePacketCreator.magicAttack(player.getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, attack.allDamage, -1, attack.speed);
        if (attack.skill == 2121001 || attack.skill == 2221001 || attack.skill == 2321001) {
            packet = MaplePacketCreator.magicAttack(player.getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.charge, attack.speed);
        }
        player.getMap().broadcastMessage(player, packet, false, true);
        MapleStatEffect effect = attack.getAttackEffect(c.getPlayer());
        int maxdamage;
        // TODO fix magic damage calculation
        maxdamage = 99999;
        ISkill skill = SkillFactory.getSkill(attack.skill);
        int skillLevel = c.getPlayer().getSkillLevel(skill);
        MapleStatEffect effect_ = skill.getEffect(skillLevel);
        if (effect_.getCooldown() > 0) {
            if (player.skillisCooling(attack.skill)) {
                //player.getCheatTracker().registerOffense(CheatingOffense.COOLDOWN_HACK);
                return;
            } else {
                c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect_.getCooldown()));
                ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(c.getPlayer(), attack.skill), effect_.getCooldown() * 1000);
                player.addCooldown(attack.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, timer);
            }
        }
        applyAttack(attack, player, maxdamage, effect.getAttackCount());
        // MP Eater
        for (int i = 1; i <= 3; ++i) {
            ISkill eaterSkill = SkillFactory.getSkill(2000000 + i * 100000);
            int eaterLevel = player.getSkillLevel(eaterSkill);
            if (eaterLevel > 0) {
                for (Pair<Integer, List<Integer>> singleDamage : attack.allDamage) {
                    eaterSkill.getEffect(eaterLevel).applyPassive(player, player.getMap().getMapObject(singleDamage.getLeft()), 0);
                }
                break;
            }
        }
    }
}