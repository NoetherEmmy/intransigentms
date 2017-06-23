package net.sf.odinms.net.world.remote;

import java.rmi.RemoteException;
import java.util.List;

public interface WorldChannelCommonOperations {
    boolean isConnected(String charName) throws RemoteException;

    void broadcastMessage(String sender, byte[] message) throws RemoteException;

    void whisper(String sender, String target, int channel, String message) throws RemoteException;

    void shutdown(int time) throws RemoteException;

    void broadcastSMega(String sender, byte[] message) throws RemoteException;

    void broadcastGMMessage(String sender, byte[] message) throws RemoteException;

    void loggedOn(String name, int characterId, int channel, int[] buddies) throws RemoteException;

    void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException;

    List<CheaterData> getCheaters() throws RemoteException;

    void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException;

    void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException;

    void broadcastToClan(byte[] message, int clan) throws RemoteException;

    int onlineClanMembers(int clan) throws RemoteException;
}
