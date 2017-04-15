package net.sf.odinms.server;

public enum MapleSquadType {
    ZAKUM(0),
    HORNTAIL(1),
    UNKNOWN(2);

    final byte type;

    MapleSquadType(final int type) {
        this.type = (byte) type;
    }
}
