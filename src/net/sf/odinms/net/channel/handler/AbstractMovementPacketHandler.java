package net.sf.odinms.net.channel.handler;

import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.AnimatedMapleMapObject;
import net.sf.odinms.server.movement.*;
import net.sf.odinms.tools.data.input.LittleEndianAccessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMovementPacketHandler extends AbstractMaplePacketHandler {
    //private static Logger log = LoggerFactory.getLogger(AbstractMovementPacketHandler.class);

    protected List<LifeMovementFragment> parseMovement(final LittleEndianAccessor lea) {
        final List<LifeMovementFragment> res = new ArrayList<>();
        final int numCommands = lea.readByte();
        for (int i = 0; i < numCommands; ++i) {
            final int command = lea.readByte();
            switch (command) {
                case 0: // Normal move
                case 5:
                case 17: { // Float
                    final int xpos = lea.readShort();
                    final int ypos = lea.readShort();
                    final int xwobble = lea.readShort();
                    final int ywobble = lea.readShort();
                    final int unk = lea.readShort();
                    final int newstate = lea.readByte();
                    final int duration = lea.readShort();
                    final AbsoluteLifeMovement alm =
                        new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    alm.setUnk(unk);
                    alm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    //log.trace("Move to {},{} command {} wobble {},{} ? {} state {} duration {}", new Object[] { xpos,
                    //xpos, command, xwobble, ywobble, newstate, duration });
                    res.add(alm);
                    break;
                }
                case 1:
                case 2:
                case 6: // FJ
                case 12:
                case 13: // Shot-jump-back
                case 16: { // Float
                    final int xmod = lea.readShort();
                    final int ymod = lea.readShort();
                    final int newstate = lea.readByte();
                    final int duration = lea.readShort();
                    final RelativeLifeMovement rlm =
                        new RelativeLifeMovement(command, new Point(xmod, ymod), duration, newstate);
                    res.add(rlm);
                    // log.trace("Relative move {},{} state {}, duration {}", new Object[] { xmod, ymod, newstate,
                    // duration });
                    break;
                }
                case 3:
                case 4: // Teleport
                case 7: // Assaulter
                case 8: // Assassinate
                case 9: // Rush
                case 14: {
                    final int xpos = lea.readShort();
                    final int ypos = lea.readShort();
                    final int xwobble = lea.readShort();
                    final int ywobble = lea.readShort();
                    final int newstate = lea.readByte();
                    final TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), newstate);
                    tm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(tm);
                    break;
                }
                case 10: { // Change equip
                    res.add(new ChangeEquipSpecialAwesome(lea.readByte()));
                    break;
                }
                case 11: { // Chair
                    final int xpos = lea.readShort();
                    final int ypos = lea.readShort();
                    final int unk = lea.readShort();
                    final int newstate = lea.readByte();
                    final int duration = lea.readShort();
                    final ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
                    cm.setUnk(unk);
                    res.add(cm);
                    break;
                }
                case 15: {
                    final int xpos = lea.readShort();
                    final int ypos = lea.readShort();
                    final int xwobble = lea.readShort();
                    final int ywobble = lea.readShort();
                    final int unk = lea.readShort();
                    final int fh = lea.readShort();
                    final int newstate = lea.readByte();
                    final int duration = lea.readShort();
                    final JumpDownMovement jdm =
                        new JumpDownMovement(command, new Point(xpos, ypos), duration, newstate);
                    jdm.setUnk(unk);
                    jdm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    jdm.setFH(fh);
                    // log.trace("Move to {},{} command {} wobble {},{} ? {} state {} duration {}", new Object[] { xpos,
                    // xpos, command, xwobble, ywobble, newstate, duration });
                    res.add(jdm);
                    break;
                }
                default: {
                    //log.warn("Unhandeled movement command {} received", command);
                    //log.warn("Movement packet: {}", lea.toString());
                    return null;
                }
            }
        }
        /*
        if (numCommands != res.size()) {
            log.warn("numCommands ({}) does not match the number of deserialized movement commands ({})", numCommands, res.size());
        }
        */
        return res;
    }

    protected void updatePosition(final List<LifeMovementFragment> movement, final AnimatedMapleMapObject target, final int yoffset) {
        for (final LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    final Point position = move.getPosition();
                    position.y += yoffset;
                    target.setPosition(position);
                }
                target.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}
