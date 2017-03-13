package net.sf.odinms.server;

public class CashItemInfo {
    private final int itemId, count, price;

    public CashItemInfo(int itemId, int count, int price) {
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
