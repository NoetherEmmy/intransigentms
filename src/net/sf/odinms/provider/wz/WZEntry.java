package net.sf.odinms.provider.wz;

import net.sf.odinms.provider.MapleDataEntity;
import net.sf.odinms.provider.MapleDataEntry;

public class WZEntry implements MapleDataEntry {
    private final String name;
    private final int size, checksum;
    private int offset;
    private final MapleDataEntity parent;

    public WZEntry(final String name, final int size, final int checksum, final MapleDataEntity parent) {
        super();
        this.name = name;
        this.size = size;
        this.checksum = checksum;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public int getChecksum() {
        return checksum;
    }

    public int getOffset() {
        return offset;
    }

    public MapleDataEntity getParent() {
        return parent;
    }
}
