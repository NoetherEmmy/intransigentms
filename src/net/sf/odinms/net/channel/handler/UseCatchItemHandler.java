package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class UseCatchItemHandler extends AbstractMaplePacketHandler {
    public UseCatchItemHandler() {
    }

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        // 4A 00
        // B9 F4 8B 00 // unknown
        // 01 00 // success??
        // 32 A3 22 00 // itemid
        // 38 37 2B 00 // monsterid
        slea.readInt();
        slea.readShort();
        final int itemid = slea.readInt();
        final int monsterid = slea.readInt();
        final MapleMonster mob = c.getPlayer().getMap().getMonsterByOid(monsterid);
        if (mob != null) {
            if (mob.getHp() <= mob.getMaxHp() / 2) {
                if (itemid == 2270002) {
                    c.getPlayer()
                     .getMap()
                     .broadcastMessage(
                         MaplePacketCreator.catchMonster(
                             monsterid,
                             itemid,
                             (byte) 1
                         )
                     );
                }
                mob.getMap()
                   .killMonster(
                       mob,
                       (MapleCharacter) mob.getMap().getAllPlayers().get(0),
                       false,
                       false,
                       0
                   );
                c.getPlayer().setAPQScore(c.getPlayer().getAPQScore() + 1);
                c.getPlayer()
                 .getMap()
                 .broadcastMessage(
                     MaplePacketCreator.updateAriantPQRanking(
                         c.getPlayer().getName(),
                         c.getPlayer().getAPQScore(),
                         false
                     )
                 );
            } else {
                c.getSession()
                 .write(
                     MaplePacketCreator.serverNotice(
                         5,
                         "You cannot catch the monster as it is too strong."
                     )
                 );
            }
        }
    }
}
