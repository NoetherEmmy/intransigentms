package net.sf.odinms.client.messages.commands;

import net.sf.odinms.client.*;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.PartyQuest;
import net.sf.odinms.net.channel.handler.ChangeChannelHandler;
import net.sf.odinms.net.world.remote.CheaterData;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.net.world.remote.WorldLocation;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.*;
import net.sf.odinms.server.life.*;
import net.sf.odinms.server.maps.*;
import net.sf.odinms.tools.DeathLogReader;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.StringUtil;

import java.awt.*;
import java.io.File;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static net.sf.odinms.client.messages.CommandProcessor.*;

public class GM implements Command {
    private static String getBannedReason(String name) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            ResultSet rs;
            ps = con.prepareStatement("SELECT name, banned, banreason, macs FROM accounts WHERE name = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("banned") > 0) {
                    String user, reason, mac;
                    user = rs.getString("name");
                    reason = rs.getString("banreason");
                    mac = rs.getString("macs");
                    rs.close();
                    ps.close();
                    return "Username: " + user + " | BanReason: " + reason + " | Macs: " + mac;
                } else {
                    rs.close();
                    ps.close();
                    return "Player is not banned.";
                }
            }
            rs.close();
            ps.close();
            int accid;
            ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return "This character / account does not exist.";
            } else {
                accid = rs.getInt("accountid");
            }
            ps = con.prepareStatement("SELECT name, banned, banreason, macs FROM accounts WHERE id = ?");
            ps.setInt(1, accid);
            rs = ps.executeQuery();
            if (rs.getInt("banned") > 0) {
                String user, reason, mac;
                user = rs.getString("name");
                reason = rs.getString("banreason");
                mac = rs.getString("macs");
                rs.close();
                ps.close();
                return "Username: " + user + " | BanReason: " + reason + " | Macs: " + mac;
            } else {
                rs.close();
                ps.close();
                return "Player is not banned.";
            }
        } catch (SQLException ignored) {
        }
        return "Player is not banned.";
    }

    public void clearSlot(MapleClient c, int type) {
        MapleInventoryType invent;
        switch (type) {
            case 1:
                invent = MapleInventoryType.EQUIP;
                break;
            case 2:
                invent = MapleInventoryType.USE;
                break;
            case 3:
                invent = MapleInventoryType.ETC;
                break;
            case 4:
                invent = MapleInventoryType.SETUP;
                break;
            default:
                invent = MapleInventoryType.CASH;
                break;
        }
        List<Integer> itemMap = new ArrayList<>();
        for (IItem item : c.getPlayer().getInventory(invent).list()) {
            itemMap.add(item.getItemId());
        }
        for (int itemid : itemMap) {
            MapleInventoryManipulator.removeAllById(c, itemid, false);
        }
    }

    @Override
    public void execute(MapleClient c, final MessageCallback mc, String[] splitted) throws Exception {
        splitted[0] = splitted[0].toLowerCase();
        final ChannelServer cserv = c.getChannelServer();
        final Collection<ChannelServer> cservs = ChannelServer.getAllInstances();
        final MapleCharacter player = c.getPlayer();
        switch (splitted[0]) {
            case "!lowhp":
                player.setHp(1);
                player.updateSingleStat(MapleStat.HP, 1);
                break;
            case "!sp":
                if (splitted.length != 2) {
                    return;
                }
                int sp;
                try {
                    sp = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException asd) {
                    return;
                }
                player.setRemainingSp(sp + player.getRemainingSp());
                player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
                break;
            case "!ap":
                if (splitted.length != 2) {
                    return;
                }
                int ap;
                try {
                    ap = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException asd) {
                    return;
                }
                player.setRemainingAp(ap + player.getRemainingAp());
                player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                break;
            case "!job": {
                if (splitted.length != 2) {
                    return;
                }
                int job;
                try {
                    job = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException asd) {
                    return;
                }
                player.setJob(job);
                break;
            }
            case "!whereami":
                mc.dropMessage("You are on map " + player.getMap().getId());
                break;
            case "!shop":
                if (splitted.length != 2) {
                    return;
                }
                int shopid;
                try {
                    shopid = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException asd) {
                    return;
                }
                MapleShopFactory.getInstance().getShop(shopid).sendShop(c);
                break;
            case "!opennpc": {
                if (splitted.length != 2) {
                    return;
                }
                int npcid;
                try {
                    npcid = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException ignored) {
                    return;
                }
                MapleNPC npc = MapleLifeFactory.getNPC(npcid);
                if (!npc.getName().equalsIgnoreCase("MISSINGNO")) {
                    NPCScriptManager.getInstance().start(c, npcid);
                } else {
                    mc.dropMessage("UNKNOWN NPC");
                }
                break;
            }
            case "!levelup":
                player.levelUp();
                player.setExp(0);
                player.updateSingleStat(MapleStat.EXP, 0);
                break;
            case "!setmaxmp": {
                if (splitted.length != 2) {
                    return;
                }
                int amt;
                try {
                    amt = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException asd) {
                    return;
                }
                player.setMaxMp(amt);
                player.updateSingleStat(MapleStat.MAXMP, player.getMaxMp());
                break;
            }
            case "!setmaxhp": {
                if (splitted.length != 2) {
                    return;
                }
                int amt;
                try {
                    amt = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException asd) {
                    return;
                }
                player.setMaxHp(amt);
                player.updateSingleStat(MapleStat.MAXHP, player.getMaxHp());
                break;
            }
            case "!healmap":
                for (MapleCharacter map : player.getMap().getCharacters()) {
                    if (map != null) {
                        map.setHp(map.getCurrentMaxHp());
                        map.updateSingleStat(MapleStat.HP, map.getHp());
                        map.setMp(map.getCurrentMaxMp());
                        map.updateSingleStat(MapleStat.MP, map.getMp());
                    }
                }
                break;
            case "!item": {
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (splitted.length < 2) {
                    return;
                }
                int item;
                short quantity = (short) getOptionalIntArg(splitted, 2, 1);
                try {
                    item = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException e) {
                    mc.dropMessage("Error while making item.");
                    return;
                }
                if (item >= 5000000 && item <= 5000100) {
                    if (quantity > 1) {
                        quantity = 1;
                    }
                    int petId = MaplePet.createPet(item);
                    MapleInventoryManipulator.addById(c, item, quantity, player.getName(), petId);
                } else if (ii.getInventoryType(item).equals(MapleInventoryType.EQUIP) && !ii.isThrowingStar(ii.getEquipById(item).getItemId()) && !ii.isBullet(ii.getEquipById(item).getItemId())) {
                    MapleInventoryManipulator.addFromDrop(c, ii.randomizeStats(c, (Equip) ii.getEquipById(item)), true, player.getName());
                } else {
                    MapleInventoryManipulator.addById(c, item, quantity);
                }
                break;
            }
            case "!noname": {
                if (splitted.length < 2) {
                    return;
                }
                int quantity = getOptionalIntArg(splitted, 2, 1);
                int item;
                try {
                    item = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException asd) {
                    return;
                }
                MapleInventoryManipulator.addById(c, item, (short) quantity);
                break;
            }
            case "!dropmesos": {
                if (splitted.length < 2) {
                    return;
                }
                int amt;
                try {
                    amt = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException asd) {
                    return;
                }
                player.getMap().spawnMesoDrop(amt, amt, player.getPosition(), player, player, false);
                break;
            }
            case "!level": {
                if (splitted.length != 2) {
                    return;
                }
                int level;
                try {
                    level = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException asd) {
                    return;
                }
                player.setLevel(level);
                player.levelUp();
                player.setExp(0);
                player.updateSingleStat(MapleStat.EXP, 0);
                break;
            }
            case "!allonline": {
                int i = 0;
                for (ChannelServer cs : ChannelServer.getAllInstances()) {
                    if (!cs.getPlayerStorage().getAllCharacters().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        mc.dropMessage("Channel " + cs.getChannel());
                        for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {
                            i++;
                            if (sb.length() > 150) { // Chars per line. Could be more or less
                                mc.dropMessage(sb.toString());
                                sb = new StringBuilder();
                            }
                            sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName() + "   "));
                        }
                        mc.dropMessage(sb.toString());
                    }
                }
                break;
            }
            case "!banreason":
                if (splitted.length != 2) {
                    return;
                }
                mc.dropMessage(getBannedReason(splitted[1]));
                break;
            case "!joinguild": {
                if (splitted.length != 2) {
                    return;
                }
                Connection con = DatabaseConnection.getConnection();
                try {
                    PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
                    ps.setString(1, splitted[1]);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        if (player.getGuildId() > 0) {
                            try {
                                cserv.getWorldInterface().leaveGuild(player.getMGC());
                            } catch (RemoteException re) {
                                c.getSession().write(MaplePacketCreator.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                                return;
                            }
                            c.getSession().write(MaplePacketCreator.showGuildInfo(null));

                            player.setGuildId(0);
                            player.saveGuildStatus();
                        }
                        player.setGuildId(rs.getInt("guildid"));
                        player.setGuildRank(2); // Jr. Master
                        try {
                            cserv.getWorldInterface().addGuildMember(player.getMGC());
                        } catch (RemoteException e) {
                            cserv.reconnectWorld();
                        }
                        c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                        player.getMap().broadcastMessage(player, MaplePacketCreator.removePlayerFromMap(player.getId()), false);
                        player.getMap().broadcastMessage(player, MaplePacketCreator.spawnPlayerMapobject(player), false);
                        if (player.getNoPets() > 0) {
                            for (MaplePet pet : player.getPets()) {
                                player.getMap().broadcastMessage(player, MaplePacketCreator.showPet(player, pet, false, false), false);
                            }
                        }
                        player.saveGuildStatus();
                    } else {
                        mc.dropMessage("Guild name does not exist.");
                    }
                    rs.close();
                    ps.close();
                } catch (SQLException ignored) {
                }
                break;
            }
            case "!unbuffmap":
                for (MapleCharacter map : player.getMap().getCharacters()) {
                    if (map != null && map != player) {
                        map.cancelAllBuffs();
                    }
                }
                break;
            case "!mesos":
                if (splitted.length != 2) {
                    return;
                }
                int meso;
                try {
                    meso = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException ex) {
                    return;
                }
                player.setMeso(meso);
                break;
            case "!setname": {
                if (splitted.length == 3) {
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    String newname = splitted[2];

                    if (MapleCharacter.getIdByName(newname, 0) == -1) {
                        if (victim != null) {
                            victim.getClient().disconnect();
                            victim.getClient().getSession().close();
                            victim.setName(newname, true);
                            mc.dropMessage(splitted[1] + " is now named " + newname + "");
                        } else {
                            mc.dropMessage("The player " + splitted[1] + " is either offline or not in this channel.");
                        }
                    } else {
                        mc.dropMessage("Character name is in use.");
                    }
                } else {
                    mc.dropMessage("Incorrect syntax!");
                }
                break;
            }
            case "!clearslot":
                if (splitted.length == 2) {
                    if (splitted[1].equalsIgnoreCase("all")) {
                        clearSlot(c, 1);
                        clearSlot(c, 2);
                        clearSlot(c, 3);
                        clearSlot(c, 4);
                        clearSlot(c, 5);
                    } else if (splitted[1].equalsIgnoreCase("equip")) {
                        clearSlot(c, 1);
                    } else if (splitted[1].equalsIgnoreCase("use")) {
                        clearSlot(c, 2);
                    } else if (splitted[1].equalsIgnoreCase("etc")) {
                        clearSlot(c, 3);
                    } else if (splitted[1].equalsIgnoreCase("setup")) {
                        clearSlot(c, 4);
                    } else if (splitted[1].equalsIgnoreCase("cash")) {
                        clearSlot(c, 5);
                    } else {
                        mc.dropMessage("!clearslot " + splitted[1] + " does not exist!");
                    }
                }
                break;
            case "!ariantpq":
                if (splitted.length < 2) {
                    player.getMap().AriantPQStart();
                } else {
                    c.getSession().write(MaplePacketCreator.updateAriantPQRanking(splitted[1], 5, false));
                }
                break;
            case "!scoreboard":
                player.getMap().broadcastMessage(MaplePacketCreator.showAriantScoreBoard());
                break;
            case "!array":
                if (splitted.length >= 2) {
                    if (splitted[1].equalsIgnoreCase("*CLEAR")) {
                        cserv.setArrayString("");
                        mc.dropMessage("Array flushed.");
                    } else {
                        cserv.setArrayString(cserv.getArrayString() + StringUtil.joinStringFrom(splitted, 1));
                        mc.dropMessage("Added " + StringUtil.joinStringFrom(splitted, 1) + " to the array. Use !array to check.");
                    }
                } else {
                    mc.dropMessage("Array: " + cserv.getArrayString());
                }
                break;
            case "!slap": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                int damage;
                try {
                    damage = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException ex) {
                    return;
                }
                if (victim.getHp() > damage) {
                    victim.setHp(victim.getHp() - damage);
                    victim.updateSingleStat(MapleStat.HP, victim.getHp());
                    victim.dropMessage(5, player.getName() + " picked up a big fish and slapped you across the head. You've lost " + damage + " HP.");
                    mc.dropMessage(victim.getName() + " has " + victim.getHp() + " HP left");
                } else {
                    victim.setHp(0);
                    victim.updateSingleStat(MapleStat.HP, 0);
                    victim.dropMessage(5, player.getName() + " headshot you with a fish.");
                }
                break;
            }
            case "!rreactor":
                player.getMap().resetReactors();
                break;
            case "!coke":
                int[] coke = {9500144, 9500151, 9500152, 9500153, 9500154, 9500143, 9500145, 9500149, 9500147};
                for (int i = 0; i < coke.length; ++i) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(coke[i]), player.getPosition());
                }
                break;
            case "!papu":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8500001), player.getPosition());
                }
                break;
            case "!zakum":
                for (int m = 8800003; m <= 8800010; ++m) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(m), player.getPosition());
                }
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8800000), player.getPosition());
                player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(0, "The almighty Zakum has awakened!"));
                break;
            case "!ergoth":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300028), player.getPosition());
                }
                break;
            case "!ludimini":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8160000), player.getPosition());
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8170000), player.getPosition());
                }
                break;
            case "!cornian":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8150201), player.getPosition());
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8150200), player.getPosition());
                }
                break;
            case "!balrog":
                int[] balrog = {8130100, 8150000, 9400536};
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    for (int i = 0; i < balrog.length; ++i) {
                        player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(balrog[i]), player.getPosition());
                    }
                }
                break;
            case "!mushmom":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    int[] mushmom = {6130101, 6300005, 9400205};
                    for (int i = 0; i < mushmom.length; ++i) {
                        player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(mushmom[i]), player.getPosition());
                    }
                }
                break;
            case "!wyvern":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    for (int i = 8150300; i <= 8150302; ++i) {
                        player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(i), player.getPosition());
                    }
                }
                break;
            case "!pirate":
                int[] pirate = {9300119, 9300107, 9300105, 9300106};
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    for (int i = 0; i < pirate.length; ++i) {
                        player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(pirate[i]), player.getPosition());
                    }
                }
                break;
            case "!clone":
                int[] clone = {9001002, 9001003, 9001000, 9001001};
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    for (int i = 0; i < clone.length; ++i) {
                        player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(clone[i]), player.getPosition());
                    }
                }
                break;
            case "!anego":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400121), player.getPosition());
                }
                break;
            case "!theboss":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400300), player.getPosition());
                }
                break;
            case "!snackbar":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9500179), player.getPosition());
                }
                break;
            case "!papapixie":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300039), player.getPosition());
                }
                break;
            case "!nxslimes":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400202), player.getPosition());
                }
                break;
            case "!horseman":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400549), player.getPosition());
                }
                break;
            case "!blackcrow":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400014), player.getPosition());
                }
                break;
            case "!leafreboss":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400014), player.getPosition());
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8180001), player.getPosition());
                }
                break;
            case "!shark":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8150101), player.getPosition());
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8150100), player.getPosition());
                }
                break;
            case "!franken":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300139), player.getPosition());
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300140), player.getPosition());
                }
                break;
            case "!bird":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300090), player.getPosition());
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300089), player.getPosition());
                }
                break;
            case "!pianus":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8510000), player.getPosition());
                }
                break;
            case "!centipede":
                for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9500177), player.getPosition());
                }
                break;
            case "!horntail":
                MapleMonster ht = MapleLifeFactory.getMonster(8810026);
                player.getMap().spawnMonsterOnGroudBelow(ht, player.getPosition());
                player.getMap().killMonster(ht, player, false);
                player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(0, "As the cave shakes and rattles, here comes Horntail."));
                break;
            case "!killall": {
                String mapMessage = "";
                MapleMap map = player.getMap();
                double range = Double.POSITIVE_INFINITY;
                if (splitted.length > 1) {
                    int irange = Integer.parseInt(splitted[1]);
                    if (splitted.length == 2) {
                        range = irange * irange;
                    } else {
                        map = cserv.getMapFactory().getMap(Integer.parseInt(splitted[2]));
                        mapMessage = " in " + map.getStreetName() + " : " + map.getMapName();
                    }
                }
                List<MapleMapObject> monsters =
                    map.getMapObjectsInRange(
                        player.getPosition(),
                        range,
                        MapleMapObjectType.MONSTER
                    );
                for (final MapleMapObject monstermo : monsters) {
                    MapleMonster monster = (MapleMonster) monstermo;
                    map.killMonster(monster, player, false);
                }
                mc.dropMessage("Killed " + monsters.size() + " monsters" + mapMessage + ".");
                break;
            }
            case "!help":
                int page = CommandProcessor.getOptionalIntArg(splitted, 1, 1);
                CommandProcessor.getInstance().dropHelp(c.getPlayer(), mc, page);
                break;
            case "!say":
                if (splitted.length > 1) {
                    try {
                        cserv.getWorldInterface().broadcastMessage(player.getName(), MaplePacketCreator.serverNotice(6, "[" + player.getName() + "] " + StringUtil.joinStringFrom(splitted, 1)).getBytes());
                    } catch (RemoteException re) {
                        cserv.reconnectWorld();
                    }
                } else {
                    mc.dropMessage("Syntax: !say <message>");
                }
                break;
            case "!gender": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.setGender(victim.getGender() == 1 ? 0 : 1);
                    victim.getClient().getSession().write(MaplePacketCreator.getCharInfo(victim));
                    victim.getMap().removePlayer(victim);
                    victim.getMap().addPlayer(victim);
                } else {
                    mc.dropMessage("Player is not on.");
                }
                break;
            }
            case "!spy": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    mc.dropMessage("Players stats are:");
                    mc.dropMessage("Level: " + victim.getLevel() + "  ||  Rebirthed: " + victim.getReborns());
                    mc.dropMessage("Fame: " + victim.getFame());
                    mc.dropMessage("Str: " + victim.getStr() + "  ||  Dex: " + victim.getDex() + "  ||  Int: " + victim.getInt() + "  ||  Luk: " + victim.getLuk());
                    mc.dropMessage("Player has " + victim.getMeso() + " mesos.");
                    mc.dropMessage("Hp: " + victim.getHp() + "/" + victim.getCurrentMaxHp() + "  ||  Mp: " + victim.getMp() + "/" + victim.getCurrentMaxMp());
                    mc.dropMessage("NX Cash: " + victim.getCSPoints(0));
                } else {
                    mc.dropMessage("Player not found.");
                }
                break;
            }
            case "!levelperson": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                victim.setLevel(getOptionalIntArg(splitted, 2, victim.getLevel() + 1));
                victim.levelUp();
                victim.setExp(0);
                victim.updateSingleStat(MapleStat.EXP, 0);
                break;
            }
            case "!skill": {
                int skill;
                try {
                    skill = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                int maxlevel = SkillFactory.getSkill(skill).getMaxLevel();
                int level = getOptionalIntArg(splitted, 2, maxlevel);
                int masterlevel = getOptionalIntArg(splitted, 3, maxlevel);
                if (splitted.length == 4) {
                    player.changeSkillLevel(SkillFactory.getSkill(skill), level, masterlevel);
                } else if (splitted.length == 5) {
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[4]);
                    if (victim != null) {
                        victim.changeSkillLevel(SkillFactory.getSkill(skill), level, masterlevel);
                    } else {
                        mc.dropMessage("Victim was not found.");
                    }
                }
                break;
            }
            case "!setall":
                int max;
                try {
                    max = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                player.setStr(max);
                player.setDex(max);
                player.setInt(max);
                player.setLuk(max);
                player.updateSingleStat(MapleStat.STR, max);
                player.updateSingleStat(MapleStat.DEX, max);
                player.updateSingleStat(MapleStat.INT, max);
                player.updateSingleStat(MapleStat.LUK, max);
                break;
            case "!giftnx": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    int amount;
                    try {
                        amount = Integer.parseInt(splitted[2]);
                    } catch (NumberFormatException ex) {
                        return;
                    }
                    int type = getOptionalIntArg(splitted, 3, 1);
                    victim.modifyCSPoints(type, amount);
                    victim.dropMessage(5, player.getName() + " has gifted you " + amount + " NX points.");
                    mc.dropMessage("NX recieved.");
                } else {
                    mc.dropMessage("Player not found.");
                }
                break;
            }
            case "!maxskills":
                player.maxAllSkills();
                break;
            case "!fame": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.setFame(getOptionalIntArg(splitted, 2, 1));
                    victim.updateSingleStat(MapleStat.FAME, victim.getFame());
                } else {
                    mc.dropMessage("Player not found");
                }
                break;
            }
            case "!unhide": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.dispelSkill(9101004);
                } else {
                    mc.dropMessage("Player not found");
                }
                break;
            }
            case "!heal":
                MapleCharacter heal;
                if (splitted.length == 2) {
                    heal = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    if (heal == null) {
                        mc.dropMessage("Player was not found");
                        return;
                    }
                } else {
                    heal = player;
                }
                heal.setHp(heal.getCurrentMaxHp());
                heal.setMp(heal.getCurrentMaxMp());
                heal.updateSingleStat(MapleStat.HP, heal.getCurrentMaxHp());
                heal.updateSingleStat(MapleStat.MP, heal.getCurrentMaxMp());
                break;
            case "!unbuff": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.cancelAllBuffs();
                } else {
                    mc.dropMessage("Player not found");
                }
                break;
            }
            case "!sendhint": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    String message = StringUtil.joinStringFrom(splitted, 2);
                    victim.getMap().broadcastMessage(victim, MaplePacketCreator.sendHint(message, 0, 0), false);
                } else {
                    mc.dropMessage("Player not found");
                }
                break;
            }
            case "!smega":
                if (splitted.length > 3) {
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim != null) {
                        String type = splitted[2];
                        String text = StringUtil.joinStringFrom(splitted, 3);
                        int itemID = 5390001; // Default is cloud
                        if (type.equalsIgnoreCase("love")) {
                            itemID = 5390002;
                        } else if (type.equalsIgnoreCase("cloud")) {
                            itemID = 5390001;
                        } else if (type.equalsIgnoreCase("diablo")) {
                            itemID = 5390000;
                        }
                        String[] lines = {"", "", "", ""};

                        if (text.length() > 30) {
                            lines[0] = text.substring(0, 10);
                            lines[1] = text.substring(10, 20);
                            lines[2] = text.substring(20, 30);
                            lines[3] = text.substring(30);
                        } else if (text.length() > 20) {
                            lines[0] = text.substring(0, 10);
                            lines[1] = text.substring(10, 20);
                            lines[2] = text.substring(20);
                        } else if (text.length() > 10) {
                            lines[0] = text.substring(0, 10);
                            lines[1] = text.substring(10);
                        } else if (text.length() <= 10) {
                            lines[0] = text;
                        }
                        List<String> list = new ArrayList<>();
                        list.add(lines[0]);
                        list.add(lines[1]);
                        list.add(lines[2]);
                        list.add(lines[3]);

                        try {
                            victim.getClient().getChannelServer().getWorldInterface().broadcastSMega(null, MaplePacketCreator.getAvatarMega(victim, c.getChannel(), itemID, list, false).getBytes());
                        } catch (RemoteException e) {
                            cserv.reconnectWorld();
                        }
                    } else {
                        mc.dropMessage("Player not found.");
                    }
                } else {
                    mc.dropMessage("Syntax: !smega <player> <love/diablo/cloud> text");
                }
                break;
            case "!mutesmega": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.setCanSmega(!victim.getCanSmega());
                    victim.dropMessage(5, "Your smega ability is now " + (victim.getCanSmega() ? "on" : "off"));
                    player.dropMessage(6, "Player's smega ability is now set to " + victim.getCanSmega());
                } else {
                    mc.dropMessage("Player not found");
                }
                break;
            }
            case "!mute": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.canTalk(!victim.getCanTalk());
                    victim.dropMessage(5, "Your chatting ability is now " + (victim.getCanTalk() ? "on" : "off"));
                    player.dropMessage(6, "Player's chatting ability is now set to " + victim.getCanTalk());
                } else {
                    mc.dropMessage("Player not found");
                }
                break;
            }
            case "!givedisease": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                int type;
                if (splitted[2].equalsIgnoreCase("SEAL")) {
                    type = 120;
                } else if (splitted[2].equalsIgnoreCase("DARKNESS")) {
                    type = 121;
                } else if (splitted[2].equalsIgnoreCase("WEAKEN")) {
                    type = 122;
                } else if (splitted[2].equalsIgnoreCase("STUN")) {
                    type = 123;
                } else if (splitted[2].equalsIgnoreCase("POISON")) {
                    type = 125;
                } else if (splitted[2].equalsIgnoreCase("SEDUCE")) {
                    type = 128;
                } else {
                    mc.dropMessage("ERROR.");
                    return;
                }
                victim.giveDebuff(MapleDisease.getType(type), MobSkillFactory.getMobSkill(type, 1));
                break;
            }
            case "!dc": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                victim.getClient().disconnect();
                victim.getClient().getSession().close();
                break;
            }
            case "!charinfo": {
                StringBuilder builder = new StringBuilder();
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null) {
                    return;
                }
                builder.append(MapleClient.getLogMessage(victim, "")); // Could use null, I think?

                mc.dropMessage(builder.toString());

                builder = new StringBuilder();
                builder.append("Positions: X: ");
                builder.append(victim.getPosition().x);
                builder.append(" Y: ");
                builder.append(victim.getPosition().y);
                builder.append(" | RX0: ");
                builder.append(victim.getPosition().x + 50);
                builder.append(" | RX1: ");
                builder.append(victim.getPosition().x - 50);
                builder.append(" | FH: ");
                builder.append(victim.getMap().getFootholds().findBelow(player.getPosition()).getId());
                mc.dropMessage(builder.toString());

                builder = new StringBuilder();
                builder.append("HP: ");
                builder.append(victim.getHp());
                builder.append('/');
                builder.append(victim.getCurrentMaxHp());
                builder.append(" | MP: ");
                builder.append(victim.getMp());
                builder.append('/');
                builder.append(victim.getCurrentMaxMp());
                builder.append(" | EXP: ");
                builder.append(victim.getExp());
                builder.append(" | In a Party: ");
                builder.append(victim.getParty() != null);
                builder.append(" | In a Trade: ");
                builder.append(victim.getTrade() != null);
                mc.dropMessage(builder.toString());

                builder = new StringBuilder();
                builder.append("Remote Address: ");
                builder.append(victim.getClient().getSession().getRemoteAddress());
                mc.dropMessage(builder.toString());
                victim.getClient().dropDebugMessage(mc);
                break;
            }
            case "!connected":
                try {
                    Map<Integer, Integer> connected = cserv.getWorldInterface().getConnected();
                    StringBuilder conStr = new StringBuilder();
                    mc.dropMessage("Connected Clients: ");

                    for (int i : connected.keySet()) {
                        if (i == 0) {
                            conStr.append("Total: "); // I HAVE NO CLUE WHY.
                            conStr.append(connected.get(i));
                        } else {
                            conStr.append("Channel ");
                            conStr.append(i);
                            conStr.append(": ");
                            conStr.append(connected.get(i));
                        }
                    }
                    mc.dropMessage(conStr.toString());
                } catch (RemoteException e) {
                    cserv.reconnectWorld();
                }
                break;
            case "!clock":
                player.getMap().broadcastMessage(MaplePacketCreator.getClock(getOptionalIntArg(splitted, 1, 60)));
                break;
            case "!warp": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    if (splitted.length == 2) {
                        MapleMap target = victim.getMap();
                        player.changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                    } else {
                        MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                        victim.changeMap(target, target.getPortal(0));
                    }
                } else {
                    try {
                        victim = player;
                        WorldLocation loc = cserv.getWorldInterface().getLocation(splitted[1]);
                        if (loc != null) {
                            mc.dropMessage("You will be cross-channel warped. This may take a few seconds.");
                            MapleMap target = cserv.getMapFactory().getMap(loc.map);
                            victim.cancelAllBuffs();
                            String ip = cserv.getIP(loc.channel);
                            victim.getMap().removePlayer(victim);
                            victim.setMap(target);
                            String[] socket = ip.split(":");
                            if (victim.getTrade() != null) {
                                MapleTrade.cancelTrade(player);
                            }
                            victim.saveToDB(true, true);
                            if (victim.getCheatTracker() != null) {
                                victim.getCheatTracker().dispose();
                            }
                            ChannelServer.getInstance(c.getChannel()).removePlayer(player);
                            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                            try {
                                c.getSession().write(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            MapleMap target = cserv.getMapFactory().getMap(Integer.parseInt(splitted[1]));
                            if (splitted.length == 3) {
                                Integer portalId;
                                try {
                                    portalId = Integer.parseInt(splitted[2]);
                                } catch (NumberFormatException nfe) {
                                    portalId = null;
                                }
                                MaplePortal to;
                                if (portalId != null) {
                                    to = target.getPortal(portalId);
                                } else {
                                    to = target.getPortal(splitted[2]);
                                }
                                if (to == null) {
                                    mc.dropMessage("Could not find the portal specified.");
                                    return;
                                }
                                player.changeMap(target, to);
                            } else {
                                player.changeMap(target, target.getPortal(0));
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                break;
            }
            case "!warphere": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                MapleMap pmap = player.getMap();
                if (victim != null) {
                    victim.changeMap(pmap, player.getPosition());
                } else {
                    try {
                        String name = splitted[1];
                        WorldChannelInterface wci = cserv.getWorldInterface();
                        int channel = wci.find(name);
                        if (channel > -1) {
                            ChannelServer pserv = ChannelServer.getInstance(channel);
                            MapleCharacter world_victim = pserv.getPlayerStorage().getCharacterByName(name);
                            if (world_victim != null) {
                                ChangeChannelHandler.changeChannel(c.getChannel(), world_victim.getClient());
                                world_victim.changeMap(pmap, player.getPosition());
                            }
                        } else {
                            mc.dropMessage("Player not online.");
                        }
                    } catch (RemoteException e) {
                        cserv.reconnectWorld();
                    }
                }
                break;
            }
            case "!jail": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.changeMap(980000404, 0);
                    mc.dropMessage(victim.getName() + " was jailed!");
                    victim.dropMessage("You've been jailed.");
                } else {
                    mc.dropMessage(splitted[1] + " not found!");
                }
                break;
            }
            case "!map":
                int mapid;
                try {
                    mapid = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                player.changeMap(mapid, getOptionalIntArg(splitted, 2, 0));
                break;
            case "!warpallhere":
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                    if (mch.getMapId() != player.getMapId()) {
                        mch.changeMap(player.getMap(), player.getPosition());
                    }
                }
                break;
            case "!warpwholeworld":
                for (ChannelServer channels : cservs) {
                    for (MapleCharacter mch : channels.getPlayerStorage().getAllCharacters()) {
                        if (mch.getClient().getChannel() != c.getChannel()) {
                            ChangeChannelHandler.changeChannel(c.getChannel(), mch.getClient());
                        }
                        if (mch.getMapId() != player.getMapId()) {
                            mch.changeMap(player.getMap(), player.getPosition());
                        }
                    }
                }
                break;
            case "!mesosrate": { // All these could be so much shorter but cbf.
                int set;
                try {
                    set = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                if (splitted.length > 2) {
                    for (ChannelServer channel : cservs) {
                        channel.setMesoRate(set);
                        channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Meso Rate has been changed to " + set + "x"));
                    }
                } else if (splitted.length == 2) {
                    cserv.setMesoRate(set);
                    cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Meso Rate has been changed to " + set + "x"));
                } else {
                    mc.dropMessage("Syntax: !mesorate <number>");
                }
                break;
            }
            case "!droprate": {
                int set;
                try {
                    set = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                if (splitted.length > 2) {
                    for (ChannelServer channel : cservs) {
                        channel.setDropRate(set);
                        channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Drop Rate has been changed to " + set + "x"));
                    }
                } else if (splitted.length == 2) {
                    cserv.setDropRate(set);
                    cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Drop Rate has been changed to " + set + "x"));
                } else {
                    mc.dropMessage("Syntax: !droprate <number>");
                }
                break;
            }
            case "!bossdroprate": {
                int set;
                try {
                    set = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                if (splitted.length > 2) {
                    for (ChannelServer channel : cservs) {
                        channel.setBossDropRate(set);
                        channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Boss Drop Rate has been changed to " + set + "x"));
                    }
                } else if (splitted.length == 2) {
                    cserv.setBossDropRate(set);
                    cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Boss Drop Rate has been changed to " + set + "x"));
                } else {
                    mc.dropMessage("Syntax: !bossdroprate <number>");
                }
                break;
            }
            case "!exprate": {
                int set;
                try {
                    set = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                if (splitted.length > 2) {
                    for (ChannelServer channel : cservs) {
                        channel.setExpRate(set);
                        channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Exp Rate has been changed to " + set + "x"));
                    }
                } else if (splitted.length == 2) {
                    cserv.setExpRate(set);
                    cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Exp Rate has been changed to " + set + "x"));
                } else {
                    mc.dropMessage("Syntax: !exprate <number>");
                }
                break;
            }
            case "!godlyitemrate": {
                int set;
                try {
                    set = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                if (splitted.length > 2) {
                    for (ChannelServer channel : cservs) {
                        channel.setGodlyItemRate((short) set);
                        channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Godly items will now drop at a " + set + "% rate."));
                    }
                } else if (splitted.length == 2) {
                    cserv.setGodlyItemRate((short) set);
                    cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Godly items will now drop at a  " + set + "% rate."));
                } else {
                    mc.dropMessage("Syntax: !godlyitemrate <number>");
                }
                break;
            }
            case "!itemstat": {
                int set;
                try {
                    set = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                if (splitted.length > 2) {
                    for (ChannelServer channel : cservs) {
                        channel.setItemMultiplier((short) set);
                        channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Item stat multiplier has been changed to " + set + "x"));
                    }
                } else if (splitted.length == 2) {
                    cserv.setItemMultiplier((short) set);
                    cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Item stat multiplier has been changed to " + set + "x"));
                } else {
                    mc.dropMessage("Syntax: !setItemMultiplier <number>");
                }
                break;
            }
            case "!togglegodlyitems":
                if (splitted.length > 1) {
                    for (ChannelServer channel : cservs) {
                        channel.setGodlyItems(!cserv.isGodlyItems());
                        if (channel.isGodlyItems()) {
                            channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Godly items will now drop at a " + channel.getGodlyItemRate() + "% rate. Items like these will be multiplied by " + channel.getItemMultiplier() + "x each rate."));
                        } else {
                            channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Godly item drops have been turned off."));
                        }
                    }
                } else {
                    cserv.setGodlyItems(!cserv.isGodlyItems());
                    if (cserv.isGodlyItems()) {
                        cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Godly items will now drop at a " + cserv.getGodlyItemRate() + "% rate. Items like these will be multiplied by " + cserv.getItemMultiplier() + "x each rate."));
                    } else {
                        cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Godly item drops have been turned off."));
                    }
                }
                break;
            case "!servermessage":
                String outputMessage = StringUtil.joinStringFrom(splitted, 1);
                if (outputMessage.equalsIgnoreCase("!array")) {
                    outputMessage = cserv.getArrayString();
                }
                cserv.setServerMessage(outputMessage);
                break;
            case "!whosthere": {
                StringBuilder builder = new StringBuilder();
                mc.dropMessage("Players on Map: ");
                for (MapleCharacter chr : player.getMap().getCharacters()) {
                    if (builder.length() > 150) { // wild guess :o
                        mc.dropMessage(builder.toString());
                        builder = new StringBuilder();
                    }
                    builder.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                    builder.append(", ");
                }
                player.dropMessage(6, builder.toString());
                break;
            }
            case "!cheaters":
                try {
                    List<CheaterData> cheaters = cserv.getWorldInterface().getCheaters();
                    for (CheaterData cheater : cheaters) {
                        mc.dropMessage(cheater.getInfo());
                    }
                } catch (RemoteException e) {
                    cserv.reconnectWorld();
                }
                break;
            case "!getrings":
                mc.dropMessage("1112800 - clover");
                mc.dropMessage("1112001 - crush");
                mc.dropMessage("1112801 - flower");
                mc.dropMessage("1112802 - Star");
                mc.dropMessage("1112803 - moonstone");
                mc.dropMessage("1112806 - Stargem");
                mc.dropMessage("1112807 - golden");
                mc.dropMessage("1112809 - silverswan");
                break;
            case "!ring":
                final Map<String, Integer> rings = new LinkedHashMap<>();
                rings.put("clover", 1112800);
                rings.put("crush", 1112001);
                rings.put("flower", 1112801);
                rings.put("star", 1112802);
                rings.put("stargem", 1112806);
                rings.put("silverswan", 1112809);
                rings.put("golden", 1112807);
                if (rings.containsKey(splitted[3])) {
                    MapleCharacter partner1 = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    MapleCharacter partner2 = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
                    int ret = MapleRing.createRing(rings.get(splitted[3]), partner1, partner2);
                    switch (ret) {
                        case -2:
                            mc.dropMessage("Partner number 1 was not found.");
                            break;
                        case -1:
                            mc.dropMessage("Partner number 2 was not found.");
                            break;
                        case 0:
                            mc.dropMessage("Error: One of the players already posesses a ring.");
                            break;
                        default:
                            mc.dropMessage("Success!");
                    }
                } else {
                    mc.dropMessage("Ring name was not found.");
                }
                rings.clear();
                break;
            case "!removering": {
                MapleCharacter victim = player;
                if (splitted.length == 2) {
                    victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                }
                if (victim != null) {
                    if (MapleRing.checkRingDB(victim)) {
                        MapleRing.removeRingFromDb(victim);
                    } else {
                        victim.dropMessage("You have no ring.");
                    }
                }
                break;
            }
            case "!nearestspawn": {
                final MaplePortal portal = player.getMap().findClosestSpawnpoint(player.getPosition());
                mc.dropMessage(portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName() + " target: " + portal.getTarget() + " targetMapId: " + portal.getTargetMapId());
                break;
            }
            case "!nearestportal": {
                final MaplePortal portal =
                    player.getMap()
                          .getPortals()
                          .stream()
                          .reduce(
                              (closest, port) -> {
                                  if (port.getPosition().distanceSq(player.getPosition()) <
                                      closest.getPosition().distanceSq(player.getPosition())) {
                                      return port;
                                  }
                                  return closest;
                              }
                          )
                          .orElse(null);
                if (portal != null) {
                    mc.dropMessage("name: " + portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName() + " target: " + portal.getTarget() + " targetMapId: " + portal.getTargetMapId());
                } else {
                    mc.dropMessage("null");
                }
                break;
            }
            case "!unban":
                if (MapleCharacter.unban(splitted[1])) {
                    mc.dropMessage("Success!");
                } else {
                    mc.dropMessage("Error while unbanning.");
                }
                break;
            case "!spawn":
                try {
                    int mid;
                    int num = getOptionalIntArg(splitted, 2, 1);
                    try {
                        mid = Integer.parseInt(splitted[1]);
                    } catch (NumberFormatException nfe) {
                        return;
                    }

                    Integer hp = getNamedIntArg(splitted, 1, "hp");
                    Integer exp = getNamedIntArg(splitted, 1, "exp");
                    Double php = getNamedDoubleArg(splitted, 1, "php");
                    Double pexp = getNamedDoubleArg(splitted, 1, "pexp");
                    MapleMonster onemob = MapleLifeFactory.getMonster(mid);
                    if (onemob == null) {
                        player.dropMessage("Could not find mob ID specified.");
                        break;
                    }
                    int newhp;
                    int newexp;
                    if (hp != null) {
                        newhp = hp;
                    } else if (php != null) {
                        newhp = (int) (onemob.getMaxHp() * (php / 100));
                    } else {
                        newhp = onemob.getMaxHp();
                    }
                    if (exp != null) {
                        newexp = exp;
                    } else if (pexp != null) {
                        newexp = (int) (onemob.getExp() * (pexp / 100));
                    } else {
                        newexp = onemob.getExp();
                    }
                    if (newhp < 1) {
                        newhp = 1;
                    }
                    MapleMonsterStats overrideStats = new MapleMonsterStats();
                    overrideStats.setHp(newhp);
                    overrideStats.setExp(newexp);
                    overrideStats.setMp(onemob.getMaxMp());
                    if (num > 20) num = 20;
                    for (int i = 0; i < num; ++i) {
                        MapleMonster mob = MapleLifeFactory.getMonster(mid);
                        if (mob == null) continue;
                        mob.setHp(newhp);
                        mob.setOverrideStats(overrideStats);
                        player.getMap().spawnMonsterOnGroudBelow(mob, player.getPosition());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "!ban": {
                String originalReason = StringUtil.joinStringFrom(splitted, 2);
                String reason = player.getName() + " banned " + splitted[1] + ": " + originalReason;
                MapleCharacter target = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (target != null) {
                    if (!target.isGM() || player.getGMLevel() > 3) {
                        String readableTargetName = MapleCharacterUtil.makeMapleReadable(target.getName());
                        String ip = target.getClient().getSession().getRemoteAddress().toString().split(":")[0];
                        reason += "  IP: " + ip;
                        target.ban(reason, false);
                        try {
                            cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(6, readableTargetName + " has been banned for " + originalReason).getBytes());
                        } catch (RemoteException re) {
                            cserv.reconnectWorld();
                        }
                    } else {
                        mc.dropMessage("Please dont ban " + cserv.getServerName() + " GMs");
                    }
                } else {
                    if (MapleCharacter.ban(splitted[1], reason, false)) {
                        String readableTargetName = MapleCharacterUtil.makeMapleReadable(splitted[1]);
                        //String ip = target.getClient().getSession().getRemoteAddress().toString().split(":")[0];
                        //reason += " (IP: " + ip + ")";
                        try {
                            cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(6, readableTargetName + " has been banned for " + originalReason).getBytes());
                        } catch (RemoteException re) {
                            cserv.reconnectWorld();
                        }
                    } else {
                        mc.dropMessage("Failed to ban " + splitted[1]);
                    }
                }
                break;
            }
            case "!tempban": {
                Calendar tempB = Calendar.getInstance();
                String originalReason = joinAfterString(splitted, ":");

                if (splitted.length < 4 || originalReason == null) {
                    mc.dropMessage("Syntax helper: !tempban <name> [i / m / w / d / h] <amount> [r [reason id] : Text Reason");
                    return;
                }

                int yChange = getNamedIntArg(splitted, 1, "y", 0);
                int mChange = getNamedIntArg(splitted, 1, "m", 0);
                int wChange = getNamedIntArg(splitted, 1, "w", 0);
                int dChange = getNamedIntArg(splitted, 1, "d", 0);
                int hChange = getNamedIntArg(splitted, 1, "h", 0);
                int iChange = getNamedIntArg(splitted, 1, "i", 0);
                int gReason = getNamedIntArg(splitted, 1, "r", 7);

                String reason = player.getName() + " tempbanned " + splitted[1] + ": " + originalReason;

                if (gReason > 14) {
                    mc.dropMessage("You have entered an incorrect ban reason ID, please try again.");
                    return;
                }

                DateFormat df = DateFormat.getInstance();
                tempB.set(tempB.get(Calendar.YEAR) + yChange, tempB.get(Calendar.MONTH) + mChange, tempB.get(Calendar.DATE) +
                        (wChange * 7) + dChange, tempB.get(Calendar.HOUR_OF_DAY) + hChange, tempB.get(Calendar.MINUTE) +
                        iChange);

                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);

                if (victim == null) {
                    int accId = MapleClient.findAccIdForCharacterName(splitted[1]);
                    if (accId >= 0 && MapleCharacter.tempban(reason, tempB, gReason, accId)) {
                        String readableTargetName = MapleCharacterUtil.makeMapleReadable(splitted[1]);
                        cserv.broadcastPacket(MaplePacketCreator.serverNotice(6, readableTargetName + " has been banned for " + originalReason));
                    } else {
                        mc.dropMessage("There was a problem offline banning character " + splitted[1] + ".");
                    }
                } else {
                    victim.tempban(reason, tempB, gReason);
                    mc.dropMessage("The character " + splitted[1] + " has been successfully tempbanned till " + df.format(tempB.getTime()));
                }
                break;
            }
            case "!search":
                if (splitted.length > 2) {
                    String type = splitted[1].toUpperCase();
                    String search = null;
                    Pattern pattern = null;
                    if (splitted[2].equals("-re")) {
                        pattern = Pattern.compile(StringUtil.joinStringFrom(splitted, 3));
                    } else {
                        search = StringUtil.joinStringFrom(splitted, 2).toLowerCase();
                    }
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    MapleData data;
                    MapleDataProvider dataProvider =
                        MapleDataProviderFactory.getDataProvider(
                            new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz")
                        );
                    mc.dropMessage("<< Type: " + type + " | Search: " + (search != null ? search : pattern) + " >>");
                    List<String> retNpcs = new ArrayList<>();
                    switch (type) {
                        case "NPC":
                        case "NPCS":
                            data = dataProvider.getData("Npc.img");
                            if (search != null) {
                                for (MapleData npcIdData : data.getChildren()) {
                                    String npcNameFromData =
                                        MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME");
                                    if (npcNameFromData.toLowerCase().contains(search)) {
                                        int npcIdFromData = Integer.parseInt(npcIdData.getName());
                                        retNpcs.add(npcIdFromData + " - " + npcNameFromData);
                                    }
                                }
                            } else {
                                for (MapleData npcIdData : data.getChildren()) {
                                    String npcNameFromData =
                                        MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME");
                                    if (pattern.matcher(npcNameFromData).matches()) {
                                        int npcIdFromData = Integer.parseInt(npcIdData.getName());
                                        retNpcs.add(npcIdFromData + " - " + npcNameFromData);
                                    }
                                }
                            }
                            if (retNpcs.isEmpty()) {
                                mc.dropMessage("No NPCs found with the provided query.");
                            } else {
                                retNpcs.forEach(mc::dropMessage);
                            }
                            break;
                        case "MAP":
                        case "MAPS":
                            data = dataProvider.getData("Map.img");
                            List<String> retMaps = new ArrayList<>();
                            if (search != null) {
                                for (MapleData mapAreaData : data.getChildren()) {
                                    for (MapleData mapIdData : mapAreaData.getChildren()) {
                                        String mapNameFromData =
                                            MapleDataTool.getString(
                                                mapIdData.getChildByPath("streetName"),
                                                "NO-NAME"
                                            ) +
                                            " - " +
                                            MapleDataTool.getString(
                                                mapIdData.getChildByPath("mapName"),
                                                "NO-NAME"
                                            );
                                        if (mapNameFromData.toLowerCase().contains(search)) {
                                            int mapIdFromData = Integer.parseInt(mapIdData.getName());
                                            retMaps.add(mapIdFromData + " - " + mapNameFromData);
                                        }
                                    }
                                }
                            } else {
                                for (MapleData mapAreaData : data.getChildren()) {
                                    for (MapleData mapIdData : mapAreaData.getChildren()) {
                                        String mapNameFromData =
                                            MapleDataTool.getString(
                                                mapIdData.getChildByPath("streetName"),
                                                "NO-NAME"
                                            ) +
                                            " - " +
                                            MapleDataTool.getString(
                                                mapIdData.getChildByPath("mapName"),
                                                "NO-NAME"
                                            );
                                        if (pattern.matcher(mapNameFromData).matches()) {
                                            int mapIdFromData = Integer.parseInt(mapIdData.getName());
                                            retMaps.add(mapIdFromData + " - " + mapNameFromData);
                                        }
                                    }
                                }
                            }
                            if (retMaps.isEmpty()) {
                                mc.dropMessage("No maps found with the provided query.");
                            } else {
                                retMaps.forEach(mc::dropMessage);
                            }
                            break;
                        case "MOB":
                        case "MOBS":
                        case "MONSTER":
                        case "MONSTERS":
                            List<String> retMobs = new ArrayList<>();
                            data = dataProvider.getData("Mob.img");
                            if (search != null) {
                                for (MapleData mobIdData : data.getChildren()) {
                                    String mobNameFromData =
                                        MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                                    if (mobNameFromData.toLowerCase().contains(search)) {
                                        int mobIdFromData = Integer.parseInt(mobIdData.getName());
                                        retMobs.add(mobIdFromData + " - " + mobNameFromData);
                                    }
                                }
                            } else {
                                for (MapleData mobIdData : data.getChildren()) {
                                    String mobNameFromData =
                                        MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                                    if (pattern.matcher(mobNameFromData).matches()) {
                                        int mobIdFromData = Integer.parseInt(mobIdData.getName());
                                        retMobs.add(mobIdFromData + " - " + mobNameFromData);
                                    }
                                }
                            }
                            if (retMobs.isEmpty()) {
                                mc.dropMessage("No mobs found with the provided query.");
                            } else {
                                retMobs.forEach(mc::dropMessage);
                            }
                            break;
                        case "REACTOR":
                        case "REACTORS":
                            mc.dropMessage("Sorry, that search type is unavailable.");
                            break;
                        case "ITEM":
                        case "ITEMS":
                            List<String> retItems = new ArrayList<>();
                            if (search != null) {
                                for (Map.Entry<Integer, String> itemEntry : ii.getAllItems().entrySet()) {
                                    if (itemEntry.getValue().toLowerCase().contains(search)) {
                                        retItems.add(itemEntry.getKey() + " - " + itemEntry.getValue());
                                    }
                                }
                            } else {
                                for (Map.Entry<Integer, String> itemEntry : ii.getAllItems().entrySet()) {
                                    if (pattern.matcher(itemEntry.getValue()).matches()) {
                                        retItems.add(itemEntry.getKey() + " - " + itemEntry.getValue());
                                    }
                                }
                            }
                            if (retItems.isEmpty()) {
                                mc.dropMessage("No items found with the provided query.");
                            } else {
                                retItems.forEach(mc::dropMessage);
                            }
                            break;
                        case "SKILL":
                        case "SKILLS":
                            List<String> retSkills = new ArrayList<>();
                            data = dataProvider.getData("Skill.img");
                            if (search != null) {
                                for (MapleData skillIdData : data.getChildren()) {
                                    String skillNameFromData = MapleDataTool.getString(skillIdData.getChildByPath("name"), "NO-NAME");
                                    if (skillNameFromData.toLowerCase().contains(search)) {
                                        int skillIdFromData = Integer.parseInt(skillIdData.getName());
                                        retSkills.add(skillIdFromData + " - " + skillNameFromData);
                                    }
                                }
                            } else {
                                for (MapleData skillIdData : data.getChildren()) {
                                    String skillNameFromData = MapleDataTool.getString(skillIdData.getChildByPath("name"), "NO-NAME");
                                    if (pattern.matcher(skillNameFromData).matches()) {
                                        int skillIdFromData = Integer.parseInt(skillIdData.getName());
                                        retSkills.add(skillIdFromData + " - " + skillNameFromData);
                                    }
                                }
                            }
                            if (retSkills.isEmpty()) {
                                mc.dropMessage("No skills found with the provided query.");
                            } else {
                                retSkills.forEach(mc::dropMessage);
                            }
                            break;
                        default:
                            mc.dropMessage("Sorry, that search type is unavailable.");
                            break;
                    }
                } else {
                    mc.dropMessage("Invalid search. Proper usage: '!search <type> [-re] <searchFor>', where <type> is MAP, USE, ETC, CASH, EQUIP, MOB (or MONSTER), or SKILL.");
                }
                break;
            case "!npc": {
                int npcId;
                try {
                    npcId = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                MapleNPC npc = MapleLifeFactory.getNPC(npcId);
                if (!npc.getName().equalsIgnoreCase("MISSINGNO")) {
                    npc.setPosition(player.getPosition());
                    npc.setCy(player.getPosition().y);
                    npc.setRx0(player.getPosition().x + 50);
                    npc.setRx1(player.getPosition().x - 50);
                    npc.setFh(player.getMap().getFootholds().findBelow(player.getPosition()).getId());
                    npc.setCustom(true);
                    player.getMap().addMapObject(npc);
                    player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                } else {
                    mc.dropMessage("You have entered an invalid Npc-Id");
                }

                break;
            }
            case "!removenpcs":
                List<MapleMapObject> npcs =
                    player
                        .getMap()
                        .getMapObjectsInRange(
                            player.getPosition(),
                            Double.POSITIVE_INFINITY,
                            MapleMapObjectType.NPC
                        );
                for (MapleMapObject npcmo : npcs) {
                    MapleNPC npc = (MapleNPC) npcmo;
                    if (npc.isCustom()) {
                        player.getMap().removeMapObject(npc.getObjectId());
                    }
                }
                break;
            case "!mynpcpos": {
                Point pos = player.getPosition();
                mc.dropMessage("X: " + pos.x + " | Y: " + pos.y + " | RX0: " + (pos.x + 50) + " | RX1: " + (pos.x - 50) + " | FH: " + player.getMap().getFootholds().findBelow(pos).getId());
                break;
            }
            case "!cleardrops": {
                MapleMap map = player.getMap();
                double range = Double.POSITIVE_INFINITY;
                List<MapleMapObject> items =
                    map.getMapObjectsInRange(player.getPosition(), range, MapleMapObjectType.ITEM);
                for (MapleMapObject itemmo : items) {
                    map.removeMapObject(itemmo);
                    map.broadcastMessage(MaplePacketCreator.removeItemFromMap(itemmo.getObjectId(), 0, player.getId()));
                }
                mc.dropMessage("You have destroyed " + items.size() + " items on the ground.");
                break;
            }
            case "!cleardropcache":
                MapleMonsterInformationProvider.getInstance().clearDrops();
                break;
            case "!clearshops":
                MapleShopFactory.getInstance().clear();
                break;
            case "!clearevents":
                for (ChannelServer instance : ChannelServer.getAllInstances()) {
                    instance.reloadEvents();
                }
                break;
            case "!permban": {
                String name = splitted[1];
                String reason = StringUtil.joinStringFrom(splitted, 2);
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(name);
                if (victim != null) {
                    if (!victim.isGM()) {
                        victim.ban(reason, true);
                        mc.dropMessage("Character permanently banned.");
                    } else {
                        mc.dropMessage("You can't ban a GM. Sorry");
                    }
                } else {
                    if (MapleCharacter.ban(name, reason, false)) {
                        mc.dropMessage("Permanently banned sucessfully");
                    } else {
                        mc.dropMessage("Error while banning.");
                    }
                }
                break;
            }
            case "!emote": {
                String name = splitted[1];
                int emote;
                try {
                    emote = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(name);
                if (victim != null) {
                    victim.getMap().broadcastMessage(victim, MaplePacketCreator.facialExpression(victim, emote), victim.getPosition());
                } else {
                    mc.dropMessage("Player was not found");
                }
                break;
            }
            case "!proitem":
                if (splitted.length == 3) {
                    int itemid;
                    short multiply;
                    try {
                        itemid = Integer.parseInt(splitted[1]);
                        multiply = Short.parseShort(splitted[2]);
                    } catch (NumberFormatException nfe) {
                        return;
                    }
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    IItem item = ii.getEquipById(itemid);
                    MapleInventoryType type = ii.getInventoryType(itemid);
                    if (type.equals(MapleInventoryType.EQUIP)) {
                        MapleInventoryManipulator.addFromDrop(c, ii.hardcoreItem((Equip) item, multiply));
                    } else {
                        mc.dropMessage("Make sure it's an equippable item.");
                    }
                } else {
                    mc.dropMessage("Invalid syntax.");
                }
                break;
            case "!addclones":
                if (splitted.length < 2) return;
                int clones;
                try {
                    clones = getOptionalIntArg(splitted, 1, 1);
                } catch (NumberFormatException nfe) {
                    return;
                }
                if (player.getFakeChars().size() >= 5) {
                    mc.dropMessage("You are not allowed to clone yourself over 5 times.");
                } else {
                    for (int i = 0; i < clones && i + player.getFakeChars().size() <= 6; ++i) {
                        FakeCharacter fc = new FakeCharacter(player, player.getId() + player.getFakeChars().size() + clones + i);
                        player.getFakeChars().add(fc);
                        c.getChannelServer().addClone(fc);
                    }
                    mc.dropMessage("You have cloned yourself " + player.getFakeChars().size() + " times so far.");
                }
                break;
            case "!removeclones":
                for (FakeCharacter fc : player.getFakeChars()) {
                    if (fc.getFakeChar().getMap() == player.getMap()) {
                        c.getChannelServer().getAllClones().remove(fc);
                        player.getMap().removePlayer(fc.getFakeChar());
                    }
                }
                player.getFakeChars().clear();
                mc.dropMessage("All your clones in the map removed.");
                break;
            case "!removeallclones":
                for (FakeCharacter fc : c.getChannelServer().getAllClones()) {
                    if (fc.getOwner() != null) {
                        fc.getOwner().getFakeChars().remove(fc);
                    }
                    fc.getFakeChar().getMap().removePlayer(fc.getFakeChar());
                }
                c.getChannelServer().getAllClones().clear();
                mc.dropMessage("ALL clones have been removed.");
                break;
            case "!follow": {
                int slot = Integer.parseInt(splitted[1]);
                FakeCharacter fc = player.getFakeChars().get(slot);
                if (fc == null) {
                    mc.dropMessage("Clone does not exist.");
                } else {
                    fc.setFollow(true);
                }
                break;
            }
            case "!pause": {
                int slot = Integer.parseInt(splitted[1]);
                FakeCharacter fc = player.getFakeChars().get(slot);
                if (fc == null) {
                    mc.dropMessage("Clone does not exist.");
                } else {
                    fc.setFollow(false);
                }
                break;
            }
            case "!stance":
                if (splitted.length == 3) {
                    int slot = Integer.parseInt(splitted[1]);
                    int stance = Integer.parseInt(splitted[2]);
                    player.getFakeChars().get(slot).getFakeChar().setStance(stance);
                }
                break;
            case "!killmonster":
                if (splitted.length == 2) {
                    MapleMap map = c.getPlayer().getMap();
                    int targetId = Integer.parseInt(splitted[1]);
                    List<MapleMapObject> monsters =
                        map.getMapObjectsInRange(
                            c.getPlayer().getPosition(),
                            Double.POSITIVE_INFINITY,
                            MapleMapObjectType.MONSTER
                        );
                    for (MapleMapObject monsterm : monsters) {
                        MapleMonster monster = (MapleMonster) monsterm;
                        if (monster.getId() == targetId) {
                            map.killMonster(monster, player, false);
                            break;
                        }
                    }
                }
                break;
            case "!removeoid":
                if (splitted.length == 2) {
                    MapleMap map = c.getPlayer().getMap();
                    int oid = Integer.parseInt(splitted[1]);
                    MapleMapObject obj = map.getMapObject(oid);
                    if (obj == null) {
                        mc.dropMessage("This oid does not exist.");
                    } else {
                        map.removeMapObject(obj);
                    }
                }
                break;
            case "!gmtext": {
                int text;
                // RegularChat
                if (splitted[1].equalsIgnoreCase("normal")) {
                    text = 0;
                    // MultiChat
                } else if (splitted[1].equalsIgnoreCase("orange")) {
                    text = 1;
                } else if (splitted[1].equalsIgnoreCase("pink")) {
                    text = 2;
                } else if (splitted[1].equalsIgnoreCase("purple")) {
                    text = 3;
                } else if (splitted[1].equalsIgnoreCase("green")) {
                    text = 4;
                    // ServerNotice
                } else if (splitted[1].equalsIgnoreCase("red")) {
                    text = 5;
                } else if (splitted[1].equalsIgnoreCase("blue")) {
                    text = 6;
                    // RegularChat
                } else if (splitted[1].equalsIgnoreCase("whitebg")) {
                    text = 7;
                    // Whisper
                } else if (splitted[1].equalsIgnoreCase("lightinggreen")) {
                    text = 8;
                    // MapleTip
                } else if (splitted[1].equalsIgnoreCase("yellow")) {
                    text = 9;
                } else {
                    mc.dropMessage("Wrong syntax: use !gmtext normal/orange/pink/purple/green/blue/red/whitebg/lightinggreen/yellow");
                    return;
                }

                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("UPDATE characters SET gmtext = ? WHERE name = ?");
                ps.setString(2, player.getName());
                ps.setInt(1, text);
                ps.executeUpdate();
                ps.close();
                player.setGMText(text);
                break;
            }
            case "!currentdate":
                Calendar cal = Calendar.getInstance();
                int day = cal.get(Calendar.DATE);
                int month = cal.get(Calendar.MONTH) + 1; // It's an array of months.

                int year = cal.get(Calendar.YEAR);
                mc.dropMessage(day + "/" + month + "/" + year);
                break;
            case "!maxmesos":
                player.gainMeso(Integer.MAX_VALUE - player.getMeso());
                break;
            case "!fullcharge":
                player.setEnergyBar(10000);
                c.getSession().write(MaplePacketCreator.giveEnergyCharge(10000));
                break;
            case "!youlose":
                for (MapleCharacter victim : player.getMap().getCharacters()) {
                    if (victim != null) {
                        if (victim.getHp() <= 0) {
                            victim.dropMessage("You have lost the event.");
                            victim.changeMap(100000000);
                        } else {
                            victim.setHp(victim.getCurrentMaxHp());
                            victim.updateSingleStat(MapleStat.HP, victim.getHp());
                            victim.setMp(victim.getCurrentMaxMp());
                            victim.updateSingleStat(MapleStat.MP, victim.getMp());
                        }
                    }
                }
                break;
            case "!resettrialcooldown":
                player.setLastTrialTime(0L);
                mc.dropMessage("Your Monster Trial cooldown has been reset.");
                break;
            case "!resetap":
                player.setStr(4);
                player.updateSingleStat(MapleStat.STR, player.getStr());
                player.setDex(4);
                player.updateSingleStat(MapleStat.DEX, player.getDex());
                player.setInt(4);
                player.updateSingleStat(MapleStat.INT, player.getInt());
                player.setLuk(4);
                player.updateSingleStat(MapleStat.LUK, player.getLuk());
                break;
            case "!resetskills":
                player.resetAllSkills();
                break;
            case "!resetdailyprize":
                Date ldp = new Date();
                ldp.setTime(0);
                player.setLastDailyPrize(ldp);
                break;
            case "!sha1":
                if (splitted.length > 1) {
                    mc.dropMessage(LoginCrypto.hexSha1(splitted[1]));
                }
                break;
            case "!sendmedamagepacket":
                int damagefrom = Integer.parseInt(splitted[1]);
                player.getClient().getSession().write(MaplePacketCreator.damagePlayer(damagefrom, 0, player.getId(), 69));
                break;
            case "!levelpersongrad": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                int target = getOptionalIntArg(splitted, 2, victim.getLevel() + 1);
                int startlevel = victim.getLevel();
                for (int i = 0; i < target - startlevel; ++i) {
                    victim.levelUp();
                    victim.setExp(0);
                    victim.updateSingleStat(MapleStat.EXP, 0);
                }
                break;
            }
            case "!cachecashequips":
                MapleItemInformationProvider.getInstance().cacheCashEquips();
                break;
            case "!invokemethod":
                if (player.getMap().getPartyQuestInstance() != null) {
                    if (splitted.length > 2) {
                        String[] stringArgs = Arrays.copyOfRange(splitted, 2, splitted.length);
                        List<Object> args = new ArrayList<>(2);
                        for (String stringArg : stringArgs) {
                            try {
                                int intArg = Integer.parseInt(stringArg);
                                args.add(intArg);
                            } catch (NumberFormatException nfe) {
                                args.add(stringArg);
                            }
                        }
                        player.getMap().getPartyQuestInstance().invokeMethod(splitted[1], args.toArray());
                    } else {
                        player.getMap().getPartyQuestInstance().invokeMethod(splitted[1]);
                    }
                } else {
                    MapleMap map = cserv.getMapFactory().getMap(Integer.parseInt(splitted[1]));
                    if (splitted.length > 3) {
                        String[] stringArgs = Arrays.copyOfRange(splitted, 3, splitted.length);
                        List<Object> args = new ArrayList<>(2);
                        for (String stringArg : stringArgs) {
                            try {
                                int intArg = Integer.parseInt(stringArg);
                                args.add(intArg);
                            } catch (NumberFormatException nfe) {
                                args.add(stringArg);
                            }
                        }
                        map.getPartyQuestInstance().invokeMethod(splitted[2], args.toArray());
                    } else {
                        map.getPartyQuestInstance().invokeMethod(splitted[2]);
                    }
                }
                break;
            case "!clearpqs":
                player.getClient().getChannelServer().disposePartyQuests();
                break;
            case "!reloadpqmiscript":
                if (splitted.length > 1) {
                    MapleMap map = cserv.getMapFactory().getMap(Integer.parseInt(splitted[1]));
                    if (map.getPartyQuestInstance() != null) {
                        map.getPartyQuestInstance().reloadScript();
                    }
                } else if (player.getMap().getPartyQuestInstance() != null) {
                    player.getMap().getPartyQuestInstance().reloadScript();
                }
                break;
            case "!registerpqmi": {
                if (player.getMap().getPartyQuestInstance() != null) {
                    player.dropMessage(5, "Map instance is already registered for this map.");
                    break;
                }
                final MapleMap map = player.getMap();
                if (player.getPartyQuest() != null) {
                    map.removePlayer(player);
                    player.getPartyQuest().registerMap(player.getMapId());
                    map.addPlayer(player);
                } else if (player.getParty() != null && !player.getParty().getMembers().isEmpty()) {
                    PartyQuest pq = new PartyQuest(c.getChannel(), splitted[1], 1, 100000000);
                    pq.registerParty(player.getParty());
                    map.removePlayer(player);
                    pq.registerMap(player.getMapId());
                    map.addPlayer(player);
                } else {
                    player.dropMessage(5, "You're not in a party.");
                }
                break;
            }
            case "!toggledpm":
                player.toggleDpm();
                player.dropMessage("Showing DPM on death is now " + (player.doShowDpm() ? "on" : "off") + ".");
                break;
            case "!showdpm":
                final DecimalFormat df = new DecimalFormat("#.000");
                TimerManager tMan = TimerManager.getInstance();
                int duration, repeatTime;
                switch (splitted.length) {
                    case 1:
                        player.getMap()
                              .getMapObjectsInRange(
                                  player.getPosition(),
                                  Double.POSITIVE_INFINITY,
                                  MapleMapObjectType.MONSTER
                              )
                              .stream()
                              .map(mmo -> (MapleMonster) mmo)
                              .forEach(mob ->
                                  player.dropMessage(
                                      mob.getName() +
                                          ", oid: " +
                                          mob.getObjectId() +
                                          ", incoming DPM: " +
                                          df.format(mob.avgIncomingDpm())
                                  )
                              );
                        return;
                    case 2:
                        duration = Integer.parseInt(splitted[1]) * 1000;
                        repeatTime = 5000;
                        break;
                    case 3:
                        duration = Integer.parseInt(splitted[1]) * 1000;
                        repeatTime = Integer.parseInt(splitted[2]) * 1000;
                        break;
                    default:
                        player.dropMessage("Invalid syntax.");
                        return;
                }
                final ScheduledFuture<?> showDpmTask = tMan.register(() ->
                        player.getMap()
                              .getMapObjectsInRange(
                                  player.getPosition(),
                                  Double.POSITIVE_INFINITY,
                                  MapleMapObjectType.MONSTER
                              )
                              .stream()
                              .map(mmo -> (MapleMonster) mmo)
                              .filter(MapleMonster::isBoss)
                              .forEach(mob ->
                                  player.dropMessage(
                                      mob.getName() +
                                          ", oid: " +
                                          mob.getObjectId() +
                                          ", incoming DPM: " +
                                          df.format(mob.avgIncomingDpm())
                                  )
                              ),
                    repeatTime, 0);
                tMan.schedule(() -> showDpmTask.cancel(false), duration);
                break;
            case "!toggletrackmissgodmode":
                ChannelServer.getAllInstances().forEach(cs -> cs.setTrackMissGodmode(!cs.getTrackMissGodmode()));
                player.dropMessage(
                    "Miss godmode is now " +
                        (c.getChannelServer().getTrackMissGodmode() ? "" : "no longer ") +
                        "being tracked."
                );
                break;
            case "!registerevent":
                String syntax = "Syntax: !registerevent <map_id> | !registerevent <event_name>";
                if (splitted.length == 2) {
                    try {
                        int mapId = Integer.parseInt(splitted[1]);
                        try {
                            if (c.getChannelServer().getMapFactory().getMap(mapId) != null) {
                                c.getChannelServer().setEventMap(mapId);
                                mc.dropMessage("Event map successfully set to " + mapId);
                            } else {
                                mc.dropMessage("That map doesn't seem to exist!");
                            }
                        } catch (Exception e) {
                            mc.dropMessage("That map doesn't seem to exist!");
                        }
                    } catch (NumberFormatException nfe) {
                        int mapId;
                        switch (splitted[1].toLowerCase()) {
                            case "":
                                mapId = 0;
                                break;
                            default:
                                mc.dropMessage(syntax);
                                return;
                        }
                        c.getChannelServer().setEventMap(mapId);
                        mc.dropMessage("Event map successfully set to " + mapId);
                    }
                } else {
                    mc.dropMessage(syntax);
                }
                break;
            case "!unregisterevent":
                c.getChannelServer().setEventMap(0);
                break;
            case "!warpoutofevent":
                cserv.getPlayerStorage()
                     .getAllCharacters()
                     .stream()
                     .filter(p -> p.getPreEventMap() > 0)
                     .forEach(p -> {
                         p.changeMap(p.getPreEventMap());
                         p.setPreEventMap(0);
                     });
                break;
            case "!resetpreeventmaps":
                cserv.getPlayerStorage()
                     .getAllCharacters()
                     .forEach(p -> p.setPreEventMap(0));
                break;
            case "!giftvp": {
                if (splitted.length != 3) {
                    mc.dropMessage("Syntax: !giftvp <player_name> <vote_point_count>");
                    return;
                }
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    int amount;
                    try {
                        amount = Integer.parseInt(splitted[2]);
                    } catch (NumberFormatException nfe) {
                        mc.dropMessage("Couldn't parse integer for <votePointCount>");
                        return;
                    }
                    victim.setVotePoints(victim.getVotePoints() + amount);
                    victim.dropMessage(5, player.getName() + " has gifted you " + amount + " vote points.");
                    mc.dropMessage("Vote points recieved.");
                } else {
                    mc.dropMessage("Player not found.");
                }
                break;
            }
            case "!unregisterallpqmis": {
                MapleMapFactory mapFact = c.getChannelServer().getMapFactory();
                for (int i = 5000; i <= 5012; ++i) {
                    if (mapFact.isMapLoaded(i)) {
                        mapFact.getMap(i).unregisterPartyQuestInstance();
                    }
                }
                break;
            }
            case "!givedeathitems": {
                if (splitted.length < 3) {
                    mc.dropMessage(
                        "Syntax: !givedeathitems <deceased_player> <player_to_give_to> [offset=0] [useCache=false]"
                    );
                    return;
                }

                String deceasedName = splitted[1];

                final MapleCharacter target = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
                if (target == null) {
                    mc.dropMessage(
                        "Could not find the player " +
                            MapleCharacterUtil.makeMapleReadable(splitted[2]) +
                            " on your channel"
                    );
                    return;
                }

                int offset = 0;
                boolean useCache = false;
                if (splitted.length > 3) {
                    try {
                        offset = Integer.parseInt(splitted[3]);
                    } catch (NumberFormatException nfe) {
                        mc.dropMessage("Could not parse integer for optional argument [offset]");
                        return;
                    }
                    if (splitted.length > 4) {
                        switch (splitted[4].toLowerCase()) {
                            case "true":
                            case "yes":
                                useCache = true;
                                break;
                        }
                    }
                }

                List<IItem> items;
                try {
                    items = DeathLogReader.getInstance().readDeathItems(deceasedName, offset, useCache);
                } catch (Exception e) {
                    mc.dropMessage("Retrieving death items failed:");
                    mc.dropMessage(e.toString());
                    return;
                }

                items.forEach(i -> gainItem(target.getClient(), i));
                break;
            }
            case "!stunall": {
                final long stunTime;
                if (splitted.length > 1) {
                    try {
                        stunTime = Integer.parseInt(splitted[1]) * 1000L;
                    } catch (NumberFormatException nfe) {
                        mc.dropMessage("Could not parse given value for stun time.");
                        return;
                    }
                } else {
                    stunTime = 5L * 1000L;
                }
                int i = 0;
                for (MapleCharacter p : player.getMap().getCharacters()) {
                    if (!p.isGM()) {
                        p.forciblyGiveDebuff(123, 13, stunTime);
                        i++;
                    }
                }
                mc.dropMessage(i + " players stunned for " + (stunTime / 1000L) + " seconds.");
                break;
            }
            case "!muteall": {
                int i = 0;
                for (MapleCharacter p : player.getMap().getCharacters()) {
                    if (!p.isGM()) {
                        if (p.getCanTalk()) i++;
                        p.canTalk(false);
                    }
                }
                mc.dropMessage(i + " players muted.");
                break;
            }
            case "!unmuteall": {
                int i = 0;
                for (MapleCharacter p : player.getMap().getCharacters()) {
                    if (!p.isGM()) {
                        if (!p.getCanTalk()) i++;
                        p.canTalk(true);
                    }
                }
                mc.dropMessage(i + " players unmuted.");
                break;
            }
            case "!togglemuteall": {
                int i = 0, j = 0;
                for (MapleCharacter p : player.getMap().getCharacters()) {
                    if (!p.isGM()) {
                        if (p.getCanTalk()) {
                            i++;
                        } else {
                            j++;
                        }
                        p.canTalk(!p.getCanTalk());
                    }
                }
                mc.dropMessage(i + " players muted, " + j + " players unmuted.");
                break;
            }
            case "!drop": {
                if (splitted.length < 2 || splitted.length > 4) {
                    mc.dropMessage("Invalid syntax. Use: !drop <item_id> [quantity] [separate]");
                    return;
                }
                final int itemId;
                final short quantity;
                boolean separate = splitted.length > 3 && Boolean.parseBoolean(splitted[3]);
                try {
                    itemId = Integer.parseInt(splitted[1]);
                    if (splitted.length > 2) {
                        quantity = Short.parseShort(splitted[2]);
                    } else {
                        quantity = 1;
                    }
                } catch (NumberFormatException nfe) {
                    mc.dropMessage("Could not parse numeric arguments. Use: !drop <item_id> [quantity]");
                    return;
                }
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final IItem item;
                if (ii.getInventoryType(itemId).equals(MapleInventoryType.EQUIP)) {
                    item = ii.getEquipById(itemId);
                    item.setQuantity((short) 1);
                    if (quantity == 1) {
                        player.getMap().spawnItemDrop(player, player, item, player.getPosition(), true, true);
                    } else {
                        final Point pos = new Point(player.getPosition());
                        for (short i = 0; i < quantity; ++i) {
                            final int _i = i;
                            TimerManager
                                .getInstance()
                                .schedule(() ->
                                    player
                                        .getMap()
                                        .spawnItemDrop(
                                            player,
                                            player,
                                            item,
                                            new Point(
                                                pos.x + (_i + 1) / 2 * 40 * (_i % 2 == 0 ? 1 : -1),
                                                pos.y
                                            ),
                                            true,
                                            true
                                        ),
                                    i * 150L
                                );
                        }
                    }
                } else if (separate && quantity > 1) {
                    final Point pos = new Point(player.getPosition());
                    for (short i = 0; i < quantity; ++i) {
                        final int _i = i;
                        TimerManager
                            .getInstance()
                            .schedule(() ->
                                player
                                    .getMap()
                                    .spawnItemDrop(
                                        player,
                                        player,
                                        new Item(itemId, (byte) 0, quantity),
                                        new Point(
                                            pos.x + (_i + 1) / 2 * 40 * (_i % 2 == 0 ? 1 : -1),
                                            pos.y
                                        ),
                                        true,
                                        true
                                    ),
                                i * 150L
                            );
                    }
                } else {
                    item = new Item(itemId, (byte) 0, quantity);
                    player.getMap().spawnItemDrop(player, player, item, player.getPosition(), true, true);
                }
                break;
            }
            case "!makeeqp": {
                if (splitted.length < 2 || splitted.length % 2 == 1) {
                    mc.dropMessage("Invalid syntax. Use: !makeeqp <item_id> [val1 stat1] [val2 stat2]...");
                    return;
                }
                if (player.getInventory(MapleInventoryType.EQUIP).isFull()) {
                    mc.dropMessage("Your equipment inventory is full.");
                    return;
                }
                try {
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    int itemId = Integer.parseInt(splitted[1]);
                    if (
                        ii.getInventoryType(itemId) != MapleInventoryType.EQUIP ||
                        ii.isThrowingStar(itemId) ||
                        ii.isBullet(itemId)
                    ) {
                        mc.dropMessage("That is not the ID of an equipment item.");
                        return;
                    }
                    Equip item = ii.getEquipByIdAsEquip(itemId);
                    for (int i = 2; i < splitted.length; i += 2) {
                        short val = Short.parseShort(splitted[i]);
                        String stat = splitted[i + 1].toLowerCase();
                        switch (stat) {
                            case "accuracy":
                            case "acc":
                                item.setAcc(val);
                                break;
                            case "avoidability":
                            case "eva":
                            case "avoid":
                                item.setAvoid(val);
                                break;
                            case "dex":
                                item.setDex(val);
                                break;
                            case "str":
                                item.setStr(val);
                                break;
                            case "hands":
                                item.setHands(val);
                                break;
                            case "luck":
                            case "luk":
                                item.setLuk(val);
                                break;
                            case "int":
                                item.setInt(val);
                                break;
                            case "watt":
                            case "watk":
                            case "atk":
                            case "att":
                                item.setWatk(val);
                                break;
                            case "wdef":
                                item.setWdef(val);
                                break;
                            case "matt":
                            case "matk":
                                item.setMatk(val);
                                break;
                            case "mdef":
                                item.setMdef(val);
                                break;
                            case "maxhp":
                            case "hp":
                                item.setHp(val);
                                break;
                            case "maxmp":
                            case "mp":
                                item.setMp(val);
                                break;
                            case "speed":
                                item.setSpeed(val);
                                break;
                            case "jump":
                                item.setJump(val);
                                break;
                            case "upgradeslots":
                            case "tuc":
                            case "slots":
                                item.setUpgradeSlots((byte) val);
                                break;
                            case "level":
                            case "lvl":
                            case "lv":
                                item.setLevel((byte) val);
                                break;
                            default:
                                mc.dropMessage("Unrecognized argument: " + stat);
                                return;
                        }
                    }
                    MapleInventoryManipulator.addFromDrop(c, item, true);
                } catch (NumberFormatException nfe) {
                    mc.dropMessage(
                        "Error parsing numeric argument. " +
                            "Use: !makeeqp <item_id> [val1 stat1] [val2 stat2]..."
                    );
                    return;
                }
                break;
            }
            case "!makeeqpperson": {
                if (splitted.length < 3 || splitted.length % 2 == 0) {
                    mc.dropMessage(
                        "Invalid syntax. " +
                            "Use: !makeeqpperson <player_name> <item_id> [val1 stat1] [val2 stat2]..."
                    );
                    return;
                }
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null) {
                    mc.dropMessage("Cound not find the specified player on your channel.");
                    return;
                }
                if (victim.getInventory(MapleInventoryType.EQUIP).isFull()) {
                    mc.dropMessage("Their equipment inventory is full.");
                    return;
                }
                try {
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    int itemId = Integer.parseInt(splitted[2]);
                    if (
                        ii.getInventoryType(itemId) != MapleInventoryType.EQUIP ||
                        ii.isThrowingStar(itemId) ||
                        ii.isBullet(itemId)
                    ) {
                        mc.dropMessage("That is not the ID of an equipment item.");
                        return;
                    }
                    Equip item = ii.getEquipByIdAsEquip(itemId);
                    for (int i = 3; i < splitted.length; i += 2) {
                        short val = Short.parseShort(splitted[i]);
                        String stat = splitted[i + 1].toLowerCase();
                        switch (stat) {
                            case "accuracy":
                            case "acc":
                                item.setAcc(val);
                                break;
                            case "avoidability":
                            case "eva":
                            case "avoid":
                                item.setAvoid(val);
                                break;
                            case "dex":
                                item.setDex(val);
                                break;
                            case "str":
                                item.setStr(val);
                                break;
                            case "hands":
                                item.setHands(val);
                                break;
                            case "luck":
                            case "luk":
                                item.setLuk(val);
                                break;
                            case "int":
                                item.setInt(val);
                                break;
                            case "watt":
                            case "watk":
                            case "atk":
                            case "att":
                                item.setWatk(val);
                                break;
                            case "wdef":
                                item.setWdef(val);
                                break;
                            case "matt":
                            case "matk":
                                item.setMatk(val);
                                break;
                            case "mdef":
                                item.setMdef(val);
                                break;
                            case "maxhp":
                            case "hp":
                                item.setHp(val);
                                break;
                            case "maxmp":
                            case "mp":
                                item.setMp(val);
                                break;
                            case "speed":
                                item.setSpeed(val);
                                break;
                            case "jump":
                                item.setJump(val);
                                break;
                            case "upgradeslots":
                            case "tuc":
                            case "slots":
                                item.setUpgradeSlots((byte) val);
                                break;
                            case "level":
                            case "lvl":
                            case "lv":
                                item.setLevel((byte) val);
                                break;
                            default:
                                mc.dropMessage("Unrecognized argument: " + stat);
                                return;
                        }
                    }
                    MapleInventoryManipulator.addFromDrop(victim.getClient(), item, true);
                } catch (NumberFormatException nfe) {
                    mc.dropMessage(
                        "Error parsing numeric argument. " +
                            "Use: !makeeqpperson <player_name> <item_id> [val1 stat1] [val2 stat2]..."
                    );
                    return;
                }
                break;
            }
            case "!killmob": {
                boolean withDrops = false;
                if (splitted.length > 1) {
                    withDrops = Boolean.parseBoolean(splitted[1]);
                }
                if (player.getMap().getAllMonsters().size() < 1) {
                    mc.dropMessage("There are no mobs on this map to kill.");
                    return;
                }
                player.getMap().killMonster(player.getMap().getAllMonsters().get(0), player, withDrops);
                break;
            }
            case "!killallanddrop": {
                MapleMap map = player.getMap();
                List<MapleMapObject> monsters =
                    map.getMapObjectsInRange(
                        player.getPosition(),
                        Double.POSITIVE_INFINITY,
                        MapleMapObjectType.MONSTER
                    );
                for (final MapleMapObject monstermo : monsters) {
                    MapleMonster monster = (MapleMonster) monstermo;
                    map.killMonster(monster, player, true);
                }
                mc.dropMessage("Killed " + monsters.size() + " monsters.");
                break;
            }
            case "!itemquantity": {
                if (splitted.length != 3) {
                    mc.dropMessage("Invalid syntax. Use: !itemquantity <player_name> <item_id>");
                    return;
                }
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null) {
                    mc.dropMessage("Could not find that player on your channel.");
                    return;
                }
                int itemId;
                try {
                    itemId = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException nfe) {
                    mc.dropMessage("Could not parse integer argument for item ID.");
                    return;
                }
                int q = victim.getItemQuantity(itemId, false);
                mc.dropMessage(victim.getName() + " has " + q + " of item " + itemId + ".");
                break;
            }
            case "!listinv": {
                if (splitted.length != 3) {
                    mc.dropMessage("Invalid syntax. Use: !listinv <player_name> <inv_type>");
                    return;
                }
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null) {
                    mc.dropMessage("Could not find that player on your channel.");
                    return;
                }
                MapleInventoryType type;
                switch (splitted[2].toLowerCase()) {
                    case "use":
                    case "consume":
                        type = MapleInventoryType.USE;
                        break;
                    case "eq":
                    case "eqp":
                    case "equip":
                    case "equipment":
                        type = MapleInventoryType.EQUIP;
                        break;
                    case "equipped":
                        type = MapleInventoryType.EQUIPPED;
                        break;
                    case "etc":
                        type = MapleInventoryType.ETC;
                        break;
                    case "cash":
                    case "nx":
                        type = MapleInventoryType.CASH;
                        break;
                    case "setup":
                        type = MapleInventoryType.SETUP;
                        break;
                    default:
                        mc.dropMessage("Unrecognized inventory type.");
                        return;
                }
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                MapleInventory inv = victim.getInventory(type);
                StringBuilder sb = new StringBuilder();
                for (IItem item : inv) {
                    sb.append(ii.getName(item.getItemId()));
                    if (item.getQuantity() > 1) {
                        sb.append(" x")
                          .append(item.getQuantity());
                    }
                    sb.append(", ");
                }
                mc.dropMessage(type.name() + " items for " + victim.getName() + ":");
                mc.dropMessage(sb.toString());
                break;
            }
            case "!forcenpc": {
                if (splitted.length != 3) {
                    mc.dropMessage("Invalid syntax. Use: !forcenpc <player_name> <npc_id>");
                    return;
                }
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null) {
                    mc.dropMessage("Could not find that player on your channel.");
                    return;
                }
                int npcId;
                try {
                    npcId = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException nfe) {
                    mc.dropMessage("Could not parse integer argument for NPC ID.");
                    return;
                }
                try {
                    if (victim.getClient().getCM() != null) {
                        victim.getClient().getCM().dispose();
                    }
                    NPCScriptManager
                        .getInstance()
                        .start(victim.getClient(), npcId);
                } catch (Exception e) {
                    mc.dropMessage("There was an error forcing the NPC conversation: " + e);
                    return;
                }
                mc.dropMessage("Success.");
                break;
            }
            case "!reverselookup": {
                try {
                    mc.dropMessage(
                        MapleItemInformationProvider
                            .getInstance()
                            .getName(
                                Integer.parseInt(splitted[1])
                            )
                    );
                } catch (Exception e) {
                    mc.dropMessage("An error occured: " + e.getLocalizedMessage());
                }
                break;
            }
            case "!warpmy": {
                if (splitted.length != 3) {
                    mc.dropMessage(
                        "Invalid syntax. Use !warpmy <left/right> <map_id> or !warpmy <left/right> preevent"
                    );
                    return;
                }
                Predicate<MapleCharacter> toBeWarped;
                if (splitted[1].equalsIgnoreCase("left")) {
                    toBeWarped = p -> !p.isGM() && p.getPosition().x < player.getPosition().x;
                } else if (splitted[1].equalsIgnoreCase("right")) {
                    toBeWarped = p -> !p.isGM() && p.getPosition().x > player.getPosition().x;
                } else {
                    mc.dropMessage(
                        "Invalid syntax. Use !warpmy <left/right> <map_id> or !warpmy <left/right> preevent"
                    );
                    return;
                }
                boolean preEvent_ = false;
                int mapId_ = 0;
                if (splitted[2].equalsIgnoreCase("preevent")) {
                    preEvent_ = true;
                } else {
                    try {
                        mapId_ = Integer.parseInt(splitted[1]);
                    } catch (NumberFormatException nfe) {
                        mc.dropMessage("Could not parse integer for map ID.");
                        return;
                    }
                }
                final boolean preEvent = preEvent_;
                final int mapId = mapId_;
                player
                    .getMap()
                    .getCharacters()
                    .stream()
                    .filter(toBeWarped)
                    .forEach(p -> {
                        if (preEvent) {
                            p.changeMap(p.getPreEventMap());
                        } else {
                            p.changeMap(mapId);
                        }
                    });
                break;
            }
            case "!hackcheck": {
                StringBuilder builder = new StringBuilder();
                final String victimName = splitted[1];
                final MapleCharacter victim =
                    ChannelServer
                        .getAllInstances()
                        .stream()
                        .map(cs -> cs.getPlayerStorage().getCharacterByName(victimName))
                        .filter(Objects::nonNull)
                        .findAny()
                        .orElse(null);
                if (victim == null) break;
                builder.append(MapleCharacterUtil.makeMapleReadable(victim.getName()));
                builder.append(", ID: ");
                builder.append(victim.getId());
                builder.append(", Account: ");
                builder.append(MapleCharacterUtil.makeMapleReadable(victim.getClient().getAccountName()));
                mc.dropMessage(builder.toString());

                builder = new StringBuilder();
                builder.append("Account ID: ");
                builder.append(victim.getAccountID());
                builder.append(", Remote addr.: ");
                builder.append(victim.getClient().getSession().getRemoteAddress());
                mc.dropMessage(builder.toString());
                mc.dropMessage("MACs:");
                victim.getClient().getMacs().stream().map(m -> "    " + m).forEach(mc::dropMessage);

                final Set<Integer> accounts = new LinkedHashSet<>();
                mc.dropMessage("Accounts with same password:");
                Connection con = DatabaseConnection.getConnection();
                try {
                    PreparedStatement ps;
                    ResultSet rs;
                    ps = con.prepareStatement("SELECT id, name FROM accounts WHERE password = ? AND id != ?");
                    ps.setString(1, victim.getClient().getAccountPass());
                    ps.setInt(2, victim.getAccountID());
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        accounts.add(rs.getInt("id"));
                        mc.dropMessage("    " + MapleCharacterUtil.makeMapleReadable(rs.getString("name")));
                    }
                    rs.close();
                    ps.close();

                    mc.dropMessage("Accounts with same MAC(s):");
                    for (final String mac : victim.getClient().getMacs()) {
                        mc.dropMessage("    " + mac + ":");
                        ps = con.prepareStatement("SELECT id, name FROM accounts WHERE macs LIKE ? AND id != ?");
                        ps.setString(1, "%" + mac + "%");
                        ps.setInt(2, victim.getAccountID());
                        rs = ps.executeQuery();
                        while (rs.next()) {
                            accounts.add(rs.getInt("id"));
                            mc.dropMessage("        " + MapleCharacterUtil.makeMapleReadable(rs.getString("name")));
                        }
                        rs.close();
                        ps.close();
                    }

                    mc.dropMessage("Accounts with same IP:");
                    ps = con.prepareStatement("SELECT id, name FROM accounts WHERE lastknownip = ? AND id != ?");
                    final String sockAddr = victim.getClient().getSession().getRemoteAddress().toString();
                    ps.setString(1, sockAddr.substring(1, sockAddr.lastIndexOf(':')));
                    ps.setInt(2, victim.getAccountID());
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        accounts.add(rs.getInt("id"));
                        mc.dropMessage("    " + MapleCharacterUtil.makeMapleReadable(rs.getString("name")));
                    }
                    rs.close();
                    ps.close();

                    mc.dropMessage("Accounts with similar names:");
                    ps = con.prepareStatement("SELECT id, name FROM accounts WHERE id != ?");
                    ps.setInt(1, victim.getAccountID());
                    rs = ps.executeQuery();
                    final String acctName = victim.getClient().getAccountName();
                    while (rs.next()) {
                        final String foreignAcctName = rs.getString("name");
                        float nameDist = distance(acctName, foreignAcctName);
                        nameDist /= Math.sqrt(floatMin((float) acctName.length(), (float) foreignAcctName.length()));
                        if (nameDist < 1.375f) {
                            accounts.add(rs.getInt("id"));
                            mc.dropMessage("    " + MapleCharacterUtil.makeMapleReadable(foreignAcctName));
                        }
                    }
                    rs.close();
                    ps.close();

                    mc.dropMessage("Suspicious account info:");
                    for (final Integer aid : accounts) {
                        ps = con.prepareStatement(
                            "SELECT name, lastlogin, banned, banreason FROM accounts WHERE id = ?"
                        );
                        ps.setInt(1, aid);
                        rs = ps.executeQuery();
                        if (rs.next()) {
                            mc.dropMessage(
                                "    " +
                                    MapleCharacterUtil.makeMapleReadable(
                                        rs.getString("name")
                                    ) +
                                    ":"
                            );
                            boolean banned = rs.getInt("banned") > 0;
                            mc.dropMessage("        Banned: " + banned);
                            if (banned) {
                                mc.dropMessage(
                                    "        Ban reason: " +
                                        rs.getString("banreason")
                                );
                            }
                            mc.dropMessage("        Last login: " + rs.getTimestamp("lastlogin"));
                        }
                        rs.close();
                        ps.close();

                        mc.dropMessage("        Characters:");
                        ps = con.prepareStatement(
                            "SELECT name, level, job FROM characters WHERE accountid = ?"
                        );
                        ps.setInt(1, aid);
                        rs = ps.executeQuery();
                        while (rs.next()) {
                            mc.dropMessage(
                                "            " +
                                    rs.getString("name") +
                                    ", level " +
                                    rs.getInt("level") +
                                    " " +
                                    MapleJob.getJobName(rs.getInt("job")) +
                                    "."
                            );
                        }
                        rs.close();
                        ps.close();
                    }
                } catch (SQLException sqle) {
                    mc.dropMessage(sqle.getLocalizedMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case "!clearquestcache":
                MapleCQuests.clearCache();
                mc.dropMessage("Quest cache cleared successfully.");
                break;
            case "!reloadallquests":
                MapleCQuests.clearCache();
                ChannelServer
                    .getAllInstances()
                    .forEach(cs ->
                        cs.getPlayerStorage()
                          .getAllCharacters()
                          .forEach(MapleCharacter::softReloadCQuests)
                    );
                break;
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("lowhp", 3),
            new CommandDefinition("sp", 3),
            new CommandDefinition("ap", 3),
            new CommandDefinition("job", 3),
            new CommandDefinition("whereami", 3),
            new CommandDefinition("shop", 3),
            new CommandDefinition("opennpc", 3),
            new CommandDefinition("levelup", 3),
            new CommandDefinition("setmaxmp", 3),
            new CommandDefinition("setmaxhp", 3),
            new CommandDefinition("healmap", 3),
            new CommandDefinition("item", 3),
            new CommandDefinition("noname", 3),
            new CommandDefinition("dropmesos", 3),
            new CommandDefinition("level", 3),
            new CommandDefinition("allonline", 3),
            new CommandDefinition("banreason", 3),
            new CommandDefinition("whitechat", 3),
            new CommandDefinition("joinguild", 3),
            new CommandDefinition("unbuffmap", 3),
            new CommandDefinition("mesos", 3),
            new CommandDefinition("setname", 3),
            new CommandDefinition("clearslot", 3),
            new CommandDefinition("ariantpq", 3),
            new CommandDefinition("scoreboard", 3),
            new CommandDefinition("array", 3),
            new CommandDefinition("slap", 3),
            new CommandDefinition("rreactor", 3),
            new CommandDefinition("coke", 3),
            new CommandDefinition("papu", 3),
            new CommandDefinition("zakum", 3),
            new CommandDefinition("ergoth", 3),
            new CommandDefinition("ludimini", 3),
            new CommandDefinition("cornian", 3),
            new CommandDefinition("balrog", 3),
            new CommandDefinition("mushmom", 3),
            new CommandDefinition("wyvern", 3),
            new CommandDefinition("pirate", 3),
            new CommandDefinition("clone", 3),
            new CommandDefinition("anego", 3),
            new CommandDefinition("theboss", 3),
            new CommandDefinition("snackbar", 3),
            new CommandDefinition("papapixie", 3),
            new CommandDefinition("nxslimes", 3),
            new CommandDefinition("horseman", 3),
            new CommandDefinition("blackcrow", 3),
            new CommandDefinition("leafreboss", 3),
            new CommandDefinition("shark", 3),
            new CommandDefinition("franken", 3),
            new CommandDefinition("bird", 3),
            new CommandDefinition("pianus", 3),
            new CommandDefinition("centipede", 3),
            new CommandDefinition("horntail", 3),
            new CommandDefinition("killall", 3),
            new CommandDefinition("help", 3),
            new CommandDefinition("say", 3),
            new CommandDefinition("gender", 3),
            new CommandDefinition("spy", 3),
            new CommandDefinition("levelperson", 3),
            new CommandDefinition("skill", 3),
            new CommandDefinition("setall", 3),
            new CommandDefinition("giftnx", 3),
            new CommandDefinition("maxskills", 3),
            new CommandDefinition("fame", 3),
            new CommandDefinition("unhide", 3),
            new CommandDefinition("heal", 3),
            new CommandDefinition("unbuff", 3),
            new CommandDefinition("sendhint", 3),
            new CommandDefinition("smega", 3),
            new CommandDefinition("mutesmega", 3),
            new CommandDefinition("mute", 3),
            new CommandDefinition("givedisease", 3),
            new CommandDefinition("dc", 3),
            new CommandDefinition("charinfo", 3),
            new CommandDefinition("connected", 3),
            new CommandDefinition("clock", 3),
            new CommandDefinition("warp", 3),
            new CommandDefinition("warphere", 3),
            new CommandDefinition("jail", 3),
            new CommandDefinition("map", 3),
            new CommandDefinition("warpallhere", 3),
            new CommandDefinition("warpwholeworld", 3),
            new CommandDefinition("mesosrate", 3),
            new CommandDefinition("droprate", 3),
            new CommandDefinition("bossdroprate", 3),
            new CommandDefinition("exprate", 3),
            new CommandDefinition("godlyitemrate", 3),
            new CommandDefinition("itemstat", 3),
            new CommandDefinition("togglegodlyitems", 3),
            new CommandDefinition("servermessage", 3),
            new CommandDefinition("whosthere", 3),
            new CommandDefinition("cheaters", 3),
            new CommandDefinition("fakerelog", 3),
            new CommandDefinition("getrings", 3),
            new CommandDefinition("ring", 3),
            new CommandDefinition("removering", 3),
            new CommandDefinition("nearestspawn", 3),
            new CommandDefinition("nearestportal", 3),
            new CommandDefinition("unban", 3),
            new CommandDefinition("spawn", 3),
            new CommandDefinition("ban", 3),
            new CommandDefinition("tempban", 3),
            new CommandDefinition("search", 3),
            new CommandDefinition("npc", 3),
            new CommandDefinition("removenpcs", 3),
            new CommandDefinition("mynpcpos", 3),
            new CommandDefinition("cleardrops", 3),
            new CommandDefinition("clearshops", 3),
            new CommandDefinition("clearevents", 3),
            new CommandDefinition("permban", 3),
            new CommandDefinition("emote", 3),
            new CommandDefinition("proitem", 3),
            new CommandDefinition("addclones", 3),
            new CommandDefinition("removeclones", 3),
            new CommandDefinition("removeallclones", 3),
            new CommandDefinition("follow", 3),
            new CommandDefinition("pause", 3),
            new CommandDefinition("stance", 3),
            new CommandDefinition("killmonster", 3),
            new CommandDefinition("removeoid", 3),
            new CommandDefinition("gmtext", 3),
            new CommandDefinition("currentdate", 3),
            new CommandDefinition("maxmesos", 3),
            new CommandDefinition("fullcharge", 3),
            new CommandDefinition("youlose", 3),
            new CommandDefinition("resettrialcooldown", 3),
            new CommandDefinition("resetap", 3),
            new CommandDefinition("resetskills", 3),
            new CommandDefinition("resetdailyprize", 3),
            new CommandDefinition("sha1", 3),
            new CommandDefinition("sendmedamagepacket", 3),
            new CommandDefinition("levelpersongrad", 3),
            new CommandDefinition("cachecashequips", 3),
            new CommandDefinition("cleardropcache", 3),
            new CommandDefinition("invokemethod", 3),
            new CommandDefinition("clearpqs", 3),
            new CommandDefinition("registerpqmi", 3),
            new CommandDefinition("toggledpm", 3),
            new CommandDefinition("showdpm", 3),
            new CommandDefinition("toggletrackmissgodmode", 3),
            new CommandDefinition("registerevent", 3),
            new CommandDefinition("unregisterevent", 3),
            new CommandDefinition("warpoutofevent", 3),
            new CommandDefinition("resetpreeventmaps", 3),
            new CommandDefinition("giftvp", 3),
            new CommandDefinition("unregisterallpqmis", 3),
            new CommandDefinition("reloadpqmiscript", 3),
            new CommandDefinition("givedeathitems", 3),
            new CommandDefinition("stunall", 3),
            new CommandDefinition("muteall", 3),
            new CommandDefinition("unmuteall", 3),
            new CommandDefinition("togglemuteall", 3),
            new CommandDefinition("drop", 3),
            new CommandDefinition("makeeqp", 3),
            new CommandDefinition("makeeqpperson", 3),
            new CommandDefinition("killmob", 3),
            new CommandDefinition("killallanddrop", 3),
            new CommandDefinition("itemquantity", 3),
            new CommandDefinition("listinv", 3),
            new CommandDefinition("forcenpc", 3),
            new CommandDefinition("reverselookup", 3),
            new CommandDefinition("warpmy", 3),
            new CommandDefinition("hackcheck", 3),
            new CommandDefinition("clearquestcache", 3),
            new CommandDefinition("reloadallquests", 3)
        };
    }

    private boolean gainItem(MapleClient c, IItem item) {
        if (item.getQuantity() >= 0) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleInventoryType type = ii.getInventoryType(item.getItemId());
            if (
                type.equals(MapleInventoryType.EQUIP) &&
                !ii.isThrowingStar(item.getItemId()) &&
                !ii.isBullet(item.getItemId())
            ) {
                if (!c.getPlayer().getInventory(type).isFull()) {
                    MapleInventoryManipulator.addFromDrop(c, item, false);
                } else {
                    c.getPlayer().dropMessage(
                        "Your " +
                            type.name().toLowerCase() +
                            " inventory is full. " +
                            ii.getName(item.getItemId()) +
                            "(s) have been added to your unclaimed items list. Please make room in your " +
                            type.name().toLowerCase() +
                            " inventory, and then type @mapleadmin into chat to claim your items."
                    );
                    c.getPlayer().addUnclaimedItem(item);
                    return false;
                }
            } else if (MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), "")) {
                if (item.getItemId() >= 5000000 && item.getItemId() <= 5000100) {
                    if (item.getQuantity() > 1) {
                        item.setQuantity((short) 1);
                    }
                    int petId = MaplePet.createPet(item.getItemId());
                    MapleInventoryManipulator.addById(c, item.getItemId(), (short) 1, null, petId);
                } else {
                    MapleInventoryManipulator.addById(c, item.getItemId(), item.getQuantity());
                }
            } else {
                c.getPlayer().dropMessage(
                    "Your " +
                        type.name().toLowerCase() +
                        " inventory is full. " +
                        ii.getName(item.getItemId()) +
                        "(s) have been added to your unclaimed items list. Please make room in your " +
                        type.name().toLowerCase() +
                        " inventory, and then type @mapleadmin into chat to claim your items."
                );
                c.getPlayer().addUnclaimedItem(item);
                return false;
            }
        }

        return true;
    }

    /** Pure */
    public static float distance(final String s, final String t) {
        if (s.isEmpty()) return t.length();
        if (t.isEmpty()) return s.length();
        if (s.equals(t)) return 0.0f;

        final int tLength = t.length();
        final int sLength = s.length();

        float[] swap;
        float[] v0 = new float[tLength + 1];
        float[] v1 = new float[tLength + 1];

        // Initialize v0 (the previous row of distances).
        // This row is A[0][i]: edit distance for an empty s.
        // The distance is just the number of characters to delete from t.
        for (int i = 0; i < v0.length; ++i) {
            v0[i] = i;
        }

        for (int i = 0; i < sLength; ++i) {
            // First element of v1 is A[i+1][0].
            // Edit distance is delete (i+1) chars from s to match empty t.
            v1[0] = i + 1;

            for (int j = 0; j < tLength; ++j) {
                v1[j + 1] =
                    floatMin(
                        v1[j] + 1.0f,
                        v0[j + 1] + 1.0f,
                        v0[j] +
                            (s.charAt(i) == t.charAt(j)
                                ? 0.0f
                                : 1.0f)
                    );
            }

            swap = v0;
            v0 = v1;
            v1 = swap;
        }

        // Latest results was in v1 which was swapped with v0.
        return v0[tLength];
    }

    /** Pure */
    private static float floatMin(float... floats) {
        float min = floats[0];
        for (int i = 1; i < floats.length; ++i) {
            if (floats[i] < min) min = floats[i];
        }
        return min;
    }
}
