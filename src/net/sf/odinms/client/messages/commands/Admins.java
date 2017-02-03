package net.sf.odinms.client.messages.commands;

import net.sf.odinms.client.*;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.*;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.handler.ChangeChannelHandler;
import net.sf.odinms.scripting.portal.PortalScriptManager;
import net.sf.odinms.scripting.reactor.ReactorScriptManager;
import net.sf.odinms.server.*;
import net.sf.odinms.server.life.*;
import net.sf.odinms.server.maps.*;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.StringUtil;
import net.sf.odinms.tools.performance.CPUSampler;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.sf.odinms.client.messages.CommandProcessor.getOptionalIntArg;

public class Admins implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception {
        splitted[0] = splitted[0].toLowerCase();
        final MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
        switch (splitted[0]) {
            case "!speakall": {
                String text = StringUtil.joinStringFrom(splitted, 1);
                for (MapleCharacter mch : player.getMap().getCharacters()) {
                    mch.getMap().broadcastMessage(MaplePacketCreator.getChatText(mch.getId(), text, false, 0));
                }
                break;
            }
            case "!dcall": {
                ChannelServer.getAllInstances()
                             .stream()
                             .flatMap(channel -> channel.getPlayerStorage().getAllCharacters().stream())
                             .filter(p -> p != player)
                             .collect(Collectors.toCollection(ArrayList::new))
                             .forEach(p -> {
                                 p.getClient().disconnect();
                                 p.getClient().getSession().close();
                             });
                break;
            }
            case "!killnear": {
                MapleMap map = player.getMap();
                List<MapleMapObject> players = map.getMapObjectsInRange(
                    player.getPosition(),
                    50000.0d,
                    Collections.singletonList(MapleMapObjectType.PLAYER)
                );
                for (MapleMapObject closeplayers : players) {
                    MapleCharacter playernear = (MapleCharacter) closeplayers;
                    if (playernear.isAlive() && playernear != player) {
                        playernear.setHp(0);
                        playernear.updateSingleStat(MapleStat.HP, 0);
                        playernear.dropMessage(5, "You were too close to a GM.");
                    }
                }
                break;
            }
            case "!packet":
                if (splitted.length > 1) {
                    c.getSession().write(MaplePacketCreator.sendPacket(StringUtil.joinStringFrom(splitted, 1)));
                } else {
                    mc.dropMessage("Please enter packet data");
                }
                break;
            case "!drop": {
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                int itemId = Integer.parseInt(splitted[1]);
                short quantity = (short) getOptionalIntArg(splitted, 2, 1);
                IItem toDrop;
                if (ii.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = ii.getEquipById(itemId);
                } else {
                    toDrop = new Item(itemId, (byte) 0, quantity);
                }
                player.getMap().spawnItemDrop(player, player, toDrop, player.getPosition(), true, true);
                break;
            }
            case "!startprofiling": {
                CPUSampler sampler = CPUSampler.getInstance();
                sampler.addIncluded("net.sf.odinms");
                sampler.start();
                break;
            }
            case "!stopprofiling": {
                CPUSampler sampler = CPUSampler.getInstance();
                try {
                    String filename = "odinprofile.txt";
                    if (splitted.length > 1) {
                        filename = splitted[1];
                    }
                    File file = new File(filename);
                    if (file.exists()) {
                        file.delete();
                    }
                    sampler.stop();
                    FileWriter fw = new FileWriter(file);
                    sampler.save(fw, 1, 10);
                    fw.close();
                } catch (IOException ignored) {
                }
                sampler.reset();
                break;
            }
            case "!reloadops":
                try {
                    ExternalCodeTableGetter.populateValues(SendPacketOpcode.getDefaultProperties(), SendPacketOpcode.values());
                    ExternalCodeTableGetter.populateValues(RecvPacketOpcode.getDefaultProperties(), RecvPacketOpcode.values());
                } catch (Exception ignored) {
                }
                PacketProcessor.getProcessor(PacketProcessor.Mode.CHANNELSERVER).reset(PacketProcessor.Mode.CHANNELSERVER);
                PacketProcessor.getProcessor(PacketProcessor.Mode.CHANNELSERVER).reset(PacketProcessor.Mode.CHANNELSERVER);
                break;
            case "!closemerchants": {
                mc.dropMessage("Closing and saving merchants, please wait...");
                for (ChannelServer channel : ChannelServer.getAllInstances()) {
                    for (MapleCharacter players : channel.getPlayerStorage().getAllCharacters()) {
                        players.getInteraction().closeShop(true);
                    }
                }
                mc.dropMessage("All merchants have been closed and saved.");
                break;
            }
            case "!shutdown": {
                int time = 60000;
                if (splitted.length > 1) {
                    time = Integer.parseInt(splitted[1]) * 60000;
                }
                CommandProcessor.forcePersisting();
                c.getChannelServer().shutdown(time);
                break;
            }
            case "!shutdownworld": {
                int time = 60000;
                if (splitted.length > 1) {
                    time = Integer.parseInt(splitted[1]) * 60000;
                }
                CommandProcessor.forcePersisting();
                c.getChannelServer().shutdownWorld(time);
                break;
            }
            case "!shutdownnow":
                CommandProcessor.forcePersisting();
                new ShutdownServer(c.getChannel()).run();
                break;
            case "!setrebirths": {
                int rebirths;
                try {
                    rebirths = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.setReborns(rebirths);
                } else {
                    mc.dropMessage("Player was not found");
                }
                break;
            }
            case "!mesoperson": {
                int mesos;
                try {
                    mesos = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.gainMeso(mesos, true, true, true);
                } else {
                    mc.dropMessage("Player was not found");
                }
                break;
            }
            case "!gmperson":
                if (splitted.length == 3) {
                    MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim != null) {
                        int level;
                        try {
                            level = Integer.parseInt(splitted[2]);
                        } catch (NumberFormatException nfe) {
                            return;
                        }
                        victim.setGM(level);
                        if (victim.isGM()) {
                            victim.dropMessage(5, "You now have level " + level + " GM powers.");
                        }
                    } else {
                        mc.dropMessage("The player " + splitted[1] + " is either offline or not in this channel");
                    }
                }
                break;
            case "!kill": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.setHp(0);
                    victim.setMp(0);
                    victim.updateSingleStat(MapleStat.HP, 0);
                    victim.updateSingleStat(MapleStat.MP, 0);
                } else {
                    mc.dropMessage("Player not found.");
                }
                break;
            }
            case "!jobperson": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                int job;
                try {
                    job = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                if (victim != null) {
                    victim.setJob(job);
                } else {
                    mc.dropMessage("Player not found");
                }
                break;
            }
            case "!spawndebug":
                player.getMap().spawnDebug(mc);
                break;
            case "!timerdebug":
                TimerManager.getInstance().dropDebugInfo(mc);
                break;
            case "!threads": {
                Thread[] threads = new Thread[Thread.activeCount()];
                Thread.enumerate(threads);
                String filter = "";
                if (splitted.length > 1) {
                    filter = splitted[1];
                }
                for (int i = 0; i < threads.length; ++i) {
                    String tstring = threads[i].toString();
                    if (tstring.toLowerCase().contains(filter.toLowerCase())) {
                        mc.dropMessage(i + ": " + tstring);
                    }
                }
                break;
            }
            case "!showtrace": {
                Thread[] threads = new Thread[Thread.activeCount()];
                Thread.enumerate(threads);
                Thread t = threads[Integer.parseInt(splitted[1])];
                mc.dropMessage(t + ":");
                for (StackTraceElement elem : t.getStackTrace()) {
                    mc.dropMessage(elem.toString());
                }

                break;
            }
            case "!shopitem":
                if (splitted.length < 5) {
                    mc.dropMessage("!shopitem <shopid> <itemid> <price> <position>");
                } else {
                    try {
                        Connection con = DatabaseConnection.getConnection();
                        PreparedStatement ps = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position) VALUES (" + Integer.parseInt(splitted[1]) + ", " + Integer.parseInt(splitted[2]) + ", " + Integer.parseInt(splitted[3]) + ", " + Integer.parseInt(splitted[4]) + ");");
                        ps.executeUpdate();
                        ps.close();
                        MapleShopFactory.getInstance().clear();
                        mc.dropMessage("Done adding shop item.");
                    } catch (SQLException e) {
                        mc.dropMessage("Something wrong happened.");
                    }
                }

                break;
            case "!pnpc": {
                int npcId = Integer.parseInt(splitted[1]);
                MapleNPC npc = MapleLifeFactory.getNPC(npcId);
                int xpos = player.getPosition().x;
                int ypos = player.getPosition().y;
                int fh = player.getMap().getFootholds().findBelow(player.getPosition()).getId();
                if (!npc.getName().equals("MISSINGNO")) {
                    npc.setPosition(player.getPosition());
                    npc.setCy(ypos);
                    npc.setRx0(xpos + 50);
                    npc.setRx1(xpos - 50);
                    npc.setFh(fh);
                    npc.setCustom(true);
                    try {
                        Connection con = DatabaseConnection.getConnection();
                        PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                        ps.setInt(1, npcId);
                        ps.setInt(2, 0);
                        ps.setInt(3, fh);
                        ps.setInt(4, ypos);
                        ps.setInt(4, ypos);
                        ps.setInt(5, xpos + 50);
                        ps.setInt(6, xpos - 50);
                        ps.setString(7, "n");
                        ps.setInt(8, xpos);
                        ps.setInt(9, ypos);
                        ps.setInt(10, player.getMapId());
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        mc.dropMessage("Failed to save NPC to the database");
                    }
                    player.getMap().addMapObject(npc);
                    player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                } else {
                    mc.dropMessage("You have entered an invalid Npc-Id");
                }
                break;
            }
            case "!toggleoffense":
                try {
                    CheatingOffense co = CheatingOffense.valueOf(splitted[1]);
                    co.setEnabled(!co.isEnabled());
                } catch (IllegalArgumentException iae) {
                    mc.dropMessage("Offense " + splitted[1] + " not found");
                }
                break;
            case "!tdrops":
                player.getMap().toggleDrops();
                break;
            case "!givemonsbuff": {
                int mask = 0;
                mask |= Integer.decode(splitted[1]);
                MobSkill skill = MobSkillFactory.getMobSkill(128, 1);
                c.getSession().write(MaplePacketCreator.applyMonsterStatusTest(Integer.valueOf(splitted[2]), mask, 0, skill));
                break;
            }
            case "!givemonstatus": {
                int mask = 0;
                mask |= Integer.decode(splitted[1]);
                c.getSession().write(MaplePacketCreator.applyMonsterStatusTest2(Integer.valueOf(splitted[2]), mask, 1000, Integer.valueOf(splitted[3])));
                break;
            }
            case "!sreactor":
                MapleReactorStats reactorSt = MapleReactorFactory.getReactor(Integer.parseInt(splitted[1]));
                MapleReactor reactor = new MapleReactor(reactorSt, Integer.parseInt(splitted[1]));
                reactor.setDelay(-1);
                reactor.setPosition(player.getPosition());
                player.getMap().spawnReactor(reactor);
                break;
            case "!hreactor":
                player.getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
                break;
            case "!lreactor": {
                MapleMap map = player.getMap();
                List<MapleMapObject> reactors = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, MapleMapObjectType.REACTOR);
                for (MapleMapObject reactorL : reactors) {
                    MapleReactor reactor2l = (MapleReactor) reactorL;
                    mc.dropMessage(
                        "Reactor: oid: " +
                            reactor2l.getObjectId() +
                            " reactor ID: " +
                            reactor2l.getId() +
                            " Position: " +
                            reactor2l.getPosition().x +
                            ", " +
                            reactor2l.getPosition().y +
                            " State: " +
                            reactor2l.getState()
                    );
                }
                break;
            }
            case "!dreactor": {
                MapleMap map = player.getMap();
                List<MapleMapObject> reactors = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, MapleMapObjectType.REACTOR);
                if (splitted[1].equalsIgnoreCase("all")) {
                    for (MapleMapObject reactorL : reactors) {
                        MapleReactor reactor2l = (MapleReactor) reactorL;
                        player.getMap().destroyReactor(reactor2l.getObjectId());
                    }
                } else {
                    player.getMap().destroyReactor(Integer.parseInt(splitted[1]));
                }
                break;
            }
            case "!writecommands":
                CommandProcessor.getInstance().writeCommandList();
                break;
            case "!saveall":
                for (ChannelServer chan : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : chan.getPlayerStorage().getAllCharacters()) {
                        chr.saveToDB(true, false);
                    }
                }
                mc.dropMessage("Save complete.");
                break;
            case "!getpw":
                MapleClient victimC = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]).getClient();
                if (!victimC.isGm()) {
                    mc.dropMessage("Username: " + victimC.getAccountName());
                    mc.dropMessage("Password: " + victimC.getAccountPass());
                }
                break;
            case "!notice": {
                int joinmod = 1;
                int range = -1;
                if (splitted[1].equalsIgnoreCase("m")) {
                    range = 0;
                } else if (splitted[1].equalsIgnoreCase("c")) {
                    range = 1;
                } else if (splitted[1].equalsIgnoreCase("w")) {
                    range = 2;
                }
                int tfrom = 2;
                int type;
                if (range == -1) {
                    range = 2;
                    tfrom = 1;
                }
                if (splitted[tfrom].equalsIgnoreCase("n")) {
                    type = 0;
                } else if (splitted[tfrom].equalsIgnoreCase("p")) {
                    type = 1;
                } else if (splitted[tfrom].equalsIgnoreCase("l")) {
                    type = 2;
                } else if (splitted[tfrom].equalsIgnoreCase("nv")) {
                    type = 5;
                } else if (splitted[tfrom].equalsIgnoreCase("v")) {
                    type = 5;
                } else if (splitted[tfrom].equalsIgnoreCase("b")) {
                    type = 6;
                } else {
                    type = 0;
                    joinmod = 0;
                }
                String prefix = "";
                if (splitted[tfrom].equalsIgnoreCase("nv")) {
                    prefix = "[Notice] ";
                }
                joinmod += tfrom;
                String outputMessage = StringUtil.joinStringFrom(splitted, joinmod);
                if (outputMessage.equalsIgnoreCase("!array")) {
                    outputMessage = c.getChannelServer().getArrayString();
                }
                MaplePacket packet = MaplePacketCreator.serverNotice(type, prefix + outputMessage);
                if (range == 0) {
                    player.getMap().broadcastMessage(packet);
                } else if (range == 1) {
                    ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
                } else {
                    try {
                        ChannelServer.getInstance(c.getChannel()).getWorldInterface().broadcastMessage(player.getName(), packet.getBytes());
                    } catch (RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                }
                break;
            }
            case "!strip": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.unequipEverything();
                    victim.dropMessage("You've been stripped by " + player.getName() + ".");
                } else {
                    player.dropMessage(6, "Player is not on.");
                }
                break;
            }
            case "!speak": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    String text = StringUtil.joinStringFrom(splitted, 2);
                    victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), text, false, 0));
                } else {
                    mc.dropMessage("Player not found");
                }
                break;
            }
            case "!changechannel": {
                int channel;
                if (splitted.length == 3) {
                    try {
                        channel = Integer.parseInt(splitted[2]);
                    } catch (NumberFormatException nfe) {
                        return;
                    }
                    if (channel <= ChannelServer.getAllInstances().size() || channel < 0) {
                        String name = splitted[1];
                        try {
                            int vchannel = c.getChannelServer().getWorldInterface().find(name);
                            if (vchannel > -1) {
                                ChannelServer pserv = ChannelServer.getInstance(vchannel);
                                MapleCharacter victim = pserv.getPlayerStorage().getCharacterByName(name);
                                ChangeChannelHandler.changeChannel(channel, victim.getClient());
                            } else {
                                mc.dropMessage("Player not found");
                            }
                        } catch (RemoteException re) {
                            c.getChannelServer().reconnectWorld();
                        }
                    } else {
                        mc.dropMessage("Channel not found.");
                    }
                } else {
                    try {
                        channel = Integer.parseInt(splitted[1]);
                    } catch (NumberFormatException nfe) {
                        return;
                    }
                    if (channel <= ChannelServer.getAllInstances().size() || channel < 0) {
                        ChangeChannelHandler.changeChannel(channel, c);
                    }
                }
                break;
            }
            case "!clearguilds":
                try {
                    mc.dropMessage("Attempting to reload all guilds... this may take a while...");
                    cserv.getWorldInterface().clearGuilds();
                    mc.dropMessage("Completed.");
                } catch (RemoteException re) {
                    mc.dropMessage("RemoteException occurred while attempting to reload guilds.");
                }
                break;
            case "!clearportalscripts":
                PortalScriptManager.getInstance().clearScripts();
                break;
            case "!clearreactordrops":
                ReactorScriptManager.getInstance().clearDrops();
                break;
            case "!monsterdebug": {
                MapleMap map = player.getMap();
                double range = Double.POSITIVE_INFINITY;
                if (splitted.length > 1) {
                    int irange = Integer.parseInt(splitted[1]);
                    if (splitted.length <= 2) {
                        range = irange * irange;
                    } else {
                        map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    }
                }
                List<MapleMapObject> monsters = map.getMapObjectsInRange(player.getPosition(), range, Collections.singletonList(MapleMapObjectType.MONSTER));
                for (MapleMapObject monstermo : monsters) {
                    MapleMonster monster = (MapleMonster) monstermo;
                    mc.dropMessage("Monster " + monster.toString());
                }
                break;
            }
            case "!itemperson": {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                int item;
                try {
                    item = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException nfe) {
                    return;
                }
                short quantity = (short) getOptionalIntArg(splitted, 3, 1);
                if (victim != null) {
                    MapleInventoryManipulator.addById(victim.getClient(), item, quantity);
                } else {
                    mc.dropMessage("Player not found");
                }
                break;
            }
            case "!setaccgm": {
                int accountid;
                Connection con = DatabaseConnection.getConnection();
                try {
                    PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
                    ps.setString(1, splitted[1]);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        accountid = rs.getInt("accountid");
                        ps.close();
                        ps = con.prepareStatement("UPDATE accounts SET gm = ? WHERE id = ?");
                        ps.setInt(1, 1);
                        ps.setInt(2, accountid);
                        ps.executeUpdate();
                    } else {
                        mc.dropMessage("Player was not found in the database.");
                    }
                    ps.close();
                    rs.close();
                } catch (SQLException ignored) {
                }
                break;
            }
            case "!servercheck":
                try {
                    cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(1, "Server check will commence soon. Please @save, and log off safely.").getBytes());
                } catch (RemoteException re) {
                    cserv.reconnectWorld();
                }
                break;
            case "!itemvac":
                List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.ITEM));
                for (MapleMapObject item : items) {
                    MapleMapItem mapItem = (MapleMapItem) item;
                    if (mapItem.getMeso() > 0) {
                        player.gainMeso(mapItem.getMeso(), true);
                    } else if (mapItem.getItem().getItemId() >= 5000000 && mapItem.getItem().getItemId() <= 5000100) {
                        int petId = MaplePet.createPet(mapItem.getItem().getItemId());
                        if (petId == -1) {
                            return;
                        }
                        MapleInventoryManipulator.addById(c, mapItem.getItem().getItemId(), mapItem.getItem().getQuantity(), null, petId);
                    } else {
                        MapleInventoryManipulator.addFromDrop(c, mapItem.getItem(), true);
                    }
                    mapItem.setPickedUp(true);
                    player.getMap().removeMapObject(item); // just incase ?
                    player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapItem.getObjectId(), 2, player.getId()), mapItem.getPosition());
                }
                break;
            case "!playernpc": {
                int scriptId = Integer.parseInt(splitted[2]);
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                int npcId;
                if (splitted.length != 3) {
                    mc.dropMessage("Pleaase use the correct syntax. !playernpc <char name> <script name>");
                } else if (scriptId < 9901000 || scriptId > 9901319) {
                    mc.dropMessage("Please enter a script name between 9901000 and 9901319");
                } else if (victim == null) {
                    mc.dropMessage("The character is not in this channel");
                } else {
                    try {
                        Connection con = DatabaseConnection.getConnection();
                        PreparedStatement ps = con.prepareStatement("SELECT * FROM playernpcs WHERE ScriptId = ?");
                        ps.setInt(1, scriptId);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            mc.dropMessage("The script id is already in use !");
                            rs.close();
                        } else {
                            rs.close();
                            ps = con.prepareStatement("INSERT INTO playernpcs (name, hair, face, skin, x, cy, map, ScriptId, Foothold, rx0, rx1, gender, dir) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                            ps.setString(1, victim.getName());
                            ps.setInt(2, victim.getHair());
                            ps.setInt(3, victim.getFace());
                            ps.setInt(4, victim.getSkinColor().getId());
                            ps.setInt(5, player.getPosition().x);
                            ps.setInt(6, player.getPosition().y);
                            ps.setInt(7, player.getMapId());
                            ps.setInt(8, scriptId);
                            ps.setInt(9, player.getMap().getFootholds().findBelow(player.getPosition()).getId());
                            ps.setInt(10, player.getPosition().x + 50); // I should really remove rx1 rx0. Useless.
                            ps.setInt(11, player.getPosition().x - 50);
                            ps.setInt(12, victim.getGender());
                            ps.setInt(13, player.isFacingLeft() ? 0 : 1);
                            ps.executeUpdate();
                            rs = ps.getGeneratedKeys();
                            rs.next();
                            npcId = rs.getInt(1);
                            ps.close();
                            ps = con.prepareStatement("INSERT INTO playernpcs_equip (NpcId, equipid, equippos) VALUES (?, ?, ?)");
                            ps.setInt(1, npcId);
                            for (IItem equip : victim.getInventory(MapleInventoryType.EQUIPPED)) {
                                ps.setInt(2, equip.getItemId());
                                ps.setInt(3, equip.getPosition());
                                ps.executeUpdate();
                            }
                            ps.close();
                            rs.close();

                            ps = con.prepareStatement("SELECT * FROM playernpcs WHERE ScriptId = ?");
                            ps.setInt(1, scriptId);
                            rs = ps.executeQuery();
                            rs.next();
                            PlayerNPCs pn = new PlayerNPCs(rs);
                            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                                MapleMap map = channel.getMapFactory().getMap(player.getMapId());
                                map.broadcastMessage(MaplePacketCreator.SpawnPlayerNPC(pn));
                                map.broadcastMessage(MaplePacketCreator.getPlayerNPC(pn));
                                map.addMapObject(pn);
                            }
                        }
                        ps.close();
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case "!removeplayernpcs": {
                for (ChannelServer channel : ChannelServer.getAllInstances()) {
                    for (MapleMapObject object : channel.getMapFactory().getMap(player.getMapId()).getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.PLAYER_NPC))) {
                        channel.getMapFactory().getMap(player.getMapId()).removeMapObject(object);
                    }
                }
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("DELETE FROM playernpcs WHERE map = ?");
                ps.setInt(1, player.getMapId());
                ps.executeUpdate();
                ps.close();
                break;
            }
            case "!pmob": {
                int npcId = Integer.parseInt(splitted[1]);
                int mobTime = Integer.parseInt(splitted[2]);
                int xpos = player.getPosition().x;
                int ypos = player.getPosition().y;
                int fh = player.getMap().getFootholds().findBelow(player.getPosition()).getId();
                if (splitted[2] == null) {
                    mobTime = 0;
                }
                MapleMonster mob = MapleLifeFactory.getMonster(npcId);
                if (mob != null && !mob.getName().equals("MISSINGNO")) {
                    mob.setPosition(player.getPosition());
                    mob.setCy(ypos);
                    mob.setRx0(xpos + 50);
                    mob.setRx1(xpos - 50);
                    mob.setFh(fh);
                    try {
                        Connection con = DatabaseConnection.getConnection();
                        PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid, mobtime ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                        ps.setInt(1, npcId);
                        ps.setInt(2, 0);
                        ps.setInt(3, fh);
                        ps.setInt(4, ypos);
                        ps.setInt(5, xpos + 50);
                        ps.setInt(6, xpos - 50);
                        ps.setString(7, "m");
                        ps.setInt(8, xpos);
                        ps.setInt(9, ypos);
                        ps.setInt(10, player.getMapId());
                        ps.setInt(11, mobTime);
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        mc.dropMessage("Failed to save MOB to the database");
                    }
                    player.getMap().addMonsterSpawn(mob, mobTime);
                } else {
                    mc.dropMessage("You have entered an invalid Npc-Id");
                }
                break;
            }
            case "!reinitdiscord":
                mc.dropMessage("Reinitialization of discord bot " + (DeathBot.reInit() ? "succeeded" : "failed") + ".");
                break;
            case "!disposediscord":
                DeathBot.getInstance().dispose();
                mc.dropMessage("Discord bot disposed.");
                break;
            case "!clearquests":
                MapleQuest.clearQuests();
                mc.dropMessage("Quest cache has bene cleared.");
                break;
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("speakall", 4),
            new CommandDefinition("dcall", 4),
            new CommandDefinition("killnear", 4),
            new CommandDefinition("packet", 4),
            new CommandDefinition("drop", 4),
            new CommandDefinition("startprofiling", 4),
            new CommandDefinition("stopprofiling", 4),
            new CommandDefinition("reloadops", 4),
            new CommandDefinition("closemerchants", 4),
            new CommandDefinition("shutdown", 4),
            new CommandDefinition("shutdownworld", 4),
            new CommandDefinition("shutdownnow", 4),
            new CommandDefinition("setrebirths", 4),
            new CommandDefinition("mesoperson", 4),
            new CommandDefinition("gmperson", 4),
            new CommandDefinition("kill", 4),
            new CommandDefinition("jobperson", 4),
            new CommandDefinition("spawndebug", 4),
            new CommandDefinition("timerdebug", 4),
            new CommandDefinition("threads", 4),
            new CommandDefinition("showtrace", 4),
            new CommandDefinition("toggleoffense", 4),
            new CommandDefinition("tdrops", 4),
            new CommandDefinition("givemonsbuff", 4),
            new CommandDefinition("givemonstatus", 4),
            new CommandDefinition("sreactor", 4),
            new CommandDefinition("hreactor", 4),
            new CommandDefinition("dreactor", 4),
            new CommandDefinition("lreactor", 4),
            new CommandDefinition("writecommands", 4),
            new CommandDefinition("saveall", 4),
            new CommandDefinition("getpw", 4),
            new CommandDefinition("notice", 4),
            new CommandDefinition("speak", 4),
            new CommandDefinition("changechannel", 4),
            new CommandDefinition("clearguilds", 4),
            new CommandDefinition("clearportalscripts", 4),
            new CommandDefinition("shopitem", 4),
            new CommandDefinition("clearreactordrops", 4),
            new CommandDefinition("monsterdebug", 4),
            new CommandDefinition("itemperson", 4),
            new CommandDefinition("setaccgm", 4),
            new CommandDefinition("pnpc", 4),
            new CommandDefinition("strip", 4),
            new CommandDefinition("servercheck", 4),
            new CommandDefinition("itemvac", 4),
            new CommandDefinition("playernpc", 4),
            new CommandDefinition("removeplayernpcs", 4),
            new CommandDefinition("pmob", 4),
            new CommandDefinition("reinitdiscord", 4),
            new CommandDefinition("disposediscord", 4),
            new CommandDefinition("clearquests", 4)
        };
    }
}
