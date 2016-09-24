package net.sf.odinms.scripting.npc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.sf.odinms.database.DatabaseConnection;

public class HTSquad {
    private static ResultSet results;

    public static int createSquad(int ch, int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM htsquads WHERE channel = ?");
        ps.setInt(1, ch);
        results = ps.executeQuery();
        results.next();
        if (results.getInt("status") == 0) {
            PreparedStatement ps1 = con.prepareStatement("UPDATE htsquads SET leaderid = ?, status = 1, members = 1 WHERE channel = ?");
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

    public static int checkSquad(int ch) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM htsquads WHERE channel = ?");
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

    public static int setFighting(int ch, int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM htsquads WHERE channel = ?");
        ps.setInt(1, ch);
        results = ps.executeQuery();
        results.next();
        if (results.getInt("status") == 1) {
            PreparedStatement ps1 = con.prepareStatement("UPDATE htsquads SET status = '2' WHERE channel = ?");
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

    public static int checkLeader(int ch, int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT count(*) as num FROM htsquads WHERE channel = ? AND leaderid = ?");
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

    public static int removeSquad(int ch, int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT count(*) as num FROM htsquads WHERE channel = ? AND leaderid = ?");
        ps.setInt(1, ch);
        ps.setInt(2, id);
        results = ps.executeQuery();
        results.next();
        if (results.getInt("num") > 0) {
            PreparedStatement ps1 = con.prepareStatement("UPDATE htsquads SET leaderid = 0, status = 0, members = 0 WHERE channel = ?");
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

    public static int numMembers(int ch) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM htsquads WHERE channel = ?");
        ps.setInt(1, ch);
        results = ps.executeQuery();
        results.next();
        int toReturn = results.getInt("members");
        ps.close();
        return toReturn;
    }

    public static int addMember(int ch) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM htsquads WHERE channel = ?");
        ps.setInt(1, ch);
        results = ps.executeQuery();
        results.next();
        if (results.getInt("status") == 1) {
            PreparedStatement ps1 = con.prepareStatement("UPDATE htsquads SET members = ? WHERE channel = ?");
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