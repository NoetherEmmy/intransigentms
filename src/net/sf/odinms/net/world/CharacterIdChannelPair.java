package net.sf.odinms.net.world;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class CharacterIdChannelPair implements Externalizable {
    private int charid, channel;

    public CharacterIdChannelPair() {
    }

    public CharacterIdChannelPair(final int charid, final int channel) {
        super();
        this.charid = charid;
        this.channel = channel;
    }

    public int getCharacterId() {
        return charid;
    }

    public int getChannel() {
        return channel;
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        charid = in.readInt();
        channel = in.readByte();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(charid);
        out.writeByte(channel);
    }
}
