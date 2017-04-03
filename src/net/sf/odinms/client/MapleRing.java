package net.sf.odinms.client;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MapleRing implements Comparable<MapleRing> {
    private final int ringId, ringId2, partnerId, itemId;
    private final String partnerName;
    private boolean equipped;

    private MapleRing(int id, int id2, int partnerId, int itemid, String partnername) {
        this.ringId = id;
        this.ringId2 = id2;
        this.partnerId = partnerId;
        this.itemId = itemid;
        this.partnerName = partnername;
    }

    public static MapleRing loadFromDb(int ringId) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM rings WHERE id = ?");
            ps.setInt(1, ringId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            MapleRing ret =
                new MapleRing(
                    ringId,
                    rs.getInt("partnerRingId"),
                    rs.getInt("partnerChrId"),
                    rs.getInt("itemid"),
                    rs.getString("partnerName")
                );
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException sqle) {
            return null;
        }
    }

    public static int createRing(int itemid, final MapleCharacter partner1, final MapleCharacter partner2) {
        try {
            if (partner1 == null) {
                return -2; // Partner number 1 is not on the same channel
            } else if (partner2 == null) {
                return -1; // Partner number 2 is not on the same channel
            } else if (checkRingDB(partner1) || checkRingDB(partner2)) {
                return 0;  // Error, or already have ring
            }
            int[] ringID = new int[2];
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps =
                con.prepareStatement(
                    "INSERT INTO rings (itemid, partnerChrId, partnername) VALUES (?, ?, ?)"
                );
            ps.setInt(1, itemid);
            ps.setInt(2, partner2.getId());
            ps.setString(3, partner2.getName());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            ringID[0] = rs.getInt(1);
            rs.close();
            ps.close();
            ps = con.prepareStatement(
                "INSERT INTO rings (itemid, partnerRingId, partnerChrId, partnername) VALUES (?, ?, ?, ?)"
            );
            ps.setInt(1, itemid);
            ps.setInt(2, ringID[0]);
            ps.setInt(3, partner1.getId());
            ps.setString(4, partner1.getName());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            rs.next();
            ringID[1] = rs.getInt(1);
            rs.close();
            ps.close();
            ps = con.prepareStatement("UPDATE rings SET partnerRingId = ? WHERE id = ?");
            ps.setInt(1, ringID[1]);
            ps.setInt(2, ringID[0]);
            ps.executeUpdate();
            ps.close();
            MapleInventoryManipulator.addRing(partner1, itemid, ringID[0]);
            MapleInventoryManipulator.addRing(partner2, itemid, ringID[1]);
            TimerManager.getInstance().schedule(() -> {
                partner1.getClient().getSession().write(MaplePacketCreator.getCharInfo(partner1));
                partner1.getMap().removePlayer(partner1);
                partner1.getMap().addPlayer(partner1);
                partner2.getClient().getSession().write(MaplePacketCreator.getCharInfo(partner2));
                partner2.getMap().removePlayer(partner2);
                partner2.getMap().addPlayer(partner2);
            }, 1000L);
            partner1.dropMessage(5, "Congratulations to you and " + partner2.getName() + ".");
            partner1.dropMessage(5, "Please log off and log back in if the rings do not work.");
            partner2.dropMessage(5, "Congratulations to you and " + partner1.getName() + ".");
            partner2.dropMessage(5, "Please log off and log back in if the rings do not work.");
            return 1;
        } catch (SQLException sqle) {
            return 0;
        }
    }

    public int getRingId() {
        return ringId;
    }

    public int getPartnerRingId() {
        return ringId2;
    }

    public int getPartnerChrId() {
        return partnerId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public boolean isEquipped() {
        return equipped;
    }

    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MapleRing && ((MapleRing) o).getRingId() == ringId;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + ringId;
        return hash;
    }

    @Override
    public int compareTo(MapleRing other) {
        if (ringId < other.getRingId()) {
            return -1;
        } else if (ringId == other.getRingId()) {
            return 0;
        } else {
            return 1;
        }
    }

    public static boolean checkRingDB(MapleCharacter player) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id FROM rings WHERE partnerChrId = ?");
            ps.setInt(1, player.getId());
            ResultSet rs = ps.executeQuery();
            boolean has = rs.next();
            rs.close();
            ps.close();
            return has;
        } catch (SQLException sqle) {
            return true;
        }
    }

    public static void removeRingFromDb(MapleCharacter player) {
        try {
            Connection con = DatabaseConnection.getConnection();
            int otherId;
            PreparedStatement ps = con.prepareStatement("SELECT partnerRingId FROM rings WHERE partnerChrId = ?");
            ps.setInt(1, player.getId());
            ResultSet rs = ps.executeQuery();
            rs.next();
            otherId = rs.getInt("partnerRingId");
            rs.close();
            ps = con.prepareStatement("DELETE FROM rings WHERE partnerChrId = ?");
            ps.setInt(1, player.getId());
            ps.executeUpdate();
            ps = con.prepareStatement("DELETE FROM rings WHERE partnerChrId = ?");
            ps.setInt(1, otherId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ignored) {
        }
    }
}
