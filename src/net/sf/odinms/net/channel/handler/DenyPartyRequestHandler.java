package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class DenyPartyRequestHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.readByte();
        final String from = slea.readMapleAsciiString();
        @SuppressWarnings("unused") final String to = slea.readMapleAsciiString();
        final MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(from);
        if (cfrom != null) {
            cfrom
                .getClient()
                .getSession()
                .write(
                    MaplePacketCreator.partyStatusMessage(
                        23,
                        c.getPlayer().getName()
                    )
                );
        }
    }
}
