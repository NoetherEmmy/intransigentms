package net.sf.odinms.net.mina;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.MapleCustomEncryption;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class MaplePacketEncoder implements ProtocolEncoder {
    //private static Logger log = LoggerFactory.getLogger(MaplePacketEncoder.class);

    @Override
    public void encode(final IoSession session, final Object message, final ProtocolEncoderOutput out) throws Exception {
        final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);

        if (client != null) {
            final byte[] input = ((MaplePacket) message).getBytes();
            final byte[] unencrypted = new byte[input.length];
            System.arraycopy(input, 0, unencrypted, 0, input.length);

            final byte[] ret = new byte[unencrypted.length + 4];

            final byte[] header = client.getSendCrypto().getPacketHeader(unencrypted.length);
            synchronized(client.getSendCrypto()){
                MapleCustomEncryption.encryptData(unencrypted);
                client.getSendCrypto().crypt(unencrypted);

                System.arraycopy(header, 0, ret, 0, 4);
                System.arraycopy(unencrypted, 0, ret, 4, unencrypted.length);

                final ByteBuffer out_buffer = ByteBuffer.wrap(ret);
                out.write(out_buffer);
            }
        } else { // no client object created yet, send unencrypted (hello)
                out.write(ByteBuffer.wrap(((MaplePacket) message).getBytes()));
        }
    }

    @Override
    public void dispose(final IoSession session) throws Exception {
    }
}
