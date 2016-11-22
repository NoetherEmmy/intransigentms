package net.sf.odinms.server.maps;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.tools.MaplePacketCreator;

import java.awt.*;

public class MapleMapItem extends AbstractMapleMapObject {
    protected final IItem item;
    protected final MapleMapObject dropper;
    protected final MapleCharacter owner;
    protected final int meso;
    protected int displayMeso;
    protected boolean pickedUp = false;

    /** Creates a new instance of MapleMapItem */
    public MapleMapItem(IItem item, Point position, MapleMapObject dropper, MapleCharacter owner) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.owner = owner;
        this.meso = 0;
    }

    public MapleMapItem(int meso, int displayMeso, Point position, MapleMapObject dropper, MapleCharacter owner) {
        setPosition(position);
        this.item = null;
        this.meso = meso;
        this.displayMeso = displayMeso;
        this.dropper = dropper;
        this.owner = owner;
    }

    public IItem getItem() {
        return item;
    }

    public MapleMapObject getDropper() {
        return dropper;
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public int getMeso() {
        return meso;
    }

    public boolean isPickedUp() {
        return pickedUp;
    }

    public void setPickedUp(boolean pickedUp) {
        this.pickedUp = pickedUp;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.removeItemFromMap(getObjectId(), 1, 0));
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.ITEM;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (getMeso() > 0) {
            client.getSession().write(MaplePacketCreator.dropMesoFromMapObject(displayMeso, getObjectId(),
            getDropper().getObjectId(), getOwner().getId(), null, getPosition(), (byte) 2));
        } else {
            client.getSession().write(MaplePacketCreator.dropItemFromMapObject(getItem().getItemId(), getObjectId(),
            0, getOwner().getId(), null, getPosition(), (byte) 2));
        }
    }
}
