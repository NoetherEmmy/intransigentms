package net.sf.odinms.net.channel.remote;

import net.sf.odinms.client.BuddyList.BuddyAddResult;
import net.sf.odinms.client.BuddyList.BuddyOperation;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.world.MapleMessenger;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.guild.MapleGuildSummary;
import net.sf.odinms.net.world.remote.WorldChannelCommonOperations;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChannelWorldInterface extends Remote, WorldChannelCommonOperations {
    void setChannelId(int id) throws RemoteException;

    int getChannelId() throws RemoteException;

    String getIP() throws RemoteException;

    boolean isConnected(int characterId) throws RemoteException;

    int getConnected() throws RemoteException;

    int getLocation(String name) throws RemoteException;

    void updateParty(MapleParty party, PartyOperation operation, MaplePartyCharacter target) throws RemoteException;

    void partyChat(MapleParty party, String chattext, String namefrom) throws RemoteException;

    boolean isAvailable() throws RemoteException;

    BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom) throws RemoteException;

    void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation op) throws RemoteException;

    int[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException;

    MapleCharacter getPlayer(String name) throws RemoteException;

    void sendPacket(List<Integer> targetIds, MaplePacket packet, int exception) throws RemoteException;

    void setGuildAndRank(int cid, int guildid, int rank) throws RemoteException;

    void setOfflineGuildStatus(int guildid, byte guildrank, int cid) throws RemoteException;

    void setGuildAndRank(List<Integer> cids, int guildid, int rank, int exception) throws RemoteException;

    void reloadGuildCharacters() throws RemoteException;

    void changeEmblem(int gid, List<Integer> affectedPlayers, MapleGuildSummary mgs) throws RemoteException;

    void addMessengerPlayer(MapleMessenger messenger, String namefrom, int fromchannel, int position) throws RemoteException;

    void removeMessengerPlayer(MapleMessenger messenger, int position) throws RemoteException;

    void messengerChat(MapleMessenger messenger, String chattext, String namefrom) throws RemoteException;

    void declineChat(String target, String namefrom) throws RemoteException;

    void updateMessenger(MapleMessenger messenger, String namefrom, int position, int fromchannel) throws RemoteException;
}
