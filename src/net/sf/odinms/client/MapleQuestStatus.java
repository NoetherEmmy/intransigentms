package net.sf.odinms.client;

import net.sf.odinms.server.quest.MapleQuest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapleQuestStatus {
    public enum Status {
        UNDEFINED(-1),
        NOT_STARTED(0),
        STARTED(1),
        COMPLETED(2);
        final int status;

        Status(int id) {
            status = id;
        }

        public int getId() {
            return status;
        }

        public static Status getById(int id) {
            for (final Status l : Status.values()) {
                if (l.getId() == id) return l;
            }
            return null;
        }
    }
    private final MapleQuest quest;
    private Status status;
    private final Map<Integer, Integer> killedMobs = new LinkedHashMap<>();
    private int npc;
    private long completionTime;
    private int forfeited;

    public MapleQuestStatus(MapleQuest quest, Status status) {
        this.quest = quest;
        setStatus(status);
        this.completionTime = System.currentTimeMillis();
        if (status == Status.STARTED) {
            registerMobs();
        }
    }

    public MapleQuestStatus(MapleQuest quest, Status status, int npc) {
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

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getNpc() {
        return npc;
    }

    public void setNpc(int npc) {
        this.npc = npc;
    }

    private void registerMobs() {
        List<Integer> relevants = quest.getRelevantMobs();
        for (int i : relevants) {
            killedMobs.put(i, 0);
        }
    }

    public boolean mobKilled(int id) {
        if (killedMobs.get(id) != null) {
            killedMobs.put(id, killedMobs.get(id) + 1);
            return true;
        }
        return false;
    }

    public void setMobKills(int id, int count) {
        killedMobs.put(id, count);
    }

    public boolean hasMobKills() {
        return !killedMobs.isEmpty();
    }

    public int getMobKills(int id) {
        if (killedMobs.get(id) == null) return 0;
        return killedMobs.get(id);
    }

    public Map<Integer, Integer> getMobKills() {
        return Collections.unmodifiableMap(killedMobs);
    }

    public int getMobNum(int id) {
        int i = 0;
        for (int kMob : killedMobs.values()) {
            i++;
            if (kMob == id) return i;
        }
        return i;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }

    public int getForfeited() {
        return forfeited;
    }

    public void setForfeited(int forfeited) {
        if (forfeited >= this.forfeited) {
            this.forfeited = forfeited;
        } else {
            throw new IllegalArgumentException("Can't set forfeits to something lower than before.");
        }
    }
}
