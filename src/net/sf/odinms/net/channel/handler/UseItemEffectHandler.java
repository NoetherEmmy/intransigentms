package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class UseItemEffectHandler extends AbstractMaplePacketHandler {
    //private static final Logger log = LoggerFactory.getLogger(UseItemHandler.class);

    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final int itemId = slea.readInt();

        if (itemId >= 5000000 && itemId <= 5000053) {
            log.warn(slea.toString());
        }

        if (itemId != 0) {
            final IItem toUse = c.getPlayer().getInventory(MapleInventoryType.CASH).findById(itemId);
            if (toUse == null) {
                c.getPlayer()
                 .getCheatTracker()
                 .registerOffense(
                     CheatingOffense.USING_UNAVAILABLE_ITEM,
                     "" + itemId
                 );
                return;
            }
        }
        c.getPlayer().setItemEffect(itemId);
        c.getPlayer()
         .getMap()
         .broadcastMessage(
             c.getPlayer(),
             MaplePacketCreator.itemEffect(
                 c.getPlayer().getId(),
                 itemId
             ),
             false
         );
        if (c.getPlayer().hasFakeChar()) {
            for (final FakeCharacter ch : c.getPlayer().getFakeChars()) {
                ch.getFakeChar().setItemEffect(itemId);
                c.getPlayer()
                 .getMap()
                 .broadcastMessage(
                     ch.getFakeChar(),
                     MaplePacketCreator.itemEffect(
                         c.getPlayer().getId(),
                         itemId
                     ),
                     false
                 );
            }
        }
    }
}
