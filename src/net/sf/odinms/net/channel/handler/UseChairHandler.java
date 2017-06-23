package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class UseChairHandler extends AbstractMaplePacketHandler {
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final MapleCharacter p = c.getPlayer();
        p.resetAfkTime();
        final int itemId = slea.readInt();
        final IItem toUse = p.getInventory(MapleInventoryType.SETUP).findById(itemId);

        if (toUse == null) {
            p.getCheatTracker().registerOffense(
                CheatingOffense.USING_UNAVAILABLE_ITEM,
                Integer.toString(itemId)
            );
            return;
        }
        p.setChair(itemId);
        p.getMap().broadcastMessage(p, MaplePacketCreator.showChair(p.getId(), itemId), false);
        c.getSession().write(MaplePacketCreator.enableActions());
        for (final FakeCharacter ch : p.getFakeChars()) {
            ch.getFakeChar().setChair(itemId);
            ch.getFakeChar()
              .getMap()
              .broadcastMessage(
                  ch.getFakeChar(),
                  MaplePacketCreator.showChair(
                      ch.getFakeChar().getId(),
                      itemId
                  ),
                  false
              );
        }
    }
}
