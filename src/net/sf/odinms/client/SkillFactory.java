package net.sf.odinms.client;

import net.sf.odinms.net.world.WorldServer;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.tools.StringUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class SkillFactory {
    private static final Map<Integer, ISkill> skills = new HashMap<>();
    private static final MapleDataProvider datasource =
        MapleDataProviderFactory.getDataProvider(
            new File(
                System.getProperty(WorldServer.WZPATH) + "/Skill.wz"
            )
        );
    private static final MapleData stringData =
        MapleDataProviderFactory.getDataProvider(
            new File(
                System.getProperty(WorldServer.WZPATH) + "/String.wz"
            )
        ).getData("Skill.img");

    public static ISkill getSkill(final int id) {
        ISkill ret = skills.get(id);
        if (ret != null) return ret;
        synchronized (skills) {
            // Check if someone else that's also synchronized has loaded the skill by now.
            ret = skills.get(id);
            if (ret == null) {
                final int job = id / 10000;
                final MapleData skillRoot =
                    datasource.getData(
                        StringUtil.getLeftPaddedStr(
                            String.valueOf(job),
                            '0',
                            3
                        ) + ".img"
                    );
                final MapleData skillData =
                    skillRoot.getChildByPath(
                        "skill/" +
                            StringUtil.getLeftPaddedStr(
                                String.valueOf(id),
                                '0',
                                7
                            )
                    );
                if (skillData != null) ret = Skill.loadFromData(id, skillData);
                skills.put(id, ret);
            }
            return ret;
        }
    }

    public static String getSkillName(final int id) {
        String strId = Integer.toString(id);
        strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
        final MapleData skillRoot = stringData.getChildByPath(strId);
        if (skillRoot != null) return MapleDataTool.getString(skillRoot.getChildByPath("name"), "");
        return null;
    }
}
