package net.sf.odinms.client;

public class MapleKeyBinding {
    private final int type, action;

    public MapleKeyBinding(final int type, final int action) {
        super();
        this.type = type;
        this.action = action;
    }

    public int getType() {
        return type;
    }

    public int getAction() {
        return action;
    }
}
