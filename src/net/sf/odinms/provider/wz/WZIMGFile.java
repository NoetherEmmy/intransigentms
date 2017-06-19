package net.sf.odinms.provider.wz;

import net.sf.odinms.tools.data.input.GenericSeekableLittleEndianAccessor;
import net.sf.odinms.tools.data.input.RandomAccessByteStream;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;

public class WZIMGFile {
    //private final Logger log = LoggerFactory.getLogger(WZIMGFile.class);
    private final WZFileEntry file;
    private final WZIMGEntry root;
    private final boolean provideImages;

    public WZIMGFile(final File wzfile, final WZFileEntry file, final boolean provideImages, final boolean modernImg) throws IOException {
        final RandomAccessFile raf = new RandomAccessFile(wzfile, "r");
        final SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new RandomAccessByteStream(raf));
        slea.seek(file.getOffset());
        this.file = file;
        this.provideImages = provideImages;
        root = new WZIMGEntry(file.getParent());
        root.setName(file.getName());
        root.setType(MapleDataType.EXTENDED);
        // dumpImg(new FileOutputStream(file.getName()), slea);
        parseExtended(root, slea, 0);
        root.finish();
        raf.close();
    }

    protected void dumpImg(final OutputStream out, final SeekableLittleEndianAccessor slea) throws IOException {
        final DataOutputStream os = new DataOutputStream(out);
        final long oldPos = slea.getPosition();
        slea.seek(file.getOffset());
        for (int x = 0; x < file.getSize(); ++x) {
            os.write(slea.readByte());
        }
        slea.seek(oldPos);
    }

    public WZIMGEntry getRoot() {
        return root;
    }

    private void parse(final WZIMGEntry entry, final SeekableLittleEndianAccessor slea) {
        byte marker = slea.readByte();
        switch (marker) {
            case 0: {
                final String name = WZTool.readDecodedString();
                entry.setName(name);
                break;
            } case 1: {
                final String name = WZTool.readDecodedStringAtOffsetAndReset(slea, file.getOffset() + slea.readInt());
                entry.setName(name);
                break;
            } default:
                System.err.println("Unknown Image identifier: " + marker + " at offset " + (slea.getPosition() - file.getOffset()));
        }
        marker = slea.readByte();
        switch (marker) {
            case 0:
                entry.setType(MapleDataType.IMG_0x00);
                break;
            case 2:
            case 11:
                entry.setType(MapleDataType.SHORT);
                entry.setData(slea.readShort());
                break;
            case 3:
                entry.setType(MapleDataType.INT);
                entry.setData(WZTool.readValue(slea));
                break;
            case 4:
                entry.setType(MapleDataType.FLOAT);
                entry.setData(WZTool.readFloatValue(slea));
                break;
            case 5:
                entry.setType(MapleDataType.DOUBLE);
                entry.setData(slea.readDouble());
                break;
            case 8:
                entry.setType(MapleDataType.STRING);
                final byte iMarker = slea.readByte();
                if (iMarker == 0) {
                    // String pathToData = MapleDataTool.getFullDataPath(entry);
                    entry.setData(WZTool.readDecodedString());
                } else if (iMarker == 1) {
                    entry.setData(WZTool.readDecodedStringAtOffsetAndReset(slea, slea.readInt() + file.getOffset()));
                } else {
                    System.err.println("Unknown String type " + iMarker);
                }
                break;
            case 9:
                entry.setType(MapleDataType.EXTENDED);
                long endOfExtendedBlock = slea.readInt();
                endOfExtendedBlock += slea.getPosition();
                parseExtended(entry, slea, endOfExtendedBlock);
                break;
            default:
                System.err.println("Unknown Image type " + marker);
        }
    }

    private void parseExtended(final WZIMGEntry entry, final SeekableLittleEndianAccessor slea, final long endOfExtendedBlock) {
        byte marker = slea.readByte();
        final String type;
        switch (marker) {
            case 0x73:
                type = WZTool.readDecodedString();
                break;
            case 0x1B:
                type = WZTool.readDecodedStringAtOffsetAndReset(slea, file.getOffset() + slea.readInt());
                break;
            default:
                throw new RuntimeException("Unknown extended image identifier: " + marker + " at offset " +
                (slea.getPosition() - file.getOffset()));
        }

        /*
         * "Shape2D#Vector2D"
         * "Shape2D#Convex2D"
         * "Property"
         * "Sound_DX8"
         * "Canvas"
         * "UOL"
         */
        switch (type) {
            case "Property": {
                entry.setType(MapleDataType.PROPERTY);
                slea.readByte();
                slea.readByte();
                final int children = WZTool.readValue(slea);
                for (int i = 0; i < children; ++i) {
                    final WZIMGEntry cEntry = new WZIMGEntry(entry);
                    parse(cEntry, slea);
                    cEntry.finish();
                    entry.addChild(cEntry);
                }
                break;
            }
            case "Canvas": {
                entry.setType(MapleDataType.CANVAS);
                slea.readByte();
                marker = slea.readByte();
                if (marker == 0) {
                    // ???
                } else if (marker == 1) {
                    slea.readByte();
                    slea.readByte();
                    final int children = WZTool.readValue(slea);
                    for (int i = 0; i < children; ++i) {
                        final WZIMGEntry child = new WZIMGEntry(entry);
                        parse(child, slea);
                        child.finish();
                        entry.addChild(child);
                    }
                } else {
                    System.err.println("Canvas marker != 1 (" + marker + ")");
                }
                final int width = WZTool.readValue(slea);
                final int height = WZTool.readValue(slea);
                final int format = WZTool.readValue(slea);
                final int format2 = slea.readByte();
                slea.readInt();
                final int dataLength = slea.readInt() - 1;
                slea.readByte();

                if (provideImages) {
                    final byte[] pngdata = slea.read(dataLength);
                    entry.setData(new PNGMapleCanvas(width, height, dataLength, format + format2, pngdata));
                } else {
                    entry.setData(new PNGMapleCanvas(width, height, dataLength, format + format2, null));
                    slea.skip(dataLength);
                }
                break;
            }
            case "Shape2D#Vector2D":
                entry.setType(MapleDataType.VECTOR);
                final int x = WZTool.readValue(slea);
                final int y = WZTool.readValue(slea);
                entry.setData(new Point(x, y));
                break;
            case "Shape2D#Convex2D": {
                final int children = WZTool.readValue(slea);
                for (int i = 0; i < children; ++i) {
                    final WZIMGEntry cEntry = new WZIMGEntry(entry);
                    parseExtended(cEntry, slea, 0);
                    cEntry.finish();
                    entry.addChild(cEntry);
                }
                break;
            }
            case "Sound_DX8": {
                entry.setType(MapleDataType.SOUND);
                slea.readByte();
                final int dataLength = WZTool.readValue(slea);
                WZTool.readValue(slea);
                final int offset = (int) slea.getPosition();
                entry.setData(new ImgMapleSound(dataLength, offset - file.getOffset()));
                slea.seek(endOfExtendedBlock);
                break;
            }
            case "UOL":
                entry.setType(MapleDataType.UOL);
                slea.readByte();
                final byte uolmarker = slea.readByte();
                switch (uolmarker) {
                    case 0:
                        entry.setData(WZTool.readDecodedString());
                        break;
                    case 1:
                        entry.setData(WZTool.readDecodedStringAtOffsetAndReset(slea, file.getOffset() + slea.readInt()));
                        break;
                    default:
                        System.err.println("Unknown UOL marker: " + uolmarker + " " + entry.getName());
                }
                break;
            default:
                throw new RuntimeException("Unhandeled extended type: " + type);
        }
    }
}
