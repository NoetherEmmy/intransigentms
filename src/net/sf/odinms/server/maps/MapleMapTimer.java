package net.sf.odinms.server.maps;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.MaplePacketCreator;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

public class MapleMapTimer {
    private final Calendar predictedStopTime;
    private final int mapToWarpTo;
    private final int minLevelToWarp, maxLevelToWarp;
    private final ScheduledFuture<?> sf0F;

    public MapleMapTimer(ScheduledFuture<?> sfO, int newDuration, int mapToWarpToP, int minLevelToWarpP, int maxLevelToWarpP) {
        Calendar startTime = Calendar.getInstance();
        predictedStopTime = Calendar.getInstance();
        predictedStopTime.add(Calendar.SECOND, newDuration);
        mapToWarpTo = mapToWarpToP;
        minLevelToWarp = minLevelToWarpP;
        maxLevelToWarp = maxLevelToWarpP;
        sf0F = sfO;
    }

    public MaplePacket makeSpawnData() {
        final int timeLeft;
        final long stopTimeStamp = predictedStopTime.getTimeInMillis();
        final long currentTimeStamp = Calendar.getInstance().getTimeInMillis();
        timeLeft = (int) (stopTimeStamp - currentTimeStamp) / 1000;
        return MaplePacketCreator.getClock(timeLeft);
    }

    public void sendSpawnData(MapleClient c) {
        c.getSession().write(makeSpawnData());
    }

    public ScheduledFuture<?> getSF0F() {
        return sf0F;
    }

    public int warpToMap() {
        return mapToWarpTo;
    }

    public int minLevelToWarp() {
        return minLevelToWarp;
    }

    public int maxLevelToWarp() {
        return maxLevelToWarp;
    }

    public int getTimeLeft() {
        final long stopTimeStamp = predictedStopTime.getTimeInMillis();
        final long currentTimeStamp = Calendar.getInstance().getTimeInMillis();
        return (int) (stopTimeStamp - currentTimeStamp) / 1000;
    }
}
