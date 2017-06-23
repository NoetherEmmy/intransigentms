package net.sf.odinms.client;

import net.sf.odinms.tools.HexTool;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public final class LoginCrypto {
    private LoginCrypto() {
    }

    private static final char[] iota64 = new char[64];

    private static String toSimpleHexString(final byte[] bytes) {
        return HexTool.toString(bytes).replace(" ", "").toLowerCase();
    }

    private static String hashWithDigest(final String in, final String digest) {
        try {
            final MessageDigest Digester = MessageDigest.getInstance(digest);
            Digester.update(in.getBytes("UTF-8"), 0, in.length());
            final byte[] sha1Hash = Digester.digest();
            return toSimpleHexString(sha1Hash);
        } catch (final NoSuchAlgorithmException ex) {
            throw new RuntimeException("Hashing the password failed", ex);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding the string failed", e);
        }

    }

    public static String hexSha1(final String in) {
        return hashWithDigest(in, "SHA-1");
    }

    private static String hexSha512(final String in) {
        return hashWithDigest(in, "SHA-512");
    }

    public static boolean checkSha1Hash(final String hash, final String password) {
        return hash.equals(hexSha1(password));
    }

    public static boolean checkSaltedSha512Hash(final String hash, final String password, final String salt) {
        return hash.equals(makeSaltedSha512Hash(password, salt));
    }

    public static String makeSaltedSha512Hash(final String password, final String salt) {
        return hexSha512(password + salt);
    }

    public static String makeSalt() {
        final byte[] salt = new byte[16];
        new Random().nextBytes(salt);
        return toSimpleHexString(salt);
    }

    static {
        int i = 0;
        iota64[i++] = '.';
        iota64[i++] = '/';
        for (char c = 'A'; c <= 'Z'; ++c) {
            iota64[i++] = c;
        }
        for (char c = 'a'; c <= 'z'; ++c) {
            iota64[i++] = c;
        }
        for (char c = '0'; c <= '9'; ++c) {
            iota64[i++] = c;
        }
    }

    public static String hashPassword(final String password) {
        final byte[] randomBytes = new byte[6];
        final java.util.Random randomGenerator = new java.util.Random();
        randomGenerator.nextBytes(randomBytes);
        return myCrypt(password, genSalt(randomBytes));
    }

    public static boolean checkPassword(final String password, final String hash) {
        return (myCrypt(password, hash).equals(hash));
    }

    public static boolean isLegacyPassword(final String hash) {
        return hash.substring(0, 3).equals("$H$");
    }

    private static String myCrypt(final String password, String seed) throws RuntimeException {
        String out = null;
        int count = 8;
        final MessageDigest digester;
        if (!seed.substring(0, 3).equals("$H$")) {
            final byte[] randomBytes = new byte[6];
            final java.util.Random randomGenerator = new java.util.Random();
            randomGenerator.nextBytes(randomBytes);
            seed = genSalt(randomBytes);
        }
        final String salt = seed.substring(4, 12);
        if (salt.length() != 8) {
            throw new RuntimeException("Error hashing password - Invalid seed.");
        }
        byte[] sha1Hash;
        try {
            digester = MessageDigest.getInstance("SHA-1");
            digester.update((salt + password).getBytes("iso-8859-1"), 0, (salt + password).length());
            sha1Hash = digester.digest();
            do {
                final byte[] CombinedBytes = new byte[sha1Hash.length + password.length()];
                System.arraycopy(sha1Hash, 0, CombinedBytes, 0, sha1Hash.length);
                System.arraycopy(
                    password.getBytes("iso-8859-1"),
                    0,
                    CombinedBytes,
                    sha1Hash.length,
                    password.getBytes("iso-8859-1").length
                );
                digester.update(CombinedBytes, 0, CombinedBytes.length);
                sha1Hash = digester.digest();
            } while (--count > 0);
            out = seed.substring(0, 12);
            out += encode64(sha1Hash);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            System.err.println("Error hashing password. " + e);
        }
        if (out == null) {
            throw new RuntimeException("Error hashing password: out == null");
        }
        return out;
    }

    private static String genSalt(final byte[] Random) {
        String Salt = "$H$";
        Salt += iota64[30];
        Salt += encode64(Random);
        return Salt;
    }

    private static String encode64(final byte[] Input) {
        final int iLen = Input.length;
        final int oDataLen = (iLen * 4 + 2) / 3;
        final int oLen = ((iLen + 2) / 3) * 4;
        final char[] out = new char[oLen];
        int ip = 0, op = 0;
        while (ip < iLen) {
            final int i0 = Input[ip++] & 0xff;
            final int i1 = ip < iLen ? Input[ip++] & 0xff : 0;
            final int i2 = ip < iLen ? Input[ip++] & 0xff : 0;
            final int o0 = i0 >>> 2;
            final int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            final int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            final int o3 = i2 & 0x3F;
            out[op++] = iota64[o0];
            out[op++] = iota64[o1];
            out[op] = op < oDataLen ? iota64[o2] : '=';
            op++;
            out[op] = op < oDataLen ? iota64[o3] : '=';
            op++;
        }
        return new String(out);
    }
}
