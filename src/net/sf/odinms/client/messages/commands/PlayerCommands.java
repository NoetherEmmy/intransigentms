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
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.SoundInformationProvider;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleMonsterStats;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.quest.MapleQuest;
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
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class PlayerCommands implements Command {
    @Override
    public void execute(final MapleClient c, final MessageCallback mc, final String... splitted) throws Exception {
        splitted[0] = splitted[0].toLowerCase();
        final MapleCharacter player = c.getPlayer();
        if (splitted[0].equals("@command") || splitted[0].equals("@commands") || splitted[0].equals("@help")) {
            mc.dropMessage("================================================================");
            mc.dropMessage("               " + c.getChannelServer().getServerName() + " Commands");
            mc.dropMessage("================================================================");
            mc.dropMessage("@snipedisplay - | - Toggles displaying the damage you do every time you use the Snipe skill.");
            mc.dropMessage("@samsara - | - Displays the current cooldown for the Samsara ability.");
            mc.dropMessage("@checkstats - | - Displays your stats.");
            mc.dropMessage("@save - | - Saves your progress.");
            mc.dropMessage("@expfix - | - Fixes your negative experience.");
            mc.dropMessage("@dispose - | - Unsticks you from any hanging NPC interactions.");
            mc.dropMessage("@mapfix - | - Fixes you if you've fallen off the map.");
            mc.dropMessage("@music [section_number] [title] - | - Changes the background music for the map you're in.");
            mc.dropMessage("@engage <partner_name> - | - Begins the process of engagement for marriage.");
            mc.dropMessage("@questinfo - | - Gets the info for your current IntransigentQuest.");
            mc.dropMessage("@questeffectivelevel - | - Displays your current quest effective level and its damage penalties.");
            mc.dropMessage("@ria - | - Opens chat with Ria to get info about IntransigentQuests.");
            mc.dropMessage("@cancelquest - | - Cancels your current quest.");
            mc.dropMessage("@togglesmega - | - Turns smegas off/on.");
            mc.dropMessage("@str/@dex/@int/@luk <number> - | - Automatically adds AP to your stats.");
            mc.dropMessage("@gm <message> - | - Sends a message to the GMs online.");
            mc.dropMessage("@afk <playername> - | - Shows how long a person has been AFK.");
            mc.dropMessage("@onlinetime - | - Shows how long a person has been online.");
            mc.dropMessage("@online - | - Lists all online players.");
            mc.dropMessage("@event - | - Teleports you to the currectly active event, if there is one.");
            mc.dropMessage("@roll <dice> [dice...] - | - Rolls some dice.");
            mc.dropMessage("@monstertrialtime - | - Shows how much longer you must wait to enter another Monster Trial.");
            mc.dropMessage("@dailyprize - | - Displays the amount of time you have until you can get another prize from T-1337.");
            mc.dropMessage("@mapleadmin - | - Opens up chat with Maple Adminstrator NPC.");
            mc.dropMessage("@monsterlevels - | - Displays levels and relative XP multipliers for all monsters on the map.");
            mc.dropMessage("@absolutexprate - | - Displays your current XP multiplier before relative multipliers.");
            mc.dropMessage("@monstersinrange [levels_below] [levels_above] - | - Lists monsters within the range specified of your level. Both arguments are optional.");
            mc.dropMessage("@monstertrialtier - | - Lists your current Monster Trial tier, points, and points needed for next tier.");
            mc.dropMessage("@damagescale - | - Displays the current multiplier by which all incoming damage is multiplied.");
            mc.dropMessage("@votepoints - | - Displays your current vote point count.");
            mc.dropMessage("@sell - | - Opens up an NPC to mass-sell equipment items.");
            mc.dropMessage("@buyback - | - Opens up an NPC to buy back items sold at NPC shops or sold using @sell.");
            mc.dropMessage("@vskills - | - Allows viewing and taking points in/out of virtual skills.");
            mc.dropMessage("@morgue <playername> - | - Displays the past 5 lives of the player.");
            mc.dropMessage("@deathinfo <playername> - | - Displays death count, highest level achieved, paragon level, and suicide count of player.");
            mc.dropMessage("@overflowexp <playername> - | - Displays overflow EXP (EXP gained past level 250) for the player.");
            mc.dropMessage("@expboostinfo - | - Displays how much time you have left on your EXP bonus.");
            mc.dropMessage("@deathpenalty - | - Displays your current death penalty level and its effects, as well as how long until you can next rest.");
            mc.dropMessage("@defense/@defence - | - Displays your true current weapon and magic defense.");
            mc.dropMessage("@magic - | - Displays your true current magic attack.");
            mc.dropMessage("@monsterhp - | - Displays the current HP % of all mobs on the map.");
            mc.dropMessage("@bosshp [repeat_time_in_milliseconds] - | - Displays the current HP % of all bosses on the map once, or optionally repeating (if specified). Cancels previous @bosshp displays.");
            mc.dropMessage("@showvuln [repeat_time_in_milliseconds] - | - Same as @bosshp, but instead displays the vulnerability of all monsters in the map that have a vulnerability greater than 100%.");
            mc.dropMessage("@truedamage - | - Toggles the display of true damage received.");
            mc.dropMessage("@whodrops - | - Allows selection of an item and lists monsters who drop the selected item.");
            mc.dropMessage("@whodrops <itemid> - | - Lists monsters who drop the item with that ID.");
            mc.dropMessage("@whodrops <searchstring> - | - Lists monsters who drop the item with the name that is the closest fit for <searchstring>.");
            mc.dropMessage("@whoquestdrops <searchstring> - | - Lists monsters who drop as a quest drop the item with the name that is the closest fit for <searchstring>.");
            mc.dropMessage("@monsterdrops <monsterid> [eqp/etc/use] - | - Lists all items (of the specified type, if specified) that a monster drops.");
            mc.dropMessage("@monsterdrops <searchstring> [eqp/etc/use] - | - Lists all items (of the specified type, if specified) that a monster drops.");
            mc.dropMessage("@pqpoints - | - Displays your current PQ point total.");
            mc.dropMessage("@showpqpoints - | - Toggles whether or not your current PQ point total is displayed every time the total is changed.");
            //mc.dropMessage("@readingtime - | - Displays how long you've been reading.");
            mc.dropMessage("@donated - | - Allows access to donator benefits.");
            mc.dropMessage("@voteupdate - | - Updates your total vote point and NX count, for when you vote while still in-game.");
            mc.dropMessage("@vote - | - Displays the amount of time left until you may vote again.");
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
            final MapleMap curMap = player.getMap();
            try {
                if (curMap.getGroundBelow(player.getPosition()) == null) {
                    player.changeMap(player.getMapId(), 0);
                } else {
                    player.dropMessage(
                        6,
                        "You are not currently stuck. Ground below: (" +
                            curMap.getGroundBelow(player.getPosition()).x +
                            ", " +
                            curMap.getGroundBelow(player.getPosition()).y +
                            ")"
                    );
                }
            } catch (final Exception e) {
                mc.dropMessage("@mapfix failed: " + e);
                e.printStackTrace();
            }
        } else if (splitted[0].equals("@questinfo")) {
            if (splitted.length == 1) {
                mc.dropMessage(
                    "Active quest slots (" +
                        player.getQuestSlots() +
                        " available slot" +
                        (player.getQuestSlots() < 2 ? "" : "s") +
                        "):"
                );
                boolean onAQuest = false;
                byte currSlot = (byte) 1;
                for (final CQuest cQuest : player.getCQuests()) {
                    if (cQuest != null && cQuest.getQuest().getId() != 0) {
                        mc.dropMessage("    " + currSlot + ". " + cQuest.getQuest().getTitle());
                        onAQuest = true;
                    }
                    currSlot++;
                }
                if (!onAQuest) {
                    mc.dropMessage("    You don't have any quests currently underway.");
                }
            } else if (splitted[1].equalsIgnoreCase("all")) {
                boolean onAQuest = false;
                byte currSlot = (byte) 1;
                for (final CQuest cQuest : player.getCQuests()) {
                    if (cQuest != null && cQuest.getQuest().getId() != 0) {
                        mc.dropMessage("--------------------------------------------");
                        mc.dropMessage("Slot " + currSlot + ": " + cQuest.getQuest().getTitle());
                        cQuest.getQuest().readMonsterTargets().forEach((mobId, qtyAndName) ->
                            mc.dropMessage(
                                "    " +
                                    qtyAndName.getRight() +
                                    "s killed: " +
                                    cQuest.getQuestKills(mobId) +
                                    "/" +
                                    qtyAndName.getLeft()
                            )
                        );
                        cQuest.getQuest().readItemsToCollect().forEach((itemId, qtyAndName) ->
                            mc.dropMessage(
                                "    " +
                                    qtyAndName.getRight() +
                                    "s collected: " +
                                    player.getQuestCollected(itemId) +
                                    "/" +
                                    qtyAndName.getLeft()
                            )
                        );
                        cQuest.getQuest().readOtherObjectives().forEach((name, count) ->
                            mc.dropMessage(
                                "    " +
                                    name +
                                    ": " +
                                    cQuest.getObjectiveProgress(name) +
                                    "/" +
                                    count
                            )
                        );
                        mc.dropMessage(
                            "    Quest effective level: " +
                                (
                                    cQuest.getEffectivePlayerLevel() > 0 ?
                                        cQuest.getEffectivePlayerLevel() :
                                        player.getLevel()
                                )
                        );
                        if (cQuest.canComplete()) {
                            mc.dropMessage("    [Quest is ready to turn in]");
                        } else {
                            mc.dropMessage("    [Quest still in progress]");
                        }
                        onAQuest = true;
                    }
                    currSlot++;
                }
                if (!onAQuest) {
                    mc.dropMessage("You don't have any quests currently underway.");
                }
            } else {
                final byte questSlot;
                try {
                    questSlot = Byte.parseByte(splitted[1]);
                } catch (final NumberFormatException nfe) {
                    mc.dropMessage("Invalid syntax. Use: @questinfo | @questinfo all | @questinfo <quest_slot>");
                    return;
                }
                if (questSlot < 1 || questSlot > player.getQuestSlots()) {
                    mc.dropMessage("You don't have that slot.");
                    return;
                }
                final CQuest cQuest = player.getCQuest(questSlot);
                if (cQuest == null || cQuest.getQuest().getId() == 0) {
                    mc.dropMessage("You don't currently have an active quest in that slot.");
                    return;
                }
                mc.dropMessage(cQuest.getQuest().getTitle());
                cQuest.getQuest().readMonsterTargets().forEach((mid, qtyAndName) ->
                    mc.dropMessage(
                        "    " +
                            qtyAndName.getRight() +
                            "s killed: " +
                            cQuest.getQuestKills(mid) +
                            "/" +
                            qtyAndName.getLeft()
                    )
                );
                cQuest.getQuest().readItemsToCollect().forEach((itemId, qtyAndName) ->
                    mc.dropMessage(
                        "    " +
                            qtyAndName.getRight() +
                            "s collected: " +
                            player.getQuestCollected(itemId) +
                            "/" +
                            qtyAndName.getLeft()
                    )
                );
                cQuest.getQuest().readOtherObjectives().forEach((name, count) ->
                    mc.dropMessage(
                        "    " +
                            name +
                            ": " +
                            cQuest.getObjectiveProgress(name) +
                            "/" +
                            count
                    )
                );
                mc.dropMessage(
                    "    Quest effective level: " +
                        (
                            cQuest.getEffectivePlayerLevel() > 0 ?
                                cQuest.getEffectivePlayerLevel() :
                                player.getLevel()
                        )
                );
                if (cQuest.canComplete()) {
                    mc.dropMessage("    [Quest is ready to turn in]");
                } else {
                    mc.dropMessage("    [Quest still in progress]");
                }
            }
        } else if (splitted[0].equals("@togglesmega")) {
            player.setSmegaEnabled(!player.getSmegaEnabled());
            final String text =
                !player.getSmegaEnabled() ?
                    "[Disable] Smegas are now disabled." :
                    "[Enable] Smegas are now enabled.";
            mc.dropMessage(text);
        } else if (
            splitted[0].equals("@str") ||
            splitted[0].equals("@dex") ||
            splitted[0].equals("@int") ||
            splitted[0].equals("@luk")
        ) {
            if (splitted.length != 2) {
                mc.dropMessage("Syntax: @<stat> <amount>");
                mc.dropMessage("stat: <STR> <DEX> <INT> <LUK>");
                return;
            }
            final int x;
            try {
                x = Integer.parseInt(splitted[1]);
            } catch (final NumberFormatException nfe) {
                mc.dropMessage("Syntax: @<stat> <amount>");
                mc.dropMessage("stat: <STR> <DEX> <INT> <LUK>");
                return;
            }
            final int max = 30000;
            if (x > 0 && x <= player.getRemainingAp() && x < max) {
                if (splitted[0].equals("@str") && x + player.getStr() < max) {
                    player.addAP(c, 1, x);
                } else if (splitted[0].equals("@dex") && x + player.getDex() < max) {
                    player.addAP(c, 2, x);
                } else if (splitted[0].equals("@int") && x + player.getInt() < max) {
                    player.addAP(c, 3, x);
                } else if (splitted[0].equals("@luk") && x + player.getLuk() < max) {
                    player.addAP(c, 4, x);
                } else {
                    mc.dropMessage(
                        "Make sure the stat you are trying to raise will not be over " +
                            max +
                            "."
                    );
                }
            } else {
                mc.dropMessage("Please make sure your AP is valid.");
            }
        } else if (splitted[0].equals("@gm")) {
            if (splitted.length < 2) return;
            try {
                c.getChannelServer()
                 .getWorldInterface()
                 .broadcastGMMessage(
                     null,
                     MaplePacketCreator.serverNotice(
                         6,
                         "Channel: " +
                             c.getChannel() +
                             "  " +
                             player.getName() +
                             ": " +
                             StringUtil.joinStringFrom(splitted, 1)
                     ).getBytes()
                 );
            } catch (final RemoteException ex) {
                c.getChannelServer().reconnectWorld();
            }
            mc.dropMessage("Message sent.");
            //player.dropMessage(1, "Please don't flood GMs with your messages.");
        } else if (splitted[0].equals("@afk")) {
            if (splitted.length >= 2) {
                final String name = splitted[1];
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                if (victim == null) {
                    try {
                        final WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
                        final int channel = wci.find(name);
                        if (channel == -1) {
                            mc.dropMessage("This player is not currently online.");
                            return;
                        }
                        victim = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(name);
                        if (victim == null || victim.isGM()) {
                            mc.dropMessage("This player is not currently online.");
                            return;
                        }
                    } catch (final RemoteException re) {
                        c.getChannelServer().reconnectWorld();
                        return;
                    }
                }
                if (victim.isGM()) {
                    mc.dropMessage("This player is not currently online.");
                    return;
                }
                final long blahblah = System.currentTimeMillis() - victim.getAfkTime();
                if (Math.floor(blahblah / 60000) == 0) { // Less than a minute
                    mc.dropMessage("This player has not been AFK in the last minute.");
                } else {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(victim.getName());
                    sb.append(" has been AFK for");
                    compareTime(sb, blahblah);
                    mc.dropMessage(sb.toString());
                }
            } else {
                mc.dropMessage("Invalid syntax. Use: @afk <player_name>");
            }
        } else if (splitted[0].equals("@onlinetime")) {
            if (splitted.length >= 2) {
                final String name = splitted[1];
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                if (victim == null) {
                    try {
                        final WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
                        final int channel = wci.find(name);
                        if (channel == -1) {
                            mc.dropMessage("This player is not online.");
                            return;
                        }
                        victim = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(name);
                        if (victim == null || victim.isGM()) {
                            mc.dropMessage("This player is not online.");
                            return;
                        }
                    } catch (final RemoteException re) {
                        c.getChannelServer().reconnectWorld();
                        return;
                    }
                }
                final long blahblah = System.currentTimeMillis() - victim.getLastLogin();
                final StringBuilder sb = new StringBuilder();
                sb.append(victim.getName());
                sb.append(" has been online for");
                compareTime(sb, blahblah);
                mc.dropMessage(sb.toString());
            } else {
                mc.dropMessage("Invalid syntax. Use: @onlinetime <player_name>");
            }
        } else if (splitted[0].equals("@monstertrialtime")) {
            if (System.currentTimeMillis() - player.getLastTrialTime() < 2L * 60L * 60L * 1000L) {
                final long timesincelast = System.currentTimeMillis() - player.getLastTrialTime();
                double inminutes = timesincelast / 60000.0d;
                inminutes = Math.floor(inminutes);
                final int cooldown = 120 - (int) inminutes;
                mc.dropMessage(
                    "You must wait " +
                        cooldown +
                        " more minute(s) before you may enter the Monster Trials again."
                );
            } else {
                mc.dropMessage("You may enter the Monster Trials.");
            }
        } else if (splitted[0].equals("@mapleadmin")) {
            final NPCScriptManager npc = NPCScriptManager.getInstance();
            npc.start(c, 9010000);
        } else if (splitted[0].equals("@monsterlevels")) {
            final List<Integer> monsterids = new ArrayList<>(4);
            double rx;
            int absxp = player.getAbsoluteXp();
            absxp *= c.getChannelServer().getExpRate();
            for (final MapleMonster monster : player.getMap().getAllMonsters()) {
                if (!monsterids.contains(monster.getId())) {
                    monsterids.add(monster.getId());
                    rx = player.getRelativeXp(monster.getLevel());
                    BigDecimal rxbd = new BigDecimal(rx);
                    rxbd = rxbd.setScale(2, RoundingMode.HALF_UP);
                    mc.dropMessage(
                        monster.getName() +
                            " | Level: " +
                            monster.getLevel() +
                            " Relative XP: " +
                            rxbd.toString() +
                            "x Total XP: " +
                            rxbd.multiply(BigDecimal.valueOf(absxp)) +
                            "x"
                    );
                }
            }
        } else if (splitted[0].equals("@absolutexprate")) {
            mc.dropMessage(
                "Your total absolute XP multiplier: " +
                    (player.getAbsoluteXp() * c.getChannelServer().getExpRate())
            );
        } else if (splitted[0].equalsIgnoreCase("@whodrops")) {
            if (splitted.length < 2) {
                final NPCScriptManager npc = NPCScriptManager.getInstance();
                npc.start(c, 9201094);
            } else {
                try {
                    final int searchId = Integer.parseInt(splitted[1]);
                    final Set<String> retMobs = new LinkedHashSet<>();
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    mc.dropMessage(ii.getName(searchId) + " (" + searchId + ") is dropped by the following mobs:");
                    final Connection con = DatabaseConnection.getConnection();
                    final PreparedStatement ps =
                        con.prepareStatement(
                            "SELECT monsterid FROM monsterdrops WHERE itemid = ?"
                        );
                    ps.setInt(1, searchId);
                    final ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        final int mobId = rs.getInt("monsterid");
                        final MapleMonster mob = MapleLifeFactory.getMonster(mobId);
                        if (mob != null) {
                            retMobs.add(mob.getName());
                        }
                    }
                    rs.close();
                    ps.close();
                    if (!retMobs.isEmpty()) {
                        for (final String singleRetMob : retMobs) {
                            mc.dropMessage(singleRetMob);
                        }
                    } else {
                        mc.dropMessage("No mobs drop this item.");
                    }
                } catch (final SQLException sqle) {
                    System.err.print("@whodrops failed: " + sqle);
                } catch (final NumberFormatException nfe) {
                    try {
                        final StringBuilder searchstring = new StringBuilder();
                        for (int i = 1; i < splitted.length; ++i) {
                            if (i == 1) {
                                searchstring.append(splitted[i]);
                            } else {
                                searchstring.append(' ').append(splitted[i]);
                            }
                        }
                        if (searchstring.length() == 0) {
                            mc.dropMessage(
                                "Invalid syntax. Use: @whodrops | @whodrops <search_string> | @whodrops <item_id>"
                            );
                            return;
                        }
                        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        final String searchstring_ = searchstring.toString();
                        final Pair<Integer, String> consumecandidate = ii.getConsumeByName(searchstring_);
                        final Pair<Integer, String> eqpcandidate = ii.getEqpByName(searchstring_);
                        final Pair<Integer, String> etccandidate = ii.getEtcByName(searchstring_);
                        Pair<Integer, String> candidate = consumecandidate;
                        if (etccandidate != null && (candidate == null || etccandidate.getRight().length() < candidate.getRight().length())) {
                            candidate = etccandidate;
                        }
                        if (eqpcandidate != null && (candidate == null || eqpcandidate.getRight().length() < candidate.getRight().length())) {
                            candidate = eqpcandidate;
                        }

                        try {
                            final int searchid;
                            if (candidate != null) {
                                searchid = candidate.getLeft();
                            } else {
                                mc.dropMessage("No item could be found with the search string provided.");
                                return;
                            }
                            final Set<String> retMobs = new LinkedHashSet<>();
                            mc.dropMessage(candidate.getRight() + " is dropped by the following mobs:");
                            final Connection con = DatabaseConnection.getConnection();
                            final PreparedStatement ps =
                                con.prepareStatement(
                                    "SELECT monsterid FROM monsterdrops WHERE itemid = ?"
                                );
                            ps.setInt(1, searchid);
                            final ResultSet rs = ps.executeQuery();
                            while (rs.next()) {
                                final int mobId = rs.getInt("monsterid");
                                final MapleMonster mob = MapleLifeFactory.getMonster(mobId);
                                if (mob != null) {
                                    retMobs.add(mob.getName());
                                }
                            }
                            rs.close();
                            ps.close();
                            if (!retMobs.isEmpty()) {
                                for (final String singleRetMob : retMobs) {
                                    mc.dropMessage(singleRetMob);
                                }
                            } else {
                                mc.dropMessage("No mobs drop this item.");
                            }
                        } catch (final SQLException sqle) {
                            System.err.print("@whodrops failed: " + sqle);
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (splitted[0].equals("@monsterdrops")) {
            if (splitted.length < 2) {
                mc.dropMessage(
                    "Invalid syntax. Use: @monsterdrops <monster_id> [eqp/use/etc] | " +
                        "@monsterdrops <search_string> [eqp/use/etc]"
                );
            } else {
                try {
                    final int searchId = Integer.parseInt(splitted[1]);
                    MapleInventoryType itemType = null;
                    String itemTypeString = null;
                    if (splitted.length > 2) {
                        switch (splitted[2].toLowerCase()) {
                            case "eqp":
                                itemTypeString = "equip";
                                itemType = MapleInventoryType.EQUIP;
                                break;
                            case "use":
                                itemTypeString = "use";
                                itemType = MapleInventoryType.USE;
                                break;
                            case "etc":
                                itemTypeString = "etc";
                                itemType = MapleInventoryType.ETC;
                                break;
                            default:
                                mc.dropMessage(
                                    "Invalid syntax. Use: @monsterdrops <monster_id> [eqp/use/etc] | " +
                                        "@monsterdrops <search_string> [eqp/use/etc]"
                                );
                                return;
                        }
                    }
                    final Set<String> retItems = new LinkedHashSet<>();
                    final MapleMonster mob = MapleLifeFactory.getMonster(searchId);
                    if (mob == null) {
                        mc.dropMessage("There is no such monster with that ID.");
                        return;
                    }
                    if (itemTypeString != null) {
                        mc.dropMessage(
                            mob.getName() +
                                " (" +
                                searchId +
                                ") drops the following " +
                                itemTypeString +
                                " items:"
                        );
                    } else {
                        mc.dropMessage(mob.getName() + " (" + searchId + ") drops the following items:");
                    }
                    final Connection con = DatabaseConnection.getConnection();
                    final PreparedStatement ps =
                        con.prepareStatement(
                            "SELECT itemid FROM monsterdrops WHERE monsterid = ?"
                        );
                    ps.setInt(1, searchId);
                    final ResultSet rs = ps.executeQuery();
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    while (rs.next()) {
                        final int itemId = rs.getInt("itemid");
                        if (itemType == null || itemType == ii.getInventoryType(itemId)) {
                            retItems.add(ii.getName(itemId));
                        }
                    }
                    rs.close();
                    ps.close();
                    if (!retItems.isEmpty()) {
                        final StringBuilder retItemString_ = new StringBuilder();
                        for (final String singleRetItem : retItems) {
                            retItemString_.append(singleRetItem).append(", ");
                        }
                        final String retItemString = retItemString_.toString();
                        mc.dropMessage(retItemString.substring(0, retItemString.length() - 2));
                    } else {
                        if (itemTypeString != null) {
                            mc.dropMessage("This mob does not drop any items of the specified kind.");
                        } else {
                            mc.dropMessage("This mob does not drop any items.");
                        }
                    }
                } catch (final SQLException sqle) {
                    System.err.print("@monsterdrops failed: " + sqle);
                } catch (final NumberFormatException nfe) {
                    try {
                        int searchId = 0;
                        StringBuilder searchString = null;
                        MapleInventoryType itemType = null;
                        String itemTypeString = null;
                        for (int i = 1; i < splitted.length; ++i) {
                            if (i == 1) {
                                searchString = new StringBuilder(splitted[i]);
                            } else {
                                switch (splitted[i].toLowerCase()) {
                                    case "eqp":
                                        itemTypeString = "equip";
                                        itemType = MapleInventoryType.EQUIP;
                                        break;
                                    case "use":
                                        itemTypeString = "use";
                                        itemType = MapleInventoryType.USE;
                                        break;
                                    case "etc":
                                        itemTypeString = "etc";
                                        itemType = MapleInventoryType.ETC;
                                        break;
                                    default:
                                        itemType = null;
                                        searchString.append(' ').append(splitted[i]);
                                        break;
                                }
                            }
                        }
                        if (searchString == null) {
                            mc.dropMessage(
                                "Invalid syntax. Use: @monsterdrops <monsterid> [eqp/use/etc] " +
                                    "| @monsterdrops <searchstring> [eqp/use/etc]"
                            );
                            return;
                        }
                        searchString = new StringBuilder(searchString.toString().toUpperCase());
                        final Set<String> retItems = new LinkedHashSet<>();
                        String bestMatch = null;
                        final Set<Map.Entry<Integer, MapleMonsterStats>> monsterStats =
                            MapleLifeFactory
                                .readMonsterStats()
                                .entrySet();
                        final String searchString_ = searchString.toString();
                        for (final Map.Entry<Integer, MapleMonsterStats> ms : monsterStats) {
                            if (ms == null) continue;
                            final MapleMonsterStats stats = ms.getValue();
                            if (stats == null) continue;
                            final String name = ms.getValue().getName();
                            if (name == null) continue;
                            if (name.toUpperCase().startsWith(searchString_)) {
                                if (bestMatch == null || name.length() < bestMatch.length()) {
                                    bestMatch = name;
                                    searchId = ms.getKey();
                                }
                            }
                        }
                        if (bestMatch != null) {
                            if (itemTypeString != null) {
                                mc.dropMessage(bestMatch + " drops the following " + itemTypeString + " items:");
                            } else {
                                mc.dropMessage(bestMatch + " drops the following items:");
                            }
                        } else {
                            mc.dropMessage(
                                "No mobs were found that start with \"" +
                                    searchString_.toLowerCase() +
                                    "\"."
                            );
                            return;
                        }
                        final Connection con = DatabaseConnection.getConnection();
                        final PreparedStatement ps =
                            con.prepareStatement(
                                "SELECT itemid FROM monsterdrops WHERE monsterid = ?"
                            );
                        ps.setInt(1, searchId);
                        final ResultSet rs = ps.executeQuery();
                        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        while (rs.next()) {
                            final int itemId = rs.getInt("itemid");
                            if (itemType == null || itemType == ii.getInventoryType(itemId)) {
                                retItems.add(ii.getName(itemId));
                            }
                        }
                        rs.close();
                        ps.close();
                        if (!retItems.isEmpty()) {
                            final StringBuilder retItemString_ = new StringBuilder();
                            for (final String singleRetItem : retItems) {
                                retItemString_.append(singleRetItem).append(", ");
                            }
                            final String retItemString = retItemString_.toString();
                            mc.dropMessage(retItemString.substring(0, retItemString.length() - 2));
                        } else {
                            if (itemTypeString != null) {
                                mc.dropMessage("This mob does not drop any items of the specified kind.");
                            } else {
                                mc.dropMessage("This mob does not drop any items.");
                            }
                        }
                    } catch (final SQLException sqle) {
                        System.err.print("@monsterdrops failed: " + sqle);
                    }
                }
            }
        } else if (splitted[0].equals("@gmlevel")) {
            if (splitted.length == 2) {
                try {
                    final int gmlevel = Integer.parseInt(splitted[1]);
                    if (gmlevel >= 0) {
                        int accountgmlevel = 0;
                        final Connection con = DatabaseConnection.getConnection();
                        final PreparedStatement ps = con.prepareStatement("SELECT gm FROM accounts WHERE id = ?");
                        ps.setInt(1, player.getAccountID());
                        final ResultSet rs = ps.executeQuery();
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
                } catch (final NumberFormatException ignored) {
                }
            }
        } else if (splitted[0].equals("@online")) {
            for (final ChannelServer cs : ChannelServer.getAllInstances()) {
                if (cs.getPlayerStorage().getAllCharacters().stream().anyMatch(p -> !p.isGM())) {
                    StringBuilder sb = new StringBuilder();
                    mc.dropMessage("Channel " + cs.getChannel());
                    for (final MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {
                        if (!chr.isGM()) {
                            if (sb.length() + chr.getName().length() > 75) { // Chars per line. Could be more or less
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
            int upperrange;
            final int lowerrange;
            boolean sortbylevel = false;
            final String incorrectsyntax =
                "Invalid syntax. Use: @monstersinrange [lower_range] [upper_range] " +
                    "['level'|'xphpratio']";
            switch (splitted.length) {
                case 1:
                    upperrange = 5;
                    lowerrange = 2;
                    sortbylevel = false;
                    break;
                case 2:
                    try {
                        upperrange = Integer.parseInt(splitted[1]);
                    } catch (final NumberFormatException nfe) {
                        if (splitted[1].equalsIgnoreCase("level")) {
                            sortbylevel = true;
                        } else if (splitted[1].equalsIgnoreCase("xphpratio")) {
                            sortbylevel = false;
                        } else {
                            mc.dropMessage(incorrectsyntax);
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
                        } catch (final NumberFormatException nfe) {
                            mc.dropMessage(incorrectsyntax);
                            return;
                        }
                        lowerrange = 2;
                    } else if (splitted[2].equalsIgnoreCase("xphpratio")) {
                        sortbylevel = false;
                        try {
                            upperrange = Integer.parseInt(splitted[1]);
                        } catch (final NumberFormatException nfe) {
                            mc.dropMessage(incorrectsyntax);
                            return;
                        }
                        lowerrange = 2;
                    } else {
                        try {
                            upperrange = Integer.parseInt(splitted[2]);
                            lowerrange = Integer.parseInt(splitted[1]);
                        } catch (final NumberFormatException nfe) {
                            mc.dropMessage(incorrectsyntax);
                            return;
                        }
                    }
                    break;
                case 4:
                    try {
                        upperrange = Integer.parseInt(splitted[2]);
                        lowerrange = Integer.parseInt(splitted[1]);
                    } catch (final NumberFormatException nfe) {
                        mc.dropMessage(incorrectsyntax);
                        return;
                    }
                    if (splitted[3].equalsIgnoreCase("level")) {
                        sortbylevel = true;
                    } else if (splitted[3].equalsIgnoreCase("xphpratio")) {
                        sortbylevel = false;
                    } else {
                        mc.dropMessage(incorrectsyntax);
                        return;
                    }
                    break;
                default:
                    mc.dropMessage(incorrectsyntax);
                    return;
            }
            if (upperrange >= 0 && upperrange <= 30 && lowerrange >= 0 && lowerrange <= 30) {
                final int max = player.getLevel() + upperrange;
                final int min = player.getLevel() - lowerrange;
                final MapleData data;
                final MapleDataProvider dataProvider =
                    MapleDataProviderFactory.getDataProvider(
                        new File(System.getProperty(WorldServer.WZPATH) + "/" + "String.wz")
                    );
                data = dataProvider.getData("Mob.img");
                final List<MapleMonster> mobList = new ArrayList<>();
                try {
                    for (final MapleData mobIdData : data.getChildren()) {
                        final int mobIdFromData = Integer.parseInt(mobIdData.getName());
                        final MapleMonster mm = MapleLifeFactory.getMonster(mobIdFromData);
                        if (mm != null) {
                            if (mm.getLevel() >= min && mm.getLevel() <= max) {
                                mobList.add(mm);
                            }
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                if (!mobList.isEmpty()) {
                    if (sortbylevel) {
                        mobList.sort(Comparator.comparingInt(MapleMonster::getLevel));
                    } else {
                        mobList.sort((o1, o2) -> {
                            final double xphpratio1 = ((double) o1.getExp() * player.getTotalMonsterXp(o1.getLevel())) / (double) o1.getHp();
                            final double xphpratio2 = ((double) o2.getExp() * player.getTotalMonsterXp(o2.getLevel())) / (double) o2.getHp();
                            return Double.valueOf(xphpratio1).compareTo(xphpratio2);
                        });
                    }

                    for (final MapleMonster mob : mobList) {
                        final double xphpratio = ((double) mob.getExp() * player.getTotalMonsterXp(mob.getLevel())) / (double) mob.getHp();
                        BigDecimal xhrbd = BigDecimal.valueOf(xphpratio);
                        xhrbd = xhrbd.setScale(2, RoundingMode.HALF_UP);
                        mc.dropMessage(
                            mob.getName() +
                                ": level " +
                                mob.getLevel() +
                                ", XP/HP ratio " +
                                xhrbd
                        );
                    }
                    final String sort;
                    if (sortbylevel) {
                        sort = "level";
                    } else {
                        sort = "XP/HP ratio";
                    }
                    mc.dropMessage(
                        "The above mobs are within " +
                            lowerrange +
                            " levels below and " +
                            upperrange +
                            " levels above you, sorted by " +
                            sort +
                            ", descending as you scroll upwards."
                    );
                } else {
                    mc.dropMessage("No mobs are in the specified range.");
                }
            } else {
                mc.dropMessage("Invalid syntax, or range too large.");
            }
        } else if (splitted[0].equals("@monstertrialtier")) {
            mc.dropMessage(
                "Your Monster Trial tier: " +
                    player.getMonsterTrialTier() +
                    " Your Monster Trial points: " +
                    player.getMonsterTrialPoints() +
                    " Points for next tier: " +
                    player.getTierPoints(player.getMonsterTrialTier() + 1)
            );
        } else if (splitted[0].equals("@damagescale")) {
            final float damagescale = player.getDamageScale();
            BigDecimal ds = new BigDecimal(damagescale);
            ds = ds.setScale(1, RoundingMode.HALF_UP);
            mc.dropMessage("Your current damage scale: " + ds.toString() + "x");
        } else if (splitted[0].equals("@votepoints")) {
            mc.dropMessage("Your current vote point count: " + player.getVotePoints());
        } else if (splitted[0].equals("@morgue")) {
            if (splitted.length != 2) {
                mc.dropMessage("Invalid syntax. Use: @morgue <player_name>");
                return;
            }
            final String name = splitted[1];
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (victim != null) {
                final List<List<Integer>> morgue = victim.getPastLives();
                if (morgue.isEmpty()) {
                    mc.dropMessage("This player has no past lives; this is their first life lived.");
                } else {
                    mc.dropMessage("Past 5 lives, from oldest to most recent:");
                    for (int i = morgue.size() - 1; i >= 0; --i) {
                        final String causeofdeath;
                        if (morgue.get(i).get(2) == 0) {
                            causeofdeath = "Suicide";
                        } else {
                            final MapleMonster mobcause = MapleLifeFactory.getMonster(morgue.get(i).get(2));
                            causeofdeath = mobcause != null ? mobcause.getName() : "Suicide";
                        }
                        mc.dropMessage(
                            "Level: " +
                                morgue.get(i).get(0) +
                                ", Job: " +
                                MapleJob.getJobName(morgue.get(i).get(1)) +
                                ", Cause of death: " +
                                causeofdeath +
                                "."
                        );
                    }
                }
            } else {
                mc.dropMessage("There exists no such player.");
            }
        } else if (splitted[0].equals("@deathinfo")) {
            if (splitted.length != 2) {
                mc.dropMessage("Incorrect syntax. Use: @deathinfo <player_name>");
            }
            final String name = splitted[1];
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
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
                final long hours = timeleft / 3600000L;
                timeleft %= 3600000L;
                final long minutes = timeleft / 60000L;
                timeleft %= 60000L;
                final long seconds = timeleft / 1000L;
                mc.dropMessage(
                    "Your " +
                        player.getExpBonusMulti() +
                        "x exp boost lasts for another " +
                        hours +
                        " hours, " +
                        minutes +
                        " minutes, and " +
                        seconds +
                        " seconds."
                );
            } else {
                mc.dropMessage("You do not currently have an exp boost active.");
            }
        } else if (splitted[0].equals("@deathpenalty")) {
            if (player.getDeathPenalty() == 0) {
                mc.dropMessage("You do not currently have any death penalties.");
            } else {
                final int hppenalty;
                final int mppenalty;
                switch (player.getJob().getId() / 100) {
                    case 0: // Beginner
                        hppenalty = 190;
                        mppenalty = 0;
                        break;
                    case 1: // Warrior
                        hppenalty = 800;
                        mppenalty = 120;
                        break;
                    case 2: // Mage
                        hppenalty = 150;
                        mppenalty = 800;
                        break;
                    case 3: // Archer
                        hppenalty = 270;
                        mppenalty = 140;
                        break;
                    case 4: // Rogue
                        hppenalty = 190;
                        mppenalty = 140;
                        break;
                    case 5: // Pirate
                        hppenalty = 270;
                        mppenalty = 140;
                        break;
                    default: // GM, or something went wrong
                        hppenalty = 270;
                        mppenalty = 150;
                        break;
                }
                mc.dropMessage("Current death penalty level: " + player.getDeathPenalty());
                mc.dropMessage(
                    "Current effects: -" +
                        (hppenalty * player.getDeathPenalty()) +
                        " maxHP, -" +
                        (mppenalty * player.getDeathPenalty()) +
                        " maxMP,"
                );
                mc.dropMessage(
                    "-" +
                        Math.min(3 * player.getDeathPenalty(), 100) +
                        "% weapon damage, -" +
                        Math.min(3 * player.getDeathPenalty(), 100) +
                        "% magic damage"
                );
                mc.dropMessage(player.getStrengtheningTimeString());
            }
        } else if (splitted[0].equals("@monsterhp")) {
            final DecimalFormat df = new DecimalFormat("#.00");
            player.getMap().getMapObjectsInRange(
                new Point(),
                Double.POSITIVE_INFINITY,
                MapleMapObjectType.MONSTER
            )
            .stream()
            .map(mmo -> (MapleMonster) mmo)
            .forEach(mob -> {
                final double hpPercentage = (double) mob.getHp() / ((double) mob.getMaxHp()) * 100.0d;
                player.dropMessage("Monster: " + mob.getName() + ", HP: " + df.format(hpPercentage) + "%");
            });
        } else if (splitted[0].equals("@truedamage")) {
            player.toggleTrueDamage();
            final String s = player.getTrueDamage() ? "on" : "off";
            mc.dropMessage("True damage is now turned " + s + ".");
        } else if (splitted[0].equals("@bosshp")) {
            final int repeatTime;
            switch (splitted.length) {
                case 1:
                    repeatTime = 0;
                    break;
                case 2:
                    try {
                        repeatTime = Integer.parseInt(splitted[1]);
                    } catch (final NumberFormatException nfe) {
                        mc.dropMessage(
                            "Could not parse repeat time for @bosshp. Make sure you are entering a valid integer."
                        );
                        return;
                    }
                    if (repeatTime < 1000 || repeatTime > 300000) {
                        mc.dropMessage("Make sure the repeat time is between 1000 and 300000 milliseconds.");
                        return;
                    }
                    break;
                default:
                    mc.dropMessage("Invalid syntax. Use: @bosshp [repeat_time_in_milliseconds]");
                    return;
            }
            if (repeatTime > 0) {
                player.setBossHpTask(repeatTime, 1000L * 60L * 60L);
            } else {
                if (player.cancelBossHpTask()) {
                    mc.dropMessage("@bosshp display has been stopped.");
                }
                final DecimalFormat df = new DecimalFormat("##0.0##");
                player
                    .getMap()
                    .getAllMonsters()
                    .stream()
                    .filter(MapleMonster::isBoss)
                    .forEach(mob -> {
                        final double hpPercentage =
                            (double) mob.getHp() / ((double) mob.getMaxHp()) * 100.0d;
                        player.dropMessage(
                            "Monster: " +
                                mob.getName() +
                                ", HP: " +
                                df.format(hpPercentage) +
                                "%"
                        );
                    });
            }
        } else if (splitted[0].equals("@donated")) {
            final NPCScriptManager npc = NPCScriptManager.getInstance();
            npc.start(c, 9010010);
        } else if (splitted[0].equals("@cancelquest")) {
            if (splitted.length != 2) {
                mc.dropMessage("Invalid syntax. Use: @cancelquest <quest_slot>");
                return;
            }
            final byte questSlot;
            try {
                questSlot = Byte.parseByte(splitted[1]);
            } catch (final NumberFormatException nfe) {
                mc.dropMessage("Invalid input for quest slot.");
                return;
            }
            if (questSlot > player.getQuestSlots()) {
                mc.dropMessage("You don't have that many quest slots!");
                return;
            }
            final CQuest cQuest = player.getCQuest(questSlot);
            if (cQuest == null || cQuest.getQuest().getId() == 0) {
                mc.dropMessage("You don't currently have a quest active in that slot.");
                return;
            }
            final String questName = cQuest.getQuest().getTitle();
            player.forfeitCQuest(questSlot);
            player.sendHint("#eQuest #b" + questName + "#k canceled.");
        } else if (splitted[0].equals("@vote")) {
            player.dropVoteTime();
        } else if (splitted[0].equals("@sell")) {
            final NPCScriptManager npc = NPCScriptManager.getInstance();
            npc.start(c, 9201081);
        } else if (splitted[0].equals("@showpqpoints")) {
            player.toggleShowPqPoints();
            final String s = player.showPqPoints() ? "on" : "off";
            mc.dropMessage("PQ point display is now turned " + s + ".");
        } /*else if (splitted[0].equals("@readingtime")) {
            if (player.getReadingTime() > 0) {
                long sittingTime = System.currentTimeMillis() - ((long) player.getReadingTime() * 1000L);
                long hours = sittingTime / 3600000L;
                sittingTime %= 3600000L;
                long minutes = sittingTime / 60000L;
                sittingTime %= 60000L;
                long seconds = sittingTime / 1000L;
                player.dropMessage(
                    "You've been reading for a total of " +
                        hours +
                        " hours, " +
                        minutes +
                        " minutes, and " +
                        seconds +
                        " seconds this session."
                );
            } else {
                player.dropMessage("It doesn't look like you're reading at the moment.");
            }
        }*/ else if (splitted[0].equals("@defense") || splitted[0].equals("@defence")) {
            final int wdef = player.getTotalWdef();
            final int mdef = player.getTotalMdef();
            final double dodgeChance;
            if (wdef > 1999) {
                dodgeChance = 100.0d * (Math.log1p(wdef - 1999.0d) / Math.log(2.0d)) / 25.0d;
            } else {
                dodgeChance = 0.0d;
            }
            final int wAtkReduce = Math.max(wdef - 1999, 0);
            final int mAtkReduce;
            if (mdef > 1999) {
                mAtkReduce = (int) Math.ceil(Math.pow(mdef - 1999.0d, 1.2d));
            } else {
                mAtkReduce = 0;
            }
            final DecimalFormat df = new DecimalFormat("#.000");
            player.dropMessage("Weapon defense: " + wdef + ", magic defense: " + mdef);
            player.dropMessage("Chance to dodge weapon attacks based on defense: " + df.format(dodgeChance) + "%");
            player.dropMessage("Absolute damage reduction vs. weapon attacks: " + wAtkReduce);
            player.dropMessage("Absolute damage reduction vs. magic attacks: " + mAtkReduce);
        } else if (splitted[0].equals("@ria")) {
            NPCScriptManager.getInstance().start(c, 9010003);
        } else if (splitted[0].equals("@pqpoints")) {
            if (player.getPartyQuest() == null) {
                mc.dropMessage("You are not currently in a PQ that has points.");
                return;
            }
            mc.dropMessage("Current PQ point total: " + player.getPartyQuest().getPoints());
        } else if (splitted[0].equals("@overflowexp")) {
            if (splitted.length != 2 || !Pattern.matches("[A-Za-z][A-Za-z0-9]+", splitted[1])) {
                mc.dropMessage("Incorrect syntax. Use: @overflowexp <player_name>");
                return;
            }
            final String name = splitted[1];
            final String nameLower = name.toLowerCase();
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (victim == null) {
                // Not on this channel
                for (final ChannelServer cs : ChannelServer.getAllInstances()) {
                    if (!cs.getPlayerStorage().getAllCharacters().isEmpty()) {
                        victim = cs.getPlayerStorage()
                                   .getAllCharacters()
                                   .stream()
                                   .filter(chr -> nameLower.equals(chr.getName().toLowerCase()) && !chr.isGM())
                                   .findFirst()
                                   .orElse(null);
                    }
                    if (victim != null) break;
                }
            }
            Long overflowExp = null;
            if (victim == null) {
                // Not online
                final Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {
                    ps = con.prepareStatement("SELECT overflowexp FROM characters WHERE name LIKE ?");
                    ps.setString(1, name);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        overflowExp = rs.getLong("overflowexp");
                    }
                } catch (final SQLException sqle) {
                    mc.dropMessage("There was an exception finding the player specified.");
                    sqle.printStackTrace();
                    return;
                } finally {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                }
            }
            if (victim != null) {
                final String rawOverflow = "" + victim.getOverflowExp();
                final List<String> digitGroupings = new ArrayList<>(5);
                digitGroupings.add(rawOverflow.substring(0, rawOverflow.length() % 3));
                for (int i = rawOverflow.length() % 3; i < rawOverflow.length(); i += 3) {
                    digitGroupings.add(rawOverflow.substring(i, i + 3));
                }
                mc.dropMessage(
                    victim.getName() +
                        "'s total overflow EXP: " +
                        digitGroupings
                            .stream()
                            .filter(s -> !s.isEmpty())
                            .reduce((accu, grouping) -> accu + "," + grouping)
                            .orElse("0")
                );
            } else if (overflowExp != null) {
                final String rawOverflow = "" + overflowExp;
                final List<String> digitGroupings = new ArrayList<>(5);
                digitGroupings.add(rawOverflow.substring(0, rawOverflow.length() % 3));
                for (int i = rawOverflow.length() % 3; i < rawOverflow.length(); i += 3) {
                    digitGroupings.add(rawOverflow.substring(i, i + 3));
                }
                mc.dropMessage(
                    name +
                        "'s total overflow EXP: " +
                        digitGroupings
                            .stream()
                            .filter(s -> !s.isEmpty())
                            .reduce((accu, grouping) -> accu + "," + grouping)
                            .orElse("0")
                );
            } else {
                mc.dropMessage("There exists no such player.");
            }
        } else if (splitted[0].equals("@vskills")) {
            NPCScriptManager.getInstance().start(c, 9201095);
        } else if (splitted[0].equals("@voteupdate")) {
            player.voteUpdate();
        } else if (splitted[0].equals("@buyback")) {
            NPCScriptManager.getInstance().start(c, 9201097);
        } else if (splitted[0].equals("@snipedisplay")) {
            player.toggleShowSnipeDmg();
            mc.dropMessage("Snipe damage display is now " + (player.showSnipeDmg() ? "on" : "off") + ".");
        } else if (splitted[0].equals("@event")) {
            final int eventMapId = c.getChannelServer().getEventMap();
            if (eventMapId == 0) {
                mc.dropMessage(
                    "It doesn't look like there's an event going on in this channel at the moment. " +
                        "Maybe you're in the wrong channel?"
                );
            } else {
                mc.dropMessage("Going to the event, please wait...");
                TimerManager.getInstance().schedule(() -> {
                    if (player.isAlive() && player.getMapId() != 100 && player.getMapId() != eventMapId) {
                        player.setPreEventMap(player.getMapId());
                        player.changeMap(eventMapId);
                    }
                }, 4L * 1000L);
            }
        } else if (splitted[0].equals("@magic")) {
            mc.dropMessage("Your current total magic attack: " + player.getTotalMagic());
        } else if (splitted[0].equals("@samsara")) {
            if (player.getSkillLevel(5121000) > 0) {
                final StringBuilder sb = new StringBuilder();
                final long timeDiff =
                    player.getLastSamsara() +
                        MapleCharacter.SAMSARA_COOLDOWN -
                        System.currentTimeMillis();
                if (timeDiff > 0) {
                    sb.append("You may use Samsara again in ");
                    compareTime(
                        sb,
                        player.getLastSamsara() +
                            MapleCharacter.SAMSARA_COOLDOWN -
                            System.currentTimeMillis()
                    );
                } else {
                    sb.append("You may use Samsara.");
                }
                mc.dropMessage(sb.toString());
            } else {
                mc.dropMessage("You do not have access to the Samsara ability.");
            }
        } else if (splitted[0].equals("@dailyprize")) {
            player.dropDailyPrizeTime(true);
        } else if (splitted[0].equals("@roll")) {
            final String invalidSyntax =
                "Invalid syntax. Use: @roll <dice> [dice...], " +
                    "where `dice` is #d$, " +
                    "where # is the number of dice and $ is the number of faces on each die.";
            if (splitted.length < 2) {
                mc.dropMessage(invalidSyntax);
                return;
            }
            final Pattern dicePattern = Pattern.compile("(?i)[1-9][0-9]?d[1-9][0-9]{0,3}");
            final Random rand = new Random();

            int total = 0;
            final StringBuilder msg = new StringBuilder();
            msg.append(player.getName()).append(" rolled ");

            for (int i = 1; i < splitted.length; ++i) {
                if (!dicePattern.matcher(splitted[i]).matches()) {
                    mc.dropMessage(invalidSyntax);
                    return;
                }
                final String[] nfSplit = splitted[i].split("(?i)d");
                final int n = Integer.parseInt(nfSplit[0]),
                          f = Integer.parseInt(nfSplit[1]);
                if (i > 1) msg.append(" + ");
                msg.append(n).append('d').append(f);
                total += IntStream.range(0, n).map(j -> 1 + rand.nextInt(f)).sum();
            }
            msg.append(", for a total of ").append(total).append('.');
            player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, msg.toString()));
        } else if (splitted[0].equals("@engage")) {
            if (splitted.length != 2) {
                mc.dropMessage("Invalid syntax. Use: @engage <partner_name>");
                return;
            }
            final boolean hasUseItem =
                IntStream
                    .rangeClosed(2240000, 2240003)
                    .anyMatch(id -> player.getItemQuantity(id, false) > 0);
            if (!hasUseItem) {
                mc.dropMessage("You need an engagement ring from Moody to get engaged.");
                return;
            }
            final String partnerName = splitted[1];
            final MapleCharacter partner = c.getChannelServer().getPlayerStorage().getCharacterByName(partnerName);
            if (partnerName.equalsIgnoreCase(player.getName())) {
                mc.dropMessage("You can't get engaged with yourself.");
            } else if (partner == null) {
                mc.dropMessage(
                    partnerName +
                        " was not found on this channel. " +
                        "If you are both logged in, please make sure you are in the same channel, " +
                        "and that you spelled your partner's name correctly."
                );
            } else if (!player.isMarried() && !partner.isMarried()) {
                NPCScriptManager.getInstance().start(partner.getClient(), 9201002, "marriagequestion", player);
            } else {
                mc.dropMessage("It looks like you or your partner are already married!");
            }
        } else if (splitted[0].equals("@whoquestdrops")) {
            try {
                final StringBuilder searchString = new StringBuilder();
                for (int i = 1; i < splitted.length; ++i) {
                    if (i == 1) {
                        searchString.append(splitted[i]);
                    } else {
                        searchString.append(' ').append(splitted[i]);
                    }
                }
                if (searchString.length() == 0) {
                    mc.dropMessage("Invalid syntax. Use: @whoquestdrops <search_string>");
                    return;
                }
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final String searchString_ = searchString.toString();
                final Pair<Integer, String> consumecandidate = ii.getConsumeByName(searchString_);
                final Pair<Integer, String> eqpcandidate = ii.getEqpByName(searchString_);
                final Pair<Integer, String> etccandidate = ii.getEtcByName(searchString_);
                Pair<Integer, String> candidate = consumecandidate;
                if (etccandidate != null && (candidate == null || etccandidate.getRight().length() < candidate.getRight().length())) {
                    candidate = etccandidate;
                }
                if (eqpcandidate != null && (candidate == null || eqpcandidate.getRight().length() < candidate.getRight().length())) {
                    candidate = eqpcandidate;
                }

                try {
                    final int searchid;
                    if (candidate != null) {
                        searchid = candidate.getLeft();
                    } else {
                        mc.dropMessage("No item could be found with the search string provided.");
                        return;
                    }
                    final List<Pair<String, Integer>> retMobs = new ArrayList<>();
                    mc.dropMessage(candidate.getRight() + " is dropped as a quest drop by the following mobs:");
                    final Connection con = DatabaseConnection.getConnection();
                    final PreparedStatement ps =
                        con.prepareStatement(
                            "SELECT monsterid, questid FROM monsterquestdrops WHERE itemid = ?"
                        );
                    ps.setInt(1, searchid);
                    final ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        final int mobId = rs.getInt("monsterid");
                        final int questId = rs.getInt("questid");
                        final MapleMonster mob = MapleLifeFactory.getMonster(mobId);
                        if (mob != null) {
                            retMobs.add(new Pair<>(mob.getName(), questId));
                        }
                    }
                    rs.close();
                    ps.close();
                    if (!retMobs.isEmpty()) {
                        for (final Pair<String, Integer> singleRetMob : retMobs) {
                            final MapleQuest theQuest = MapleQuest.getInstance(singleRetMob.getRight());
                            if (theQuest == null) continue;
                            mc.dropMessage(
                                singleRetMob.getLeft() +
                                    ", quest: " +
                                    theQuest.getName()
                            );
                        }
                    } else {
                        mc.dropMessage("No mobs drop this item as a quest drop.");
                    }
                } catch (final SQLException sqle) {
                    System.err.print("@whoquestdrops failed: " + sqle);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else if (splitted[0].equals("@questeffectivelevel")) {
            if (player.getQuestEffectiveLevel() < 1) {
                mc.dropMessage("You don't currently have a quest effective level.");
                return;
            }
            final DecimalFormat df = new DecimalFormat("#0.000");
            mc.dropMessage("Your current quest effective level: " + player.getQuestEffectiveLevel());
            mc.dropMessage(
                "    Outgoing damage multiplier: " +
                    df.format(player.getQuestEffectiveLevelDmgMulti())
            );
            mc.dropMessage(
                "    Incoming damage multiplier: " +
                    df.format(
                        1.0f + (float) (player.getLevel() - player.getQuestEffectiveLevel()) / 16.0f
                    )
            );
        } else if (splitted[0].equals("@music")) {
            final SoundInformationProvider sip = SoundInformationProvider.getInstance();
            switch (splitted.length) {
                case 1:
                    mc.dropMessage(
                        "There are " +
                            sip.getAllBgmNames().size() +
                            " sections of music to choose from."
                    );
                    mc.dropMessage("Use @music <section_number> to list the pieces in that section,");
                    mc.dropMessage("or @music <section_number> <title> to play a specific piece.");
                    return;
                case 2: {
                    final int sectionNumber;
                    try {
                        sectionNumber = Integer.parseInt(splitted[1]);
                    } catch (final NumberFormatException nfe) {
                        mc.dropMessage(
                            "Invalid syntax. Use: @music | @music <section_number> | @music <section_number> <title>"
                        );
                        return;
                    }
                    if (sectionNumber < 1 || sectionNumber > sip.getAllBgmNames().size()) {
                        mc.dropMessage("There is no such section with that number.");
                        return;
                    }
                    String section = null;
                    int i = 1;
                    for (final String section_ : sip.getAllBgmNames().keySet()) {
                        if (i > sectionNumber) break;
                        section = section_;
                        i++;
                    }
                    if (section == null) {
                        mc.dropMessage("There is no such section with that number.");
                        return;
                    }
                    mc.dropMessage("Pieces in section " + sectionNumber + ":");
                    sip.getAllBgmNames().get(section).forEach(t -> mc.dropMessage("    " + t));
                    return;
                }
                case 3: {
                    final int sectionNumber;
                    try {
                        sectionNumber = Integer.parseInt(splitted[1]);
                    } catch (final NumberFormatException nfe) {
                        mc.dropMessage(
                            "Invalid syntax. Use: @music | @music <section_number> | @music <section_number> <title>"
                        );
                        return;
                    }
                    if (sectionNumber < 1 || sectionNumber > sip.getAllBgmNames().size()) {
                        mc.dropMessage("There is no such section with that number.");
                        return;
                    }
                    String section = null;
                    int i = 1;
                    for (final String section_ : sip.getAllBgmNames().keySet()) {
                        if (i > sectionNumber) break;
                        section = section_;
                        i++;
                    }
                    if (section == null) {
                        mc.dropMessage("There is no such section with that number.");
                        return;
                    }
                    final String title = splitted[2];
                    if (sip.getAllBgmNames().get(section).stream().anyMatch(t -> t.equals(title))) {
                        if (!player.getCheatTracker().Spam(90 * 1000, 6)) { // 90 seconds
                            player.getMap().broadcastMessage(MaplePacketCreator.musicChange(section + "/" + title));
                            player
                                .getMap()
                                .broadcastMessage(
                                    MaplePacketCreator.serverNotice(
                                        5,
                                        player.getName() +
                                            " has changed the music: " +
                                            sectionNumber +
                                            " -- " +
                                            title
                                    )
                                );
                        } else {
                            mc.dropMessage(
                                "Looks like you're trying to change up the music a bit fast there. " +
                                    "Try again in a minute or so."
                            );
                        }
                    } else {
                        mc.dropMessage(
                            "There is no such piece with the title '" +
                                title +
                                "' in section " +
                                sectionNumber +
                                "."
                        );
                    }
                    return;
                }
                default:
                    mc.dropMessage(
                        "Invalid syntax. Use: @music | @music <section_number> | @music <section_number> <title>"
                    );
            }
        } else if (splitted[0].equals("@showvuln")) {
            final long repeatTime;
            switch (splitted.length) {
                case 1:
                    repeatTime = 0L;
                    break;
                case 2:
                    try {
                        repeatTime = Integer.parseInt(splitted[1]);
                    } catch (final NumberFormatException nfe) {
                        mc.dropMessage(
                            "Could not parse repeat time for @showvuln. Make sure you are entering a valid integer."
                        );
                        return;
                    }
                    if (repeatTime < 1000L || repeatTime > 300000L) {
                        mc.dropMessage("Make sure the repeat time is between 1000 and 300000 milliseconds.");
                        return;
                    }
                    break;
                default:
                    mc.dropMessage("Invalid syntax. Use: @showvuln [repeat_time_in_milliseconds]");
                    return;
            }
            if (repeatTime > 0L) {
                player.setShowVulnTask(repeatTime, 1000L * 60L * 60L);
            } else {
                if (player.cancelShowVulnTask()) {
                    mc.dropMessage("@showvuln display has been stopped.");
                }
                final DecimalFormat df = new DecimalFormat("##0.0###");
                player
                    .getMap()
                    .getAllMonsters()
                    .stream()
                    .filter(mob -> mob.getVulnerability() > 1.0d)
                    .forEach(mob ->
                        player.dropMessage(
                            "Monster: " +
                                mob.getName() +
                                ", vulnerability: " +
                                df.format(mob.getVulnerability() * 100.0d) +
                                "%"
                        )
                    );
            }
        }
    }

    private void compareTime(final StringBuilder sb, final long timeDiff) {
        double secondsAway = (double) (timeDiff / 1000L);
        double minutesAway = 0.0d;
        double hoursAway = 0.0d;

        while (secondsAway > 60.0d) {
            minutesAway++;
            secondsAway -= 60.0d;
        }
        while (minutesAway > 60.0d) {
            hoursAway++;
            minutesAway -= 60.0d;
        }
        boolean hours = false;
        boolean minutes = false;
        if (hoursAway > 0.0d) {
            sb.append(' ');
            sb.append((int) hoursAway);
            sb.append(" hours");
            hours = true;
        }
        if (minutesAway > 0.0d) {
            if (hours) sb.append(" -");
            sb.append(' ');
            sb.append((int) minutesAway);
            sb.append(" minutes");
            minutes = true;
        }
        if (secondsAway > 0.0d) {
            if (minutes) sb.append(" and");
            sb.append(' ');
            sb.append((int) secondsAway);
            sb.append(" seconds.");
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
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
            //new CommandDefinition("readingtime", 0),
            new CommandDefinition("defense", 0),
            new CommandDefinition("defence", 0),
            new CommandDefinition("ria", 0),
            new CommandDefinition("pqpoints", 0),
            new CommandDefinition("overflowexp", 0),
            new CommandDefinition("vskills", 0),
            new CommandDefinition("voteupdate", 0),
            new CommandDefinition("buyback", 0),
            new CommandDefinition("snipedisplay", 0),
            new CommandDefinition("event", 0),
            new CommandDefinition("magic", 0),
            new CommandDefinition("samsara", 0),
            new CommandDefinition("dailyprize", 0),
            new CommandDefinition("roll", 0),
            new CommandDefinition("engage", 0),
            new CommandDefinition("whoquestdrops", 0),
            new CommandDefinition("questeffectivelevel", 0),
            new CommandDefinition("music", 0),
            new CommandDefinition("showvuln", 0)
        };
    }
}
