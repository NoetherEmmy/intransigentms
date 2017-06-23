package net.sf.odinms.net.login.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class ViewCharHandler extends AbstractMaplePacketHandler {
    //private static final Logger log = LoggerFactory.getLogger(ViewCharHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final Connection con = DatabaseConnection.getConnection();
        try {
            final PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ?");
            ps.setInt(1, c.getAccID());
            int charsNum = 0;
            final List<Integer> worlds = new ArrayList<>();
            final List<MapleCharacter> chars = new ArrayList<>();
            final ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                final int cworld = rs.getInt("world");
                boolean inside = false;
                for (final int w : worlds) {
                    if (w == cworld) inside = true;
                }
                if (!inside) worlds.add(cworld);
                final MapleCharacter chr = MapleCharacter.loadCharFromDB(rs.getInt("id"), c, false);
                chars.add(chr);
                charsNum++;
            }
            rs.close();
            ps.close();
            final int unk = charsNum + (3 - charsNum % 3);
            c.getSession().write(MaplePacketCreator.showAllCharacter(charsNum, unk));
            for (final int w : worlds) {
                final List<MapleCharacter> chrsinworld = new ArrayList<>();
                for (final MapleCharacter chr : chars) {
                    if (chr.getWorld() == w) {
                        chrsinworld.add(chr);
                    }
                }
                c.getSession().write(MaplePacketCreator.showAllCharacterInfo(w, chrsinworld));
            }
        } catch (final Exception e) {
            System.err.println("Viewing all chars failed");
            e.printStackTrace();
        }
    }
}
