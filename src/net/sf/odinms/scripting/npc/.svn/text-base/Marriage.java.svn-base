package net.sf.odinms.scripting.npc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.database.DatabaseConnection;

public class Marriage {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Marriage.class);

    public static void createMarriage(MapleCharacter player, MapleCharacter partner) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO marriages (husbandid, wifeid) VALUES (?, ?)");
            ps.setInt(1, player.getId());
            ps.setInt(2, partner.getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            log.warn("Problem marrying " + player.getName() + " and " + partner.getName(), ex);
        }
    }

    public static void createEngagement(MapleCharacter player, MapleCharacter partner) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO engagements (husbandid, wifeid) VALUES (?, ?)");
            ps.setInt(1, player.getId());
            ps.setInt(2, partner.getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            log.warn("Problem announcing engagement with " + player.getName() + " and " + partner.getName(), ex);
        }
    }

    public static void divorceEngagement(MapleCharacter player) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM engagements WHERE husbandid = ?");
            if (player.getGender() != 0) {
                ps = con.prepareStatement("DELETE FROM engagements WHERE wifeid = ?");
            }
            ps.executeUpdate();
            PreparedStatement ps1 = con.prepareStatement("UPDATE characters SET marriagequest = 0 WHERE id = ?");
            ps1.setInt(1, player.getPartnerId());
            ps1.executeUpdate();
            ps1.close();
            ps.close();
        } catch (SQLException ex) {
            log.warn("Problem divorcing" + player.getName() + " and his or her partner", ex);
        }
    }

    public static void divorceMarriage(MapleCharacter player) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM marriages WHERE husbandid = ?");
            if (player.getGender() != 0) {
                ps = con.prepareStatement("DELETE FROM marriages WHERE wifeid = ?");
            }
            ps.setInt(1, player.getId());
            ps.executeUpdate();
            PreparedStatement ps1 = con.prepareStatement("UPDATE characters SET married = 0 WHERE id = ?");
            ps1.setInt(2, player.getPartnerId());
            ps1.executeUpdate();
            PreparedStatement ps2 = con.prepareStatement("UPDATE characters SET partnerid = 0 WHERE id = ?");
            ps2.setInt(2, player.getPartnerId());
            ps2.executeUpdate();
            ps.close();
            ps1.close();
            ps2.close();
        } catch (SQLException ex) {
            log.warn("Problem divorcing" + player.getName() + " and his or her partner", ex);
        }
    }
}