package net.sf.odinms.server.life;

import net.sf.odinms.net.world.WorldServer;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.provider.wz.MapleDataType;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.StringUtil;

import java.io.File;
import java.util.*;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public final class MapleLifeFactory {
    //private static final Logger log = LoggerFactory.getLogger(MapleMapFactory.class);
    private static final MapleDataProvider data =
        MapleDataProviderFactory.getDataProvider(
            new File(System.getProperty(WorldServer.WZPATH) + "/Mob.wz")
        );
    private static final MapleDataProvider stringDataWZ =
        MapleDataProviderFactory.getDataProvider(
            new File(System.getProperty(WorldServer.WZPATH) + "/String.wz")
        );
    private static final MapleData mobStringData = stringDataWZ.getData("Mob.img");
    private static final MapleData npcStringData = stringDataWZ.getData("Npc.img");
    private static final Map<Integer, MapleMonsterStats> monsterStats = new LinkedHashMap<>();

    public static AbstractLoadedMapleLife getLife(final int id, final String type) {
        if (type.equalsIgnoreCase("n")) {
            return getNPC(id);
        } else if (type.equalsIgnoreCase("m")) {
            return getMonster(id);
        } else {
            System.err.println("Unknown Life type: " + type);
            return null;
        }
    }

    public static MapleMonster getMonster(final int mid) {
        final MapleMonsterStats stats;
        if (monsterStats.containsKey(mid)) {
            stats = monsterStats.get(mid);
        } else {
            try {
                final MapleData monsterData =
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
                final MapleData monsterInfoData = monsterData.getChildByPath("info");
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
                } catch (final Exception e) {
                    stats.setName("MISSINGNO");
                    System.err.println(e + "   !ID!:  " + mid);
                }
                //
                stats.setBuffToGive(MapleDataTool.getIntConvert("buff", monsterInfoData, -1));
                stats.setExplosive(MapleDataTool.getIntConvert("explosiveReward", monsterInfoData, 0) > 0);
                stats.setAccuracy(MapleDataTool.getIntConvert("acc", monsterInfoData, 0));
                stats.setAvoid(MapleDataTool.getIntConvert("eva", monsterInfoData, 0));
                final MapleData firstAttackData = monsterInfoData.getChildByPath("firstAttack");
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
                    final MapleData hpTagColor = monsterInfoData.getChildByPath("hpTagColor");
                    final MapleData hpTagBgColor = monsterInfoData.getChildByPath("hpTagBgcolor");
                    if (hpTagBgColor == null || hpTagColor == null) {
                        /*
                        System.out.println(
                            "Monster " +
                                stats.getName() +
                                " (" +
                                mid +
                                ") flagged as boss without boss HP bars."
                        );
                        */
                        stats.setTagColor(0);
                        stats.setTagBgColor(0);
                    } else {
                        stats.setTagColor(MapleDataTool.getIntConvert("hpTagColor", monsterInfoData));
                        stats.setTagBgColor(MapleDataTool.getIntConvert("hpTagBgcolor", monsterInfoData));
                    }
                }
                for (final MapleData idata : monsterData) {
                    if (!idata.getName().equals("info")) {
                        final int delay =
                            idata
                                .getChildren()
                                .stream()
                                .mapToInt(pic -> MapleDataTool.getIntConvert("delay", pic, 0))
                                .sum();
                        stats.setAnimationTime(idata.getName(), delay);
                    }
                }

                final MapleData reviveInfo = monsterInfoData.getChildByPath("revive");
                if (reviveInfo != null) {
                    final List<Integer> revives = new ArrayList<>();
                    for (final MapleData data_ : reviveInfo) {
                        revives.add(MapleDataTool.getInt(data_));
                    }
                    stats.setRevives(revives);
                }

                decodeElementalString(stats, MapleDataTool.getString("elemAttr", monsterInfoData, ""));

                final MapleData monsterSkillData = monsterInfoData.getChildByPath("skill");
                if (monsterSkillData != null) {
                    int i = 0;
                    final List<Pair<Integer, Integer>> skills = new ArrayList<>();
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
            } catch (final Exception e) {
                e.printStackTrace();
                monsterStats.put(mid, null);
                return null;
            }

            monsterStats.put(mid, stats);
        }
        return stats == null ? null : new MapleMonster(mid, stats);
    }

    public static void decodeElementalString (final MapleMonsterStats stats, final String elemAttr) {
        for (int i = 0; i < elemAttr.length(); i += 2) {
            final Element e = Element.getFromChar(elemAttr.charAt(i));
            final ElementalEffectiveness ee =
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

    public static MapleNPC getNPC(final int nid) {
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
