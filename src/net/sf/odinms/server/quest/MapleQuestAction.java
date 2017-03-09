package net.sf.odinms.server.quest;

import net.sf.odinms.client.*;
import net.sf.odinms.client.messages.ServernoticeMapleClientMessageCallback;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MapleQuestAction {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MapleQuestAction.class);
    private final MapleQuestActionType type;
    private final MapleData data;
    private final MapleQuest quest;

    /** Creates a new instance of MapleQuestAction */
    public MapleQuestAction(MapleQuestActionType type, MapleData data, MapleQuest quest) {
        this.type = type;
        this.data = data;
        this.quest = quest;
    }

    public boolean check(MapleCharacter c) {
        switch (type) {
            case MESO:
                int mesos =
                    MapleDataTool.getInt(data) *
                        ChannelServer
                            .getInstance(c.getClient().getChannel())
                            .getMesoRate();
                if (c.getMeso() + mesos < 0) return false;
                break;
        }
        return true;
    }

    private boolean canGetItem(MapleData item, MapleCharacter c) {
        if (item.getChildByPath("gender") != null) {
            int gender = MapleDataTool.getInt(item.getChildByPath("gender"));
            if (gender >= 0 && gender <= 1 && gender != c.getGender()) return false;
        }
        if (item.getChildByPath("job") != null) {
            int job = MapleDataTool.getInt(item.getChildByPath("job"));
            if (job < 100) {
                if (MapleJob.getBy5ByteEncoding(job).getId() / 100 != c.getJob().getId() / 100) {
                    return false;
                }
            } else {
                if (job != c.getJob().getId()) return false;
            }
        }
        return true;
    }

    public void run(MapleCharacter c, Integer extSelection) {
        MapleQuestStatus status;
        ServernoticeMapleClientMessageCallback snmcmc = new ServernoticeMapleClientMessageCallback(5, c.getClient());
        switch (type) {
            case EXP:
                status = c.getQuest(quest);
                if (status.getStatus() == MapleQuestStatus.Status.NOT_STARTED && status.getForfeited() > 0) {
                    break;
                }
                c.gainExp(
                    MapleDataTool.getInt(data) *
                        ChannelServer.getInstance(c.getClient().getChannel()).getExpRate() *
                        c.getAbsoluteXp(),
                    true,
                    true
                );
                break;
            case ITEM:
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                Map<Integer, Integer> props = new LinkedHashMap<>();
                for (MapleData iEntry : data.getChildren()) {
                    if (
                        iEntry.getChildByPath("prop") != null &&
                        MapleDataTool.getInt(iEntry.getChildByPath("prop")) != -1 &&
                        canGetItem(iEntry, c)
                    ) {
                        for (int i = 0; i < MapleDataTool.getInt(iEntry.getChildByPath("prop")); ++i) {
                            props.put(props.size(), MapleDataTool.getInt(iEntry.getChildByPath("id")));
                        }
                    }
                }
                int selection = 0, extNum = 0;
                if (!props.isEmpty()) {
                    Random r = new Random();
                    selection = props.get(r.nextInt(props.size()));
                }
                for (MapleData iEntry : data.getChildren()) {
                    if (!canGetItem(iEntry, c)) continue;
                    if (iEntry.getChildByPath("prop") != null) {
                        if (MapleDataTool.getInt(iEntry.getChildByPath("prop")) == -1) {
                            if (extSelection != extNum++) continue;
                        } else if (MapleDataTool.getInt(iEntry.getChildByPath("id")) != selection) {
                            continue;
                        }
                    }
                    if (MapleDataTool.getInt(iEntry.getChildByPath("count")) < 0) { // Remove items
                        int itemId = MapleDataTool.getInt(iEntry.getChildByPath("id"));
                        MapleInventoryType iType = ii.getInventoryType(itemId);
                        short quantity = (short) (MapleDataTool.getInt(iEntry.getChildByPath("count")) * -1);
                        try {
                            MapleInventoryManipulator.removeById(c.getClient(), iType, itemId, quantity, true, false);
                        } catch (InventoryException ie) {
                            // It's better to catch this here so we'll at least try to remove the other items
                            log.warn("[h4x] Completing " + quest + " without meeting the requirements", ie);
                        }
                        c.getClient()
                         .getSession()
                         .write(
                             MaplePacketCreator.getShowItemGain(
                                 itemId,
                                 (short) MapleDataTool.getInt(iEntry.getChildByPath("count")),
                                 true
                             )
                         );
                    } else { // Add items
                        int itemId = MapleDataTool.getInt(iEntry.getChildByPath("id"));
                        short quantity = (short) MapleDataTool.getInt(iEntry.getChildByPath("count"));
                        MapleInventoryManipulator.addById(c.getClient(), itemId, quantity, null, -1);
                        c.getClient().getSession().write(MaplePacketCreator.getShowItemGain(itemId, quantity, true));
                    }
                }
                break;
            //case NEXTQUEST:
                //int nextquest = MapleDataTool.getInt(data);
                //Need to somehow make the chat popup for the next quest...
                //break;
            case MESO:
                status = c.getQuest(quest);
                if (status.getStatus() == MapleQuestStatus.Status.NOT_STARTED && status.getForfeited() > 0) {
                    break;
                }
                c.gainMeso(
                    MapleDataTool.getInt(data) *
                        ChannelServer
                            .getInstance(c.getClient().getChannel())
                            .getMesoRate(),
                    true,
                    false,
                    true
                );
                break;
            case QUEST:
                for (MapleData qEntry : data) {
                    int questid = MapleDataTool.getInt(qEntry.getChildByPath("id"));
                    int stat = MapleDataTool.getInt(qEntry.getChildByPath("state"));
                    c.updateQuest(
                        new MapleQuestStatus(
                            MapleQuest.getInstance(questid),
                            MapleQuestStatus.Status.getById(stat)
                        )
                    );
                }
                break;
            case SKILL:
                for (MapleData sEntry : data) {
                    int skillid = MapleDataTool.getInt(sEntry.getChildByPath("id"));
                    int skillLevel = MapleDataTool.getInt(sEntry.getChildByPath("skillLevel"));
                    int masterLevel = MapleDataTool.getInt(sEntry.getChildByPath("masterLevel"));
                    ISkill skillObject = SkillFactory.getSkill(skillid);
                    boolean shouldLearn = false;
                    MapleData applicableJobs = sEntry.getChildByPath("job");
                    for (MapleData applicableJob : applicableJobs) {
                        MapleJob job = MapleJob.getById(MapleDataTool.getInt(applicableJob));
                        if (c.getJob() == job) {
                            shouldLearn = true;
                            break;
                        }
                    }
                    if (skillObject.isBeginnerSkill()) shouldLearn = true;
                    skillLevel = Math.max(skillLevel, c.getSkillLevel(skillObject));
                    masterLevel = Math.max(masterLevel, c.getMasterLevel(skillObject));
                    if (shouldLearn) {
                        c.changeSkillLevel(skillObject, skillLevel, masterLevel);
                        snmcmc.dropMessage(
                            "You have learned " +
                                SkillFactory.getSkillName(skillid) +
                                " with level " +
                                skillLevel +
                                " and with max level " +
                                masterLevel +
                                "."
                        );
                    }
                }
                break;
            case FAME:
                status = c.getQuest(quest);
                if (status.getStatus() == MapleQuestStatus.Status.NOT_STARTED && status.getForfeited() > 0) {
                    break;
                }
                c.addFame(MapleDataTool.getInt(data));
                c.updateSingleStat(MapleStat.FAME, c.getFame());
                int fameGain = MapleDataTool.getInt(data);
                c.getClient().getSession().write(MaplePacketCreator.getShowFameGain(fameGain));
                break;
            case BUFF:
                status = c.getQuest(quest);
                if (status.getStatus() == MapleQuestStatus.Status.NOT_STARTED && status.getForfeited() > 0) {
                    break;
                }
                MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
                mii.getItemEffect(MapleDataTool.getInt(data)).applyTo(c);
                break;
        }
    }

    public MapleQuestActionType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + ": " + data.getName();
    }
}
