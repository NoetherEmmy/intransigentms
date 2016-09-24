package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.scripting.npc.Marriage;

public class RingActionHandler extends AbstractMaplePacketHandler {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RingActionHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        byte mode = slea.readByte();
        MapleCharacter player = c.getPlayer();
        switch (mode) {
            case 0x00: //Send
                String partnerName = slea.readMapleAsciiString();
                MapleCharacter partner = c.getChannelServer().getPlayerStorage().getCharacterByName(partnerName);
                if (partnerName.equalsIgnoreCase(player.getName())) {
                    player.dropMessage(1, "You cannot put your own name in it.");
                } else if (partner == null) {
                    player.dropMessage(1, partnerName + " was not found on this channel. If you are both logged in, please make sure you are in the same channel.");
                } else if (partner.getGender() == player.getGender()) {
                    player.dropMessage(1, "Your partner is the same gender as you.");
                } else if (!player.isMarried() && !partner.isMarried()) {
                    NPCScriptManager.getInstance().start(partner.getClient(), 9201002, "marriagequestion", player);
                }
                break;
            case 0x01: //Cancel send
                player.dropMessage(1, "You've cancelled the request.");
                break;
            case 0x03: //Drop ring
                Marriage.divorceEngagement(player);
                player.setMarriageQuestLevel(0);
                player.dropMessage(1, "Your engagement has been broken up.");
                break;
            default:
                log.info("Unhandled Ring Packet : " + slea.toString());
                break;
        }
    }
}