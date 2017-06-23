package net.sf.odinms.server.maps;

import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.TimerManager;

import java.util.concurrent.ScheduledFuture;

public class BossMapMonitor {
    private final MaplePortal portal;
    private final MapleMap map, pMap;
    private final ScheduledFuture<?> schedule;

    public BossMapMonitor(final MapleMap map, final MapleMap pMap, final MaplePortal portal) {
        this.map = map;
        this.pMap = pMap;
        this.portal = portal;
        schedule = TimerManager.getInstance().register(() -> {
            if (map.playerCount() <= 0) run();
        }, 10L * 1000L);
    }

    private void run() {
        map.killAllMonsters(false);
        map.resetReactors();
        pMap.resetReactors();
        portal.setPortalState(MapleMapPortal.OPEN);
        if (map.mobCount() == 0 && map.playerCount() == 0) {
            schedule.cancel(false);
        } else {
            System.err.println(
                "ERROR: map.mobCount() == 0 && map.playerCount() == 0 in BossMapMonitor"
            );
        }
    }
}
