package net.sf.odinms.client;

import java.util.regex.Pattern;

public final class MapleCharacterUtil {
    private MapleCharacterUtil() {
    }

    public static boolean canCreateChar(final String name, final int world) {
        return isNameLegal(name) && MapleCharacter.getIdByName(name, world) < 0;
    }

    public static boolean isNameLegal(final String name) {
        return !(name.length() < 3 || name.length() > 12) &&
               Pattern.compile("[a-zA-Z0-9_-]{3,12}").matcher(name).matches();
    }

    public static boolean hasSymbols(final String name) {
        final String[] symbols =
        {
            "`", "~", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")",
            "_", "-", "=", "+", "{", "[", "]", "}", "|", ";", ":", "'",
            ",", "<", ">", ".", "?", "/"
        };
        for (byte s = 0; s < symbols.length; ++s) {
            if (name.contains(symbols[s])) return true;
        }
        return false;
    }

    public static String makeMapleReadable(final String in) {
        String wui = in.replace('I', 'i');
        wui = wui.replace('l', 'L');
        wui = wui.replace("rn", "Rn");
        wui = wui.replace("vv", "Vv");
        wui = wui.replace("VV", "Vv");
        return wui;
    }
}
