package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnergyOrbDamageHandler extends AbstractDealDamageHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        player.resetAfkTime();
        if (player.getEnergyBar() >= 10000) {
            AttackInfo attack = parseDamage(slea, false);

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
                
                if (player.getParty() != null && player.isBareHanded() && player.getTotalInt() >= 350) {
                    // Monk healing
                    final Rectangle bounds = calculateBoundingBox(player.getPosition(), player.isFacingLeft());
                    List<MapleMapObject> affecteds =
                        player.getMap()
                              .getMapObjectsInRect(
                                  bounds,
                                  Collections.singletonList(MapleMapObjectType.PLAYER)
                              );
                    List<MapleCharacter> affectedp = new ArrayList<>(affecteds.size());

                    for (MapleMapObject affectedmo : affecteds) {
                        MapleCharacter affected = (MapleCharacter) affectedmo;

                        if (
                            affected != null &&
                            affected != player &&
                            affected.isAlive() &&
                            player.getParty().equals(affected.getParty())
                        ) {
                            affectedp.add(affected);
                        }
                    }

                    int healing = (int) ((double) dmg.getLeft() * ((double) player.getTotalInt() / 300.0d) * (0.5d + 0.5d / affectedp.size()));
                    for (MapleCharacter affected : affectedp) {
                        affected.setHp(Math.min(affected.getHp() + healing, affected.getMaxHp()));
                        affected.updateSingleStat(MapleStat.HP, affected.getHp());

                        affected.getClient()
                                .getSession()
                                .write(
                                    MaplePacketCreator.showOwnBuffEffect(
                                        5110001,
                                        2
                                    )
                                );
                        
                        affected.getMap().broadcastMessage(
                            affected,
                            MaplePacketCreator.showBuffeffect(
                                affected.getId(),
                                5110001,
                                2,
                                (byte) 3
                            ),
                            false
                        );
                    }
                }
                
                MapleMonster monster = null;
                if (dmg != null) {
                    monster = player.getMap().getMonsterByOid(dmg.getLeft());
                }
                if (monster != null) {
                    double multiplier = monster.getVulnerability();
                    if (multiplier != 1.0d) {
                        List<Integer> additionalDmg = new ArrayList<>(1);
                        List<Integer> newDmg = new ArrayList<>(1);

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

    private Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
        Point mylt, myrb;
        if (facingLeft) {
            mylt = new Point(-300 + posFrom.x, -200 + posFrom.y);
            myrb = new Point(300 + posFrom.x, 200 + posFrom.y);
        } else {
            myrb = new Point(-300 * -1 + posFrom.x, 200 + posFrom.y);
            mylt = new Point(300 * -1 + posFrom.x, -200 + posFrom.y);
        }
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }
}
