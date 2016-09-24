package net.sf.odinms.tools; 

import java.io.Console; 
import java.io.FileOutputStream; 
import java.io.FileInputStream; 
import java.io.IOException; 
import java.util.Properties; 
  
public class QuestCreator { 
    public static void main(String args[]) { 
        StringBuilder sb = new StringBuilder(); 
        Console con = System.console(); 
        Properties p = new Properties(); 
        boolean create_config = false; 
        boolean create_script_file = false; 

        System.out.println("Welcome to the QQC!\r\n\r\n"); 

        try { 
            p.load(new FileInputStream("qqc_config.ini")); 
        } catch (Exception e) { 
            System.out.println("Seems like it's the first time you use QQC.\r\nGot to config really quick first.\r\n"); 
            create_config = true; 
        } 

        if (create_config) { 
            sb.append("#Quick Quest Creator - Configuration\r\n\r\n"); 
             
            String moople = con.readLine("Are you using Moople (non-RMI): (true/false) "); 
            while (!moople.equals("false") && !moople.equals("true")) { 
                System.out.println("If you are using Moople non-RMI type true. Otherweise type false!"); 
                moople = con.readLine("Are you using Moople (non-RMI): (true/false) "); 
            } 
             
            String create_script = con.readLine("\r\nShall the QQC always add a basic NPC script for a created Quest? (true/false/?) "); 
            while (!create_script.equals("true") && !create_script.equals("false") && !create_script.equals("?")) { 
                create_script = con.readLine("\r\nShall the QQC always add a basic NPC script for a created Quest? (true/false/?) ");     
            } 
             
            while (create_script.equals("?")) { 
                System.out.println("\r\nSetting this to true will add a basic NPC script for every created custom Quest.\r\nThe Script contains: Quest Selecting, Quest Accepting, Quest Canceling, \r\nRewarding Player for succesful Quest."); 
                System.out.println("It will keep the script basic. So there won't be much NPC chat.\r\n"); 
                create_script = con.readLine("\r\nShall the QQC always add a basic NPC script for a created Quest? (true/false/?) "); 
            } 
             
            sb.append("always_script="); 
            if (create_script.equals("true")) { 
                sb.append("true"); 
            } else if (create_script.equals("false")) { 
                sb.append("false"); 
                System.out.println("\r\nNonetheless the QQC will ask you for every quest if you want to!\r\nThis is why you still have to config the following:"); 
            } 
            System.out.println("\r\n"); 
            sb.append("\r\n"); 
             
            if (moople.equals("true")) { 
                sb.append("moople=true").append("\r\n\r\n"); 
                 
                String all_world = con.readLine("   Since you are using Moople. \r\n   Do you want an NPC Script created for all worlds? (true/false) "); 
                while (!all_world.equals("true") && !all_world.equals("false") && !all_world.equals("?")) { 
                    System.out.println("If you want one for all worlds type true else type false."); 
                    all_world = con.readLine("   Since you are using Moople.\r\n   Do you want an NPC Script created for all worlds? (true/false) "); 
                } 
                                 
                if (all_world.equals("true")) { 
                    sb.append("all_world=true").append("\r\n"); 
                    sb.append("world="); 
                } else if (all_world.equals("false")) { 
                    sb.append("all_world=false").append("\r\n");; 
                    try { 
                        p.load(new FileInputStream("moople.ini")); 
                    } catch (Exception e) { 
                        System.out.println("There seems to be a problem. QQC will exit now."); 
                        System.exit(0); 
                    } 
                     
                    int worlds = Integer.parseInt(p.getProperty("worlds")); 
                    String world = con.readLine("\r\n   QQC found " + worlds + " world(s). For which one do you want scripts to be created? \r\n   (0 for world0, 1 for world1, etc) "); 
                    while (Integer.parseInt(world) + 1 > worlds) { 
                        world = con.readLine("\r\n   world" + world + " doesn't exist. Try again: "); 
                    } 
                     
                    sb.append("world=").append(world); 
                } 
                sb.append("\r\n\r\nnpc_dir="); 
             
            } else if (moople.equals("false")) { 
                sb.append("moople=false").append("\r\n\r\n"); 
                sb.append("all_world=").append("\r\n"); 
                sb.append("world=").append("\r\n\r\n"); 
                 
                String dir = con.readLine("\r\nAre your NPC Scripts located in scripts/npc? (true/false) "); 
                while (!dir.equals("false") && !dir.equals("true")) { 
                    System.out.println("If the scripts are located in scripts/npc then just type true else type false!"); 
                    dir = con.readLine("Are your NPC Scripts located in scripts/npc? (true/false) "); 
                } 
                 
                if (dir.equals("true")) { 
                    sb.append("npc_dir=").append("scripts/npc").append("\r\n");; 
                } else if (dir.equals("false")) { 
                    sb.append("npc_dir=").append(con.readLine("  Type your NPC direction here: ")).append("\r\n"); 
                } 
            } 
            sb.append("\r\n\r\n").append("next_quest=1000"); 
            System.out.println("\r\nThe QQC Configuration file will now be created. \r\nTo start creating your own quests, please restart the QuestCreator!"); 
            FileOutputStream out = null; 
            try { 
                out = new FileOutputStream("qqc_config.ini"); 
                out.write(sb.toString().getBytes()); 
            } catch (Exception e) { 
                System.out.println(e); 
            } finally { 
                try { 
                    if (out != null) out.close(); 
                } catch (IOException ex) { 
                    System.out.println(ex); 
                } 
            } 
        } else { 
            System.out.println("Config file succesfully loaded.\r\nStart creating Quest"); 
            sb.append("# Created Quest (ID: " + p.getProperty("next_quest") + ")").append("\r\n\r\n"); 
             
            sb.append("# Quest Name as String").append("\r\n"); 
            sb.append("Title=").append(con.readLine("   Quest Title: ")).append("\r\n\r\n"); 
            System.out.println(""); 
             
            sb.append("# Monster Target Name as String").append("\r\n"); 
            sb.append("MonsterName1=").append(con.readLine("   1st Monster Name: ")).append("\r\n"); 
            sb.append("MonsterName2=").append(con.readLine("   2nd Monster Name: ")).append("\r\n\r\n"); 
            System.out.println(""); 
             
            sb.append("# Monster Target ID as Integer").append("\r\n"); 
            sb.append("MonsterID1=").append(Integer.parseInt(con.readLine("   1st Monster ID: "))).append("\r\n"); 
            sb.append("MonsterID2=").append(Integer.parseInt(con.readLine("   2nd Monster ID: "))).append("\r\n\r\n"); 
            System.out.println(""); 
             
            sb.append("# How many to kill of each as Integer").append("\r\n"); 
            sb.append("ToKill1=").append(Integer.parseInt(con.readLine("   How many to kill of 1st: "))).append("\r\n"); 
            sb.append("ToKill2=").append(Integer.parseInt(con.readLine("   How many to kill of 2nd: "))).append("\r\n\r\n"); 
            System.out.println(""); 
             
            sb.append("# Collect Item Name as String").append("\r\n"); 
            sb.append("ItemName1=").append(con.readLine("   1st Collect Item Name: ")).append("\r\n"); 
            sb.append("ItemName2=").append(con.readLine("   2nd Collect Item Name: ")).append("\r\n\r\n"); 
            System.out.println(""); 
             
            sb.append("# Collect Item ID as Integer").append("\r\n"); 
            sb.append("CollectItemID1=").append(Integer.parseInt(con.readLine("   1st Collect Item ID: "))).append("\r\n"); 
            sb.append("CollectItemID2=").append(Integer.parseInt(con.readLine("   2nd Collect Item ID: "))).append("\r\n\r\n"); 
            System.out.println(""); 
             
            sb.append("# How many to collect of each as Integer").append("\r\n"); 
            sb.append("ToCollect1=").append(Integer.parseInt(con.readLine("   How many to collect of 1st: "))).append("\r\n"); 
            sb.append("ToCollect2=").append(Integer.parseInt(con.readLine("   How many to collect of 2nd: "))).append("\r\n\r\n"); 
            System.out.println(""); 
             
            sb.append("# Rewards as Integer").append("\r\n"); 
            sb.append("EXP=").append(Integer.parseInt(con.readLine("   Exp reward: "))).append("\r\n"); 
            sb.append("MESO=").append(Integer.parseInt(con.readLine("   Meso reward: "))).append("\r\n"); 
            String item = con.readLine("   Item ID reward: "); 
            if (Integer.parseInt(item) > 0) { 
                sb.append("ITEM=").append(item).append("\r\n"); 
                sb.append("ITEM_amount=").append(con.readLine("   Amount of Items: ")).append("\r\n\r\n"); 
            } else { 
                sb.append("ITEM=").append("0").append("\r\n"); 
                sb.append("Item_amount=").append("0").append("\r\n\r\n"); 
            } 
            System.out.println(""); 
             
            sb.append("# Quest NPC as String").append("\r\n"); 
            sb.append("NPC=").append(con.readLine("   Quest NPC Name: ")).append("\r\n\r\n"); 
            System.out.println(""); 
             
            sb.append("# Quest Info as String").append("\r\n"); 
            sb.append("Info=").append(con.readLine("   Quest Info: ")).append("\r\n");     
             
            FileOutputStream out = null; 
            try { 
                out = new FileOutputStream("quests/" + p.getProperty("next_quest") + ".ini"); 
                out.write(sb.toString().getBytes()); 
            } catch (Exception e) { 
                System.out.println(e); 
            } finally { 
                try { 
                    if (out != null) out.close(); 
                } catch (IOException ex) { 
                    System.out.println(ex); 
                } 
            } 
             
            int new_next_quest = (Integer.parseInt(p.getProperty("next_quest")) + 1); 
            StringBuilder s = new StringBuilder(); 
            s.append("always_script=").append(p.getProperty("always_script")).append("\r\n"); 
            s.append("moople=").append(p.getProperty("moople")).append("\r\n\r\n");
            s.append("all_world=").append(p.getProperty("all_world")).append("\r\n"); 
            s.append("world=").append(p.getProperty("world")).append("\r\n\r\n"); 
            s.append("npc_dir=").append(p.getProperty("npc_dir")).append("\r\n\r\n"); 
            s.append("next_quest=").append(new_next_quest); 
                         
            if (p.getProperty("always_script").equals("false")) { 
                String script = con.readLine("\r\nDo you want to create the basic NPC Script for this Quest? (y/n)"); 
                 
                while (!script.equals("y") && !script.equals("n")) { 
                    script = con.readLine("\r\nDo you want to create the basic NPC Script for this Quest? (y/n)"); 
                } 
                 
                if (script.equals("y")) { 
                    create_script_file = true; 
                } else if (script.equals("n")) { 
                    System.out.println("\r\nNo Script was created. QQC will exit now!"); 
                    System.exit(0); 
                } 
            } 
             
            if (p.getProperty("always_script").equals("true")) { 
                create_script_file = true; 
            } 
             
            if (create_script_file) { 
                int npcid = Integer.parseInt(con.readLine("\r\n   Type in the Quest NPC's ID: ")); 
                int id = new_next_quest - 1; 
                 
                if (p.getProperty("moople").equals("true")) { 
                    if (p.getProperty("all_world").equals("true")) { 
                        Properties ini = new Properties(); 
                        try { 
                            ini.load(new FileInputStream("moople.ini")); 
                        } catch (Exception e) { 
                            System.out.println("There seems to be a problem. QQC will exit now"); 
                            System.exit(0); 
                        } 
                         
                        int worlds = Integer.parseInt(ini.getProperty("worlds")); 
                        for (int x = 0; x < worlds; x++) { 
                            createQuestScript(id, npcid, "scripts/npc/world" + x); 
                        } 
                    } else { 
                        createQuestScript(id, npcid, "scripts/npc/world" + p.getProperty("world")); 
                    } 
                 
                } else { 
                    createQuestScript(id, npcid, p.getProperty("npc_dir")); 
                } 
                 
                System.out.println("\r\nScript was created."); 
            } 
             
            FileOutputStream out2 = null;     
            try { 
                out2 = new FileOutputStream("qqc_config.ini"); 
                out2.write(s.toString().getBytes()); 
            } catch (Exception e) { 
                System.out.println(e); 
            } finally { 
                try { 
                    if (out2 != null) out2.close(); 
                } catch (IOException ex) { 
                    System.out.println(ex); 
                } 
            } 

            System.out.println("QQC will exit now."); 
        } 
    } 
     
    public static void createQuestScript(int id, int npcid, String direction) { 
        StringBuilder b = new StringBuilder(); 
        b.append("/**\r\n * Quick Quest: ").append(id).append("\r\n * Author: LikeABaws \r\n **/"); 
        b.append("\r\n\r\n"); 
        b.append("var id = ").append(id); 
        b.append("\r\n\r\n"); 
        b.append("function start() {").append("\r\n"); 
        b.append("    status = -1;").append("\r\n"); 
        b.append("    action(1, 0, 0);").append("\r\n").append("}").append("\r\n\r\n"); 
        b.append("function action (mode, type, selection) {").append("\r\n"); 
        b.append("    if (mode == -1) {").append("\r\n"); 
        b.append("        cm.dispose();").append("\r\n"); 
        b.append("    } else {").append("\r\n"); 
        b.append("        if (mode == 0 && status == 0) {").append("\r\n"); 
        b.append("            cm.dispose();").append("\r\n"); 
        b.append("            return;").append("\r\n").append("        }").append("\r\n"); 
        b.append("        if (mode == 1)").append("\r\n"); 
        b.append("            status++;").append("\r\n"); 
        b.append("        else").append("\r\n"); 
        b.append("            status--;").append("\r\n"); 
        b.append("        if (!cm.onQuest()) {").append("\r\n"); 
        b.append("            if (status == 0) {").append("\r\n"); 
        b.append("                if (mode == 0) {").append("\r\n"); 
        b.append("                    cm.sendOk(cm.randomText(3));").append("\r\n"); 
        b.append("                    cm.dispose();").append("\r\n"); 
        b.append("                } else {").append("\r\n"); 
        b.append("                    cm.sendSimple(cm.selectQuest(id, cm.randomText(1)));").append("\r\n"); 
        b.append("                }").append("\r\n"); 
        b.append("            } else if (status == 1) { ").append("\r\n"); 
        b.append("                cm.sendAcceptDecline(cm.getPlayer().getCQuest().loadInfo(id));").append("\r\n"); 
        b.append("            } else if (status == 2) { ").append("\r\n"); 
        b.append("                cm.startCQuest(id);").append("\r\n"); 
        b.append("                cm.dispose();").append("\r\n"); 
        b.append("            }").append("\r\n"); 
        b.append("        } else if (!cm.onQuest(id)) {").append("\r\n"); 
        b.append("            if (status == 0) {").append("\r\n"); 
        b.append("                cm.sendYesNo(cm.randomText(4) + cm.getPlayer().getCQuest().getTitle() + cm.randomText(5));").append("\r\n"); 
        b.append("            } else if (status == 1) {").append("\r\n"); 
        b.append("                cm.startCQuest(0);").append("\r\n"); 
        b.append("                cm.dispose();").append("\r\n"); 
        b.append("            }").append("\r\n"); 
        b.append("        } else if (cm.onQuest(id) && cm.canComplete()) {").append("\r\n"); 
        b.append("            if (status == 0) {").append("\r\n"); 
        b.append("                cm.sendSimple(cm.selectQuest(id, cm.randomText(1)));").append("\r\n"); 
        b.append("            } else if (status == 1) {").append("\r\n"); 
        b.append("                cm.sendOk(cm.showReward(cm.randomText(2)));").append("\r\n"); 
        b.append("            } else if (status == 2) {").append("\r\n"); 
        b.append("                cm.rewardPlayer(0, 0);").append("\r\n"); 
        b.append("                cm.getPlayer().sendHint(cm.randomText(6));").append("\r\n"); 
        b.append("                cm.dispose();").append("\r\n"); 
        b.append("            }").append("\r\n"); 
        b.append("        } else if (cm.onQuest(id) && !cm.canComplete()) {").append("\r\n"); 
        b.append("            if (status == 0) {").append("\r\n"); 
        b.append("                if (mode == 0) {").append("\r\n"); 
        b.append("                    cm.dispose();").append("\r\n"); 
        b.append("                } else {").append("\r\n"); 
        b.append("                    cm.sendSimple(cm.selectQuest(id, cm.randomText(1)));").append("\r\n"); 
        b.append("                }").append("\r\n"); 
        b.append("            } else if (status == 1) {").append("\r\n"); 
        b.append("                cm.sendYesNo(cm.randomText(7));").append("\r\n");
        b.append("            } else if (status == 2) {").append("\r\n"); 
        b.append("                cm.startCQuest(0);").append("\r\n"); 
        b.append("                cm.dispose();").append("\r\n"); 
        b.append("            }").append("\r\n"); 
        b.append("        }").append("\r\n"); 
        b.append("    }").append("\r\n"); 
        b.append("}"); 
         
        FileOutputStream out2 = null;     
        try { 
            out2 = new FileOutputStream(direction + "/" + npcid + ".js"); 
            out2.write(b.toString().getBytes()); 
        } catch (Exception e) { 
            System.out.println(e); 
        } finally { 
            try { 
                if (out2 != null) out2.close(); 
            } catch (IOException ex) { 
                System.out.println(ex); 
            } 
        }         
    } 
}  