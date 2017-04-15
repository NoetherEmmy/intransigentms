package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;

public class PartychatHandler extends AbstractMaplePacketHandler {
    // private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PartychatHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final int type = slea.readByte(); // 0 for buddys, 1 for partys
        final int numRecipients = slea.readByte();
        final int[] recipients = new int[numRecipients];
        for (int i = 0; i < numRecipients; ++i) {
            recipients[i] = slea.readInt();
        }
        final String chattext = slea.readMapleAsciiString();
        if (!CommandProcessor.getInstance().processCommand(c, chattext)) {
            final MapleCharacter player = c.getPlayer();
            try {
                if (type == 0) {
                    c.getChannelServer().getWorldInterface().buddyChat(recipients, player.getId(), player.getName(), chattext);
                } else if (type == 1 && player.getParty() != null) {
                    c.getChannelServer().getWorldInterface().partyChat(player.getParty().getId(), chattext, player.getName());
                } else if (type == 2 && player.getGuildId() > 0) {
                    c.getChannelServer().getWorldInterface().guildChat(c.getPlayer().getGuildId(), c.getPlayer().getName(), c.getPlayer().getId(), chattext);
                } else if (type == 3 && player.getGuild() != null) {
                    final int allianceId = player.getGuild().getAllianceId();
                    if (allianceId > 0) {
                        c.getChannelServer().getWorldInterface().allianceMessage(allianceId, MaplePacketCreator.multiChat(player.getName(), chattext, 3), player.getId(), -1);
                    }
                }
            } catch (final RemoteException re) {
                c.getChannelServer().reconnectWorld();
            }
        }
    }
}
