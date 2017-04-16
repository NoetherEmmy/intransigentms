package net.sf.odinms.server.life;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.tools.Pair;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MobSkillFactory {
    private static final Map<Pair<Integer, Integer>, MobSkill> mobSkills = new HashMap<>();
    private static final MapleDataProvider dataSource =
        MapleDataProviderFactory.getDataProvider(
            new File(
                System.getProperty("net.sf.odinms.wzpath") + "/Skill.wz"
            )
        );
    private static final MapleData skillRoot = dataSource.getData("MobSkill.img");

    public static MobSkill getMobSkill(final int skillId, final int level) {
        MobSkill ret = mobSkills.get(new Pair<>(skillId, level));
        if (ret != null) return ret;
        synchronized (mobSkills) {
            // See if someone else that's also synchronized has loaded the skill by now:
            ret = mobSkills.get(new Pair<>(skillId, level));
            if (ret == null) {
                final MapleData skillData = skillRoot.getChildByPath(skillId + "/level/" + level);
                if (skillData != null) {
                    final int mpCon = MapleDataTool.getInt(skillData.getChildByPath("mpCon"), 0);
                    final List<Integer> toSummon = new ArrayList<>();
                    for (int i = 0; i > -1; ++i) {
                        if (skillData.getChildByPath(String.valueOf(i)) == null) break;
                        toSummon.add(MapleDataTool.getInt(skillData.getChildByPath(String.valueOf(i)), 0));
                    }
                    final int effect = MapleDataTool.getInt("summonEffect", skillData, 0);
                    final int hp = MapleDataTool.getInt("hp", skillData, 100);
                    final int x = MapleDataTool.getInt("x", skillData, 1);
                    final int y = MapleDataTool.getInt("y", skillData, 1);
                    final long duration = (long) MapleDataTool.getInt("time", skillData, 0) * 1000L;
                    final long cooltime = (long) MapleDataTool.getInt("interval", skillData, 0) * 1000L;
                    final int iprop = MapleDataTool.getInt("prop", skillData, 100);
                    final float prop = (float) iprop / 100.0f;
                    final int limit = MapleDataTool.getInt("limit", skillData, 0);
                    final MapleData ltd = skillData.getChildByPath("lt");
                    Point lt = null, rb = null;
                    if (ltd != null) {
                        lt = (Point) ltd.getData();
                        rb = (Point) skillData.getChildByPath("rb").getData();
                    }
                    ret = new MobSkill(skillId, level);
                    ret.addSummons(toSummon);
                    ret.setCoolTime(cooltime);
                    ret.setDuration(duration);
                    ret.setHp(hp);
                    ret.setMpCon(mpCon);
                    ret.setSpawnEffect(effect);
                    ret.setX(x);
                    ret.setY(y);
                    ret.setProp(prop);
                    ret.setLimit(limit);
                    ret.setLtRb(lt, rb);
                }
                mobSkills.put(new Pair<>(skillId, level), ret);
            }
            return ret;
        }
    }
}
