package net.sf.odinms.tools;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MapleAESOFB {
    private byte[] iv;
    private Cipher cipher;
    private final short mapleVersion;
    private static final byte[] funnyBytes = new byte[] {
        (byte)0xEC, (byte)0x3F, (byte)0x77, (byte)0xA4, (byte)0x45, (byte)0xD0, (byte)0x71, (byte)0xBF,
        (byte)0xB7, (byte)0x98, (byte)0x20, (byte)0xFC, (byte)0x4B, (byte)0xE9, (byte)0xB3, (byte)0xE1,
        (byte)0x5C, (byte)0x22, (byte)0xF7, (byte)0x0C, (byte)0x44, (byte)0x1B, (byte)0x81, (byte)0xBD,
        (byte)0x63, (byte)0x8D, (byte)0xD4, (byte)0xC3, (byte)0xF2, (byte)0x10, (byte)0x19, (byte)0xE0,
        (byte)0xFB, (byte)0xA1, (byte)0x6E, (byte)0x66, (byte)0xEA, (byte)0xAE, (byte)0xD6, (byte)0xCE,
        (byte)0x06, (byte)0x18, (byte)0x4E, (byte)0xEB, (byte)0x78, (byte)0x95, (byte)0xDB, (byte)0xBA,
        (byte)0xB6, (byte)0x42, (byte)0x7A, (byte)0x2A, (byte)0x83, (byte)0x0B, (byte)0x54, (byte)0x67,
        (byte)0x6D, (byte)0xE8, (byte)0x65, (byte)0xE7, (byte)0x2F, (byte)0x07, (byte)0xF3, (byte)0xAA,
        (byte)0x27, (byte)0x7B, (byte)0x85, (byte)0xB0, (byte)0x26, (byte)0xFD, (byte)0x8B, (byte)0xA9,
        (byte)0xFA, (byte)0xBE, (byte)0xA8, (byte)0xD7, (byte)0xCB, (byte)0xCC, (byte)0x92, (byte)0xDA,
        (byte)0xF9, (byte)0x93, (byte)0x60, (byte)0x2D, (byte)0xDD, (byte)0xD2, (byte)0xA2, (byte)0x9B,
        (byte)0x39, (byte)0x5F, (byte)0x82, (byte)0x21, (byte)0x4C, (byte)0x69, (byte)0xF8, (byte)0x31,
        (byte)0x87, (byte)0xEE, (byte)0x8E, (byte)0xAD, (byte)0x8C, (byte)0x6A, (byte)0xBC, (byte)0xB5,
        (byte)0x6B, (byte)0x59, (byte)0x13, (byte)0xF1, (byte)0x04, (byte)0x00, (byte)0xF6, (byte)0x5A,
        (byte)0x35, (byte)0x79, (byte)0x48, (byte)0x8F, (byte)0x15, (byte)0xCD, (byte)0x97, (byte)0x57,
        (byte)0x12, (byte)0x3E, (byte)0x37, (byte)0xFF, (byte)0x9D, (byte)0x4F, (byte)0x51, (byte)0xF5,
        (byte)0xA3, (byte)0x70, (byte)0xBB, (byte)0x14, (byte)0x75, (byte)0xC2, (byte)0xB8, (byte)0x72,
        (byte)0xC0, (byte)0xED, (byte)0x7D, (byte)0x68, (byte)0xC9, (byte)0x2E, (byte)0x0D, (byte)0x62,
        (byte)0x46, (byte)0x17, (byte)0x11, (byte)0x4D, (byte)0x6C, (byte)0xC4, (byte)0x7E, (byte)0x53,
        (byte)0xC1, (byte)0x25, (byte)0xC7, (byte)0x9A, (byte)0x1C, (byte)0x88, (byte)0x58, (byte)0x2C,
        (byte)0x89, (byte)0xDC, (byte)0x02, (byte)0x64, (byte)0x40, (byte)0x01, (byte)0x5D, (byte)0x38,
        (byte)0xA5, (byte)0xE2, (byte)0xAF, (byte)0x55, (byte)0xD5, (byte)0xEF, (byte)0x1A, (byte)0x7C,
        (byte)0xA7, (byte)0x5B, (byte)0xA6, (byte)0x6F, (byte)0x86, (byte)0x9F, (byte)0x73, (byte)0xE6,
        (byte)0x0A, (byte)0xDE, (byte)0x2B, (byte)0x99, (byte)0x4A, (byte)0x47, (byte)0x9C, (byte)0xDF,
        (byte)0x09, (byte)0x76, (byte)0x9E, (byte)0x30, (byte)0x0E, (byte)0xE4, (byte)0xB2, (byte)0x94,
        (byte)0xA0, (byte)0x3B, (byte)0x34, (byte)0x1D, (byte)0x28, (byte)0x0F, (byte)0x36, (byte)0xE3,
        (byte)0x23, (byte)0xB4, (byte)0x03, (byte)0xD8, (byte)0x90, (byte)0xC8, (byte)0x3C, (byte)0xFE,
        (byte)0x5E, (byte)0x32, (byte)0x24, (byte)0x50, (byte)0x1F, (byte)0x3A, (byte)0x43, (byte)0x8A,
        (byte)0x96, (byte)0x41, (byte)0x74, (byte)0xAC, (byte)0x52, (byte)0x33, (byte)0xF0, (byte)0xD9,
        (byte)0x29, (byte)0x80, (byte)0xB1, (byte)0x16, (byte)0xD3, (byte)0xAB, (byte)0x91, (byte)0xB9,
        (byte)0x84, (byte)0x7F, (byte)0x61, (byte)0x1E, (byte)0xCF, (byte)0xC5, (byte)0xD1, (byte)0x56,
        (byte)0x3D, (byte)0xCA, (byte)0xF4, (byte)0x05, (byte)0xC6, (byte)0xE5, (byte)0x08, (byte)0x49
    };

    /* ORIGINAL
        private static final byte[] funnyBytes2 = new byte[] { (byte) 0xEC, 0x3F, 0x77, (byte) 0xA4, 0x45, (byte) 0xD0,
        0x71, (byte) 0xBF, (byte) 0xB7, (byte) 0x98, 0x20, (byte) 0xFC, 0x4B, (byte) 0xE9, (byte) 0xB3, (byte) 0xE1,
        0x5C, 0x22, (byte) 0xF7, 0x0C, 0x44, 0x1B, (byte) 0x81, (byte) 0xBD, 0x63, (byte) 0x8D, (byte) 0xD4,
        (byte) 0xC3, (byte) 0xF2, 0x10, 0x19, (byte) 0xE0, (byte) 0xFB, (byte) 0xA1, 0x6E, 0x66, (byte) 0xEA,
        (byte) 0xAE, (byte) 0xD6, (byte) 0xCE, 0x06, 0x18, 0x4E, (byte) 0xEB, 0x78, (byte) 0x95, (byte) 0xDB,
        (byte) 0xBA, (byte) 0xB6, 0x42, 0x7A, 0x2A, (byte) 0x83, 0x0B, 0x54, 0x67, 0x6D, (byte) 0xE8, 0x65,
        (byte) 0xE7, 0x2F, 0x07, (byte) 0xF3, (byte) 0xAA, 0x27, 0x7B, (byte) 0x85, (byte) 0xB0, 0x26, (byte) 0xFD,
        (byte) 0x8B, (byte) 0xA9, (byte) 0xFA, (byte) 0xBE, (byte) 0xA8, (byte) 0xD7, (byte) 0xCB, (byte) 0xCC,
        (byte) 0x92, (byte) 0xDA, (byte) 0xF9, (byte) 0x93, 0x60, 0x2D, (byte) 0xDD, (byte) 0xD2, (byte) 0xA2,
        (byte) 0x9B, 0x39, 0x5F, (byte) 0x82, 0x21, 0x4C, 0x69, (byte) 0xF8, 0x31, (byte) 0x87, (byte) 0xEE,
        (byte) 0x8E, (byte) 0xAD, (byte) 0x8C, 0x6A, (byte) 0xBC, (byte) 0xB5, 0x6B, 0x59, 0x13, (byte) 0xF1, 0x04,
        0x00, (byte) 0xF6, 0x5A, 0x35, 0x79, 0x48, (byte) 0x8F, 0x15, (byte) 0xCD, (byte) 0x97, 0x57, 0x12, 0x3E, 0x37,
        (byte) 0xFF, (byte) 0x9D, 0x4F, 0x51, (byte) 0xF5, (byte) 0xA3, 0x70, (byte) 0xBB, 0x14, 0x75, (byte) 0xC2,
        (byte) 0xB8, 0x72, (byte) 0xC0, (byte) 0xED, 0x7D, 0x68, (byte) 0xC9, 0x2E, 0x0D, 0x62, 0x46, 0x17, 0x11, 0x4D,
        0x6C, (byte) 0xC4, 0x7E, 0x53, (byte) 0xC1, 0x25, (byte) 0xC7, (byte) 0x9A, 0x1C, (byte) 0x88, 0x58, 0x2C,
        (byte) 0x89, (byte) 0xDC, 0x02, 0x64, 0x40, 0x01, 0x5D, 0x38, (byte) 0xA5, (byte) 0xE2, (byte) 0xAF, 0x55,
        (byte) 0xD5, (byte) 0xEF, 0x1A, 0x7C, (byte) 0xA7, 0x5B, (byte) 0xA6, 0x6F, (byte) 0x86, (byte) 0x9F, 0x73,
        (byte) 0xE6, 0x0A, (byte) 0xDE, 0x2B, (byte) 0x99, 0x4A, 0x47, (byte) 0x9C, (byte) 0xDF, 0x09, 0x76,
        (byte) 0x9E, 0x30, 0x0E, (byte) 0xE4, (byte) 0xB2, (byte) 0x94, (byte) 0xA0, 0x3B, 0x34, 0x1D, 0x28, 0x0F,
        0x36, (byte) 0xE3, 0x23, (byte) 0xB4, 0x03, (byte) 0xD8, (byte) 0x90, (byte) 0xC8, 0x3C, (byte) 0xFE, 0x5E,
        0x32, 0x24, 0x50, 0x1F, 0x3A, 0x43, (byte) 0x8A, (byte) 0x96, 0x41, 0x74, (byte) 0xAC, 0x52, 0x33, (byte) 0xF0,
        (byte) 0xD9, 0x29, (byte) 0x80, (byte) 0xB1, 0x16, (byte) 0xD3, (byte) 0xAB, (byte) 0x91, (byte) 0xB9,
        (byte) 0x84, 0x7F, 0x61, 0x1E, (byte) 0xCF, (byte) 0xC5, (byte) 0xD1, 0x56, 0x3D, (byte) 0xCA, (byte) 0xF4,
        0x05, (byte) 0xC6, (byte) 0xE5, 0x08, 0x49, 0x4F, 0x64, 0x69, 0x6E, 0x4D, 0x53, 0x7E, 0x46, 0x72, 0x7A };*/

    /**
     * Class constructor - Creates an instance of the MapleStory encryption
     * cipher.
     *
     * @param key The 256 bit AES key to use.
     * @param iv The 4-byte IV to use.
     */
    public MapleAESOFB(final byte[] key, final byte[] iv, final short mapleVersion) {
        final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        //final Logger log = LoggerFactory.getLogger(MapleAESOFB.class);
        try {
            cipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.err.println("ERROR");
            e.printStackTrace();
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        } catch (final InvalidKeyException ike) {
            log.error(
                "Error initalizing the encryption cipher. " +
                    "Make sure you're using the Unlimited Strength cryptography jar files."
            );
        }
        setIv(iv);
        this.mapleVersion = (short) (((mapleVersion >> 8) & 0xFF) | ((mapleVersion << 8) & 0xFF00));
    }

    /**
     * Sets the IV of this instance.
     *
     * @param iv The new IV.
     */
    private void setIv(final byte[] iv) {
        this.iv = iv;
    }

    /**
     * For debugging/testing purposes only.
     *
     * @return The IV.
     */
    public byte[] getIv() {
        return iv;
    }

    /**
     * Encrypts <code>data</code> and generates a new IV.
     *
     * @param data The bytes to encrypt.
     * @return The encrypted bytes.
     */
    public byte[] crypt(final byte[] data) {
        int remaining = data.length;
        int llength = 0x5B0;
        int start = 0;
        while (remaining > 0) {
            final byte[] myIv = BitTools.multiplyBytes(iv, 4, 4);
            if (remaining < llength) {
                llength = remaining;
            }
            for (int x = start; x < start + llength; ++x) {
                if ((x - start) % myIv.length == 0) {
                    try {
                        final byte[] newIv = cipher.doFinal(myIv);
                        System.arraycopy(newIv, 0, myIv, 0, myIv.length);
                        //System.out.println("Iv is now " + HexTool.toString(this.iv));
                    } catch (IllegalBlockSizeException | BadPaddingException e) {
                        e.printStackTrace();
                    }
                }
                data[x] ^= myIv[(x - start) % myIv.length];
            }
            start += llength;
            remaining -= llength;
            llength = 0x5B4;
        }
        updateIv();
        return data;
    }

    /**
     * Generates a new IV.
     */
    private void updateIv() {
        iv = getNewIv(iv);
    }

    /**
     * Generates a packet header for a packet that is <code>length</code>
     * long.
     *
     * @param length How long the packet that this header is for is.
     * @return The header.
     */
    public byte[] getPacketHeader(final int length) {
        int iiv = (iv[3]) & 0xFF;
        iiv |= (iv[2] << 8) & 0xFF00;

        iiv ^= mapleVersion;
        final int mlength = ((length << 8) & 0xFF00) | (length >>> 8);
        final int xoredIv = iiv ^ mlength;

        final byte[] ret = new byte[4];
        ret[0] = (byte) ((iiv >>> 8) & 0xFF);
        ret[1] = (byte) (iiv & 0xFF);
        ret[2] = (byte) ((xoredIv >>> 8) & 0xFF);
        ret[3] = (byte) (xoredIv & 0xFF);
        return ret;
    }

    /**
     * Gets the packet length from a header.
     *
     * @param packetHeader The header as an integer.
     * @return The length of the packet.
     */
    public static int getPacketLength(final int packetHeader) {
        int packetLength = ((packetHeader >>> 16) ^ (packetHeader & 0xFFFF));
        packetLength = ((packetLength << 8) & 0xFF00) | ((packetLength >>> 8) & 0xFF); // Fix
        // Endianness
        return packetLength;
    }

    /**
     * Check the packet to make sure it has a header.
     *
     * @param packet The packet to check.
     * @return <code>True</code> if the packet has a correct header,
     *         <code>false</code> otherwise.
     */
    public boolean checkPacket(final byte[] packet) {
        return ((((packet[0] ^ iv[2]) & 0xFF) == ((mapleVersion >> 8) & 0xFF)) && (((packet[1] ^ iv[3]) & 0xFF) == (mapleVersion & 0xFF)));
    }

    /**
     * Check the header for validity.
     *
     * @param packetHeader The packet header to check.
     * @return <code>True</code> if the header is correct, <code>false</code>
     *         otherwise.
     */
    public boolean checkPacket(final int packetHeader) {
        final byte[] packetHeaderBuf = new byte[2];
        packetHeaderBuf[0] = (byte) ((packetHeader >> 24) & 0xFF);
        packetHeaderBuf[1] = (byte) ((packetHeader >> 16) & 0xFF);
        return checkPacket(packetHeaderBuf);
    }

    /**
     * Gets a new IV from <code>oldIv</code>
     *
     * @param oldIv The old IV to get a new IV from.
     * @return The new IV.
     */
    public static byte[] getNewIv(final byte[] oldIv) {
        final byte[] in = {
            (byte)0xf2, (byte)0x53, (byte)0x50, (byte)0xc6
        };
        for (int x = 0; x < 4; ++x) {
            funnyShit(oldIv[x], in);
            //funnyRamon(oldIv[x], in);
            //System.out.println(HexTool.toString(in));
        }
        return in;
    }

    /**
     * Returns the IV of this instance as a string.
     */
    @Override
    public String toString() {
        return "IV: " + HexTool.toString(iv);
    }

    /**
     * Does funny stuff. <code>this.OldIV</code> must not equal
     * <code>in</code> Modifies <code>in</code> and returns it for
     * convenience.
     *
     * @param inputByte The byte to apply the funny stuff to.
     * @param in Something needed for all this to occur.
     * @return The modified version of <code>in</code>.
     */
    public static byte[] funnyShit(final byte inputByte, final byte[] in) {
        byte elina = in[1];
        byte moritz = funnyBytes[(int) elina & 0xFF];
        moritz -= inputByte;
        in[0] += moritz;
        moritz = in[2];
        moritz ^= funnyBytes[(int) inputByte & 0xFF];
        elina -= (int) moritz & 0xFF;
        in[1] = elina;
        elina = in[3];
        moritz = elina;
        elina -= (int) in[0] & 0xFF;
        moritz = funnyBytes[(int) moritz & 0xFF];
        moritz += inputByte;
        moritz ^= in[2];
        in[2] = moritz;
        elina += (int) funnyBytes[(int) inputByte & 0xFF] & 0xFF;
        in[3] = elina;

        int merry = ((int) in[0]) & 0xFF;
        merry |= (in[1] << 8) & 0xFF00;
        merry |= (in[2] << 16) & 0xFF0000;
        merry |= (in[3] << 24) & 0xFF000000;
        int ret_value = merry;
        ret_value = ret_value >>> 0x1d;
        merry = merry << 3;
        ret_value = ret_value | merry;

        in[0] = (byte) (ret_value & 0xFF);
        in[1] = (byte) ((ret_value >> 8) & 0xFF);
        in[2] = (byte) ((ret_value >> 16) & 0xFF);
        in[3] = (byte) ((ret_value >> 24) & 0xFF);

        return in;
    }

    public static byte[] funnyRamon(final byte inputByte, final byte[] in) {
        final byte[] rammyByte = new byte[] {
            (byte)0xEC, (byte)0x3F, (byte)0x77, (byte)0xA4, (byte)0x45, (byte)0xD0, (byte)0x71, (byte)0xBF,
            (byte)0xB7, (byte)0x98, (byte)0x20, (byte)0xFC, (byte)0x4B, (byte)0xE9, (byte)0xB3, (byte)0xE1,
            (byte)0x5C, (byte)0x22, (byte)0xF7, (byte)0x0C, (byte)0x44, (byte)0x1B, (byte)0x81, (byte)0xBD,
            (byte)0x63, (byte)0x8D, (byte)0xD4, (byte)0xC3, (byte)0xF2, (byte)0x10, (byte)0x19, (byte)0xE0,
            (byte)0xFB, (byte)0xA1, (byte)0x6E, (byte)0x66, (byte)0xEA, (byte)0xAE, (byte)0xD6, (byte)0xCE,
            (byte)0x06, (byte)0x18, (byte)0x4E, (byte)0xEB, (byte)0x78, (byte)0x95, (byte)0xDB, (byte)0xBA,
            (byte)0xB6, (byte)0x42, (byte)0x7A, (byte)0x2A, (byte)0x83, (byte)0x0B, (byte)0x54, (byte)0x67,
            (byte)0x6D, (byte)0xE8, (byte)0x65, (byte)0xE7, (byte)0x2F, (byte)0x07, (byte)0xF3, (byte)0xAA,
            (byte)0x27, (byte)0x7B, (byte)0x85, (byte)0xB0, (byte)0x26, (byte)0xFD, (byte)0x8B, (byte)0xA9,
            (byte)0xFA, (byte)0xBE, (byte)0xA8, (byte)0xD7, (byte)0xCB, (byte)0xCC, (byte)0x92, (byte)0xDA,
            (byte)0xF9, (byte)0x93, (byte)0x60, (byte)0x2D, (byte)0xDD, (byte)0xD2, (byte)0xA2, (byte)0x9B,
            (byte)0x39, (byte)0x5F, (byte)0x82, (byte)0x21, (byte)0x4C, (byte)0x69, (byte)0xF8, (byte)0x31,
            (byte)0x87, (byte)0xEE, (byte)0x8E, (byte)0xAD, (byte)0x8C, (byte)0x6A, (byte)0xBC, (byte)0xB5,
            (byte)0x6B, (byte)0x59, (byte)0x13, (byte)0xF1, (byte)0x04, (byte)0x00, (byte)0xF6, (byte)0x5A,
            (byte)0x35, (byte)0x79, (byte)0x48, (byte)0x8F, (byte)0x15, (byte)0xCD, (byte)0x97, (byte)0x57,
            (byte)0x12, (byte)0x3E, (byte)0x37, (byte)0xFF, (byte)0x9D, (byte)0x4F, (byte)0x51, (byte)0xF5,
            (byte)0xA3, (byte)0x70, (byte)0xBB, (byte)0x14, (byte)0x75, (byte)0xC2, (byte)0xB8, (byte)0x72,
            (byte)0xC0, (byte)0xED, (byte)0x7D, (byte)0x68, (byte)0xC9, (byte)0x2E, (byte)0x0D, (byte)0x62,
            (byte)0x46, (byte)0x17, (byte)0x11, (byte)0x4D, (byte)0x6C, (byte)0xC4, (byte)0x7E, (byte)0x53,
            (byte)0xC1, (byte)0x25, (byte)0xC7, (byte)0x9A, (byte)0x1C, (byte)0x88, (byte)0x58, (byte)0x2C,
            (byte)0x89, (byte)0xDC, (byte)0x02, (byte)0x64, (byte)0x40, (byte)0x01, (byte)0x5D, (byte)0x38,
            (byte)0xA5, (byte)0xE2, (byte)0xAF, (byte)0x55, (byte)0xD5, (byte)0xEF, (byte)0x1A, (byte)0x7C,
            (byte)0xA7, (byte)0x5B, (byte)0xA6, (byte)0x6F, (byte)0x86, (byte)0x9F, (byte)0x73, (byte)0xE6,
            (byte)0x0A, (byte)0xDE, (byte)0x2B, (byte)0x99, (byte)0x4A, (byte)0x47, (byte)0x9C, (byte)0xDF,
            (byte)0x09, (byte)0x76, (byte)0x9E, (byte)0x30, (byte)0x0E, (byte)0xE4, (byte)0xB2, (byte)0x94,
            (byte)0xA0, (byte)0x3B, (byte)0x34, (byte)0x1D, (byte)0x28, (byte)0x0F, (byte)0x36, (byte)0xE3,
            (byte)0x23, (byte)0xB4, (byte)0x03, (byte)0xD8, (byte)0x90, (byte)0xC8, (byte)0x3C, (byte)0xFE,
            (byte)0x5E, (byte)0x32, (byte)0x24, (byte)0x50, (byte)0x1F, (byte)0x3A, (byte)0x43, (byte)0x8A,
            (byte)0x96, (byte)0x41, (byte)0x74, (byte)0xAC, (byte)0x52, (byte)0x33, (byte)0xF0, (byte)0xD9,
            (byte)0x29, (byte)0x80, (byte)0xB1, (byte)0x16, (byte)0xD3, (byte)0xAB, (byte)0x91, (byte)0xB9,
            (byte)0x84, (byte)0x7F, (byte)0x61, (byte)0x1E, (byte)0xCF, (byte)0xC5, (byte)0xD1, (byte)0x56,
            (byte)0x3D, (byte)0xCA, (byte)0xF4, (byte)0x05, (byte)0xC6, (byte)0xE5, (byte)0x08, (byte)0x49
        };

        //for (int i = 0; i < 4; ++i) {
            byte a = in[1];
            byte b = a;
            b = rammyByte[b];
            b -= inputByte;
            in[0] += b;
            b = in[2];
            b = (byte) (b ^ rammyByte[inputByte]);
            a -= b;
            in[1] = a;
            a = in[3];
            b = a;
            a -= in[0];
            b = rammyByte[b];
            b += inputByte;
            b = ((byte) (b ^ in[2]));
            in[2] = b;
            a += rammyByte[inputByte];
            in[3] = a;
            int c = 0, d = 0;
            for (int j = 0; j < 4; ++j) {
                c = in[0] + in[1] * 0x100 + in[2] * 0x100 * 0x100 + in[3] * 0x100 * 0x100 * 0x100;
                d = c;
            }
            c = c >> 0x1D;
            d = d << 0x03;
            c = c | d;
            in[0] = ((byte) (c % 0x100));
            c = c / 0x100;
            in[1] = ((byte) (c % 0x100));
            c = c / 0x100;
            in[2] = ((byte) (c % 0x100));
            in[3] = ((byte) (c / 0x100));
        //}
        //for (int i = 0; i < 4; ++i) {
            //inputByte[i] = in[i];
        //}
        return in;
    }
}
