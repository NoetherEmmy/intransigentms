package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public final class SpouseChatHandler extends AbstractMaplePacketHandler {
    @Override
    public final void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final String recipient = slea.readMapleAsciiString();
        final String msg = slea.readMapleAsciiString();
        c.getPlayer().dropMessage("Test: " + recipient);
        if (c.getPlayer().isMarried()) {
            final MapleCharacter spouse = c.getPlayer().getPartner();
            if (spouse != null) {
                spouse
                    .getClient()
                    .getSession()
                    .write(
                        MaplePacketCreator.sendSpouseChat(
                            c.getPlayer().getName(),
                            msg
                        )
                    );
                c.getSession()
                 .write(
                     MaplePacketCreator.sendSpouseChat(
                         c.getPlayer().getName(),
                         msg
                     )
                 );
            } else {
                c.getPlayer()
                 .dropMessage(
                     5,
                     "You are either not married or your spouse is currently offline."
                 );
            }
        } else {
            c.getPlayer().dropMessage(5, "You are not married.");
        }
    }
}
