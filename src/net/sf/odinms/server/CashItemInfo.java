package net.sf.odinms.server;

public class CashItemInfo {
    private final int itemId, count, price;

    public CashItemInfo(final int itemId, final int count, final int price) {
        this.itemId = itemId;
        this.count = count;
        this.price = price;
    }

    public int getId() {
        return itemId;
    }

    public int getCount() {
        return count;
    }

    public int getPrice() {
        return price;
    }
}
