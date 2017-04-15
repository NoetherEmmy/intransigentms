package net.sf.odinms.tools;

public class BitTools {
    /**
     * Static class dummy constructor.
     */
    private BitTools() {
    }

    /**
     * Reads a short from <code>array</code> at <code>index</code>
     *
     * @param array The byte array to read the short integer from.
     * @param index Where reading begins.
     * @return The short integer value.
     */
    public static int getShort(final byte[] array, final int index) {
        int ret = array[index];
        ret &= 0xFF;
        ret |= ((int) (array[index + 1]) << 8) & 0xFF00;
        return ret;
    }

    /**
     * Reads a string from <code>array</code> at
     * <code>index</code> <code>length</code> in length.
     *
     * @param array The array to read the string from.
     * @param index Where reading begins.
     * @param length The number of bytes to read.
     * @return The string read.
     */
    public static String getString(final byte[] array, final int index, final int length) {
        final char[] cret = new char[length];
        for (int x = 0; x < length; ++x) {
            cret[x] = (char) array[x + index];
        }
        return String.valueOf(cret);
    }

    /**
     * Reads a maplestory-convention string from <code>array</code> at
     * <code>index</code>
     *
     * @param array The byte array to read from.
     * @param index Where reading begins.
     * @return The string read.
     */
    public static String getMapleString(final byte[] array, final int index) {
        final int length = ((int) (array[index]) & 0xFF) | (array[index + 1] << 8 & 0xFF00);
        return BitTools.getString(array, index + 2, length);
    }

    /**
     * Rotates the bits of <code>in</code> <code>count</code> places to the
     * left.
     *
     * @param in The byte to rotate the bits
     * @param count Number of times to rotate.
     * @return The rotated byte.
     */
    public static byte rollLeft(final byte in, final int count) {
        /*
         * in: 11001101 count: 3 out: 0110 1110
         */
        int tmp = (int) in & 0xFF;
        tmp = tmp << (count % 8);
        return (byte) ((tmp & 0xFF) | (tmp >> 8));
    }

    /**
     * Rotates the bits of <code>in</code> <code>count</code> places to the
     * right.
     *
     * @param in The byte to rotate the bits
     * @param count Number of times to rotate.
     * @return The rotated byte.
     */
    public static byte rollRight(final byte in, final int count) {
        /*
         * in: 11001101 count: 3 out: 1011 10011
         *
         * 0000 1011 1011 0000 0101 1000
         *
         */
        int tmp = (int) in & 0xFF;
        tmp = (tmp << 8) >>> (count % 8);

        return (byte) ((tmp & 0xFF) | (tmp >>> 8));
    }

    /**
     * Repeats <code>count</code> bytes of <code>in</code> <code>mul</code> times.
     *
     * @param in The array of bytes containing the bytes to multiply.
     * @param count The number of bytes to repeat.
     * @param mul The number of times to repeat.
     * @return The repeated bytes.
     */
    public static byte[] multiplyBytes(final byte[] in, final int count, final int mul) {
        final byte[] ret = new byte[count * mul];
        for (int x = 0; x < count * mul; ++x) {
            ret[x] = in[x % count];
        }
        return ret;
    }

    /**
     * Turns a double-precision floating point integer into an integer.
     *
     * @param d The double to transform.
     * @return The converted integer.
     */
    public static int doubleToShortBits(final double d) {
        final long l = Double.doubleToLongBits(d);
        return (int) (l >> 48);
    }
}
