package net.sf.odinms.provider;

import net.sf.odinms.provider.wz.MapleDataType;

import java.util.List;

public interface MapleData extends MapleDataEntity, Iterable<MapleData> {
    @Override
    String getName();

    MapleDataType getType();

    List<MapleData> getChildren();

    MapleData getChildByPath(String path);

    Object getData();
}
