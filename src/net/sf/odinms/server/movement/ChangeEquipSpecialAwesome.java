package net.sf.odinms.server.movement;

import net.sf.odinms.tools.data.output.LittleEndianWriter;

import java.awt.*;

public class ChangeEquipSpecialAwesome implements LifeMovementFragment {
    private final int wui;

    public ChangeEquipSpecialAwesome(int wui) {
        this.wui = wui;
    }

    @Override
    public void serialize(LittleEndianWriter lew) {
        lew.write(10);
        lew.write(wui);
    }

    @Override
    public Point getPosition() {
        return new Point();
    }
}
