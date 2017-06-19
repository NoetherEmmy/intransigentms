package net.sf.odinms.server.quest;

import net.sf.odinms.client.*;
import net.sf.odinms.client.messages.ServernoticeMapleClientMessageCallback;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.scripting.AbstractPlayerInteraction;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MapleQuestAction {
    //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MapleQuestAction.class);
    private final MapleQuestActionType type;
    private final MapleData data;
    private final MapleQuest quest;

    /** Creates a new instance of MapleQuestAction */
    public MapleQuestAction(final MapleQuestActionType type, final MapleData data, final MapleQuest quest) {
        this.type = type;
        this.data = data;
        this.quest = quest;
    }

    public boolean check(final MapleCharacter c) {
        switch (type) {
            case MESO:
                final int mesos =
                    MapleDataTool.getInt(data) *
                        ChannelServer
                            .getInstance(c.getClient().getChannel())
                            .getMesoRate();
                if (c.getMeso() + mesos < 0) return false;
                break;
        }
        return true;
    }

    private boolean canGetItem(final MapleData item, final MapleCharacter c) {
        if (item.getChildByPath("gender") != null) {
            final int gender = MapleDataTool.getInt(item.getChildByPath("gender"));
            if (gender >= 0 && gender <= 1 && gender != c.getGender()) return false;
        }
        if (item.getChildByPath("job") != null) {
            final int job = MapleDataTool.getInt(item.getChildByPath("job"));
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

    public void run(final MapleCharacter c, final Integer extSelection) {
        final MapleQuestStatus status;
        final ServernoticeMapleClientMessageCallback snmcmc = new ServernoticeMapleClientMessageCallback(5, c.getClient());
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
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final Map<Integer, Integer> props = new LinkedHashMap<>();
                for (final MapleData iEntry : data.getChildren()) {
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
                    final Random r = new Random();
                    selection = props.get(r.nextInt(props.size()));
                }
                final AbstractPlayerInteraction api = new AbstractPlayerInteraction(c.getClient());
                for (final MapleData iEntry : data.getChildren()) {
                    if (!canGetItem(iEntry, c)) continue;
                    if (iEntry.getChildByPath("prop") != null) {
                        if (MapleDataTool.getInt(iEntry.getChildByPath("prop")) == -1) {
                            if (extSelection != extNum++) continue;
                        } else if (MapleDataTool.getInt(iEntry.getChildByPath("id")) != selection) {
                            continue;
                        }
                    }
                    if (MapleDataTool.getInt(iEntry.getChildByPath("count")) < 0) { // Remove items
                        final int itemId = MapleDataTool.getInt(iEntry.getChildByPath("id"));
                        final MapleInventoryType iType = ii.getInventoryType(itemId);
                        final short quantity = (short) (MapleDataTool.getInt(iEntry.getChildByPath("count")) * -1);
                        try {
                            MapleInventoryManipulator.removeById(c.getClient(), iType, itemId, quantity, true, false);
                        } catch (final InventoryException ie) {
                            // It's better to catch this here so we'll at least try to remove the other items
                            System.err.println("[h4x] Completing " + quest + " without meeting the requirements");
                            ie.printStackTrace();
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
                        final int itemId = MapleDataTool.getInt(iEntry.getChildByPath("id"));
                        final short quantity = (short) MapleDataTool.getInt(iEntry.getChildByPath("count"));
                        if (!api.gainItem(itemId, quantity, false, true)) {
                            c.dropMessage(
                                6,
                                "Your inventory is full. Please make sure you have room in your inventory, " +
                                    "and then type @mapleadmin into chat to claim the item."
                            );
                        }
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
                for (final MapleData qEntry : data) {
                    final int questid = MapleDataTool.getInt(qEntry.getChildByPath("id"));
                    final int stat = MapleDataTool.getInt(qEntry.getChildByPath("state"));
                    c.updateQuest(
                        new MapleQuestStatus(
                            MapleQuest.getInstance(questid),
                            MapleQuestStatus.Status.getById(stat)
                        )
                    );
                }
                break;
            case SKILL:
                for (final MapleData sEntry : data) {
                    final int skillid = MapleDataTool.getInt(sEntry.getChildByPath("id"));
                    int skillLevel = MapleDataTool.getInt(sEntry.getChildByPath("skillLevel"));
                    int masterLevel = MapleDataTool.getInt(sEntry.getChildByPath("masterLevel"));
                    final ISkill skillObject = SkillFactory.getSkill(skillid);
                    boolean shouldLearn = false;
                    final MapleData applicableJobs = sEntry.getChildByPath("job");
                    for (final MapleData applicableJob : applicableJobs) {
                        final MapleJob job = MapleJob.getById(MapleDataTool.getInt(applicableJob));
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
                final int fameGain = MapleDataTool.getInt(data);
                c.getClient().getSession().write(MaplePacketCreator.getShowFameGain(fameGain));
                break;
            case BUFF:
                status = c.getQuest(quest);
                if (status.getStatus() == MapleQuestStatus.Status.NOT_STARTED && status.getForfeited() > 0) {
                    break;
                }
                final MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
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
