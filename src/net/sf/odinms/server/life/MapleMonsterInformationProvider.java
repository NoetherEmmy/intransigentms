package net.sf.odinms.server.life;

import net.sf.odinms.database.DatabaseConnection;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapleMonsterInformationProvider {
    public static class DropEntry {
        public DropEntry(final int itemId, final int chance) {
            this(itemId, chance, 0);
        }

        public DropEntry(final int itemId, final int chance, final int questId) {
            this.itemId = itemId;
            this.chance = chance;
            this.questId = questId;
        }

        public final int itemId, chance, questId;
        public int assignedRangeStart, assignedRangeLength;

        @Override
        public String toString() {
            return itemId + " chance: " + chance;
        }
    }

    public static final int APPROX_FADE_DELAY = 90;
    private static MapleMonsterInformationProvider instance;
    private final Map<Integer, List<DropEntry>> drops = new HashMap<>();
    //private static final Logger log = LoggerFactory.getLogger(MapleMonsterInformationProvider.class);

    private MapleMonsterInformationProvider() {
    }

    public static MapleMonsterInformationProvider getInstance() {
        if (instance == null) instance = new MapleMonsterInformationProvider();
        return instance;
    }

    public List<DropEntry> retrieveDropChances(final int monsterId) {
        if (drops.containsKey(monsterId)) return drops.get(monsterId);
        final List<DropEntry> ret = new ArrayList<>();
        try {
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps1 =
                con.prepareStatement(
                    "SELECT itemid, chance, monsterid FROM monsterdrops " +
                        "WHERE (monsterid = ? AND chance >= 0) OR (monsterid <= 0)"
                );
            ps1.setInt(1, monsterId);
            final ResultSet rs1 = ps1.executeQuery();
            MapleMonster theMonster = null;
            while (rs1.next()) {
                final int rowMonsterId = rs1.getInt("monsterid");
                int chance = rs1.getInt("chance");
                if (rowMonsterId != monsterId && rowMonsterId != 0) {
                    if (theMonster == null) {
                        theMonster = MapleLifeFactory.getMonster(monsterId);
                    }
                    chance += theMonster.getLevel() * rowMonsterId;
                }
                ret.add(new DropEntry(rs1.getInt("itemid"), chance));
            }
            rs1.close();
            ps1.close();

            final PreparedStatement ps2 =
                con.prepareStatement(
                    "SELECT itemid, monsterid, chance, questid FROM monsterquestdrops " +
                        "WHERE (monsterid = ? AND chance >= 0) OR (monsterid <= 0)"
                );
            ps2.setInt(1, monsterId);
            final ResultSet rs2 = ps2.executeQuery();
            theMonster = null;
            while (rs2.next()) {
                final int rowMonsterId = rs2.getInt("monsterid");
                int chance = rs2.getInt("chance");
                if (rowMonsterId != monsterId && rowMonsterId != 0) {
                    if (theMonster == null) {
                        theMonster = MapleLifeFactory.getMonster(monsterId);
                    }
                    chance += theMonster.getLevel() * rowMonsterId;
                }
                ret.add(new DropEntry(rs2.getInt("itemid"), chance, rs2.getInt("questid")));
            }
            rs2.close();
            ps2.close();
        } catch (final Exception e) {
            System.err.println("Error retrieving drops for monster " + monsterId);
            e.printStackTrace();
        }
        drops.put(monsterId, ret);
        return ret;
    }

    public void clearDrops() {
        drops.clear();
    }
}
