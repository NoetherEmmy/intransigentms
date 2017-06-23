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

    public BuddyList(final int capacity) {
        super();
        this.capacity = capacity;
    }

    public boolean contains(final int characterId) {
        return buddies.containsKey(characterId);
    }

    public boolean containsVisible(final int characterId) {
        final BuddylistEntry ble = buddies.get(characterId);
        return ble != null && ble.isVisible();
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(final int capacity) {
        this.capacity = capacity;
    }

    public void addCapacity(final int capacity) {
        this.capacity += capacity;
    }

    public BuddylistEntry get(final int characterId) {
        return buddies.get(characterId);
    }

    public BuddylistEntry get(final String characterName) {
        final String lowerCaseName = characterName.toLowerCase();
        return
            buddies
                .values()
                .stream()
                .filter(ble -> ble.getName().toLowerCase().equals(lowerCaseName))
                .findFirst()
                .orElse(null);
    }

    public void put(final BuddylistEntry entry) {
        buddies.put(entry.getCharacterId(), entry);
    }

    public void remove(final int characterId) {
        buddies.remove(characterId);
    }

    public Collection<BuddylistEntry> getBuddies() {
        return buddies.values();
    }

    public boolean isFull() {
        return buddies.size() >= capacity;
    }

    public int[] getBuddyIds() {
        final int[] buddyIds = new int[buddies.size()];
        int i = 0;
        for (final BuddylistEntry ble : buddies.values()) {
            buddyIds[i++] = ble.getCharacterId();
        }
        return buddyIds;
    }

    public void loadFromDb(final int characterId) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps =
            con.prepareStatement(
                "SELECT b.buddyid, b.pending, c.name as buddyname FROM " +
                    "buddies as b, characters as c WHERE " +
                    "c.id = b.buddyid AND b.characterid = ?"
            );
        ps.setInt(1, characterId);
        final ResultSet rs = ps.executeQuery();
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

    public void addBuddyRequest(final MapleClient c, final int cidFrom, final String nameFrom, final int channelFrom) {
        put(new BuddylistEntry(nameFrom, cidFrom, channelFrom, false));
        if (pendingRequests.isEmpty()) {
            c.getSession().write(MaplePacketCreator.requestBuddylistAdd(cidFrom, nameFrom));
        } else {
            pendingRequests.push(new CharacterNameAndId(cidFrom, nameFrom));
        }
    }
}
