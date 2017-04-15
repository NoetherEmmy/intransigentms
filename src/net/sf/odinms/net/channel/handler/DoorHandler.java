package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.MapleDoor;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class DoorHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final int oid = slea.readInt();
        @SuppressWarnings("unused") final boolean mode = (slea.readByte() == 0); // 1 town to target, 0 target to town.
        for (final MapleMapObject obj : c.getPlayer().getMap().getMapObjects()) {
            if (obj instanceof MapleDoor) {
                final MapleDoor door = (MapleDoor) obj;
                if (door.getOwner().getId() == oid) {
                    door.warp(c.getPlayer(), mode);
                    return;
                }
            }
        }
    }
}
