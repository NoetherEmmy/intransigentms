package net.sf.odinms.server;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClanHolder {
    private final Map<MapleCharacter, Integer> online = new LinkedHashMap<>(); // Only for each channel, sadly
    private static final Map<String, Integer> offline = new LinkedHashMap<>(); // Only contains name

    public void registerPlayer(final MapleCharacter chr) {
        if (!offline.containsKey(chr.getName())) {
            offline.put(chr.getName(), chr.getClan());
        }
        if (!online.containsKey(chr)) {
            online.put(chr, chr.getClan());
        }
    }

    public void playerOnline(final MapleCharacter chr) {
        online.put(chr, chr.getClan());
    }

    public void deregisterPlayer(final MapleCharacter chr) {
        online.remove(chr);
    }

    public static int countOfflineByClan(final int clan) {
        int size = 0;
        for (final String name : offline.keySet()) {
            if (offline.get(name) == clan) size++;
        }
        return size;
    }

    public int countOnlineByClan(final int clan) {
        int size = 0;
        for (final MapleCharacter chr : online.keySet()) {
            if (online.get(chr) == clan) size++;
        }
        return size;
    }

    public List<MapleCharacter> getAllOnlinePlayersFromClan(final int clan) {
        final List<MapleCharacter> players = new ArrayList<>();
        for (final MapleCharacter player : online.keySet()) {
            if (online.get(player) == clan) players.add(player);
        }
        return players;
    }

    public List<String> getAllOfflinePlayersFromClan(final int clan) {
        final List<String> players = new ArrayList<>();
        for (final String name : offline.keySet()) {
            if (offline.get(name) == clan) players.add(name);
        }
        return players;
    }

    public static void loadAllClans() {
        final Connection con = DatabaseConnection.getConnection();
        try {
            final PreparedStatement ps = con.prepareStatement("SELECT name, clan FROM characters WHERE clan >= 0");
            final ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                offline.put(rs.getString("name"), rs.getInt("clan"));
            }
            rs.close();
            ps.close();
        } catch (final SQLException se) {
            se.printStackTrace();
        }
    }
}
