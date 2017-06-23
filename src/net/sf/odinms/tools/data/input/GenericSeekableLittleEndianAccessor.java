package net.sf.odinms.tools.data.input;

import java.io.IOException;

public class GenericSeekableLittleEndianAccessor extends GenericLittleEndianAccessor implements SeekableLittleEndianAccessor {
    //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GenericSeekableLittleEndianAccessor.class);
    private final SeekableInputStreamBytestream bs;

    /**
     * Class constructor
     * Provide a seekable input stream to wrap this object around.
     *
     * @param bs The byte stream to wrap this around.
     */
    public GenericSeekableLittleEndianAccessor(final SeekableInputStreamBytestream bs) {
        super(bs);
        this.bs = bs;
    }

    /**
     * Seek the pointer to <code>offset</code>
     *
     * @param offset The offset to seek to.
     * @see net.sf.odinms.tools.data.input.SeekableInputStreamBytestream#seek
     */
    @Override
    public void seek(final long offset) {
        try {
            bs.seek(offset);
        } catch (final IOException e) {
            System.err.println("Seek failed");
            e.printStackTrace();
        }
    }

    /**
     * Get the current position of the pointer.
     *
     * @return The current position of the pointer as a long integer.
     * @see net.sf.odinms.tools.data.input.SeekableInputStreamBytestream#getPosition
     */
    @Override
    public long getPosition() {
        try {
            return bs.getPosition();
        } catch (final IOException e) {
            System.err.println("getPosition failed");
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Skip <code>num</code> number of bytes in the stream.
     *
     * @param num The number of bytes to skip.
     */
    @Override
    public void skip(final int num) {
        seek(getPosition() + num);
    }
}
