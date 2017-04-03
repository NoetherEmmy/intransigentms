package net.sf.odinms.provider.wz;

import net.sf.odinms.provider.MapleCanvas;

import java.awt.*;
import java.awt.image.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class PNGMapleCanvas implements MapleCanvas {
    private static final int[] ZAHLEN = new int[] {2, 1, 0, 3};
    private final int height, width;
    private final int dataLength;
    private final int format;

    private final byte[] data;

    public PNGMapleCanvas(int width, int height, int dataLength, int format, byte[] data) {
        super();
        this.height = height;
        this.width = width;
        this.dataLength = dataLength;
        this.format = format;
        this.data = data;
    }

    public int getHeight() {
            return height;
    }

    public int getWidth() {
            return width;
    }

    public int getFormat() {
            return format;
    }

    public byte[] getData() {
            return data;
    }

    @Override
    public BufferedImage getImage() {
        int sizeUncompressed = 0;
        int size8888;
        int maxWriteBuf = 2;
        int maxHeight = 3;
        byte[] writeBuf = new byte[maxWriteBuf];
        //byte[] rowPointers = new byte[maxHeight];
        switch (format) {
            case 1:
            case 513:
                sizeUncompressed = height * width * 4;
                break;
            case 2:
                sizeUncompressed = height * width * 8;
                break;
            case 517:
                sizeUncompressed = height * width / 128;
                break;
        }
        size8888 = height * width * 8;
        if (size8888 > maxWriteBuf) {
            maxWriteBuf = size8888;
            writeBuf = new byte[maxWriteBuf];
        }
        /*
        if (height > maxHeight) {
            maxHeight = height;
            rowPointers = new byte[maxHeight]; // ???
        }
        */
        Inflater dec = new Inflater();
        dec.setInput(data, 0, dataLength);
        int declen;
        byte[] uc = new byte[sizeUncompressed];
        try {
            declen = dec.inflate(uc);
        } catch ( DataFormatException ex) {
            throw new RuntimeException("Error", ex);
        }
        dec.end();
        if (format == 1) {
            for (int i = 0; i < sizeUncompressed; ++i) {
                byte low = (byte) (uc[i] & 0x0F);
                byte high = (byte) (uc[i] & 0xF0);
                writeBuf[(i << 1)] = (byte) (((low << 4) | low) & 0xFF);
                writeBuf[(i << 1) + 1] = (byte) (high | ((high >>> 4) & 0xF));
            }
        } else if (format == 2) {
            writeBuf = uc;
        } else if (format == 513) {
            for (int i = 0; i < declen; i += 2) {
                byte bBits = (byte) ((uc[i] & 0x1F) << 3);
                byte gBits = (byte) (((uc[i + 1] & 0x07) << 5) | ((uc[i] & 0xE0) >> 3));
                byte rBits = (byte) (uc[i + 1] & 0xF8);
                writeBuf[(i << 1)] = (byte) (bBits | (bBits >> 5));
                writeBuf[(i << 1) + 1] = (byte) (gBits | (gBits >> 6));
                writeBuf[(i << 1) + 2] = (byte) (rBits | (rBits >> 5));
                writeBuf[(i << 1) + 3] = (byte) 0xFF;
            }
        } else if (format == 517) {
            byte b;
            int pixelIndex;
            for (int i = 0; i < declen; ++i) {
                for (int j = 0; j < 8; ++j) {
                    b = (byte) (((uc[i] & (0x01 << (7 - j))) >> (7 - j)) * 255);
                    for (int k = 0; k < 16; ++k) {
                        pixelIndex = (i << 9) + (j << 6) + k * 2;
                        writeBuf[pixelIndex] = b;
                        writeBuf[pixelIndex + 1] = b;
                        writeBuf[pixelIndex + 2] = b;
                        writeBuf[pixelIndex + 3] = (byte) 0xFF;
                    }
                }
            }
        }
        DataBufferByte imgData = new DataBufferByte(writeBuf, sizeUncompressed);

        //SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, c.getWidth(), c.getHeight(), 4, c.getWidth() * 4, new int[] {2, 1, 0, 3});
        SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height, 4, width * 4, ZAHLEN);
        WritableRaster imgRaster = Raster.createWritableRaster(sm, imgData, new Point());

        BufferedImage aa = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        aa.setData(imgRaster);
        return aa;
    }
}
