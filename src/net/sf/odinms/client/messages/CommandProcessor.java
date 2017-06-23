package net.sf.odinms.client.messages;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.ClassFinder;
import net.sf.odinms.tools.MockIOSession;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.StringUtil;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class CommandProcessor implements CommandProcessorMBean {
    private static final List<Pair<MapleCharacter, String>> gmlog =
        Collections.synchronizedList(
            new ArrayList<Pair<MapleCharacter, String>>()
        );
    private final Map<String, DefinitionCommandPair> commands = new LinkedHashMap<>();
    private static CommandProcessor instance = new CommandProcessor();
    private static final Runnable persister;

    static {
        persister = new PersistingTask();
        TimerManager.getInstance().register(persister, 62000);
    }

    private CommandProcessor() {
        instance = this;
        reloadCommands();
    }

    public static class PersistingTask implements Runnable {
        @Override
        public void run() {
            synchronized (gmlog) {
                final Connection con = DatabaseConnection.getConnection();
                try {
                    final PreparedStatement ps = con.prepareStatement("INSERT INTO gmlog (cid, command) VALUES (?, ?)");
                    final Iterator<Pair<MapleCharacter, String>> gmlogiter = gmlog.iterator();
                    while (gmlogiter.hasNext()) {
                        final Pair<MapleCharacter, String> logentry = gmlogiter.next();
                        ps.setInt(1, logentry.getLeft().getId());
                        ps.setString(2, logentry.getRight());
                        ps.executeUpdate();
                    }
                    ps.close();
                } catch (final SQLException e) {
                    System.err.println("Error persisting cheatlog: " + e);
                }
                gmlog.clear();
            }
        }
    }

    public static void registerMBean() {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            mBeanServer.registerMBean(
                instance,
                new ObjectName("net.sf.odinms.client.messages:name=CommandProcessor")
            );
        } catch (final Exception e) {
            System.err.println("Error registering CommandProcessor MBean: " + e);
        }
    }

    public static String joinAfterString(final String[] splitted, final String str) {
        for (int i = 1; i < splitted.length; ++i) {
            if (splitted[i].equalsIgnoreCase(str) && i + 1 < splitted.length) {
                return StringUtil.joinStringFrom(splitted, i + 1);
            }
        }
        return null;
    }

    public static int getOptionalIntArg(final String[] splitted, final int position, final int def) {
        if (splitted.length > position) {
            try {
                return Integer.parseInt(splitted[position]);
            } catch (final NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }

    public static String getNamedArg(final String[] splitted, final int startpos, final String name) {
        for (int i = startpos; i < splitted.length; ++i) {
            if (splitted[i].equalsIgnoreCase(name) && i + 1 < splitted.length) {
                return splitted[i + 1];
            }
        }
        return null;
    }

    public static Integer getNamedIntArg(final String[] splitted, final int startpos, final String name) {
        final String arg = getNamedArg(splitted, startpos, name);
        if (arg != null) {
            try {
                return Integer.parseInt(arg);
            } catch (final NumberFormatException ignored) {
            }
        }
        return null;
    }

    public static int getNamedIntArg(final String[] splitted, final int startpos, final String name, final int def) {
        final Integer ret = getNamedIntArg(splitted, startpos, name);
        if (ret == null) return def;
        return ret;
    }

    public static Double getNamedDoubleArg(final String[] splitted, final int startpos, final String name) {
        final String arg = getNamedArg(splitted, startpos, name);
        if (arg != null) {
            try {
                return Double.parseDouble(arg);
            } catch (final NumberFormatException ignored) {
            }
        }
        return null;
    }

    @Override
    public String processCommandJMX(final int cserver, final int mapid, final String command) {
        final ChannelServer cserv = ChannelServer.getInstance(cserver);
        if (cserv == null) {
            return "The specified channel server does not exist in this serverprocess.";
        }
        final MapleClient c = new MapleClient(null, null, new MockIOSession());
        final MapleCharacter chr = MapleCharacter.getDefault(c, 26023);
        c.setPlayer(chr);
        final MapleMap map = cserv.getMapFactory().getMap(mapid);
        if (map != null) {
            chr.setMap(map);
            SkillFactory.getSkill(9101004).getEffect(1).applyTo(chr);
            map.addPlayer(chr);
        }
        cserv.addPlayer(chr);
        final MessageCallback mc = new StringMessageCallback();
        try {
            processCommandInternal(c, mc, command);
        } finally {
            if (map != null) map.removePlayer(chr);
            cserv.removePlayer(chr);
        }
        return mc.toString();
    }

    public boolean processCommand(final MapleClient c, final String line) {
        return instance.processCommandInternal(c, new ServernoticeMapleClientMessageCallback(c), line);
    }

    public static void forcePersisting() {
        persister.run();
    }

    public static CommandProcessor getInstance() {
        return instance;
    }

    public void reloadCommands() {
        commands.clear();
        try {
            final ClassFinder classFinder = new ClassFinder();
            final String[] classes = classFinder.listClasses("net.sf.odinms.client.messages.commands", true);
            for (final String clazz : classes) {
                final Class<?> clasz = Class.forName(clazz);
                if (Command.class.isAssignableFrom(clasz)) {
                    try {
                        final Command newInstance = (Command) clasz.newInstance();
                        registerCommand(newInstance);
                    } catch (final Exception e) {
                        System.err.println("Error instantiating command class: " + e);
                    }
                }
            }
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void registerCommand(final Command command) {
        final CommandDefinition[] definition = command.getDefinition();
        for (final CommandDefinition def : definition) {
            commands.put(def.getCommand().toLowerCase(), new DefinitionCommandPair(command, def));
        }
    }

    public void dropHelp(final MapleCharacter chr, final MessageCallback mc, final int page) {
        final List<DefinitionCommandPair> allCommands = new ArrayList<>(commands.values());
        final int startEntry = (page - 1) * 20;
        mc.dropMessage("Commands Page: " + page);
        for (int i = startEntry; i < startEntry + 20 && i < allCommands.size(); ++i) {
            final CommandDefinition commandDefinition = allCommands.get(i).getDefinition();
            if (chr.getGMLevel() == commandDefinition.getRequiredLevel()) {
                dropHelpForDefinition(mc, commandDefinition);
            }
        }
    }

    public void writeCommandList() {
        try {
            final List<DefinitionCommandPair> allCommands = new ArrayList<>(commands.values());
            final FileWriter fw = new FileWriter(new File("Commands.txt"));
            final String lineSeparator = System.getProperty("line.separator");
            fw.flush();
            for (int x = 4; x >= 0; x--) {
                fw.write("---------------------------------");
                fw.write(lineSeparator);
                fw.write("          LEVEL " + x + " Commands.");
                fw.write(lineSeparator);
                fw.write("---------------------------------");
                fw.write(lineSeparator);
                fw.write(lineSeparator);
                for (int i = 0; i < allCommands.size(); ++i) {
                    if (allCommands.get(i).getDefinition().getRequiredLevel() == x) {
                        fw.write(allCommands.get(i).getDefinition().getCommand());
                        fw.write(lineSeparator);
                    }
                }
            }
            fw.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void dropHelpForDefinition(final MessageCallback mc, final CommandDefinition commandDefinition) {
        mc.dropMessage(commandDefinition.getCommand());
    }

    private boolean processCommandInternal(final MapleClient c, final MessageCallback mc, final String line) {
        switch (line.charAt(0)) {
            case '!': // GM commands
            case '@': // Player commands
            final String[] splitted = line.split(" ");
            if (splitted.length > 0) {
                final DefinitionCommandPair definitionCommandPair = commands.get(splitted[0].toLowerCase().substring(1));
                if (
                    definitionCommandPair != null &&
                    c.getPlayer().getGMLevel() >= definitionCommandPair.getDefinition().getRequiredLevel()
                ) {
                    if (definitionCommandPair.getDefinition().getRequiredLevel() >= 3) {
                        gmlog.add(new Pair<>(c.getPlayer(), line));
                        System.out.println("Notice: " + c.getPlayer().getName() + " used a command: " + line);
                    } else if (c.getPlayer().getCheatTracker().Spam(1000, 7)) {
                        c.getPlayer().dropMessage(1, "Please try again later.");
                        return true;
                    }
                    try {
                        definitionCommandPair.getCommand().execute(c, mc, splitted);
                    } catch (final Exception e) {
                        System.err.println("Command error, line " + line + ": " + e);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}

class DefinitionCommandPair {
    private final Command command;
    private final CommandDefinition definition;

    public DefinitionCommandPair(final Command command, final CommandDefinition definition) {
        super();
        this.command = command;
        this.definition = definition;
    }

    public Command getCommand() {
        return command;
    }

    public CommandDefinition getDefinition() {
        return definition;
    }
}
