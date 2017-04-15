package net.sf.odinms.client.messages.commands;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.maps.SavedLocationType;

import java.util.LinkedHashMap;
import java.util.Map;

public class Extras implements Command {
    @Override
    public void execute(final MapleClient c, final MessageCallback mc, final String[] splitted) throws Exception {
        splitted[0] = splitted[0].toLowerCase();
        final MapleCharacter player = c.getPlayer();
        if (player.getClient().getChannelServer().extraCommands()) {
            switch (splitted[0]) {
                case "@cody":
                    NPCScriptManager.getInstance().start(c, 9200000);
                    break;
                case "@storage":
                    player.getStorage().sendStorage(c, 2080005);
                    break;
                case "@news":
                    NPCScriptManager.getInstance().start(c, 9040011);
                    break;
                case "@kin":
                    NPCScriptManager.getInstance().start(c, 9900000);
                    break;
                case "@nimakin":
                    NPCScriptManager.getInstance().start(c, 9900001);
                    break;
                case "@reward":
                    NPCScriptManager.getInstance().start(c, 2050019);
                    break;
                case "@reward1":
                    NPCScriptManager.getInstance().start(c, 2020004);
                    break;
                case "@fredrick":
                    NPCScriptManager.getInstance().start(c, 9030000);
                    break;
                case "@spinel":
                    NPCScriptManager.getInstance().start(c, 9000020);
                    break;
                case "@clan":
                    NPCScriptManager.getInstance().start(c, 9201061, "ClanNPC", null);
                    break;
                case "@banme":
                    player.ban(player.getName() + " banned him/her self.", false);
                    break;
                case "@goafk":
                    player.setChalkboard("I'm AFK! Drop me a message!");
                    break;
                case "@slime":
                    if (player.getMeso() >= 50000000) {
                        player.gainMeso(-50000000);
                        MapleInventoryManipulator.addById(c, 4001013, (byte) 1);
                    }
                    break;
                case "@go":
                    final Map<String, Integer> maps = new LinkedHashMap<>();
                    maps.put("fm", 910000000);
                    maps.put("henesys", 100000000);
                    maps.put("ellinia", 101000000);
                    maps.put("perion", 102000000);
                    maps.put("kerning", 103000000);
                    maps.put("lith", 104000000);
                    maps.put("sleepywood", 105040300);
                    maps.put("florina", 110000000);
                    maps.put("orbis", 200000000);
                    maps.put("happy", 209000000);
                    maps.put("elnath", 211000000);
                    maps.put("ludi", 220000000);
                    maps.put("aqua", 230000000);
                    maps.put("leafre", 240000000);
                    maps.put("mulung", 250000000);
                    maps.put("herb", 251000000);
                    maps.put("omega", 221000000);
                    maps.put("korean", 222000000);
                    maps.put("nlc", 600000000);
                    maps.put("excavation", 990000000);
                    maps.put("mushmom", 100000005);
                    maps.put("griffey", 240020101);
                    maps.put("manon", 240020401);
                    maps.put("horseman", 682000001);
                    maps.put("balrog", 105090900);
                    maps.put("showa", 801000000);
                    maps.put("guild", 200000301);
                    maps.put("shrine", 800000000);
                    maps.put("skelegon", 104040001);
                    maps.put("mall", 910000022);
                    if (splitted.length != 2) {
                        final StringBuilder builder = new StringBuilder("Syntax: @go <mapname>");
                        final int i = 0;
                        for (final String mapss : maps.keySet()) {
                            if (i % 10 == 0) { // 10 maps per line
                                mc.dropMessage(builder.toString());
                            } else {
                                builder.append(mapss).append(", ");
                            }
                        }
                        mc.dropMessage(builder.toString());
                    } else if (maps.containsKey(splitted[1])) {
                        final int map = maps.get(splitted[1]);
                        if (map == 910000000) {
                            player.saveLocation(SavedLocationType.FREE_MARKET);
                        }
                        player.changeMap(map);
                        mc.dropMessage("Please feel free to suggest any more locations");
                    } else {
                        mc.dropMessage("I could not find the map that you requested, go get an eye test.");
                    }
                    maps.clear();
                    break;
                case "@buynx":
                    if (splitted.length != 2) {
                        mc.dropMessage("Syntax: @buynx <number>");
                        return;
                    }
                    final int nxamount;
                    try {
                        nxamount = Integer.parseInt(splitted[1]);
                    } catch (final NumberFormatException asd) {
                        return;
                    }
                    final int nxcost = 5000;
                    final int cost = nxamount * nxcost;
                    if (nxamount > 0 && nxamount < 420000) {
                        if (player.getMeso() >= cost) {
                            player.gainMeso(-cost, true, true, true);
                            player.modifyCSPoints(1, nxamount);
                            mc.dropMessage("You spent " + cost + " mesos. You have gained " + nxamount + " nx.");
                        } else {
                            mc.dropMessage("You don't have enough mesos. 1 NX is " + nxcost + " mesos.");
                        }
                    }
                    break;
            }
        } else {
            mc.dropMessage("Your server administrator has not enabled this command.");
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("cody", 0),
            new CommandDefinition("storage", 0),
            new CommandDefinition("news", 0),
            new CommandDefinition("kin", 0),
            new CommandDefinition("nimakin", 0),
            new CommandDefinition("reward", 0),
            new CommandDefinition("reward1", 0),
            new CommandDefinition("fredrick", 0),
            new CommandDefinition("spinel", 0),
            new CommandDefinition("clan", 0),
            new CommandDefinition("banme", 0),
            new CommandDefinition("goafk", 0),
            new CommandDefinition("slime", 0),
            new CommandDefinition("go", 0),
            new CommandDefinition("buynx", 0)
        };
    }
}
