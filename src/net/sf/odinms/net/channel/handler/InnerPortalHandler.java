package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

    public class InnerPortalHandler extends AbstractMaplePacketHandler {
    //private static org.slf4j.Logger log = LoggerFactory.getLogger(SpecialPortalHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        /*
        // TODO Need to code this check somehow =/
        [5D 00] // Opcode
        [02] // Map Portal
        [06 00 68 69 64 65 30 32] //the entered portal name
        [6B 0D] // to x
        [30 01] // to y
        [C5 00] // x
        [C4 01] // y

        slea.readByte();
        String portal = slea.readMapleAsciiString();
        int toX = slea.readShort();
        int toY = slea.readShort();
        int X = slea.readShort();
        int Y = slea.readShort();
        log.info("[Hacks] Player {} is trying to jump to a different map portal rather than the correct one");
        */
    }
}
