package net.sf.odinms.server.movement;

import net.sf.odinms.tools.data.output.LittleEndianWriter;

import java.awt.*;

public interface LifeMovementFragment {
    void serialize(LittleEndianWriter lew);

    Point getPosition();
}
