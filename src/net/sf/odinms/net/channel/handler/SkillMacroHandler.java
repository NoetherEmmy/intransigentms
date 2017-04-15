package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillMacro;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class SkillMacroHandler extends AbstractMaplePacketHandler {
    public SkillMacroHandler() {
    }

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final int num = slea.readByte();
        for (int i = 0; i < num; ++i) {
            final String name = slea.readMapleAsciiString();
            final int shout = slea.readByte();
            final int skill1 = slea.readInt();
            final int skill2 = slea.readInt();
            final int skill3 = slea.readInt();
            final SkillMacro macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
            c.getPlayer().updateMacros(i, macro);
        }
    }
}
