package net.sf.odinms.net.channel;

import net.sf.odinms.client.*;
import net.sf.odinms.client.BuddyList.BuddyAddResult;
import net.sf.odinms.client.BuddyList.BuddyOperation;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.ByteArrayMaplePacket;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.*;
import net.sf.odinms.net.world.guild.MapleGuildSummary;
import net.sf.odinms.net.world.remote.CheaterData;
import net.sf.odinms.server.ShutdownServer;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.CollectionUtil;
import net.sf.odinms.tools.MaplePacketCreator;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class ChannelWorldInterfaceImpl extends UnicastRemoteObject implements ChannelWorldInterface {
    private static final long serialVersionUID = 7815256899088644192L;
    private ChannelServer server;

    public ChannelWorldInterfaceImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public ChannelWorldInterfaceImpl(final ChannelServer server) throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        this.server = server;
    }

    public void setChannelId(final int id) throws RemoteException {
        server.setChannel(id);
    }

    public int getChannelId() throws RemoteException {
        return server.getChannel();
    }

    public String getIP() throws RemoteException {
        return server.getIP();
    }

    public void broadcastMessage(final String sender, final byte[] message) throws RemoteException {
        final MaplePacket packet = new ByteArrayMaplePacket(message);
        server.broadcastPacket(packet);
    }

    public void whisper(final String sender, final String target, final int channel, final String message) throws RemoteException {
        if (isConnected(target)) {
            server.getPlayerStorage().getCharacterByName(target).getClient().getSession().write(
                MaplePacketCreator.getWhisper(sender, channel, message)
            );
        }
    }

    public boolean isConnected(final String charName) throws RemoteException {
        return server.getPlayerStorage().getCharacterByName(charName) != null;
    }

    public MapleCharacter getPlayer(final String charName) throws RemoteException {
        return server.getPlayerStorage().getCharacterByName(charName);
    }

    public void shutdown(final int time) throws RemoteException {
        server.broadcastPacket(
            MaplePacketCreator.serverNotice(
                0,
                "The world will be shut down in " +
                    (time / 60000) +
                    " minutes, please log off safely"
            )
        );
        TimerManager.getInstance().schedule(new ShutdownServer(server.getChannel()), time);
    }

    public int getConnected() throws RemoteException {
        return server.getConnectedClients();
    }

    @Override
    public void loggedOff(final String name, final int characterId, final int channel, final int[] buddies) throws RemoteException {
        updateBuddies(characterId, channel, buddies, true);
    }

    @Override
    public void loggedOn(final String name, final int characterId, final int channel, final int[] buddies) throws RemoteException {
        updateBuddies(characterId, channel, buddies, false);
    }

    private void updateBuddies(final int characterId, final int channel, final int[] buddies, final boolean offline) {
        final IPlayerStorage playerStorage = server.getPlayerStorage();
        for (final int buddy : buddies) {
            final MapleCharacter chr = playerStorage.getCharacterById(buddy);
            if (chr != null) {
                final BuddylistEntry ble = chr.getBuddylist().get(characterId);
                if (ble != null && ble.isVisible()) {
                    final int mcChannel;
                    if (offline) {
                        ble.setChannel(-1);
                        mcChannel = -1;
                    } else {
                        ble.setChannel(channel);
                        mcChannel = channel - 1;
                    }
                    chr.getBuddylist().put(ble);
                    chr.getClient()
                       .getSession()
                       .write(MaplePacketCreator.updateBuddyChannel(ble.getCharacterId(), mcChannel));
                }
            }
        }
    }

    @Override
    public void updateParty(final MapleParty party,
                            final PartyOperation operation,
                            final MaplePartyCharacter target) throws RemoteException {
        for (final MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannel()) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    if (operation == PartyOperation.DISBAND) {
                        chr.setParty(null);
                    } else {
                        chr.setParty(party);
                    }
                    chr.getClient()
                       .getSession()
                       .write(
                           MaplePacketCreator.updateParty(
                               chr.getClient().getChannel(),
                               party,
                               operation,
                               target
                           )
                       );
                }
            }
        }
        switch (operation) {
            case LEAVE:
            case EXPEL:
                if (target.getChannel() == server.getChannel()) {
                    final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(target.getName());
                    if (chr != null) {
                        chr.getClient()
                           .getSession()
                           .write(
                               MaplePacketCreator.updateParty(
                                   chr.getClient().getChannel(),
                                   party,
                                   operation,
                                   target
                               )
                           );
                        chr.setParty(null);
                    }
                }
        }
    }

    @Override
    public void partyChat(final MapleParty party, final String chattext, final String namefrom) throws RemoteException {
        for (final MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannel() && !(partychar.getName().equals(namefrom))) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    chr.getClient().getSession().write(MaplePacketCreator.multiChat(namefrom, chattext, 1));
                }
            }
        }
    }

    public boolean isAvailable() throws RemoteException {
        return true;
    }

    public int getLocation(final String name) throws RemoteException {
        final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(name);
        if (chr != null) {
            return server.getPlayerStorage().getCharacterByName(name).getMapId();
        }
        return -1;
    }

    public List<CheaterData> getCheaters() throws RemoteException {
        final List<CheaterData> cheaters = new ArrayList<>();
        final List<MapleCharacter> allplayers = new ArrayList<>(server.getPlayerStorage().getAllCharacters());
        /*Collections.sort(allplayers, new Comparator<MapleCharacter>() {
        @Override
        public int compare(MapleCharacter o1, MapleCharacter o2) {
        int thisVal = o1.getCheatTracker().getPoints();
        int anotherVal = o2.getCheatTracker().getPoints();
        return (thisVal<anotherVal ? 1 : (thisVal==anotherVal ? 0 : -1));
        }
        });*/
        for (int x = allplayers.size() - 1; x >= 0; x--) {
            final MapleCharacter cheater = allplayers.get(x);
            if (cheater.getCheatTracker().getPoints() > 0) {
                cheaters.add(
                    new CheaterData(
                        cheater.getCheatTracker().getPoints(),
                        MapleCharacterUtil.makeMapleReadable(cheater.getName()) +
                            " (" +
                            cheater.getCheatTracker().getPoints() +
                            ") " +
                            cheater.getCheatTracker().getSummary()
                    )
                );
            }
        }
        Collections.sort(cheaters);
        return CollectionUtil.copyFirst(cheaters, 10);
    }

    @Override
    public BuddyAddResult requestBuddyAdd(final String addName, final int channelFrom, final int cidFrom, final String nameFrom) {
        final MapleCharacter addChar = server.getPlayerStorage().getCharacterByName(addName);
        if (addChar != null) {
            final BuddyList buddylist = addChar.getBuddylist();
            if (buddylist.isFull()) {
                return BuddyAddResult.BUDDYLIST_FULL;
            }
            if (!buddylist.contains(cidFrom)) {
                buddylist.addBuddyRequest(addChar.getClient(), cidFrom, nameFrom, channelFrom);
            } else {
                if (buddylist.containsVisible(cidFrom)) {
                    return BuddyAddResult.ALREADY_ON_LIST;
                }
            }
        }
        return BuddyAddResult.OK;
    }

    @Override
    public boolean isConnected(final int characterId) throws RemoteException {
        return server.getPlayerStorage().getCharacterById(characterId) != null;
    }

    @Override
    public void buddyChanged(final int cid, final int cidFrom, final String name, final int channel, final BuddyOperation operation) {
        final MapleCharacter addChar = server.getPlayerStorage().getCharacterById(cid);
        if (addChar != null) {
            final BuddyList buddylist = addChar.getBuddylist();
            switch (operation) {
                case ADDED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, cidFrom, channel, true));
                        addChar
                            .getClient()
                            .getSession()
                            .write(
                                MaplePacketCreator.updateBuddyChannel(cidFrom, channel - 1)
                            );
                    }
                    break;
                case DELETED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, cidFrom, -1, buddylist.get(cidFrom).isVisible()));
                        addChar.getClient().getSession().write(MaplePacketCreator.updateBuddyChannel(cidFrom, -1));
                    }
                    break;
            }
        }
    }

    @Override
    public void buddyChat(final int[] recipientCharacterIds,
                          final int cidFrom,
                          final String nameFrom,
                          final String chattext) throws RemoteException {
        final IPlayerStorage playerStorage = server.getPlayerStorage();
        for (final int characterId : recipientCharacterIds) {
            final MapleCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(cidFrom)) {
                    chr.getClient().getSession().write(MaplePacketCreator.multiChat(nameFrom, chattext, 0));
                }
            }
        }
    }

    @Override
    public int[] multiBuddyFind(final int charIdFrom, final int[] characterIds) throws RemoteException {
        final List<Integer> ret = new ArrayList<>(characterIds.length);
        final IPlayerStorage playerStorage = server.getPlayerStorage();
        for (final int characterId : characterIds) {
            final MapleCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(charIdFrom)) {
                    ret.add(characterId);
                }
            }
        }
        final int[] retArr = new int[ret.size()];
        int pos = 0;
        for (final Integer i : ret) {
            retArr[pos++] = i;
        }
        return retArr;
    }

    @Override
    public void sendPacket(final List<Integer> targetIds, final MaplePacket packet, final int exception) throws RemoteException {
        MapleCharacter c;
        for (final int i : targetIds) {
            if (i == exception) {
                continue;
            }
            c = server.getPlayerStorage().getCharacterById(i);
            if (c != null) {
                c.getClient().getSession().write(packet);
            }
        }
    }

    @Override
    public void setGuildAndRank(final List<Integer> cids, final int guildid, final int rank,
                                final int exception) throws RemoteException {
        for (final int cid : cids) {
            if (cid != exception) {
                setGuildAndRank(cid, guildid, rank);
            }
        }
    }

    @Override
    public void setGuildAndRank(final int cid, final int guildid, final int rank) throws RemoteException {
        final MapleCharacter mc = server.getPlayerStorage().getCharacterById(cid);
        if (mc == null) {
            //System.out.println("ERROR: cannot find player in given channel");
            return;
        }

        final boolean bDifferentGuild;
        if (guildid == -1 && rank == -1) {
            bDifferentGuild = true;
        } else {
            bDifferentGuild = guildid != mc.getGuildId();
            mc.setGuildId(guildid);
            mc.setGuildRank(rank);
            mc.saveGuildStatus();
        }

        if (bDifferentGuild) {
            mc.getMap().broadcastMessage(mc, MaplePacketCreator.removePlayerFromMap(cid), false);
            mc.getMap().broadcastMessage(mc, MaplePacketCreator.spawnPlayerMapobject(mc), false);
            final MaplePet[] pets = mc.getPets();
            for (int i = 0; i < 3; ++i) {
                if (pets[i] != null) {
                    mc.getMap().broadcastMessage(mc, MaplePacketCreator.showPet(mc, pets[i], false, false), false);
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public void setOfflineGuildStatus(final int guildid, final byte guildrank, final int cid) throws RemoteException {
        //final Logger log = LoggerFactory.getLogger(this.getClass());
        try {
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps =
                con.prepareStatement(
                    "UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?"
                );
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, cid);
            ps.execute();
            ps.close();
        } catch (final SQLException sqle) {
            System.err.println("SQLException: " + sqle.getLocalizedMessage());
        }
    }

    @Override
    public void reloadGuildCharacters() throws RemoteException {
        for (final MapleCharacter mc : server.getPlayerStorage().getAllCharacters()) {
            if (mc.getGuildId() > 0) {
                // Multiple world ops, but this method is ONLY used
                // in !clearguilds gm command, so it shouldn't be a problem
                server.getWorldInterface().setGuildMemberOnline(mc.getMGC(), true, server.getChannel());
                server.getWorldInterface().memberLevelJobUpdate(mc.getMGC());
            }
        }

        ChannelServer.getInstance(this.getChannelId()).reloadGuildSummary();
    }

    @Override
    public void changeEmblem(final int gid, final List<Integer> affectedPlayers, final MapleGuildSummary mgs) throws RemoteException {
        ChannelServer.getInstance(this.getChannelId()).updateGuildSummary(gid, mgs);
        sendPacket(
            affectedPlayers,
            MaplePacketCreator.guildEmblemChange(
                gid,
                mgs.getLogoBG(),
                mgs.getLogoBGColor(),
                mgs.getLogo(),
                mgs.getLogoColor()
            ),
            -1
        );
        setGuildAndRank(affectedPlayers, -1, -1, -1); // Respawn player
    }

    public void messengerInvite(final String sender,
                                final int messengerid,
                                final String target,
                                final int fromchannel) throws RemoteException {
        if (isConnected(target)) {
            final MapleMessenger messenger = server.getPlayerStorage().getCharacterByName(target).getMessenger();
            if (messenger == null) {
                server
                    .getPlayerStorage()
                    .getCharacterByName(target)
                    .getClient()
                    .getSession()
                    .write(
                        MaplePacketCreator.messengerInvite(
                            sender,
                            messengerid
                        )
                    );
                final MapleCharacter from =
                    ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(sender);
                from.getClient().getSession().write(MaplePacketCreator.messengerNote(target, 4, 1));
            } else {
                final MapleCharacter from =
                    ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(sender);
                from
                    .getClient()
                    .getSession()
                    .write(
                        MaplePacketCreator.messengerChat(
                            sender +
                                " : " +
                                target +
                                " is already using Maple Messenger."
                        )
                    );
            }
        }
    }

    public void addMessengerPlayer(final MapleMessenger messenger,
                                   final String namefrom,
                                   final int fromchannel,
                                   final int position) throws RemoteException {
        for (final MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel() && !(messengerchar.getName().equals(namefrom))) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    final MapleCharacter from =
                        ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                    chr.getClient()
                       .getSession()
                       .write(
                           MaplePacketCreator.addMessengerPlayer(
                               namefrom,
                               from,
                               position,
                               fromchannel - 1
                           )
                       );
                    from
                        .getClient()
                        .getSession()
                        .write(
                            MaplePacketCreator.addMessengerPlayer(
                                chr.getName(),
                                chr,
                                messengerchar.getPosition(),
                                messengerchar.getChannel() - 1
                            )
                        );
                }
            } else if (
                messengerchar.getChannel() == server.getChannel() &&
                messengerchar.getName().equals(namefrom)
            ) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().getSession().write(MaplePacketCreator.joinMessenger(messengerchar.getPosition()));
                }
            }
        }
    }

    public void removeMessengerPlayer(final MapleMessenger messenger, final int position) throws RemoteException {
        for (final MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel()) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().getSession().write(MaplePacketCreator.removeMessengerPlayer(position));
                }
            }
        }
    }

    public void messengerChat(final MapleMessenger messenger, final String chattext, final String namefrom) throws RemoteException {
        for (final MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel() && !(messengerchar.getName().equals(namefrom))) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().getSession().write(MaplePacketCreator.messengerChat(chattext));
                }
            }
        }
    }

    public void declineChat(final String target, final String namefrom) throws RemoteException {
        if (isConnected(target)) {
            final MapleMessenger messenger = server.getPlayerStorage().getCharacterByName(target).getMessenger();
            if (messenger != null) {
                server.getPlayerStorage().getCharacterByName(target).getClient().getSession().write(
                        MaplePacketCreator.messengerNote(namefrom, 5, 0));
            }
        }
    }

    public void updateMessenger(final MapleMessenger messenger,
                                final String namefrom,
                                final int position,
                                final int fromchannel) throws RemoteException {
        for (final MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel() && !(messengerchar.getName().equals(namefrom))) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    final MapleCharacter from =
                        ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                    chr.getClient()
                       .getSession()
                        .write(
                            MaplePacketCreator.updateMessengerPlayer(
                                namefrom,
                                from,
                                position,
                                fromchannel - 1
                            )
                        );
                }
            }
        }
    }

    public void broadcastGMMessage(final String sender, final byte[] message) throws RemoteException {
        final MaplePacket packet = new ByteArrayMaplePacket(message);
        server.broadcastGMPacket(packet);
    }

    public void broadcastSMega(final String sender, final byte[] message) throws RemoteException {
        final MaplePacket packet = new ByteArrayMaplePacket(message);
        server.broadcastSMega(packet);
    }

    public void broadcastToClan(final byte[] message, final int clan) throws RemoteException {
        final MaplePacket packet = new ByteArrayMaplePacket(message);
        server.broadcastToClan(packet, clan);
    }

    public int onlineClanMembers(final int clan) {
        return server.onlineClanMembers(clan);
    }
}
