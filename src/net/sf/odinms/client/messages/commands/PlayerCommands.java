package net.sf.odinms.client.messages.commands;

import net.sf.odinms.client.*;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.WorldServer;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.StringUtil;

import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerCommands implements Command {

    @Override
    public void execute(MapleClient c, final MessageCallback mc, String[] splitted) throws Exception {
        splitted[0] = splitted[0].toLowerCase();
        MapleCharacter player = c.getPlayer();
        if (splitted[0].equals("@command") || splitted[0].equals("@commands") || splitted[0].equals("@help")) {
            mc.dropMessage("================================================================");
            mc.dropMessage("               " + c.getChannelServer().getServerName() + " Commands");
            mc.dropMessage("================================================================");
            mc.dropMessage("@checkstat - | - Displays your stats.");
            mc.dropMessage("@save - | - Saves your progress.");
            mc.dropMessage("@expfix - | - Fixes your negative experience.");
            mc.dropMessage("@dispose - | - Unsticks you from any hanging NPC interactions.");
            mc.dropMessage("@mapfix - | - Fixes you if you've fallen off the map.");
            mc.dropMessage("@questinfo - | - Gets the info for your current IntransigentQuest.");
            mc.dropMessage("@ria - | - Opens chat with Ria to get info about IntransigentQuests.");
            mc.dropMessage("@cancelquest - | - Cancels your current quest.");
            mc.dropMessage("@togglesmega - | - Turn smegas OFF/ON.");
            mc.dropMessage("@str/@dex/@int/@luk <number> - | - Automatically adds AP to your stats.");
            mc.dropMessage("@gm <message> - | - Sends a message to the GMs online.");
            mc.dropMessage("@afk <playername> - | - Shows how long a person has been AFK.");
            mc.dropMessage("@onlinetime - | - Shows how long a person has been online.");
            mc.dropMessage("@online - | - Lists all online players.");
            mc.dropMessage("@monstertrialtime - | - Shows how much longer you must wait to enter another Monster Trial.");
            mc.dropMessage("@mapleadmin - | - Opens up chat with Maple Adminstrator NPC.");
            mc.dropMessage("@monsterlevels - | - Displays levels and relative XP multipliers for all monsters on the map.");
            mc.dropMessage("@absolutexprate - | - Displays your current XP multiplier before relative multipliers.");
            mc.dropMessage("@monstersinrange <levels below> <levels above> - | - Lists monsters within the range specified of your level. Both arguments are optional.");
            mc.dropMessage("@monstertrialtier - | - Lists your current Monster Trial tier, points, and points needed for next tier.");
            mc.dropMessage("@damagescale - | - Displays the current multiplier by which all incoming damage is multiplied.");
            mc.dropMessage("@votepoints - | - Displays your current vote point count.");
            mc.dropMessage("@sell - | - Opens up an NPC to mass-sell equipment items.");
            mc.dropMessage("@morgue <playername> - | - Displays the past 5 lives of the player.");
            mc.dropMessage("@deathinfo <playername> - | - Displays death count, highest level achieved, paragon level, and suicide count of player.");
            mc.dropMessage("@overflowexp <playername> - | - Displays overflow EXP (EXP gained past level 250) for the player.");
            mc.dropMessage("@expboostinfo - | - Displays how much time you have left on your EXP bonus.");
            mc.dropMessage("@deathpenalty - | - Displays your current death penalty level and its effects, as well as how long until you can next rest.");
            mc.dropMessage("@defense/@defence - | - Displays your true current weapon and magic defense.");
            mc.dropMessage("@monsterhp - | - Displays the current HP % of all mobs on the map.");
            mc.dropMessage("@bosshp - | - Displays the current HP % of all bosses on the map.");
            mc.dropMessage("@truedamage - | - Toggles the display of true damage received.");
            mc.dropMessage("@whodrops - | - Allows selection of an item and lists monsters who drop the selected item.");
            mc.dropMessage("@whodrops <itemid> - | - Lists monsters who drop the item with that ID.");
            mc.dropMessage("@whodrops <searchstring> - | - Lists monsters who drop the item with the name that is the closest fit for <searchstring>.");
            mc.dropMessage("@monsterdrops <monsterid> [eqp/etc/use] - | - Lists all items (of the specified type, if specified) that a monster drops.");
            mc.dropMessage("@monsterdrops <searchstring> [eqp/etc/use] - | - Lists all items (of the specified type, if specified) that a monster drops.");
            mc.dropMessage("@pqpoints - | - Displays your current PQ point total.");
            mc.dropMessage("@showpqpoints - | - Toggles whether or not your current PQ point total is displayed every time the total is changed.");
            mc.dropMessage("@readingtime - | - Displays how long you've been reading.");
            mc.dropMessage("@donated - | - Allows access to donator benefits.");
            mc.dropMessage("@vote - | - Displays the amount of time left until you may vote again.");
            if (player.getClient().getChannelServer().extraCommands()) {
                mc.dropMessage("@cody/@storage/@news/@kin/@nimakin/@reward/@reward1/@fredrick/@spinel/@clan");
                mc.dropMessage("@banme - | - This command will ban you. GMs will not unban you from this.");
                mc.dropMessage("@goafk - | - Uses a chalkboard to say that you are AFK.");
                mc.dropMessage("@slime - | - Summons slimes for you for a small cost.");
                mc.dropMessage("@go - | - Takes you to many towns and fighting areas.");
                mc.dropMessage("@buynx - | - You can purchase NX with this command.");
            }
        } else if (splitted[0].equals("@checkstats")) {
            mc.dropMessage("Your stats are:");
            mc.dropMessage("Str: " + player.getStr());
            mc.dropMessage("Dex: " + player.getDex());
            mc.dropMessage("Int: " + player.getInt());
            mc.dropMessage("Luk: " + player.getLuk());
            mc.dropMessage("Available AP: " + player.getRemainingAp());
        } else if (splitted[0].equals("@save")) {
            if (!player.getCheatTracker().Spam(900000, 0)) { // 15 minutes
                player.saveToDB(true, true);
                mc.dropMessage("Saved.");
            } else {
                mc.dropMessage("You cannot save more than once every 15 minutes.");
            }
        } else if (splitted[0].equals("@expfix")) {
            player.setExp(0);
            player.updateSingleStat(MapleStat.EXP, player.getExp());
        } else if (splitted[0].equals("@dispose")) {
            NPCScriptManager.getInstance().dispose(c);
            mc.dropMessage("You have been disposed.");
        } else if (splitted[0].equals("@mapfix")) {
            MapleMap curmap = player.getMap();
            try {
                if (curmap.getGroundBelow(player.getPosition()) == null) {
                    MapleMap target = c.getChannelServer().getMapFactory().getMap(player.getMapId());
                    MaplePortal portal = target.getPortal(0);
                    player.changeMap(target, portal);
                } else {
                    player.dropMessage(6, "You are not currently stuck. Ground below: (" + curmap.getGroundBelow(player.getPosition()).x + ", " + curmap.getGroundBelow(player.getPosition()).y + ")");
                }
            } catch (Exception e) {
                mc.dropMessage("@mapfix failed: " + e);
                e.printStackTrace();
            }
        } else if (splitted[0].equals("@questinfo")) {
            MapleCQuests q = player.getCQuest();
            String complete;
            if (q.getId() > 0) {
                if (player.canComplete()) {
                    complete = "[Quest is ready to turn in]";
                } else {
                    complete = "[Quest still in progress]";
                }
                mc.dropMessage("Quest: " + q.getTitle());
                mc.dropMessage("-----------------------------");
                q.readMonsterTargets().entrySet().forEach(e -> 
                    mc.dropMessage(
                          e.getValue().getRight()
                        + "s killed: "
                        + player.getQuestKills(e.getKey())
                        + "/"
                        + e.getValue().getLeft()
                    )
                );
                q.readItemsToCollect().entrySet().forEach(e ->
                    mc.dropMessage(
                          e.getValue().getRight()
                        + "s collected: "
                        + player.getQuestCollected(e.getKey())
                        + "/"
                        + e.getValue().getLeft()
                    )
                );
                mc.dropMessage(complete);
            } else {
                mc.dropMessage("You don't have a quest currently underway.");
            }
        } else if (splitted[0].equals("@togglesmega")) {
            player.setSmegaEnabled(!player.getSmegaEnabled());
            String text = (!player.getSmegaEnabled() ? "[Disable] Smegas are now disabled." : "[Enable] Smegas are now enabled.");
            mc.dropMessage(text);
        } else if (splitted[0].equals("@str") || splitted[0].equals("@dex") || splitted[0].equals("@int") || splitted[0].equals("@luk")) {
            if (splitted.length != 2) {
                mc.dropMessage("Syntax: @<Stat> <amount>");
                mc.dropMessage("Stat: <STR> <DEX> <INT> <LUK>");
                return;
            }
            int x = Integer.parseInt(splitted[1]), max = 30000;
            if (x > 0 && x <= player.getRemainingAp() && x < Short.MAX_VALUE) {
                if (splitted[0].equals("@str") && x + player.getStr() < max) {
                    player.addAP(c, 1, x);
                } else if (splitted[0].equals("@dex") && x + player.getDex() < max) {
                    player.addAP(c, 2, x);
                } else if (splitted[0].equals("@int") && x + player.getInt() < max) {
                    player.addAP(c, 3, x);
                } else if (splitted[0].equals("@luk") && x + player.getLuk() < max) {
                    player.addAP(c, 4, x);
                } else {
                    mc.dropMessage("Make sure the stat you are trying to raise will not be over " + Short.MAX_VALUE + ".");
                }
            } else {
                mc.dropMessage("Please make sure your AP is valid.");
            }
        } else if (splitted[0].equals("@gm")) {
            if (splitted.length < 2) {
                return;
            }
            try {
                c.getChannelServer().getWorldInterface().broadcastGMMessage(null, MaplePacketCreator.serverNotice(6, "Channel: " + c.getChannel() + "  " + player.getName() + ": " + StringUtil.joinStringFrom(splitted, 1)).getBytes());
            } catch (RemoteException ex) {
                c.getChannelServer().reconnectWorld();
            }
            mc.dropMessage("Message sent.");
            // player.dropMessage(1, "Please don't flood GMs with your messages.");
        } else if (splitted[0].equals("@afk")) {
            if (splitted.length >= 2) {
                String name = splitted[1];
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                if (victim == null) {
                    try {
                        WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
                        int channel = wci.find(name);
                        if (channel == -1) {
                            mc.dropMessage("This player is not currently online.");
                            return;
                        }
                        victim = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(name);
                        if (victim == null || victim.isGM()) {
                            mc.dropMessage("This player is not currently online.");
                            return;
                        }
                    } catch (RemoteException re) {
                        c.getChannelServer().reconnectWorld();
                        return;
                    }
                }
                long blahblah = System.currentTimeMillis() - victim.getAfkTime();
                if (Math.floor(blahblah / 60000) == 0) { // less than a minute
                    mc.dropMessage("This player has not been AFK in the last minute.");
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(victim.getName());
                    sb.append(" has been afk for");
                    compareTime(sb, blahblah);
                    mc.dropMessage(sb.toString());
                }
            } else {
                mc.dropMessage("Incorrect Syntax.");
            }
        } else if (splitted[0].equals("@onlinetime")) {
            if (splitted.length >= 2) {
                String name = splitted[1];
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                if (victim == null) {
                    try {
                        WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
                        int channel = wci.find(name);
                        if (channel == -1) {
                            mc.dropMessage("This player is not online.");
                            return;
                        }
                        victim = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(name);
                        if (victim == null || victim.isGM()) {
                            mc.dropMessage("This player is not online.");
                            return;
                        }
                    } catch (RemoteException re) {
                        c.getChannelServer().reconnectWorld();
                        return;
                    }
                }
                long blahblah = System.currentTimeMillis() - victim.getLastLogin();
                StringBuilder sb = new StringBuilder();
                sb.append(victim.getName());
                sb.append(" has been online for");
                compareTime(sb, blahblah);
                mc.dropMessage(sb.toString());
            } else {
                mc.dropMessage("Incorrect Syntax.");
            }
        } else if (splitted[0].equals("@monstertrialtime")) {
            if (System.currentTimeMillis() - player.getLastTrialTime() < 2 * 60 * 60 * 1000) {
                long timesincelast = System.currentTimeMillis() - player.getLastTrialTime();
                double inminutes = timesincelast / 60000.0;
                inminutes = Math.floor(inminutes);
                int cooldown = 120 - (int) inminutes;
                mc.dropMessage("You must wait " + cooldown + " more minute(s) before you may enter the Monster Trials again.");
            } else {
                mc.dropMessage("You may enter the Monster Trials.");
            }
        } else if (splitted[0].equals("@mapleadmin")) {
            NPCScriptManager npc = NPCScriptManager.getInstance();
            npc.start(c, 9010000);
        } else if (splitted[0].equals("@monsterlevels")) {
            ArrayList<Integer> monsterids = new ArrayList<>(4);
            double rx;
            int absxp = player.getAbsoluteXp();
            absxp *= 4;
            for (MapleMapObject mmo : player.getMap().getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                MapleMonster monster = (MapleMonster) mmo;
                if (!monsterids.contains(monster.getId())) {
                    monsterids.add(monster.getId());
                    rx = player.getRelativeXp(monster.getLevel());
                    BigDecimal rxbd = new BigDecimal(rx);
                    rxbd = rxbd.setScale(2, RoundingMode.HALF_UP);
                    mc.dropMessage(monster.getName() + " | Level: " + monster.getLevel() + " Relative XP: " + rxbd.toString() + "x" + " Total XP: " + rxbd.multiply(BigDecimal.valueOf(absxp)).toString() + "x");
                }
            }
        } else if (splitted[0].equals("@absolutexprate")) {
            int absxp = (int) Math.floor(0.1d * (double) player.getLevel());
            if (absxp > 1) {
                mc.dropMessage("Your total absolute XP multiplier: " + absxp * c.getChannelServer().getExpRate());
            } else {
                mc.dropMessage("Your total absolute XP multiplier: " + c.getChannelServer().getExpRate());
            }
        } else if (splitted[0].equalsIgnoreCase("@whodrops")) {
            if (splitted.length < 2) {
                NPCScriptManager npc = NPCScriptManager.getInstance();
                npc.start(c, 9201094);
            } else {
                try {
                    int searchid = Integer.parseInt(splitted[1]);
                    List<String> retMobs = new ArrayList<>();
                    MapleData data;
                    MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
                    data = dataProvider.getData("Mob.img");
                    mc.dropMessage("Item " + searchid + " is dropped by the following mobs:");
                    List<Pair<Integer, String>> mobPairList = new ArrayList<>();
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("SELECT monsterid FROM monsterdrops WHERE itemid = ?");
                    ps.setInt(1, searchid);
                    ResultSet rs = ps.executeQuery();
                    for (MapleData mobIdData : data.getChildren()) {
                        int mobIdFromData = Integer.parseInt(mobIdData.getName());
                        String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                        mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
                    }
                    while (rs.next()) {
                        int mobn = rs.getInt("monsterid");
                        for (Pair<Integer, String> mobPair : mobPairList) {
                            if (mobPair.getLeft() == (mobn) && !retMobs.contains(mobPair.getRight())) {
                                retMobs.add(mobPair.getRight());
                            }
                        }
                    }
                    rs.close();
                    ps.close();
                    if (!retMobs.isEmpty()) {
                        for (String singleRetMob : retMobs) {
                            mc.dropMessage(singleRetMob);
                        }
                    } else {
                        mc.dropMessage("No mobs drop this item.");
                    }
                } catch (SQLException e) {
                    System.out.print("@whodrops failed with SQLException: " + e);
                } catch (NumberFormatException nfe) {
                    try {
                        String searchstring = "";
                        for (int i = 1; i < splitted.length; ++i) {
                            if (i == 1) {
                                searchstring += splitted[i];
                            } else {
                                searchstring += " " + splitted[i];
                            }
                        }
                        if (searchstring.isEmpty()) {
                            mc.dropMessage("Invalid syntax. Use @whodrops or @whodrops <searchstring> instead.");
                            return;
                        }
                        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        Pair<Integer, String> consumecandidate = ii.getConsumeByName(searchstring);
                        Pair<Integer, String> eqpcandidate = ii.getEqpByName(searchstring);
                        Pair<Integer, String> etccandidate = ii.getEtcByName(searchstring);
                        Pair<Integer, String> candidate = consumecandidate;
                        if (etccandidate != null && (candidate == null || etccandidate.getRight().length() < candidate.getRight().length())) {
                            candidate = etccandidate;
                        }
                        if (eqpcandidate != null && (candidate == null || eqpcandidate.getRight().length() < candidate.getRight().length())) {
                            candidate = eqpcandidate;
                        }
                    
                        try {
                            int searchid;
                            if (candidate != null) {
                                searchid = candidate.getLeft();
                            } else {
                                mc.dropMessage("No item could be found with the search string provided.");
                                return;
                            }
                            List<String> retMobs = new ArrayList<>();
                            MapleData data;
                            MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
                            data = dataProvider.getData("Mob.img");
                            mc.dropMessage(candidate.getRight() + " is dropped by the following mobs:");
                            List<Pair<Integer,String>> mobPairList = new ArrayList<>();
                            Connection con = DatabaseConnection.getConnection();
                            PreparedStatement ps = con.prepareStatement("SELECT monsterid FROM monsterdrops WHERE itemid = ?");
                            ps.setInt(1, searchid);
                            ResultSet rs = ps.executeQuery();
                            for (MapleData mobIdData : data.getChildren()) {
                                int mobIdFromData = Integer.parseInt(mobIdData.getName());
                                String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                                mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
                            }
                            while (rs.next()) {
                                int mobn = rs.getInt("monsterid");
                                for (Pair<Integer, String> mobPair : mobPairList) {
                                    if (mobPair.getLeft() == (mobn) && !retMobs.contains(mobPair.getRight())) {
                                        retMobs.add(mobPair.getRight());
                                    }
                                }
                            }
                            rs.close();
                            ps.close();
                            if (!retMobs.isEmpty()) {
                                for (String singleRetMob : retMobs) {
                                    mc.dropMessage(singleRetMob);
                                }
                            } else {
                                mc.dropMessage("No mobs drop this item.");
                            }
                        } catch (SQLException sqle) {
                            System.out.print("@whodrops failed with SQLException: " + sqle);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (splitted[0].equals("@monsterdrops")) {
            if (splitted.length < 2) {
                mc.dropMessage("Invalid syntax. Use @monsterdrops <monsterid> [eqp/use/etc] or @monsterdrops <searchstring> [eqp/use/etc] instead.");
            } else {
                try {
                    int searchid = Integer.parseInt(splitted[1]);
                    MapleInventoryType itemtype = null;
                    String itemtypestring = null;
                    if (splitted.length > 2) {
                        switch (splitted[2].toLowerCase()) {
                            case "eqp":
                                itemtypestring = "equip";
                                itemtype = MapleInventoryType.EQUIP;
                                break;
                            case "use":
                                itemtypestring = "use";
                                itemtype = MapleInventoryType.USE;
                                break;
                            case "etc":
                                itemtypestring = "etc";
                                itemtype = MapleInventoryType.ETC;
                                break;
                            default:
                                mc.dropMessage("Invalid syntax. Use @monsterdrops <monsterid> [eqp/use/etc] or @monsterdrops <searchstring> [eqp/use/etc] instead.");
                                return;
                        }
                    }
                    List<String> retItems = new ArrayList<>();
                    if (itemtypestring != null) {
                        mc.dropMessage("Monster ID " + searchid + " drops the following " + itemtypestring + " items:");
                    } else {
                        mc.dropMessage("Monster ID " + searchid + " drops the following items:");
                    }
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("SELECT itemid FROM monsterdrops WHERE monsterid = ?");
                    ps.setInt(1, searchid);
                    ResultSet rs = ps.executeQuery();
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    while (rs.next()) {
                        int itemid = rs.getInt("itemid");
                        if (itemtype == null) {
                            retItems.add(ii.getName(itemid));
                        } else {
                            if (itemtype == ii.getInventoryType(itemid)) {
                                retItems.add(ii.getName(itemid));
                            }
                        }
                    }
                    rs.close();
                    ps.close();
                    if (!retItems.isEmpty()) {
                        String retitemstring = "";
                        for (String singleRetItem : retItems) {
                            retitemstring += singleRetItem + ", ";
                        }
                        mc.dropMessage(retitemstring.substring(0, retitemstring.length() - 2));
                    } else {
                        if (itemtypestring != null) {
                            mc.dropMessage("This mob does not drop any items of the specified kind.");
                        } else {
                            mc.dropMessage("This mob does not drop any items.");
                        }
                    }
                } catch (SQLException e) {
                    System.out.print("@monsterdrops failed with SQLException: " + e);
                } catch (NumberFormatException nfe) {
                    try {
                        int searchid = 0;
                        String searchstring = null;
                        MapleInventoryType itemtype = null;
                        String itemtypestring = null;
                        for (int i = 1; i < splitted.length; ++i) {
                            if (i == 1) {
                                searchstring = splitted[i];
                            } else {
                                switch (splitted[i].toLowerCase()) {
                                    case "eqp":
                                        itemtypestring = "equip";
                                        itemtype = MapleInventoryType.EQUIP;
                                        break;
                                    case "use":
                                        itemtypestring = "use";
                                        itemtype = MapleInventoryType.USE;
                                        break;
                                    case "etc":
                                        itemtypestring = "etc";
                                        itemtype = MapleInventoryType.ETC;
                                        break;
                                    default:
                                        itemtype = null;
                                        searchstring += " " + splitted[i];
                                        break;
                                }
                            }
                        }
                        if (searchstring == null) {
                            mc.dropMessage("Invalid syntax. Use @monsterdrops <monsterid> [eqp/use/etc] or @monsterdrops <searchstring> [eqp/use/etc] instead.");
                            return;
                        }
                        searchstring = searchstring.toUpperCase();
                        List<String> retItems = new ArrayList<>();
                        MapleData data;
                        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
                        data = dataProvider.getData("Mob.img");
                        List<Pair<Integer, String>> mobPairList = new ArrayList<>();
                        for (MapleData mobIdData : data.getChildren()) {
                            int mobIdFromData = Integer.parseInt(mobIdData.getName());
                            String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                            mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
                        }
                        String bestmatch = null;
                        for (Pair<Integer, String> mobPair : mobPairList) {
                            if (mobPair.getRight().toUpperCase().startsWith(searchstring)) {
                                if (bestmatch == null) {
                                    bestmatch = mobPair.getRight();
                                    searchid = mobPair.getLeft();
                                } else {
                                    if (mobPair.getRight().length() < bestmatch.length()) {
                                        bestmatch = mobPair.getRight();
                                        searchid = mobPair.getLeft();
                                    }
                                }
                            }
                        }
                        if (bestmatch != null) {
                            if (itemtypestring != null) {
                                mc.dropMessage(bestmatch + " drops the following " + itemtypestring + " items:");
                            } else {
                                mc.dropMessage(bestmatch + " drops the following items:");
                            }
                        } else {
                            mc.dropMessage("No mobs were found that start with \"" + searchstring.toLowerCase() + "\".");
                            return;
                        }
                        Connection con = DatabaseConnection.getConnection();
                        PreparedStatement ps = con.prepareStatement("SELECT itemid FROM monsterdrops WHERE monsterid = ?");
                        ps.setInt(1, searchid);
                        ResultSet rs = ps.executeQuery();
                        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        while (rs.next()) {
                            int itemid = rs.getInt("itemid");
                            if (itemtype == null) {
                                retItems.add(ii.getName(itemid));
                            } else {
                                if (itemtype == ii.getInventoryType(itemid)) {
                                    retItems.add(ii.getName(itemid));
                                }
                            }
                        }
                        rs.close();
                        ps.close();
                        if (!retItems.isEmpty()) {
                            String retitemstring = "";
                            for (String singleRetItem : retItems) {
                                retitemstring += singleRetItem + ", ";
                            }
                            mc.dropMessage(retitemstring.substring(0, retitemstring.length() - 2));
                        } else {
                            if (itemtypestring != null) {
                                mc.dropMessage("This mob does not drop any items of the specified kind.");
                            } else {
                                mc.dropMessage("This mob does not drop any items.");
                            }
                        }
                    } catch (SQLException sqle) {
                        System.out.print("@monsterdrops failed with SQLException: " + sqle);
                    }
                }
            }
        } else if (splitted[0].equals("@gmlevel")) {
            if (splitted.length == 2) {
                try {
                    int gmlevel = Integer.parseInt(splitted[1]);
                    if (gmlevel >= 0) {
                        int accountgmlevel = 0;
                        Connection con = DatabaseConnection.getConnection();
                        PreparedStatement ps = con.prepareStatement("SELECT gm FROM accounts WHERE id = ?");
                        ps.setInt(1, player.getAccountID());
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) {
                            accountgmlevel = rs.getInt("gm");
                        }
                        rs.close();
                        ps.close();
                        if (gmlevel <= accountgmlevel) {
                            player.setGM(gmlevel);
                            mc.dropMessage("GM level successfully changed to " + gmlevel + ".");
                        }
                    }
                } catch (NumberFormatException ignored) {
                    
                }
            }
        } else if (splitted[0].equals("@online")) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                if (!cs.getPlayerStorage().getAllCharacters().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    mc.dropMessage("Channel " + cs.getChannel());
                    for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {
                        if (!chr.isGM()) {
                            if (sb.length() > 150) { // Chars per line. Could be more or less
                                mc.dropMessage(sb.toString());
                                sb = new StringBuilder();
                            }
                            sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName() + "   "));
                        }
                    }
                    mc.dropMessage(sb.toString());
                }
            }
        } else if (splitted[0].equals("@monstersinrange")) {
            int upperrange, lowerrange;
            boolean sortbylevel = false;
            String incorrectsyntax = "Incorrect syntax; instead use (where [] means optional): @monstersinrange [[lowerRange, upperRange], [sortBy (\"level\"/\"xphpratio\")]]";
            String note = "Note that if exactly one integer argument is present, it is parsed as upperRange.";
            switch (splitted.length) {
                case 1:
                    upperrange = 5;
                    lowerrange = 2;
                    sortbylevel = false;
                    break;
                case 2:
                    try {
                        upperrange = Integer.parseInt(splitted[1]);
                    } catch (NumberFormatException nfe) {
                        if (splitted[1].equalsIgnoreCase("level")) {
                            sortbylevel = true;
                        } else if (splitted[1].equalsIgnoreCase("xphpratio")) {
                            sortbylevel = false;
                        } else {
                            mc.dropMessage(incorrectsyntax);
                            mc.dropMessage(note);
                            return;
                        }
                        upperrange = 5;
                    }
                    lowerrange = 2;
                    break;
                case 3:
                    if (splitted[2].equalsIgnoreCase("level")) {
                        sortbylevel = true;
                        try {
                            upperrange = Integer.parseInt(splitted[1]);
                        } catch (NumberFormatException nfe) {
                            mc.dropMessage(incorrectsyntax);
                            mc.dropMessage(note);
                            return;
                        }
                        lowerrange = 2;
                    } else if (splitted[2].equalsIgnoreCase("xphpratio")) {
                        sortbylevel = false;
                        try {
                            upperrange = Integer.parseInt(splitted[1]);
                        } catch (NumberFormatException nfe) {
                            mc.dropMessage(incorrectsyntax);
                            mc.dropMessage(note);
                            return;
                        }
                        lowerrange = 2;
                    } else {
                        try {
                            upperrange = Integer.parseInt(splitted[2]);
                            lowerrange = Integer.parseInt(splitted[1]);
                        } catch (NumberFormatException nfe) {
                            mc.dropMessage(incorrectsyntax);
                            mc.dropMessage(note);
                            return;
                        }
                    }
                    break;
                case 4:
                    try {
                        upperrange = Integer.parseInt(splitted[2]);
                        lowerrange = Integer.parseInt(splitted[1]);
                    } catch (NumberFormatException nfe) {
                        mc.dropMessage(incorrectsyntax);
                        mc.dropMessage(note);
                        return;
                    }
                    if (splitted[3].equalsIgnoreCase("level")) {
                        sortbylevel = true;
                    } else if (splitted[3].equalsIgnoreCase("xphpratio")) {
                        sortbylevel = false;
                    } else {
                        mc.dropMessage(incorrectsyntax);
                        mc.dropMessage(note);
                        return;
                    }
                    break;
                default:
                    mc.dropMessage(incorrectsyntax);
                    mc.dropMessage(note);
                    return;
            }
            if (upperrange >= 0 && upperrange <= 10 && lowerrange >= 0 && lowerrange <= 10) {
                int max = player.getLevel() + upperrange;
                int min = player.getLevel() - lowerrange;
                MapleData data;
                MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
                data = dataProvider.getData("Mob.img");
                List<MapleMonster> mobList = new ArrayList<>();
                try {
                    for (MapleData mobIdData : data.getChildren()) {
                        int mobIdFromData = Integer.parseInt(mobIdData.getName());
                        MapleMonster mm = MapleLifeFactory.getMonster(mobIdFromData);
                        if (mm != null) {
                            if (mm.getLevel() >= min && mm.getLevel() <= max) {
                                mobList.add(mm);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!mobList.isEmpty()) {
                    if (sortbylevel) {
                        mobList.sort((o1, o2) -> {
                            int comparison = Integer.valueOf(o1.getLevel()).compareTo(o2.getLevel());
                            if (comparison < 0) {
                                return -1;
                            } else {
                                if (comparison > 0) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        });
                    } else {
                        final MapleCharacter p = player;
                        mobList.sort((o1, o2) -> {
                            double xphpratio1 = ((double) o1.getExp() * p.getTotalMonsterXp(o1.getLevel())) / (double) o1.getHp();
                            double xphpratio2 = ((double) o2.getExp() * p.getTotalMonsterXp(o2.getLevel())) / (double) o2.getHp();
                            int comparison = Double.valueOf(xphpratio1).compareTo(xphpratio2);
                            if (comparison < 0) {
                                return -1;
                            } else {
                                if (comparison > 0) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        });
                    }
                    
                    for (MapleMonster mob : mobList) {
                        double xphpratio = ((double) mob.getExp() * player.getTotalMonsterXp(mob.getLevel())) / (double) mob.getHp();
                        BigDecimal xhrbd = BigDecimal.valueOf(xphpratio);
                        xhrbd = xhrbd.setScale(2, RoundingMode.HALF_UP);
                        mc.dropMessage(mob.getName() + ": level " + mob.getLevel() + ", XP/HP ratio " + xhrbd.toString());
                    }
                    String sort;
                    if (sortbylevel) {
                        sort = "level";
                    } else {
                        sort = "XP/HP ratio";
                    }
                    mc.dropMessage("The above mobs are within " + lowerrange + " levels below and " + upperrange + " levels above you, sorted by " + sort + ", descending as you scroll upwards.");
                } else {
                    mc.dropMessage("No mobs are in the specified range.");
                }
            } else {
                mc.dropMessage("Invalid syntax, or range too large.");
            }
        } else if (splitted[0].equals("@monstertrialtier")) {
            mc.dropMessage("Your Monster Trial tier: " + player.getMonsterTrialTier() + " Your Monster Trial points: " + player.getMonsterTrialPoints() + " Points for next tier: " + player.getTierPoints(player.getMonsterTrialTier() + 1));
        } else if (splitted[0].equals("@damagescale")) {
            float damagescale = player.getDamageScale();
            BigDecimal ds = new BigDecimal(damagescale);
            ds = ds.setScale(1, RoundingMode.HALF_UP);
            mc.dropMessage("Your current damage scale: " + ds.toString() + "x");
        } else if (splitted[0].equals("@votepoints")) {
            mc.dropMessage("Your current vote point count: " + player.getVotePoints());
        } else if (splitted[0].equals("@morgue")) {
            if (splitted.length != 2) {
                mc.dropMessage("Incorrect syntax. Use: @morgue <playername>");
            }
            String name = splitted[1];
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (victim != null) {
                List<List<Integer>> morgue = victim.getPastLives();
                if (morgue.isEmpty()) {
                    mc.dropMessage("This player has no past lives; this is their first life lived.");
                } else {
                    mc.dropMessage("Past 5 lives, from oldest to most recent:");
                    for (int i = morgue.size() - 1; i >= 0; --i) {
                        String causeofdeath;
                        if (morgue.get(i).get(2) == 0) {
                            causeofdeath = "Suicide";
                        } else {
                            MapleMonster mobcause = MapleLifeFactory.getMonster(morgue.get(i).get(2));
                            causeofdeath = mobcause != null ? mobcause.getName() : "Suicide";
                        }
                        mc.dropMessage("Level: " + morgue.get(i).get(0) + ", Job: " + MapleJob.getJobName(morgue.get(i).get(1)) + ", Cause of death: " + causeofdeath + ".");
                    }
                }
            } else {
                mc.dropMessage("There exists no such player.");
            }
        } else if (splitted[0].equals("@deathinfo")) {
            if (splitted.length != 2) {
                mc.dropMessage("Incorrect syntax. Use: @deathinfo <playername>");
            }
            String name = splitted[1];
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (victim != null) {
                mc.dropMessage("Death info for " + victim.getName() + ":");
                mc.dropMessage("Death count | " + victim.getDeathCount());
                mc.dropMessage("Highest level achieved | " + victim.getHighestLevelAchieved());
                mc.dropMessage("Paragon level | " + (victim.getTotalParagonLevel()));
                mc.dropMessage("Suicide count | " + victim.getSuicides());
            } else {
                mc.dropMessage("There exists no such player on your channel.");
            }
        } else if (splitted[0].equals("@expboostinfo")) {
            if (player.getExpBonus()) {
                long timeleft = player.getExpBonusEnd() - System.currentTimeMillis();
                long hours = timeleft / (long) 3600000;
                timeleft %= (long) 3600000;
                long minutes = timeleft / (long) 60000;
                timeleft %= (long) 60000;
                long seconds = timeleft / (long) 1000;
                mc.dropMessage("Your " + player.getExpBonusMulti() + "x exp boost lasts for another " + hours + " hours, " + minutes + " minutes, and " + seconds + " seconds.");
            } else {
                mc.dropMessage("You do not currently have an exp boost active.");
            }
        } else if (splitted[0].equals("@deathpenalty")) {
            if (player.getDeathPenalty() == 0) {
                mc.dropMessage("You do not currently have any death penalties.");
            } else {
                int hppenalty, mppenalty;
                switch (player.getJob().getId() / 100) {
                    case 0: // Beginner
                        hppenalty = 95;
                        mppenalty = 0;
                        break;
                    case 1: // Warrior
                        hppenalty = 400;
                        mppenalty = 60;
                        break;
                    case 2: // Mage
                        hppenalty = 75;
                        mppenalty = 400;
                        break;
                    case 3: // Archer
                        hppenalty = 135;
                        mppenalty = 70;
                        break;
                    case 4: // Rogue
                        hppenalty = 145;
                        mppenalty = 70;
                        break;
                    case 5: // Pirate
                        hppenalty = 135;
                        mppenalty = 70;
                        break;
                    default: // GM, or something went wrong
                        hppenalty = 135;
                        mppenalty = 75;
                        break;
                }
                mc.dropMessage("Current death penalty level: " + player.getDeathPenalty());
                mc.dropMessage("Current effects: -" + (hppenalty * player.getDeathPenalty()) + " maxHP, -" + (mppenalty * player.getDeathPenalty()) + " maxMP,");
                mc.dropMessage("-" + Math.min(3 * player.getDeathPenalty(), 100) + "% weapon damage, -" + Math.min(3 * player.getDeathPenalty(), 100) + "% magic damage");
                mc.dropMessage(player.getStrengtheningTimeString());
            }
        } else if (splitted[0].equals("@monsterhp")) {
            for (MapleMapObject mmo : player.getMap().getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                MapleMonster monster = (MapleMonster) mmo;
                double hppercentage = ((double) monster.getHp()) / ((double) monster.getMaxHp()) * 100.0;
                mc.dropMessage("Monster: " + monster.getName() + ", HP: " + hppercentage + "%");
            }
        } else if (splitted[0].equals("@truedamage")) {
            player.toggleTrueDamage();
            String s = player.getTrueDamage() ? "on" : "off";
            mc.dropMessage("True damage is now turned " + s + ".");
        } else if (splitted[0].equals("@bosshp")) {
            for (MapleMapObject mmo : player.getMap().getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                MapleMonster monster = (MapleMonster) mmo;
                if (monster.isBoss()) {
                    double hppercentage = ((double) monster.getHp()) / ((double) monster.getMaxHp()) * 100.0;
                    mc.dropMessage("Monster: " + monster.getName() + ", HP: " + hppercentage + "%");
                }
            }
        } else if (splitted[0].equals("@donated")) {
            NPCScriptManager npc = NPCScriptManager.getInstance();
            npc.start(c, 9010010);
        } else if (splitted[0].equals("@cancelquest")) {
            player.getCQuest().loadQuest(0);
            player.setQuestId(0);
            player.resetQuestKills();
            player.sendHint("#eQuest canceled.");
        } else if (splitted[0].equals("@vote")) {
            player.dropVoteTime();
        } else if (splitted[0].equals("@sell")) {
            NPCScriptManager npc = NPCScriptManager.getInstance();
            npc.start(c, 9201081);
        } else if (splitted[0].equals("@showpqpoints")) {
            player.toggleShowPqPoints();
            String s = player.showPqPoints() ? "on" : "off";
            mc.dropMessage("PQ point display is now turned " + s + ".");
        } else if (splitted[0].equals("@readingtime")) {
            if (player.getReadingTime() > 0) {
                long sittingTime = System.currentTimeMillis() - ((long) player.getReadingTime() * 1000);
                long hours = sittingTime / (long) 3600000;
                sittingTime %= (long) 3600000;
                long minutes = sittingTime / (long) 60000;
                sittingTime %= (long) 60000;
                long seconds = sittingTime / (long) 1000;
                player.dropMessage("You've been reading for a total of " + hours + " hours, " + minutes + " minutes, and " + seconds + " seconds this session.");
            } else {
                player.dropMessage("It doesn't look like you're reading at the moment.");
            }
        } else if (splitted[0].equals("@defense") || splitted[0].equals("@defence")) {
            player.dropMessage("Weapon defense: " + player.getTotalWdef() + ", magic defense: " + player.getTotalMdef());
        } else if (splitted[0].equals("@ria")) {
            NPCScriptManager.getInstance().start(c, 9010003);
        } else if (splitted[0].equals("@pqpoints")) {
            if (player.getPartyQuest() == null) {
                mc.dropMessage("You are not currently in a PQ that has points.");
                return;
            }
            mc.dropMessage("Current PQ point total: " + player.getPartyQuest().getPoints());
        } else if (splitted[0].equals("@overflowexp")) {
            if (splitted.length != 2) {
                mc.dropMessage("Incorrect syntax. Use: @overflowexp <playername>");
            }
            String name = splitted[1];
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (victim != null) {
                String rawOverflow = "" + victim.getOverflowExp();
                List<String> digitGroupings = new ArrayList<>(5);
                digitGroupings.add(rawOverflow.substring(0, rawOverflow.length() % 3));
                for (int i = rawOverflow.length() % 3; i < rawOverflow.length(); i += 3) {
                    digitGroupings.add(rawOverflow.substring(i, i + 3));
                }
                mc.dropMessage(
                    victim.getName() +
                    "'s total overflow EXP: " +
                    digitGroupings.stream()
                                  .reduce((accu, grouping) -> accu + "," + grouping)
                                  .orElse("0")
                );
            } else {
                mc.dropMessage("There exists no such player on your channel.");
            }
        }
    }

    private void compareTime(StringBuilder sb, long timeDiff) {
        double secondsAway = timeDiff / 1000;
        double minutesAway = 0;
        double hoursAway = 0;

        while (secondsAway > 60) {
            minutesAway++;
            secondsAway -= 60;
        }
        while (minutesAway > 60) {
            hoursAway++;
            minutesAway -= 60;
        }
        boolean hours = false;
        boolean minutes = false;
        if (hoursAway > 0) {
            sb.append(" ");
            sb.append((int) hoursAway);
            sb.append(" hours");
            hours = true;
        }
        if (minutesAway > 0) {
            if (hours) {
                sb.append(" -");
            }
            sb.append(" ");
            sb.append((int) minutesAway);
            sb.append(" minutes");
            minutes = true;
        }
        if (secondsAway > 0) {
            if (minutes) {
                sb.append(" and");
            }
            sb.append(" ");
            sb.append((int) secondsAway);
            sb.append(" seconds.");
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("command", 0),
            new CommandDefinition("commands", 0),
            new CommandDefinition("help", 0),
            new CommandDefinition("checkstats", 0),
            new CommandDefinition("save", 0),
            new CommandDefinition("expfix", 0),
            new CommandDefinition("dispose", 0),
            new CommandDefinition("mapfix", 0),
            new CommandDefinition("questinfo", 0),
            new CommandDefinition("togglesmega", 0),
            new CommandDefinition("str", 0),
            new CommandDefinition("dex", 0),
            new CommandDefinition("int", 0),
            new CommandDefinition("luk", 0),
            new CommandDefinition("gm", 0),
            new CommandDefinition("afk", 0),
            new CommandDefinition("onlinetime", 0),
            new CommandDefinition("monstertrialtime", 0),
            new CommandDefinition("mapleadmin", 0),
            new CommandDefinition("monsterlevels", 0),
            new CommandDefinition("absolutexprate", 0),
            new CommandDefinition("whodrops", 0),
            new CommandDefinition("gmlevel", 0),
            new CommandDefinition("online", 0),
            new CommandDefinition("monstersinrange", 0),
            new CommandDefinition("monstertrialtier", 0),
            new CommandDefinition("damagescale", 0),
            new CommandDefinition("votepoints", 0),
            new CommandDefinition("morgue", 0),
            new CommandDefinition("deathinfo", 0),
            new CommandDefinition("expboostinfo", 0),
            new CommandDefinition("deathpenalty", 0),
            new CommandDefinition("monsterhp", 0),
            new CommandDefinition("truedamage", 0),
            new CommandDefinition("bosshp", 0),
            new CommandDefinition("donated", 0),
            new CommandDefinition("monsterdrops", 0),
            new CommandDefinition("cancelquest", 0),
            new CommandDefinition("vote", 0),
            new CommandDefinition("sell", 0),
            new CommandDefinition("showpqpoints", 0),
            new CommandDefinition("readingtime", 0),
            new CommandDefinition("defense", 0),
            new CommandDefinition("defence", 0),
            new CommandDefinition("ria", 0),
            new CommandDefinition("pqpoints", 0),
            new CommandDefinition("overflowexp", 0)
        };
    }
}
