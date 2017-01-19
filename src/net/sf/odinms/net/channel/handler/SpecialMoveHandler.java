package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.messages.ServernoticeMapleClientMessageCallback;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.concurrent.ScheduledFuture;

public class SpecialMoveHandler extends AbstractMaplePacketHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SpecialMoveHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readShort();
        slea.readShort();
        int skillId = slea.readInt();
        Point pos = null;
        int __skillLevel = slea.readByte();
        ISkill skill = SkillFactory.getSkill(skillId);
        int skillLevel = c.getPlayer().getSkillLevel(skill);
        MapleStatEffect effect;
        try {
            effect = skill.getEffect(skillLevel);
        } catch (IndexOutOfBoundsException ioobe) {
            System.err.println(
                "Player " +
                    c.getPlayer().getName() +
                    " tried to use level " +
                    skillLevel +
                    " of skill " +
                    skillId
            );
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        if (skill.isGMSkill() && !c.getPlayer().isGM()) {
            c.disconnect();
            c.getSession().close();
            return;
        }
        if (effect.getCooldown() > 0) {
            if (c.getPlayer().skillIsCooling(skillId)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                //c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.COOLDOWN_HACK);
                return;
            } else {
                c.getSession().write(MaplePacketCreator.skillCooldown(skillId, effect.getCooldown()));
                ScheduledFuture<?> timer =
                    TimerManager
                        .getInstance()
                        .schedule(
                            new CancelCooldownAction(
                                c.getPlayer(),
                                skillId
                            ),
                            effect.getCooldown() * 1000
                        );
                c.getPlayer()
                 .addCooldown(
                     skillId,
                     System.currentTimeMillis(),
                     effect.getCooldown() * 1000,
                     timer
                 );
            }
        }
        // Monster Magnet
        try {
            switch (skillId) {
                case 1121001:
                case 1221001:
                case 1321001:
                    int num = slea.readInt();
                    int mobId;
                    byte success;
                    for (int i = 0; i < num; ++i) {
                        mobId = slea.readInt();
                        success = slea.readByte();
                        c.getPlayer()
                         .getMap()
                         .broadcastMessage(
                             c.getPlayer(),
                             MaplePacketCreator.showMagnet(mobId, success),
                             false
                         );
                        MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(mobId);
                        if (monster != null) {
                            monster.switchController(c.getPlayer(), monster.isControllerHasAggro());
                        }
                    }
                    byte direction = slea.readByte();
                    c.getPlayer()
                     .getMap()
                     .broadcastMessage(
                         c.getPlayer(),
                         MaplePacketCreator.showBuffeffect(c.getPlayer().getId(), skillId, 1, direction),
                         false
                     );
                    for (FakeCharacter ch : c.getPlayer().getFakeChars()) {
                        ch.getFakeChar()
                          .getMap()
                          .broadcastMessage(
                              ch.getFakeChar(),
                              MaplePacketCreator.showBuffeffect(
                                  ch.getFakeChar().getId(),
                                  skillId,
                                  1,
                                  direction
                              ),
                              false
                          );
                    }
                    c.getSession().write(MaplePacketCreator.enableActions());
                    break;
            }
        } catch (Exception e) {
            log.warn("Failed to handle Monster Magnet.", e);
        }
        if (slea.available() == 5) {
            pos = new Point(slea.readShort(), slea.readShort());
        }
        try {
            if (skillLevel == 0 || skillLevel != __skillLevel) {
                log.warn(
                    c.getPlayer().getName() +
                        " is using a move skill they don't have. ID: " +
                        skill.getId()
                );
                c.disconnect();
            } else {
                if (c.getPlayer().isAlive()) {
                    if (skill.getId() != 2311002 || c.getPlayer().canDoor()) {
                        skill.getEffect(skillLevel).applyTo(c.getPlayer(), pos);
                    } else {
                        new ServernoticeMapleClientMessageCallback(5, c).dropMessage(
                            "Please wait 5 seconds before casting Mystic Door again."
                        );
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }
}
