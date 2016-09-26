package net.sf.odinms.net.world.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;

public interface WorldLoginInterface extends Remote {
    Properties getDatabaseProperties() throws RemoteException;
    Properties getWorldProperties() throws RemoteException;
    Map<Integer, Integer> getChannelLoad() throws RemoteException;
    boolean isAvailable() throws RemoteException;

    void deleteGuildCharacter(MapleGuildCharacter mgc) throws RemoteException;
}