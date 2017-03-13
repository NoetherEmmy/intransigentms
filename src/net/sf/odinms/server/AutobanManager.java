package net.sf.odinms.server;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.tools.MaplePacketCreator;

import java.rmi.RemoteException;
import java.util.*;

public class AutobanManager implements Runnable {
    private static class ExpirationEntry implements Comparable<ExpirationEntry> {
        public final long time;
        public final int acc, points;

        public ExpirationEntry(long time, int acc, int points) {
            this.time = time;
            this.acc = acc;
            this.points = points;
        }

        public int compareTo(AutobanManager.ExpirationEntry o) {
            return (int) (time - o.time);
        }
    }

    private final Map<Integer, Integer> points = new HashMap<>();
    private final Map<Integer, List<String>> reasons = new HashMap<>();
    private final Set<ExpirationEntry> expirations = new TreeSet<>();
    private static final int AUTOBAN_POINTS = 1000;
    private static AutobanManager instance;

    public static AutobanManager getInstance() {
        if (instance == null) instance = new AutobanManager();
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
            reasonList = new ArrayList<>();
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
                    c.getChannelServer()
                     .getWorldInterface()
                     .broadcastGMMessage(
                         null,
                         MaplePacketCreator.serverNotice(
                             6,
                             name + " has been banned by the system. (Reason: " + reason + ")"
                         ).getBytes()
                     );
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
