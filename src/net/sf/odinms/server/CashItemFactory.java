package net.sf.odinms.server;

import net.sf.odinms.net.world.WorldServer;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.tools.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CashItemFactory {
    private static final Map<Integer, Integer> snLookup = new LinkedHashMap<>();
    private static final Map<Integer, CashItemInfo> itemStats = new LinkedHashMap<>();
    private static final MapleDataProvider data =
        MapleDataProviderFactory.getDataProvider(
            new File(System.getProperty(WorldServer.WZPATH) + "/Etc.wz")
        );
    private static final MapleData commodities =
        data.getData(
            StringUtil.getLeftPaddedStr("Commodity.img", '0', 11)
        );

    public static CashItemInfo getItem(final int sn) {
        if (itemStats.containsKey(sn)) return itemStats.get(sn);
        CashItemInfo stats = itemStats.get(sn);
        if (stats == null) {
            final int cid = getCommodityFromSN(sn);
            final int itemId = MapleDataTool.getIntConvert(cid + "/ItemId", commodities);
            final int count = MapleDataTool.getIntConvert(cid + "/Count", commodities, 1);
            final int price = MapleDataTool.getIntConvert(cid + "/Price", commodities, 0);
            stats = new CashItemInfo(itemId, count, price);
            itemStats.put(sn, stats);
        }
        return stats;
    }

    private static int getCommodityFromSN(final int sn) {
        if (snLookup.containsKey(sn)) return snLookup.get(sn);
        int curr = snLookup.size() - 1;
        int currSN = 0;
        if (curr == -1) {
            curr = 0;
            currSN = MapleDataTool.getIntConvert("0/SN", commodities);
            snLookup.put(currSN, curr);
        }
        for (int i = snLookup.size() - 1; currSN != sn; ++i) {
            curr = i;
            currSN = MapleDataTool.getIntConvert(curr + "/SN", commodities);
            snLookup.put(currSN, curr);
        }
        return curr;
    }

    public static List<Integer> getPackageItems(final int itemId) {
        final List<Integer> packageItems = new ArrayList<>();
        final MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File("wz/Etc.wz"));
        final MapleData a = dataProvider.getData("CashPackage.img");
        if (packageItems.contains(itemId)) return packageItems;
        for (final MapleData b : a.getChildren()) {
            if (itemId == Integer.parseInt(b.getName())) {
                for (final MapleData c : b.getChildren()) {
                    for (final MapleData d : c.getChildren()) {
                        final int sn = MapleDataTool.getIntConvert("" + Integer.parseInt(d.getName()), c);
                        final CashItemInfo item = getItem(sn);
                        packageItems.add(item.getId());
                    }
                }
                return packageItems;
            }
        }
        return packageItems;
    }

    public static void clearCache() {
        itemStats.clear();
        snLookup.clear();
    }
}
