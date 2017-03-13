package net.sf.odinms.server;

import java.util.HashMap;
import java.util.Map;

public class MapleShopFactory {
    private final Map<Integer, MapleShop> shops = new HashMap<>();
    private final Map<Integer, MapleShop> npcShops = new HashMap<>();
    private static final MapleShopFactory instance = new MapleShopFactory();

    public static MapleShopFactory getInstance() {
        return instance;
    }

    public void clear() {
        shops.clear();
        npcShops.clear();
    }

    private MapleShop loadShop(int id, boolean isShopId) {
        MapleShop ret = MapleShop.createFromDB(id, isShopId);
        if (ret != null) {
            shops.put(ret.getId(), ret);
            npcShops.put(ret.getNpcId(), ret);
        } else if (isShopId) {
            shops.put(id, null);
        } else {
            npcShops.put(id, null);
        }
        return ret;
    }

    public MapleShop getShop(int shopId) {
        if (shops.containsKey(shopId)) {
            return shops.get(shopId);
        }
        return loadShop(shopId, true);
    }

    public MapleShop getShopForNPC(int npcId) {
        if (npcShops.containsKey(npcId)) {
            npcShops.get(npcId);
        }
        return loadShop(npcId, false);
    }
}
