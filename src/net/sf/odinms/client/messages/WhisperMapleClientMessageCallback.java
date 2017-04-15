package net.sf.odinms.client.messages;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.tools.MaplePacketCreator;

public class WhisperMapleClientMessageCallback implements MessageCallback {
    private final MapleClient client;
    private final String whisperfrom;

    public WhisperMapleClientMessageCallback(final String whisperfrom, final MapleClient client) {
        this.whisperfrom = whisperfrom;
        this.client = client;
    }

    @Override
    public void dropMessage(final String message) {
        client.getSession().write(MaplePacketCreator.getWhisper(whisperfrom, client.getChannel(), message));
    }
}
