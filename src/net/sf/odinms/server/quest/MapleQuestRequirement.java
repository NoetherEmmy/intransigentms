package net.sf.odinms.server.quest;

import net.sf.odinms.client.*;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.MapleItemInformationProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MapleQuestRequirement {
    private final MapleQuestRequirementType type;
    private final MapleData data;
    private final MapleQuest quest;

    /** Creates a new instance of MapleQuestRequirement */
    public MapleQuestRequirement(final MapleQuest quest, final MapleQuestRequirementType type, final MapleData data) {
        this.type = type;
        this.data = data;
        this.quest = quest;
    }

    boolean check(final MapleCharacter c, final Integer npcid) {
        switch (type) {
            case JOB:
                if (data == null) {
                    //System.err.println("getData() == null in MapleQuestRequirement.check(), case JOB:, quest: " + quest.getId());
                    return false;
                }
                try {
                    for (final MapleData jobEntry : data.getChildren()) {
                        if (c.getJob().equals(MapleJob.getById(MapleDataTool.getInt(jobEntry))) || c.isGM()) {
                            return true;
                        }
                    }
                } catch (final Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case JOB:, quest: " + quest.getId() + ", data: " + getData().getName());
                    return false;
                }
                return false;
            case QUEST:
                if (data == null) {
                    //System.err.println("getData() == null in MapleQuestRequirement.check(), case QUEST:, quest: " + quest.getId());
                    return false;
                }
                try {
                    for (final MapleData questEntry : data.getChildren()) {
                        final MapleData iddata = questEntry.getChildByPath("id");
                        final MapleData statedata = questEntry.getChildByPath("state");
                        if (iddata == null) {
                            //System.err.println("Getting quest ID failed in MapleQuestRequirement.check(): " + questEntry.getName());
                            return false;
                        }
                        if (statedata == null) {
                            System.err.println("Getting quest state failed in MapleQuestRequirement.check(): " + questEntry.getName());
                            return false;
                        }
                        final MapleQuestStatus q = c.getQuest(MapleQuest.getInstance(MapleDataTool.getInt(questEntry.getChildByPath("id"))));
                        final MapleQuestStatus.Status s =
                            MapleQuestStatus.Status.getById(MapleDataTool.getInt(questEntry.getChildByPath("state")));
                        if (q == null && s != null && s.equals(MapleQuestStatus.Status.NOT_STARTED)) {
                            continue;
                        }
                        if (q == null || !q.getStatus().equals(MapleQuestStatus.Status.getById(MapleDataTool.getInt(questEntry.getChildByPath("state"))))) {
                            return false;
                        }
                    }
                } catch (final Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case QUEST:, quest: " + quest.getId() + ", data: " + getData().getName());
                    return false;
                }
                return true;
            case ITEM:
                if (data == null) {
                    //System.err.println("getData() == null in MapleQuestRequirement.check(), case ITEM:, quest: " + quest.getId());
                    return false;
                }
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                try {
                    for (final MapleData itemEntry : data.getChildren()) {
                        final int itemId = MapleDataTool.getInt(itemEntry.getChildByPath("id"));
                        final short quantity;
                        final MapleInventoryType iType = ii.getInventoryType(itemId);
                        quantity = (short) c.getInventory(iType).listById(itemId).stream().mapToInt(IItem::getQuantity).sum();
                        if (
                            quantity < MapleDataTool.getInt(itemEntry.getChildByPath("count")) ||
                            MapleDataTool.getInt(itemEntry.getChildByPath("count")) <= 0 &&
                            quantity > 0
                        ) {
                            return false;
                        }
                    }
                } catch (final Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case ITEM:, quest: " + quest.getId() + ", data: " + getData().getName());
                    return false;
                }
                return true;
            case MIN_LEVEL:
                try {
                    return c.getLevel() >= MapleDataTool.getInt(data);
                } catch (final Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case MIN_LEVEL:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            case MAX_LEVEL:
                try {
                    return c.getLevel() <= MapleDataTool.getInt(data);
                } catch (final Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case MAX_LEVEL:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            case END_DATE:
                try {
                    final String timeStr = MapleDataTool.getString(data);
                    final Calendar cal = Calendar.getInstance();
                    final int date = Integer.parseInt(timeStr.substring(4, 6));
                    cal.set(Integer.parseInt(timeStr.substring(0, 4)), date, Integer.parseInt(timeStr.substring(6, 8)), Integer.parseInt(timeStr.substring(8, 10)), 0);
                    return cal.getTimeInMillis() >= System.currentTimeMillis();
                } catch (final Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case END_DATE:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            case MOB:
                if (data == null) {
                    //System.err.println("getData() == null in MapleQuestRequirement.check(), case MOB:, quest: " + quest.getId());
                    return false;
                }
                try {
                    for (final MapleData mobEntry : data.getChildren()) {
                        final int mobId = MapleDataTool.getInt(mobEntry.getChildByPath("id"));
                        final int killReq = MapleDataTool.getInt(mobEntry.getChildByPath("count"));
                        if (c.getQuest(quest).getMobKills(mobId) < killReq) {
                            return false;
                        }
                    }
                } catch (final Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case MOB:, quest: " + quest.getId() + ", data: " + getData().getName());
                    return false;
                }
                return true;
            case NPC:
                try {
                    return npcid == null || npcid == MapleDataTool.getInt(data);
                } catch (final Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case NPC:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            case FIELD_ENTER:
                try {
                    final MapleData zeroField = data.getChildByPath("0");
                    return zeroField != null && MapleDataTool.getInt(zeroField) == c.getMapId();
                } catch (final Exception e) {
                    //System.err.println(e + " in MapleQuestRequirement.check(), case FIELD_ENTER:, quest: " + quest.getId() + ", data: " + (getData() != null ? getData().getName() : "NULL"));
                    return false;
                }
            case INTERVAL:
                try {
                    return !c.getQuest(quest).getStatus().equals(MapleQuestStatus.Status.COMPLETED) || c.getQuest(quest).getCompletionTime() <= System.currentTimeMillis() - MapleDataTool.getInt(data) * 60 * 1000;
                } catch (final Exception e) {
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
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final List<Integer> delta =
            data.getChildren()
                .stream()
                .mapToInt(itemEntry -> MapleDataTool.getInt(itemEntry.getChildByPath("id")))
                .filter(ii::isQuestItem)
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        return Collections.unmodifiableList(delta);
    }
}
