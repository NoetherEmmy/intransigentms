package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class GiveFameHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final MapleCharacter p = c.getPlayer();
        p.resetAfkTime();
        final int who = slea.readInt();
        final int mode = slea.readByte();
        final int famechange = mode == 0 ? -1 : 1;
        final MapleCharacter target = (MapleCharacter) p.getMap().getMapObject(who);
        if (target == p) {
            p.getCheatTracker().registerOffense(CheatingOffense.FAMING_SELF);
            return;
        } else if (p.getLevel() < 15) {
            p.getCheatTracker().registerOffense(CheatingOffense.FAMING_UNDER_15);
            return;
        }
        if (target == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        switch (p.canGiveFame(target)) {
            case OK:
                if (Math.abs(target.getFame()) < 30000) {
                    target.addFame(famechange);
                    target.updateSingleStat(MapleStat.FAME, target.getFame());
                }
                p.hasGivenFame(target);
                c.getSession()
                 .write(
                     MaplePacketCreator.giveFameResponse(
                         mode,
                         target.getName(),
                         target.getFame()
                     )
                 );
                target
                    .getClient()
                    .getSession()
                    .write(
                        MaplePacketCreator.receiveFame(
                            mode,
                            p.getName()
                        )
                    );
                break;
            case NOT_TODAY:
                c.getSession().write(MaplePacketCreator.giveFameErrorResponse(3));
                break;
            case NOT_THIS_MONTH:
                c.getSession().write(MaplePacketCreator.giveFameErrorResponse(4));
                break;
        }
    }
}
