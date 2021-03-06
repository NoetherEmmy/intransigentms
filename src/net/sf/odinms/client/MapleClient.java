package net.sf.odinms.client;

import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.database.DatabaseException;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.PlayerCoolDownValueHolder;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.scripting.npc.NPCConversationManager;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.scripting.quest.QuestActionManager;
import net.sf.odinms.scripting.quest.QuestScriptManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.PlayerInteraction.HiredMerchant;
import net.sf.odinms.server.PlayerInteraction.IPlayerInteractionManager;
import net.sf.odinms.server.PlayerInteraction.MaplePlayerShopItem;
import net.sf.odinms.server.PublicChatHandler;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.IPAddressTool;
import net.sf.odinms.tools.MapleAESOFB;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import org.apache.mina.common.IoSession;

import javax.script.ScriptEngine;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class MapleClient {
    public static final int
        LOGIN_NOTLOGGEDIN = 0,
        LOGIN_SERVER_TRANSITION = 1,
        LOGIN_LOGGEDIN = 2,
        LOGIN_WAITING = 3;
    public static final String CLIENT_KEY = "CLIENT";
    //private static final Logger log = LoggerFactory.getLogger(MapleClient.class);
    private final MapleAESOFB send;
    private final MapleAESOFB receive;
    private final IoSession session;
    private MapleCharacter player;
    private int channel = 1;
    private int accId = 1;
    private boolean loggedIn = false;
    private boolean serverTransition = false;
    private Calendar birthday;
    private Calendar tempban;
    private String accountName;
    private String accountPass;
    private int world;
    private long lastPong;
    private boolean gm = false;
    private byte greason = 1;
    private boolean guest;
    private final Map<Pair<MapleCharacter, Integer>, Integer> timesTalked = new HashMap<>(); // NPC ID, times
    private final Set<String> macs = new LinkedHashSet<>();
    private final Map<String, ScriptEngine> engines = new HashMap<>();
    private ScheduledFuture<?> idleTask;
    private int attemptedLogins;

    public MapleClient(final MapleAESOFB send, final MapleAESOFB receive, final IoSession session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
    }

    public MapleAESOFB getReceiveCrypto() {
        return receive;
    }

    public MapleAESOFB getSendCrypto() {
        return send;
    }

    public synchronized IoSession getSession() {
        return session;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void setPlayer(final MapleCharacter player) {
        this.player = player;
    }

    public void sendCharList(final int server) {
        this.session.write(MaplePacketCreator.getCharList(this, server));
    }

    public List<MapleCharacter> loadCharacters(final int serverId) {
        final List<MapleCharacter> chars = new ArrayList<>();
        for (final CharNameAndId cni : loadCharactersInternal(serverId)) {
            try {
                chars.add(MapleCharacter.loadCharFromDB(cni.id, this, false));
            } catch (final SQLException e) {
                System.err.println("Loading characters failed");
                e.printStackTrace();
            }
        }
        return chars;
    }

    public List<String> loadCharacterNames(final int serverId) {
        return
            loadCharactersInternal(serverId)
                .stream()
                .map(cni -> cni.name)
                .collect(Collectors.toList());
    }

    private List<CharNameAndId> loadCharactersInternal(final int serverId) {
        final Connection con = DatabaseConnection.getConnection();
        final PreparedStatement ps;
        final List<CharNameAndId> chars = new ArrayList<>();
        try {
            ps = con.prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ?");
            ps.setInt(1, accId);
            ps.setInt(2, serverId);
            final ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
            }
            rs.close();
            ps.close();
        } catch (final SQLException e) {
            System.err.println("THROW");
            e.printStackTrace();
        }
        return chars;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    private Calendar getTempBanCalendar(final ResultSet rs) throws SQLException {
        final Calendar lTempban = Calendar.getInstance();
        final long banExpiryInMillis = rs.getLong("tempban");
        if (banExpiryInMillis == 0) {
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        final Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }
        lTempban.setTimeInMillis(0);
        return lTempban;
    }

    public Calendar getTempBanCalendar() {
        return tempban;
    }

    public byte getBanReason() {
        return greason;
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        try {
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')");
            ps.setString(1, session.getRemoteAddress().toString());
            final ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) ret = true;
            rs.close();
            ps.close();
        } catch (final SQLException ex) {
            System.err.println("Error checking ip bans");
            ex.printStackTrace();
        }
        return ret;
    }

    public boolean hasBannedMac() {
        if (macs.isEmpty()) return false;
        boolean ret = false;
        int i;
        try {
            final Connection con = DatabaseConnection.getConnection();
            final StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM macbans WHERE mac IN (");
            for (i = 0; i < macs.size(); ++i) {
                sql.append('?');
                if (i != macs.size() - 1) sql.append(", ");
            }
            sql.append(')');
            final PreparedStatement ps = con.prepareStatement(sql.toString());
            i = 0;
            for (final String mac : macs) {
                i++;
                ps.setString(i, mac);
            }
            final ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) ret = true;
            rs.close();
            ps.close();
        } catch (final SQLException ex) {
            System.err.println("Error checking mac bans. ");
            ex.printStackTrace();
        }
        return ret;
    }

    private void loadMacsIfNescessary() throws SQLException {
        if (macs.isEmpty()) {
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps = con.prepareStatement("SELECT macs FROM accounts WHERE id = ?");
            ps.setInt(1, accId);
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                final String[] macData = rs.getString("macs").split(", ");
                for (final String mac : macData) {
                    if (!mac.equals("")) macs.add(mac);
                }
            } else {
                throw new RuntimeException("No valid account associated with this client.");
            }
            rs.close();
            ps.close();
        }
    }

    public void banMacs() {
        final Connection con = DatabaseConnection.getConnection();
        try {
            loadMacsIfNescessary();
            final List<String> filtered = new ArrayList<>();
            PreparedStatement ps = con.prepareStatement("SELECT filter FROM macfilters");
            final ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                filtered.add(rs.getString("filter"));
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)");
            for (final String mac : macs) {
                if (filtered.stream().noneMatch(mac::matches)) {
                    ps.setString(1, mac);
                    try {
                        ps.executeUpdate();
                    } catch (final SQLException ignored) {
                    }
                }
            }
            ps.close();
        } catch (final SQLException e) {
            System.err.println("Error banning MACs:");
            e.printStackTrace();
        }
    }

    public int finishLogin(final boolean success) {
        if (success) {
            synchronized (MapleClient.class) {
                if (getLoginState() > LOGIN_NOTLOGGEDIN && getLoginState() != LOGIN_WAITING) {
                    loggedIn = false;
                    return 7;
                }
            }
            updateLoginState(LOGIN_LOGGEDIN);
            try {
                final Connection con = DatabaseConnection.getConnection();
                final PreparedStatement ps = con.prepareStatement("UPDATE accounts SET LastLoginInMilliseconds = ? WHERE id = ?");
                ps.setLong(1, System.currentTimeMillis());
                ps.setInt(2, this.accId);
                ps.executeUpdate();
                ps.close();
            } catch (final SQLException se) {
                se.printStackTrace();
            }
            return 0;
        } else {
            return 10;
        }
    }

    public int login(final String login, final String pwd, final boolean ipMacBanned) {
        int loginok = 5;
        attemptedLogins++;
        if (attemptedLogins > 5) {
            session.close();
        }
        final Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, password, salt, tempban, banned, gm, macs, lastknownip, greason FROM accounts WHERE name = ?");
            ps.setString(1, login);
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                final int banned = rs.getInt("banned");
                accId = rs.getInt("id");
                gm = rs.getInt("gm") > 0;
                final String passhash = rs.getString("password");
                final String salt = rs.getString("salt");
                greason = rs.getByte("greason");
                tempban = getTempBanCalendar(rs);
                if ((banned == 0 && !ipMacBanned) || banned == -1) {
                    final PreparedStatement ips = con.prepareStatement("INSERT INTO iplog (accountid, ip) VALUES (?, ?)");
                    ips.setInt(1, accId);
                    final String sockAddr = session.getRemoteAddress().toString();
                    ips.setString(2, sockAddr.substring(1, sockAddr.lastIndexOf(':')));
                    ips.executeUpdate();
                    ips.close();
                }
                if (!rs.getString("lastknownip").equals(session.getRemoteAddress().toString())) {
                    final PreparedStatement lkip = con.prepareStatement("UPDATE accounts SET lastknownip = ? WHERE id = ?");
                    final String sockAddr = session.getRemoteAddress().toString();
                    lkip.setString(1, sockAddr.substring(1, sockAddr.lastIndexOf(':')));
                    lkip.setInt(2, accId);
                    lkip.executeUpdate();
                    lkip.close();
                }
                ps.close();
                if (LoginServer.getInstance().isServerCheck() && !gm) {
                    return 7;
                } else if (banned == 1) {
                    loginok = 3;
                } else {
                    if (banned == -1) {
                        unban();
                    }
                    if (getLoginState() > LOGIN_NOTLOGGEDIN) {
                        loggedIn = false;
                        loginok = 7;
                        if (pwd.equalsIgnoreCase("fixme")) {
                            try {
                                ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE name = ?");
                                ps.setString(1, login);
                                ps.executeUpdate();
                                ps.close();
                            } catch (final SQLException ignored) {
                            }
                        }
                    } else {
                        if (passhash.equals(pwd)) {
                            loginok = 0;
                        } else {
                            final boolean updatePasswordHash = false;
                            if (LoginCrypto.isLegacyPassword(passhash) && LoginCrypto.checkPassword(pwd, passhash)) {
                                loginok = 0;
                                //updatePasswordHash = true;
                            } else if (salt == null && LoginCrypto.checkSha1Hash(passhash, pwd)) {
                                loginok = 0;
                                //updatePasswordHash = true;
                            } else if (LoginCrypto.checkSaltedSha512Hash(passhash, pwd, salt)) {
                                loginok = 0;
                            } else {
                                loggedIn = false;
                                loginok = 4;
                            }
                            if (updatePasswordHash) {
                                try (PreparedStatement pss = con.prepareStatement("UPDATE `accounts` SET `password` = ?, `salt` = ? WHERE id = ?")) {
                                    final String newSalt = LoginCrypto.makeSalt();
                                    pss.setString(1, LoginCrypto.makeSaltedSha512Hash(pwd, newSalt));
                                    pss.setString(2, newSalt);
                                    pss.setInt(3, accId);
                                    pss.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }
            rs.close();
            ps.close();
        } catch (final SQLException e) {
            System.err.println("ERROR");
            e.printStackTrace();
        }
        return loginok;
    }

    public static String getChannelServerIPFromSubnet(final String clientIPAddress, final int channel) {
        final long ipAddress = IPAddressTool.dottedQuadToLong(clientIPAddress);
        final Properties subnetInfo = LoginServer.getInstance().getSubnetInfo();

        if (subnetInfo.contains("net.sf.odinms.net.login.subnetcount")) {
            final int subnetCount = Integer.parseInt(subnetInfo.getProperty("net.sf.odinms.net.login.subnetcount"));
            for (int i = 0; i < subnetCount; ++i) {
                final String[] connectionInfo = subnetInfo.getProperty("net.sf.odinms.net.login.subnet." + i).split(":");
                final long subnet = IPAddressTool.dottedQuadToLong(connectionInfo[0]);
                final long channelIP = IPAddressTool.dottedQuadToLong(connectionInfo[1]);
                final int channelNumber = Integer.parseInt(connectionInfo[2]);

                if ((ipAddress & subnet) == (channelIP & subnet) && channel == channelNumber) {
                    return connectionInfo[1];
                }
            }
        }

        return "0.0.0.0";
    }

    private void unban() {
        int i;
        try {
            final Connection con = DatabaseConnection.getConnection();
            loadMacsIfNescessary();
            final StringBuilder sql = new StringBuilder("DELETE FROM macbans WHERE mac IN (");
            for (i = 0; i < macs.size(); ++i) {
                sql.append('?');
                if (i != macs.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(')');
            PreparedStatement ps = con.prepareStatement(sql.toString());
            i = 0;
            for (final String mac : macs) {
                i++;
                ps.setString(i, mac);
            }
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("DELETE FROM ipbans WHERE ip LIKE CONCAT(?, '%')");
            ps.setString(1, getSession().getRemoteAddress().toString().split(":")[0]);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("UPDATE accounts SET banned = 0 WHERE id = ?");
            ps.setInt(1, accId);
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException e) {
            System.err.println("Error while unbanning");
            e.printStackTrace();
        }
    }

    public void updateMacs(final String macData) {
        Collections.addAll(macs, macData.split(", "));
        final StringBuilder newMacData = new StringBuilder();
        final Iterator<String> iter = macs.iterator();
        while (iter.hasNext()) {
            final String cur = iter.next();
            newMacData.append(cur);
            if (iter.hasNext()) {
                newMacData.append(", ");
            }
        }
        final Connection con = DatabaseConnection.getConnection();
        try {
            final PreparedStatement ps = con.prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?");
            ps.setString(1, newMacData.toString());
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException e) {
            System.err.println("Error saving MACs");
            e.printStackTrace();
        }
    }

    public void setAccID(final int id) {
        this.accId = id;
    }

    public int getAccID() {
        return this.accId;
    }

    public void updateLoginState(final int newstate) {
        final Connection con = DatabaseConnection.getConnection();
        try {
            final PreparedStatement ps =
                con.prepareStatement(
                    "UPDATE accounts SET loggedin = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?"
                );
            ps.setInt(1, newstate);
            ps.setInt(2, this.accId);
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException e) {
            System.err.println("ERROR");
            e.printStackTrace();
        }
        if (newstate == LOGIN_NOTLOGGEDIN) {
            loggedIn = false;
            serverTransition = false;
        } else if (newstate == LOGIN_WAITING) {
            loggedIn = false;
            serverTransition = false;
        } else {
            serverTransition = (newstate == LOGIN_SERVER_TRANSITION);
            loggedIn = !serverTransition;
        }
    }

    public int getLoginState() {
        final Connection con = DatabaseConnection.getConnection();
        try {
            final PreparedStatement ps;
            ps = con.prepareStatement(
                "SELECT loggedin, lastlogin, UNIX_TIMESTAMP(birthday) as birthday FROM accounts WHERE id = ?"
            );
            ps.setInt(1, this.accId);
            final ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new DatabaseException("Everything sucks");
            }
            birthday = Calendar.getInstance();
            final long blubb = rs.getLong("birthday");
            if (blubb > 0) {
                birthday.setTimeInMillis(blubb * 1000);
            }
            int state = rs.getInt("loggedin");
            if (state == LOGIN_SERVER_TRANSITION) {
                final Timestamp ts = rs.getTimestamp("lastlogin");
                final long t = ts.getTime();
                final long now = System.currentTimeMillis();
                if (t + 30000 < now) { // Connecting to channel server timeout.
                    state = LOGIN_NOTLOGGEDIN;
                    updateLoginState(LOGIN_NOTLOGGEDIN);
                    if (guest) {
                        deleteAllCharacters();
                    }
                }
            }
            rs.close();
            ps.close();
            loggedIn = state == LOGIN_LOGGEDIN;
            return state;
        } catch (final SQLException e) {
            loggedIn = false;
            System.err.println("ERROR");
            e.printStackTrace();
            throw new DatabaseException("Everything sucks", e);
        }
    }

    public boolean checkBirthDate(final Calendar date) {
        if (date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR)) {
            if (date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH)) {
                if (date.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void disconnect() {
        final MapleCharacter chr = player;
        if (chr != null && loggedIn) {
            if (chr.getTrade() != null) {
                MapleTrade.cancelTrade(chr);
            }
            final Set<PlayerCoolDownValueHolder> cooldowns = chr.getAllCooldowns();
            if (cooldowns != null && !cooldowns.isEmpty()) {
                final Connection con = DatabaseConnection.getConnection();
                for (final PlayerCoolDownValueHolder cooling : cooldowns) {
                    if (cooling.length < 0) continue;
                    try {
                        final PreparedStatement ps =
                            con.prepareStatement(
                                "INSERT INTO cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)"
                            );
                        ps.setInt(1, chr.getId());
                        ps.setInt(2, cooling.skillId);
                        ps.setLong(3, cooling.startTime);
                        ps.setLong(4, cooling.length);
                        ps.executeUpdate();
                        ps.close();
                    } catch (final SQLException sqle) {
                        sqle.printStackTrace();
                    }
                }
            }
            chr.cancelAllBuffs();
            chr.cancelAllDebuffs();
            if (chr.getEventInstance() != null) {
                chr.getEventInstance().playerDisconnected(chr);
            }
            if (chr.getPartyQuest() != null) {
                chr.getPartyQuest().playerDisconnected(chr);
            }
            final IPlayerInteractionManager interaction = chr.getInteraction();
            if (interaction != null) {
                if (interaction.isOwner(chr)) {
                    if (interaction.getShopType() == 1) {
                        final HiredMerchant hm = (HiredMerchant) interaction;
                        hm.setOpen(true);
                    } else if (interaction.getShopType() == 2) {
                        for (final MaplePlayerShopItem items : interaction.getItems()) {
                            if (items.getBundles() > 0) {
                                final IItem item = items.getItem();
                                item.setQuantity((short) (items.getBundles() * item.getQuantity()));
                                MapleInventoryManipulator.addFromDrop(this, item);
                            }
                        }
                        interaction.removeAllVisitors(3, 1);
                        interaction.closeShop(true);
                    } else if (interaction.getShopType() == 3 || interaction.getShopType() == 4) {
                        interaction.removeAllVisitors(3, 1);
                        interaction.closeShop(true);
                    }
                } else {
                    interaction.removeVisitor(chr);
                }
            }
            if (PublicChatHandler.getPublicChatHolder().containsKey(chr.getId())) {
                PublicChatHandler.getPublicChatHolder().remove(chr.getId());
            }
            try {
                final WorldChannelInterface wci = getChannelServer().getWorldInterface();
                if (chr.getMessenger() != null) {
                    final MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
                    wci.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
                    chr.setMessenger(null);
                }
            } catch (final RemoteException e) {
                getChannelServer().reconnectWorld();
            }
            chr.unequipAllPets();
            chr.setMessenger(null);
            chr.getCheatTracker().dispose();
            if (!guest) {
                chr.saveToDB(true, true);
            }
            chr.getMap().removePlayer(chr);
            try {
                final WorldChannelInterface wci = getChannelServer().getWorldInterface();
                if (chr.getParty() != null) {
                    try {
                        final MaplePartyCharacter chrp = new MaplePartyCharacter(chr);
                        chrp.setOnline(false);
                        wci.updateParty(chr.getParty().getId(), PartyOperation.LOG_ONOFF, chrp);
                    } catch (final Exception e) {
                        System.err.println("Failed removing party character. Player already removed.");
                        e.printStackTrace();
                    }
                }
                if (!this.serverTransition && loggedIn) {
                    wci.loggedOff(chr.getName(), chr.getId(), channel, chr.getBuddylist().getBuddyIds());
                } else {
                    wci.loggedOn(chr.getName(), chr.getId(), channel, chr.getBuddylist().getBuddyIds());
                }
                if (chr.getGuildId() > 0) {
                    wci.setGuildMemberOnline(chr.getMGC(), false, -1);
                    final int allianceId = chr.getGuild().getAllianceId();
                    if (allianceId > 0) {
                        wci.allianceMessage(
                            allianceId,
                            MaplePacketCreator.allianceMemberOnline(chr, false),
                            chr.getId(),
                            -1
                        );
                    }
                }
            } catch (final RemoteException e) {
                getChannelServer().reconnectWorld();
            } catch (final Exception e) {
                System.err.println(getLogMessage(this, "ERROR"));
                e.printStackTrace();
            } finally {
                if (getChannelServer() != null) {
                    getChannelServer().removePlayer(chr);
                } else {
                    System.err.println(getLogMessage(this, "No channelserver associated to char {}", chr.getName()));
                }
            }
            if (guest) {
                deleteAllCharacters();
            }
        }
        if (!serverTransition && loggedIn) {
            updateLoginState(LOGIN_NOTLOGGEDIN);
        }
        final NPCScriptManager npcsm = NPCScriptManager.getInstance();
        if (npcsm != null) {
            npcsm.dispose(this);
        }
        if (QuestScriptManager.getInstance() != null) {
            QuestScriptManager.getInstance().dispose(this);
        }
    }

    public void dropDebugMessage(final MessageCallback mc) {
        final String builder =
                "Connected: " +
                getSession().isConnected() +
                " Closing: " +
                getSession().isClosing() +
                " ClientKeySet: " +
                (getSession().getAttribute(CLIENT_KEY) != null) +
                " loggedin: " +
                    loggedIn +
                " has char: " +
                (player != null);
        mc.dropMessage(builder);
    }

    public int getChannel() {
        return channel;
    }

    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(channel);
    }

    public boolean deleteCharacter(final int cid) {
        final Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, guildid, guildrank, name, allianceRank FROM characters WHERE id = ? AND accountid = ?");
            ps.setInt(1, cid);
            ps.setInt(2, accId);
            final ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return false;
            }
            if (rs.getInt("guildid") > 0) {
                final MapleGuildCharacter mgc = new MapleGuildCharacter(cid, 0, rs.getString("name"), -1, 0, rs.getInt("guildrank"), rs.getInt("guildid"), false, rs.getInt("allianceRank"));
                try {
                    LoginServer.getInstance().getWorldInterface().deleteGuildCharacter(mgc);
                } catch (final RemoteException re) {
                    getChannelServer().reconnectWorld();
                    rs.close();
                    ps.close();
                    return false;
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM characters WHERE id = ?");
            ps.setInt(1, cid);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (final SQLException e) {
            System.err.println("ERROR");
            e.printStackTrace();
        }
        return false;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public String getAccountPass() {
        return accountPass;
    }

    public void setAccountPass(final String pass) {
        this.accountPass = pass;
    }

    public void setChannel(final int channel) {
        this.channel = channel;
    }

    public int getWorld() {
        return world;
    }

    public void setWorld(final int world) {
        this.world = world;
    }

    public void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public void sendPing() {
        final long then = System.currentTimeMillis();
        getSession().write(MaplePacketCreator.getPing());
        TimerManager.getInstance().schedule(() -> {
            try {
                if (lastPong - then < 0) {
                    if (getSession().isConnected()) {
                        System.out.println("Auto DC : " + session.getRemoteAddress() + " : Ping timeout.");
                        getSession().close();
                    }
                }
            } catch (final NullPointerException ignored) {
            }
        }, 45L * 1000L); // 45 seconds
    }

    public static String getLogMessage(final MapleClient cfor, final String message) {
        return getLogMessage(cfor, message, new Object[0]);
    }

    public static String getLogMessage(final MapleCharacter cfor, final String message) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message);
    }

    public static String getLogMessage(final MapleCharacter cfor, final String message, final Object... parms) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message, parms);
    }

    public static String getLogMessage(final MapleClient cfor, final String message, final Object... parms) {
        final StringBuilder builder = new StringBuilder();
        if (cfor != null) {
            if (cfor.getPlayer() != null) {
                builder.append('<');
                builder.append(MapleCharacterUtil.makeMapleReadable(cfor.getPlayer().getName()));
                builder.append(" (cid: ");
                builder.append(cfor.getPlayer().getId());
                builder.append(")> ");
            }
            if (cfor.getAccountName() != null) {
                builder.append("(Account: ");
                builder.append(MapleCharacterUtil.makeMapleReadable(cfor.getAccountName()));
                builder.append(") ");
            }
        }
        builder.append(message);
        for (final Object parm : parms) {
            final int start = builder.indexOf("{}");
            builder.replace(start, start + 2, parm.toString());
        }
        return builder.toString();
    }

    public static int findAccIdForCharacterName(final String charName) {
        final Connection con = DatabaseConnection.getConnection();
        try {
            final PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, charName);
            final ResultSet rs = ps.executeQuery();
            int ret = -1;
            if (rs.next()) {
                ret = rs.getInt("accountid");
            }
            rs.close();
            ps.close();
            return ret;
        } catch (final SQLException e) {
            System.err.println("SQL THROW");
            e.printStackTrace();
        }
        return -1;
    }

    public static int getAccIdFromCID(final int id) {
        final Connection con = DatabaseConnection.getConnection();
        int ret = -1;
        try {
            final PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE id = ?");
            ps.setInt(1, id);
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = rs.getInt("accountid");
            }
            rs.close();
            ps.close();
            return ret;
        } catch (final SQLException e) {
            System.err.println("ERROR");
            e.printStackTrace();
        }
        return -1;
    }

    public Set<String> getMacs() {
        return Collections.unmodifiableSet(macs);
    }

    public boolean isGm() {
        return gm;
    }

    public void setScriptEngine(final String name, final ScriptEngine e) {
        engines.put(name, e);
    }

    public ScriptEngine getScriptEngine(final String name) {
        return engines.get(name);
    }

    public void removeScriptEngine(final String name) {
        engines.remove(name);
    }

    public ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public void setIdleTask(final ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    public NPCConversationManager getCM() {
        return NPCScriptManager.getInstance().getCM(this);
    }

    public QuestActionManager getQM() {
        return QuestScriptManager.getInstance().getQM(this);
    }

    public void setTimesTalked(final int n, final int t) {
        timesTalked.remove(new Pair<>(player, n));
        timesTalked.put(new Pair<>(player, n), t);
    }

    public int getTimesTalked(final int n) {
        if (timesTalked.get(new Pair<>(player, n)) == null) {
            setTimesTalked(n, 0);
        }
        return timesTalked.get(new Pair<>(player, n));
    }

    private static class CharNameAndId {
        public final String name;
        public final int id;
        public CharNameAndId(final String name, final int id) {
            super();
            this.name = name;
            this.id = id;
        }
    }

    public boolean isGuest() {
        return guest;
    }

    public void setGuest(final boolean set) {
        this.guest = set;
    }

    public void deleteAllCharacters() {
        final Connection con = DatabaseConnection.getConnection();
        try {
            int accountid = -1;
            PreparedStatement ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            ps.setString(1, accountName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                accountid = rs.getInt("id");
            }
            rs.close();
            ps.close();
            if (accountid == -1) {
                return;
            }
            ps = con.prepareStatement("SELECT id FROM characters WHERE accountid = ?");
            ps.setInt(1, accountid);
            rs = ps.executeQuery();
            while (rs.next()) {
                deleteCharacter(rs.getInt("id"));
            }
            rs.close();
            ps.close();
        } catch (final SQLException sqe) {
            sqe.printStackTrace();
        }
    }
}
