package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class CharInfoRequestHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.readInt();
        int cid = slea.readInt();
        MapleCharacter chr = (MapleCharacter) c.getPlayer().getMap().getMapObject(cid);
        if (chr != null) {
            c.getSession().write(MaplePacketCreator.charInfo(chr));
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }
}
