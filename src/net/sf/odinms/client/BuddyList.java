package net.sf.odinms.client;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BuddyList {
    public enum BuddyOperation {
        ADDED, DELETED
    }

    public enum BuddyAddResult {
        BUDDYLIST_FULL, ALREADY_ON_LIST, OK
    }

    private final Map<Integer, BuddylistEntry> buddies = new LinkedHashMap<>();
    private int capacity;
    private final Deque<CharacterNameAndId> pendingRequests = new LinkedList<>();

    public BuddyList(int capacity) {
        super();
        this.capacity = capacity;
    }

    public boolean contains(int characterId) {
        return buddies.containsKey(characterId);
    }

    public boolean containsVisible(int characterId) {
        BuddylistEntry ble = buddies.get(characterId);
        return ble != null && ble.isVisible();
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void addCapacity(int capacity) {
        this.capacity += capacity;
    }

    public BuddylistEntry get(int characterId) {
        return buddies.get(characterId);
    }

    public BuddylistEntry get(String characterName) {
        String lowerCaseName = characterName.toLowerCase();
        for (BuddylistEntry ble : buddies.values()) {
            if (ble.getName().toLowerCase().equals(lowerCaseName)) {
                return ble;
            }
        }
        return null;
    }

    public void put(BuddylistEntry entry) {
        buddies.put(entry.getCharacterId(), entry);
    }

    public void remove(int characterId) {
        buddies.remove(characterId);
    }

    public Collection<BuddylistEntry> getBuddies() {
        return buddies.values();
    }

    public boolean isFull() {
        return buddies.size() >= capacity;
    }

    public int[] getBuddyIds() {
        int[] buddyIds = new int[buddies.size()];
        int i = 0;
        for (BuddylistEntry ble : buddies.values()) {
            buddyIds[i++] = ble.getCharacterId();
        }
        return buddyIds;
    }

    public void loadFromDb(int characterId) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps =
            con.prepareStatement(
                "SELECT b.buddyid, b.pending, c.name as buddyname FROM " +
                    "buddies as b, characters as c WHERE " +
                    "c.id = b.buddyid AND b.characterid = ?"
            );
        ps.setInt(1, characterId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            if (rs.getInt("pending") == 1) {
                pendingRequests.push(
                    new CharacterNameAndId(rs.getInt("buddyid"), rs.getString("buddyname"))
                );
            } else {
                put(new BuddylistEntry(rs.getString("buddyname"), rs.getInt("buddyid"), -1, true));
            }
        }
        rs.close();
        ps.close();

        ps = con.prepareStatement("DELETE FROM buddies WHERE pending = 1 AND characterid = ?");
        ps.setInt(1, characterId);
        ps.executeUpdate();
        ps.close();
    }

    public CharacterNameAndId pollPendingRequest() {
        return pendingRequests.pollLast();
    }

    public void addBuddyRequest(MapleClient c, int cidFrom, String nameFrom, int channelFrom) {
        put(new BuddylistEntry(nameFrom, cidFrom, channelFrom, false));
        if (pendingRequests.isEmpty()) {
            c.getSession().write(MaplePacketCreator.requestBuddylistAdd(cidFrom, nameFrom));
        } else {
            pendingRequests.push(new CharacterNameAndId(cidFrom, nameFrom));
        }
    }
}
