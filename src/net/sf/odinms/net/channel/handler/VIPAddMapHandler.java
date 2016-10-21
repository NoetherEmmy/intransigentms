package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class VIPAddMapHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(VIPAddMapHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        Connection con = DatabaseConnection.getConnection();
        int operation = slea.readByte();
        int type = slea.readByte();
        MapleCharacter player = c.getPlayer();

        switch (operation) {
            case 0: // Remove map
                int mapid = slea.readInt();
                try {
                    PreparedStatement ps = con.prepareStatement("DELETE FROM viprockmaps WHERE cid = ? AND mapid = ? AND type = ?");
                    ps.setInt(1, player.getId());
                    ps.setInt(2, mapid);
                    ps.setInt(3, type);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException sqle) {
                    System.out.println("Could not handle removing VIP Rock map: " + sqle.getMessage());
                }
                break;
            case 1: // Add map
                try {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO viprockmaps (`cid`, `mapid`, `type`) VALUES (?, ?, ?)");
                    ps.setInt(1, player.getId());
                    ps.setInt(2, player.getMapId());
                    ps.setInt(3, type);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException sqle) {
                    System.out.println("Could not handle adding VIP Rock map: " + sqle.getMessage());
                }
                break;
            default:
                log.info("Unhandled VIP Rock operation: " + slea.toString());
                break;
        }
        c.getSession().write(MaplePacketCreator.refreshVIPRockMapList(player.getVIPRockMaps(type), type));
    }
}