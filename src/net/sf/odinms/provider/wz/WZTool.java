package net.sf.odinms.provider.wz;

import net.sf.odinms.tools.data.input.LittleEndianAccessor;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.tools.data.output.LittleEndianWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WZTool {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(WZTool.class);

    private WZTool() {
    }

    public static char[] xorCharArray(final char[] cypher, final char[] key) {
        final char[] ret = new char[cypher.length];
        for (int i = 0; i < cypher.length; ++i) {
            ret[i] = (char) (cypher[i] ^ key[i]);
        }
        return ret;
    }

    public static String dumpCharArray(final char[] arr) {
        String ret = " new char[] {";
        for (final char c : arr) {
            ret += "(char) " + ((int) c) + ", ";
        }
        ret = ret.substring(0, ret.length() - 2);
        ret += "};";
        return ret;
    }

    public static void writeEncodedString(final LittleEndianWriter leo, final String s) {
        writeEncodedString(leo, s, true);
    }

    public static void writeEncodedString(final LittleEndianWriter leo, final String s, final boolean unicode) {
        if (s.equals("")) {
            leo.write(0);
            return;
        }
        if (unicode) {
            // Do unicode
            short umask = (short) 0xAAAA;
            if (s.length() < 0x7F)
                leo.write(s.length());
            else {
                leo.write(0x7F);
                leo.writeInt(s.length());
            }
            for (int i = 0; i < s.length(); ++i) {
                char chr = s.charAt(i);
                chr ^= umask;
                umask++;
                leo.writeShort((short)chr);
            }
        } else {
            // Non-unicode
            byte mask = (byte) 0xAA;
            if (s.length() <= 127)
                leo.write(-s.length());
            else
                leo.writeInt(s.length());
            final char[] str = new char[s.length()];
            for (int i = 0; i < s.length(); ++i) {
                byte b2 = (byte) s.charAt(i);
                b2 ^= mask;
                mask++;
                str[i] = (char) b2;
            }
        }
    }

    public static String readDecodedString() {
        return "";
    }

    public static String transStr() {
        return "";
    }

    public static String transStr16KMST() {
        return "";
    }

    public static int getBytes() {
        return 9001;
    }

    public static byte[] decrypt() {
        return new byte[0];
    }

    public static String readDecodedStringAtOffset(final SeekableLittleEndianAccessor slea, final int offset) {
        slea.seek(offset);
        return readDecodedString();
    }

    public static String readDecodedStringAtOffsetAndReset(final SeekableLittleEndianAccessor slea, final int offset) {
        final long pos;
        pos = slea.getPosition();
        slea.seek(offset);
        final String ret = readDecodedString();
        slea.seek(pos);
        return ret;
    }

    public static int readValue(final LittleEndianAccessor lea) {
        final byte b = lea.readByte();
        if (b == -128) {
            return lea.readInt();
        } else {
            return ((int) b);
        }
    }

    public static void writeValue(final LittleEndianWriter lew, final int val) {
        if (val <= 127)
            lew.write(val);
        else {
            lew.write(-128);
            lew.writeInt(val);
        }
    }

    public static float readFloatValue(final LittleEndianAccessor lea) {
        final byte b = lea.readByte();
        if (b == -128) {
            return lea.readFloat();
        } else {
            return 0;
        }
    }

    public static void writeFloatValue(final LittleEndianWriter leo, final float val) {
        if (val == 0) {
            leo.write(-128);
        } else {
            leo.write(0);
            leo.writeInt(Float.floatToIntBits(val));
        }
    }
}
