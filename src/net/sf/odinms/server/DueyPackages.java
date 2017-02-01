package net.sf.odinms.server;

import net.sf.odinms.client.IItem;

import java.util.Calendar;

public class DueyPackages {
    private String sender;
    private IItem item;
    private int mesos;
    private int day;
    private int quantity = 1;
    private int month;
    private int year;
    private final int packageId;

    public DueyPackages(int pId, IItem item) {
        this.item = item;
        quantity = item.getQuantity();
        packageId = pId;
    }

    public DueyPackages(int packageId) { // Meso only package.
        this.packageId = packageId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String name) {
        sender = name;
    }

    public IItem getItem() {
        return item;
    }

    public int getMesos() {
        return mesos;
    }

    public void setMesos(int set) {
        mesos = set;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPackageId() {
        return packageId;
    }

    public boolean isExpired() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(year, month - 1, day);
        long diff = System.currentTimeMillis() - cal1.getTimeInMillis();
        int diffDays = (int) Math.abs(diff / (24L * 60L * 60L * 1000L));
        return diffDays > 30;
    }

    public long sentTimeInMilliseconds() {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        return cal.getTimeInMillis();
    }

    public void setSentTime(String sentTime) {
        day = Integer.parseInt(sentTime.substring(0, 2));
        month = Integer.parseInt(sentTime.substring(3, 5));
        year = Integer.parseInt(sentTime.substring(6, 10));
    }
}
