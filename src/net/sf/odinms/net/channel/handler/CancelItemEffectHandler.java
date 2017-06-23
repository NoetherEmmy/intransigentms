package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class CancelItemEffectHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final int sourceid = slea.readInt();
        final MapleStatEffect effect = MapleItemInformationProvider.getInstance().getItemEffect(-sourceid);
        c.getPlayer().cancelEffect(effect, false, -1);
        if (c.getPlayer().hasFakeChar()) {
            for (final FakeCharacter ch : c.getPlayer().getFakeChars()) {
                ch.getFakeChar().cancelEffect(effect, false, -1);
            }
        }
    }
}
