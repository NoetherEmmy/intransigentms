package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
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
    //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SpecialMoveHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final MapleCharacter p = c.getPlayer();
        slea.readShort();
        slea.readShort();
        final int skillId = slea.readInt();
        Point pos = null;
        final int __skillLevel = slea.readByte();
        final ISkill skill = SkillFactory.getSkill(skillId);
        final int skillLevel = p.getSkillLevel(skill);
        final MapleStatEffect effect;
        try {
            effect = skill.getEffect(skillLevel);
        } catch (final IndexOutOfBoundsException ioobe) {
            System.err.println(
                "Player " +
                    p.getName() +
                    " tried to use level " +
                    skillLevel +
                    " of skill " +
                    skillId
            );
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        if (!p.canQuestEffectivelyUseSkill(skillId)) {
            p.dropMessage(
                5,
                "Your quest effective level (" +
                    p.getQuestEffectiveLevel() +
                    ") is too low to use " +
                    SkillFactory.getSkillName(skillId) +
                    "."
            );
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        if (skill.isGMSkill() && !p.isGM()) {
            c.disconnect();
            c.getSession().close();
            return;
        }
        if (effect.getCooldown() > 0) {
            if (p.skillIsCooling(skillId)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                //p.getCheatTracker().registerOffense(CheatingOffense.COOLDOWN_HACK);
                return;
            }
            c.getSession().write(MaplePacketCreator.skillCooldown(skillId, effect.getCooldown()));
            final ScheduledFuture<?> timer =
                TimerManager
                    .getInstance()
                    .schedule(
                        new CancelCooldownAction(
                            p,
                            skillId
                        ),
                        effect.getCooldown() * 1000
                    );
            p.addCooldown(
                 skillId,
                 System.currentTimeMillis(),
                 effect.getCooldown() * 1000,
                 timer
             );
        }
        // Monster Magnet
        try {
            switch (skillId) {
                case 1121001:
                case 1221001:
                case 1321001:
                    final int num = slea.readInt();
                    int mobId;
                    byte success;
                    for (int i = 0; i < num; ++i) {
                        mobId = slea.readInt();
                        success = slea.readByte();
                        p.getMap()
                         .broadcastMessage(
                             p,
                             MaplePacketCreator.showMagnet(mobId, success),
                             false
                         );
                        final MapleMonster monster = p.getMap().getMonsterByOid(mobId);
                        if (monster == null) continue;
                        monster.switchController(p, monster.isControllerHasAggro());
                    }
                    final byte direction = slea.readByte();
                    p.getMap()
                     .broadcastMessage(
                         p,
                         MaplePacketCreator.showBuffeffect(p.getId(), skillId, 1, direction),
                         false
                     );
                    for (final FakeCharacter ch : p.getFakeChars()) {
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
        } catch (final Exception e) {
            System.err.println("Failed to handle Monster Magnet.");
            e.printStackTrace();
        }
        if (slea.available() == 5) {
            pos = new Point(slea.readShort(), slea.readShort());
        }
        try {
            if (skillLevel == 0 || skillLevel != __skillLevel) {
                log.warn(
                    p.getName() +
                        " is using a move skill they don't have. ID: " +
                        skill.getId()
                );
                c.disconnect();
            } else if (p.isAlive()) {
                if (skill.getId() != 2311002 || p.canDoor()) {
                    skill.getEffect(skillLevel).applyTo(p, pos);
                } else {
                    new ServernoticeMapleClientMessageCallback(5, c).dropMessage(
                        "Please wait 5 seconds before casting Mystic Door again."
                    );
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } catch (final Exception e) {
            e.printStackTrace();
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }
}
