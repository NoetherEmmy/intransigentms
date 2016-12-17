package net.sf.odinms.client;

public class MapleKeyBinding {
    private final int type;
    private final int action;

    public MapleKeyBinding(int type, int action) {
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
