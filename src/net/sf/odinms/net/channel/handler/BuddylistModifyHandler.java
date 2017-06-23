package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.client.BuddyList.BuddyAddResult;
import net.sf.odinms.client.BuddyList.BuddyOperation;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static net.sf.odinms.client.BuddyList.BuddyOperation.ADDED;
import static net.sf.odinms.client.BuddyList.BuddyOperation.DELETED;

public class BuddylistModifyHandler extends AbstractMaplePacketHandler {
    //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BuddylistModifyHandler.class);

    private static class CharacterIdNameBuddyCapacity extends CharacterNameAndId {
        private final int buddyCapacity;

        public CharacterIdNameBuddyCapacity(final int id, final String name, final int buddyCapacity) {
            super(id, name);
            this.buddyCapacity = buddyCapacity;
        }

        public int getBuddyCapacity() {
            return buddyCapacity;
        }
    }

    private void nextPendingRequest(final MapleClient c) {
        final CharacterNameAndId pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.getSession()
                .write(
                    MaplePacketCreator.requestBuddylistAdd(
                        pendingBuddyRequest.getId(),
                        pendingBuddyRequest.getName()
                    )
                );
        }
    }

    private CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(final String name) throws SQLException {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps =
            con.prepareStatement(
                "SELECT id, name, buddyCapacity FROM characters WHERE name LIKE ?"
            );
        ps.setString(1, name);
        final ResultSet rs = ps.executeQuery();
        CharacterIdNameBuddyCapacity ret = null;
        if (rs.next()) {
            ret =
                new CharacterIdNameBuddyCapacity(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("buddyCapacity")
                );
        }
        rs.close();
        ps.close();
        return ret;
    }

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final int mode = slea.readByte();
        final MapleCharacter player = c.getPlayer();
        final WorldChannelInterface worldInterface = c.getChannelServer().getWorldInterface();
        final BuddyList buddylist = player.getBuddylist();
        if (mode == 1) { // Add.
            final String addName = slea.readMapleAsciiString();
            final BuddylistEntry ble = buddylist.get(addName);
            if (ble != null && !ble.isVisible()) {
                c.getSession().write(MaplePacketCreator.buddylistMessage((byte) 13));
            } else if (buddylist.isFull()) {
                c.getSession().write(MaplePacketCreator.buddylistMessage((byte) 11));
            } else {
                try {
                    final CharacterIdNameBuddyCapacity charWithId;
                    final int channel;
                    final MapleCharacter otherChar =
                        c.getChannelServer().getPlayerStorage().getCharacterByName(addName);
                    if (otherChar != null) {
                        channel = c.getChannel();
                        charWithId =
                            new CharacterIdNameBuddyCapacity(
                                otherChar.getId(),
                                otherChar.getName(),
                                otherChar.getBuddylist().getCapacity()
                            );
                    } else {
                        channel = worldInterface.find(addName);
                        charWithId = getCharacterIdAndNameFromDatabase(addName);
                    }

                    if (charWithId != null) {
                        BuddyAddResult buddyAddResult = null;
                        if (channel != -1) {
                            final ChannelWorldInterface channelInterface =
                                worldInterface.getChannelInterface(channel);
                            buddyAddResult =
                                channelInterface.requestBuddyAdd(
                                    addName,
                                    c.getChannel(),
                                    player.getId(),
                                    player.getName()
                                );
                        } else {
                            final Connection con = DatabaseConnection.getConnection();
                            PreparedStatement ps =
                                con.prepareStatement(
                                    "SELECT COUNT(*) as buddyCount FROM buddies " +
                                        "WHERE characterid = ? AND pending = 0"
                                );
                            ps.setInt(1, charWithId.getId());
                            ResultSet rs = ps.executeQuery();
                            if (!rs.next()) {
                                throw new RuntimeException("Result set expected");
                            } else {
                                final int count = rs.getInt("buddyCount");
                                if (count >= charWithId.getBuddyCapacity()) {
                                    buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
                                }
                            }
                            rs.close();
                            ps.close();
                            ps = con.prepareStatement(
                                "SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?"
                            );
                            ps.setInt(1, charWithId.getId());
                            ps.setInt(2, player.getId());
                            rs = ps.executeQuery();
                            if (rs.next()) {
                                buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
                            }
                            rs.close();
                            ps.close();
                        }
                        if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
                            c.getSession().write(MaplePacketCreator.buddylistMessage((byte) 12));
                        } else {
                            int displayChannel = -1;
                            final int otherCid = charWithId.getId();
                            if (buddyAddResult == BuddyAddResult.ALREADY_ON_LIST && channel != -1) {
                                displayChannel = channel;
                                notifyRemoteChannel(c, channel, otherCid, ADDED);
                            } else if (buddyAddResult != BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
                                final Connection con = DatabaseConnection.getConnection();
                                final PreparedStatement ps =
                                    con.prepareStatement(
                                        "INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 1)"
                                    );
                                ps.setInt(1, charWithId.getId());
                                ps.setInt(2, player.getId());
                                ps.executeUpdate();
                                ps.close();
                            }
                            buddylist.put(new BuddylistEntry(charWithId.getName(), otherCid, displayChannel, true));
                            c.getSession().write(MaplePacketCreator.updateBuddylist(buddylist.getBuddies()));
                        }
                    } else {
                        c.getSession().write(MaplePacketCreator.buddylistMessage((byte) 15));
                    }
                } catch (RemoteException | SQLException e) {
                    System.err.println("Exception in BuddylistModifyHandler#handlePacket");
                    e.printStackTrace();
                }
            }
        } else if (mode == 2) { // Accept buddy
            final int otherCid = slea.readInt();
            if (!buddylist.isFull()) {
                try {
                    final int channel = worldInterface.find(otherCid);
                    String otherName = null;
                    final MapleCharacter otherChar =
                        c.getChannelServer().getPlayerStorage().getCharacterById(otherCid);
                    if (otherChar == null) {
                        final Connection con = DatabaseConnection.getConnection();
                        final PreparedStatement ps =
                            con.prepareStatement(
                                "SELECT name FROM characters WHERE id = ?"
                            );
                        ps.setInt(1, otherCid);
                        final ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            otherName = rs.getString("name");
                        }
                        rs.close();
                        ps.close();
                    } else {
                        otherName = otherChar.getName();
                    }
                    if (otherName != null) {
                        buddylist.put(new BuddylistEntry(otherName, otherCid, channel, true));
                        c.getSession().write(MaplePacketCreator.updateBuddylist(buddylist.getBuddies()));
                        notifyRemoteChannel(c, channel, otherCid, ADDED);
                    }
                } catch (final RemoteException e) {
                    System.err.println("REMOTE THROW");
                    e.printStackTrace();
                } catch (final SQLException e) {
                    System.err.println("SQL THROW");
                    e.printStackTrace();
                }
            }
            nextPendingRequest(c);
        } else if (mode == 3) { // Delete.
            final int otherCid = slea.readInt();
            if (buddylist.containsVisible(otherCid)) {
                try {
                    notifyRemoteChannel(c, worldInterface.find(otherCid), otherCid, DELETED);
                } catch (final RemoteException e) {
                    System.err.println("REMOTE THROW");
                    e.printStackTrace();
                }
            }
            buddylist.remove(otherCid);
            c.getSession().write(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
            nextPendingRequest(c);
        }
    }

    private void notifyRemoteChannel(final MapleClient c,
                                     final int remoteChannel,
                                     final int otherCid,
                                     final BuddyOperation operation) throws RemoteException {
        final WorldChannelInterface worldInterface = c.getChannelServer().getWorldInterface();
        final MapleCharacter player = c.getPlayer();
        if (remoteChannel != -1) {
            final ChannelWorldInterface channelInterface =
                worldInterface.getChannelInterface(remoteChannel);
            channelInterface.buddyChanged(
                otherCid,
                player.getId(),
                player.getName(),
                c.getChannel(),
                operation
            );
        }
    }
}
