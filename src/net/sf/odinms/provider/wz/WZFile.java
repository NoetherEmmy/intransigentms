package net.sf.odinms.provider.wz;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataDirectoryEntry;
import net.sf.odinms.provider.MapleDataFileEntry;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.tools.data.input.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class WZFile implements MapleDataProvider {
    static {
        ListWZFile.init();
    }

    private final File wzfile;
    private final LittleEndianAccessor lea;
    private final SeekableLittleEndianAccessor slea;
    // private LittleEndianOutputStream leo;
    private final Logger log = LoggerFactory.getLogger(WZFile.class);
    private int headerSize;
    private final WZDirectoryEntry root;
    private final boolean provideImages;
    private int cOffset;

    public WZFile(File wzfile, boolean provideImages) throws IOException {
        this.wzfile = wzfile;

        lea = new GenericLittleEndianAccessor(new InputStreamByteStream(new BufferedInputStream(new FileInputStream(wzfile))));
        RandomAccessFile raf = new RandomAccessFile(wzfile, "r");
        slea = new GenericSeekableLittleEndianAccessor(new RandomAccessByteStream(raf));
        root = new WZDirectoryEntry(wzfile.getName(), 0, 0, null);
        this.provideImages = provideImages;
        load();
    }

    @SuppressWarnings("unused")
    private void load() {
        String sPKG = lea.readAsciiString(4);
        int size1 = lea.readInt();
        int size2 = lea.readInt();
        headerSize = lea.readInt();
        String copyright = lea.readNullTerminatedAsciiString();
        short version = lea.readShort();
        parseDirectory(root);
        cOffset = (int) lea.getBytesRead();
        getOffsets(root);
    }

    // private void writeHeader(short version) throws IOException { // pseudo header leo.writeBytes("PKG1");
    // leo.writeInt(0);
    // leo.writeInt(0);
    // leo.writeInt(0);
    // leo.writeBytes("Package file v1.0 Copyright OdinMS, Mtz");
    // leo.writeByte(0);
    // leo.writeShort(version);
    // writeDirectory(root);
    // cOffset = leo.size();
    // writeOffsets(root);
    // }

    private void getOffsets(MapleDataDirectoryEntry dir) {
        for (MapleDataFileEntry file : dir.getFiles()) {
            file.setOffset(cOffset);
            cOffset += file.getSize();
        }
        for (MapleDataDirectoryEntry sdir : dir.getSubdirectories()) {
            getOffsets(sdir);
        }
    }

    private void parseDirectory(WZDirectoryEntry dir) {
        int entries = WZTool.readValue(lea);
        for (int i = 0; i < entries; ++i) {
            byte marker = lea.readByte();
            String name = null;
            @SuppressWarnings("unused")
            int dummyInt;
            int size, checksum;
            switch (marker) {
                case 0x02:
                    name = WZTool.readDecodedStringAtOffset(slea, lea.readInt() + this.headerSize + 1);
                    size = WZTool.readValue(lea);
                    checksum = WZTool.readValue(lea);
                    dummyInt = lea.readInt();
                    dir.addFile(new WZFileEntry(name, size, checksum, dir));
                    break;
                case 0x03:
                case 0x04:
                    name = WZTool.readDecodedString();
                    size = WZTool.readValue(lea);
                    checksum = WZTool.readValue(lea);
                    dummyInt = lea.readInt();
                    if (marker == 3) {
                        dir.addDirectory(new WZDirectoryEntry(name, size, checksum, dir));
                    } else {
                        dir.addFile(new WZFileEntry(name, size, checksum, dir));
                    }
                    break;
                default:
                    log.error("Default case in marker ({}):/", marker);
            }
        }

        for (MapleDataDirectoryEntry idir : dir.getSubdirectories()) {
                parseDirectory((WZDirectoryEntry) idir);
        }
    }

    // private void writeDirectory(MapleDataDirectoryEntry dir) {
    // // leo.writeInt(dir.getSize());
    //
    // for (int i = 0; i < dir.getSize(); ++i) {
    // byte marker = lea.readByte();
    //
    // // if ()
    //
    // String name = null;
    // @SuppressWarnings("unused")
    // int dummyInt;
    // int size, checksum;
    //
    // switch (marker) {
    // case 0x02:
    // name = WZTool.readDecodedStringAtOffset(slea, lea.readInt() + this.headerSize + 1);
    // size = WZTool.readValue(lea);
    // checksum = WZTool.readValue(lea);
    // dummyInt = lea.readInt();
    // dir.addFile(new WZFileEntry(name, size, checksum));
    // break;
    //
    // case 0x03:
    // case 0x04:
    // name = WZTool.readDecodedString(lea);
    // size = WZTool.readValue(lea);
    // checksum = WZTool.readValue(lea);
    // dummyInt = lea.readInt();
    // if (marker == 3) {
    // dir.addDirectory(new WZDirectoryEntry(name, size, checksum));
    // } else {
    // dir.addFile(new WZFileEntry(name, size, checksum));
    // }
    // break;
    // default:
    // log.error("Default case in marker ({}):/", marker);
    // }
    // }
    //
    // for (MapleDataDirectoryEntry idir : dir.getSubdirectories()) {
    // parseDirectory(idir);
    // }
    // }

    public WZIMGFile getImgFile(String path) throws IOException {
        String segments[] = path.split("/");
        WZDirectoryEntry dir = root;
        for (int x = 0; x < segments.length - 1; ++x) {
            dir = (WZDirectoryEntry) dir.getEntry(segments[x]);
            if (dir == null) {
                // throw new IllegalArgumentException("File " + path + " not found in " + root.getName());
                return null;
            }
        }
        WZFileEntry entry = (WZFileEntry) dir.getEntry(segments[segments.length - 1]);
        if (entry == null) {
            return null;
        }
        String fullPath = wzfile.getName().substring(0, wzfile.getName().length() - 3).toLowerCase() + "/" + path;
        return new WZIMGFile(this.wzfile, entry, provideImages, ListWZFile.isModernImgFile(fullPath));
    }

    public synchronized MapleData getData(String path) {
        try {
            WZIMGFile imgFile = getImgFile(path);
            if (imgFile == null) {
                //throw new IllegalArgumentException("File " + path + " not found in " + root.getName());
                return null;
            }
            return imgFile.getRoot();
        } catch (IOException e) {
            log.error("THROW", e);
        }
        return null;
    }

    public MapleDataDirectoryEntry getRoot() {
            return root;
    }
}