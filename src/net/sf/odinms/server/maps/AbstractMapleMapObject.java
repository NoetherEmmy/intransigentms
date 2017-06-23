package net.sf.odinms.server.maps;

import java.awt.*;

public abstract class AbstractMapleMapObject implements MapleMapObject {
    private final Point position = new Point();
    private int objectId;

    public abstract MapleMapObjectType getType();

    @Override
    public Point getPosition() {
        return new Point(position);
    }

    @Override
    public void setPosition(final Point position) {
        this.position.x = position.x;
        this.position.y = position.y;
    }

    @Override
    public int getObjectId() {
        return objectId;
    }

    @Override
    public void setObjectId(final int id) {
        this.objectId = id;
    }
}
