package net.sf.odinms.client;

import net.sf.odinms.tools.Pair;

import java.io.FileInputStream;
import java.util.*;

public class MapleCQuests {
    private int id;
    private final Map<Integer, Pair<Integer, String>> monsterTargets = new LinkedHashMap<>(4, 0.8f);
    private final Map<Integer, Pair<Integer, String>> itemsToCollect = new LinkedHashMap<>(4, 0.8f);
    private final Map<String, Integer> otherObjectives = new LinkedHashMap<>(4, 0.8f);
    private int expReward, mesoReward;
    private final Map<Integer, Integer> itemRewards = new LinkedHashMap<>(4, 0.8f);
    private final Set<Integer> prereqs = new TreeSet<>();
    private String startNpc, endNpc;
    private String title;
    private String info;
    private int dirt, bronze, silver, gold;

    public void loadQuest(int id) {
        this.id = id;
        final Properties p = new Properties();
        try {
            p.load(new FileInputStream("quests/" + id + ".ini"));

            monsterTargets.clear();
            int i = 1;
            String monsterTarget = p.getProperty("MonsterID" + i);
            while (monsterTarget != null && !monsterTarget.equals("") && Integer.parseInt(monsterTarget) != 0) {
                String toKill = p.getProperty("ToKill" + i);
                String mobName = p.getProperty("MonsterName" + i);
                if (toKill == null || mobName == null) {
                    throw new InvalidPropertiesFormatException(
                        "Number of MonsterID properties must match/correspond " +
                            "with number of ToKill & MonsterName properties"
                    );
                }
                monsterTargets.put(
                    Integer.parseInt(monsterTarget),
                    new Pair<>(Integer.parseInt(toKill), mobName)
                );
                i++;
                monsterTarget = p.getProperty("MonsterID" + i);
            }

            itemsToCollect.clear();
            i = 1;
            String itemToCollect = p.getProperty("CollectItemID" + i);
            while (itemToCollect != null && !itemToCollect.equals("") && Integer.parseInt(itemToCollect) != 0) {
                String toCollect = p.getProperty("ToCollect" + i);
                String itemName = p.getProperty("ItemName" + i);
                if (toCollect == null || itemName == null) {
                    throw new InvalidPropertiesFormatException(
                        "Number of CollectItemID properties must match/correspond " +
                            "with number of ToCollect & ItemName properties"
                    );
                }
                itemsToCollect.put(
                    Integer.parseInt(itemToCollect),
                    new Pair<>(Integer.parseInt(toCollect), itemName)
                );
                i++;
                itemToCollect = p.getProperty("CollectItemID" + i);
            }

            otherObjectives.clear();
            i = 1;
            String otherObjective = p.getProperty("OtherObjective" + i);
            while (otherObjective != null && !otherObjective.equals("")) {
                String numNeeded = p.getProperty("OtherObjectiveCount" + i);
                if (numNeeded == null) {
                    throw new InvalidPropertiesFormatException(
                        "Number of OtherObjective properties must match/correspond " +
                            "with number of OtherObjectiveCount properties"
                    );
                }
                otherObjectives.put(
                    otherObjective,
                    Integer.parseInt(numNeeded)
                );
                i++;
                otherObjective = p.getProperty("OtherObjective" + i);
            }

            expReward = Integer.parseInt(p.getProperty("EXP"));
            mesoReward = Integer.parseInt(p.getProperty("MESO"));

            itemRewards.clear();
            i = 1;
            if (p.getProperty("ITEM") != null && Integer.parseInt(p.getProperty("ITEM")) != 0) {
                itemRewards.put(
                    Integer.parseInt(p.getProperty("ITEM")),
                    Integer.parseInt(p.getProperty("ITEM_amount"))
                );
            } else if (p.getProperty("ITEM" + i) != null && Integer.parseInt(p.getProperty("ITEM" + i)) != 0) {
                itemRewards.put(
                    Integer.parseInt(p.getProperty("ITEM" + i)),
                    Integer.parseInt(p.getProperty("ITEM_amount" + i))
                );
            }
            i++;
            String itemReward = p.getProperty("ITEM" + i);
            while (itemReward != null && !itemReward.equals("") && Integer.parseInt(itemReward) != 0) {
                String itemAmount = p.getProperty("ITEM_amount" + i);
                if (itemAmount == null) {
                    throw new InvalidPropertiesFormatException(
                        "Number of ITEM properties must match/correspond with number of ITEM_amount properties"
                    );
                }
                itemRewards.put(Integer.parseInt(itemReward), Integer.parseInt(itemAmount));
                i++;
                itemReward = p.getProperty("ITEM" + i);
            }

            startNpc = "" + p.getProperty("NPC");
            endNpc = p.getProperty("NPC_end");
            if (endNpc == null) {
                endNpc = startNpc;
            }
            title = "" + p.getProperty("Title");
            info = "" + p.getProperty("Info");

            final String prereqString = p.getProperty("Prereqs");
            if (prereqString != null) {
                String[] idStrings = prereqString.split(",");
                for (String idString : idStrings) {
                    prereqs.add(Integer.parseInt(idString));
                }
            }

            dirt = bronze = silver = gold = 0;
            String dirtString = p.getProperty("Dirt");
            if (dirtString != null) {
                dirt = Integer.parseInt(dirtString);
            }
            String bronzeString = p.getProperty("Bronze");
            if (bronzeString != null) {
                bronze = Integer.parseInt(bronzeString);
            }
            String silverString = p.getProperty("Silver");
            if (silverString != null) {
                silver = Integer.parseInt(silverString);
            }
            String goldString = p.getProperty("Gold");
            if (goldString != null) {
                gold = Integer.parseInt(goldString);
            }
        } catch (Exception e) {
            System.err.println(e + " -- Failed to load MapleCQuest "  + id);
            this.id = 0;
        }
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

    public String loadTitle(int questid) {
        String title = "";
        Properties p = new Properties();
        try {
            p.load(new FileInputStream("quests/" + questid + ".ini"));
            title += p.getProperty("Title");
        } catch (Exception e) {
            System.err.println(e + " - Failed to load title of quest: " + questid);
            title += "[Failed to load title of quest: " + questid + "]";
        }
        return title;
    }

    public String getTitle() {
        return title;
    }

    public String loadInfo(int questid) {
        String info = "";
        Properties p = new Properties();
        try {
            p.load(new FileInputStream("quests/" + questid + ".ini"));
            info += p.getProperty("Info");
        } catch (Exception e) {
            System.err.println(e + " - Failed to load info of quest: " + questid);
            title += "[Failed to load info of quest " + questid + "]";
        }
        return info;
    }

    public boolean questExists(int questid) {
        try {
            FileInputStream fis = new FileInputStream("quests/" + questid + ".ini");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public String getInfo() {
        return info;
    }

    public void closeQuest() {
        loadQuest(0);
    }

    public boolean isPrereq(int questId) {
        return prereqs.contains(questId);
    }

    public Integer getNumberToKill(int monsterId) {
        if (!monsterTargets.containsKey(monsterId)) return null;
        return monsterTargets.get(monsterId).getLeft();
    }

    public Integer getNumberToCollect(int itemId) {
        if (!itemsToCollect.containsKey(itemId)) return null;
        return itemsToCollect.get(itemId).getLeft();
    }

    public Integer getNumberOfOtherObjective(String otherObjective) {
        if (!otherObjectives.containsKey(otherObjective)) return null;
        return otherObjectives.get(otherObjective);
    }

    public int getExpReward() {
        return expReward;
    }

    public int getMesoReward() {
        return mesoReward;
    }

    public Set<Integer> readPrereqs() {
        return new TreeSet<>(prereqs);
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

    public String getTargetName(int monsterId) {
        if (!monsterTargets.containsKey(monsterId)) return null;
        return monsterTargets.get(monsterId).getRight();
    }

    public String getItemName(int itemId) {
        if (!itemsToCollect.containsKey(itemId)) return null;
        return itemsToCollect.get(itemId).getRight();
    }

    public boolean requiresTarget(int monsterId) {
        return monsterTargets.containsKey(monsterId);
    }

    public boolean requiresItem(int itemId) {
        return itemsToCollect.containsKey(itemId);
    }

    public boolean requiresOtherObjective(String otherObjective) {
        return otherObjectives.containsKey(otherObjective);
    }

    public boolean requiresMonsterTargets() {
        return !monsterTargets.isEmpty();
    }

    public boolean requiresItemCollection() {
        return !itemsToCollect.isEmpty();
    }

    public boolean hasPrereqs() {
        return !prereqs.isEmpty();
    }

    public boolean requiresOtherObjectives() {
        return !otherObjectives.isEmpty();
    }

    public int getDirt() {
        return dirt;
    }

    public int getBronze() {
        return bronze;
    }

    public int getSilver() {
        return silver;
    }

    public int getGold() {
        return gold;
    }
}
