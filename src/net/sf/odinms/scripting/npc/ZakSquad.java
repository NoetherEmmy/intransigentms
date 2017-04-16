package net.sf.odinms.scripting.npc;

import net.sf.odinms.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ZakSquad {
    private static ResultSet rs;

    public static int createSquad(final int ch, final int id) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT status FROM zaksquads WHERE channel = ?");
        ps.setInt(1, ch);
        rs = ps.executeQuery();
        rs.next();
        if (rs.getInt("status") == 0) {
            final PreparedStatement ps1 = con.prepareStatement("UPDATE zaksquads SET leaderid = ?, status = 1, members = 1 WHERE channel = ?");
            ps1.setInt(1, id);
            ps1.setInt(2, ch);
            ps1.executeUpdate();
            rs.close();
            ps.close();
            ps1.close();
            return 1;
        } else {
            rs.close();
            ps.close();
            return 0;
        }
    }

    public static int checkSquad(final int ch) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT status FROM zaksquads WHERE channel = ?");
        ps.setInt(1, ch);
        rs = ps.executeQuery();
        rs.next();
        if (rs.getInt("status") == 0) {
            rs.close();
            ps.close();
            return 0;
        } else if (rs.getInt("status") == 1) {
            rs.close();
            ps.close();
            return 1;
        } else {
            rs.close();
            ps.close();
            return 2;
        }
    }

    public static int setFighting(final int ch) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT status FROM zaksquads WHERE channel = ?");
        ps.setInt(1, ch);
        rs = ps.executeQuery();
        rs.next();
        if (rs.getInt("status") == 1) {
            final PreparedStatement ps1 = con.prepareStatement("UPDATE zaksquads SET status = '2' WHERE channel = ?");
            ps1.setInt(1, ch);
            ps1.executeUpdate();
            rs.close();
            ps.close();
            ps1.close();
            return 1;
        } else {
            rs.close();
            ps.close();
            return 0;
        }
    }

    public static int checkLeader(final int ch, final int id) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT count(*) as num FROM zaksquads WHERE channel = ? AND leaderid = ?");
        ps.setInt(1, ch);
        ps.setInt(2, id);
        rs = ps.executeQuery();
        rs.next();
        if (rs.getInt("num") > 0) {
            rs.close();
            ps.close();
            return 1;
        } else {
            rs.close();
            ps.close();
            return 0;
        }
    }

    public static int removeSquad(final int ch, final int id) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT count(*) as num FROM zaksquads WHERE channel = ? AND leaderid = ?");
        ps.setInt(1, ch);
        ps.setInt(2, id);
        rs = ps.executeQuery();
        rs.next();
        if (rs.getInt("num") > 0) {
            final PreparedStatement ps1 = con.prepareStatement("UPDATE zaksquads SET leaderid = 0, status = 0, members = 0 WHERE channel = ?");
            ps1.setInt(1, ch);
            ps1.executeUpdate();
            rs.close();
            ps.close();
            ps1.close();
            return 1;
        } else {
            rs.close();
            ps.close();
            return 0;
        }
    }

    public static int numMembers(final int ch) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT * FROM zaksquads WHERE channel = ?");
        ps.setInt(1, ch);
        rs = ps.executeQuery();
        rs.next();
        final int toReturn = rs.getInt("members");
        rs.close();
        ps.close();
        return toReturn;
    }

    public static int addMember(final int ch) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT * FROM zaksquads WHERE channel = ?");
        ps.setInt(1, ch);
        rs = ps.executeQuery();
        rs.next();
        if (rs.getInt("status") == 1) {
            final PreparedStatement ps1 = con.prepareStatement("UPDATE zaksquads SET members = ? WHERE channel = ?");
            ps1.setInt(1, rs.getInt("members") + 1);
            ps1.setInt(2, ch);
            ps1.executeUpdate();
            rs.close();
            ps.close();
            ps1.close();
            return 1;
        } else {
            rs.close();
            ps.close();
            return 0;
        }
    }
}
