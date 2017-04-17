package net.sf.odinms.server.quest;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleQuestStatus;
import net.sf.odinms.client.MapleQuestStatus.Status;
import net.sf.odinms.net.world.WorldServer;
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
    protected List<MapleQuestRequirement> startReqs, completeReqs;
    protected List<MapleQuestAction> startActs, completeActs;
    protected final List<Integer> relevantMobs = new ArrayList<>();
    private boolean autoStart, autoPreComplete;
    private boolean repeatable = false;
    private String name = "MISSINGNO";
    private static final MapleDataProvider questData =
        MapleDataProviderFactory.getDataProvider(
            new File(System.getProperty(WorldServer.WZPATH) + "/Quest.wz")
        );
    private static final MapleData
        actions = questData.getData("Act.img"),
        requirements = questData.getData("Check.img"),
        info = questData.getData("QuestInfo.img");
    protected static final Logger log = LoggerFactory.getLogger(MapleQuest.class);

    protected MapleQuest() {
    }

    private MapleQuest(final int id) {
        this.id = id;
        // Read requirements
        final MapleData startReqData = requirements.getChildByPath(String.valueOf(id)).getChildByPath("0");
        startReqs = new ArrayList<>();
        if (startReqData != null) {
            for (final MapleData startReq : startReqData.getChildren()) {
                final MapleQuestRequirementType type = MapleQuestRequirementType.getByWZName(startReq.getName());
                if (type.equals(MapleQuestRequirementType.INTERVAL)) repeatable = true;
                final MapleQuestRequirement req = new MapleQuestRequirement(this, type, startReq);
                if (req.getType().equals(MapleQuestRequirementType.MOB)) {
                    for (final MapleData mob : startReq.getChildren()) {
                        relevantMobs.add(MapleDataTool.getInt(mob.getChildByPath("id")));
                    }
                }
                startReqs.add(req);
            }
        }
        final MapleData completeReqData = requirements.getChildByPath(String.valueOf(id)).getChildByPath("1");
        completeReqs = new ArrayList<>();
        if (completeReqData != null) {
            for (final MapleData completeReq : completeReqData.getChildren()) {
                final MapleQuestRequirement req =
                    new MapleQuestRequirement(
                        this,
                        MapleQuestRequirementType.getByWZName(
                            completeReq.getName()
                        ),
                        completeReq
                    );
                if (req.getType().equals(MapleQuestRequirementType.MOB)) {
                    for (final MapleData mob : completeReq.getChildren()) {
                        relevantMobs.add(MapleDataTool.getInt(mob.getChildByPath("id")));
                    }
                }
                completeReqs.add(req);
            }
        }
        final MapleData startActData = actions.getChildByPath(String.valueOf(id)).getChildByPath("0");
        startActs = new ArrayList<>();
        if (startActData != null) {
            for (final MapleData startAct : startActData.getChildren()) {
                final MapleQuestActionType questActionType = MapleQuestActionType.getByWZName(startAct.getName());
                startActs.add(new MapleQuestAction(questActionType, startAct, this));
            }
        }
        final MapleData completeActData = actions.getChildByPath(String.valueOf(id)).getChildByPath("1");
        completeActs = new ArrayList<>();
        if (completeActData != null) {
            for (final MapleData completeAct : completeActData.getChildren()) {
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
        final MapleData questInfo = info.getChildByPath(String.valueOf(id));
        autoStart = MapleDataTool.getInt("autoStart", questInfo, 0) == 1;
        autoPreComplete = MapleDataTool.getInt("autoPreComplete", questInfo, 0) == 1;
        name = MapleDataTool.getString("name", questInfo, "MISSINGNO");
    }

    public static void clearQuests() {
        quests.clear();
    }

    public static MapleQuest getInstance(final int id) {
        final MapleQuest ret;
        if (!quests.containsKey(id)) {
            try {
                if (id > 99999) {
                    ret = new MapleCustomQuest(id);
                } else {
                    ret = new MapleQuest(id);
                }
            } catch (final NullPointerException npe) {
                System.err.println("Bad quest data or no quest data at all for ID " + id);
                quests.put(id, null);
                return null;
            }
            quests.put(id, ret);
        } else {
            return quests.get(id);
        }
        return ret;
    }

    private boolean canStart(final MapleCharacter c, final Integer npcid) {
        if (
            c.getQuest(this).getStatus() != Status.NOT_STARTED &&
            !(c.getQuest(this).getStatus() == Status.COMPLETED && repeatable)
        ) {
            return false;
        }
        for (final MapleQuestRequirement r : startReqs) {
            if (!r.check(c, npcid)) return false;
        }
        return true;
    }

    public boolean canComplete(final MapleCharacter c, final Integer npcid) {
        if (!c.getQuest(this).getStatus().equals(Status.STARTED)) return false;
        for (final MapleQuestRequirement r : completeReqs) {
            if (!r.check(c, npcid)) return false;
        }
        return true;
    }

    public List<MapleQuestRequirement> getCompleteReqs() {
        return completeReqs;
    }

    public void start(final MapleCharacter c, final int npc) {
        if ((autoStart || checkNPCOnMap(c, npc)) && canStart(c, npc)) {
            for (final MapleQuestAction a : startActs) {
                a.run(c, null);
            }
            final MapleQuestStatus oldStatus = c.getQuest(this);
            final MapleQuestStatus newStatus = new MapleQuestStatus(this, MapleQuestStatus.Status.STARTED, npc);
            newStatus.setCompletionTime(oldStatus.getCompletionTime());
            newStatus.setForfeited(oldStatus.getForfeited());
            c.updateQuest(newStatus);
        }
    }

    public void complete(final MapleCharacter c, final int npc) {
        complete(c, npc, null);
    }

    public void complete(final MapleCharacter c, final int npc, final Integer selection) {
        if ((autoPreComplete || checkNPCOnMap(c, npc)) && canComplete(c, npc)) {
            for (final MapleQuestAction a : completeActs) {
                if (!a.check(c)) return;
            }
            for (final MapleQuestAction a : completeActs) {
                a.run(c, selection);
            }
            // We save forfeits only for logging purposes, they shouldn't matter anymore.
            // Completion time is set by the constructor.
            final MapleQuestStatus oldStatus = c.getQuest(this);
            final MapleQuestStatus newStatus = new MapleQuestStatus(this, MapleQuestStatus.Status.COMPLETED, npc);
            newStatus.setForfeited(oldStatus.getForfeited());
            c.updateQuest(newStatus);
        }
    }

    public void forfeit(final MapleCharacter c) {
        if (!c.getQuest(this).getStatus().equals(Status.STARTED)) return;
        final MapleQuestStatus oldStatus = c.getQuest(this);
        final MapleQuestStatus newStatus = new MapleQuestStatus(this, MapleQuestStatus.Status.NOT_STARTED);
        newStatus.setForfeited(oldStatus.getForfeited() + 1);
        newStatus.setCompletionTime(oldStatus.getCompletionTime());
        c.updateQuest(newStatus);
    }

    public void forceStart(final MapleCharacter c, final int npc) {
        final MapleQuestStatus oldStatus = c.getQuest(this);
        final MapleQuestStatus newStatus = new MapleQuestStatus(this, MapleQuestStatus.Status.STARTED, npc);
        newStatus.setForfeited(oldStatus.getForfeited());
        c.updateQuest(newStatus);
    }

    public void forceComplete(final MapleCharacter c, final int npc) {
        final MapleQuestStatus oldStatus = c.getQuest(this);
        final MapleQuestStatus newStatus = new MapleQuestStatus(this, MapleQuestStatus.Status.COMPLETED, npc);
        newStatus.setForfeited(oldStatus.getForfeited());
        c.updateQuest(newStatus);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Integer> getRelevantMobs() {
        return Collections.unmodifiableList(relevantMobs);
    }

    private boolean checkNPCOnMap(final MapleCharacter player, final int npcid) {
        return player.getMap().containsNPC(npcid);
    }

    public List<Integer> getQuestItemsToShowOnlyIfQuestIsActivated() {
        final Set<Integer> delta = new LinkedHashSet<>();
        for (final MapleQuestRequirement mqr : completeReqs) {
            if (mqr.getType() != MapleQuestRequirementType.ITEM) continue;
            delta.addAll(mqr.getQuestItemsToShowOnlyIfQuestIsActivated());
        }
        final List<Integer> returnThis = new ArrayList<>(delta);
        return Collections.unmodifiableList(returnThis);
    }

    @Override
    public String toString() {
        return "quest \"" + name + "\", id: " + id;
    }
}
