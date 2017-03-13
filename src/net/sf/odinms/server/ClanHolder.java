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

    public void registerPlayer(MapleCharacter chr) {
        if (!offline.containsKey(chr.getName())) {
            offline.put(chr.getName(), chr.getClan());
        }
        if (!online.containsKey(chr)) {
            online.put(chr, chr.getClan());
        }
    }

    public void playerOnline(MapleCharacter chr) {
        online.put(chr, chr.getClan());
    }

    public void deregisterPlayer(MapleCharacter chr) {
        online.remove(chr);
    }

    public static int countOfflineByClan(int clan) {
        int size = 0;
        for (String name : offline.keySet()) {
            if (offline.get(name) == clan) size++;
        }
        return size;
    }

    public int countOnlineByClan(int clan) {
        int size = 0;
        for (MapleCharacter chr : online.keySet()) {
            if (online.get(chr) == clan) size++;
        }
        return size;
    }

    public List<MapleCharacter> getAllOnlinePlayersFromClan(int clan) {
        List<MapleCharacter> players = new ArrayList<>();
        for (MapleCharacter player : online.keySet()) {
            if (online.get(player) == clan) players.add(player);
        }
        return players;
    }

    public List<String> getAllOfflinePlayersFromClan(int clan) {
        List<String> players = new ArrayList<>();
        for (String name : offline.keySet()) {
            if (offline.get(name) == clan) players.add(name);
        }
        return players;
    }

    public static void loadAllClans() {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT name, clan FROM characters WHERE clan >= 0");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                offline.put(rs.getString("name"), rs.getInt("clan"));
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }
}
