package net.sf.odinms.net;

import net.sf.odinms.tools.HexTool;

public class ByteArrayMaplePacket implements MaplePacket {
    public static final long serialVersionUID = -7997681658570958848L;
    private final byte[] data;
    private Runnable onSend;

    public ByteArrayMaplePacket(final byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] getBytes() {
        return data;
    }

    @Override
    public String toString() {
        return HexTool.toString(data);
    }

    public Runnable getOnSend() {
        return onSend;
    }

    public void setOnSend(final Runnable onSend) {
        this.onSend = onSend;
    }
}
