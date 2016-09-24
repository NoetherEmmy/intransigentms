package net.sf.odinms.net;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public interface MaplePacketHandler {

    void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c);

    /**
     * This method validates some general state constrains. For example that the
     * Client has to be logged in for this packet. When the method returns false
     * the Client should be disconnected. Further validation based on the
     * content of the packet and disconnecting the client if it's invalid in
     * handlePacket is recommended.
     *
     * @param c the client
     * @return true if the state of the client is valid to send this packettype
     */
    boolean validateState(MapleClient c);
}