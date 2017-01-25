package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.life.*;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.maps.MapleMist;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class TakeDamageHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        slea.readInt();
        int damageFrom = slea.readByte();
        slea.readByte();
        int damage = slea.readInt();
        int oid = 0;
        int monsterIdFrom = 0;
        int pgmr = 0;
        int direction = 0;
        int pos_x = 0;
        int pos_y = 0;
        int fake = 0;
        boolean is_pgmr = false;
        boolean is_pg = true;
        boolean deadlyAttack = false;
        int mpAttack = 0;
        MapleMonster attacker = null;
        int removedDamage = 0;
        boolean belowLevelLimit = player.getMap().getPartyQuestInstance() != null &&
                                  player.getMap().getPartyQuestInstance().getLevelLimit() > player.getLevel();
        boolean dodge = false;
        boolean advaita = false;

        if (player.getMap().isDamageMuted()) {
            return;
        }

        float damageScale = player.getDamageScale();
        damage = (int) ((float) damage * damageScale);

        if (damageFrom == -2) {
            int debuffLevel = slea.readByte();
            int debuffId = slea.readByte();
            if (debuffId == 125) {
                debuffLevel = debuffLevel - 1;
            }
            MobSkill skill = MobSkillFactory.getMobSkill(debuffId, debuffLevel);
            if (skill != null) {
                try {
                    attacker = (MapleMonster) player.getMap().getMapObject(oid);
                } catch (ClassCastException cce) {
                    cce.printStackTrace();
                }
                if (attacker != null) {
                    skill.applyEffect(player, attacker, false);
                }
            }
        } else {
            monsterIdFrom = slea.readInt();
            oid = slea.readInt();
            try {
                attacker = (MapleMonster) player.getMap().getMapObject(oid);
            } catch (ClassCastException ignored) {
            }
            try {
                player.setLastDamageSource(MapleLifeFactory.getMonster(monsterIdFrom));
            } catch (Exception e) {
                e.printStackTrace();
            }
            direction = slea.readByte();
        }

        if (c.getChannelServer().getTrackMissGodmode() && attacker != null) {
            if (damage < 1) {
                double difference = (double) Math.max(player.getLevel() - attacker.getLevel(), 0);
                double chanceToBeHit = (double) attacker.getAccuracy() / ((1.84d + 0.07d * difference) * (double) player.getAvoidability()) - 1.0d;
                if (chanceToBeHit > 0.85d) {
                    player.getCheatTracker().incrementNumGotMissed();
                }
            } else {
                player.getCheatTracker().setNumGotMissed(0);
            }
            if (player.getCheatTracker().getNumGotMissed() > 5 && player.getCheatTracker().getNumGotMissed() < 15) {
                System.out.println("Character " + player.getName() + ": 5 < cheatTracker.getNumGotMissed() < 15");
                try {
                    c.getChannelServer()
                     .getWorldInterface()
                     .broadcastGMMessage(null,
                         MaplePacketCreator.serverNotice(
                             6,
                             "WARNING: The player with name " +
                                 MapleCharacterUtil.makeMapleReadable(c.getPlayer().getName()) +
                                 " on channel " +
                                 c.getChannel() +
                                 " MAY be using miss godmode."
                         ).getBytes()
                     );
                } catch (RemoteException re) {
                    re.printStackTrace();
                    c.getChannelServer().reconnectWorld();
                }
            } else if (player.getCheatTracker().getNumGotMissed() >= 15) {
                AutobanManager.getInstance().autoban(c, "Miss godmode.");
                return;
            }
        }

        if (damageFrom != -1 && damageFrom != -2 && attacker != null) {
            MobAttackInfo attackInfo = MobAttackInfoFactory.getMobAttackInfo(attacker, damageFrom);
            if (damage != -1) {
                if (attackInfo != null && attackInfo.isDeadlyAttack()) {
                    deadlyAttack = true;
                    mpAttack = 0; // mpAttack = player.getMp() - 1;
                    if (damage != 0) {
                        damage = 0;
                    } else {
                        return;
                    }
                } else {
                    if (attackInfo != null) {
                        mpAttack += attackInfo.getMpBurn();
                    }
                }
                if (player.getMp() - mpAttack < 0) {
                    mpAttack = player.getMp();
                }
            }
            MobSkill skill = null;
            if (attackInfo != null) {
                skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
            }
            if (skill != null && damage > 0) {
                skill.applyEffect(player, attacker, false);
            }
            if (attackInfo != null) {
                attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
            }
        }

        boolean smokescreened = false;
        try {
            Iterator<MapleMapObject> mmoiter = player.getMap().getMapObjects().iterator();
            while (mmoiter.hasNext()) {
                MapleMapObject mmo = mmoiter.next();
                if (mmo instanceof MapleMist) {
                    MapleMist mist = (MapleMist) mmo;
                    if (mist.getSourceSkill().getId() == 4221006) { // Smokescreen
                        List<MapleMapObject> mmoPlayers =
                            player.getMap()
                                  .getMapObjectsInRect(
                                      mist.getBox(),
                                      Collections.singletonList(MapleMapObjectType.PLAYER)
                                  );
                        for (MapleMapObject mmoPlayer : mmoPlayers) {
                            if (player == mmoPlayer) {
                                damage = -1;
                                smokescreened = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to handle smokescreen:");
            e.printStackTrace();
        }

        if (damage == -1 && !smokescreened) { // Players with Guardian skill and shield that got
                                              // damage removed by smokescreen don't get to stun mobs
            int job = player.getJob().getId() / 10 - 40;
            fake = 4020002 + (job * 100000);
            if (damageFrom == -1 && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null) {
                int[] guardianSkillId = {1120005, 1220006};
                for (int guardian : guardianSkillId) {
                    ISkill guardianSkill = SkillFactory.getSkill(guardian);
                    if (player.getSkillLevel(guardianSkill) > 0 && attacker != null) {
                        MonsterStatusEffect monsterStatusEffect =
                            new MonsterStatusEffect(
                                Collections.singletonMap(MonsterStatus.STUN, 1),
                                guardianSkill,
                                false
                            );
                        attacker.applyStatus(player, monsterStatusEffect, false, 2 * 1000);
                    }
                }
            }
        }
        if (damage < -1 || damage > 100000) {
            AutobanManager.getInstance().autoban(
                player.getClient(),
                player.getName() + " took " + damage + " of damage."
            );
        } else if (damage > 60000) {
            System.out.println(
                player.getName() +
                    " received an abnormal amount of damage and so was disconnected: " +
                    damage
            );
            c.disconnect();
            return;
        }
        player.getCheatTracker().checkTakeDamage();
        if (belowLevelLimit && damage > 1) {
            removedDamage += damage - 1;
            damage = 1;
        }
        if (player.isInvincible()) {
            removedDamage += damage;
            damage = 0;
        }
        if (
            !belowLevelLimit &&
            (player.getJob().equals(MapleJob.BUCCANEER) || player.getJob().equals(MapleJob.MARAUDER)) &&
            player.getSkillLevel(SkillFactory.getSkill(5110001)) > 0 &&
            player.isBareHanded() &&
            player.getTotalInt() >= 350
        ) {
            player.handleEnergyChargeGain(2.0d);
        }
        if (damage > 0 && !deadlyAttack) {
            player.getCheatTracker().setAttacksWithoutHit(0);
            player.getCheatTracker().resetHPRegen();
            player.resetAfkTime();
            if (player.getPartyQuest() != null && player.getPartyQuest().getMapInstance(player.getMap()) != null) {
                player.getPartyQuest()
                      .getMapInstance(player.getMap())
                      .invokeMethod("playerHit", player, damage, attacker);
            }
            if (!player.isHidden() && player.isAlive()) {
                if (player.getTotalWdef() > 1999 && damageFrom == -1) {
                    int oldDamage = damage;
                    damage = Math.max(damage - (player.getTotalWdef() - 1999), 1);

                    double dodgeChance = (Math.log1p(player.getTotalWdef() - 1999) / Math.log(2.0d)) / 25.0d;
                    if (Math.random() < dodgeChance) {
                        damage = 0;
                        dodge = true;
                    }
                    removedDamage += oldDamage - damage;
                }
                if (player.getTotalMdef() > 1999 && (damageFrom == 0 || damageFrom == 1) && damage > 0) {
                    int oldDamage = damage;
                    damage = (int) Math.max(damage - Math.pow(player.getTotalMdef() - 1999.0d, 1.2d), 1);
                    removedDamage += oldDamage - damage;
                }
                int advaitaLevel = player.getSkillLevel(5121004);
                if (advaitaLevel > 10 && (damageFrom == 0 || damageFrom == 1)) {
                    double advaitaChance = (double) (advaitaLevel - 10) / 100.0d;
                    int oldDamage = damage;
                    if (Math.random() < advaitaChance) {
                        damage = 0;
                        advaita = true;
                    }
                    removedDamage += oldDamage - damage;
                }

                if (player.getBuffedValue(MapleBuffStat.MORPH) != null) {
                    player.cancelMorphs();
                }
                if (!belowLevelLimit && attacker != null) {
                    if (damageFrom == -1 && player.getBuffedValue(MapleBuffStat.POWERGUARD) != null) {
                        int bounceDamage = (int) (damage * (player.getBuffedValue(MapleBuffStat.POWERGUARD).doubleValue() / 100.0d));
                        bounceDamage = Math.min(bounceDamage, attacker.getMaxHp() / 10);
                        player.getMap().damageMonster(player, attacker, bounceDamage);
                        damage -= bounceDamage;
                        removedDamage += bounceDamage;
                        player.getMap()
                              .broadcastMessage(
                                  player,
                                  MaplePacketCreator.damageMonster(oid, bounceDamage),
                                  false,
                                  true
                              );
                        player.checkMonsterAggro(attacker);
                    }
                    if (damageFrom == -1 && player.getSkillLevel(SkillFactory.getSkill(2310000)) != 0) {
                        boolean isWearingShield = false;
                        for (IItem item : player.getInventory(MapleInventoryType.EQUIPPED)) {
                            IEquip equip = (IEquip) item;
                            if (equip.getItemId() / 10000 == 109) {
                                isWearingShield = true;
                                break;
                            }
                        }
                        if (isWearingShield) {
                            int bounceDamage = (int) ((double) damage * ((10.0d * (double) player.getSkillLevel(SkillFactory.getSkill(2310000))) / 100.0d));
                            bounceDamage = Math.min(bounceDamage, attacker.getMaxHp() / 2);
                            player.getMap().damageMonster(player, attacker, bounceDamage);
                            player.getMap().broadcastMessage(
                                player,
                                MaplePacketCreator.damageMonster(oid, bounceDamage),
                                true,
                                true
                            );
                            player.checkMonsterAggro(attacker);
                        }
                    }
                    if ((damageFrom == 0 || damageFrom == 1) && player.getBuffedValue(MapleBuffStat.MANA_REFLECTION) != null) {
                        int[] manaReflectSkillId = {2121002, 2221002, 2321002};
                        for (int manaReflect : manaReflectSkillId) {
                            ISkill manaReflectSkill = SkillFactory.getSkill(manaReflect);
                            if (
                                player.isBuffFrom(MapleBuffStat.MANA_REFLECTION, manaReflectSkill) &&
                                player.getSkillLevel(manaReflectSkill) > 0 &&
                                manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).makeChanceResult()
                            ) {
                                int bounceDamage = damage * (manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).getX() / 100);
                                if (bounceDamage > attacker.getMaxHp() * 0.2d) {
                                    bounceDamage = (int) (attacker.getMaxHp() * 0.2d);
                                }
                                player.getMap().damageMonster(player, attacker, bounceDamage);
                                player.getMap().broadcastMessage(
                                    player,
                                    MaplePacketCreator.damageMonster(oid, bounceDamage),
                                    true
                                );
                                player.getClient()
                                      .getSession()
                                      .write(MaplePacketCreator.showOwnBuffEffect(manaReflect, 5));
                                player.getMap()
                                      .broadcastMessage(
                                          player,
                                          MaplePacketCreator.showBuffeffect(
                                              player.getId(),
                                              manaReflect,
                                              5,
                                              (byte) 3
                                          ),
                                          false
                                      );
                                break;
                            }
                        }
                    }
                }
                if (!belowLevelLimit) {
                    try {
                        int[] achillesSkillId = {1120004, 1220005, 1320005};
                        for (int achilles : achillesSkillId) {
                            ISkill achillesSkill = SkillFactory.getSkill(achilles);
                            if (player.getSkillLevel(achillesSkill) > 0) {
                                double multiplier = (double) achillesSkill.getEffect(player.getSkillLevel(achillesSkill)).getX() / 1000.0d;
                                int oldDamage = damage;
                                int newDamage = (int) (multiplier * (double) damage);
                                removedDamage += Math.max(oldDamage - newDamage, 0);
                                damage = newDamage;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to handle Achilles: " + e);
                    }
                }

                if (
                    !belowLevelLimit &&
                    (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null ||
                        player.getSkillLevel(5100000) > 0) &&
                    mpAttack == 0
                ) {
                    int mpLoss;
                    if (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
                        mpLoss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0d));
                    } else {
                        mpLoss = (int) (damage * ((2.0d * player.getSkillLevel(5100000) + 5.0d) / 100.0d));
                    }
                    int hpLoss = damage - mpLoss;
                    int hypotheticalMpLoss = 0;
                    if (player.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                        hypotheticalMpLoss = mpLoss;
                        mpLoss = 0;
                    }
                    if (mpLoss > player.getMp()) {
                        hpLoss += mpLoss - player.getMp();
                        mpLoss = player.getMp();
                    }
                    player.addMP(-mpLoss);
                    damage = hpLoss;
                    if (hypotheticalMpLoss > 0) {
                        mpLoss = hypotheticalMpLoss;
                    }
                    removedDamage += mpLoss;
                } else if (!belowLevelLimit && player.getBuffedValue(MapleBuffStat.MESOGUARD) != null) {
                    int oldDamage = damage;
                    damage = (damage % 2 == 0) ? damage / 2 : (damage / 2) + 1; // Damage rounds up!
                    removedDamage += oldDamage - damage;
                    int mesoLoss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MESOGUARD).doubleValue() / 100.0d));
                    if (player.getMeso() < mesoLoss) {
                        player.gainMeso(-player.getMeso(), false);
                        player.cancelBuffStats(MapleBuffStat.MESOGUARD);
                    } else {
                        player.gainMeso(-mesoLoss, false);
                    }
                    player.addMP(-mpAttack);
                } else {
                    player.addMP(-mpAttack);
                }

                if (deadlyAttack) {
                    //player.addMPHP(damage, 0);
                    damage = 0;
                }

                if (player.getParty() != null) {
                    final MaplePartyCharacter thisPartyChr = player.getParty().getMemberById(player.getId());
                    List<MapleCharacter> transformed =
                        player.getMap()
                              .getCharacters()
                              .stream()
                              .filter(p ->
                                  p.getId() != player.getId() &&
                                      p.getParty() != null &&
                                      p.getParty().containsMembers(thisPartyChr) &&
                                      (
                                          (p.getTotalInt() >= 400 && p.isAffectedBySourceId(5111005)) ||
                                          (p.getTotalInt() >= 750 && p.isAffectedBySourceId(5121003))
                                      ) &&
                                      p.isBareHanded()
                              )
                              .sorted(Comparator.comparingDouble(p -> p.getPosition().distanceSq(player.getPosition())))
                              .collect(Collectors.toList());

                    for (MapleCharacter p : transformed) {
                        double absorptionProportion;
                        if (p.isAffectedBySourceId(5111005)) { // Transformation
                            absorptionProportion = (double) (p.getSkillLevel(5111005) / 2) / 100.0d;
                        } else { // Super Transformation
                            absorptionProportion = (double) p.getSkillLevel(5121003) / 100.0d;
                        }
                        int absorbed = (int) (damage * absorptionProportion);
                        removedDamage += absorbed;
                        damage -= absorbed;

                        float absorbedDamageScaling = 1.0f + 200.0f / (float) p.getTotalInt();
                        p.absorbDamage(absorbed * absorbedDamageScaling, damageFrom);
                    }
                }

                player.addHP(-damage);

                if (MapleLifeFactory.getMonster(monsterIdFrom) != null) {
                    player.getMap()
                          .broadcastMessage(
                              player,
                              MaplePacketCreator.damagePlayer(
                                  damageFrom,
                                  monsterIdFrom,
                                  player.getId(),
                                  damage,
                                  fake,
                                  direction,
                                  is_pgmr,
                                  pgmr,
                                  is_pg,
                                  oid,
                                  pos_x,
                                  pos_y
                              ),
                              false
                          );
                }
                if (player.getTrueDamage()) {
                    player.sendHint(
                        "#e" +
                            (dodge ? "MISS! " : (advaita ? "Advaita: " : "")) +
                            "#r" +
                            damage +
                            "#k#n" +
                            (removedDamage > 0 ? " #e#b(" + removedDamage + ")#k#n" : ""),
                        0,
                        0
                    );
                }
                if (
                    player.getMount() != null &&
                    (player.getMount().getSkillId() == 5221006 && player.getMount().isActive())
                ) {
                    player.decrementBattleshipHp(damage);
                }
            }
        }
    }
}
