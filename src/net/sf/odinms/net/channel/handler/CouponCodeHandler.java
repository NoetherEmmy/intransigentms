package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CouponCodeHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(2);
        String code = slea.readMapleAsciiString();
        boolean validcode = getNXCodeValid(code.toUpperCase());

        if (validcode) {
            int type = getNXCodeType(code);
            int item = getNXCodeItem(code);

            if (type != 5) setNXCodeUsed(code, c.getPlayer().getName());
            /*
             * Explanation of type!
             * Basically, this makes coupon codes do
             * different things!
             *
             * Type 0: NX, Type 1: Maple Points,
             * Type 2: Gift Tokens, Type 3: NX + Gift Tokens
             * Type 4: Item
             * Type 5: NX Coupon that can be used over and over
             *
             * When using Types 0-3, the item is the amount
             * of NX or Maple Points you get. When using Type 4
             * the item is the ID of the item you get. Enjoy!
             */
            switch (type) {
                case 0:
                case 1:
                case 2:
                    c.getPlayer().modifyCSPoints(type, item);
                    break;
                case 3:
                    c.getPlayer().modifyCSPoints(1, item);
                    c.getPlayer().modifyCSPoints(2, item / 5000);
                    break;
                case 4:
                    MapleInventoryManipulator.addById(c, item, (short) 1);
                    c.getSession().write(MaplePacketCreator.showCouponRedeemedItem(item));
                    break;
                case 5:
                    c.getPlayer().modifyCSPoints(1, item);
                    break;
            }
            c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
        } else {
            c.getSession().write(MaplePacketCreator.wrongCouponCode());
        }
        c.getSession().write(MaplePacketCreator.enableCSUse0());
        c.getSession().write(MaplePacketCreator.enableCSUse1());
        c.getSession().write(MaplePacketCreator.enableCSUse2());
    }

    private boolean getNXCodeValid(String code) {
        boolean valid = false;
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT `valid` FROM nxcode WHERE code = ?");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) valid = rs.getInt("valid") != 0;
            rs.close();
            ps.close();
        } catch (SQLException ignored) {
        }
        return valid;
    }

    private int getNXCodeType(String code) {
        int type = -1;
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT `type` FROM nxcode WHERE code = ?");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) type = rs.getInt("type");
            rs.close();
            ps.close();
        } catch (SQLException ignored) {
        }
        return type;
    }

    private int getNXCodeItem(String code) {
        int item = -1;
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT `item` FROM nxcode WHERE code = ?");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) item = rs.getInt("item");
            rs.close();
            ps.close();
        } catch (SQLException ignored) {
        }
        return item;
    }

    public void setNXCodeUsed(String code, String name) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE nxcode SET `valid` = 0 WHERE code = ?");
            ps.setString(1, code);
            ps.executeUpdate();
            ps = con.prepareStatement("UPDATE nxcode SET `user` = ? WHERE code = ?");
            ps.setString(1, name);
            ps.setString(2, code);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ignored) {
        }
    }
}
