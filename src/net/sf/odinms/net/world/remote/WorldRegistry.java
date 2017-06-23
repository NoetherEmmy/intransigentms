package net.sf.odinms.net.world.remote;

import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.login.remote.LoginWorldInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WorldRegistry extends Remote {
    WorldChannelInterface registerChannelServer(String authKey, ChannelWorldInterface cb) throws RemoteException;

    void deregisterChannelServer(int channel) throws RemoteException;

    WorldLoginInterface registerLoginServer(String authKey, LoginWorldInterface cb) throws RemoteException;

    void deregisterLoginServer(LoginWorldInterface cb) throws RemoteException;

    String getStatus() throws RemoteException;
}
