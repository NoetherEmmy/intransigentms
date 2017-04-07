package net.sf.odinms.server.maps;

import net.sf.odinms.client.MapleClient;

import java.awt.*;

public interface MapleMapObject {
    int getObjectId();

    void setObjectId(int id);

    MapleMapObjectType getType();

    /**
     * Returns a copy of the current position.
     */
    Point getPosition();

    /**
     * Sets the current position of the object to the position given in the {@code Point}.
     */
    void setPosition(Point position);

    void sendSpawnData(MapleClient client);

    void sendDestroyData(MapleClient client);
}
