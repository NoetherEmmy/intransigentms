package net.sf.odinms.server.maps;

import java.awt.Point;
import net.sf.odinms.client.MapleClient;

public interface MapleMapObject {
    public int getObjectId();

    public void setObjectId(int id);

    public MapleMapObjectType getType();

    /**
     * returns a copy of the current position
     * @return
     */
    public Point getPosition();

    /**
     * sets the current position of the object to the position given in the point.
     * @param position
     */
    public void setPosition(Point position);

    public void sendSpawnData (MapleClient client);

    public void sendDestroyData (MapleClient client);
}