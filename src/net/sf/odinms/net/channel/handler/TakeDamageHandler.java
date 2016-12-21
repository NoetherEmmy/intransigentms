package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.life.*;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.maps.MapleMist;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Iterator;

public class TakeDamageHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        slea.readInt();
        int damagefrom = slea.readByte();
        slea.readByte();
        int damage = slea.readInt();
        int oid = 0;
        int monsteridfrom = 0;
        int pgmr = 0;
        int direction = 0;
        int pos_x = 0;
        int pos_y = 0;
        int fake = 0;
        boolean is_pgmr = false;
        boolean is_pg = true;
        boolean deadlyattack = false;
        int mpattack = 0;
        MapleMonster attacker = null;
        int removeddamage = 0;
        boolean belowLevelLimit = player.getMap().getPartyQuestInstance() != null &&
                                  player.getMap().getPartyQuestInstance().getLevelLimit() > player.getLevel();
        boolean dodge = false;

        float damageScale = player.getDamageScale();
        damage = (int) ((float) damage * damageScale);

        if (damagefrom == -2) {
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
            monsteridfrom = slea.readInt();
            oid = slea.readInt();
            try {
                attacker = (MapleMonster) player.getMap().getMapObject(oid);
            } catch (ClassCastException ignored) {
            }
            try {
                player.setLastDamageSource(MapleLifeFactory.getMonster(monsteridfrom));
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
                System.out.println("Character" + player.getName() + " 5 < cheatTracker.getNumGotMissed() < 15");
                try {
                    c.getChannelServer()
                     .getWorldInterface()
                     .broadcastGMMessage(null,
                         MaplePacketCreator.serverNotice(6,
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
            }
        }

        if (damagefrom != -1 && damagefrom != -2 && attacker != null) {
            MobAttackInfo attackInfo = MobAttackInfoFactory.getMobAttackInfo(attacker, damagefrom);
            if (damage != -1) {
                if (attackInfo != null && attackInfo.isDeadlyAttack()) {
                    deadlyattack = true;
                    mpattack = 0; // mpattack = player.getMp() - 1;
                    if (damage != 0) {
                        damage = 0;
                    } else {
                        return;
                    }
                } else {
                    if (attackInfo != null) {
                        mpattack += attackInfo.getMpBurn();
                    }
                }
                if (player.getMp() - mpattack < 0) {
                    mpattack = player.getMp();
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
            synchronized (player.getMap().getMapObjects()) {
                Iterator<MapleMapObject> mmoiter = player.getMap().getMapObjects().iterator();
                while (mmoiter.hasNext()) {
                    MapleMapObject mmo = mmoiter.next();
                    if (mmo instanceof MapleMist) {
                        MapleMist mist = (MapleMist) mmo;
                        if (mist.getSourceSkill().getId() == 4221006) { // Smokescreen
                            for (MapleMapObject mmoplayer : player.getMap().getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.PLAYER))) {
                                if (player == mmoplayer) {
                                    damage = -1;
                                    smokescreened = true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to handle smokescreen:");
            e.printStackTrace();
        }

        if (damage == -1 && !smokescreened) { // Players with Guardian skill and shield that got damage removed by smokescreen don't get to stun mobs
            int job = player.getJob().getId() / 10 - 40;
            fake = 4020002 + (job * 100000);
            if (damagefrom == -1 && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null) {
                int[] guardianSkillId = {1120005, 1220006};
                for (int guardian : guardianSkillId) {
                    ISkill guardianSkill = SkillFactory.getSkill(guardian);
                    if (player.getSkillLevel(guardianSkill) > 0 && attacker != null) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.STUN, 1), guardianSkill, false);
                        attacker.applyStatus(player, monsterStatusEffect, false, 2 * 1000);
                    }
                }
            }
        }
        if (damage < -1 || damage > 100000) {
            AutobanManager.getInstance().autoban(player.getClient(), "XSource| " + player.getName() + " took " + damage + " of damage.");
        } else if (damage > 60000) {
            System.out.println(player.getName() + " received an abnormal amount of damage and so was disconnected: " + damage);
            c.disconnect();
            return;
        }
        player.getCheatTracker().checkTakeDamage();
        if (belowLevelLimit && damage > 1) {
            removeddamage += damage - 1;
            damage = 1;
        }
        if (player.isInvincible()) {
            removeddamage += damage;
            damage = 0;
        }
        if (damage > 0 && !deadlyattack) {
            player.getCheatTracker().setAttacksWithoutHit(0);
            player.getCheatTracker().resetHPRegen();
            player.resetAfkTime();
            if (player.getPartyQuest() != null && player.getPartyQuest().getMapInstance(player.getMap()) != null) {
                player.getPartyQuest().getMapInstance(player.getMap()).invokeMethod("playerHit", player, damage, attacker);
            }
            if (!player.isHidden() && player.isAlive()) {
                if (player.getTotalWdef() > 1999 && damagefrom == -1) {
                    int olddamage = damage;
                    damage = Math.max(damage - (player.getTotalWdef() - 1999), 1);

                    double dodgeChance = (Math.log1p(player.getTotalWdef() - 1999) / Math.log(2.0d)) / 25.0d;
                    if (Math.random() < dodgeChance) {
                        damage = 0;
                        dodge = true;
                    }
                    removeddamage += olddamage - damage;
                }
                if (player.getTotalMdef() > 1999 && (damagefrom == 0 || damagefrom == 1)) {
                    int olddamage = damage;
                    damage = (int) Math.max(damage - Math.pow(player.getTotalMdef() - 1999.0d, 1.2d), 1);
                    removeddamage += olddamage - damage;
                }

                if (player.getBuffedValue(MapleBuffStat.MORPH) != null) {
                    player.cancelMorphs();
                }
                if (!belowLevelLimit && attacker != null) {
                    if (damagefrom == -1 && player.getBuffedValue(MapleBuffStat.POWERGUARD) != null) {
                        int bouncedamage = (int) (damage * (player.getBuffedValue(MapleBuffStat.POWERGUARD).doubleValue() / 100));
                        bouncedamage = Math.min(bouncedamage, attacker.getMaxHp() / 10);
                        player.getMap().damageMonster(player, attacker, bouncedamage);
                        damage -= bouncedamage;
                        removeddamage += bouncedamage;
                        player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(oid, bouncedamage), false, true);
                        player.checkMonsterAggro(attacker);
                    }
                    if (damagefrom == -1 && player.getSkillLevel(SkillFactory.getSkill(2310000)) != 0) {
                        boolean iswearingshield = false;
                        for (IItem item : player.getInventory(MapleInventoryType.EQUIPPED)) {
                            IEquip equip = (IEquip) item;
                            if (equip.getItemId() / 10000 == 109) {
                                iswearingshield = true;
                                break;
                            }
                        }
                        if (iswearingshield) {
                            int bouncedamage = (int) ((double) damage * ((10.0d * (double) player.getSkillLevel(SkillFactory.getSkill(2310000))) / 100.0d));
                            bouncedamage = Math.min(bouncedamage, attacker.getMaxHp() / 2);
                            player.getMap().damageMonster(player, attacker, bouncedamage);
                            player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(oid, bouncedamage), true, true);
                            player.checkMonsterAggro(attacker);
                        }
                    }
                    if ((damagefrom == 0 || damagefrom == 1) && player.getBuffedValue(MapleBuffStat.MANA_REFLECTION) != null) {
                        int[] manaReflectSkillId = {2121002, 2221002, 2321002};
                        for (int manaReflect : manaReflectSkillId) {
                            ISkill manaReflectSkill = SkillFactory.getSkill(manaReflect);
                            if (player.isBuffFrom(MapleBuffStat.MANA_REFLECTION, manaReflectSkill) && player.getSkillLevel(manaReflectSkill) > 0 && manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).makeChanceResult()) {
                                int bouncedamage = damage * (manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).getX() / 100);
                                if (bouncedamage > attacker.getMaxHp() * 0.2d) {
                                    bouncedamage = (int) (attacker.getMaxHp() * 0.2d);
                                }
                                player.getMap().damageMonster(player, attacker, bouncedamage);
                                player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(oid, bouncedamage), true);
                                player.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(manaReflect, 5));
                                player.getMap().broadcastMessage(player, MaplePacketCreator.showBuffeffect(player.getId(), manaReflect, 5, (byte) 3), false);
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
                                int olddamage = damage;
                                int newdamage = (int) (multiplier * (double) damage);
                                removeddamage += Math.max(olddamage - newdamage, 0);
                                damage = newdamage;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to handle achilles: " + e);
                    }
                }
                if (!belowLevelLimit && player.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null && mpattack == 0) {
                    int mploss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0d));
                    int hploss = damage - mploss;
                    int hypotheticalmploss = 0;
                    if (player.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                        hypotheticalmploss = mploss;
                        mploss = 0;
                    }
                    if (mploss > player.getMp()) {
                        hploss += mploss - player.getMp();
                        mploss = player.getMp();
                    }
                    player.addMPHP(-hploss, -mploss);
                    damage = hploss;
                    if (hypotheticalmploss > 0) {
                        mploss = hypotheticalmploss;
                    }
                    removeddamage += mploss;
                } else if (!belowLevelLimit && player.getBuffedValue(MapleBuffStat.MESOGUARD) != null) {
                    int olddamage = damage;
                    damage = (damage % 2 == 0) ? damage / 2 : (damage / 2) + 1; // Damage rounds up!
                    removeddamage += olddamage - damage;
                    int mesoloss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MESOGUARD).doubleValue() / 100.0d));
                    if (player.getMeso() < mesoloss) {
                        player.gainMeso(-player.getMeso(), false);
                        player.cancelBuffStats(MapleBuffStat.MESOGUARD);
                    } else {
                        player.gainMeso(-mesoloss, false);
                    }
                    player.addMPHP(-damage, -mpattack);
                } else {
                    player.addMPHP(-damage, -mpattack);
                }

                if (deadlyattack) {
                    player.addMPHP(damage, 0);
                    damage = 0;
                }
                if (MapleLifeFactory.getMonster(monsteridfrom) != null) {
                    player.getMap().broadcastMessage(player, MaplePacketCreator.damagePlayer(damagefrom, monsteridfrom, player.getId(), damage, fake, direction, is_pgmr, pgmr, is_pg, oid, pos_x, pos_y), false);
                }
                if (player.getTrueDamage()) {
                    player.sendHint("#e" + (dodge ? "MISS! " : "") + "#r" + damage + "#k#n" + (removeddamage > 0 ? " #e#b(" + removeddamage + ")#k#n" : ""), 0, 0);
                }
                if (player.getMount() != null && (player.getMount().getSkillId() == 5221006 && player.getMount().isActive())) {
                    player.decrementBattleshipHp(damage);
                }
            }
        }
    }
}
