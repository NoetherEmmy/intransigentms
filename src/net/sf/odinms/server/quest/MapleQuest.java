package net.sf.odinms.server.quest;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleQuestStatus;
import net.sf.odinms.client.MapleQuestStatus.Status;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class MapleQuest {
    private static final Map<Integer, MapleQuest> quests = new HashMap<>();
    protected int id;
    protected List<MapleQuestRequirement> startReqs;
    protected List<MapleQuestRequirement> completeReqs;
    protected List<MapleQuestAction> startActs;
    protected List<MapleQuestAction> completeActs;
    protected final List<Integer> relevantMobs = new ArrayList<>();
    private boolean autoStart, autoPreComplete;
    private boolean repeatable = false;
    private static final MapleDataProvider questData =
        MapleDataProviderFactory.getDataProvider(
            new File(System.getProperty("net.sf.odinms.wzpath") + "/Quest.wz")
        );
    private static final MapleData actions = questData.getData("Act.img");
    private static final MapleData requirements = questData.getData("Check.img");
    private static final MapleData info = questData.getData("QuestInfo.img");
    protected static final Logger log = LoggerFactory.getLogger(MapleQuest.class);

    protected MapleQuest() {
    }

    /** Creates a new instance of MapleQuest */
    private MapleQuest(int id) {
        this.id = id;
        // Read requirements
        MapleData startReqData = requirements.getChildByPath(String.valueOf(id)).getChildByPath("0");
        startReqs = new ArrayList<>();
        if (startReqData != null) {
            for (MapleData startReq : startReqData.getChildren()) {
                MapleQuestRequirementType type = MapleQuestRequirementType.getByWZName(startReq.getName());
                if (type.equals(MapleQuestRequirementType.INTERVAL))
                    repeatable = true;
                MapleQuestRequirement req = new MapleQuestRequirement(this, type, startReq);
                if (req.getType().equals(MapleQuestRequirementType.MOB)) {
                    for (MapleData mob : startReq.getChildren()) {
                        relevantMobs.add(MapleDataTool.getInt(mob.getChildByPath("id")));
                    }
                }
                startReqs.add(req);
            }
        }
        MapleData completeReqData = requirements.getChildByPath(String.valueOf(id)).getChildByPath("1");
        completeReqs = new ArrayList<>();
        if (completeReqData != null) {
            for (MapleData completeReq : completeReqData.getChildren()) {
                MapleQuestRequirement req =
                    new MapleQuestRequirement(
                        this,
                        MapleQuestRequirementType.getByWZName(
                            completeReq.getName()
                        ),
                        completeReq
                    );
                if (req.getType().equals(MapleQuestRequirementType.MOB)) {
                    for (MapleData mob : completeReq.getChildren()) {
                        relevantMobs.add(MapleDataTool.getInt(mob.getChildByPath("id")));
                    }
                }
                completeReqs.add(req);
            }
        }
        MapleData startActData = actions.getChildByPath(String.valueOf(id)).getChildByPath("0");
        startActs = new ArrayList<>();
        if (startActData != null) {
            for (MapleData startAct : startActData.getChildren()) {
                MapleQuestActionType questActionType = MapleQuestActionType.getByWZName(startAct.getName());
                startActs.add(new MapleQuestAction(questActionType, startAct, this));
            }
        }
        MapleData completeActData = actions.getChildByPath(String.valueOf(id)).getChildByPath("1");
        completeActs = new ArrayList<>();
        if (completeActData != null) {
            for (MapleData completeAct : completeActData.getChildren()) {
                completeActs.add(
                    new MapleQuestAction(
                        MapleQuestActionType.getByWZName(
                            completeAct.getName()
                        ),
                        completeAct,
                        this
                    )
                );
            }
        }
        MapleData questInfo = info.getChildByPath(String.valueOf(id));
        autoStart = MapleDataTool.getInt("autoStart", questInfo, 0) == 1;
        autoPreComplete = MapleDataTool.getInt("autoPreComplete", questInfo, 0) == 1;
    }

    public static void clearQuests() {
        quests.clear();
    }

    public static MapleQuest getInstance(int id) {
        MapleQuest ret = quests.get(id);
        if (ret == null) {
            if (id > 99999) {
                ret = new MapleCustomQuest(id);
            } else {
                ret = new MapleQuest(id);
            }
            quests.put(id, ret);
        }
        return ret;
    }

    private boolean canStart(MapleCharacter c, Integer npcid) {
        if (
            c.getQuest(this).getStatus() != Status.NOT_STARTED &&
            !(c.getQuest(this).getStatus() == Status.COMPLETED && repeatable)
        ) {
            return false;
        }
        for (MapleQuestRequirement r : startReqs) {
            if (!r.check(c, npcid)) return false;
        }
        return true;
    }

    public boolean canComplete(MapleCharacter c, Integer npcid) {
        if (!c.getQuest(this).getStatus().equals(Status.STARTED)) return false;
        for (MapleQuestRequirement r : completeReqs) {
            if (!r.check(c, npcid)) return false;
        }
        return true;
    }

    public List<MapleQuestRequirement> getCompleteReqs() {
        return completeReqs;
    }

    public void start(MapleCharacter c, int npc) {
        if ((autoStart || checkNPCOnMap(c, npc)) && canStart(c, npc)) {
            for (MapleQuestAction a : startActs) {
                a.run(c, null);
            }
            MapleQuestStatus oldStatus = c.getQuest(this);
            MapleQuestStatus newStatus = new MapleQuestStatus(this, MapleQuestStatus.Status.STARTED, npc);
            newStatus.setCompletionTime(oldStatus.getCompletionTime());
            newStatus.setForfeited(oldStatus.getForfeited());
            c.updateQuest(newStatus);
        }
    }

    public void complete(MapleCharacter c, int npc) {
        complete(c, npc, null);
    }

    public void complete(MapleCharacter c, int npc, Integer selection) {
        if ((autoPreComplete || checkNPCOnMap(c, npc)) && canComplete(c, npc)) {
            for (MapleQuestAction a : completeActs) {
                if (!a.check(c)) {
                    return;
                }
            }
            for (MapleQuestAction a : completeActs) {
                a.run(c, selection);
            }
            // we save forfeits only for logging purposes, they shouldn't matter anymore
            // completion time is set by the constructor
            MapleQuestStatus oldStatus = c.getQuest(this);
            MapleQuestStatus newStatus = new MapleQuestStatus(this, MapleQuestStatus.Status.COMPLETED, npc);
            newStatus.setForfeited(oldStatus.getForfeited());
            c.updateQuest(newStatus);
        }
    }

    public void forfeit(MapleCharacter c) {
        if (!c.getQuest(this).getStatus().equals(Status.STARTED)) return;
        MapleQuestStatus oldStatus = c.getQuest(this);
        MapleQuestStatus newStatus = new MapleQuestStatus(this, MapleQuestStatus.Status.NOT_STARTED);
        newStatus.setForfeited(oldStatus.getForfeited() + 1);
        newStatus.setCompletionTime(oldStatus.getCompletionTime());
        c.updateQuest(newStatus);
    }

    public void forceStart(MapleCharacter c, int npc) {
        MapleQuestStatus oldStatus = c.getQuest(this);
        MapleQuestStatus newStatus = new MapleQuestStatus(this, MapleQuestStatus.Status.STARTED, npc);
        newStatus.setForfeited(oldStatus.getForfeited());
        c.updateQuest(newStatus);
    }

    public void forceComplete(MapleCharacter c, int npc) {
        MapleQuestStatus oldStatus = c.getQuest(this);
        MapleQuestStatus newStatus = new MapleQuestStatus(this, MapleQuestStatus.Status.COMPLETED, npc);
        newStatus.setForfeited(oldStatus.getForfeited());
        c.updateQuest(newStatus);
    }

    public int getId() {
        return id;
    }

    public List<Integer> getRelevantMobs() {
        return Collections.unmodifiableList(relevantMobs);
    }

    private boolean checkNPCOnMap(MapleCharacter player, int npcid) {
        return player.getMap().containsNPC(npcid);
    }

    public List<Integer> getQuestItemsToShowOnlyIfQuestIsActivated() {
        Set<Integer> delta = new LinkedHashSet<>();
        for (MapleQuestRequirement mqr : completeReqs) {
            if (mqr.getType() != MapleQuestRequirementType.ITEM) continue;
            delta.addAll(mqr.getQuestItemsToShowOnlyIfQuestIsActivated());
        }
        List<Integer> returnThis = new ArrayList<>(delta);
        return Collections.unmodifiableList(returnThis);
    }
}
