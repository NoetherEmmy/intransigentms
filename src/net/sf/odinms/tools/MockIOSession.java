package net.sf.odinms.tools;

import java.net.SocketAddress;
import net.sf.odinms.net.MaplePacket;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;

public class MockIOSession extends BaseIoSession {

    /**
     * Does nothing.
     */
    @Override
    protected void updateTrafficMask() {
    }

    /**
     * Does nothing.
     */
    @Override
    public IoSessionConfig getConfig() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public IoFilterChain getFilterChain() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public IoHandler getHandler() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public SocketAddress getLocalAddress() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public IoService getService() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public SocketAddress getServiceAddress() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public IoServiceConfig getServiceConfig() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public TransportType getTransportType() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public CloseFuture close() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    protected void close0() {
    }

    /**
     * Does nothing.
     */
    @Override
    public WriteFuture write(Object message, SocketAddress remoteAddress) {
        return null;
    }

    /**
     * "Fake writes" a packet to the client, only running the OnSend event of
     * the packet.
     */
    @Override
    public WriteFuture write(Object message) {
        if (message instanceof MaplePacket) {
            MaplePacket mp = (MaplePacket) message;
            if (mp.getOnSend() != null) {
                mp.getOnSend().run();
            }
        }
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    protected void write0(WriteRequest writeRequest) {
    }
}