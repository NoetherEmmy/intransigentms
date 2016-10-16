package net.sf.odinms.net.channel.handler;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.InventoryException;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleWeaponType;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.Element;
import net.sf.odinms.server.life.ElementalEffectiveness;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class RangedAttackHandler extends AbstractDealDamageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RangedAttackHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        AttackInfo attack = parseDamage(slea, true);
        MapleCharacter player = c.getPlayer();
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
                                multiplier = 1.5d;
                                break;
                            case STRONG:
                                multiplier = 0.5d;
                                break;
                            case IMMUNE:
                                multiplier = 0.0d;
                                break;
                            default:
                                multiplier = 1.0d;
                                break;
                        }
                        if (multiplier != 1.0d) {
                            for (Integer dmgnumber : dmg.getRight()) {
                                additionaldmg.add((int) (dmgnumber * (multiplier - 1.0d)));
                                newdmg.add((int) (dmgnumber * multiplier));
                            }
                            attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                            for (Integer additionald : additionaldmg) {
                                player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(dmg.getLeft(), additionald), true);
                            }
                        }
                    }
                }
            }
            
            if (skillused != null && skillused.getId() == 5211004 && player.getItemQuantity(2331000, false) > 0) {
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, 2331000, 1, false, true);
                attack.charge = 1;
                double capsulemultiplier = (skillused.getEffect(player.getSkillLevel(skillused)).getDamage() + 40.0) / (double) skillused.getEffect(player.getSkillLevel(skillused)).getDamage();
                for (int i = 0; i < attack.allDamage.size(); ++i) {
                    Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                    if (dmg != null) {
                        List<Integer> additionaldmg = new LinkedList<>();
                        List<Integer> newdmg = new LinkedList<>();
                        for (Integer dmgnumber : dmg.getRight()) {
                            additionaldmg.add((int) (dmgnumber * (capsulemultiplier - 1.0)));
                            newdmg.add((int) (dmgnumber * capsulemultiplier));
                        }
                        attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                        for (Integer additionald : additionaldmg) {
                            player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(dmg.getLeft(), additionald), true);
                        }
                    }
                }
            }
            
            if (skillused != null && skillused.getId() == 5211005 && player.getItemQuantity(2332000, false) > 0) {
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, 2332000, 1, false, true);
                attack.charge = 1;
            }
            
            if (player.getDeathPenalty() > 0) {
                double dpmultiplier = Math.max(1.0 - (double) player.getDeathPenalty() * 0.03, 0.0);
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

            if (attack.skill == 5121002) {
                player.getMap().broadcastMessage(player, MaplePacketCreator.rangedAttack(player.getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, 0, attack.allDamage, attack.speed), false);
                applyAttack(attack, player, 1);
            } else {
                MapleInventory equip = player.getInventory(MapleInventoryType.EQUIPPED);
                IItem weapon = equip.getItem((byte) -11);
                MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
                MapleWeaponType type = mii.getWeaponType(weapon.getItemId());
                if (type == MapleWeaponType.NOT_A_WEAPON) {
                    throw new RuntimeException("[h4x] Player " + player.getName() + " is attacking with something that's not a weapon");
                }
                MapleInventory use = player.getInventory(MapleInventoryType.USE);
                int projectile = 0;
                int bulletCount = 1;

                MapleStatEffect effect = null;
                if (attack.skill != 0) {
                    effect = attack.getAttackEffect(c.getPlayer());
                    bulletCount = effect.getBulletCount();
                    if (effect.getCooldown() > 0) {
                        c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
                    }
                }
                boolean hasShadowPartner = player.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null;
                int damageBulletCount = bulletCount;
                if (hasShadowPartner) {
                    bulletCount *= 2;
                }
                for (int i = 0; i < 255; ++i) {
                    IItem item = use.getItem((byte) i);
                    if (item != null) {
                        boolean clawCondition = type == MapleWeaponType.CLAW && mii.isThrowingStar(item.getItemId()) && weapon.getItemId() != 1472063;
                        boolean bowCondition = type == MapleWeaponType.BOW && mii.isArrowForBow(item.getItemId());
                        boolean crossbowCondition = type == MapleWeaponType.CROSSBOW && mii.isArrowForCrossBow(item.getItemId());
                        boolean gunCondition = type == MapleWeaponType.GUN && mii.isBullet(item.getItemId());
                        boolean mittenCondition = weapon.getItemId() == 1472063 && (mii.isArrowForBow(item.getItemId()) || mii.isArrowForCrossBow(item.getItemId()));
                        if ((clawCondition || bowCondition || crossbowCondition || mittenCondition || gunCondition) && item.getQuantity() >= bulletCount) {
                            projectile = item.getItemId();
                            break;
                        }
                    }
                }
                boolean soulArrow = player.getBuffedValue(MapleBuffStat.SOULARROW) != null;
                boolean shadowClaw = player.getBuffedValue(MapleBuffStat.SHADOW_CLAW) != null;
                if (!soulArrow && !shadowClaw && projectile != 0) {
                    int bulletConsume = bulletCount;
                    if (effect != null && effect.getBulletConsume() != 0) {
                        bulletConsume = effect.getBulletConsume() * (hasShadowPartner ? 2 : 1);
                    }
                    try {
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, bulletConsume, false, true);
                    } catch (InventoryException ie) {
                        String projectilename = mii.getName(projectile);
                        player.dropMessage(5, "You do not have enough " + projectilename + "s to use that skill.");
                        return;
                    }
                }

                if (projectile != 0 || soulArrow) {
                    int visProjectile = projectile; // Visible projectile sent to players
                    if (mii.isThrowingStar(projectile)) {
                        //see if player has cash stars
                        MapleInventory cash = player.getInventory(MapleInventoryType.CASH);
                        for (int i = 0; i < 255; ++i) {
                            IItem item = cash.getItem((byte) i);
                            if (item != null) {
                                //cash stars have prefix 5021xxx
                                if (item.getItemId() / 1000 == 5021) {
                                    visProjectile = item.getItemId();
                                    break;
                                }
                            }
                        }
                    } else { // Bow, Crossbow
                        if (soulArrow || attack.skill == 3111004 || attack.skill == 3211004) {
                            visProjectile = 0; // arrow rain/eruption show no arrows
                        }
                    }

                    MaplePacket packet;
                    try {
                        switch (attack.skill) {
                            case 3121004: // Hurricane
                            case 3221001: // Pierce
                            case 5221004: // Rapid Fire
                                packet = MaplePacketCreator.rangedAttack(player.getId(), attack.skill, attack.direction, attack.numAttackedAndDamage, visProjectile, attack.allDamage, attack.speed);
                                break;
                            default:
                                packet = MaplePacketCreator.rangedAttack(player.getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, visProjectile, attack.allDamage, attack.speed);
                                break;
                        }
                        player.getMap().broadcastMessage(player, packet, false, true);
                    } catch (Exception e) {
                        log.warn("Failed to handle ranged attack.", e);
                    }

                    //int basedamage;
                    //int projectileWatk = 0;
                    /*
                    if (projectile != 0) {
                        projectileWatk = mii.getWatkForProjectile(projectile);
                    }
                    if (attack.skill != 4001344) { // not lucky 7
                        if (projectileWatk != 0) {
                            basedamage = c.getPlayer().calculateMaxBaseDamage(c.getPlayer().getTotalWatk() + projectileWatk);
                        } else {
                            basedamage = c.getPlayer().getCurrentMaxBaseDamage();
                        }
                    } else { // l7 has a different formula :>
                        basedamage = (int) (((c.getPlayer().getTotalLuk() * 5.0) / 100.0) * (c.getPlayer().getTotalWatk() + projectileWatk));
                    }
                    if (attack.skill == 3101005) { // arrowbomb is hardcore like that O.o
                        basedamage *= effect.getX() / 100.0;
                    }
                    */
                    //int maxdamage = basedamage;
                    /*
                    double critdamagerate = 0.0;
                    if (player.getJob().isA(MapleJob.ASSASSIN)) {
                        ISkill criticalthrow = SkillFactory.getSkill(4100001);
                        int critlevel = player.getSkillLevel(criticalthrow);
                        if (critlevel > 0) {
                            critdamagerate = (criticalthrow.getEffect(player.getSkillLevel(criticalthrow)).getDamage() / 100.0);
                        }
                    } else if (player.getJob().isA(MapleJob.BOWMAN)) {
                        ISkill criticalshot = SkillFactory.getSkill(3000001);
                        int critlevel = player.getSkillLevel(criticalshot);
                        if (critlevel > 0) {
                            critdamagerate = (criticalshot.getEffect(critlevel).getDamage() / 100.0) - 1.0;
                        }
                    }
                    */
                    //int critdamage = (int) (basedamage * critdamagerate);
                    /*
                    if (effect != null) {
                        maxdamage *= effect.getDamage() / 100.0;
                    }
                    maxdamage += critdamage;
                    maxdamage *= damageBulletCount;
                    if (hasShadowPartner) {
                        ISkill shadowPartner = SkillFactory.getSkill(4111002);
                        int shadowPartnerLevel = player.getSkillLevel(shadowPartner);
                        MapleStatEffect shadowPartnerEffect = shadowPartner.getEffect(shadowPartnerLevel);
                        if (attack.skill != 0) {
                            maxdamage *= (1.0 + shadowPartnerEffect.getY() / 100.0);
                        } else {
                            maxdamage *= (1.0 + shadowPartnerEffect.getX() / 100.0);
                        }
                    }
                    if (attack.skill == 4111004) {
                        maxdamage = 35000;
                    }
                    */
                    if (effect != null) {
                        int money = effect.getMoneyCon();
                        if (money != 0) {
                            double moneyMod = money * 0.5;
                            money = (int) (money + Math.random() * moneyMod);
                            if (money > player.getMeso()) {
                                money = player.getMeso();
                            }
                            player.gainMeso(-money, false);
                        }
                    }
                    if (attack.skill != 0) {
                        ISkill skill = SkillFactory.getSkill(attack.skill);
                        int skillLevel = c.getPlayer().getSkillLevel(skill);
                        MapleStatEffect effect_ = skill.getEffect(skillLevel);
                        if (effect_.getCooldown() > 0) {
                            if (player.skillIsCooling(attack.skill)) {
                                //player.getCheatTracker().registerOffense(CheatingOffense.COOLDOWN_HACK);
                                return;
                            } else {
                                c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect_.getCooldown()));
                                ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(c.getPlayer(), attack.skill), effect_.getCooldown() * 1000);
                                player.addCooldown(attack.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, timer);
                            }
                        }
                    }
                    applyAttack(attack, player, bulletCount);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}