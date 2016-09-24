package net.sf.odinms.server;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.tools.MaplePacketCreator;

public class AutobanManager implements Runnable {

    private static class ExpirationEntry implements Comparable<ExpirationEntry> {

        public long time;
        public int acc;
        public int points;

        public ExpirationEntry(long time, int acc, int points) {
            this.time = time;
            this.acc = acc;
            this.points = points;
        }

        public int compareTo(AutobanManager.ExpirationEntry o) {
            return (int) (time - o.time);
        }
    }
    private Map<Integer, Integer> points = new HashMap<Integer, Integer>();
    private Map<Integer, List<String>> reasons = new HashMap<Integer, List<String>>();
    private Set<ExpirationEntry> expirations = new TreeSet<ExpirationEntry>();
    private static final int AUTOBAN_POINTS = 1000;
    private static AutobanManager instance = null;

    public static AutobanManager getInstance() {
        if (instance == null) {
            instance = new AutobanManager();
        }
        return instance;
    }

    public void autoban(MapleClient c, String reason) {
        if (c.getPlayer().isGM()) return;
        addPoints(c, AUTOBAN_POINTS, 0, reason);
    }

    public synchronized void addPoints(MapleClient c, int points, long expiration, String reason) {
        if (c.getPlayer().isGM()) return;
        int acc = c.getPlayer().getAccountID();
        List<String> reasonList;
        if (this.points.containsKey(acc)) {
            if (this.points.get(acc) >= AUTOBAN_POINTS) {
                return;
            }
            this.points.put(acc, this.points.get(acc) + points);
            reasonList = this.reasons.get(acc);
            reasonList.add(reason);
        } else {
            this.points.put(acc, points);
            reasonList = new LinkedList<String>();
            reasonList.add(reason);
            this.reasons.put(acc, reasonList);
        }
        if (this.points.get(acc) >= AUTOBAN_POINTS) {
            String name = c.getPlayer().getName();
            StringBuilder banReason = new StringBuilder();
            for (String s : reasons.get(acc)) {
                banReason.append(s);
            }
            if (c.getChannelServer().AutoBan()) {
                c.getPlayer().ban(banReason.toString(), true);
                try {
                    c.getChannelServer().getWorldInterface().broadcastGMMessage(null, MaplePacketCreator.serverNotice(6, name + " has been banned by the system. (Reason: " + reason + ")").getBytes());
                } catch (RemoteException e) {
                    c.getChannelServer().reconnectWorld();
                }
            }
            return;
        }
        if (expiration > 0) {
            expirations.add(new ExpirationEntry(System.currentTimeMillis() + expiration, acc, points));
        }
    }

    public void run() {
        long now = System.currentTimeMillis();
        for (ExpirationEntry e : expirations) {
            if (e.time <= now) {
                this.points.put(e.acc, this.points.get(e.acc) - e.points);
            } else {
                return;
            }
        }
    }
}