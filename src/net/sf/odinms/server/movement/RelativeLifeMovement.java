package net.sf.odinms.server.movement;

import net.sf.odinms.tools.data.output.LittleEndianWriter;

import java.awt.*;

public class RelativeLifeMovement extends AbstractLifeMovement {
    public RelativeLifeMovement(final int type, final Point position, final int duration, final int newstate) {
        super(type, position, duration, newstate);
    }

    @Override
    public void serialize(final LittleEndianWriter lew) {
        lew.write(getType());
        lew.writeShort(getPosition().x);
        lew.writeShort(getPosition().y);
        lew.write(getNewstate());
        lew.writeShort(getDuration());
    }
}
