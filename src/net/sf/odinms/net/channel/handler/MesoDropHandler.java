package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class MesoDropHandler extends AbstractMaplePacketHandler {
    public MesoDropHandler() {
    }

    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.readInt();
        final int meso = slea.readInt();
        if (!c.getPlayer().isAlive() || c.getPlayer().getCheatTracker().Spam(500, 2) || meso > 50000) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (meso <= c.getPlayer().getMeso() && meso >= 10) {
            c.getPlayer().gainMeso(-meso, false, true);
            c.getPlayer().getMap().spawnMesoDrop(
                meso,
                meso,
                c.getPlayer().getPosition(),
                c.getPlayer(),
                c.getPlayer(),
                false
            );
        } else {
            c.getPlayer().setMeso(0);
        }
    }
}
