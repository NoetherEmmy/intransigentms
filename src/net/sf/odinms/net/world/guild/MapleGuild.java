package net.sf.odinms.net.world.guild;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.WorldRegistryImpl;
import net.sf.odinms.tools.MaplePacketCreator;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MapleGuild implements java.io.Serializable {
    public static final int CREATE_GUILD_COST = 5000000;
    public static final int CHANGE_EMBLEM_COST = 15000000;
    public static final int INCREASE_CAPACITY_COST = 5000000;
    public static final boolean ENABLE_BBS = true;

    private enum BCOp {
        NONE, DISBAND, EMBELMCHANGE
    }

    public static final long serialVersionUID = 6322150443228168192L;
    private final List<MapleGuildCharacter> members;
    private final String[] rankTitles = new String[5];
    private String name;
    private int id;
    private int gp;
    private int logo;
    private int logoColor;
    private int leader;
    private int capacity;
    private int logoBG;
    private int logoBGColor;
    private String notice;
    private int signature;
    private final Map<Integer, List<Integer>> notifications = new LinkedHashMap<>();
    private boolean bDirty = true;
    private int allianceId;

    public MapleGuild(final int guildid, final MapleGuildCharacter initiator) {
        members = new ArrayList<>();
        final Connection con;
        try {
            con = DatabaseConnection.getConnection();
        } catch (final Exception e) {
            System.err.println("Unable to connect to database to load guild information: " + e);
            return;
        }
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildid);
            ResultSet rs = ps.executeQuery();
            if (!rs.first()) {
                rs.close();
                ps.close();
                id = -1;
                return;
            }
            id = guildid;
            name = rs.getString("name");
            gp = rs.getInt("GP");
            logo = rs.getInt("logo");
            logoColor = rs.getInt("logoColor");
            logoBG = rs.getInt("logoBG");
            logoBGColor = rs.getInt("logoBGColor");
            capacity = rs.getInt("capacity");
            for (int i = 1; i <= 5; ++i) {
                rankTitles[i - 1] = rs.getString("rank" + i + "title");
            }
            leader = rs.getInt("leader");
            notice = rs.getString("notice");
            signature = rs.getInt("signature");
            allianceId = rs.getInt("allianceId");
            ps.close();
            rs.close();
            ps = con.prepareStatement("SELECT id, name, level, job, guildrank, allianceRank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC");
            ps.setInt(1, guildid);
            rs = ps.executeQuery();
            if (!rs.first()) {
                rs.close();
                ps.close();
                System.err.println("No members in guild, ID: " + guildid);
                return;
            }
            do {
                members.add(new MapleGuildCharacter(rs.getInt("id"), rs.getInt("level"), rs.getString("name"), -1, rs.getInt("job"), rs.getInt("guildrank"), guildid, false, rs.getInt("allianceRank")));
            } while (rs.next());
            if (initiator != null) {
                setOnline(initiator.getId(), true, initiator.getChannel());
            }
            rs.close();
            ps.close();
        } catch (final SQLException sqle) {
            System.err.println("Unable to read guild information from SQL: " + sqle);
        }
    }

    public void buildNotifications() {
        if (!bDirty) return;
        final Set<Integer> chs = WorldRegistryImpl.getInstance().getChannelServer();
        if (notifications.keySet().size() != chs.size()) {
            notifications.clear();
            for (final Integer ch : chs) {
                notifications.put(ch, new java.util.ArrayList<>());
            }
        } else {
            for (final List<Integer> l : notifications.values()) {
                l.clear();
            }
        }
        synchronized (members) {
            for (final MapleGuildCharacter mgc : members) {
                if (!mgc.isOnline()) {
                    continue;
                }
                final List<Integer> ch = notifications.get(mgc.getChannel());
                if (ch == null) {
                    System.err.println("Unable to connect to channel " + mgc.getChannel());
                } else {
                    ch.add(mgc.getId());
                }
            }
        }
        bDirty = false;
    }

    public void writeToDB() {
        writeToDB(false);
    }

    public void writeToDB(final boolean bDisband) {
        final Connection con;
        try {
            con = DatabaseConnection.getConnection();
        } catch (final Exception e) {
            System.err.println("Unable to connect to database to write guild information: " + e);
            return;
        }
        try {
            if (!bDisband) {
                String sql =
                    "UPDATE guilds SET " +
                        "GP = ?, " +
                        "logo = ?, " +
                        "logoColor = ?, " +
                        "logoBG = ?, " +
                        "logoBGColor = ?, ";
                for (int i = 0; i < 5; ++i) {
                    sql += "rank" + (i + 1) + "title = ?, ";
                }
                sql += "capacity = ?, " + "notice = ? WHERE guildid = ?";
                final PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, gp);
                ps.setInt(2, logo);
                ps.setInt(3, logoColor);
                ps.setInt(4, logoBG);
                ps.setInt(5, logoBGColor);
                for (int i = 6; i < 11; ++i) {
                    ps.setString(i, rankTitles[i - 6]);
                }
                ps.setInt(11, capacity);
                ps.setString(12, notice);
                ps.setInt(13, id);
                ps.execute();
                ps.close();
            } else {
                PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
                ps.setInt(1, id);
                ps.execute();
                ps.close();
                ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
                ps.setInt(1, id);
                ps.execute();
                ps.close();
                broadcast(MaplePacketCreator.guildDisband(id));
            }
        } catch (final SQLException sqle) {
            System.err.println(sqle.getLocalizedMessage() + " | " + sqle);
        }
    }

    public int getId() {
        return id;
    }

    public int getLeaderId() {
        return leader;
    }

    public int getGP() {
        return gp;
    }

    public int getLogo() {
        return logo;
    }

    public void setLogo(final int l) {
        logo = l;
    }

    public int getLogoColor() {
        return logoColor;
    }

    public void setLogoColor(final int c) {
        logoColor = c;
    }

    public int getLogoBG() {
        return logoBG;
    }

    public void setLogoBG(final int bg) {
        logoBG = bg;
    }

    public int getLogoBGColor() {
        return logoBGColor;
    }

    public void setLogoBGColor(final int c) {
        logoBGColor = c;
    }

    public String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public String getName() {
        return name;
    }

    public java.util.Collection<MapleGuildCharacter> getMembers() {
        return java.util.Collections.unmodifiableCollection(members);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSignature() {
        return signature;
    }

    public void broadcast(final MaplePacket packet) {
        broadcast(packet, -1, BCOp.NONE);
    }

    public void broadcast(final MaplePacket packet, final int exception) {
        broadcast(packet, exception, BCOp.NONE);
    }

    public void broadcast(final MaplePacket packet, final int exceptionId, final BCOp bcop) {
        final WorldRegistryImpl wr = WorldRegistryImpl.getInstance();
        final Set<Integer> chs = wr.getChannelServer();
        synchronized (notifications) {
            if (bDirty) {
                buildNotifications();
            }
            try {
                ChannelWorldInterface cwi;
                for (final Integer ch : chs) {
                    cwi = wr.getChannel(ch);
                    if (!notifications.get(ch).isEmpty()) {
                        if (bcop == BCOp.DISBAND) {
                            cwi.setGuildAndRank(notifications.get(ch), 0, 5, exceptionId);
                        } else if (bcop == BCOp.EMBELMCHANGE) {
                            cwi.changeEmblem(this.id, notifications.get(ch), new MapleGuildSummary(this));
                        } else {
                            cwi.sendPacket(notifications.get(ch), packet, exceptionId);
                        }
                    }
                }
            } catch (final RemoteException re) {
                System.err.println("Failed to contact channel(s) for broadcast:");
                re.printStackTrace();
            }
        }
    }

    public void guildMessage(final MaplePacket serverNotice) {
        for (final MapleGuildCharacter mgc : members) {
            for (final ChannelServer cs : ChannelServer.getAllInstances()) {
                if (cs.getPlayerStorage().getCharacterById(mgc.getId()) != null) {
                    final MapleCharacter chr = cs.getPlayerStorage().getCharacterById(mgc.getId());
                    chr.getClient().getSession().write(serverNotice);
                    break;
                }
            }
        }
    }

    public void setOnline(final int cid, final boolean online, final int channel) {
        boolean bBroadcast = true;
        for (final MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
                if (mgc.isOnline() && online) {
                    bBroadcast = false;
                }
                mgc.setOnline(online);
                mgc.setChannel(channel);
                break;
            }
        }
        if (bBroadcast) {
            this.broadcast(MaplePacketCreator.guildMemberOnline(id, cid, online), cid);
        }
        bDirty = true;
    }

    public void guildChat(final String name, final int cid, final String msg) {
        this.broadcast(MaplePacketCreator.multiChat(name, msg, 2), cid);
    }

    public String getRankTitle(final int rank) {
        return rankTitles[rank - 1];
    }

    public static int createGuild(final int leaderId, final String name) {
        final Connection con;
        try {
            //Properties dbProp = new Properties();
            //InputStreamReader is = new FileReader("db.properties");
            //dbProp.load(is);
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.first()) {
                rs.close();
                ps.close();
                return 0;
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`) VALUES (?, ?, ?)");
            ps.setInt(1, leaderId);
            ps.setString(2, name);
            ps.setInt(3, (int) System.currentTimeMillis());
            ps.execute();
            ps.close();
            ps = con.prepareStatement("SELECT guildid FROM guilds WHERE leader = ?");
            ps.setInt(1, leaderId);
            rs = ps.executeQuery();
            int guildid = 0;
            if (rs.first()) guildid = rs.getInt("guildid");
            rs.close();
            ps.close();
            return guildid;
        } catch (final Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int addGuildMember(final MapleGuildCharacter mgc) {
        synchronized (members) {
            if (members.size() >= capacity) return 0;

            for (int i = members.size() - 1; i >= 0; --i) {
                if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(mgc.getName()) < 0) {
                    members.add(i + 1, mgc);
                    bDirty = true;
                    break;
                }
            }
        }
        this.broadcast(MaplePacketCreator.newGuildMember(mgc));
        return 1;
    }

    public void leaveGuild(final MapleGuildCharacter mgc) {
        this.broadcast(MaplePacketCreator.memberLeft(mgc, false));
        synchronized (members) {
            members.remove(mgc);
            bDirty = true;
        }
    }

    public void expelMember(final MapleGuildCharacter initiator, final String name, final int cid) {
        synchronized (members) {
            final java.util.Iterator<MapleGuildCharacter> itr = members.iterator();
            MapleGuildCharacter mgc;
            while (itr.hasNext()) {
                mgc = itr.next();
                if (mgc.getId() == cid && initiator.getGuildRank() < mgc.getGuildRank()) {
                    this.broadcast(MaplePacketCreator.memberLeft(mgc, true));
                    itr.remove();
                    bDirty = true;
                    this.broadcast(MaplePacketCreator.serverNotice(5, initiator.getName() + " has expelled " + mgc.getName() + "."));
                    try {
                        if (mgc.isOnline()) {
                            WorldRegistryImpl.getInstance().getChannel(mgc.getChannel()).setGuildAndRank(cid, 0, 5);
                        } else {
                            final String sendTo = mgc.getName();
                            final String sendFrom = initiator.getName();
                            final String msg = "You have been expelled from the guild.";
                            try {
                                MaplePacketCreator.sendUnkwnNote(sendTo, msg, sendFrom);
                            } catch (final SQLException sqle) {
                                System.err.println("Exception during saving note: " + sqle);
                            }
                            WorldRegistryImpl.getInstance().getChannel(1).setOfflineGuildStatus((short) 0, (byte) 5, cid);
                        }
                    } catch (final RemoteException re) {
                        re.printStackTrace();
                        return;
                    }
                    return;
                }
            }
            System.err.println("Unable to find member with name " + name + " and ID " + cid);
        }
    }

    public void changeRank(final int cid, final int newRank) {
        for (final MapleGuildCharacter mgc : members) {
            if (cid == mgc.getId()) {
                try {
                    if (mgc.isOnline()) {
                        WorldRegistryImpl.getInstance().getChannel(mgc.getChannel()).setGuildAndRank(cid, this.id, newRank);
                    } else {
                        WorldRegistryImpl.getInstance().getChannel(1).setOfflineGuildStatus((short) this.id, (byte) newRank, cid);
                    }
                } catch (final RemoteException re) {
                    re.printStackTrace();
                    return;
                }
                mgc.setGuildRank(newRank);
                this.broadcast(MaplePacketCreator.changeRank(mgc));
                return;
            }
        }
        System.err.println("Unable to find the correct ID for changeRank(" + cid + ", " + newRank + ")");
    }

    public void setGuildNotice(final String notice) {
        this.notice = notice;
        writeToDB();
        this.broadcast(MaplePacketCreator.guildNotice(this.id, notice));
    }

    public void memberLevelJobUpdate(final MapleGuildCharacter mgc) {
        for (final MapleGuildCharacter member : members) {
            if (mgc.equals(member)) {
                member.setJobId(mgc.getJobId());
                member.setLevel(mgc.getLevel());
                this.broadcast(MaplePacketCreator.guildMemberLevelJobUpdate(mgc));
                break;
            }
        }
    }

    public void changeRankTitle(final String[] ranks) {
        System.arraycopy(ranks, 0, rankTitles, 0, 5);
        this.broadcast(MaplePacketCreator.rankTitleChange(this.id, ranks));
        this.writeToDB();
    }

    public void disbandGuild() {
        this.writeToDB(true);
        this.broadcast(null, -1, BCOp.DISBAND);
    }

    public void setGuildEmblem(final short bg, final byte bgcolor, final short logo, final byte logocolor) {
        this.logoBG = bg;
        this.logoBGColor = bgcolor;
        this.logo = logo;
        this.logoColor = logocolor;
        this.writeToDB();
        this.broadcast(null, -1, BCOp.EMBELMCHANGE);
    }

    public MapleGuildCharacter getMGC(final int cid) {
        for (final MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
                return mgc;
            }
        }
        return null;
    }

    public boolean increaseCapacity() {
        if (capacity >= 100) {
            return false;
        }
        capacity += 5;
        this.writeToDB();
        this.broadcast(MaplePacketCreator.guildCapacityChange(this.id, this.capacity));
        return true;
    }

    public void gainGP(final int amount) {
        this.gp += amount;
        this.writeToDB();
        this.guildMessage(MaplePacketCreator.updateGP(this.id, this.gp));
    }

    public static MapleGuildResponse sendInvite(final MapleClient c, final String targetName) {
        final MapleCharacter mc = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
        if (mc == null) {
            return MapleGuildResponse.NOT_IN_CHANNEL;
        }
        if (mc.getGuildId() > 0) {
            return MapleGuildResponse.ALREADY_IN_GUILD;
        }
        mc.getClient().getSession().write(MaplePacketCreator.guildInvite(c.getPlayer().getGuildId(), c.getPlayer().getName()));
        return null;
    }

    public static void displayGuildRanks(final MapleClient c, final int npcid) {
        try {
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps = con.prepareStatement("SELECT `name`, `GP`, `logoBG`, `logoBGColor`, `logo`, `logoColor` FROM guilds ORDER BY `GP` DESC LIMIT 50");
            final ResultSet rs = ps.executeQuery();
            c.getSession().write(MaplePacketCreator.showGuildRanks(npcid, rs));
            ps.close();
            rs.close();
        } catch (final SQLException sqle) {
            System.err.println("Failed to display guild ranks: " + sqle);
        }
    }

    public int getAllianceId() {
        return this.allianceId;
    }

    public void setAllianceId(final int aid) {
        this.allianceId = aid;
        try {
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps = con.prepareStatement("UPDATE guilds SET allianceId = ? WHERE guildid = ?");
            ps.setInt(1, aid);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException ignored) {
        }
    }
}
