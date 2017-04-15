package net.sf.odinms.server.maps;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.StringUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class MapleReactorFactory {
    //private static Logger log = LoggerFactory.getLogger(MapleReactorFactory.class);
    private static final MapleDataProvider data =
        MapleDataProviderFactory.getDataProvider(
            new File(
                System.getProperty("net.sf.odinms.wzpath") + "/Reactor.wz"
            )
        );
    private static final Map<Integer, MapleReactorStats> reactorStats = new HashMap<>();

    public static MapleReactorStats getReactor(final int rid) {
        MapleReactorStats stats = reactorStats.get(rid);
        if (stats == null) {
            int infoId = rid;
            MapleData reactorData =
                data.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) + ".img", '0', 11));
            final MapleData link = reactorData.getChildByPath("info/link");
            if (link != null) {
                infoId = MapleDataTool.getIntConvert("info/link", reactorData);
                stats = reactorStats.get(infoId);
            }
            if (stats == null) {
                reactorData =
                    data.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) + ".img", '0', 11));
                MapleData reactorInfoData = reactorData.getChildByPath("0/event/0");
                stats = new MapleReactorStats();

                if (reactorInfoData != null) {
                    boolean areaSet = false;
                    int i = 0;
                    while (reactorInfoData != null) {
                        Pair<Integer, Integer> reactItem = null;
                        final int type = MapleDataTool.getIntConvert("type", reactorInfoData);
                        if (type == 100 || type == 99) { // Reactor waits for item or is an obstacle
                            reactItem =
                                new Pair<>(
                                    MapleDataTool.getIntConvert("0", reactorInfoData),
                                    MapleDataTool.getIntConvert("1", reactorInfoData)
                                );
                            if (!areaSet) { // Only set area of effect for item-triggered reactors once
                                stats.setTL(MapleDataTool.getPoint("lt", reactorInfoData));
                                stats.setBR(MapleDataTool.getPoint("rb", reactorInfoData));
                                areaSet = true;
                            }
                        }
                        final byte nextState = (byte) MapleDataTool.getIntConvert("state", reactorInfoData);
                        stats.addState((byte) i, type, reactItem, nextState);
                        i++;
                        reactorInfoData = reactorData.getChildByPath(i + "/event/0");
                    }
                } else {
                    stats.addState((byte) 0, 999, null, (byte) 0);
                }

                reactorStats.put(infoId, stats);
                if (rid != infoId) {
                    reactorStats.put(rid, stats);
                }
            } else { // Stats exist at infoId but not rid; add to map
                reactorStats.put(rid, stats);
            }
        }
        return stats;
    }
}
