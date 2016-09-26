package net.sf.odinms.client;

import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.Element;

public interface ISkill {

    int getId();

    MapleStatEffect getEffect(int level);

    int getMaxLevel();

    int getAnimationTime();

    boolean canBeLearnedBy(MapleJob job);

    boolean isFourthJob();

    boolean isBeginnerSkill();

    boolean isGMSkill();

    Element getElement();
}
