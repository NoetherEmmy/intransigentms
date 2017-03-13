package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.server.movement.AbsoluteLifeMovement;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.List;

public class MovePlayerHandler extends AbstractMovementPacketHandler {
    //private static Logger log = LoggerFactory.getLogger(MovePlayerHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        player.resetAfkTime();
        try {
            slea.readByte();
            slea.readInt();
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return;
        }
        //log.trace("Movement command received: unk1 {} unk2 {}", new Object[] { unk1, unk2 });
        final List<LifeMovementFragment> res = parseMovement(slea);
        if (res == null) return;
        if (slea.available() != 18) return;
        MaplePacket packet = MaplePacketCreator.movePlayer(player.getId(), res);
        if (!player.isHidden()) {
            player.getMap().broadcastMessage(player, packet, false);
        } else {
            player.getMap().broadcastGMMessage(player, packet, false);
        }
        //c.getSession().write(MaplePacketCreator.movePlayer(30000, res));
        if (CheatingOffense.FAST_MOVE.isEnabled() || CheatingOffense.HIGH_JUMP.isEnabled()) {
            checkMovementSpeed(player, res);
        }
        updatePosition(res, player, 0);
        player.getMap().movePlayer(player, player.getPosition());

        if (player.hasFakeChar()) {
            int i = 1;
            for (final FakeCharacter ch : player.getFakeChars()) {
                if (ch.follow() && ch.getFakeChar().getMap() == player.getMap()) {
                    TimerManager.getInstance().schedule(() -> {
                        ch.getFakeChar()
                          .getMap()
                          .broadcastMessage(
                              ch.getFakeChar(),
                              MaplePacketCreator.movePlayer(
                                  ch.getFakeChar().getId(),
                                  res
                              ),
                              false
                          );
                        updatePosition(res, ch.getFakeChar(), 0);
                        ch.getFakeChar().getMap().movePlayer(ch.getFakeChar(), ch.getFakeChar().getPosition());
                    }, i * 300);
                    i++;
                }
            }
        }
        if (
            player.getPartyQuest() != null &&
            player.getMap().getPartyQuestInstance() != null &&
            player.getMap().getPartyQuestInstance().isListeningForPlayerMovement()
        ) {
            player.getMap().getPartyQuestInstance().heardPlayerMovement(player, player.getPosition());
        }
    }

    private static void checkMovementSpeed(MapleCharacter chr, List<LifeMovementFragment> moves) {
        //boolean wasALM = true;
        //Point oldPosition = new Point (player.getPosition());
        double playerSpeedMod = chr.getSpeedMod() + 0.005d;
        //double playerJumpMod = player.getJumpMod() + 0.005;
        boolean encounteredUnk0 = false;
        for (LifeMovementFragment lmf : moves) {
            if (lmf.getClass() == AbsoluteLifeMovement.class) {
                final AbsoluteLifeMovement alm = (AbsoluteLifeMovement) lmf;
                double speedMod = Math.abs(alm.getPixelsPerSecond().x) / 125.0d;
                //int distancePerSec = Math.abs(alm.getPixelsPerSecond().x);
                //double jumpMod = Math.abs(alm.getPixelsPerSecond().y) / 525.0d;
                //double normalSpeed = distancePerSec / playerSpeedMod;
                //System.out.println(speedMod + "(" + playerSpeedMod + ") " + alm.getUnk());
                if (speedMod > playerSpeedMod) {
                    if (alm.getUnk() == 0) { // To prevent FJ messing up
                        encounteredUnk0 = true;
                    }
                    if (!encounteredUnk0) {
                        if (speedMod > playerSpeedMod) {
                            chr.getCheatTracker().registerOffense(CheatingOffense.FAST_MOVE);
                        }
                    }
                }
            //if (wasALM && (oldPosition.y == newPosition.y)) {
            //    int distance = Math.abs(oldPosition.x - newPosition.x);
            //    if (alm.getDuration() > 60) { // short durations are strange and show too fast movement
            //        double distancePerSec = (distance / (double) ((LifeMovement) move).getDuration()) * 1000.0d;
            //        double speedMod = distancePerSec / 125.0d;
            //        double normalSpeed = distancePerSec / playerSpeedMod;
            //        System.out.println(speedMod + " " + normalSpeed + " " + distancePerSec + " " + distance + " "
            //                         + alm.getWobble());
            //    }
            //}
            //oldPosition = newPosition;
            //wasALM = true;
            //} else {
            //wasALM = false;
            }
        }
    }
}
