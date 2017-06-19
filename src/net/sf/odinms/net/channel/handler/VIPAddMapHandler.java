package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.HexTool;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class VIPAddMapHandler extends AbstractMaplePacketHandler {
    //private static final Logger log = LoggerFactory.getLogger(VIPAddMapHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final Connection con = DatabaseConnection.getConnection();
        final int operation = slea.readByte();
        final int type = slea.readByte();
        final MapleCharacter player = c.getPlayer();

        switch (operation) {
            case 0: // Remove map
                final int mapid = slea.readInt();
                try {
                    final PreparedStatement ps = con.prepareStatement("DELETE FROM viprockmaps WHERE cid = ? AND mapid = ? AND type = ?");
                    ps.setInt(1, player.getId());
                    ps.setInt(2, mapid);
                    ps.setInt(3, type);
                    ps.executeUpdate();
                    ps.close();
                } catch (final SQLException sqle) {
                    System.err.println("Could not handle removing VIP Rock map: " + sqle.getMessage());
                }
                break;
            case 1: // Add map
                try {
                    final PreparedStatement ps = con.prepareStatement("INSERT INTO viprockmaps (`cid`, `mapid`, `type`) VALUES (?, ?, ?)");
                    ps.setInt(1, player.getId());
                    ps.setInt(2, player.getMapId());
                    ps.setInt(3, type);
                    ps.executeUpdate();
                    ps.close();
                } catch (final SQLException sqle) {
                    System.err.println("Could not handle adding VIP Rock map: " + sqle.getMessage());
                }
                break;
            default:
                log.info(
                    "Unhandled VIP Rock operation, operation = " +
                        operation +
                        ", remaining:\n" +
                        HexTool.toString(slea)
                );
                break;
        }
        c.getSession().write(MaplePacketCreator.refreshVIPRockMapList(player.getVIPRockMaps(type), type));
    }
}
