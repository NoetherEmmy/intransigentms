package net.sf.odinms.server;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.WhisperMapleClientMessageCallback;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.StringUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class PublicChatHandler {
    private static final Map<Integer, Integer> playerHolder = new LinkedHashMap<>();

    private static void addPlayer(final MapleCharacter chr) {
        playerHolder.put(chr.getId(), chr.getClient().getChannel());
        for (final int chrIds : playerHolder.keySet()) {
            final MapleCharacter chrs =
                ChannelServer.getInstance(
                    playerHolder.get(chrIds)
                )
                .getPlayerStorage()
                .getCharacterById(chrIds);
            if (chrs == null) {
                playerHolder.remove(chrIds);
                continue;
            }
            chrs
                .getClient()
                .getSession()
                .write(
                    MaplePacketCreator.multiChat(
                        "",
                        chr.getName() +
                            " has been added to the chat.",
                        2
                    )
                );
        }
    }

    private static void removePlayer(final MapleCharacter chr) {
        if (playerHolder.containsKey(chr.getId())) {
            playerHolder.remove(chr.getId());
            for (final int chrIds : playerHolder.keySet()) {
                final MapleCharacter chrs =
                    ChannelServer.getInstance(
                        playerHolder.get(chrIds)
                    )
                    .getPlayerStorage()
                    .getCharacterById(chrIds);
                if (chrs == null) {
                    playerHolder.remove(chrIds);
                    continue;
                }
                chrs
                    .getClient()
                    .getSession()
                    .write(
                        MaplePacketCreator.multiChat(
                            "",
                            chr.getName() +
                                " has left the chat.",
                            2
                        )
                    );
            }
        }
    }

    private static void sendMessage(final MapleCharacter chr, final String message) {
        if (playerHolder.containsKey(chr.getId())) {
            for (final int chrIds : playerHolder.keySet()) {
                final MapleCharacter chrs =
                    ChannelServer.getInstance(
                        playerHolder.get(chrIds)
                    )
                    .getPlayerStorage()
                    .getCharacterById(chrIds);
                if (chrs == null) { // Changing channels?
                    playerHolder.remove(chrIds);
                    continue;
                }
                chrs
                    .getClient()
                    .getSession()
                    .write(
                        MaplePacketCreator.multiChat(
                            chr.getName(),
                            message,
                            3
                        )
                    );
            }
        }
    }

    public static Map<Integer, Integer> getPublicChatHolder() {
        return playerHolder;
    }

    public static boolean doChat(final MapleClient c, final String text) {
        final MapleCharacter player = c.getPlayer();
        final WhisperMapleClientMessageCallback mc = new WhisperMapleClientMessageCallback("ChatBot", c);
        if (text.charAt(0) == '`') { // ` is much easier than typing ~
            final String[] splitted = text.substring(1).split(" ");
            if (splitted[0].equalsIgnoreCase("connect")) {
                if (playerHolder.containsKey(player.getId())) {
                    mc.dropMessage("You are already in the chat channel.");
                } else {
                    addPlayer(player);
                }
            } else if (splitted[0].equalsIgnoreCase("leave")) {
                if (playerHolder.containsKey(player.getId())) {
                    removePlayer(player);
                } else {
                    mc.dropMessage("You are not in a chat room yet.");
                }
            } else if (splitted[0].equalsIgnoreCase("online")) {
                if (playerHolder.containsKey(player.getId())) {
                    mc.dropMessage(
                        "There are currently " +
                            playerHolder.size() +
                            " connected people in this channel."
                    );
                } else {
                    mc.dropMessage("Please make sure you're in a chat room first.");
                }
            } else if (splitted[0].equalsIgnoreCase("whoson")) {
                StringBuilder sb = new StringBuilder();
                c.getSession().write(MaplePacketCreator.multiChat("", "Current people in the chat:", 3));
                c.getSession().write(MaplePacketCreator.serverNotice(6, ""));
                int i = 0;
                if (playerHolder.isEmpty()) {
                    c.getSession()
                     .write(
                         MaplePacketCreator.multiChat(
                             "",
                             "No one is in this chat, unfortunately.",
                             3
                         )
                     );
                } else {
                    for (final int chrIds : playerHolder.keySet()) {
                        final MapleCharacter chrs =
                            ChannelServer.getInstance(
                                playerHolder.get(chrIds)
                            )
                            .getPlayerStorage()
                            .getCharacterById(chrIds);
                        if (chrs == null) { // Changing channels
                            playerHolder.remove(chrIds);
                            continue;
                        }
                        if (sb.length() > 70) {
                            c.getSession().write(MaplePacketCreator.multiChat("", sb.toString(), 3));
                            sb = new StringBuilder();
                        }
                        sb.append(chrs.getName()).append("      ");
                        i++;
                    }
                    c.getSession().write(MaplePacketCreator.multiChat("", sb.toString(), 3));
                    c.getSession().write(MaplePacketCreator.multiChat("", i + " total connected.", 3));
                }
            } else if (playerHolder.containsKey(player.getId())) {
                final String message = StringUtil.joinStringFrom(splitted, 0).trim();
                sendMessage(player, message);
            } else {
                mc.dropMessage("I don't understand what you just said.");
            }
        } else {
            return false;
        }
        return true;
    }
}
