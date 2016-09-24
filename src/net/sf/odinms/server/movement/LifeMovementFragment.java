package net.sf.odinms.server.movement;

import java.awt.Point;
import net.sf.odinms.tools.data.output.LittleEndianWriter;

public interface LifeMovementFragment {
    void serialize(LittleEndianWriter lew);

    Point getPosition();
}