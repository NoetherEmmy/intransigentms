package net.sf.odinms.tools.data.output;

public interface LittleEndianWriter {
    /**
     * Write an array of bytes to the sequence.
     *
     * @param b The bytes to write.
     */
    void write(byte[] b);

    /**
     * Write a byte to the sequence.
     *
     * @param b The byte to write.
     */
    void write(byte b);

    /**
     * Write a byte in integer form to the sequence.
     *
     * @param b The byte as an <code>Integer</code> to write.
     */
    void write(int b);

    /**
     * Writes an integer to the sequence.
     *
     * @param i The integer to write.
     */
    void writeInt(int i);

    /**
     * Write a short integer to the sequence.
     *
     * @param s The short integer to write.
     */
    void writeShort(int s);

    /**
     * Write a long integer to the sequence.
     * @param l The long integer to write.
     */
    void writeLong(long l);

    /**
     * Writes an ASCII string the the sequence.
     *
     * @param s The ASCII string to write.
     */
    void writeAsciiString(String s);

    /**
     * Writes a null-terminated ASCII string to the sequence.
     *
     * @param s The ASCII string to write.
     */
    void writeNullTerminatedAsciiString(String s);

    /**
     * Writes a maple-convention ASCII string to the sequence.
     *
     * @param s The ASCII string to use maple-convention to write.
     */
    void writeMapleAsciiString(String s);
}
