package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.tools.data.input.StreamUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MoveSummonHandler extends AbstractMovementPacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final Point startPos = StreamUtil.readShortPoint(slea);
        final List<LifeMovementFragment> res = parseMovement(slea);
        final MapleCharacter player = c.getPlayer();
        final Collection<MapleSummon> summons = player.getSummons().values();
        MapleSummon summon = null;
        for (final MapleSummon sum : summons) {
            if (sum.getObjectId() == oid) summon = sum;
        }
        if (summon != null) {
            updatePosition(res, summon, 0);
            // player = ((MapleCharacter) c.getPlayer().getMap().getMapObject(30000));
            player
                .getMap()
                .broadcastMessage(
                    player,
                    MaplePacketCreator.moveSummon(
                        player.getId(),
                        oid,
                        startPos,
                        res
                    ),
                    summon.getPosition()
                );
        } else {
            final List<Integer> summonKeys = new ArrayList<>();
            for (final Integer key : player.getSummons().keySet()) {
                if (player.getSummons().get(key) == null) summonKeys.add(key);
            }
            for (final Integer key : summonKeys) {
                player.getSummons().remove(key);
            }
        }
    }
}
