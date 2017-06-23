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
import java.util.List;
import java.util.stream.Collectors;

public class EnergyOrbDamageHandler extends AbstractDealDamageHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        player.resetAfkTime();

        if (
            player.getEnergyBar() < 10000 ||
            player.getSkillLevel(5110001) <= 0
        ) {
            return;
        }

        final AttackInfo attack = parseDamage(slea, false);

        final boolean questEffectiveBlock = !player.canQuestEffectivelyUseSkill(5110001);
        if (questEffectiveBlock || player.getMap().isDamageMuted()) {
            AbstractDealDamageHandler.cancelDamage(attack, c, questEffectiveBlock);
            return;
        }

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

        applyAttack(attack, player, 1);

        for (int i = 0; i < attack.allDamage.size(); ++i) {
            final Pair<Integer, List<Integer>> dmg = attack.allDamage.get(i);

            if (player.getParty() != null && player.isBareHanded() && player.getTotalInt() >= 350) {
                // Monk healing
                final Rectangle bounds = calculateBoundingBox(player.getPosition(), player.isFacingLeft());
                final List<MapleMapObject> affecteds =
                    player
                        .getMap()
                        .getMapObjectsInRect(
                            bounds,
                            MapleMapObjectType.PLAYER
                        );
                final List<MapleCharacter> affectedp =
                    affecteds
                        .stream()
                        .map(affectedmo -> (MapleCharacter) affectedmo)
                        .filter(affected ->
                            affected != null &&
                            !affected.equals(player) &&
                            affected.isAlive() &&
                            player.getParty().equals(affected.getParty())
                        )
                        .collect(Collectors.toCollection(ArrayList::new));

                if (!affectedp.isEmpty()) {
                    final int healing = (int) ((double) player.getTotalMagic() * ((double) player.getTotalInt() / 300.0d) * (0.5d + 0.5d / affectedp.size()) * (2.0d - Math.random()));
                    affectedp.forEach(affected -> {
                        affected.setHp(affected.getHp() + healing);
                        affected.updateSingleStat(MapleStat.HP, affected.getHp());
                        affected
                            .getClient()
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
                    });
                }
            }

            if (dmg == null) continue;
            final MapleMonster monster = player.getMap().getMonsterByOid(dmg.getLeft());
            if (monster == null) continue;
            final double multiplier = monster.getVulnerability();
            if (multiplier != 1.0d) {
                final List<Integer> additionalDmg = new ArrayList<>(1);
                final List<Integer> newDmg = new ArrayList<>(1);
                dmg.getRight().forEach(dmgNumber -> {
                    additionalDmg.add((int) (dmgNumber * (multiplier - 1.0d)));
                    newDmg.add((int) (dmgNumber * multiplier));
                });
                attack.allDamage.set(i, new Pair<>(dmg.getLeft(), newDmg));
                additionalDmg.forEach(additionald ->
                    player
                        .getMap()
                        .broadcastMessage(
                            player,
                            MaplePacketCreator.damageMonster(
                                dmg.getLeft(),
                                additionald
                            ),
                            true
                        )
                );
            }
        }
    }

    private Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft) {
        final Point mylt;
        final Point myrb;
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
