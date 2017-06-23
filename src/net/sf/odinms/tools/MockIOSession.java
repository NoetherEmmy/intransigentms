package net.sf.odinms.tools;

import net.sf.odinms.net.MaplePacket;
import org.apache.mina.common.*;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;

import java.net.SocketAddress;

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
    public WriteFuture write(final Object message, final SocketAddress remoteAddress) {
        return null;
    }

    /**
     * "Fake writes" a packet to the client, only running the OnSend event of
     * the packet.
     */
    @Override
    public WriteFuture write(final Object message) {
        if (message instanceof MaplePacket) {
            final MaplePacket mp = (MaplePacket) message;
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
    protected void write0(final WriteRequest writeRequest) {
    }
}
