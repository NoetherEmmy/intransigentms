package net.sf.odinms.client;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.movement.AbsoluteLifeMovement;
import net.sf.odinms.server.movement.LifeMovement;
import net.sf.odinms.server.movement.LifeMovementFragment;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
//import java.util.logging.Level;
//import java.util.logging.Logger;

public class MaplePet extends Item {
    private String name;
    private int uniqueid;
    private int closeness;
    private int level = 1;
    private int fullness = 100;
    private int Fh;
    private Point pos;
    private int stance;

    private MaplePet(final int id, final byte position, final int uniqueid) {
        super(id, position, (short) 1);
        this.uniqueid = uniqueid;
    }

    public static MaplePet loadFromDb(final int itemid, final byte position, final int petid) {
        try {
            final MaplePet ret = new MaplePet(itemid, position, petid);
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps = con.prepareStatement("SELECT * FROM pets WHERE petid = ?");
            ps.setInt(1, petid);
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret.setName(rs.getString("name"));
                ret.setCloseness(rs.getInt("closeness"));
                ret.setLevel(rs.getInt("level"));
                ret.setFullness(rs.getInt("fullness"));
                rs.close();
                ps.close();
                return ret;
            } else {
                rs.close();
                ps.close();
                return null;
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void saveToDb() {
        try {
            final Connection con = DatabaseConnection.getConnection();

            final PreparedStatement ps = con.prepareStatement("UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ? WHERE petid = ?");
            ps.setString(1, name);
            if (this.level > 30) {
                this.level = 30;
            }
            ps.setInt(2, level);
            ps.setInt(3, closeness);
            ps.setInt(4, fullness);
            ps.setInt(5, uniqueid);
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static int createPet(final int itemid) {
        try {
            final MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps = con.prepareStatement("INSERT INTO pets (name, level, closeness, fullness) VALUES (?, ?, ?, ?)");
            ps.setString(1, mii.getName(itemid));
            ps.setInt(2, 1);
            ps.setInt(3, 0);
            ps.setInt(4, 100);
            ps.executeUpdate();
            final ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            final int ret = rs.getInt(1);
            rs.close();
            ps.close();
            return ret;
        } catch (final SQLException ex) {
            ex.printStackTrace();
            return -1;
        }

    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public void setUniqueId(final int id) {
        this.uniqueid = id;
    }

    public int getCloseness() {
        return closeness;
    }

    public void setCloseness(final int closeness) {
        this.closeness = closeness;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(final int level) {
        this.level = level;
    }

    public int getFullness() {
        return fullness;
    }

    public void setFullness(final int fullness) {
        this.fullness = fullness;
    }

    public int getFh() {
        return Fh;
    }

    public void setFh(final int Fh) {
        this.Fh = Fh;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(final Point pos) {
        this.pos = pos;
    }

    public int getStance() {
        return stance;
    }

    public void setStance(final int stance) {
        this.stance = stance;
    }

    public boolean canConsume(final int itemId) {
        final MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        for (final int petId : mii.petsCanConsume(itemId)) {
            if (petId == this.getItemId()) return true;
        }
        return false;
    }

    public void updatePosition(final List<LifeMovementFragment> movement) {
        for (final LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    final Point position = move.getPosition();
                    this.setPos(position);
                }
                this.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}
