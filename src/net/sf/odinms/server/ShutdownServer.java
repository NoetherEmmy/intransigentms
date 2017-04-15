package net.sf.odinms.server;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.sql.SQLException;

public class ShutdownServer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ShutdownServer.class);

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
            log.error("SHUTDOWN ERROR", t);
        }
        int c = 200;
        while (ChannelServer.getInstance(myChannel).getConnectedClients() > 0 && c > 0) {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                log.error("ERROR", e);
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
            log.error("SHUTDOWN ERROR", t);
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
                log.error("THROW", e);
            }
            System.exit(0);
        }
    }
}
