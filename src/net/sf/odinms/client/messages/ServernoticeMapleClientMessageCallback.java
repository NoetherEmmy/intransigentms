package net.sf.odinms.client.messages;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.tools.MaplePacketCreator;

public class ServernoticeMapleClientMessageCallback implements MessageCallback {
    private final MapleClient client;
    private final int mode;

    public ServernoticeMapleClientMessageCallback(final MapleClient client) {
        this(6, client);
    }

    public ServernoticeMapleClientMessageCallback(final int mode, final MapleClient client) {
        this.client = client;
        this.mode = mode;
    }

    @Override
    public void dropMessage(final String message) {
        client.getSession().write(MaplePacketCreator.serverNotice(mode, message));
    }
}
