package net.sf.odinms.client;

import net.sf.odinms.scripting.AbstractPlayerInteraction;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.StringUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * This class is an extension of the functionality of {@link MapleCharacter}
 * to make managing custom quests easier.
 * </p>
 *
 * <p>
 * {@code CQuests} are held in the "slots" of an {@link ArrayList} in the
 * corresponding {@link MapleCharacter}, so that each one corresponds to
 * a quest slot.
 * </p>
 *
 * <p>
 * {@code CQuests} maintain a reference to their corresponding
 * {@link MapleCharacter} in order to do some operations on it (mostly
 * {@link MapleCharacter#sendHint} and {@link MapleCharacter#dropMessage}).
 * </p>
 */
public class CQuest {
    private MapleCQuests quest;
    private final MapleCharacter player;
    private final Map<Integer, Integer> questKills = new LinkedHashMap<>(4, 0.8f);
    private final Map<String, Integer> otherObjectiveProgress = new LinkedHashMap<>(4, 0.8f);
    private int effectivePlayerLevel;

    public CQuest(MapleCharacter player) {
        this.player = player;
    }

    public void closeQuest() {
        loadQuest(0);
    }

    public void loadQuest(int questId) {
        loadQuest(questId, 0);
    }

    public void loadQuest(int questId, int effectivePlayerLevel) {
        quest = MapleCQuests.loadQuest(questId);
        resetProgress();
        this.effectivePlayerLevel = effectivePlayerLevel;
        for (Integer mobId : quest.readMonsterTargets().keySet()) {
            questKills.put(mobId, 0);
        }
        for (String objective : quest.readOtherObjectives().keySet()) {
            otherObjectiveProgress.put(objective, 0);
        }
        player.updateQuestEffectiveLevel();
    }

    public MapleCQuests getQuest() {
        return quest;
    }

    /** Returns 0 when there is an actual 0 or if there is no value. */
    public int getQuestKills(int mobId) {
        Integer kills = questKills.get(mobId);
        return kills != null ? kills : 0;
    }

    public Map<Integer, Integer> readQuestKills() {
        return new LinkedHashMap<>(questKills);
    }

    /** Returns 0 when there is an actual 0 or if there is no value. */
    public int getObjectiveProgress(String obj) {
        Integer count = otherObjectiveProgress.get(obj);
        return count != null ? count : 0;
    }

    public Map<String, Integer> readObjectiveProgress() {
        return new LinkedHashMap<>(otherObjectiveProgress);
    }

    /**
     * Returns old quest kill value for the given monster
     * if present, otherwise {@code null}.
     */
    public Integer setQuestKill(int mobId, int killCount) {
        return questKills.put(mobId, killCount);
    }

    /**
     * Returns new quest kill value for the given monster if
     * an entry for that monster already exists, otherwise
     * this is a no-op and returns {@code null}.
     *
     * @see CQuest#incrementQuestKill
     */
    public Integer doQuestKill(int mobId) {
        return incrementQuestKill(mobId, 1);
    }

    /**
     * Returns new quest kill value for the given monster if
     * an entry for that monster already exists, otherwise
     * this is a no-op and returns {@code null}.
     */
    public Integer incrementQuestKill(int mobId, final int amount) {
        Integer ret = questKills.computeIfPresent(mobId, (mid, oldVal) -> oldVal + amount);
        if (ret != null && getQuestKills(mobId) <= quest.getNumberToKill(mobId)) {
            player.sendHint(
                "#e" +
                    quest.getTargetName(mobId) +
                    ": " +
                    (getQuestKills(mobId) == quest.getNumberToKill(mobId) ? "#g" : "#r") +
                    getQuestKills(mobId) +
                    " #k/ " +
                    quest.getNumberToKill(mobId)
            );
            if (getQuestKills(mobId) == quest.getNumberToKill(mobId) && canComplete()) {
                TimerManager.getInstance().schedule(() -> {
                    player.sendHint("#eReturn to the NPC: " + quest.getEndNpc());
                    player.dropMessage("Return to the NPC: " + quest.getEndNpc());
                }, 1500L);
            }
        }
        return ret;
    }

    /**
     * Returns old objective progress value for the given objective
     * if present, otherwise {@code null}.
     */
    public Integer setObjectiveProgress(String objective, int count) {
        return otherObjectiveProgress.put(objective, count);
    }

    /**
     * Returns new objective progress value for the given objective
     * if an entry for that objective already exists, otherwise
     * this is a no-op and returns {@code null}.
     *
     * @see CQuest#incrementObjectiveProgress
     */
    public Integer doObjectiveProgress(String objective) {
        return incrementObjectiveProgress(objective, 1);
    }

    /**
     * Returns new objective progress value for the given objective
     * if an entry for that objective already exists, otherwise
     * this is a no-op and returns {@code null}.
     */
    public Integer incrementObjectiveProgress(String objective, final int amount) {
        Integer ret = otherObjectiveProgress.computeIfPresent(objective, (obj, oldVal) -> oldVal + amount);
        if (ret != null && getObjectiveProgress(objective) <= quest.getNumberOfOtherObjective(objective)) {
            player.sendHint(
                "#e" +
                    objective +
                    ": " +
                    (getObjectiveProgress(objective) == quest.getNumberOfOtherObjective(objective) ?
                        "#g" :
                        "#r") +
                    getObjectiveProgress(objective) +
                    " #k/ " +
                    quest.getNumberOfOtherObjective(objective)
            );
        }
        return ret;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void resetProgress() {
        questKills.clear();
        otherObjectiveProgress.clear();
    }

    public boolean canComplete() {
        return quest.canComplete(this);
    }

    public void complete() {
        final MapleClient c = player.getClient();
        final AbstractPlayerInteraction api = new AbstractPlayerInteraction(c);
        final boolean firstTime = !player.completedCQuest(quest.getId()) || quest.isRepeatable();
        double expMulti = (double) player.getExpEffectiveLevel() / (firstTime ? 10.0d : 20.0d);
        player.gainExp((int) (quest.getExpReward() * expMulti), true, true);
        player.gainMeso(quest.getMesoReward() / (firstTime ? 1 : 2), true, false, true);
        quest.readItemsToCollect().forEach((itemId, qtyAndName) -> api.gainItem(itemId, (short) -qtyAndName.getLeft()));
        if (firstTime) {
            quest.readItemRewards().forEach((itemId, count) -> api.gainItem(itemId, count.shortValue()));
            quest.readOtherRewards().forEach(consumer -> consumer.accept(player));
        }
        CQuestStatus completionLevel =
            quest.getCompletionLevel(
                effectivePlayerLevel > 0 ?
                    effectivePlayerLevel :
                    player.getLevel()
            );
        player.setCQuestCompleted(quest.getId(), completionLevel);
        c.getSession().write(MaplePacketCreator.playSound("Dojan/clear"));
        c.getSession().write(MaplePacketCreator.showEffect("dojang/end/clear"));
        String completionColor;
        switch (completionLevel) {
            case FEARLESS:
                completionColor = "#d";
                break;
            case VALIANT:
                completionColor = "#b";
                break;
            case ADVENTURESOME:
                completionColor = "#r";
                break;
            default:
                completionColor = "";
        }
        player.sendHint(
            "Quest complete: #e" +
                quest.getTitle() +
                "#n\r\n\r\nCompletion level: #e" +
                completionColor +
                StringUtil.makeEnumHumanReadable(completionLevel.name())
        );
        closeQuest();
    }

    public boolean hasCompletedGoal(int mobId, int itemId, String objective) {
        if (mobId > 0) {
            Integer req = quest.getNumberToKill(mobId);
            return req != null && getQuestKills(mobId) >= req;
        }
        if (itemId > 0) {
            Integer req = quest.getNumberToCollect(itemId);
            return req != null && player.getQuestCollected(itemId) >= req;
        }
        if (objective != null && !objective.equals("")) {
            Integer req = quest.getNumberOfOtherObjective(objective);
            return req != null && getObjectiveProgress(objective) >= req;
        }
        return false;
    }

    public void reload() {
        int id = quest.getId();
        loadQuest(id, effectivePlayerLevel);
    }

    public void setEffectivePlayerLevel(int level) {
        int id = quest.getId();
        loadQuest(id, level);
    }

    public void resetEffectivePlayerLevel() {
        setEffectivePlayerLevel(0);
    }

    public int getEffectivePlayerLevel() {
        return effectivePlayerLevel;
    }

    public boolean canAdvance() {
        if (effectivePlayerLevel < 1) return true;
        if (player.getQuestEffectiveLevel() > 0) {
            return player.getQuestEffectiveLevel() <= effectivePlayerLevel;
        }
        return player.getLevel() <= effectivePlayerLevel;
    }

    public void softReload() {
        final int questId = quest.getId();
        quest = MapleCQuests.loadQuest(questId);
        player.updateQuestEffectiveLevel();
    }
}
