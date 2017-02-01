package net.sf.odinms.server;

import net.sf.odinms.client.IItem;

import java.util.Calendar;

public class MTSItemInfo {
    private final int price;
    private final IItem item;
    private final String seller;
    private final int id, cid, year, month;
    private int day = 1;

    public MTSItemInfo(IItem item, int price, int id, int cid, String seller, String date) {
        this.item = item;
        this.price = price;
        this.seller = seller;
        this.id = id;
        this.cid = cid;
        year = Integer.parseInt(date.substring(0, 4));
        month = Integer.parseInt(date.substring(5, 7));
        day = Integer.parseInt(date.substring(8, 10));
    }

    public IItem getItem() {
        return item;
    }

    public int getPrice() {
        return price;
    }

    public int getRealPrice() {
        return price + getTaxes();
    }

    public int getTaxes() {
        return 110 + (int) (price * 0.1d);
    }

    public int getID() {
        return id;
    }

    public int getCID() {
        return cid;
    }

    public long getEndingDate() {
        Calendar now = Calendar.getInstance();
        now.set(year, month - 1, day);
        return now.getTimeInMillis();
    }

    public String getSeller() {
        return seller;
    }
}
