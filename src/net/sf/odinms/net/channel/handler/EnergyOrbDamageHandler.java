package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;
import java.util.List;

public class EnergyOrbDamageHandler extends AbstractDealDamageHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        player.resetAfkTime();
        if (player.getEnergyBar() >= 10000) {
            AttackInfo attack = parseDamage(slea, false);

            boolean someHit =
                attack.allDamage.size() > 0 &&
                    attack.allDamage
                          .stream()
                          .allMatch(dmg ->
                              dmg.getRight()
                                 .stream()
                                 .anyMatch(num ->
                                     num > 0
                                 )
                          );
            if (
                player.getTotalInt() >= 650 &&
                attack.skill == 5121002 &&
                player.isUnarmed()
            ) {
                // Ahimsa
                c.getSession().write(MaplePacketCreator.giveEnergyCharge(0));
                player.setEnergyBar(0);
                if (someHit) {
                    long duration = (long) player.getSkillLevel(5121002) / 3L * 1000L;
                    player.getMap().setDamageMuted(true, duration);
                }
            }

            if (player.getMap().isDamageMuted()) {
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    MapleMonster monster = null;
                    if (dmg != null) {
                        monster = player.getMap().getMonsterByOid(dmg.getLeft());
                    }
                    if (monster != null) {
                        List<Integer> additionalDmg = new ArrayList<>(dmg.getRight().size());
                        for (Integer dmgNumber : dmg.getRight()) {
                            additionalDmg.add(-dmgNumber);
                        }
                        for (Integer additionald : additionalDmg) {
                            c.getSession().write(MaplePacketCreator.damageMonster(dmg.getLeft(), additionald));
                        }
                    }
                }
                return;
            }

            applyAttack(attack, player, 1);
            player.getMap().broadcastMessage(
                player,
                MaplePacketCreator.closeRangeAttack(
                    player.getId(),
                    attack.skill,
                    attack.stance,
                    attack.numAttackedAndDamage,
                    attack.allDamage,
                    attack.speed
                ),
                false,
                true
            );

            for (int i = 0; i < attack.allDamage.size(); ++i) {
                Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                MapleMonster monster = null;
                if (dmg != null) {
                    monster = player.getMap().getMonsterByOid(dmg.getLeft());
                }
                if (monster != null) {
                    double multiplier = monster.getVulnerability();
                    if (multiplier != 1.0d) {
                        List<Integer> additionalDmg = new ArrayList<>();
                        List<Integer> newDmg = new ArrayList<>();

                        for (Integer dmgNumber : dmg.getRight()) {
                            additionalDmg.add((int) (dmgNumber * (multiplier - 1.0d)));
                            newDmg.add((int) (dmgNumber * multiplier));
                        }
                        attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newDmg));
                        for (Integer additionald : additionalDmg) {
                            player.getMap()
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
            }
        }
    }
}
