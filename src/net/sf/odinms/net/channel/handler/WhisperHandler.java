package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;

public class WhisperHandler extends AbstractMaplePacketHandler {
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final byte mode = slea.readByte();
        final String recipient = slea.readMapleAsciiString();
        final int channel;
        try {
            channel = c.getChannelServer().getWorldInterface().find(recipient);
        } catch (final RemoteException re) {
            c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
            c.getChannelServer().reconnectWorld();
            return;
        }
        if (channel == -1) {
            c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
        } else {
            if (mode == 6) { // Whisper
                final String text = slea.readMapleAsciiString();
                if (!CommandProcessor.getInstance().processCommand(c, text)) {
                    final ChannelServer pserv = ChannelServer.getInstance(channel);
                    final MapleCharacter victim = pserv.getPlayerStorage().getCharacterByName(recipient);
                    victim.getClient().getSession().write(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                    c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                }
            } else if (mode == 5) { // Find
                final ChannelServer pserv = ChannelServer.getInstance(channel);
                final MapleCharacter victim = pserv.getPlayerStorage().getCharacterByName(recipient);
                if (!victim.isGM() || (c.getPlayer().isGM() && victim.isGM())) {
                    if (victim.inCS()) {
                        c.getSession().write(MaplePacketCreator.getFindReplyWithCSorMTS(victim.getName(), false));
                    } else if (victim.inMTS()) {
                        c.getSession().write(MaplePacketCreator.getFindReplyWithCSorMTS(victim.getName(), true));
                    } else if (c.getChannel() == victim.getClient().getChannel()) {
                        c.getSession().write(MaplePacketCreator.getFindReplyWithMap(victim.getName(), victim.getMapId()));
                    } else {
                        c.getSession().write(MaplePacketCreator.getFindReply(victim.getName(), (byte) victim.getClient().getChannel()));
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                }
            }
        }
    }
}
