package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class CancelBuffHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int sourceid = slea.readInt();
        final ISkill skill = SkillFactory.getSkill(sourceid);
        switch (sourceid) {
            case 3121004:
            case 3221001:
            case 2121001:
            case 2221001:
            case 5221004:
            case 2321001:
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.skillCancel(c.getPlayer(), sourceid), false);
                break;
            default:
                c.getPlayer().cancelEffect(skill.getEffect(1), false, -1);
                break;
        }
    }
}
