package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class PetChatHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int petId = slea.readInt();
        slea.readInt();
        final int unknownShort = slea.readShort();
        final String text = slea.readMapleAsciiString();
        final MapleCharacter player = c.getPlayer();
        player
            .getMap()
            .broadcastMessage(
                player,
                MaplePacketCreator.petChat(
                    player.getId(),
                    unknownShort,
                    text,
                    c.getPlayer().getPetIndex(petId)
                ),
                true
            );
    }
}
