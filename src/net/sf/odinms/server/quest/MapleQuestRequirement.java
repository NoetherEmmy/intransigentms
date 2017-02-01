package net.sf.odinms.server.quest;

import net.sf.odinms.client.*;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.MapleItemInformationProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class MapleQuestRequirement {
    private final MapleQuestRequirementType type;
    private final MapleData data;
    private final MapleQuest quest;

    /** Creates a new instance of MapleQuestRequirement */
    public MapleQuestRequirement(MapleQuest quest, MapleQuestRequirementType type, MapleData data) {
        this.type = type;
        this.data = data;
        this.quest = quest;
    }

    boolean check(MapleCharacter c, Integer npcid) {
        switch (getType()) {
            case JOB:
                if (getData() == null) {
                    //System.err.println("getData() == null in MapleQuestRequirement.check(), case JOB:, quest: " + quest.getId());
                    return false;
                }
                try {
                    for (MapleData jobEntry : getData().getChildren()) {
                        if (c.getJob().equals(MapleJob.getById(MapleDataTool.getInt(jobEntry))) || c.isGM()) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case JOB:, quest: " + quest.getId() + ", data: " + getData().getName());
                    return false;
                }
                return false;
            case QUEST:
                if (getData() == null) {
                    //System.err.println("getData() == null in MapleQuestRequirement.check(), case QUEST:, quest: " + quest.getId());
                    return false;
                }
                try {
                    for (MapleData questEntry : getData().getChildren()) {
                        MapleData iddata = questEntry.getChildByPath("id");
                        MapleData statedata = questEntry.getChildByPath("state");
                        if (iddata == null) {
                            System.err.println("Getting quest ID failed in MapleQuestRequirement.check(): " + questEntry.getName());
                            return false;
                        }
                        if (statedata == null) {
                            System.err.println("Getting quest state failed in MapleQuestRequirement.check(): " + questEntry.getName());
                            return false;
                        }
                        MapleQuestStatus q = c.getQuest(MapleQuest.getInstance(MapleDataTool.getInt(questEntry.getChildByPath("id"))));
                        try {
                            if (q == null && MapleQuestStatus.Status.getById(MapleDataTool.getInt(questEntry.getChildByPath("state"))).equals(MapleQuestStatus.Status.NOT_STARTED)) {
                                continue;
                            }
                        } catch (NullPointerException ignored) {
                        }
                        if (q == null || !q.getStatus().equals(MapleQuestStatus.Status.getById(MapleDataTool.getInt(questEntry.getChildByPath("state"))))) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case QUEST:, quest: " + quest.getId() + ", data: " + getData().getName());
                    return false;
                }
                return true;
            case ITEM:
                if (getData() == null) {
                    //System.err.println("getData() == null in MapleQuestRequirement.check(), case ITEM:, quest: " + quest.getId());
                    return false;
                }
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                try {
                    for (MapleData itemEntry : getData().getChildren()) {
                        int itemId = MapleDataTool.getInt(itemEntry.getChildByPath("id"));

                        short quantity = 0;
                        MapleInventoryType iType = ii.getInventoryType(itemId);
                        for (IItem item : c.getInventory(iType).listById(itemId)) {
                            quantity += item.getQuantity();
                        }
                        if (quantity < MapleDataTool.getInt(itemEntry.getChildByPath("count")) || MapleDataTool.getInt(itemEntry.getChildByPath("count")) <= 0 && quantity > 0) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case ITEM:, quest: " + quest.getId() + ", data: " + getData().getName());
                    return false;
                }
                return true;
            case MIN_LEVEL:
                try {
                    return c.getLevel() >= MapleDataTool.getInt(getData());
                } catch (Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case MIN_LEVEL:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            case MAX_LEVEL:
                try {
                    return c.getLevel() <= MapleDataTool.getInt(getData());
                } catch (Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case MAX_LEVEL:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            case END_DATE:
                try {
                    String timeStr = MapleDataTool.getString(getData());
                    Calendar cal = Calendar.getInstance();
                    cal.set(Integer.parseInt(timeStr.substring(0, 4)), Integer.parseInt(timeStr.substring(4, 6)), Integer.parseInt(timeStr.substring(6, 8)), Integer.parseInt(timeStr.substring(8, 10)), 0);
                    return cal.getTimeInMillis() >= System.currentTimeMillis();
                } catch (Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case END_DATE:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            case MOB:
                if (getData() == null) {
                    //System.err.println("getData() == null in MapleQuestRequirement.check(), case MOB:, quest: " + quest.getId());
                    return false;
                }
                try {
                    for (MapleData mobEntry : getData().getChildren()) {
                        int mobId = MapleDataTool.getInt(mobEntry.getChildByPath("id"));
                        int killReq = MapleDataTool.getInt(mobEntry.getChildByPath("count"));
                        if (c.getQuest(quest).getMobKills(mobId) < killReq) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case MOB:, quest: " + quest.getId() + ", data: " + getData().getName());
                    return false;
                }
                return true;
            case NPC:
                try {
                    return npcid == null || npcid == MapleDataTool.getInt(getData());
                } catch (Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case NPC:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            case FIELD_ENTER:
                try {
                    MapleData zeroField = getData().getChildByPath("0");
                    return zeroField != null && MapleDataTool.getInt(zeroField) == c.getMapId();
                } catch (Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case FIELD_ENTER:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            case INTERVAL:
                try {
                    return !c.getQuest(quest).getStatus().equals(MapleQuestStatus.Status.COMPLETED) || c.getQuest(quest).getCompletionTime() <= System.currentTimeMillis() - MapleDataTool.getInt(getData()) * 60 * 1000;
                } catch (Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case INTERVAL:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            //case PET:
            //case MIN_PET_TAMENESS:
            default:
                return true;
        }
    }

    public MapleQuestRequirementType getType() {
        return type;
    }

    public MapleData getData() {
        return data;
    }

    @Override
    public String toString() {
        return type + " " + data + " " + quest;
    }

    public List<Integer> getQuestItemsToShowOnlyIfQuestIsActivated() {
        if (type != MapleQuestRequirementType.ITEM) return null;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<Integer> delta = new ArrayList<>();
        for (MapleData itemEntry : getData().getChildren()) {
            int itemId = MapleDataTool.getInt(itemEntry.getChildByPath("id"));
            if (ii.isQuestItem(itemId)) delta.add(itemId);
        }
        return Collections.unmodifiableList(delta);
    }
}
