package net.sf.odinms.server.quest;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataEntity;
import net.sf.odinms.provider.wz.MapleDataType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MapleCustomQuestData implements MapleData, Serializable {
    private static final long serialVersionUID = -8600005891655365066L;
    private final List<MapleCustomQuestData> children = new LinkedList<>();
    private final String name;
    private final Object data;
    private final MapleDataEntity parent;

    public MapleCustomQuestData(final String name, final Object data, final MapleDataEntity parent) {
        this.name = name;
        this.data = data;
        this.parent = parent;
    }

    public void addChild(final MapleData child) {
        children.add((MapleCustomQuestData) child);
    }

    public String getName() {
        return name;
    }

    public MapleDataType getType() {
        return MapleDataType.UNKNOWN_TYPE;
    }

    public List<MapleData> getChildren() {
        return new ArrayList<>(children);
    }

    public MapleData getChildByPath(final String name) {
        if (name.equals(this.name)) return this;
        final String lookup;
        final String nextName;
        if (!name.contains("/")) {
            lookup = name;
            nextName = name;
        } else {
            lookup = name.substring(0, name.indexOf("/"));
            nextName = name.substring(name.indexOf("/") + 1);
        }
        return
            children
                .stream()
                .filter(child -> child.getName().equals(lookup))
                .findFirst()
                .map(child -> child.getChildByPath(nextName))
                .orElse(null);
    }

    public Object getData() {
        return data;
    }

    public Iterator<MapleData> iterator() {
        return getChildren().iterator();
    }

    public MapleDataEntity getParent() {
        return parent;
    }
}
