package net.sf.odinms.client;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class MapleCharacterUtil {
    private MapleCharacterUtil() {
    }

    /** pure?: yes */
    public static boolean canCreateChar(final String name, final int world) {
        return isNameLegal(name) && MapleCharacter.getIdByName(name, world) < 0;
    }

    /** pure?: yes */
    public static boolean isNameLegal(final String name) {
        return !(name.length() < 4 || name.length() > 12) &&
               Pattern.compile("[a-zA-Z0-9_-]{3,12}").matcher(name).matches();
    }

    /** pure?: yes */
    public static boolean hasSymbols(final String name) {
        final String[] symbols =
        {
            "`", "~", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")",
            "_", "-", "=", "+", "{", "[", "]", "}", "|", ";", ":", "'",
            ",", "<", ">", ".", "?", "/"
        };

        return Stream.of(symbols).anyMatch(name::contains);
    }

    /** pure?: yes <br /> nullable?: no */
    public static String makeMapleReadable(final String in) {
        return
            in.replace('I', 'i')
              .replace('l', 'L')
              .replace("rn", "Rn")
              .replace("vv", "Vv")
              .replace("VV", "Vv");
    }
}
