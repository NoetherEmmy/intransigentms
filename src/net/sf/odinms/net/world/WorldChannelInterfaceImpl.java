package net.sf.odinms.net.world;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.login.remote.LoginWorldInterface;
import net.sf.odinms.net.world.guild.MapleAlliance;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.remote.CheaterData;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.net.world.remote.WorldLocation;
import net.sf.odinms.tools.CollectionUtil;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

//import net.sf.odinms.server.DeathBot;

public class WorldChannelInterfaceImpl extends UnicastRemoteObject implements WorldChannelInterface {
    private static final long serialVersionUID = -5568606556235590482L;
    private ChannelWorldInterface cb;
    private int dbId;
    private boolean ready = false;

    public WorldChannelInterfaceImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public WorldChannelInterfaceImpl(final ChannelWorldInterface cb, final int dbId) throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        this.cb = cb;
        this.dbId = dbId;
    }

    @Override
    public Properties getDatabaseProperties() throws RemoteException {
        return WorldServer.getInstance().getDbProp();
    }

    @Override
    public Properties getGameProperties() throws RemoteException {
        final Properties ret = new Properties(WorldServer.getInstance().getWorldProp());
        try {
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps = con.prepareStatement("SELECT * FROM channelconfig WHERE channelid = ?");
            ps.setInt(1, dbId);
            final ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ret.setProperty(rs.getString("name"), rs.getString("value"));
            }
            rs.close();
            ps.close();
        } catch (final SQLException sqle) {
            System.err.println("Could not retrieve channel configuration:");
            sqle.printStackTrace();
        }
        return ret;
    }

    @Override
    public void serverReady() throws RemoteException {
        ready = true;
        for (final LoginWorldInterface wli : WorldRegistryImpl.getInstance().getLoginServer()) {
            try {
                wli.channelOnline(cb.getChannelId(), cb.getIP());
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterLoginServer(wli);
            }
        }
        System.out.println("Channel " + cb.getChannelId() + " is online.");
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public String getIP(final int channel) throws RemoteException {
        final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(channel);
        if (cwi == null) {
            return "0.0.0.0:0";
        } else {
            try {
                return cwi.getIP();
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(channel);
                return "0.0.0.0:0";
            }
        }
    }

    @Override
    public void whisper(final String sender, final String target, final int channel, final String message) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.whisper(sender, target, channel, message);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public boolean isConnected(final String charName) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                if (cwi.isConnected(charName)) {
                    return true;
                }
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
        return false;
    }

    @Override
    public void broadcastMessage(final String sender, final byte[] message) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.broadcastMessage(sender, message);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public int find(final String charName) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                if (cwi.isConnected(charName)) {
                    return cwi.getChannelId();
                }
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
        return -1;
    }

    @Override
    public int find(final int characterId) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                if (cwi.isConnected(characterId)) {
                    return cwi.getChannelId();
                }
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
        return -1;
    }

    @Override
    public void shutdown(final int time) throws RemoteException {
        //DeathBot.getInstance().dispose();
        for (final LoginWorldInterface lwi : WorldRegistryImpl.getInstance().getLoginServer()) {
            try {
                lwi.shutdown();
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterLoginServer(lwi);
            }
        }
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.shutdown(time);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public Map<Integer, Integer> getConnected() throws RemoteException {
        final Map<Integer, Integer> ret = new LinkedHashMap<>();
        int total = 0;
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                final int curConnected = cwi.getConnected();
                ret.put(i, curConnected);
                total += curConnected;
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
        ret.put(0, total);
        return ret;
    }

    @Override
    public void loggedOn(final String name, final int characterId, final int channel, final int[] buddies) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.loggedOn(name, characterId, channel, buddies);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void loggedOff(final String name, final int characterId, final int channel, final int[] buddies) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.loggedOff(name, characterId, channel, buddies);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void updateParty(final int partyid,
                            final PartyOperation operation,
                            final MaplePartyCharacter target) throws RemoteException {
        final MapleParty party = WorldRegistryImpl.getInstance().getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        switch (operation) {
            case JOIN:
                party.addMember(target);
                break;
            case EXPEL:
            case LEAVE:
                party.removeMember(target);
                break;
            case DISBAND:
                WorldRegistryImpl.getInstance().disbandParty(partyid);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                party.updateMember(target);
                break;
            case CHANGE_LEADER:
                party.setLeader(target);
                break;
            default:
                throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
        }
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.updateParty(party, operation, target);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public MapleParty createParty(final MaplePartyCharacter chrfor) throws RemoteException {
        return WorldRegistryImpl.getInstance().createParty(chrfor);
    }

    @Override
    public MapleParty getParty(final int partyid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getParty(partyid);
    }

    @Override
    public void partyChat(final int partyid, final String chattext, final String namefrom) throws RemoteException {
        final MapleParty party = WorldRegistryImpl.getInstance().getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.partyChat(party, chattext, namefrom);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public boolean isAvailable() throws RemoteException {
        return true;
    }

    @Override
    public WorldLocation getLocation(final String charName) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                if (cwi.isConnected(charName)) {
                    return new WorldLocation(cwi.getLocation(charName), cwi.getChannelId());
                }
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
        return null;
    }

    @Override
    public List<CheaterData> getCheaters() throws RemoteException {
        final List<CheaterData> allCheaters = new ArrayList<>();
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                allCheaters.addAll(cwi.getCheaters());
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 10);
    }

    @Override
    public ChannelWorldInterface getChannelInterface(final int channel) {
        return WorldRegistryImpl.getInstance().getChannel(channel);
    }

    @Override
    public void buddyChat(final int[] recipientCharacterIds,
                          final int cidFrom,
                          final String nameFrom,
                          final String chattext) throws RemoteException {
        for (final ChannelWorldInterface cwi : WorldRegistryImpl.getInstance().getAllChannelServers()) {
            cwi.buddyChat(recipientCharacterIds, cidFrom, nameFrom, chattext);
        }
    }

    @Override
    public CharacterIdChannelPair[] multiBuddyFind(final int charIdFrom, final int[] characterIds) throws RemoteException {
        final List<CharacterIdChannelPair> foundsChars = new ArrayList<>(characterIds.length);
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            for (final int charid : cwi.multiBuddyFind(charIdFrom, characterIds)) {
                foundsChars.add(new CharacterIdChannelPair(charid, i));
            }
        }
        return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
    }

    @Override
    public MapleGuild getGuild(final int id, final MapleGuildCharacter mgc) throws RemoteException {
        return WorldRegistryImpl.getInstance().getGuild(id, mgc);
    }

    @Override
    public void clearGuilds() throws RemoteException {
        WorldRegistryImpl.getInstance().clearGuilds();
    }

    @Override
    public void setGuildMemberOnline(final MapleGuildCharacter mgc,
                                     final boolean bOnline, final int channel) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildMemberOnline(mgc, bOnline, channel);
    }

    @Override
    public int addGuildMember(final MapleGuildCharacter mgc) throws RemoteException {
        return WorldRegistryImpl.getInstance().addGuildMember(mgc);
    }

    @Override
    public void guildChat(final int gid, final String name, final int cid, final String msg) throws RemoteException {
        WorldRegistryImpl.getInstance().guildChat(gid, name, cid, msg);
    }

    @Override
    public void leaveGuild(final MapleGuildCharacter mgc) throws RemoteException {
        WorldRegistryImpl.getInstance().leaveGuild(mgc);
    }

    @Override
    public void changeRank(final int gid, final int cid, final int newRank) throws RemoteException {
        WorldRegistryImpl.getInstance().changeRank(gid, cid, newRank);
    }

    @Override
    public void expelMember(final MapleGuildCharacter initiator, final String name, final int cid) throws RemoteException {
        WorldRegistryImpl.getInstance().expelMember(initiator, name, cid);
    }

    @Override
    public void setGuildNotice(final int gid, final String notice) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildNotice(gid, notice);
    }

    @Override
    public void memberLevelJobUpdate(final MapleGuildCharacter mgc) throws RemoteException {
        WorldRegistryImpl.getInstance().memberLevelJobUpdate(mgc);
    }

    @Override
    public void changeRankTitle(final int gid, final String[] ranks) throws RemoteException {
        WorldRegistryImpl.getInstance().changeRankTitle(gid, ranks);
    }

    @Override
    public int createGuild(final int leaderId, final String name) throws RemoteException {
        return WorldRegistryImpl.getInstance().createGuild(leaderId, name);
    }

    @Override
    public void setGuildEmblem(final int gid, final short bg, final byte bgcolor, final short logo, final byte logocolor) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildEmblem(gid, bg, bgcolor, logo, logocolor);
    }

    @Override
    public void disbandGuild(final int gid) throws RemoteException {
        WorldRegistryImpl.getInstance().disbandGuild(gid);
    }

    @Override
    public boolean increaseGuildCapacity(final int gid) throws RemoteException {
        return WorldRegistryImpl.getInstance().increaseGuildCapacity(gid);
    }

    @Override
    public void gainGP(final int gid, final int amount) throws RemoteException {
        WorldRegistryImpl.getInstance().gainGP(gid, amount);
    }

    @Override
    public MapleMessenger createMessenger(final MapleMessengerCharacter chrfor) throws RemoteException {
        return WorldRegistryImpl.getInstance().createMessenger(chrfor);
    }

    @Override
    public MapleMessenger getMessenger(final int messengerid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getMessenger(messengerid);
    }

    @Override
    public void messengerInvite(final String sender,
                                final int messengerid,
                                final String target,
                                final int fromchannel) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.messengerInvite(sender, messengerid, target, fromchannel);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void leaveMessenger(final int messengerid, final MapleMessengerCharacter target) throws RemoteException {
        final MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        final int position = messenger.getPositionByName(target.getName());
        messenger.removeMember(target);

        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.removeMessengerPlayer(messenger, position);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void joinMessenger(final int messengerid,
                              final MapleMessengerCharacter target,
                              final String from,
                              final int fromchannel) throws RemoteException {
        final MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target);

        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.addMessengerPlayer(messenger, from, fromchannel, target.getPosition());
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void messengerChat(final int messengerid, final String chattext, final String namefrom) throws RemoteException {
        final MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.messengerChat(messenger, chattext, namefrom);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void declineChat(final String target, final String namefrom) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.declineChat(target, namefrom);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void updateMessenger(final int messengerid, final String namefrom, final int fromchannel) throws RemoteException {
        final MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        final int position = messenger.getPositionByName(namefrom);

        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.updateMessenger(messenger, namefrom, position, fromchannel);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void silentLeaveMessenger(final int messengerid, final MapleMessengerCharacter target) throws RemoteException {
        final MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentRemoveMember(target);
    }

    @Override
    public void silentJoinMessenger(final int messengerid,
                                    final MapleMessengerCharacter target,
                                    final int position) throws RemoteException {
        final MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentAddMember(target, position);
    }

    @Override
    public void addBuffsToStorage(final int chrid, final Set<PlayerBuffValueHolder> toStore) throws RemoteException {
        final PlayerBuffStorage buffStorage = WorldRegistryImpl.getInstance().getPlayerBuffStorage();
        buffStorage.addBuffsToStorage(chrid, toStore);
    }

    @Override
    public Set<PlayerBuffValueHolder> getBuffsFromStorage(final int chrid) throws RemoteException {
        final PlayerBuffStorage buffStorage = WorldRegistryImpl.getInstance().getPlayerBuffStorage();
        return buffStorage.getBuffsFromStorage(chrid);
    }

    @Override
    public void addCooldownsToStorage(final int chrid, final Set<PlayerCoolDownValueHolder> toStore) throws RemoteException {
        final PlayerBuffStorage buffStorage = WorldRegistryImpl.getInstance().getPlayerBuffStorage();
        buffStorage.addCooldownsToStorage(chrid, toStore);
    }

    @Override
    public Set<PlayerCoolDownValueHolder> getCooldownsFromStorage(final int chrid) throws RemoteException {
        final PlayerBuffStorage buffStorage = WorldRegistryImpl.getInstance().getPlayerBuffStorage();
        return buffStorage.getCooldownsFromStorage(chrid);
    }

    @Override
    public void broadcastGMMessage(final String sender, final byte[] message) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.broadcastGMMessage(sender, message);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void broadcastSMega(final String sender, final byte[] message) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.broadcastSMega(sender, message);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void broadcastToClan(final byte[] message, final int clan) throws RemoteException {
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.broadcastToClan(message, clan);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public int onlineClanMembers(final int clan) throws RemoteException {
        int size = 0;
        for (final int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                size += cwi.onlineClanMembers(clan);
            } catch (final RemoteException re) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
        return size;
    }

    @Override
    public MapleAlliance getAlliance(final int id) throws RemoteException {
        return WorldRegistryImpl.getInstance().getAlliance(id);
    }

    @Override
    public void addAlliance(final int id, final MapleAlliance alliance) throws RemoteException {
        WorldRegistryImpl.getInstance().addAlliance(id, alliance);
    }

    @Override
    public void disbandAlliance(final int id) throws RemoteException {
        WorldRegistryImpl.getInstance().disbandAlliance(id);
    }

    @Override
    public void allianceMessage(final int id, final MaplePacket packet, final int exception, final int guildex) throws RemoteException {
        WorldRegistryImpl.getInstance().allianceMessage(id, packet, exception, guildex);
    }

    @Override
    public boolean setAllianceNotice(final int aId, final String notice) throws RemoteException {
        return WorldRegistryImpl.getInstance().setAllianceNotice(aId, notice);
    }

    @Override
    public boolean setAllianceRanks(final int aId, final String[] ranks) throws RemoteException {
        return WorldRegistryImpl.getInstance().setAllianceRanks(aId, ranks);
    }

    @Override
    public boolean removeGuildFromAlliance(final int aId, final int guildId) throws RemoteException {
        return WorldRegistryImpl.getInstance().removeGuildFromAlliance(aId, guildId);
    }

    @Override
    public boolean addGuildtoAlliance(final int aId, final int guildId) throws RemoteException {
        return WorldRegistryImpl.getInstance().addGuildtoAlliance(aId, guildId);
    }

    @Override
    public boolean setGuildAllianceId(final int gId, final int aId) throws RemoteException {
        return WorldRegistryImpl.getInstance().setGuildAllianceId(gId, aId);
    }

    @Override
    public boolean increaseAllianceCapacity(final int aId, final int inc) throws RemoteException {
        return WorldRegistryImpl.getInstance().increaseAllianceCapacity(aId, inc);
    }
}
