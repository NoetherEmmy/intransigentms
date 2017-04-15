package net.sf.odinms.client;

import net.sf.odinms.net.LongValueHolder;

public enum MapleDisease implements LongValueHolder {
    NULL(0x0L),
    SLOW(0x1L),
    SEDUCE(0x80L),
    STUN(0x2000000000000L),
    POISON(0x4000000000000L),
    SEAL(0x8000000000000L),
    DARKNESS(0x10000000000000L),
    WEAKEN(0x4000000000000000L),
    CURSE(0x8000000000000000L);
    private final long i;

    MapleDisease(final long i) {
        this.i = i;
    }

    @Override
    public long getValue() {
        return i;
    }

    public static MapleDisease getType(final int skill) {
        switch (skill) {
            case 120:
                return MapleDisease.SEAL;
            case 121:
                return MapleDisease.DARKNESS;
            case 122:
                return MapleDisease.WEAKEN;
            case 123:
                return MapleDisease.STUN;
            case 125:
                return MapleDisease.POISON;
            case 128:
                return MapleDisease.SEDUCE;
            default:
                return null;
        }
    }
}
