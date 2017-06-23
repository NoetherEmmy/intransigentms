package net.sf.odinms.net.world.guild;

import net.sf.odinms.client.MapleCharacter;

public class MapleGuildCharacter implements java.io.Serializable {
    public static final long serialVersionUID = 2058609046116597760L;
    private int level;
    private final int id;
    private int channel;
    private int jobid;
    private int guildrank, guildid;
    private int allianceRank;
    private boolean online;
    private final String name;

    public MapleGuildCharacter(final MapleCharacter c) {
        name = c.getName();
        level = c.getLevel();
        id = c.getId();
        channel = c.getClient().getChannel();
        jobid = c.getJob().getId();
        guildrank = c.getGuildRank();
        guildid = c.getGuildId();
        online = true;
        allianceRank = c.getAllianceRank();
    }

    public MapleGuildCharacter(final int id, final int lv, final String name, final int channel, final int job, final int rank, final int gid, final boolean on, final int allianceRank) {
        this.level = lv;
        this.id = id;
        this.name = name;
        if (on) this.channel = channel;
        jobid = job;
        online = on;
        guildrank = rank;
        guildid = gid;
        this.allianceRank = allianceRank;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(final int l) {
        level = l;
    }

    public int getId() {
        return id;
    }

    public void setChannel(final int ch) {
        channel = ch;
    }

    public int getChannel() {
        return channel;
    }

    public int getJobId() {
        return jobid;
    }

    public void setJobId(final int job) {
        jobid = job;
    }

    public int getGuildId() {
        return guildid;
    }

    public void setGuildId(final int gid) {
        guildid = gid;
    }

    public void setGuildRank(final int rank) {
        guildrank = rank;
    }

    public int getGuildRank() {
        return guildrank;
    }

    public boolean isOnline() {
        return online;
    }

    public String getName() {
        return name;
    }

    public void setAllianceRank(final int rank) {
        allianceRank = rank;
    }

    public int getAllianceRank() {
        return allianceRank;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof MapleGuildCharacter)) return false;
        final MapleGuildCharacter o = (MapleGuildCharacter) other;
        return (o.getId() == id && o.getName().equals(name));
    }

    public void setOnline(final boolean f) {
        online = f;
    }
}
