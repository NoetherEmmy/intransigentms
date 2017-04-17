package net.sf.odinms.net.channel;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.MapleServerHandler;
import net.sf.odinms.net.PacketProcessor;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.mina.MapleCodecFactory;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.WorldServer;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.guild.MapleGuildSummary;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.net.world.remote.WorldRegistry;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.scripting.event.EventScriptManager;
import net.sf.odinms.server.*;
import net.sf.odinms.server.PlayerInteraction.HiredMerchant;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.server.maps.MapleMapFactory;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import javax.management.*;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelServer implements Runnable, ChannelServerMBean {
    private static int uniqueID = 1;
    private static Properties initialProp;
    private static WorldRegistry worldRegistry;
    private final PlayerStorage players = new PlayerStorage();
    private String serverMessage;
    private int expRate;
    private int mesoRate;
    private int dropRate;
    private int bossdropRate;
    private int petExpRate;
    private boolean dropUndroppables;
    private boolean moreThanOne;
    private int channel;
    private final String key;
    private Properties props = new Properties();
    private ChannelWorldInterface cwi;
    private WorldChannelInterface wci;
    private IoAcceptor acceptor;
    private String ip;
    private boolean shutdown = false;
    private boolean finishedShutdown = false;
    private String arrayString = "";
    private String serverName;
    private boolean AB;
    private boolean godlyItems;
    private boolean CS;
    private boolean MT;
    private boolean GMItems;
    private boolean extraCommands;
    private short itemStatMultiplier;
    private short godlyItemRate;
    private int PvPis;
    private final MapleMapFactory mapFactory;
    private EventScriptManager eventSM;
    private final Map<String, PartyQuest> partyQuests = new LinkedHashMap<>(3);
    private final Map<String, Set<Integer>> partyQuestItems = new LinkedHashMap<>(3);
    private static final Map<Integer, ChannelServer> instances = new HashMap<>();
    private static final Map<String, ChannelServer> pendingInstances = new HashMap<>();
    private final Map<Integer, MapleGuildSummary> gsStore = new LinkedHashMap<>();
    private Boolean worldReady = true;
    private final Map<MapleSquadType, MapleSquad> mapleSquads = new HashMap<>();
    private final ClanHolder clans = new ClanHolder();
    private final Collection<FakeCharacter> clones = new ArrayList<>();
    private int levelCap;
    private boolean multiLevel;
    private boolean trackMissGodmode = true;
    private int eventMap;

    private ChannelServer(final String key) {
        mapFactory =
                new MapleMapFactory(
                    MapleDataProviderFactory.getDataProvider(
                        new File(
                        System.getProperty(
                            WorldServer.WZPATH
                            ) +
                                "/Map.wz"
                        )
                    ), MapleDataProviderFactory.getDataProvider(
                        new File(
                            System.getProperty(
                                WorldServer.WZPATH
                            ) +
                                "/String.wz"
                        )
                    )
                );
        this.key = key;
    }

    public static WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }

    public void reconnectWorld() {
        // Check if the connection is really gone.
        try {
            wci.isAvailable();
        } catch (final RemoteException ex) {
            synchronized (worldReady) {
                worldReady = false;
            }
            synchronized (cwi) {
                synchronized (worldReady) {
                    if (worldReady) return;
                }
                System.err.println("Reconnecting to world server.");
                synchronized (wci) {
                    // Completely re-establish the rmi connection.
                    try {
                        initialProp = new Properties();
                        FileReader fr = new FileReader(System.getProperty("net.sf.odinms.channel.config"));
                        initialProp.load(fr);
                        fr.close();
                        final Registry registry = LocateRegistry.getRegistry(initialProp.getProperty("net.sf.odinms.world.host"),
                        Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
                        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
                        cwi = new ChannelWorldInterfaceImpl(this);
                        wci = worldRegistry.registerChannelServer(key, cwi);
                        props = wci.getGameProperties();
                        expRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.exp"));
                        mesoRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.meso"));
                        dropRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.drop"));
                        bossdropRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.bossdrop"));
                        petExpRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.petExp"));
                        serverMessage = props.getProperty("net.sf.odinms.world.serverMessage");
                        dropUndroppables = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.alldrop", "false"));
                        moreThanOne = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.morethanone", "false"));
                        serverName = props.getProperty("net.sf.odinms.world.serverName");
                        godlyItems = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.godlyItems", "false"));
                        itemStatMultiplier = Short.parseShort(props.getProperty("net.sf.odinms.world.itemStatMultiplier"));
                        godlyItemRate = Short.parseShort(props.getProperty("net.sf.odinms.world.godlyItemRate"));
                        multiLevel = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.multiLevel", "false"));
                        levelCap = Integer.parseInt(props.getProperty("net.sf.odinms.world.levelCap"));
                        AB = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.Autoban", "false"));
                        CS = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.CashShop", "false"));
                        MT = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.MTS", "false"));
                        GMItems = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.GMItems", "false"));
                        extraCommands = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.extraCommands", "false"));
                        PvPis = Integer.parseInt(props.getProperty("net.sf.odinms.world.PvPis", "4"));
                        final Properties dbProp = new Properties();
                        fr = new FileReader("db.properties");
                        dbProp.load(fr);
                        fr.close();
                        DatabaseConnection.setProps(dbProp);
                        DatabaseConnection.getConnection();
                        wci.serverReady();
                    } catch (final Exception e) {
                        System.err.println("Reconnecting failed: " + e);
                    }
                    worldReady = true;
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        try {
            cwi = new ChannelWorldInterfaceImpl(this);
            wci = worldRegistry.registerChannelServer(key, cwi);
            props = wci.getGameProperties();
            expRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.exp"));
            mesoRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.meso"));
            dropRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.drop"));
            bossdropRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.bossdrop"));
            petExpRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.petExp"));
            serverMessage = props.getProperty("net.sf.odinms.world.serverMessage");
            dropUndroppables = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.alldrop", "false"));
            moreThanOne = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.morethanone", "false"));
            eventSM = new EventScriptManager(this, props.getProperty("net.sf.odinms.channel.events").split(","));
            serverName = props.getProperty("net.sf.odinms.world.serverName");
            godlyItems = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.godlyItems", "false"));
            itemStatMultiplier = Short.parseShort(props.getProperty("net.sf.odinms.world.itemStatMultiplier"));
            godlyItemRate = Short.parseShort(props.getProperty("net.sf.odinms.world.godlyItemRate"));
            multiLevel = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.multiLevel", "false"));
            levelCap = Integer.parseInt(props.getProperty("net.sf.odinms.world.levelCap"));
            AB = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.Autoban", "false"));
            CS = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.CashShop", "false"));
            MT = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.MTS", "false"));
            GMItems = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.GMItems", "false"));
            extraCommands = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.extraCommands", "false"));
            PvPis = Integer.parseInt(props.getProperty("net.sf.odinms.world.PvPis", "4"));
            final Properties dbProp = new Properties();
            final FileReader fileReader = new FileReader("db.properties");
            dbProp.load(fileReader);
            fileReader.close();
            DatabaseConnection.setProps(dbProp);
            DatabaseConnection.getConnection();
            final Connection c = DatabaseConnection.getConnection();
            PreparedStatement ps;
            try {
                ps = c.prepareStatement("UPDATE accounts SET loggedin = 0");
                ps.executeUpdate();
                ps = c.prepareStatement("UPDATE characters SET HasMerchant = 0");
                ps.executeUpdate();
                ps.close();
            } catch (final SQLException ex) {
                System.err.println("Could not reset databases: " + ex);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        final int port = Integer.parseInt(props.getProperty("net.sf.odinms.channel.net.port"));
        ip = props.getProperty("net.sf.odinms.channel.net.interface") + ":" + port;
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        acceptor = new SocketAcceptor();
        final SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        //cfg.setThreadModel(ThreadModel.MANUAL); // *fingers crossed*, I hope the executor filter handles everything
        //executor = new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        //cfg.getFilterChain().addLast("executor", new ExecutorFilter(executor));
        cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
        //Item.loadInitialDataFromDB();
        final TimerManager tMan = TimerManager.getInstance();
        tMan.start();
        tMan.register(AutobanManager.getInstance(), 60000);
        try {
            final MapleServerHandler serverHandler =
                new MapleServerHandler(
                    PacketProcessor.getProcessor(
                        PacketProcessor.Mode.CHANNELSERVER
                    ),
                    channel
                );
            acceptor.bind(new InetSocketAddress(port), serverHandler, cfg);
            System.out.println("Channel " + channel + ": Listening on port: " + port);
            wci.serverReady();
            eventSM.init();
        } catch (final IOException e) {
            System.err.println("Binding to port " + port + " failed (ch: " + channel + ") " + e);
        }
    }

    public void shutdown() {
        shutdown = true;
        final List<CloseFuture> futures;
        final Collection<MapleCharacter> allchars = players.getAllCharacters();
        final MapleCharacter[] chrs = allchars.toArray(new MapleCharacter[allchars.size()]);
        for (final MapleCharacter chr : chrs) {
            if (chr.getTrade() != null) {
                MapleTrade.cancelTrade(chr);
            }
            if (chr.getEventInstance() != null) {
                chr.getEventInstance().playerDisconnected(chr);
            }
            if (chr.getPartyQuest() != null) {
                chr.getPartyQuest().playerDisconnected(chr);
            }
            if (!chr.getClient().isGuest()) {
                chr.saveToDB(true, true);
            }
            if (chr.getCheatTracker() != null) {
                chr.getCheatTracker().dispose();
            }
            removePlayer(chr);
        }
        futures =
            Arrays
                .stream(chrs)
                .map(chr -> chr.getClient().getSession().close())
                .collect(Collectors.toCollection(LinkedList::new));
        for (final CloseFuture future : futures) {
            future.join(500);
        }
        finishedShutdown = true;
        wci = null;
        cwi = null;
    }

    public void unbind() {
        acceptor.unbindAll();
    }

    public boolean hasFinishedShutdown() {
        return finishedShutdown;
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public static ChannelServer newInstance(final String key) throws InstanceAlreadyExistsException,
                                                               MBeanRegistrationException,
                                                               NotCompliantMBeanException,
                                                               MalformedObjectNameException {
        final ChannelServer instance = new ChannelServer(key);
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(
            instance,
            new ObjectName(
                "net.sf.odinms.net.channel:type=ChannelServer,name=ChannelServer" + uniqueID++
            )
        );
        pendingInstances.put(key, instance);
        return instance;
    }

    public static ChannelServer getInstance(final int channel) {
        return instances.get(channel);
    }

    public void addPlayer(final MapleCharacter chr) {
        players.registerPlayer(chr);
        if (chr.getClan() > -1) {
            clans.playerOnline(chr);
        }
    }

    public IPlayerStorage getPlayerStorage() {
        return players;
    }

    public void removePlayer(final MapleCharacter chr) {
        players.deregisterPlayer(chr);
        if (chr.getClan() > -1) {
            clans.deregisterPlayer(chr);
        }
    }

    public void addToClan(final MapleCharacter chr) {
        clans.registerPlayer(chr);
    }

    public ClanHolder getClanHolder() {
        return clans;
    }

    @Override
    public int getConnectedClients() {
        return players.getAllCharacters().size();
    }

    @Override
    public String getServerMessage() {
        return serverMessage;
    }

    @Override
    public void setServerMessage(final String newMessage) {
        serverMessage = newMessage;
        broadcastPacket(MaplePacketCreator.serverMessage(serverMessage));
    }

    public void broadcastPacket(final MaplePacket data) {
        for (final MapleCharacter chr : players.getAllCharacters()) {
            chr.getClient().getSession().write(data);
        }
    }

    public void setEventMap(final int eventMapId) {
        eventMap = eventMapId;
    }

    public int getEventMap() {
        return eventMap;
    }

    @Override
    public int getExpRate() {
        return expRate;
    }

    @Override
    public void setExpRate(final int expRate) {
        this.expRate = expRate;
    }

    public String getArrayString() {
        return arrayString;
    }

    public void setArrayString(final String newStr) {
        arrayString = newStr;
    }

    @Override
    public int getChannel() {
        return channel;
    }

    public void setChannel(final int channel) {
        if (pendingInstances.containsKey(key)) {
            pendingInstances.remove(key);
        }
        if (instances.containsKey(channel)) {
            instances.remove(channel);
        }
        instances.put(channel, this);
        this.channel = channel;
        mapFactory.setChannel(channel);
    }

    public static Collection<ChannelServer> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public String getIP() {
        return ip;
    }

    public String getIP(final int channel) {
        try {
            return getWorldInterface().getIP(channel);
        } catch (final RemoteException re) {
            System.err.println("Lost connection to world server: " + re);
            throw new RuntimeException("Lost connection to world server");
        }
    }

    public WorldChannelInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (final InterruptedException ignored) {
                }
            }
        }
        return wci;
    }

    public String getProperty(final String name) {
        return props.getProperty(name);
    }

    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public void shutdown(final int time) {
        broadcastPacket(
            MaplePacketCreator.serverNotice(
                0,
                "The world will be shut down in " +
                    (time / 60000) +
                    " minutes, please log off safely."
            )
        );
        TimerManager.getInstance().schedule(new ShutdownServer(channel), time);
    }

    @Override
    public void shutdownWorld(final int time) {
        try {
            getWorldInterface().shutdown(time);
        } catch (final RemoteException e) {
            reconnectWorld();
        }
    }

    @Override
    public int getLoadedMaps() {
        return mapFactory.getLoadedMaps();
    }

    public EventScriptManager getEventSM() {
        return eventSM;
    }

    public void reloadEvents() {
        eventSM.cancel();
        eventSM = new EventScriptManager(this, props.getProperty("net.sf.odinms.channel.events").split(","));
        eventSM.init();
    }

    public PartyQuest getPartyQuest(final String name) {
        return partyQuests.get(name);
    }

    public void registerPartyQuest(final PartyQuest pq) {
        partyQuests.put(pq.getName(), pq);
    }

    public void unregisterPartyQuest(final String name) {
        partyQuests.remove(name);
    }

    public void disposePartyQuests() {
        while (!partyQuests.isEmpty()) {
            partyQuests.values().stream().findAny().ifPresent(PartyQuest::dispose);
        }
    }

    public List<PartyQuest> readPartyQuests() {
        synchronized (partyQuests) {
            return new ArrayList<>(partyQuests.values());
        }
    }

    public void addPqItem(final PartyQuest pq, final int id) {
        addPqItem(pq.getName(), id);
    }

    public void addPqItem(final String pqName, final int id) {
        if (partyQuestItems.containsKey(pqName)) {
            partyQuestItems.get(pqName).add(id);
        } else {
            final Set<Integer> items = new LinkedHashSet<>(4, 0.8f);
            items.add(id);
            partyQuestItems.put(pqName, items);
        }
    }

    public Set<Integer> readPqItems(final String pqName) {
        if (partyQuestItems.containsKey(pqName)) {
            return new LinkedHashSet<>(partyQuestItems.get(pqName));
        } else {
            return Collections.emptySet();
        }
    }

    public Set<Integer> readAllPqItems() {
        return partyQuestItems.values().stream().reduce((union, s) -> {
            union.addAll(s);
            return union;
        }).orElseGet(Collections::emptySet);
    }

    @Override
    public int getMesoRate() {
        return mesoRate;
    }

    @Override
    public void setMesoRate(final int mesoRate) {
        this.mesoRate = mesoRate;
    }

    @Override
    public int getDropRate() {
        return dropRate;
    }

    @Override
    public void setDropRate(final int dropRate) {
        this.dropRate = dropRate;
    }

    @Override
    public int getBossDropRate() {
        return bossdropRate;
    }

    @Override
    public void setBossDropRate(final int bossdropRate) {
        this.bossdropRate = bossdropRate;
    }

    @Override
    public int getPetExpRate() {
        return petExpRate;
    }

    @Override
    public void setPetExpRate(final int petExpRate) {
        this.petExpRate = petExpRate;
    }

    public boolean allowUndroppablesDrop() {
        return dropUndroppables;
    }

    public boolean allowMoreThanOne() {
        return moreThanOne;
    }

    public boolean getTrackMissGodmode() {
        return trackMissGodmode;
    }

    public void setTrackMissGodmode(final boolean tmg) {
        trackMissGodmode = tmg;
    }

    public MapleGuild getGuild(final MapleGuildCharacter mgc) {
        final int gid = mgc.getGuildId();
        final MapleGuild g;
        try {
            g = this.getWorldInterface().getGuild(gid, mgc);
        } catch (final RemoteException re) {
            System.err.println("RemoteException while fetching MapleGuild: " + re);
            return null;
        }

        gsStore.putIfAbsent(gid, new MapleGuildSummary(g));

        return g;
    }

    public MapleGuildSummary getGuildSummary(final int gid) {
        if (gsStore.containsKey(gid)) {
            return gsStore.get(gid);
        } else {
            try {
                final MapleGuild g = this.getWorldInterface().getGuild(gid, null);
                if (g != null) {
                    gsStore.put(gid, new MapleGuildSummary(g));
                }
                return gsStore.get(gid);
            } catch (final RemoteException re) {
                System.err.println("RemoteException while fetching GuildSummary: " + re);
                return null;
            }
        }
    }

    public void updateGuildSummary(final int gid, final MapleGuildSummary mgs) {
        gsStore.put(gid, mgs);
    }

    public void reloadGuildSummary() {
        try {
            MapleGuild g;
            for (final int i : gsStore.keySet()) {
                g = this.getWorldInterface().getGuild(i, null);
                if (g != null) {
                    gsStore.put(i, new MapleGuildSummary(g));
                } else {
                    gsStore.remove(i);
                }
            }
        } catch (final RemoteException re) {
            System.err.println("RemoteException while reloading GuildSummary: " + re);
        }
    }

    public static void main(final String[] args) throws IOException,
                                                  NotBoundException,
                                                  InstanceAlreadyExistsException,
                                                  MBeanRegistrationException,
                                                  NotCompliantMBeanException,
                                                  MalformedObjectNameException {
        initialProp = new Properties();
        initialProp.load(new FileReader(System.getProperty("net.sf.odinms.channel.config")));
        final Registry registry =
            LocateRegistry.getRegistry(
                initialProp.getProperty(
                    "net.sf.odinms.world.host"
                ),
                Registry.REGISTRY_PORT,
                new SslRMIClientSocketFactory()
            );
        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
        for (int i = 0; i < Integer.parseInt(initialProp.getProperty("net.sf.odinms.channel.count", "0")); ++i) {
            newInstance(initialProp.getProperty("net.sf.odinms.channel." + i + ".key")).run();
        }
        DatabaseConnection.getConnection();
        CommandProcessor.registerMBean();
        ClanHolder.loadAllClans();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (final ChannelServer channel : getAllInstances()) {
                for (int i = 910000001; i <= 910000022; ++i) {
                    final List<MapleMapObject> hiredMerchants =
                        channel
                            .getMapFactory()
                            .getMap(i)
                            .getMapObjectsInRange(
                                new Point(),
                                Double.POSITIVE_INFINITY,
                                MapleMapObjectType.HIRED_MERCHANT
                            );
                    for (final MapleMapObject obj : hiredMerchants) {
                        final HiredMerchant hm = (HiredMerchant) obj;
                        hm.closeShop(true);
                    }
                }
                for (final MapleCharacter mc : channel.getPlayerStorage().getAllCharacters()) {
                    mc.saveToDB(true, true);
                }
            }
        }));
        MapleItemInformationProvider.getInstance().cacheCashEquips();
    }

    public MapleSquad getMapleSquad(final MapleSquadType type) {
        return mapleSquads.get(type);
    }

    public boolean addMapleSquad(final MapleSquad squad, final MapleSquadType type) {
        if (mapleSquads.get(type) == null) {
            mapleSquads.remove(type);
            mapleSquads.put(type, squad);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeMapleSquad(final MapleSquad squad, final MapleSquadType type) {
        if (mapleSquads.containsKey(type)) {
            if (mapleSquads.get(type) == squad) {
                mapleSquads.remove(type);
                return true;
            }
        }
        return false;
    }

    public boolean isGodlyItems() {
        return godlyItems;
    }

    public void setGodlyItems(final boolean blahblah) {
        this.godlyItems = blahblah;
    }

    public short getItemMultiplier() {
        return itemStatMultiplier;
    }

    public void setItemMultiplier(final Short blahblah) {
        this.itemStatMultiplier = blahblah;
    }

    public short getGodlyItemRate() {
        return godlyItemRate;
    }

    public void setGodlyItemRate(final Short blahblah) {
        this.godlyItemRate = blahblah;
    }

    public void broadcastSMega(final MaplePacket data) {
        for (final MapleCharacter chr : players.getAllCharacters()) {
            if (chr.getSmegaEnabled()) {
                chr.getClient().getSession().write(data);
            }
        }
    }

    public void broadcastGMPacket(final MaplePacket data) {
        for (final MapleCharacter chr : players.getAllCharacters()) {
            if (chr.isGM()) {
                chr.getClient().getSession().write(data);
            }
        }
    }

    public String getServerName() {
        return serverName;
    }

    public void broadcastToClan(final MaplePacket data, final int clan) {
        for (final MapleCharacter chr : clans.getAllOnlinePlayersFromClan(clan)) {
            chr.getClient().getSession().write(data);
        }
    }

    public int onlineClanMembers(final int clan) {
        return clans.countOnlineByClan(clan);
    }

    public List<MapleCharacter> getPartyMembers(final MapleParty party) {
        final int ch = channel;
        return
            party
                .getMembers()
                .stream()
                .filter(partychar -> partychar.getChannel() == ch)
                .map(partychar -> getPlayerStorage().getCharacterByName(partychar.getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void addClone(final FakeCharacter fc) {
        clones.add(fc);
    }

    public void removeClone(final FakeCharacter fc) {
        clones.remove(fc);
    }

    public Collection<FakeCharacter> getAllClones() {
        return clones;
    }

    public int getLevelCap() {
        return levelCap;
    }

    public boolean getMultiLevel() {
        return multiLevel;
    }

    public boolean AutoBan() {
        return AB;
    }

    public boolean CStoFM() {
        return CS;
    }

    public boolean MTtoFM() {
        return MT;
    }

    public boolean CanGMItem() {
        return GMItems;
    }

    public boolean extraCommands() {
        return extraCommands;
    }

    public int PvPis() {
        if (PvPis > 0 && PvPis < 21 || PvPis >= 100000000 && PvPis <=990000000) {
            return PvPis;
        } else {
            return 4;
        }
    }
}
