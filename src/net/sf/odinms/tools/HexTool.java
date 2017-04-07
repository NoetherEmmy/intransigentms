package net.sf.odinms.tools;

import net.sf.odinms.tools.data.input.LittleEndianAccessor;
import org.apache.mina.common.ByteBuffer;

import java.io.ByteArrayOutputStream;

public class HexTool {
    private static final char[] HEX =
    {
        '0', '1', '2', '3',
        '4', '5', '6', '7',
        '8', '9', 'A', 'B',
        'C', 'D', 'E', 'F'
    };

    /**
     * Static class dummy constructor.
     */
    private HexTool() {
    }

    /**
     * Turns a byte into a hexadecimal string.
     *
     * @param  byteValue The byte to convert.
     * @return The hexadecimal representation of {@code byteValue}
     */
    public static String toString(byte byteValue) {
        int tmp = byteValue << 8;
        char[] retstr = new char[] {
            HEX[(tmp >> 12) & 0x0F],
            HEX[(tmp >> 8) & 0x0F]
        };
        return String.valueOf(retstr);
    }

    /**
     * Turns a {@link org.apache.mina.common.ByteBuffer ByteBuffer}
     * into a hexadecimal string.
     *
     * @param  buf The <code>ByteBuffer</code> to convert.
     * @return The hexadecimal representation of <code>buf</code>.
     */
    public static String toString(ByteBuffer buf) {
        buf.flip();
        byte[] arr = new byte[buf.remaining()];
        buf.get(arr);
        String ret = toString(arr);
        buf.flip();
        buf.put(arr);
        return ret;
    }

    /**
     * Turns an integer into a hexadecimal string.
     *
     * @param  intValue The integer to transform.
     * @return The hexadecimal representation of <code>intValue</code>.
     */
    public static String toString(int intValue) {
        return Integer.toHexString(intValue);
    }

    /**
     * Turns an array of bytes into a hexadecimal string.
     *
     * @param  bytes The bytes to convert.
     * @return The hexadecimal representation of {@code bytes}.
     */
    public static String toString(byte[] bytes) {
        StringBuilder hexed = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            hexed.append(toString(bytes[i]));
            hexed.append(' ');
        }
        if (hexed.length() == 0) return "";
        return hexed.substring(0, hexed.length() - 1);
    }

    /**
     * Reads the remaining bytes of a
     * {@link net.sf.odinms.tools.data.input.LittleEndianAccessor LittleEndianAccessor}
     * into a hexadecimal string, exhausting it.
     *
     * @param  lea The {@code LittleEndianAccessor} to read from.
     * @return The hexadecimal representation of the byes read from {@code lea}.
     */
    public static String toString(LittleEndianAccessor lea) {
        final int restLen = (int) lea.available();
        byte[] bytes = new byte[restLen];
        for (int i = 0; i < restLen; ++i) {
            bytes[i] = lea.readByte();
        }
        return toString(bytes);
    }

    /**
     * Turns an array of bytes into a ASCII string. Any non-printable characters
     * are replaced by a period (<code>.</code>)
     *
     * @param  bytes The bytes to convert.
     * @return The ASCII hexadecimal representation of <code>bytes</code>
     */
    public static String toStringFromAscii(byte[] bytes) {
        char[] ret = new char[bytes.length];
        for (int x = 0; x < bytes.length; ++x) {
            if (bytes[x] < 32 && bytes[x] >= 0) {
                ret[x] = '.';
            } else {
                int chr = ((short) bytes[x]) & 0xFF;
                ret[x] = (char) chr;
            }
        }
        return String.valueOf(ret);
    }

    public static String toPaddedStringFromAscii(byte[] bytes) {
        String str = toStringFromAscii(bytes);
        StringBuilder ret = new StringBuilder(str.length() * 3);
        for (int i = 0; i < str.length(); ++i) {
            ret.append(str.charAt(i));
            ret.append("  ");
        }
        return ret.toString();
    }

    /**
     * Turns an hexadecimal string into a byte array.
     *
     * @param  hex The string to convert.
     * @return The byte array representation of {@code hex}
     */
    public static byte[] getByteArrayFromHexString(String hex) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int nexti = 0, nextb = 0;
        boolean highoc = true;
        outer: for (;;) {
            int number = -1;
            while (number == -1) {
                if (nexti == hex.length()) break outer;
                char chr = hex.charAt(nexti);
                if (chr >= '0' && chr <= '9') {
                    number = chr - '0';
                } else if (chr >= 'a' && chr <= 'f') {
                    number = chr - 'a' + 10;
                } else if (chr >= 'A' && chr <= 'F') {
                    number = chr - 'A' + 10;
                } else {
                    number = -1;
                }
                nexti++;
            }
            if (highoc) {
                nextb = number << 4;
                highoc = false;
            } else {
                nextb |= number;
                highoc = true;
                baos.write(nextb);
            }
        }
        return baos.toByteArray();
    }
}
