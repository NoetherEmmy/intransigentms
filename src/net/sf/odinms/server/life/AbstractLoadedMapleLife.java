package net.sf.odinms.server.life;

import net.sf.odinms.server.maps.AbstractAnimatedMapleMapObject;

public abstract class AbstractLoadedMapleLife extends AbstractAnimatedMapleMapObject {
    private final int id;
    private int f;
    private boolean hide;
    private int fh, start_fh, cy, rx0, rx1;

    public AbstractLoadedMapleLife(final int id) {
        this.id = id;
    }

    public AbstractLoadedMapleLife(final AbstractLoadedMapleLife life) {
        this(life.getId());
        this.f = life.f;
        this.hide = life.hide;
        this.fh = life.fh;
        this.start_fh = life.fh;
        this.cy = life.cy;
        this.rx0 = life.rx0;
        this.rx1 = life.rx1;
    }

    public int getF() {
        return f;
    }

    public void setF(final int f) {
        this.f = f;
    }

    public boolean isHidden() {
        return hide;
    }

    public void setHide(final boolean hide) {
        this.hide = hide;
    }

    public int getFh() {
        return fh;
    }

    public void setFh(final int fh) {
        this.fh = fh;
    }

    public int getStartFh() {
        return start_fh;
    }

    public int getCy() {
        return cy;
    }

    public void setCy(final int cy) {
        this.cy = cy;
    }

    public int getRx0() {
        return rx0;
    }

    public void setRx0(final int rx0) {
        this.rx0 = rx0;
    }

    public int getRx1() {
        return rx1;
    }

    public void setRx1(final int rx1) {
        this.rx1 = rx1;
    }

    public int getId() {
        return id;
    }
}
