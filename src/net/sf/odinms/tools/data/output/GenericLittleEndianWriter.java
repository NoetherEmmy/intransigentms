package net.sf.odinms.tools.data.output;

import java.nio.charset.Charset;

public class GenericLittleEndianWriter implements LittleEndianWriter {
    private static final Charset ASCII = Charset.forName("US-ASCII");
    private ByteOutputStream bos;

    /**
     * Class constructor - Protected to prevent instantiation with no arguments.
     */
    protected GenericLittleEndianWriter() {
    }

    /**
     * Sets the byte-output stream for this instance of the object.
     *
     * @param bos The new output stream to set.
     */
    protected void setByteOutputStream(final ByteOutputStream bos) {
        this.bos = bos;
    }

    /**
     * Class constructor - only this one can be used.
     *
     * @param bos The stream to wrap this objecr around.
     */
    public GenericLittleEndianWriter(final ByteOutputStream bos) {
        this.bos = bos;
    }

    /**
     * Write an array of bytes to the stream.
     *
     * @param b The bytes to write.
     */
    @Override
    public void write(final byte[] b) {
        for (int x = 0; x < b.length; ++x) {
            bos.writeByte(b[x]);
        }
    }

    /**
     * Write a byte to the stream.
     *
     * @param b The byte to write.
     */
    @Override
    public void write(final byte b) {
        bos.writeByte(b);
    }

    /**
     * Write a byte in integer form to the stream.
     *
     * @param b The byte as an <code>Integer</code> to write.
     */
    @Override
    public void write(final int b) {
        bos.writeByte((byte) b);
    }

    /**
     * Write a short integer to the stream.
     *
     * @param i The short integer to write.
     */
    @Override
    public void writeShort(final int i) {
        bos.writeByte((byte) (i & 0xFF));
        bos.writeByte((byte) ((i >>> 8) & 0xFF));
    }

    /**
     * Writes an integer to the stream.
     *
     * @param i The integer to write.
     */
    @Override
    public void writeInt(final int i) {
        bos.writeByte((byte) (i & 0xFF));
        bos.writeByte((byte) ((i >>> 8) & 0xFF));
        bos.writeByte((byte) ((i >>> 16) & 0xFF));
        bos.writeByte((byte) ((i >>> 24) & 0xFF));
    }

    /**
     * Writes an ASCII string the the stream.
     *
     * @param s The ASCII string to write.
     */
    @Override
    public void writeAsciiString(final String s) {
        write(s.getBytes(ASCII));
    }

    /**
     * Writes a maple-convention ASCII string to the stream.
     *
     * @param s The ASCII string to use maple-convention to write.
     */
    @Override
    public void writeMapleAsciiString(final String s) {
        writeShort((short) s.length());
        writeAsciiString(s);
    }

    /**
     * Writes a null-terminated ASCII string to the stream.
     *
     * @param s The ASCII string to write.
     */
    @Override
    public void writeNullTerminatedAsciiString(final String s) {
        writeAsciiString(s);
        write(0);
    }

    /**
     * Write a long integer to the stream.
     * @param l The long integer to write.
     */
    @Override
    public void writeLong(final long l) {
        bos.writeByte((byte) (l & 0xFF));
        bos.writeByte((byte) ((l >>> 8) & 0xFF));
        bos.writeByte((byte) ((l >>> 16) & 0xFF));
        bos.writeByte((byte) ((l >>> 24) & 0xFF));
        bos.writeByte((byte) ((l >>> 32) & 0xFF));
        bos.writeByte((byte) ((l >>> 40) & 0xFF));
        bos.writeByte((byte) ((l >>> 48) & 0xFF));
        bos.writeByte((byte) ((l >>> 56) & 0xFF));
    }
}
