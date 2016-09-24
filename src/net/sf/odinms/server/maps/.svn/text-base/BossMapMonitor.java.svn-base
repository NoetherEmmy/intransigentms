package net.sf.odinms.server.maps;

import net.sf.odinms.server.MaplePortal;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.server.TimerManager;

public class BossMapMonitor {

    private MaplePortal portal;
    private MapleMap map;
    private MapleMap pMap;
    private ScheduledFuture<?> schedule;

    public BossMapMonitor(final MapleMap map, MapleMap pMap, MaplePortal portal) {
        this.map = map;
        this.pMap = pMap;
        this.portal = portal;
        schedule = TimerManager.getInstance().register(new Runnable() {

            public void run() {
                if (map.playerCount() <= 0) {
                    BossMapMonitor.this.run();
                }
            }
        }, 10000);
    }

    private void run() {
        map.killAllMonsters(false);
        map.resetReactors();
        pMap.resetReactors();
        portal.setPortalState(MapleMapPortal.OPEN);
        if (map.mobCount() == 0 && map.playerCount() == 0) {
            schedule.cancel(false);
        } else {
            System.out.println("BossMapMonitor is having errors.");
        }
    }
}