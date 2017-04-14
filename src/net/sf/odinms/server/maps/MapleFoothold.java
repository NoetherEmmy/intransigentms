package net.sf.odinms.server.maps;

import java.awt.*;

public class MapleFoothold implements Comparable<MapleFoothold> {
    private final Point p1, p2;
    private final int id;
    private int next, prev;

    public MapleFoothold(Point p1, Point p2, int id) {
        this.p1 = p1;
        this.p2 = p2;
        /*
        p2.x--;
        p2.y--;
        */
        this.id = id;
    }

    public boolean isWall() {
        return p1.x == p2.x;
    }

    public int getX1() {
        return p1.x;
    }

    public int getX2() {
        return p2.x;
    }

    public int getY1() {
        return p1.y;
    }

    public int getY2() {
        return p2.y;
    }

    @Override
    public int compareTo(final MapleFoothold other) {
        /*
        if (p2.y < other.getY1() && p1.y >= other.getY2()) {
            return -1;
        }
        */
        return p1.y - other.getY1();

        // The comparator below is intransitive:
        /*
        if (p2.y < other.getY1()) {
            return -1;
        } else if (p1.y > other.getY2()) {
            return 1;
        } else {
            return 0;
        }
        */
    }

    public int getId() {
        return id;
    }

    public int getNext() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }

    public int getPrev() {
        return prev;
    }

    public void setPrev(int prev) {
        this.prev = prev;
    }
}
