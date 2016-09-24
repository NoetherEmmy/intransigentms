package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class DistributeSPHandler extends AbstractMaplePacketHandler {

    private class SP {

        private ISkill skill;
        private MapleClient c;

        SP(MapleClient c, ISkill skill) {
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

    private void addSP(SP sp) {
        ISkill skill = sp.getSkill();
        MapleCharacter player = sp.getClient().getPlayer();
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
                int snailsLevel = player.getSkillLevel(SkillFactory.getSkill(1000));
                int recoveryLevel = player.getSkillLevel(SkillFactory.getSkill(1001));
                int nimbleFeetLevel = player.getSkillLevel(SkillFactory.getSkill(1002));
                remainingSp = Math.min((player.getLevel() - 1), 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;

        }
        int maxlevel = skill.isFourthJob() ? player.getMasterLevel(skill) : skill.getMaxLevel();
        int curLevel = player.getSkillLevel(skill);
        if (remainingSp > 0 && curLevel + 1 <= maxlevel && skill.canBeLearnedBy(player.getJob())) {
            if (!skill.isBeginnerSkill()) {
                player.setRemainingSp(player.getRemainingSp() - 1);
            }
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
            player.changeSkillLevel(skill, curLevel + 1, player.getMasterLevel(skill));
        } else if (!skill.canBeLearnedBy(player.getJob())) {
            player.getClient().disconnect();
            return;
        } else if (!(remainingSp > 0 && curLevel + 1 <= maxlevel)) {
            return;
        }
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        SP sp = new SP(c, SkillFactory.getSkill(slea.readInt()));
        addSP(sp);
    }
}