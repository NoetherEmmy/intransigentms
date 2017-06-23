package net.sf.odinms.provider.wz;

import net.sf.odinms.provider.MapleDataEntity;
import net.sf.odinms.provider.MapleDataFileEntry;

public class WZFileEntry extends WZEntry implements MapleDataFileEntry {
    private int offset;

    public WZFileEntry(final String name, final int size, final int checksum, final MapleDataEntity parent) {
        super(name, size, checksum, parent);
    }

    @Override
    public int getOffset() {
        return offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }
}
