package net.sf.odinms.client;

import com.moandjiezana.toml.Toml;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.sf.odinms.tools.Pair;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class MapleCQuests {
    private static final Map<Integer, MapleCQuests> quests = new LinkedHashMap<>();
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
    private static boolean loadedAllQuests = false;
    private final int id;
    private final Map<Integer, Pair<Integer, String>> monsterTargets = new LinkedHashMap<>(4, 0.8f);
    private final Map<Integer, Pair<Integer, String>> itemsToCollect = new LinkedHashMap<>(4, 0.8f);
    private final Map<String, Integer> otherObjectives = new LinkedHashMap<>(4, 0.8f);
    private int expReward, mesoReward;
    private final Map<Integer, Integer> itemRewards = new LinkedHashMap<>(4, 0.8f);
    private final Set<Consumer<MapleCharacter>> otherRewards = new LinkedHashSet<>(2);
    private final Set<Integer> prereqQuests = new LinkedHashSet<>(3);
    private final Set<Predicate<MapleCharacter>> prereqs = new LinkedHashSet<>(3);
    private String startNpc, endNpc;
    private String title, info;
    private int adventuresome, valiant, fearless;
    private boolean repeatable = false;

    public MapleCQuests(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getStartNpc() {
        return startNpc;
    }

    public String getEndNpc() {
        return endNpc;
    }

    public boolean hasIdenticalStartEnd() {
        return startNpc.equals(endNpc);
    }

    public String getTitle() {
        return title;
    }

    public String getInfo() {
        return info;
    }

    /** Nullable */
    public Integer getNumberToKill(final int monsterId) {
        if (!monsterTargets.containsKey(monsterId)) return null;
        return monsterTargets.get(monsterId).getLeft();
    }

    /** Nullable */
    public Integer getNumberToCollect(final int itemId) {
        if (!itemsToCollect.containsKey(itemId)) return null;
        return itemsToCollect.get(itemId).getLeft();
    }

    /** Nullable */
    public Integer getNumberOfOtherObjective(final String otherObjective) {
        if (!otherObjectives.containsKey(otherObjective)) return null;
        return otherObjectives.get(otherObjective);
    }

    public int getExpReward() {
        return expReward;
    }

    public int getMesoReward() {
        return mesoReward;
    }

    public Set<Predicate<MapleCharacter>> readPrereqs() {
        return new LinkedHashSet<>(prereqs);
    }

    public Set<Integer> readPrereqQuests() {
        return new LinkedHashSet<>(prereqQuests);
    }

    public Map<Integer, Pair<Integer, String>> readMonsterTargets() {
        return new LinkedHashMap<>(monsterTargets);
    }

    public Map<Integer, Pair<Integer, String>> readItemsToCollect() {
        return new LinkedHashMap<>(itemsToCollect);
    }

    public Map<String, Integer> readOtherObjectives() {
        return new LinkedHashMap<>(otherObjectives);
    }

    public Map<Integer, Integer> readItemRewards() {
        return new LinkedHashMap<>(itemRewards);
    }

    public Set<Consumer<MapleCharacter>> readOtherRewards() {
        return new LinkedHashSet<>(otherRewards);
    }

    /** Nullable */
    public String getTargetName(final int monsterId) {
        if (!monsterTargets.containsKey(monsterId)) return null;
        return monsterTargets.get(monsterId).getRight();
    }

    /** Nullable */
    public String getItemName(final int itemId) {
        if (!itemsToCollect.containsKey(itemId)) return null;
        return itemsToCollect.get(itemId).getRight();
    }

    public boolean requiresTarget(final int monsterId) {
        return monsterTargets.containsKey(monsterId);
    }

    public boolean requiresItem(final int itemId) {
        return itemsToCollect.containsKey(itemId);
    }

    public boolean requiresOtherObjective(final String otherObjective) {
        return otherObjectives.containsKey(otherObjective);
    }

    public boolean requiresMonsterTargets() {
        return !monsterTargets.isEmpty();
    }

    public boolean requiresItemCollection() {
        return !itemsToCollect.isEmpty();
    }

    public boolean hasPrereqs() {
        return !prereqs.isEmpty() || !prereqQuests.isEmpty();
    }

    public boolean requiresOtherObjectives() {
        return !otherObjectives.isEmpty();
    }

    public int getAdventuresome() {
        return adventuresome;
    }

    public int getValiant() {
        return valiant;
    }

    public int getFearless() {
        return fearless;
    }

    public boolean canComplete(final CQuest cq) {
        return
            id != 0 &&
            monsterTargets
                .entrySet()
                .stream()
                .allMatch(e -> cq.getQuestKills(e.getKey()) >= e.getValue().getLeft()) &&
            itemsToCollect
                .entrySet()
                .stream()
                .allMatch(e -> cq.getPlayer().getQuestCollected(e.getKey()) >= e.getValue().getLeft()) &&
            otherObjectives
                .entrySet()
                .stream()
                .allMatch(e -> cq.getObjectiveProgress(e.getKey()) >= e.getValue());
    }

    public boolean meetsPrereqs(final MapleCharacter c) {
        return
            prereqs.stream().allMatch(p -> p.test(c)) &&
            prereqQuests.stream().allMatch(c::completedCQuest);
    }

    public CQuestStatus getCompletionLevel(final int playerLevel) {
        if (playerLevel > adventuresome) return CQuestStatus.UNIMPRESSED;
        if (playerLevel > valiant) return CQuestStatus.ADVENTURESOME;
        if (playerLevel > fearless) return CQuestStatus.VALIANT;
        return CQuestStatus.FEARLESS;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    /** Nullable */
    public static synchronized MapleCQuests loadQuest(final int questId) {
        if (quests.containsKey(questId)) return quests.get(questId);
        return readQuest(questId);
    }

    /** Nullable */
    private static MapleCQuests readQuest(final int id) {
        final MapleCQuests q = new MapleCQuests(id);
        try {
            final Toml t = new Toml().parse(new FileInputStream("quests/" + id + ".toml"));

            q.title = t.getString("title");
            q.startNpc = t.getString("npc");
            q.endNpc = t.getString("endnpc");
            if (q.endNpc == null) q.endNpc = q.startNpc;
            // Data is stored using Unix line endings (LF) because that's our locale,
            // but the client expects Windows-style (CRLF), so we replace the line
            // endings here since clients will have to display `info`.
            q.info = t.getString("info").replaceAll("\n", "\r\n");
            final Boolean repeatable = t.getBoolean("repeatable");
            if (repeatable != null) q.repeatable = repeatable;

            final Toml reqTable = t.getTable("requirements");
            if (reqTable != null && !reqTable.isEmpty()) {
                reqTable
                    .getList("quests")
                    .stream()
                    .map(i -> ((Long) i).intValue())
                    .forEach(q.prereqQuests::add);
                reqTable
                    .getList("predicates")
                    .stream()
                    .map(s -> {
                        final String code = (String) s;
                        try {
                            ScriptObjectMirror objMirror =
                                (ScriptObjectMirror) engine.eval(code);
                            @SuppressWarnings("unchecked")
                            final Predicate<MapleCharacter> p = objMirror.to(Predicate.class);
                            return p;
                        } catch (Exception e) {
                            System.err.println("Failed to load MapleCQuests " + id);
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .forEach(q.prereqs::add);
                if (q.prereqs.contains(null)) return null;
            }

            t.getTables("monsters").forEach(mt ->
                q.monsterTargets.put(
                    mt.getLong("id").intValue(),
                    new Pair<>(
                        mt.getLong("count").intValue(),
                        mt.getString("name")
                    )
                )
            );

            t.getTables("items").forEach(it ->
                q.itemsToCollect.put(
                    it.getLong("id").intValue(),
                    new Pair<>(
                        it.getLong("count").intValue(),
                        it.getString("name")
                    )
                )
            );

            t.getTables("objectives").forEach(ot ->
                q.otherObjectives.put(
                    ot.getString("name"),
                    ot.getLong("count").intValue()
                )
            );

            final Toml rewardsTable = t.getTable("rewards");
            if (rewardsTable != null && !rewardsTable.isEmpty()) {
                final Long exp = rewardsTable.getLong("exp");
                if (exp != null) q.expReward = exp.intValue();
                final Long mesos = rewardsTable.getLong("mesos");
                if (mesos != null) q.mesoReward = mesos.intValue();
                rewardsTable.getTables("items").forEach(it ->
                    q.itemRewards.put(
                        it.getLong("id").intValue(),
                        it.getLong("count").intValue()
                    )
                );
                rewardsTable
                    .getList("consumers")
                    .stream()
                    .map(s -> {
                        final String code = (String) s;
                        try {
                            final ScriptObjectMirror objMirror =
                                (ScriptObjectMirror) engine.eval(code);
                            @SuppressWarnings("unchecked")
                            final Consumer<MapleCharacter> c = objMirror.to(Consumer.class);
                            return c;
                        } catch (Exception e) {
                            System.err.println("Failed to load MapleCQuests " + id);
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .forEach(q.otherRewards::add);
                if (q.otherRewards.contains(null)) return null;
            }

            final Toml parTable = t.getTable("par");
            if (parTable != null && !parTable.isEmpty()) {
                q.fearless = parTable.getLong("fearless").intValue();
                q.valiant = parTable.getLong("valiant").intValue();
                q.adventuresome = parTable.getLong("adventuresome").intValue();
            }
        } catch (final Exception e) {
            System.err.println("Failed to load MapleCQuests " + id);
            e.printStackTrace();
            return null;
        }
        quests.put(id, q);
        return q;
    }

    public static boolean questExists(final int questId) {
        try {
            final FileInputStream fis = new FileInputStream("quests/" + questId + ".toml");
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    public static Collection<MapleCQuests> getAllQuests() {
        try {
            loadAllQuests();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return getCachedQuests();
    }

    public static Map<Integer, MapleCQuests> getAllQuestsMap() {
        try {
            loadAllQuests();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return getQuestCache();
    }

    public static Set<Integer> getAllQuestIds() {
        try {
            loadAllQuests();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return getCachedQuestIds();
    }

    public static Collection<MapleCQuests> getCachedQuests() {
        return Collections.unmodifiableCollection(quests.values());
    }

    public static Map<Integer, MapleCQuests> getQuestCache() {
        return Collections.unmodifiableMap(quests);
    }

    public static Set<Integer> getCachedQuestIds() {
        return Collections.unmodifiableSet(quests.keySet());
    }

    private static synchronized void loadAllQuests() throws FileNotFoundException {
        if (loadedAllQuests) return;
        final File[] fileList = new File("quests/").listFiles();
        if (fileList == null) {
            throw new FileNotFoundException("quests/ is not a valid directory");
        }
        final Pattern filePattern = Pattern.compile("[0-9]+\\.toml");
        Arrays
            .stream(fileList)
            .filter(f -> filePattern.matcher(f.getName()).matches())
            .map(f ->
                Integer.parseInt(
                    f.getName().replace(".toml", "")
                )
            )
            .forEach(MapleCQuests::loadQuest);
        loadedAllQuests = true;
    }

    public static void clearCache() {
        quests.clear();
        loadedAllQuests = false;
    }
}
