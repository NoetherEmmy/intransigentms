package net.sf.odinms.server.life;

public enum Element {
    NEUTRAL, FIRE, ICE, LIGHTING, POISON, HOLY;

    public static Element getFromChar(char c) {
        switch (Character.toUpperCase(c)) {
            case 'F':
                return FIRE;
            case 'I':
                return ICE;
            case 'L':
                return LIGHTING;
            case 'P': //
            case 'S':
                return POISON;
            case 'H':
                return HOLY;
        }
        throw new IllegalArgumentException("Unknown element char '" + c + "'");
    }
}
