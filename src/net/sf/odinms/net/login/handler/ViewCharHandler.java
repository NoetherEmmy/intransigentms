package net.sf.odinms.net.login.handler;

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
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ViewCharHandler extends AbstractMaplePacketHandler {
    private static final Logger log = LoggerFactory.getLogger(ViewCharHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ?");
            ps.setInt(1, c.getAccID());
            int charsNum = 0;
            List<Integer> worlds = new ArrayList<>();
            List<MapleCharacter> chars = new ArrayList<>();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int cworld = rs.getInt("world");
                boolean inside = false;
                for (int w : worlds) {
                    if (w == cworld) inside = true;
                }
                if (!inside) worlds.add(cworld);
                MapleCharacter chr = MapleCharacter.loadCharFromDB(rs.getInt("id"), c, false);
                chars.add(chr);
                charsNum++;
            }
            rs.close();
            ps.close();
            int unk = charsNum + (3 - charsNum % 3);
            c.getSession().write(MaplePacketCreator.showAllCharacter(charsNum, unk));
            for (int w : worlds) {
                List<MapleCharacter> chrsinworld = new ArrayList<>();
                for (MapleCharacter chr : chars) {
                    if (chr.getWorld() == w) {
                        chrsinworld.add(chr);
                    }
                }
                c.getSession().write(MaplePacketCreator.showAllCharacterInfo(w, chrsinworld));
            }
        } catch (Exception e) {
            log.error("Viewing all chars failed", e);
        }
    }
}
