package net.sf.odinms.scripting.npc;

import net.sf.odinms.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class HTSquad {
    private static ResultSet results;

    public static int createSquad(final int ch, final int id) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT * FROM htsquads WHERE channel = ?");
        ps.setInt(1, ch);
        results = ps.executeQuery();
        results.next();
        if (results.getInt("status") == 0) {
            final PreparedStatement ps1 = con.prepareStatement("UPDATE htsquads SET leaderid = ?, status = 1, members = 1 WHERE channel = ?");
            ps1.setInt(1, id);
            ps1.setInt(2, ch);
            ps1.executeUpdate();
            ps.close();
            ps1.close();
            return 1;
        } else {
            ps.close();
            return 0;
        }
    }

    public static int checkSquad(final int ch) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT * FROM htsquads WHERE channel = ?");
        ps.setInt(1, ch);
        results = ps.executeQuery();
        results.next();
        if (results.getInt("status") == 0) {
            ps.close();
            return 0;
        } else if (results.getInt("status") == 1) {
            ps.close();
            return 1;
        } else {
            ps.close();
            return 2;
        }
    }

    public static int setFighting(final int ch) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT * FROM htsquads WHERE channel = ?");
        ps.setInt(1, ch);
        results = ps.executeQuery();
        results.next();
        if (results.getInt("status") == 1) {
            final PreparedStatement ps1 = con.prepareStatement("UPDATE htsquads SET status = '2' WHERE channel = ?");
            ps1.setInt(1, ch);
            ps1.executeUpdate();
            ps.close();
            ps1.close();
            return 1;
        } else {
            ps.close();
            return 0;
        }
    }

    public static int checkLeader(final int ch, final int id) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT count(*) as num FROM htsquads WHERE channel = ? AND leaderid = ?");
        ps.setInt(1, ch);
        ps.setInt(2, id);
        results = ps.executeQuery();
        results.next();
        if (results.getInt("num") > 0) {
            ps.close();
            return 1;
        } else {
            ps.close();
            return 0;
        }
    }

    public static int removeSquad(final int ch, final int id) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT count(*) as num FROM htsquads WHERE channel = ? AND leaderid = ?");
        ps.setInt(1, ch);
        ps.setInt(2, id);
        results = ps.executeQuery();
        results.next();
        if (results.getInt("num") > 0) {
            final PreparedStatement ps1 = con.prepareStatement("UPDATE htsquads SET leaderid = 0, status = 0, members = 0 WHERE channel = ?");
            ps1.setInt(1, ch);
            ps1.executeUpdate();
            ps.close();
            ps1.close();
            return 1;
        } else {
            ps.close();
            return 0;
        }
    }

    public static int numMembers(final int ch) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT * FROM htsquads WHERE channel = ?");
        ps.setInt(1, ch);
        results = ps.executeQuery();
        results.next();
        final int toReturn = results.getInt("members");
        ps.close();
        return toReturn;
    }

    public static int addMember(final int ch) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps = con.prepareStatement("SELECT * FROM htsquads WHERE channel = ?");
        ps.setInt(1, ch);
        results = ps.executeQuery();
        results.next();
        if (results.getInt("status") == 1) {
            final PreparedStatement ps1 = con.prepareStatement("UPDATE htsquads SET members = ? WHERE channel = ?");
            ps1.setInt(1, results.getInt("members") + 1);
            ps1.setInt(2, ch);
            ps1.executeUpdate();
            ps.close();
            ps1.close();
            return 1;
        } else {
            ps.close();
            return 0;
        }
    }
}
