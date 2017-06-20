package net.sf.odinms.server;

import net.sf.odinms.client.messages.MessageCallback;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class TimerManager implements TimerManagerMBean {
    //private static final Logger log = LoggerFactory.getLogger(TimerManager.class);
    private static final TimerManager instance = new TimerManager();
    private ScheduledThreadPoolExecutor ses;

    private TimerManager() {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            mBeanServer.registerMBean(this, new ObjectName("net.sf.odinms.server:type=TimerManger"));
        } catch (final Exception e) {
            System.err.println("Error registering MBean ");
            e.printStackTrace();
        }
    }

    public static TimerManager getInstance() {
        return instance;
    }

    public void start() {
        if (ses != null && !ses.isShutdown() && !ses.isTerminated()) {
            return; // Starting the same TimerManager twice is a no-op.
        }
        final ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(4, new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = new Thread(r);
                t.setName("Timermanager-Worker-" + threadNumber.getAndIncrement());
                return t;
            }
        });
        stpe.setMaximumPoolSize(4);
        stpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        ses = stpe;
    }

    public void stop() {
        ses.shutdown();
    }

    public ScheduledFuture<?> register(final Runnable r, final long repeatTime, final long delay) {
        return ses.scheduleAtFixedRate(new LoggingSaveRunnable(r), delay, repeatTime, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> register(final Runnable r, final long repeatTime) {
        return ses.scheduleAtFixedRate(new LoggingSaveRunnable(r), 0, repeatTime, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> schedule(final Runnable r, final long delay) {
        return ses.schedule(new LoggingSaveRunnable(r), delay, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> scheduleAtTimestamp(final Runnable r, final long timestamp) {
        return schedule(r, timestamp - System.currentTimeMillis());
    }

    public void dropDebugInfo(final MessageCallback callback) {
        StringBuilder builder = new StringBuilder();
        builder.append("Terminated: ");
        builder.append(ses.isTerminated());
        builder.append(" Shutdown: ");
        builder.append(ses.isShutdown());
        callback.dropMessage(builder.toString());

        builder = new StringBuilder();
        builder.append("Completed Tasks: ");
        builder.append(ses.getCompletedTaskCount());
        builder.append(" Active Tasks: ");
        builder.append(ses.getActiveCount());
        builder.append(" Task Count: ");
        builder.append(ses.getTaskCount());
        callback.dropMessage(builder.toString());

        builder = new StringBuilder();
        builder.append("Queued Tasks: ");
        builder.append(ses.getQueue().toArray().length);
        callback.dropMessage(builder.toString());
    }

    @Override
    public long getActiveCount() {
        return ses.getActiveCount();
    }

    @Override
    public long getCompletedTaskCount() {
        return ses.getCompletedTaskCount();
    }

    @Override
    public int getQueuedTasks() {
        return ses.getQueue().toArray().length;
    }

    @Override
    public long getTaskCount() {
        return ses.getTaskCount();
    }

    @Override
    public boolean isShutdown() {
        return ses.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return ses.isTerminated();
    }

    private static class LoggingSaveRunnable implements Runnable {
        final Runnable r;
        public LoggingSaveRunnable(final Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            try {
                r.run();
            } catch (final Throwable t) {
                System.err.println("ERROR ");
                t.printStackTrace();
            }
        }
    }
}
