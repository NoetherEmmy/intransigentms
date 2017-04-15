package net.sf.odinms.server;

import net.sf.odinms.client.IItem;

import java.util.Calendar;

public class DueyPackages {
    private String sender;
    private IItem item;
    private int mesos;
    private int quantity = 1;
    private int day, month, year;
    private final int packageId;

    public DueyPackages(final int pId, final IItem item) {
        this.item = item;
        quantity = item.getQuantity();
        packageId = pId;
    }

    public DueyPackages(final int packageId) { // Meso only package.
        this.packageId = packageId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(final String name) {
        sender = name;
    }

    public IItem getItem() {
        return item;
    }

    public int getMesos() {
        return mesos;
    }

    public void setMesos(final int set) {
        mesos = set;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPackageId() {
        return packageId;
    }

    public boolean isExpired() {
        final Calendar cal1 = Calendar.getInstance();
        cal1.set(year, month - 1, day);
        final long diff = System.currentTimeMillis() - cal1.getTimeInMillis();
        final int diffDays = (int) Math.abs(diff / (24L * 60L * 60L * 1000L));
        return diffDays > 30;
    }

    public long sentTimeInMilliseconds() {
        final Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        return cal.getTimeInMillis();
    }

    public void setSentTime(final String sentTime) {
        day = Integer.parseInt(sentTime.substring(0, 2));
        month = Integer.parseInt(sentTime.substring(3, 5));
        year = Integer.parseInt(sentTime.substring(6, 10));
    }
}
