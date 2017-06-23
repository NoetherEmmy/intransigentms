package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleKeyBinding;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class KeymapChangeHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final MapleCharacter player = c.getPlayer();
        if (slea.available() >= 8) {
            slea.readInt();
            final int numChanges = slea.readInt();
            for (int i = 0; i < numChanges; ++i) {
                final int key = slea.readInt();
                final int type = slea.readByte();
                final int action = slea.readInt();
                final MapleKeyBinding newbinding = new MapleKeyBinding(type, action);
                player.changeKeybinding(key, newbinding);
            }
        }
    }
}
