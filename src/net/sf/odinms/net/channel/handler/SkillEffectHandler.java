package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class SkillEffectHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final int skillId = slea.readInt();
        final int level = slea.readByte();
        final byte flags = slea.readByte();
        final int speed = slea.readByte();
        if (
            (
            skillId == 3121004 ||
            skillId == 5221004 ||
            skillId == 1121001 ||
            skillId == 1221001 ||
            skillId == 1321001 ||
            skillId == 2121001 ||
            skillId == 2221001 ||
            skillId == 2321001 ||
            skillId == 2111002 ||
            skillId == 4211001 ||
            skillId == 3221001 ||
            skillId == 5101004 ||
            skillId == 5201002
            ) &&
            level >= 1
        ) {
            c.getPlayer()
             .getMap()
             .broadcastMessage(
                 c.getPlayer(),
                 MaplePacketCreator.skillEffect(
                     c.getPlayer(),
                     skillId,
                     level,
                     flags,
                     speed
                 ),
                 false
             );
            for (final FakeCharacter ch : c.getPlayer().getFakeChars()) {
                ch.getFakeChar()
                  .getMap()
                  .broadcastMessage(
                      ch.getFakeChar(),
                      MaplePacketCreator.skillEffect(
                          ch.getFakeChar(),
                          skillId,
                          level,
                          flags,
                          speed
                      ),
                      false
                  );
            }
        } else {
            c.getSession().close();
        }
    }
}
