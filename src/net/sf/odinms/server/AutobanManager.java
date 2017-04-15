package net.sf.odinms.server;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.tools.MaplePacketCreator;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

public class AutobanManager implements Runnable {
    private static class ExpirationEntry implements Comparable<ExpirationEntry> {
        public final long time;
        public final int acc, points;

        public ExpirationEntry(final long time, final int acc, final int points) {
            this.time = time;
            this.acc = acc;
            this.points = points;
        }

        public int compareTo(final AutobanManager.ExpirationEntry o) {
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

    public void autoban(final MapleClient c, final String reason) {
        if (c.getPlayer().isGM()) return;
        addPoints(c, AUTOBAN_POINTS, 0, reason);
    }

    public synchronized void addPoints(final MapleClient c, final int points, final long expiration, final String reason) {
        if (c.getPlayer().isGM()) return;

        final int acc = c.getPlayer().getAccountID();
        final List<String> reasonList;

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
            final String name = c.getPlayer().getName();
            final String banReason = reasons.get(acc).stream().collect(Collectors.joining());

            if (c.getChannelServer().AutoBan()) {
                c.getPlayer().ban(banReason, true);
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
                } catch (final RemoteException e) {
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
        final long now = System.currentTimeMillis();
        for (final ExpirationEntry e : expirations) {
            if (e.time <= now) {
                this.points.put(e.acc, this.points.get(e.acc) - e.points);
            } else {
                return;
            }
        }
    }
}
