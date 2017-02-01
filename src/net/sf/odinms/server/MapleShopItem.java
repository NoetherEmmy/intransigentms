package net.sf.odinms.server;

public class MapleShopItem {
    private final short buyable;
    private final int itemId, price;
    private short availible;

    /** Creates a new instance of MapleShopItem */
    public MapleShopItem(short buyable, int itemId, int price) {
        this.buyable = buyable;
        this.itemId = itemId;
        this.price = price;
    }

    public short getBuyable() {
        return buyable;
    }

    public short getAvailible() {
        return availible;
    }

    public void setAvailible(short set) {
        availible = set;
    }

    public void decAvailible() {
        availible--;
    }

    public void incAvailible() {
        availible++;
    }

    public int getItemId() {
        return itemId;
    }

    public int getPrice() {
        return price;
    }

    public long getRefresh() {
        return 0L;
    }
}
