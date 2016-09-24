package net.sf.odinms.scripting.npc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.sf.odinms.database.DatabaseConnection;

public class ZakSquad {
    private static ResultSet rs;

    public static int createSquad(int ch, int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT status FROM zaksquads WHERE channel = ?");
        ps.setInt(1, ch);
        rs = ps.executeQuery();
        rs.next();
        if (rs.getInt("status") == 0) {
            PreparedStatement ps1 = con.prepareStatement("UPDATE zaksquads SET leaderid = ?, status = 1, members = 1 WHERE channel = ?");
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

    public static int checkSquad(int ch) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT status FROM zaksquads WHERE channel = ?");
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

    public static int setFighting(int ch, int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT status FROM zaksquads WHERE channel = ?");
        ps.setInt(1, ch);
        rs = ps.executeQuery();
        rs.next();
        if (rs.getInt("status") == 1) {
            PreparedStatement ps1 = con.prepareStatement("UPDATE zaksquads SET status = '2' WHERE channel = ?");
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

    public static int checkLeader(int ch, int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT count(*) as num FROM zaksquads WHERE channel = ? AND leaderid = ?");
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

    public static int removeSquad(int ch, int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT count(*) as num FROM zaksquads WHERE channel = ? AND leaderid = ?");
        ps.setInt(1, ch);
        ps.setInt(2, id);
        rs = ps.executeQuery();
        rs.next();
        if (rs.getInt("num") > 0) {
            PreparedStatement ps1 = con.prepareStatement("UPDATE zaksquads SET leaderid = 0, status = 0, members = 0 WHERE channel = ?");
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

    public static int numMembers(int ch) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM zaksquads WHERE channel = ?");
        ps.setInt(1, ch);
        rs = ps.executeQuery();
        rs.next();
        int toReturn = rs.getInt("members");
        rs.close();
        ps.close();
        return toReturn;
    }

    public static int addMember(int ch) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM zaksquads WHERE channel = ?");
        ps.setInt(1, ch);
        rs = ps.executeQuery();
        rs.next();
        if (rs.getInt("status") == 1) {
            PreparedStatement ps1 = con.prepareStatement("UPDATE zaksquads SET members = ? WHERE channel = ?");
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