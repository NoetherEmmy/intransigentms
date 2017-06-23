package net.sf.odinms.server;

public interface TimerManagerMBean {
    boolean isTerminated();

    boolean isShutdown();

    long getCompletedTaskCount();

    long getActiveCount();

    long getTaskCount();

    int getQueuedTasks();
}
