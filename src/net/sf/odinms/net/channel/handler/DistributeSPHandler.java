package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class DistributeSPHandler extends AbstractMaplePacketHandler {
    private class SP {
        private final ISkill skill;
        private final MapleClient c;

        SP(final MapleClient c, final ISkill skill) {
            this.skill = skill;
            this.c = c;
        }

        public ISkill getSkill() {
            return skill;
        }

        public MapleClient getClient() {
            return c;
        }
    }

    private synchronized void addSP(final SP sp) {
        final ISkill skill = sp.getSkill();
        final MapleCharacter player = sp.getClient().getPlayer();
        int remainingSp = player.getRemainingSp();

        switch (skill.getId()) {
            case 8:
            case 1003:
            case 1004:
            case 1005:
                player.getClient().disconnect();
                return;
            case 1000:
            case 1001:
            case 1002:
                final int snailsLevel = player.getSkillLevel(1000);
                final int recoveryLevel = player.getSkillLevel(1001);
                final int nimbleFeetLevel = player.getSkillLevel(1002);
                remainingSp = Math.min(player.getLevel() - 1, 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
        }

        final int maxlevel =
            skill.isFourthJob() ?
                Math.min(player.getMasterLevel(skill), skill.getMaxLevel()) :
                skill.getMaxLevel();
        final int curLevel = player.getSkillLevel(skill);
        if (remainingSp > 0 && curLevel + 1 <= maxlevel && player.canLearnSkill(skill)) {
            if (!skill.isBeginnerSkill()) {
                player.setRemainingSp(player.getRemainingSp() - 1);
            }
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
            player.changeSkillLevel(skill, curLevel + 1, player.getMasterLevel(skill));
        } else if (!skill.canBeLearnedBy(player.getJob())) {
            player.getClient().disconnect();
        } else {
            player.dropMessage(1, "You can't learn that skill yet.");
        }
    }

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        slea.readInt();
        final SP sp = new SP(c, SkillFactory.getSkill(slea.readInt()));
        addSP(sp);
    }
}
