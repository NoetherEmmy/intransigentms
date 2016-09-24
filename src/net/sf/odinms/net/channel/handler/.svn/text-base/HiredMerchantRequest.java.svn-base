package net.sf.odinms.net.channel.handler;

import java.util.Arrays;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class HiredMerchantRequest extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.isGuest()) {
            if (c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 23000, Arrays.asList(MapleMapObjectType.HIRED_MERCHANT, MapleMapObjectType.SHOP)).size() == 0) {
                if (!c.getPlayer().hasMerchant()) {
                    c.getSession().write(MaplePacketCreator.hiredMerchantBox());
                } else {
                    c.getPlayer().dropMessage(1, "You already have a store open, please go close that store first.");
                }
            } else {
                c.getPlayer().dropMessage(1, "You may not establish a store here.");
            }
        } else {
            c.getPlayer().dropMessage(1, "Guest users are not allowed to open hired merchants.");
        }
    }
}