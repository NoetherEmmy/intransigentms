package net.sf.odinms.server.movement;

import java.awt.*;

public interface LifeMovement extends LifeMovementFragment {
    @Override
    Point getPosition();

    int getNewstate();

    int getDuration();

    int getType();
}
