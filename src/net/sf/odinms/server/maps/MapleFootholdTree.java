package net.sf.odinms.server.maps;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapleFootholdTree {
    private MapleFootholdTree nw, ne, sw, se;
    private final List<MapleFoothold> footholds = new ArrayList<>();
    private final Point p1, p2, center;
    private int depth, maxDropX, minDropX;

    public MapleFootholdTree(final Point p1, final Point p2) {
        this.p1 = p1;
        this.p2 = p2;
        center = new Point((p2.x - p1.x) / 2, (p2.y - p1.y) / 2);
    }

    public MapleFootholdTree(final Point p1, final Point p2, final int depth) {
        this.p1 = p1;
        this.p2 = p2;
        this.depth = depth;
        center = new Point((p2.x - p1.x) / 2, (p2.y - p1.y) / 2);
    }

    public void insert(final MapleFoothold f) {
        if (depth == 0) {
            if (f.getX1() > maxDropX) maxDropX = f.getX1();
            if (f.getX1() < minDropX) minDropX = f.getX1();
            if (f.getX2() > maxDropX) maxDropX = f.getX2();
            if (f.getX2() < minDropX) minDropX = f.getX2();
        }
        final int maxDepth = 8;
        if (/*footholds.size() == 0 || */
            depth == maxDepth ||
            (f.getX1() >= p1.x && f.getX2() <= p2.x &&
            f.getY1() >= p1.y && f.getY2() <= p2.y)
        ) {
            footholds.add(f);
        } else {
            if (nw == null) {
                nw = new MapleFootholdTree(p1, center, depth + 1);
                ne = new MapleFootholdTree(new Point(center.x, p1.y), new Point(p2.x, center.y), depth + 1);
                sw = new MapleFootholdTree(new Point(p1.x, center.y), new Point(center.x, p2.y), depth + 1);
                se = new MapleFootholdTree(center, p2, depth + 1);
            }
            if (f.getX2() <= center.x && f.getY2() <= center.y) {
                nw.insert(f);
            } else if (f.getX1() > center.x && f.getY2() <= center.y) {
                ne.insert(f);
            } else if (f.getX2() <= center.x && f.getY1() > center.y) {
                sw.insert(f);
            } else {
                se.insert(f);
            }
        }
    }

    private List<MapleFoothold> getRelevants(final Point p) {
        return getRelevants(p, new ArrayList<>());
    }

    private List<MapleFoothold> getRelevants(final Point p, final List<MapleFoothold> list) {
        list.addAll(footholds);
        if (nw != null) {
            if (p.x <= center.x && p.y <= center.y) nw.getRelevants(p, list);
            else if (p.x > center.x && p.y <= center.y) ne.getRelevants(p, list);
            else if (p.x <= center.x && p.y > center.y) sw.getRelevants(p, list);
            else se.getRelevants(p, list);
        }
        return list;
    }

    private MapleFoothold findWallR(final Point p1, final Point p2) {
        MapleFoothold ret;
        for (final MapleFoothold f : footholds) {
            //if (f.isWall()) System.out.println(f.getX1() + " " + f.getX2());
            if (f.isWall() && f.getX1() >= p1.x && f.getX1() <= p2.x && f.getY1() >= p1.y && f.getY2() <= p1.y) {
                return f;
            }
        }
        if (nw != null) {
            if (p1.x <= center.x && p1.y <= center.y) {
                ret = nw.findWallR(p1, p2);
                if (ret != null) return ret;
            }
            if ((p1.x > center.x || p2.x > center.x) && p1.y <= center.y) {
                ret = ne.findWallR(p1, p2);
                if (ret != null) return ret;
            }
            if (p1.x <= center.x && p1.y > center.y) {
                ret = sw.findWallR(p1, p2);
                if (ret != null) return ret;
            }
            if ((p1.x > center.x || p2.x > center.x) && p1.y > center.y) {
                ret = se.findWallR(p1, p2);
                if (ret != null) return ret;
            }
        }
        return null;
    }

    public MapleFoothold findWall(final Point p1, final Point p2) {
        if (p1.y != p2.y) throw new IllegalArgumentException();
        return findWallR(p1, p2);
    }

    public MapleFoothold findBelow(final Point p) {
        final List<MapleFoothold> relevants = getRelevants(p);
        // Find fhs with matching x-coordinates
        final List<MapleFoothold> xMatches = new ArrayList<>();
        for (final MapleFoothold fh : relevants) {
            if (fh.getX1() <= p.x && fh.getX2() >= p.x) xMatches.add(fh);
        }
        Collections.sort(xMatches);
        for (final MapleFoothold fh : xMatches) {
            if (!fh.isWall() && fh.getY1() != fh.getY2()) {
                final int calcY;
                final double s1 = Math.abs(fh.getY2() - fh.getY1());
                final double s2 = Math.abs(fh.getX2() - fh.getX1());
                final double s4 = Math.abs(p.x - fh.getX1());
                final double alpha = Math.atan(s2 / s1);
                final double beta = Math.atan(s1 / s2);
                final double s5 = Math.cos(alpha) * (s4 / Math.cos(beta));
                if (fh.getY2() < fh.getY1()) {
                    calcY = fh.getY1() - (int) s5;
                } else {
                    calcY = fh.getY1() + (int) s5;
                }
                if (calcY >= p.y) return fh;
            } else if (!fh.isWall()) {
                if (fh.getY1() >= p.y) return fh;
            }
        }
        return null;
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

    public int getMaxDropX() {
        return maxDropX;
    }

    public int getMinDropX() {
        return minDropX;
    }
}
