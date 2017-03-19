package net.sf.odinms.server;

/*
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
*/

public final class DeathBot {
    /*
    private static DeathBot instance;
    private String token;
    private JDA jda;
    private boolean on = true;
    private static List<String> quotes;

    private DeathBot() {}

    public static synchronized DeathBot getInstance() {
        if (instance == null) {
            instance = new DeathBot();
            instance.init();
        }
        return instance;
    }

    public static synchronized boolean reInit() {
        if (instance != null) {
            return instance.init();
        }
        instance = new DeathBot();
        return instance.init();
    }

    private synchronized boolean init() {
        on = true;
        try {
            if (token == null) {
                token = Files.readAllLines(
                    Paths.get("discordbottoken.txt"),
                    StandardCharsets.UTF_8
                ).get(0);
                System.out.println(token);
            }
            if (quotes == null) {
                quotes = Files.readAllLines(
                    Paths.get("quotes.txt"),
                    StandardCharsets.UTF_8
                );
            }
            if (jda == null) {
                jda =
                    new JDABuilder(AccountType.BOT)
                    .setToken(token)
                    .setAudioEnabled(false)
                    .addListener(new DeathBotListener())
                    .buildBlocking();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public synchronized void toggle() {
        on = !on;
    }

    public boolean isOn() {
        return on;
    }

    public boolean announceDeath(MapleCharacter p, int deathMapId) {
        final StringBuilder msg = new StringBuilder();
        List<Integer> life = p.getLastPastLife().orElse(null);
        if (life == null) {
            System.err.println("Could not retrieve past life.");
            return false;
        }
        if (life.get(0) < 6) {
            return true;
        }

        MapleMonster mob = MapleLifeFactory.getMonster(life.get(2));
        String causeofdeath;
        if (mob == null) {
            causeofdeath = "by their own hand";
        } else {
            causeofdeath = mob.getName();
            if (
                causeofdeath.charAt(0) == 'A' ||
                causeofdeath.charAt(0) == 'E' ||
                causeofdeath.charAt(0) == 'I' ||
                causeofdeath.charAt(0) == 'O' ||
                causeofdeath.charAt(0) == 'U'
            ) {
                causeofdeath = "at the hands of an " + causeofdeath;
            } else {
                causeofdeath = "at the hands of a " + causeofdeath;
            }
        }

        msg.append(p.getName())
           .append(", level ")
           .append(life.get(0))
           .append(' ')
           .append(MapleJob.getJobName(life.get(1)))
           .append(", has just perished ")
           .append(causeofdeath)
           .append(". May the gods let their soul rest until eternity.\n\n")
           .append(p.getName())
           .append("'s last words: \"")
           .append(quotes.get((int) (Math.random() * quotes.size())))
           .append("\"\n\n:rip:");

        TextChannel graveyard = jda.getTextChannelById("267935436935004161");
        if (graveyard == null) {
            System.err.println("Getting text channel by ID returns null.");
            return false;
        }
        if (!graveyard.getName().equals("graveyard")) {
            System.err.println("Wrong text channel name.");
            return false;
        }
        graveyard.sendMessage(msg.toString()).queue();

        return true;
    }

    public void dispose() {
        try {
            token = null;
            quotes.clear();
            quotes = null;
            jda.shutdown();
            jda = null;
            instance = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class DeathBotListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            DeathBot deathBot = DeathBot.getInstance();
            Message message = event.getMessage();
            String msg = message.getContent();
            MessageChannel channel = event.getChannel();

            if (event.isFromType(ChannelType.TEXT) && channel.getName().equals("graveyard")) {
                //System.out.println("Channel: " + channel.getName() + ", msg: " + msg);
                if (msg.equals("!stop")) {
                    if (deathBot.isOn()) {
                        deathBot.toggle();
                        channel.sendMessage("Death feed stopped.").queue();
                    } else {
                        channel.sendMessage("Death feed is not currently active.").queue();
                    }
                } else if (msg.equals("!resume")) {
                    if (!deathBot.isOn()) {
                        deathBot.toggle();
                        channel.sendMessage("Death feed resumed.").queue();
                    } else {
                        channel.sendMessage("Death feed is already active.").queue();
                    }
                }
            }
        }
    }
    */
}
