package net.sf.odinms.provider;

import java.util.List;

public interface MapleDataDirectoryEntry extends MapleDataEntry {
    List<MapleDataDirectoryEntry> getSubdirectories();

    List<MapleDataFileEntry> getFiles();

    MapleDataEntry getEntry(String name);
}
