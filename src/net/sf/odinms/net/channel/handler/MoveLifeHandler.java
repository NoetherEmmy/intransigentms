package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacterUtil;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MobSkill;
import net.sf.odinms.server.life.MobSkillFactory;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Random;

public class MoveLifeHandler extends AbstractMovementPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(MoveLifeHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final short moveId = slea.readShort();
        final MapleMap map = c.getPlayer().getMap();
        final MapleMapObject mmo = map.getMapObject(oid);
        if (mmo == null || mmo.getType() != MapleMapObjectType.MONSTER) {
            /*
            if (mmo != null) {
                log.warn(
                    "[dc] Player {} is trying to move something which is not a monster. It is a {}.",
                    new Object[] {
                        c.getPlayer().getName(),
                        map.getMapObject(oid).getClass().getCanonicalName()
                    }
                );
            }
            */
            return;
        }
        final MapleMonster monster = (MapleMonster) mmo;
        final List<LifeMovementFragment> res;
        final int skillByte = slea.readByte();
        final int skill = slea.readByte();
        final int skill_1 = slea.readByte() & 0xFF;
        final int skill_2 = slea.readByte();
        final int skill_3 = slea.readByte();
        @SuppressWarnings("unused") final int skill_4 = slea.readByte();
        MobSkill toUse = null;
        final Random rand = new Random();
        if (skillByte == 1 && monster.getNoSkills() > 0) {
            final int random = rand.nextInt(monster.getNoSkills());
            toUse = monster.getSkills().get(random);
            final int percHpLeft = monster.getHp() / monster.getMaxHp() * 100;
            if (toUse.getHP() < percHpLeft || !monster.canUseSkill(toUse)) toUse = null;
        }
        if (skill_1 >= 100 && skill_1 <= 200 && monster.hasSkill(skill_1, skill_2)) {
            final MobSkill skillData = MobSkillFactory.getMobSkill(skill_1, skill_2);
            if (skillData != null && monster.canUseSkill(skillData)) {
                skillData.applyEffect(c.getPlayer(), monster, true);
            }
        }
        slea.readByte();
        slea.readInt();
        final int start_x = slea.readShort();
        final int start_y = slea.readShort();
        final Point startPos = new Point(start_x, start_y);
        res = parseMovement(slea);
        if (!c.getPlayer().equals(monster.getController())) {
            if (monster.isAttackedBy(c.getPlayer())) { // Aggro and controller change.
                monster.switchController(c.getPlayer(), true);
            } else {
                return;
            }
        } else {
            if (
                skill == -1 &&
                monster.isControllerKnowsAboutAggro() &&
                !monster.isMobile() &&
                !monster.isFirstAttack()
            ) {
                monster.setControllerHasAggro(false);
                monster.setControllerKnowsAboutAggro(false);
            }
        }
        final boolean aggro = monster.isControllerHasAggro();

        if (toUse != null) {
            c.getSession().write(
                MaplePacketCreator.moveMonsterResponse(
                    oid,
                    moveId,
                    monster.getMp(),
                    aggro,
                    toUse.getSkillId(),
                    toUse.getSkillLevel()
                )
            );
        } else {
            c.getSession().write(
                MaplePacketCreator.moveMonsterResponse(
                    oid,
                    moveId,
                    monster.getMp(),
                    aggro
                )
            );
        }
        if (aggro) monster.setControllerKnowsAboutAggro(true);
        if (res == null) return;
        if (slea.available() != 9) {
            map.removePlayer(c.getPlayer());
            map.addPlayer(c.getPlayer());
            log.warn("slea.available != 9 (movement parsing error)");
            c.getPlayer().getCheatTracker().incrementVac();
            if (c.getPlayer().getCheatTracker().getVac() >= 5) {
                AutobanManager.getInstance().autoban(c, "Monster vac.");
                return;
            }
            try {
                c.getChannelServer()
                 .getWorldInterface()
                 .broadcastGMMessage(
                     null,
                     MaplePacketCreator.serverNotice(6,
                         "WARNING: It appears that the player with name " +
                         MapleCharacterUtil.makeMapleReadable(c.getPlayer().getName()) +
                         " on channel " +
                         c.getChannel() +
                         " is vac hacking."
                     ).getBytes()
                 );
            } catch (final RemoteException re) {
                re.printStackTrace();
                c.getChannelServer().reconnectWorld();
            }
            return;
        }
        final MaplePacket packet =
            MaplePacketCreator.moveMonster(
                skillByte,
                skill,
                skill_1,
                skill_2,
                skill_3,
                oid,
                startPos,
                res
            );
        map.broadcastMessage(c.getPlayer(), packet, monster.getPosition());
        updatePosition(res, monster, -1);
        map.moveMonster(monster, monster.getPosition());
        c.getPlayer().getCheatTracker().checkMoveMonster(monster.getPosition());
    }
}
