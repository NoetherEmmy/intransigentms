package net.sf.odinms.net.world.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.CharacterIdChannelPair;
import net.sf.odinms.net.world.MapleMessenger;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.PlayerBuffValueHolder;
import net.sf.odinms.net.world.PlayerCoolDownValueHolder;
import net.sf.odinms.net.world.guild.MapleAlliance;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;

public interface WorldChannelInterface extends Remote, WorldChannelCommonOperations {

    Properties getDatabaseProperties() throws RemoteException;

    Properties getGameProperties() throws RemoteException;

    void serverReady() throws RemoteException;

    String getIP(int channel) throws RemoteException;

    int find(String charName) throws RemoteException;

    int find(int characterId) throws RemoteException;

    Map<Integer, Integer> getConnected() throws RemoteException;

    MapleParty createParty(MaplePartyCharacter chrfor) throws RemoteException;

    MapleParty getParty(int partyid) throws RemoteException;

    void updateParty(int partyid, PartyOperation operation, MaplePartyCharacter target) throws RemoteException;

    void partyChat(int partyid, String chattext, String namefrom) throws RemoteException;

    boolean isAvailable() throws RemoteException;

    ChannelWorldInterface getChannelInterface(int channel) throws RemoteException;

    WorldLocation getLocation(String name) throws RemoteException;

    CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException;

    MapleGuild getGuild(int id, MapleGuildCharacter mgc) throws RemoteException;

    void clearGuilds() throws RemoteException;

    void setGuildMemberOnline(MapleGuildCharacter mgc, boolean bOnline, int channel) throws RemoteException;

    int addGuildMember(MapleGuildCharacter mgc) throws RemoteException;

    void leaveGuild(MapleGuildCharacter mgc) throws RemoteException;

    void guildChat(int gid, String name, int cid, String msg) throws RemoteException;

    void changeRank(int gid, int cid, int newRank) throws RemoteException;

    void expelMember(MapleGuildCharacter initiator, String name, int cid) throws RemoteException;

    void setGuildNotice(int gid, String notice) throws RemoteException;

    void memberLevelJobUpdate(MapleGuildCharacter mgc) throws RemoteException;

    void changeRankTitle(int gid, String[] ranks) throws RemoteException;

    int createGuild(int leaderId, String name) throws RemoteException;

    void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) throws RemoteException;

    void disbandGuild(int gid) throws RemoteException;

    boolean increaseGuildCapacity(int gid) throws RemoteException;

    void gainGP(int gid, int amount) throws RemoteException;

    MapleMessenger createMessenger(MapleMessengerCharacter chrfor) throws RemoteException;

    MapleMessenger getMessenger(int messengerid) throws RemoteException;

    void leaveMessenger(int messengerid, MapleMessengerCharacter target) throws RemoteException;

    void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromchannel) throws RemoteException;

    void silentJoinMessenger(int messengerid, MapleMessengerCharacter target, int position) throws RemoteException;

    void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) throws RemoteException;

    void messengerChat(int messengerid, String chattext, String namefrom) throws RemoteException;

    void declineChat(String target, String namefrom) throws RemoteException;

    void updateMessenger(int messengerid, String namefrom, int fromchannel) throws RemoteException;

    void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) throws RemoteException;

    List<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) throws RemoteException;

    void addCooldownsToStorage(int chrid, List<PlayerCoolDownValueHolder> toStore) throws RemoteException;

    List<PlayerCoolDownValueHolder> getCooldownsFromStorage(int chrid) throws RemoteException;

    MapleAlliance getAlliance(int id) throws RemoteException;

    void addAlliance(int id, MapleAlliance addAlliance) throws RemoteException;

    void disbandAlliance(int id) throws RemoteException;

    void allianceMessage(int id, MaplePacket packet, int exception, int guildex) throws RemoteException;

    boolean setAllianceNotice(int aId, String notice) throws RemoteException;

    boolean setAllianceRanks(int aId, String[] ranks) throws RemoteException;

    boolean removeGuildFromAlliance(int aId, int guildId) throws RemoteException;

    boolean addGuildtoAlliance(int aId, int guildId) throws RemoteException;

    boolean setGuildAllianceId(int gId, int aId) throws RemoteException;

    boolean increaseAllianceCapacity(int aId, int inc) throws RemoteException;
}