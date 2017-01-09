package net.sf.odinms.client;

import net.sf.odinms.tools.Pair;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;

public class MapleCQuests {
    private int id;
    private final Map<Integer, Pair<Integer, String>> monsterTargets = new HashMap<>(4, 0.8f);
    private final Map<Integer, Pair<Integer, String>> itemsToCollect = new HashMap<>(4, 0.8f);
    private int expReward, mesoReward;
    private final Map<Integer, Integer> itemRewards = new HashMap<>(3);
    private String startNpc, endNpc;
    private String title;
    private String info;

    public void loadQuest(int id) {
        final Properties p = new Properties();
        try {
            p.load(new FileInputStream("quests/" + id + ".ini"));
            this.id = id;
            
            monsterTargets.clear();
            int i = 1;
            String monsterTarget = p.getProperty("MonsterID" + i);
            while (monsterTarget != null && !monsterTarget.equals("") && Integer.parseInt(monsterTarget) != 0) {
                String toKill = p.getProperty("ToKill" + i);
                String mobName = p.getProperty("MonsterName" + i);
                if (toKill == null || mobName == null) {
                    throw new InvalidPropertiesFormatException(
                        "Number of MonsterID properties must match/correspond with number of ToKill & MonsterName properties"
                    );
                }
                monsterTargets.put(Integer.parseInt(monsterTarget), new Pair<>(Integer.parseInt(toKill), mobName));
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
                        "Number of CollectItemID properties must match/correspond with number of ToCollect & ItemName properties"
                    );
                }
                itemsToCollect.put(Integer.parseInt(itemToCollect), new Pair<>(Integer.parseInt(toCollect), itemName));
                i++;
                itemToCollect = p.getProperty("CollectItemID" + i);
            }
            
            expReward = Integer.parseInt(p.getProperty("EXP"));
            mesoReward = Integer.parseInt(p.getProperty("MESO"));

            itemRewards.clear();
            i = 1;
            if (p.getProperty("ITEM") != null && Integer.parseInt(p.getProperty("ITEM")) != 0) {
                itemRewards.put(Integer.parseInt(p.getProperty("ITEM")), Integer.parseInt(p.getProperty("ITEM_amount")));
            } else if (p.getProperty("ITEM" + i) != null && Integer.parseInt(p.getProperty("ITEM" + i)) != 0) {
                itemRewards.put(Integer.parseInt(p.getProperty("ITEM" + i)), Integer.parseInt(p.getProperty("ITEM_amount" + i)));
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
    
    public Integer getNumberToKill(int monsterId) {
        if (!monsterTargets.containsKey(monsterId)) return null;
        return monsterTargets.get(monsterId).getLeft();
    }
    
    public Integer getNumberToCollect(int itemId) {
        if (!itemsToCollect.containsKey(itemId)) return null;
        return itemsToCollect.get(itemId).getLeft();
    }
    
    public int getExpReward() {
        return expReward;
    }
    
    public int getMesoReward() {
        return mesoReward;
    }
    
    public Map<Integer, Pair<Integer, String>> readMonsterTargets() {
        return new HashMap<>(monsterTargets);
    }
    
    public Map<Integer, Pair<Integer, String>> readItemsToCollect() {
        return new HashMap<>(itemsToCollect);
    }
    
    public Map<Integer, Integer> readItemRewards() {
        return new HashMap<>(itemRewards);
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
    
    public boolean requiresMonsterTargets() {
        return !monsterTargets.isEmpty();
    }
    
    public boolean requiresItemCollection() {
        return !itemsToCollect.isEmpty();
    }
}
