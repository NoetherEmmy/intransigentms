package net.sf.odinms.net.mina;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.tools.MapleAESOFB;
import net.sf.odinms.tools.MapleCustomEncryption;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class MaplePacketDecoder extends CumulativeProtocolDecoder {
    private static final String DECODER_STATE_KEY = MaplePacketDecoder.class.getName() + ".STATE";
    //private static final Logger log = LoggerFactory.getLogger(MaplePacketDecoder.class);

    private static class DecoderState {
        public int packetlength = -1;
    }

    @Override
    protected boolean doDecode(final IoSession session, final ByteBuffer in, final ProtocolDecoderOutput out) throws Exception {
        final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        DecoderState decoderState = (DecoderState) session.getAttribute(DECODER_STATE_KEY);

        if (decoderState == null) {
            decoderState = new DecoderState();
            session.setAttribute(DECODER_STATE_KEY, decoderState);
        }

        if (in.remaining() >= 4 && decoderState.packetlength == -1) {
            final int packetHeader = in.getInt();
            if (!client.getReceiveCrypto().checkPacket(packetHeader)) {
                System.err.println(MapleClient.getLogMessage(client, "Client failed packet check -> disconnecting"));
                session.close();
                return false;
            }
            decoderState.packetlength = MapleAESOFB.getPacketLength(packetHeader);
        } else if (in.remaining() < 4 && decoderState.packetlength == -1) {
            //System.err.println("decode... not enough data");
            return false;
        }

        if (in.remaining() >= decoderState.packetlength) {
            final byte[] decryptedPacket = new byte[decoderState.packetlength];
            in.get(decryptedPacket, 0, decoderState.packetlength);
            decoderState.packetlength = -1;

            client.getReceiveCrypto().crypt(decryptedPacket);
            MapleCustomEncryption.decryptData(decryptedPacket);
            out.write(decryptedPacket);

            return true;
        } else {
            //System.err.println("decode... not enough data to decode (need " + decoderState.packetlength + ")");
            return false;
        }
    }
}
