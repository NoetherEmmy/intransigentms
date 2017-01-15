package net.sf.odinms.scripting.quest;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleQuestStatus.Status;
import net.sf.odinms.scripting.npc.NPCConversationManager;
import net.sf.odinms.server.quest.MapleQuest;

public class QuestActionManager extends NPCConversationManager {
    private final boolean start;
    private final int quest;

    public QuestActionManager(MapleClient c, int npc, int quest, boolean start) {
        super(c, npc, null, null);
        this.quest = quest;
        this.start = start;
    }

    public int getQuest() {
        return quest;
    }

    public boolean isStart() {
        return start;
    }

    @Override
    public void dispose() {
        QuestScriptManager.getInstance().dispose(this, getClient());
    }

    public void forceStartQuest() {
        if (getQuestStatus(quest) != Status.COMPLETED) {
            forceStartQuest(quest);
        } else {
            dispose();
        }
    }

    public void forceStartQuest(int id) {
        if (getQuestStatus(id) != Status.COMPLETED) {
            MapleQuest.getInstance(id).forceStart(getPlayer(), getNpc());
        } else {
            dispose();
        }
    }

    public void forceCompleteQuest() {
            forceCompleteQuest(quest);
    }

    public void forceCompleteQuest(int id) {
            MapleQuest.getInstance(id).forceComplete(getPlayer(), getNpc());
    }
}
