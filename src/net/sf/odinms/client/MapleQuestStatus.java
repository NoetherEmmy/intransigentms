package net.sf.odinms.client;

import net.sf.odinms.server.quest.MapleQuest;

import java.util.*;

public class MapleQuestStatus {
    public enum Status {
        UNDEFINED(-1),
        NOT_STARTED(0),
        STARTED(1),
        COMPLETED(2);
        final int status;

        Status(final int id) {
            status = id;
        }

        public int getId() {
            return status;
        }

        public static Status getById(final int id) {
            return Arrays.stream(Status.values()).filter(l -> l.getId() == id).findFirst().orElse(null);
        }
    }
    private final MapleQuest quest;
    private Status status;
    private final Map<Integer, Integer> killedMobs = new LinkedHashMap<>();
    private int npc;
    private long completionTime;
    private int forfeited;

    public MapleQuestStatus(final MapleQuest quest, final Status status) {
        this.quest = quest;
        setStatus(status);
        this.completionTime = System.currentTimeMillis();
        if (status == Status.STARTED) {
            registerMobs();
        }
    }

    public MapleQuestStatus(final MapleQuest quest, final Status status, final int npc) {
        this.quest = quest;
        this.setStatus(status);
        this.setNpc(npc);
        this.completionTime = System.currentTimeMillis();
        if (status == Status.STARTED) {
            registerMobs();
        }
    }

    public MapleQuest getQuest() {
        return quest;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public int getNpc() {
        return npc;
    }

    public void setNpc(final int npc) {
        this.npc = npc;
    }

    private void registerMobs() {
        final List<Integer> relevants = quest.getRelevantMobs();
        for (final int i : relevants) {
            killedMobs.put(i, 0);
        }
    }

    public boolean mobKilled(final int id) {
        if (killedMobs.get(id) != null) {
            killedMobs.put(id, killedMobs.get(id) + 1);
            return true;
        }
        return false;
    }

    public void setMobKills(final int id, final int count) {
        killedMobs.put(id, count);
    }

    public boolean hasMobKills() {
        return !killedMobs.isEmpty();
    }

    public int getMobKills(final int id) {
        if (killedMobs.get(id) == null) return 0;
        return killedMobs.get(id);
    }

    public Map<Integer, Integer> getMobKills() {
        return Collections.unmodifiableMap(killedMobs);
    }

    public int getMobNum(final int id) {
        int i = 0;
        for (final int kMob : killedMobs.values()) {
            i++;
            if (kMob == id) return i;
        }
        return i;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(final long completionTime) {
        this.completionTime = completionTime;
    }

    public int getForfeited() {
        return forfeited;
    }

    public void setForfeited(final int forfeited) {
        if (forfeited >= this.forfeited) {
            this.forfeited = forfeited;
        } else {
            throw new IllegalArgumentException("Can't set forfeits to something lower than before.");
        }
    }
}
