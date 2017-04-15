package net.sf.odinms.client;

public enum MapleSkinColor {
    NORMAL(0),
    DARK(1),
    BLACK(2),
    PALE(3),
    BLUE(4),
    WHITE(9);
    final int id;

    MapleSkinColor(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static MapleSkinColor getById(final int id) {
        for (final MapleSkinColor l : MapleSkinColor.values()) {
            if (l.getId() == id) return l;
        }
        return null;
    }
}
