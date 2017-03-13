package net.sf.odinms.net.login.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface LoginWorldInterface extends Remote {
    void channelOnline(int channel, String ip) throws RemoteException;

    void channelOffline(int channel) throws RemoteException;

    void shutdown() throws RemoteException;

    boolean isAvailable() throws RemoteException;

    double getPossibleLoginAverage() throws RemoteException;

    int getWaitingUsers() throws RemoteException;
}
