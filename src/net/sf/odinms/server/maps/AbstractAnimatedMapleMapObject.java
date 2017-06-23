package net.sf.odinms.server.maps;

public abstract class AbstractAnimatedMapleMapObject extends AbstractMapleMapObject implements AnimatedMapleMapObject {
    private int stance;

    public int getStance() {
        return stance;
    }

    public void setStance(final int stance) {
        this.stance = stance;
    }

    public boolean isFacingLeft() {
        return stance % 2 == 1;
    }
}
