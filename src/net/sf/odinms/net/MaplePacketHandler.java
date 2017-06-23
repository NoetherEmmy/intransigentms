package net.sf.odinms.net;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public interface MaplePacketHandler {
    void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c);

    /**
     * This method validates some general state constraints. For example, the
     * client has to be logged in for this packet. When the method returns <code>false</code>,
     * the client should be disconnected. Further validation based on the
     * content of the packet, and disconnecting the client if it's invalid in
     * handlePacket is recommended.
     *
     * @param c the client
     * @return <code>true</code> if the state of the client is valid to send this packet type
     */
    boolean validateState(MapleClient c);
}
