package net.sf.odinms.server.PlayerInteraction;

import net.sf.odinms.client.IItem;

public class MaplePlayerShopItem {
    private final IItem item;
    private short bundles;
    private final int price;

    public MaplePlayerShopItem(final IItem item, final short bundles, final int price) {
        this.item = item;
        this.bundles = bundles;
        this.price = price;
    }

    public IItem getItem() {
        return item;
    }

    public short getBundles() {
        return bundles;
    }

    public int getPrice() {
        return price;
    }

    public void setBundles(final short bundles) {
        this.bundles = bundles;
    }
}
