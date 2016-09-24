package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.PublicChatHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class GeneralchatHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        String text = slea.readMapleAsciiString();
        int show = slea.readByte();
        if (!CommandProcessor.getInstance().processCommand(c, text) && c.getPlayer().getCanTalk() && !PublicChatHandler.doChat(c, text)) {
            if (!c.getPlayer().isHidden()) {
                if (c.getPlayer().getGMText() == 0) {
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text, false, show));
                } else if (c.getPlayer().getGMText() == 7) {
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text, true, show));
                } else {
                    switch (c.getPlayer().getGMText()) {
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                            //MultiChat
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.multiChat(c.getPlayer().getName(), text, c.getPlayer().getGMText() - 1));
                            break;
                        case 5:
                        case 6:
                            //Server Notice
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(c.getPlayer().getGMText(), c.getPlayer().getName() + " : " + text));
                            break;
                        case 8:
                            //Whisper
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                            break;
                        case 9:
                            //MapleTip
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendYellowTip(c.getPlayer().getName() + " : " + text));
                            break;
                    }
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text, false, 1));
                }
            } else {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, c.getPlayer().getName() + " : " + text));
            }
        }
    }
}