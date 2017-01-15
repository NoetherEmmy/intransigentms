package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.PartyQuest;
import net.sf.odinms.net.world.*;
import net.sf.odinms.net.world.guild.MapleAlliance;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class PlayerLoggedinHandler extends AbstractMaplePacketHandler {
    @Override
    public boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, final MapleClient c) {
        int cid = slea.readInt();
        final MapleCharacter player;
        try {
            player = MapleCharacter.loadCharFromDB(cid, c, true);
            c.setPlayer(player);
        } catch (SQLException e) {
            System.out.println("Loading the char failed" + e);
            return;
        }
        c.setAccID(player.getAccountID());
        c.getSession().write(MaplePacketCreator.setGender(player));
        int state = c.getLoginState();
        boolean allowLogin = true;
        ChannelServer channelServer = c.getChannelServer();
        synchronized (this) {
            try {
                WorldChannelInterface worldInterface = channelServer.getWorldInterface();
                if (state == MapleClient.LOGIN_SERVER_TRANSITION) {
                    for (String charName : c.loadCharacterNames(c.getWorld())) {
                        int worldLoc = worldInterface.find(charName);
                        if (worldLoc > -1) {
                            ChannelServer cserv = ChannelServer.getInstance(worldLoc);
                            MapleCharacter dPlayer = cserv.getPlayerStorage().getCharacterByName(charName);
                            dPlayer.getMap().removePlayer(dPlayer);
                            cserv.removePlayer(dPlayer);
                            dPlayer.getClient().disconnect();
                            dPlayer.getClient().getSession().close();
                            System.out.println(player.getName() + ": Attempting to double login with: " + charName);
                            break;
                        }
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                channelServer.reconnectWorld();
                allowLogin = false;
            }
            if (state != MapleClient.LOGIN_SERVER_TRANSITION || !allowLogin) {
                c.setPlayer(null);
                c.getSession().close();
                return;
            }
            c.updateLoginState(MapleClient.LOGIN_LOGGEDIN);
        }
        ChannelServer cserv = ChannelServer.getInstance(c.getChannel());
        cserv.addPlayer(player);
        try {
            WorldChannelInterface wci = ChannelServer.getInstance(c.getChannel()).getWorldInterface();
            List<PlayerBuffValueHolder> buffs = wci.getBuffsFromStorage(cid);
            if (buffs != null) {
                c.getPlayer().silentGiveBuffs(buffs);
            }
            List<PlayerCoolDownValueHolder> cooldowns = wci.getCooldownsFromStorage(cid);
            if (cooldowns != null) {
                c.getPlayer().giveCoolDowns(cooldowns);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            c.getChannelServer().reconnectWorld();
        }
        if (player.isDead()) {
            player.permadeath();
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM cooldowns WHERE charid = ?");
            ps.setInt(1, c.getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getLong("length") + rs.getLong("StartTime") - System.currentTimeMillis() <= 0) {
                    continue;
                }
                c.getPlayer().giveCoolDowns(rs.getInt("SkillID"), rs.getLong("StartTime"), rs.getLong("length"));
            }
            ps = con.prepareStatement("DELETE FROM cooldowns WHERE charid = ?");
            ps.setInt(1, c.getPlayer().getId());
            ps.executeUpdate();
            rs.close();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
        c.getSession().write(MaplePacketCreator.getCharInfo(player));
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE RecieverId = ? and checked = 1");
            ps.setInt(1, c.getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                DueyHandler.reciveMsg(c, c.getPlayer().getId());
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
        if (player.isGM()) {
            SkillFactory.getSkill(9001000).getEffect(1).applyTo(player);
            SkillFactory.getSkill(9101004).getEffect(1).applyTo(player);
        }
        c.getSession().write(MaplePacketCreator.serverMessage(c.getChannelServer().getServerMessage()));
        player.getMap().addPlayer(player);
        try {
            Collection<BuddylistEntry> buddies = player.getBuddylist().getBuddies();
            int buddyIds[] = player.getBuddylist().getBuddyIds();
            cserv.getWorldInterface().loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
            if (player.getParty() != null) {
                channelServer.getWorldInterface().updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
            }
            CharacterIdChannelPair[] onlineBuddies = cserv.getWorldInterface().multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                BuddylistEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                player.getBuddylist().put(ble);
            }
            c.getSession().write(MaplePacketCreator.updateBuddylist(buddies));
            c.getPlayer().sendMacros();
            try {
                c.getPlayer().showNote();
            } catch (SQLException ignored) {
            }
            if (player.getGuildId() > 0) {
                c.getChannelServer().getWorldInterface().setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                int allianceId = player.getGuild().getAllianceId();
                if (allianceId > 0) {
                    MapleAlliance newAlliance = channelServer.getWorldInterface().getAlliance(allianceId);
                    if (newAlliance == null) {
                        newAlliance = MapleAlliance.loadAlliance(allianceId);
                        channelServer.getWorldInterface().addAlliance(allianceId, newAlliance);
                    }
                    c.getSession().write(MaplePacketCreator.getAllianceInfo(newAlliance));
                    c.getSession().write(MaplePacketCreator.getGuildAlliances(newAlliance, c));
                    c.getChannelServer().getWorldInterface().allianceMessage(allianceId, MaplePacketCreator.allianceMemberOnline(player, true), player.getId(), -1);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            channelServer.reconnectWorld();
        }
        player.updatePartyMemberHP();
        player.sendKeymap();
        for (MapleQuestStatus status : player.getStartedQuests()) {
            if (status.hasMobKills()) {
                c.getSession().write(MaplePacketCreator.updateQuestMobKills(status));
            }
        }
        CharacterNameAndId pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            player.getBuddylist().put(new BuddylistEntry(pendingBuddyRequest.getName(), pendingBuddyRequest.getId(), -1, false));
            c.getSession().write(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), pendingBuddyRequest.getName()));
        }
        if (player.getMapId() == 100) {
            NPCScriptManager.getInstance().start(c, 1061000);
        } else if (player.getMapId() >= 1000 && player.getMapId() <= 1006) {
            player.changeMap(player.getTrialReturnMap(), 0);
        } else if (player.getMapId() == 240060200) {
            player.changeMap(240040700, 0);
        } else if (player.getMapId() == 3000) {
            player.changeMap(player.getBossReturnMap(), 0);
        }

        if (player.getMap() != null) {
            boolean reconnect = false;
            if (player.getParty() != null && player.getMap().isPQMap()) {
                ChannelServer pqChannel =
                    ChannelServer
                        .getAllInstances()
                        .stream()
                        .filter(cs ->
                            cs.readPartyQuests()
                              .stream()
                              .anyMatch(pq ->
                                  pq.hasIdRegistered(player.getId())
                              )
                        )
                        .findAny()
                        .orElse(null);
                if (pqChannel != null) {
                    PartyQuest thePq =
                        pqChannel
                            .readPartyQuests()
                            .stream()
                            .filter(pq ->
                                pq.hasIdRegistered(player.getId())
                            )
                            .findAny()
                            .orElse(null);
                    if (thePq != null) {
                        thePq.playerReconnected(player);
                        reconnect = true;
                    }
                }
            }
            if (!reconnect && player.getMap().getPartyQuestInstance() != null) {
                if (player.getMap().isPQMap() && player.getParty() != null) {
                    if (player.getMapId() / 100 == player.getParty().getLeader().getMapid() / 100 && player.getMapId() != player.getParty().getLeader().getMapid()) {
                        player.changeMap(player.getParty().getLeader().getMapid(), 0);
                    } else {
                        player.changeMap(100000000, 0);
                    }
                    channelServer.readAllPqItems().forEach(id -> MapleInventoryManipulator.removeAllById(c, id, false));
                } else if (player.getMap().isPQMap()) {
                    player.changeMap(100000000, 0);
                    channelServer.readAllPqItems().forEach(id -> MapleInventoryManipulator.removeAllById(c, id, false));
                }
            }
        }
        player.checkMessenger();
        player.checkBerserk();
        if (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD) == null ||
            player.getBuffedValue(MapleBuffStat.MAGIC_GUARD) < 1) {
            player.setMagicGuard(false);
            player.setMagicGuardCancelTask(null);
        }
        final WorldServer ws = WorldServer.getInstance();
        ws.getEnergyChargeRetention(player.getId())
          .ifPresent(ec -> {
              player.setEnergyBar(ec);
              ws.removeEnergyChargeRetention(player.getId());
          });
        player.updatePastLifeExp();
        player.reactivateExpBonus();
        player.dropVoteTime();
        player.setInvincible(false);
        if (player.getZakDc()) {
            NPCScriptManager.getInstance().start(c, 2030013);
        }
        //
        c.getSession().write(MaplePacketCreator.setNPCScriptable(9000014, "Geanie"));
        c.getSession().write(MaplePacketCreator.setNPCScriptable(9010010, "Cassandra"));
        c.getSession().write(MaplePacketCreator.setNPCScriptable(2041017, "Ace of Hearts"));
        c.getSession().write(MaplePacketCreator.setNPCScriptable(9010000, "Maple Administrator"));
        c.getSession().write(MaplePacketCreator.setNPCScriptable(2051001, "Kay"));
        c.getSession().write(MaplePacketCreator.setNPCScriptable(9270030, "Ralph the wanderer"));
        //
    }
}
