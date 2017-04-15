package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ReportHandler extends AbstractMaplePacketHandler {
    final String[] reasons = {
        "Hacking",
        "Botting",
        "Scamming",
        "Fake GM",
        "Harassment",
        "Advertising"
    };

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final int reportedCharId = slea.readInt();
        final byte reason = slea.readByte();
        String chatlog = "No chatlog";
        final short clogLen = slea.readShort();
        if (clogLen > 0) {
            chatlog = slea.readAsciiString(clogLen);
        }
        System.out.println(c.getPlayer().getName() + " reported character with ID " + reportedCharId);

        if (addReportEntry(c.getPlayer().getId(), reportedCharId, reason, chatlog)) {
            c.getSession().write(MaplePacketCreator.reportReply((byte) 0));
        } else {
            c.getSession().write(MaplePacketCreator.reportReply((byte) 4));
        }
        try {
            final WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
            wci.broadcastGMMessage(
                null,
                MaplePacketCreator.serverNotice(
                    5,
                    c.getPlayer().getName() +
                        " reported " +
                        MapleCharacter.getNameById(reportedCharId, 0) +
                        " for " +
                        reasons[reason] +
                        "."
                ).getBytes()
            );
        } catch (final RemoteException re) {
            c.getChannelServer().reconnectWorld();
        }
    }

    private boolean addReportEntry(final int reporterId, final int victimId, final byte reason, final String chatlog) {
        try {
            final Connection dcon = DatabaseConnection.getConnection();
            final PreparedStatement ps;
            ps = dcon.prepareStatement(
                "INSERT INTO reports VALUES (NULL, CURRENT_TIMESTAMP, ?, ?, ?, ?, 'UNHANDLED')"
            );
            ps.setInt(1, reporterId);
            ps.setInt(2, victimId);
            ps.setInt(3, reason);
            ps.setString(4, chatlog);
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException sqle) {
            return false;
        }
        return true;
    }
}
