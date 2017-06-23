package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class NoteActionHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final int action = slea.readByte();

        if (action == 1) { // Delete
            final int num = slea.readByte();
            slea.readShort();
            for (int i = 0; i < num; ++i) {
                deleteNote(slea.readInt());
                slea.readByte();
            }
        }
    }

    private void deleteNote(final int id) {
        final Connection con = DatabaseConnection.getConnection();
        try {
            final PreparedStatement ps = con.prepareStatement("DELETE FROM notes WHERE `id` = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException ignored) {
        }
    }
}
