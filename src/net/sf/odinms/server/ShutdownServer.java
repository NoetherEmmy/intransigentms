package net.sf.odinms.server;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;

import java.rmi.RemoteException;
import java.sql.SQLException;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class ShutdownServer implements Runnable {
    //private static final Logger log = LoggerFactory.getLogger(ShutdownServer.class);

    private final int myChannel;

    public ShutdownServer(final int channel) {
        myChannel = channel;
    }

    @Override
    public void run() {
        //DeathBot.getInstance().dispose();
        try {
            ChannelServer.getInstance(myChannel).shutdown();
        } catch (final Throwable t) {
            System.err.println("SHUTDOWN ERROR");
            t.printStackTrace();
        }
        int c = 200;
        while (ChannelServer.getInstance(myChannel).getConnectedClients() > 0 && c > 0) {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                System.err.println("ERROR");
                e.printStackTrace();
            }
            c--;
        }
        try {
            ChannelServer.getWorldRegistry().deregisterChannelServer(myChannel);
        } catch (final RemoteException e) {
            // We are shutting down.
        }
        try {
            ChannelServer.getInstance(myChannel).unbind();
        } catch (final Throwable t) {
            System.err.println("SHUTDOWN ERROR");
            t.printStackTrace();
        }

        boolean allShutdownFinished = true;
        for (final ChannelServer cserv : ChannelServer.getAllInstances()){
            if (!cserv.hasFinishedShutdown()) {
                allShutdownFinished = false;
            }
        }
        if (allShutdownFinished) {
            TimerManager.getInstance().stop();
            try {
                DatabaseConnection.closeAll();
            } catch (final SQLException e) {
                System.err.println("THROW");
                e.printStackTrace();
            }
            System.exit(0);
        }
    }
}
