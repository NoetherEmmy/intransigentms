package net.sf.odinms.server.maps;

public interface AnimatedMapleMapObject extends MapleMapObject {
    int getStance();

    void setStance(int stance);

    boolean isFacingLeft();
}
