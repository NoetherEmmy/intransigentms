package net.sf.odinms.net.login;

import net.sf.odinms.net.login.remote.LoginWorldInterface;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class LoginWorldInterfaceImpl extends UnicastRemoteObject implements LoginWorldInterface {
    private static final long serialVersionUID = -3405666366539470037L;

    public LoginWorldInterfaceImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public void channelOnline(final int channel, final String ip) throws RemoteException {
        LoginServer.getInstance().addChannel(channel, ip);
    }

    public void channelOffline(final int channel) throws RemoteException {
        LoginServer.getInstance().removeChannel(channel);
    }

    public void shutdown() throws RemoteException {
        LoginServer.getInstance().shutdown();
    }

    public boolean isAvailable() throws RemoteException {
        return true;
    }

    public double getPossibleLoginAverage() throws RemoteException {
        return LoginWorker.getInstance().getPossibleLoginAverage();
    }

    public int getWaitingUsers() throws RemoteException {
        return LoginWorker.getInstance().getWaitingUsers();
    }
}
