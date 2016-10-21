// object for custom quests
package net.sf.odinms.client;

import java.io.FileInputStream;
import java.util.Properties;

public class MapleCQuests { 
     
    private int id;
    private int target1, target2;
    private int item1, item2;
    private int reward1, reward2, reward3, reward4;
    private int amount1, amount2, amount3, amount4;
    private String npc;
    private String title;
    private String target1n, target2n;
    private String item1n, item2n;
    private String info;
     
    public void loadQuest(int id) { 
        Properties p = new Properties(); 
        try { 
            p.load(new FileInputStream("quests/" + id + ".ini"));
            this.id = id; 
            this.target1 = Integer.parseInt(p.getProperty("MonsterID1")); 
            this.target2 = Integer.parseInt(p.getProperty("MonsterID2")); 
            this.item1 = Integer.parseInt(p.getProperty("CollectItemID1")); 
            this.item2 = Integer.parseInt(p.getProperty("CollectItemID2")); 
            this.amount1 = Integer.parseInt(p.getProperty("ToKill1")); 
            this.amount2 = Integer.parseInt(p.getProperty("ToKill2")); 
            this.amount3 = Integer.parseInt(p.getProperty("ToCollect1")); 
            this.amount4 = Integer.parseInt(p.getProperty("ToCollect2")); 
            this.reward1 = Integer.parseInt(p.getProperty("EXP")); 
            this.reward2 = Integer.parseInt(p.getProperty("MESO")); 
            this.reward3 = Integer.parseInt(p.getProperty("ITEM")); 
            this.reward4 = Integer.parseInt(p.getProperty("ITEM_amount")); 
            this.npc = "" + p.getProperty("NPC"); 
            this.title = "" + p.getProperty("Title"); 
            this.target1n = "" + p.getProperty("MonsterName1"); 
            this.target2n = "" + p.getProperty("MonsterName2"); 
            this.item1n = "" + p.getProperty("ItemName1"); 
            this.item2n = "" + p.getProperty("ItemName2"); 
            this.info = "" + p.getProperty("Info"); 
        } catch (Exception e) { 
            System.out.println(e + " - Failed to load Quest "  + id); 
            this.id = 0; 
        } 
    } 
     
    public int getId() { 
        return id;
    } 
         
    public int getTargetId(int type) { 
        switch (type) { 
            case 1: 
                return target1; 
            case 2: 
                return target2; 
        } 
        return 0; 
    } 
     
    public String getTargetName(int type) { 
        switch (type) { 
            case 1: 
                return target1n; 
            case 2: 
                return target2n; 
        } 
        return ""; 
    } 
     
    public int getItemId(int type) { 
        switch (type) { 
            case 1: 
                return item1; 
            case 2: 
                return item2; 
        } 
        return 0; 
    } 
         
    public String getItemName(int type) { 
        switch (type) { 
            case 1: 
                return item1n; 
            case 2: 
                return item2n; 
        } 
        return ""; 
    } 
     
    public int getReward(int type) { 
        switch (type) { 
            case 1: 
                return reward1; 
            case 2: 
                return reward2; 
            case 3: 
                return reward3; 
        } 
        return 0; 
    } 
     
    public int getItemRewardAmount() { 
        return reward4; 
    } 
     
    public String getNPC() { 
        return npc; 
    } 
     
    public String loadTitle(int questid) { 
        String title = ""; 
        Properties p = new Properties(); 
        try { 
            p.load(new FileInputStream("quests/" + questid + ".ini")); 
            title += p.getProperty("Title"); 
        } catch (Exception e) { 
            System.out.println(e + " - Failed to load Title of Quest: " + questid);
            title += "[Failed to load Title of Quest: ]"; 
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
            System.out.println(e + " - Failed to load Info of Quest: " + questid); 
            title += "[Failed to load info of quest]";
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
     
    public int getToKill(int type) { 
        switch (type) { 
            case 1: 
                return amount1; 
            case 2: 
                return amount2; 
        } 
        return 0; 
    } 
     
    public int getToCollect(int type) { 
        switch (type) { 
            case 1: 
                return amount3; 
            case 2: 
                return amount4; 
        } 
        return 0; 
    } 
     
    public void closeQuest() { 
        loadQuest(0); 
    } 
}