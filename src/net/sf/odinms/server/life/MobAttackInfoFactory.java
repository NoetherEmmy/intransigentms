package net.sf.odinms.server.life;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.StringUtil;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class MobAttackInfoFactory {
    private static final Map<Pair<Integer, Integer>, MobAttackInfo> mobAttacks = new LinkedHashMap<>();
    private static final MapleDataProvider dataSource =
        MapleDataProviderFactory.getDataProvider(
            new File(
                System.getProperty("net.sf.odinms.wzpath") + "/Mob.wz"
            )
        );

    public static MobAttackInfo getMobAttackInfo(final MapleMonster mob, final int attack) {
        MobAttackInfo ret = mobAttacks.get(new Pair<>(mob.getId(), attack));
        if (ret != null) {
            return ret;
        }
        synchronized (mobAttacks) {
            // See if someone else that's also synchronized has loaded the skill by now
            ret = mobAttacks.get(new Pair<>(mob.getId(), attack));
            if (ret == null) {
                MapleData mobData =
                    dataSource.getData(
                        StringUtil.getLeftPaddedStr(
                            Integer.toString(mob.getId()) + ".img",
                            '0',
                            11
                        )
                    );
                if (mobData != null) {
                    final MapleData infoData = mobData.getChildByPath("info");
                    final String linkedmob = MapleDataTool.getString("link", mobData, "");
                    if (!linkedmob.equals("")) {
                        mobData = dataSource.getData(StringUtil.getLeftPaddedStr(linkedmob + ".img", '0', 11));
                    }
                    final MapleData attackData = mobData.getChildByPath("attack" + (attack + 1) + "/info");
                    if (attackData != null) {
                        final MapleData deadlyAttack = attackData.getChildByPath("deadlyAttack");
                        final int mpBurn = MapleDataTool.getInt("mpBurn", attackData, 0);
                        final int disease = MapleDataTool.getInt("disease", attackData, 0);
                        final int level = MapleDataTool.getInt("level", attackData, 0);
                        final int mpCon = MapleDataTool.getInt("conMP", attackData, 0);
                        ret = new MobAttackInfo(mob.getId(), attack);
                        //ret.setDeadlyAttack(deadlyAttack != null);
                        ret.setDeadlyAttack();
                        ret.setMpBurn(mpBurn);
                        ret.setDiseaseSkill(disease);
                        ret.setDiseaseLevel(level);
                        ret.setMpCon(mpCon);
                    }
                }
                mobAttacks.put(new Pair<>(mob.getId(), attack), ret);
            }
            return ret;
        }
    }
}
