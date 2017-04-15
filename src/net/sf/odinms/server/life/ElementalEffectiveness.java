package net.sf.odinms.server.life;

public enum ElementalEffectiveness {
    NORMAL, IMMUNE, STRONG, WEAK;

    public static ElementalEffectiveness getByNumber(final int num) {
        switch (num) {
            case 1:
                return IMMUNE;
            case 2:
                return STRONG;
            case 3:
                return WEAK;
            default:
                throw new IllegalArgumentException("Unkown effectiveness: " + num);
        }
    }
}
