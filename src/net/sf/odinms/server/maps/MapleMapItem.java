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
    public MapleMapItem(final IItem item, final Point position, final MapleMapObject dropper, final MapleCharacter owner) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.owner = owner;
        this.meso = 0;
    }

    public MapleMapItem(final int meso, final int displayMeso, final Point position, final MapleMapObject dropper, final MapleCharacter owner) {
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

    public void setPickedUp(final boolean pickedUp) {
        this.pickedUp = pickedUp;
    }

    @Override
    public void sendDestroyData(final MapleClient client) {
        client.getSession().write(MaplePacketCreator.removeItemFromMap(getObjectId(), 1, 0));
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.ITEM;
    }

    @Override
    public void sendSpawnData(final MapleClient client) {
        if (meso > 0) {
            client.getSession().write(MaplePacketCreator.dropMesoFromMapObject(displayMeso, getObjectId(),
            dropper.getObjectId(), owner.getId(), null, getPosition(), (byte) 2));
        } else {
            client.getSession().write(MaplePacketCreator.dropItemFromMapObject(item.getItemId(), getObjectId(),
            0, owner.getId(), null, getPosition(), (byte) 2));
        }
    }
}
