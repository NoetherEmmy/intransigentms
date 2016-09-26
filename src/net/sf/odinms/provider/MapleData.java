package net.sf.odinms.provider;

import java.util.List;
import net.sf.odinms.provider.wz.MapleDataType;

public interface MapleData extends MapleDataEntity, Iterable<MapleData> {
    @Override
    String getName();
    MapleDataType getType();
    List<MapleData> getChildren();
    MapleData getChildByPath(String path);
    Object getData();
}