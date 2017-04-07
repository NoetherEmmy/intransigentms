package net.sf.odinms.provider.wz;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class WZIMGEntry implements MapleData {
    private String name;
    private MapleDataType type;
    private final List<MapleData> children = new ArrayList<>(10);
    private Object data;
    private final MapleDataEntity parent;

    public WZIMGEntry(MapleDataEntity parent) {
        this.parent = parent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MapleDataType getType() {
        return type;
    }

    @Override
    public List<MapleData> getChildren() {
        // List<MapleData> mapleDataChildren = (List) children;
        return Collections.unmodifiableList(children);
    }

    @Override
    public MapleData getChildByPath(String path) {
        String[] segments = path.split("/");
        if (segments[0].equals("..")) {
            return ((MapleData) parent).getChildByPath(path.substring(path.indexOf("/") + 1));
        }

        MapleData ret = this;
        for (int x = 0; x < segments.length; ++x) {
            boolean foundChild = false;
            for (MapleData child : ret.getChildren()) {
                if (child.getName().equals(segments[x])) {
                    ret = child;
                    foundChild = true;
                    break;
                }
            }
            if (!foundChild) return null;
        }
        return ret;
    }

    @Override
    public Object getData() {
        return data;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(MapleDataType type) {
        this.type = type;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void addChild(WZIMGEntry entry) {
        children.add(entry);
    }

    @Override
    public Iterator<MapleData> iterator() {
        return getChildren().iterator();
    }

    @Override
    public String toString() {
        return name + ":" + data;
    }

    public MapleDataEntity getParent() {
        return parent;
    }

    public void finish() {
        ((ArrayList<MapleData>) children).trimToSize();
    }
}
