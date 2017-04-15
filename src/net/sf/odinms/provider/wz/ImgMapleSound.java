package net.sf.odinms.provider.wz;

public class ImgMapleSound {
    private final int dataLenght;
    private final int offset;

    /**
     * @param dataLength length of the sound data
     * @param offset offset in the img file
     */
    public ImgMapleSound(final int dataLength, final int offset) {
        this.dataLenght = dataLength;
        this.offset = offset;
    }

    public int getDataLength() {
        return dataLenght;
    }

    public int getOffset() {
        return offset;
    }
}
