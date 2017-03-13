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
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaplePet extends Item {
    private String name;
    private int uniqueid;
    private int closeness;
    private int level = 1;
    private int fullness = 100;
    private int Fh;
    private Point pos;
    private int stance;

    private MaplePet(int id, byte position, int uniqueid) {
        super(id, position, (short) 1);
        this.uniqueid = uniqueid;
    }

    public static MaplePet loadFromDb(int itemid, byte position, int petid) {
        try {
            MaplePet ret = new MaplePet(itemid, position, petid);
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM pets WHERE petid = ?");
            ps.setInt(1, petid);
            ResultSet rs = ps.executeQuery();
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
        } catch (SQLException ex) {
            Logger.getLogger(MaplePet.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void saveToDb() {
        try {
            Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ? WHERE petid = ?");
            ps.setString(1, getName());
            if (this.level > 30) {
                this.level = 30;
            }
            ps.setInt(2, getLevel());
            ps.setInt(3, getCloseness());
            ps.setInt(4, getFullness());
            ps.setInt(5, getUniqueId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            Logger.getLogger(MaplePet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static int createPet(int itemid) {
        try {
            MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO pets (name, level, closeness, fullness) VALUES (?, ?, ?, ?)");
            ps.setString(1, mii.getName(itemid));
            ps.setInt(2, 1);
            ps.setInt(3, 0);
            ps.setInt(4, 100);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            int ret = rs.getInt(1);
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            Logger.getLogger(MaplePet.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public void setUniqueId(int id) {
        this.uniqueid = id;
    }

    public int getCloseness() {
        return closeness;
    }

    public void setCloseness(int closeness) {
        this.closeness = closeness;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getFullness() {
        return fullness;
    }

    public void setFullness(int fullness) {
        this.fullness = fullness;
    }

    public int getFh() {
        return Fh;
    }

    public void setFh(int Fh) {
        this.Fh = Fh;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public int getStance() {
        return stance;
    }

    public void setStance(int stance) {
        this.stance = stance;
    }

    public boolean canConsume(int itemId) {
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        for (int petId : mii.petsCanConsume(itemId)) {
            if (petId == this.getItemId()) return true;
        }
        return false;
    }

    public void updatePosition(List<LifeMovementFragment> movement) {
        for (LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    Point position = move.getPosition();
                    this.setPos(position);
                }
                this.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}
