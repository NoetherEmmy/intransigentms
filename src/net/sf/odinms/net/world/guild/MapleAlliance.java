
package net.sf.odinms.net.world.guild;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.tools.MaplePacketCreator;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MapleAlliance implements java.io.Serializable {
    public static final long serialVersionUID = 24081985245L;
    private final int[] guilds = new int[5];
    private int allianceId = -1;
    private int capacity;
    private String name;
    private String notice = "";
    private String[] rankTitles = new String[5];

    public MapleAlliance() {
    }

    public MapleAlliance(final String name, final int id, final int guild1, final int guild2) {
        this.name = name;
        allianceId = id;
        guilds[0] = guild1;
        guilds[1] = guild2;
        guilds[2] = -1;
        guilds[3] = -1;
        guilds[4] = -1;
        rankTitles[0] = "Master";
        rankTitles[1] = "Jr.Master";
        rankTitles[2] = "Member";
        rankTitles[3] = "Member";
        rankTitles[4] = "Member";
    }

    public static MapleAlliance loadAlliance(final int id) {
        if (id <= 0) return null;
        final Connection con = DatabaseConnection.getConnection();
        final MapleAlliance alliance = new MapleAlliance();
        try {
            final PreparedStatement ps = con.prepareStatement("SELECT * FROM alliance WHERE id = ?");
            ps.setInt(1, id);
            final ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ps.close();
                rs.close();
                return null;
            }
            alliance.allianceId = id;
            alliance.capacity = rs.getInt("capacity");
            alliance.name = rs.getString("name");
            alliance.notice = rs.getString("notice");
            for (int i = 1; i <= 5; ++i) {
                alliance.rankTitles[i - 1] = rs.getString("rank_title" + i);
            }
            for (int i = 1; i <= 5; ++i) {
                alliance.guilds[i - 1] = rs.getInt("guild" + i);
            }
            ps.close();
            rs.close();
        } catch (final SQLException ignored) {
        }
        return alliance;
    }

    public static void disbandAlliance(final MapleClient c, final int allianceId) {
        final Connection con = DatabaseConnection.getConnection();
        try {
            final PreparedStatement ps = con.prepareStatement("DELETE FROM `alliance` WHERE id = ?");
            ps.setInt(1, allianceId);
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        try {
            c.getChannelServer().getWorldInterface().allianceMessage(c.getPlayer().getGuild().getAllianceId(), MaplePacketCreator.disbandAlliance(allianceId), -1, -1);
            c.getChannelServer().getWorldInterface().disbandAlliance(allianceId);
        } catch (final RemoteException r) {
            c.getChannelServer().reconnectWorld();
        }
    }

    public static boolean canBeUsedAllianceName(final String name) {
        if (name.contains(" ") || name.length() > 12) {
            return false;
        }
        final Connection con = DatabaseConnection.getConnection();
        boolean ret = true;
        try {
            final PreparedStatement ps = con.prepareStatement("SELECT * FROM alliance WHERE name = ?");
            ps.setString(1, name);
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = false;
            }
            ps.close();
            rs.close();
            return ret;
        } catch (final SQLException e) {
            return false;
        }
    }

    public static MapleAlliance createAlliance(final MapleCharacter chr1, final MapleCharacter chr2, final String name) {
        final Connection con = DatabaseConnection.getConnection();
        final int id;
        final int guild1 = chr1.getGuildId();
        final int guild2 = chr2.getGuildId();
        try {
            final PreparedStatement ps = con.prepareStatement("INSERT INTO `alliance` (`name`, `guild1`, `guild2`) VALUES (?, ?, ?)");
            ps.setString(1, name);
            ps.setInt(2, guild1);
            ps.setInt(3, guild2);
            ps.executeUpdate();
            final ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            id = rs.getInt(1);
            rs.close();
            ps.close();
        } catch (final SQLException e) {
            e.printStackTrace();
            return null;
        }
        final MapleAlliance alliance = new MapleAlliance(name, id, guild1, guild2);
        try {
            final WorldChannelInterface wci = chr1.getClient().getChannelServer().getWorldInterface();
            wci.setGuildAllianceId(guild1, id);
            wci.setGuildAllianceId(guild2, id);
            chr1.setAllianceRank(1);
            chr1.saveGuildStatus();
            chr2.setAllianceRank(2);
            chr2.saveGuildStatus();
            wci.addAlliance(id, alliance);
            wci.allianceMessage(id, MaplePacketCreator.makeNewAlliance(alliance, chr1.getClient()), -1, -1);
            return alliance;
        } catch (final RemoteException e) {
            chr1.getClient().getChannelServer().reconnectWorld();
            return null;
        }
    }

    public void saveToDB() {
        final Connection con = DatabaseConnection.getConnection();
        final StringBuilder sb = new StringBuilder();
        sb.append("capacity = ?, ");
        sb.append("notice = ?, ");
        for (int i = 1; i <= 5; ++i) {
            sb.append("rank_title").append(i).append(" = ?, ");
        }
        for (int i = 1; i <= 5; ++i) {
            sb.append("guild").append(i).append(" = ?, ");
        }
        try {
            final PreparedStatement ps = con.prepareStatement("UPDATE `alliance` SET " + sb.toString() + " WHERE id = ?");
            ps.setInt(1, this.capacity);
            ps.setString(2, this.notice);
            for (int i = 0; i < rankTitles.length; ++i) {
                ps.setString(i + 3, rankTitles[i]);
            }
            for (int i = 0; i < guilds.length; ++i) {
                ps.setInt(i + 8, guilds[i]);
            }
            ps.setInt(13, this.allianceId);
            ps.executeQuery();
            ps.close();
        } catch (final SQLException ignored) {
        }
    }

    public boolean addRemGuildFromDB(final int gid, final boolean add) {
        final Connection con = DatabaseConnection.getConnection();
        boolean ret = false;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM alliance WHERE id = ?");
            ps.setInt(1, this.allianceId);
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int avail = -1;
                for (int i = 1; i <= 5; ++i) {
                    final int guildId = rs.getInt("guild" + i);
                    if (add) {
                        if (guildId == -1) {
                            avail = i;
                            break;
                        }
                    } else {
                        if (guildId == gid) {
                            avail = i;
                            break;
                        }
                    }
                }
                rs.close();
                if (avail != -1) { // Empty slot.
                    ps = con.prepareStatement("UPDATE alliance SET " + ("guild" + avail) + " = ? WHERE id = ?");
                    if (add) {
                        ps.setInt(1, gid);
                    } else {
                        ps.setInt(1, -1);
                    }
                    ps.setInt(2, this.allianceId);
                    ps.executeUpdate();
                    ret = true;
                }
                ps.close();
                rs.close();
            }
        } catch (final SQLException ignored) {
        }
        return ret;
    }

    public boolean removeGuild(final int gid) {
        synchronized (guilds) {
            final int gIndex = getGuildIndex(gid);
            if (gIndex != -1) {
                guilds[gIndex] = -1;
            }
            return addRemGuildFromDB(gid, false);
        }
    }

    public boolean addGuild(final int gid) {
        synchronized (guilds) {
            if (getGuildIndex(gid) == -1) {
                final int emptyIndex = getGuildIndex(-1);
                if (emptyIndex != -1) {
                    guilds[emptyIndex] = gid;
                    return addRemGuildFromDB(gid, true);
                }
            }
        }
        return false;
    }

    private int getGuildIndex(final int gid) {
        for (int i = 0; i < guilds.length; ++i) {
            if (guilds[i] == gid) {
                return i;
            }
        }
        return -1;
    }

    public void setRankTitle(final String[] ranks) {
        rankTitles = ranks;
    }

    public void setNotice(final String notice) {
        this.notice = notice;
    }

    public int getId() {
        return allianceId;
    }

    public String getName() {
        return name;
    }

    public String getRankTitle(final int rank) {
        return rankTitles[rank - 1];
    }

    public String getAllianceNotice() {
        return notice;
    }

    public List<Integer> getGuilds() {
        final List<Integer> guilds_ = new ArrayList<>();
        for (final int guild : guilds) {
            if (guild != -1) {
                guilds_.add(guild);
            }
        }
        return guilds_;
    }

    public String getNotice() {
        return notice;
    }

    public void increaseCapacity(final int inc) {
        capacity += inc;
    }

    public int getCapacity() {
        return capacity;
    }
}
