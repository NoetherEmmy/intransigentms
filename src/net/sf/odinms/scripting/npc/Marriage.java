package net.sf.odinms.scripting.npc;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class Marriage {
    //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Marriage.class);

    public static boolean createMarriage(final MapleCharacter player, final MapleCharacter partner) {
        try {
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps =
                con.prepareStatement(
                    "INSERT INTO marriages (husbandid, wifeid) VALUES (?, ?)"
                );
            ps.setInt(1, player.getId());
            ps.setInt(2, partner.getId());
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException sqle) {
            log.warn("Problem marrying " + player.getName() + " and " + partner.getName(), sqle);
            return false;
        }
        return true;
    }

    public static boolean createEngagement(final MapleCharacter player, final MapleCharacter partner) {
        try {
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps =
                con.prepareStatement(
                    "INSERT INTO engagements (husbandid, wifeid) VALUES (?, ?)"
                );
            ps.setInt(1, player.getId());
            ps.setInt(2, partner.getId());
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException sqle) {
            log.warn(
                "Problem announcing engagement with " +
                    player.getName() +
                    " and " +
                    partner.getName(),
                sqle
            );
            return false;
        }
        return true;
    }

    public static void divorceEngagement(final MapleCharacter player) {
        try {
            final Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM engagements WHERE husbandid = ?");
            if (player.getGender() != 0) {
                ps = con.prepareStatement("DELETE FROM engagements WHERE wifeid = ?");
            }
            ps.setInt(1, player.getId());
            ps.executeUpdate();
            final PreparedStatement ps1 =
                con.prepareStatement(
                    "UPDATE characters SET marriagequest = 0 WHERE id = ?"
                );
            ps1.setInt(1, player.getPartnerId());
            ps1.executeUpdate();
            ps1.close();
            ps.close();
        } catch (final SQLException sqle) {
            System.err.println("Problem divorcing " + player.getName() + " and his or her partner");
            sqle.printStackTrace();
        }
    }

    public static void divorceMarriage(final MapleCharacter player) {
        try {
            final Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM marriages WHERE husbandid = ?");
            if (player.getGender() != 0) {
                ps = con.prepareStatement("DELETE FROM marriages WHERE wifeid = ?");
            }
            ps.setInt(1, player.getId());
            ps.executeUpdate();
            final PreparedStatement ps1 =
                con.prepareStatement(
                    "UPDATE characters SET married = 0 WHERE id = ?"
                );
            ps1.setInt(2, player.getPartnerId());
            ps1.executeUpdate();
            final PreparedStatement ps2 =
                con.prepareStatement(
                    "UPDATE characters SET partnerid = 0 WHERE id = ?"
                );
            ps2.setInt(2, player.getPartnerId());
            ps2.executeUpdate();
            ps.close();
            ps1.close();
            ps2.close();
        } catch (final SQLException sqle) {
            System.err.println("Problem divorcing " + player.getName() + " and his or her partner");
            sqle.printStackTrace();
        }
    }
}
