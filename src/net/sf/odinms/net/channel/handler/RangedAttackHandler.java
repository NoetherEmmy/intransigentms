package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class RangedAttackHandler extends AbstractDealDamageHandler {
    //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RangedAttackHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final AttackInfo attack = parseDamage(slea, true);
        final MapleCharacter player = c.getPlayer();

        if (attack.skill > 0 && SkillFactory.getSkill(attack.skill) == null) {
            System.err.println(player.getName() + " is using nonexistent skill: " + attack.skill);
            return;
        }

        //boolean someHit = true;
            /*
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
                  */
        final boolean questEffectiveBlock = !player.canQuestEffectivelyUseSkill(attack.skill);
        if (
            player.getTotalInt() >= 650 &&
            attack.skill == 5121002 &&
            player.isBareHanded() &&
            !questEffectiveBlock
        ) {
            // Ahimsa
            c.getSession().write(MaplePacketCreator.giveEnergyCharge(0));
            player.setEnergyBar(0);
            final long duration = (long) player.getSkillLevel(5121002) / 3L * 1000L;
            player.getMap().setDamageMuted(true, duration);
        }

        if (questEffectiveBlock || player.getMap().isDamageMuted()) {
            AbstractDealDamageHandler.cancelDamage(attack, c, questEffectiveBlock);
            return;
        }

        final ISkill skillUsed = SkillFactory.getSkill(attack.skill);
        AbstractDealDamageHandler.baseMultDamage(attack, c, skillUsed, 0);

        if (skillUsed != null && skillUsed.getId() == 5211004 && player.getItemQuantity(2331000, false) > 0) {
            // Handling Blaze/Glaze Capsules
            MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, 2331000, 1, false, true);
            attack.charge = 1;
            final double capsulemultiplier =
                (skillUsed.getEffect(player.getSkillLevel(skillUsed)).getDamage() + 40.0d) /
                    (double) skillUsed.getEffect(player.getSkillLevel(skillUsed)).getDamage();
            for (int i = 0; i < attack.allDamage.size(); ++i) {
                final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);
                if (dmg == null) continue;
                final MapleMonster m = player.getMap().getMonsterByOid(dmg.getLeft());
                if (m == null || m.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) continue;
                final List<Integer> additionaldmg = new ArrayList<>(dmg.getRight().size());
                final List<Integer> newdmg = new ArrayList<>(dmg.getRight().size());
                dmg.getRight().forEach(dmgnumber -> {
                    additionaldmg.add((int) (dmgnumber * (capsulemultiplier - 1.0d)));
                    newdmg.add((int) (dmgnumber * capsulemultiplier));
                });
                attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newdmg));
                additionaldmg.stream().filter(d -> d != 0).forEach(additionald ->
                    player.getMap().broadcastMessage(
                        player,
                        MaplePacketCreator.damageMonster(dmg.getLeft(), additionald),
                        true
                    )
                );
            }
        }

        if (skillUsed != null && skillUsed.getId() == 5211005 && player.getItemQuantity(2332000, false) > 0) {
            MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, 2332000, 1, false, true);
            attack.charge = 1;
        }

        if (player.getDeathPenalty() > 0 || player.getQuestEffectiveLevel() > 0) {
            AbstractDealDamageHandler.applyDamagePenalty(attack, c);
        }

        if (attack.skill == 5121002) {
            player.getMap().broadcastMessage(
                player,
                MaplePacketCreator.rangedAttack(
                    player.getId(),
                    attack.skill,
                    attack.stance,
                    attack.numAttackedAndDamage,
                    0,
                    attack.allDamage,
                    attack.speed
                ),
                false
            );
            applyAttack(attack, player, 1);
            return;
        }

        final MapleInventory equip = player.getInventory(MapleInventoryType.EQUIPPED);
        final IItem weapon = equip.getItem((byte) -11);
        if (weapon == null) return;
        final MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        final MapleWeaponType type = mii.getWeaponType(weapon.getItemId());
        if (type == MapleWeaponType.NOT_A_WEAPON) {
            throw new RuntimeException(
                "[h4x] Player " +
                    player.getName() +
                    " is attacking with something that's not a weapon."
            );
        }

        final MapleInventory use = player.getInventory(MapleInventoryType.USE);
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

        final boolean hasShadowPartner = player.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null;
        final int damageBulletCount = bulletCount;
        if (hasShadowPartner && attack.skill != 4121003) bulletCount *= 2;

        for (int i = 0; i < 255; ++i) {
            final IItem item = use.getItem((byte) i);
            if (item == null) continue;
            final boolean clawCondition = type == MapleWeaponType.CLAW && mii.isThrowingStar(item.getItemId()) && weapon.getItemId() != 1472063;
            final boolean bowCondition = type == MapleWeaponType.BOW && mii.isArrowForBow(item.getItemId());
            final boolean crossbowCondition = type == MapleWeaponType.CROSSBOW && mii.isArrowForCrossBow(item.getItemId());
            final boolean gunCondition = type == MapleWeaponType.GUN && mii.isBullet(item.getItemId());
            final boolean mittenCondition = weapon.getItemId() == 1472063 && (mii.isArrowForBow(item.getItemId()) || mii.isArrowForCrossBow(item.getItemId()));
            if ((clawCondition || bowCondition || crossbowCondition || mittenCondition || gunCondition) && item.getQuantity() >= bulletCount) {
                projectile = item.getItemId();
                break;
            }
        }

        final boolean soulArrow = player.getBuffedValue(MapleBuffStat.SOULARROW) != null;
        final boolean shadowClaw = player.getBuffedValue(MapleBuffStat.SHADOW_CLAW) != null;
        if (!soulArrow && !shadowClaw && projectile != 0) {
            int bulletConsume = bulletCount;
            if (effect != null && effect.getBulletConsume() != 0) {
                bulletConsume = effect.getBulletConsume() * (hasShadowPartner && attack.skill != 4121003 ? 2 : 1);
            }
            try {
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, bulletConsume, false, true);
            } catch (final InventoryException ie) {
                player.dropMessage(5, "You do not have enough " + mii.getName(projectile) + "s to use that skill.");
                return;
            }
        }

        if (projectile != 0 || soulArrow || attack.skill == 4221003) {
            int visProjectile = projectile; // Visible projectile sent to players
            if (mii.isThrowingStar(projectile)) {
                // See if player has cash stars
                final MapleInventory cash = player.getInventory(MapleInventoryType.CASH);
                // Cash stars have prefix 5021xxx
                visProjectile =
                    IntStream
                        .range(0, 255)
                        .mapToObj(i -> cash.getItem((byte) i))
                        .filter(Objects::nonNull)
                        .filter(item -> item.getItemId() / 1000 == 5021)
                        .findFirst()
                        .map(IItem::getItemId)
                        .orElse(projectile);
            } else { // Bow, Crossbow
                if (soulArrow || attack.skill == 3111004 || attack.skill == 3211004) {
                    visProjectile = 0; // Arrow rain/eruption show no arrows
                }
            }

            final MaplePacket packet;
            try {
                switch (attack.skill) {
                    case 3121004: // Hurricane
                    case 3221001: // Pierce
                    case 5221004: // Rapid Fire
                        packet =
                            MaplePacketCreator.rangedAttack(
                                player.getId(),
                                attack.skill,
                                attack.direction,
                                attack.numAttackedAndDamage,
                                visProjectile,
                                attack.allDamage,
                                attack.speed
                            );
                        break;
                    default:
                        packet =
                            MaplePacketCreator.rangedAttack(
                                player.getId(),
                                attack.skill,
                                attack.stance,
                                attack.numAttackedAndDamage,
                                visProjectile,
                                attack.allDamage,
                                attack.speed
                            );
                        break;
                }
                player.getMap().broadcastMessage(player, packet, false, true);
            } catch (final Exception e) {
                System.err.println("Failed to handle ranged attack");
                e.printStackTrace();
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
                    final double moneyMod = (double) money * 0.5d;
                    money = (int) (money + Math.random() * moneyMod);
                    if (money > player.getMeso()) {
                        money = player.getMeso();
                    }
                    player.gainMeso(-money, false);
                }
            }

            if (attack.skill > 0) {
                if (!AbstractDealDamageHandler.processSkill(attack, c)) return;
            }

            applyAttack(attack, player, bulletCount);
        }
    }
}
