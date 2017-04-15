package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class AutoAggroHandler extends AbstractMaplePacketHandler {
    //private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AutoAggroHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final MapleMap map = c.getPlayer().getMap();
        final MapleMonster monster = map.getMonsterByOid(oid);
        if (monster != null && monster.getController() != null) {
            if (!monster.isControllerHasAggro()) {
                if (map.getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(c.getPlayer(), true);
                } else {
                    monster.switchController(monster.getController(), true);
                }
            } else {
                if (map.getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(c.getPlayer(), true);
                }
            }
        } else if (monster != null && monster.getController() == null) {
            monster.switchController(c.getPlayer(), true);
        }
    }
}
