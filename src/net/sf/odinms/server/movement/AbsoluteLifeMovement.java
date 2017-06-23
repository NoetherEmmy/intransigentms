package net.sf.odinms.server.movement;

import net.sf.odinms.tools.data.output.LittleEndianWriter;

import java.awt.*;

public class AbsoluteLifeMovement extends AbstractLifeMovement {
    private Point pixelsPerSecond;
    private int unk;

    public AbsoluteLifeMovement(final int type, final Point position, final int duration, final int newstate) {
        super(type, position, duration, newstate);
    }

    public Point getPixelsPerSecond() {
        return pixelsPerSecond;
    }

    public void setPixelsPerSecond(final Point wobble) {
        this.pixelsPerSecond = wobble;
    }

    public int getUnk() {
        return unk;
    }

    public void setUnk(final int unk) {
        this.unk = unk;
    }

    @Override
    public void serialize(final LittleEndianWriter lew) {
        lew.write(getType());
        lew.writeShort(getPosition().x);
        lew.writeShort(getPosition().y);
        lew.writeShort(pixelsPerSecond.x);
        lew.writeShort(pixelsPerSecond.y);
        lew.writeShort(unk);
        lew.write(getNewstate());
        lew.writeShort(getDuration());
    }
}
