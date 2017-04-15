package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleMessenger;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;

public class MessengerHandler extends AbstractMaplePacketHandler {
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final String input;
        final byte mode = slea.readByte();
        final MapleCharacter player = c.getPlayer();
        final WorldChannelInterface wci = ChannelServer.getInstance(c.getChannel()).getWorldInterface();
        MapleMessenger messenger = player.getMessenger();
        switch (mode) {
            case 0x00: // Open
                if (messenger == null) {
                    final int messengerid = slea.readInt();
                    if (messengerid == 0) { // Create
                        try {
                            final MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
                            messenger = wci.createMessenger(messengerplayer);
                            player.setMessenger(messenger);
                            player.setMessengerPosition(0);
                        } catch (final RemoteException e) {
                            c.getChannelServer().reconnectWorld();
                        }
                    } else { // Join
                        try {
                            messenger = wci.getMessenger(messengerid);
                            if (messenger != null) {
                                final int position = messenger.getLowestPosition();
                                final MapleMessengerCharacter messengerplayer =
                                    new MapleMessengerCharacter(player, position);
                                if (messenger.getMembers().size() < 3) {
                                    player.setMessenger(messenger);
                                    player.setMessengerPosition(position);
                                    wci.joinMessenger(
                                        messenger.getId(),
                                        messengerplayer,
                                        player.getName(),
                                        messengerplayer.getChannel()
                                    );
                                }
                            }
                        } catch (final RemoteException re) {
                            c.getChannelServer().reconnectWorld();
                        }
                    }
                }
                break;
            case 0x02: // Exit
                if (messenger != null) {
                    final MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
                    try {
                        wci.leaveMessenger(messenger.getId(), messengerplayer);
                    } catch (final RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                    player.setMessenger(null);
                    player.setMessengerPosition(4);
                }
                break;
            case 0x03: // Invite
                if (messenger.getMembers().size() < 3) {
                    input = slea.readMapleAsciiString();
                    final MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(input);
                    if (target != null) {
                        if (target.getMessenger() == null) {
                            target
                                .getClient()
                                .getSession()
                                .write(
                                    MaplePacketCreator.messengerInvite(
                                        c.getPlayer().getName(),
                                        messenger.getId()
                                    )
                                );
                            c.getSession().write(MaplePacketCreator.messengerNote(input, 4, 1));
                        } else {
                            c.getSession()
                             .write(
                                 MaplePacketCreator.messengerChat(
                                     player.getName() +
                                         " : " +
                                         input +
                                         " is already using Maple Messenger"
                                 )
                             );
                        }
                    } else {
                        try {
                            if (ChannelServer.getInstance(c.getChannel()).getWorldInterface().isConnected(input)) {
                                ChannelServer
                                    .getInstance(c.getChannel())
                                    .getWorldInterface()
                                    .messengerInvite(
                                        c.getPlayer().getName(),
                                        messenger.getId(),
                                        input,
                                        c.getChannel()
                                    );
                            } else {
                                c.getSession().write(MaplePacketCreator.messengerNote(input, 4, 0));
                            }
                        } catch (final RemoteException e) {
                            c.getChannelServer().reconnectWorld();
                        }
                    }
                } else {
                    c.getSession()
                     .write(
                         MaplePacketCreator.messengerChat(
                             player.getName() +
                                 " : You cannot have more than 3 people in the Maple Messenger"
                         )
                     );
                }
                break;
            case 0x05: // Decline
                final String targeted = slea.readMapleAsciiString();
                final MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(targeted);
                if (target != null) {
                    if (target.getMessenger() != null) {
                        target
                            .getClient()
                            .getSession()
                            .write(
                                MaplePacketCreator.messengerNote(
                                    player.getName(),
                                    5,
                                    0
                                )
                            );
                    }
                } else {
                    try {
                        wci.declineChat(targeted, player.getName());
                    } catch (final RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                }
                break;
            case 0x06: // Message
                if (messenger != null) {
                    final MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
                    input = slea.readMapleAsciiString();
                    try {
                        wci.messengerChat(messenger.getId(), input, messengerplayer.getName());
                    } catch (final RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                }
                break;
        }
    }
}
