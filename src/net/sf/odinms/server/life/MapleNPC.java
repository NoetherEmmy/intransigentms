package net.sf.odinms.server.life;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.server.MapleShop;
import net.sf.odinms.server.MapleShopFactory;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;

public class MapleNPC extends AbstractLoadedMapleLife {
    private final MapleNPCStats stats;
    private boolean custom = false;

    public MapleNPC(final int id, final MapleNPCStats stats) {
        super(id);
        this.stats = stats;
    }

    public boolean hasShop() {
        return MapleShopFactory.getInstance().getShopForNPC(getId()) != null;
    }

    public void sendShop(final MapleClient c) {
        final MapleShop shop = MapleShopFactory.getInstance().getShopForNPC(getId());
        shop.sendShop(c);
    }

    @Override
    public void sendSpawnData(final MapleClient client) {
         if (this.getId() >= 9010011 && this.getId() <= 9010013) {
            client.getSession().write(MaplePacketCreator.spawnNPCRequestController(this, false));
         } else {
            client.getSession().write(MaplePacketCreator.spawnNPC(this));
            client.getSession().write(MaplePacketCreator.spawnNPCRequestController(this, true));
        }
    }

    @Override
    public void sendDestroyData(final MapleClient client) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.NPC;
    }

    public String getName() {
        return stats.getName();
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(final boolean custom) {
        this.custom = custom;
    }
}
