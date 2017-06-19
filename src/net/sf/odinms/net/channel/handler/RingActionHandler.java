package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.scripting.npc.Marriage;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.tools.HexTool;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class RingActionHandler extends AbstractMaplePacketHandler {
    //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RingActionHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final byte mode = slea.readByte();
        final MapleCharacter player = c.getPlayer();

        switch (mode) {
            case 0x00: // Send
                final String partnerName = slea.readMapleAsciiString();
                final MapleCharacter partner = c.getChannelServer().getPlayerStorage().getCharacterByName(partnerName);
                if (partnerName.equalsIgnoreCase(player.getName())) {
                    player.dropMessage(1, "You cannot put your own name in it.");
                } else if (partner == null) {
                    player.dropMessage(
                        1,
                        partnerName +
                            " was not found on this channel. " +
                            "If you are both logged in, please make sure you are in the same channel."
                    );
                } else if (!player.isMarried() && !partner.isMarried()) {
                    NPCScriptManager.getInstance().start(partner.getClient(), 9201002, "marriagequestion", player);
                }
                break;
            case 0x01: // Cancel send
                player.dropMessage(1, "You've cancelled the request.");
                break;
            case 0x03: // Drop ring
                Marriage.divorceEngagement(player);
                player.setMarriageQuestLevel(0);
                player.dropMessage(1, "Your engagement has been broken up.");
                break;
            default:
                System.err.println(
                    "Unhandled ring packet, mode = " +
                        Integer.toHexString(mode) +
                        ", remaining:"
                );
                System.err.println(HexTool.toString(slea));
                break;
        }
    }
}
