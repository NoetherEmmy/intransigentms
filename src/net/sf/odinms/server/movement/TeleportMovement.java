package net.sf.odinms.server.movement;

import net.sf.odinms.tools.data.output.LittleEndianWriter;

import java.awt.*;

public class TeleportMovement extends AbsoluteLifeMovement {
    public TeleportMovement(final int type, final Point position, final int newstate) {
        super(type, position, 0, newstate);
    }

    @Override
    public void serialize(final LittleEndianWriter lew) {
        lew.write(getType());
        lew.writeShort(getPosition().x);
        lew.writeShort(getPosition().y);
        lew.writeShort(getPixelsPerSecond().x);
        lew.writeShort(getPixelsPerSecond().y);
        lew.write(getNewstate());
    }
}
