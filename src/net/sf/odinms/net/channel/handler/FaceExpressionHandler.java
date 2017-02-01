package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class FaceExpressionHandler extends AbstractMaplePacketHandler {
    //private static Logger log = LoggerFactory.getLogger(FaceExpressionHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter p = c.getPlayer();
        p.resetAfkTime();
        int emote = slea.readInt();
        if (emote > 7) {
            int emoteid = 5159992 + emote;
            MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(emoteid);
            MapleInventory iv = p.getInventory(type);
            if (iv.findById(emoteid) == null) {
                //log.info("[h4x] Player {} is using a face expression he does not have: {}", p.getName(), Integer.valueOf(emoteid));
                p.getCheatTracker()
                 .registerOffense(
                     CheatingOffense.USING_UNAVAILABLE_ITEM,
                     "" + emoteid
                 );
                return;
            }
        }
        for (FakeCharacter ch : p.getFakeChars()) {
            p.getMap()
             .broadcastMessage(
                 ch.getFakeChar(),
                 MaplePacketCreator.facialExpression(
                     ch.getFakeChar(),
                     emote
                 ),
                 false
             );
        }
        p.getMap().broadcastMessage(p, MaplePacketCreator.facialExpression(p, emote), false);
    }
}
