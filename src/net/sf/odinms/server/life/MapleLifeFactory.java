package net.sf.odinms.server.life;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.provider.wz.MapleDataType;
import net.sf.odinms.server.maps.MapleMapFactory;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class MapleLifeFactory {
    private static final Logger log = LoggerFactory.getLogger(MapleMapFactory.class);
    private static final MapleDataProvider data =
        MapleDataProviderFactory.getDataProvider(
            new File(System.getProperty("net.sf.odinms.wzpath") + "/Mob.wz")
        );
    private static final MapleDataProvider stringDataWZ =
        MapleDataProviderFactory.getDataProvider(
            new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")
        );
    private static final MapleData mobStringData = stringDataWZ.getData("Mob.img");
    private static final MapleData npcStringData = stringDataWZ.getData("Npc.img");
    private static final Map<Integer, MapleMonsterStats> monsterStats = new LinkedHashMap<>();

    public static AbstractLoadedMapleLife getLife(int id, String type) {
        if (type.equalsIgnoreCase("n")) {
            return getNPC(id);
        } else if (type.equalsIgnoreCase("m")) {
            return getMonster(id);
        } else {
            log.warn("Unknown Life type: {}", type);
            return null;
        }
    }

    public static MapleMonster getMonster(int mid) {
        MapleMonsterStats stats;
        if (monsterStats.containsKey(mid)) {
            stats = monsterStats.get(mid);
        } else {
            try {
                MapleData monsterData =
                    data.getData(
                        StringUtil.getLeftPaddedStr(
                            Integer.toString(mid) + ".img",
                            '0',
                            11
                        )
                    );
                if (monsterData == null) {
                    monsterStats.put(mid, null);
                    return null;
                }
                MapleData monsterInfoData = monsterData.getChildByPath("info");
                stats = new MapleMonsterStats();
                stats.setHp(MapleDataTool.getIntConvert("maxHP", monsterInfoData));
                stats.setMp(MapleDataTool.getIntConvert("maxMP", monsterInfoData, 0));
                stats.setExp(MapleDataTool.getIntConvert("exp", monsterInfoData, 0));
                stats.setPADamage(MapleDataTool.getIntConvert("PADamage", monsterInfoData, 0));
                stats.setWdef(MapleDataTool.getIntConvert("PDDamage", monsterInfoData, 0));
                stats.setMdef(MapleDataTool.getIntConvert("MDDamage", monsterInfoData, 0));
                stats.setLevel(MapleDataTool.getIntConvert("level", monsterInfoData));
                stats.setRemoveAfter(MapleDataTool.getIntConvert("removeAfter", monsterInfoData, 0));
                stats.setBoss(MapleDataTool.getIntConvert("boss", monsterInfoData, 0) > 0);
                stats.setFfaLoot(MapleDataTool.getIntConvert("publicReward", monsterInfoData, 0) > 0);
                stats.setUndead(MapleDataTool.getIntConvert("undead", monsterInfoData, 0) > 0);
                //
                try {
                    stats.setName(MapleDataTool.getString(mid + "/name", mobStringData, "MISSINGNO"));
                } catch (Exception e) {
                    stats.setName("MISSINGNO");
                    System.err.println(e + "   !ID!:  " + mid);
                }
                //
                stats.setBuffToGive(MapleDataTool.getIntConvert("buff", monsterInfoData, -1));
                stats.setExplosive(MapleDataTool.getIntConvert("explosiveReward", monsterInfoData, 0) > 0);
                stats.setAccuracy(MapleDataTool.getIntConvert("acc", monsterInfoData, 0));
                stats.setAvoid(MapleDataTool.getIntConvert("eva", monsterInfoData, 0));
                MapleData firstAttackData = monsterInfoData.getChildByPath("firstAttack");
                int firstAttack = 0;
                if (firstAttackData != null) {
                    if (firstAttackData.getType() == MapleDataType.FLOAT) {
                        firstAttack = Math.round(MapleDataTool.getFloat(firstAttackData));
                    } else {
                        firstAttack = MapleDataTool.getInt(firstAttackData);
                    }
                }
                stats.setFirstAttack(firstAttack > 0);
                if (stats.isBoss() || mid == 8810018) {
                    MapleData hpTagColor = monsterInfoData.getChildByPath("hpTagColor");
                    MapleData hpTagBgColor = monsterInfoData.getChildByPath("hpTagBgcolor");
                    if (hpTagBgColor == null || hpTagColor == null) {
                        log.trace(
                            "Monster " +
                                stats.getName() +
                                " (" +
                                mid +
                                ") flagged as boss without boss HP bars."
                        );
                        stats.setTagColor(0);
                        stats.setTagBgColor(0);
                    } else {
                        stats.setTagColor(MapleDataTool.getIntConvert("hpTagColor", monsterInfoData));
                        stats.setTagBgColor(MapleDataTool.getIntConvert("hpTagBgcolor", monsterInfoData));
                    }
                }
                for (MapleData idata : monsterData) {
                    if (!idata.getName().equals("info")) {
                        int delay = 0;
                        for (MapleData pic : idata.getChildren()) {
                            delay += MapleDataTool.getIntConvert("delay", pic, 0);
                        }
                        stats.setAnimationTime(idata.getName(), delay);
                    }
                }

                MapleData reviveInfo = monsterInfoData.getChildByPath("revive");
                if (reviveInfo != null) {
                    List<Integer> revives = new ArrayList<>();
                    for (MapleData data_ : reviveInfo) {
                        revives.add(MapleDataTool.getInt(data_));
                    }
                    stats.setRevives(revives);
                }

                decodeElementalString(stats, MapleDataTool.getString("elemAttr", monsterInfoData, ""));

                MapleData monsterSkillData = monsterInfoData.getChildByPath("skill");
                if (monsterSkillData != null) {
                    int i = 0;
                    List<Pair<Integer, Integer>> skills = new ArrayList<>();
                    while (monsterSkillData.getChildByPath(Integer.toString(i)) != null) {
                        skills.add(
                            new Pair<>(
                                MapleDataTool.getInt(i + "/skill", monsterSkillData, 0),
                                MapleDataTool.getInt(i + "/level", monsterSkillData, 0)
                            )
                        );
                        i++;
                    }
                    stats.setSkills(skills, mid);
                }
            } catch (Exception e) {
                e.printStackTrace();
                monsterStats.put(mid, null);
                return null;
            }

            monsterStats.put(mid, stats);
        }
        return new MapleMonster(mid, stats);
    }

    public static void decodeElementalString (MapleMonsterStats stats, String elemAttr) {
        for (int i = 0; i < elemAttr.length(); i += 2) {
            Element e = Element.getFromChar(elemAttr.charAt(i));
            ElementalEffectiveness ee =
                ElementalEffectiveness.getByNumber(
                    Integer.valueOf(
                        String.valueOf(
                            elemAttr.charAt(i + 1)
                        )
                    )
                );
            stats.setEffectiveness(e, ee);
        }
    }

    public static MapleNPC getNPC(int nid) {
        return new MapleNPC(
            nid,
            new MapleNPCStats(
                MapleDataTool.getString(nid + "/name", npcStringData, "MISSINGNO")
            )
        );
    }

    public static Map<Integer, MapleMonsterStats> readMonsterStats() {
        return Collections.unmodifiableMap(monsterStats);
    }
}
