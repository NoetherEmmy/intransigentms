package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.Iterator;

public class DamageSummonHandler extends AbstractMaplePacketHandler {
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        slea.readInt();
        final int unkByte = slea.readByte();
        final int damage = slea.readInt();
        final int monsterIdFrom = slea.readInt();
        slea.readByte();
        final Iterator<MapleSummon> iter = player.getSummons().values().iterator();
        while (iter.hasNext()) {
            final MapleSummon summon = iter.next();
            if (summon.isPuppet() && summon.getOwner() == player) {
                summon.addHP(-damage);
                if (summon.getHP() <= 0) {
                    player.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                }
                player
                    .getMap()
                    .broadcastMessage(
                        player,
                        MaplePacketCreator.damageSummon(
                            player.getId(),
                            summon.getSkill(),
                            damage,
                            unkByte,
                            monsterIdFrom
                        ),
                        summon.getPosition()
                    );
                break;
            }
        }
    }
}
