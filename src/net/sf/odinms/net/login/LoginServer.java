package net.sf.odinms.net.login;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MapleServerHandler;
import net.sf.odinms.net.PacketProcessor;
import net.sf.odinms.net.login.remote.LoginWorldInterface;
import net.sf.odinms.net.mina.MapleCodecFactory;
import net.sf.odinms.net.world.remote.WorldLoginInterface;
import net.sf.odinms.net.world.remote.WorldRegistry;
import net.sf.odinms.server.TimerManager;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class LoginServer implements Runnable, LoginServerMBean {

    public static final int PORT = 8484;
    private IoAcceptor acceptor;
    private static WorldRegistry worldRegistry = null;
    private final Map<Integer, String> channelServer = new HashMap<>();
    private LoginWorldInterface lwi;
    private WorldLoginInterface wli;
    private Properties prop = new Properties();
    private final Properties initialProp = new Properties();
    private Boolean worldReady = Boolean.TRUE;
    private final Properties subnetInfo = new Properties();
    private Map<Integer, Integer> load = new HashMap<>();
    private String serverName;
    private String eventMessage;
    int flag;
    int maxCharacters;
    int userLimit;
    int loginInterval;
    private boolean serverCheck;
    private boolean twoWorlds;
    private boolean AutoReg;
    private byte AutoRegLimit;
    private static final LoginServer instance = new LoginServer();


    static {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            mBeanServer.registerMBean(instance, new ObjectName("net.sf.odinms.net.login:type=LoginServer,name=LoginServer"));
        } catch (Exception e) {
            System.out.println("MBEAN ERROR" + e);
        }
    }

    private LoginServer() {
    }

    public static LoginServer getInstance() {
        return instance;
    }

    public Set<Integer> getChannels() {
        return channelServer.keySet();
    }

    public void addChannel(int channel, String ip) {
        channelServer.put(channel, ip);
        load.put(channel, 0);
    }

    public void removeChannel(int channel) {
        channelServer.remove(channel);
        load.remove(channel);
    }

    public String getIP(int channel) {
        return channelServer.get(channel);
    }

    @Override
    public int getPossibleLogins() {
        int ret = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement limitCheck = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE loggedin > 1 AND gm = 0");
            ResultSet rs = limitCheck.executeQuery();
            if (rs.next()) {
                int usersOn = rs.getInt(1);
                if (usersOn < userLimit) {
                    ret = userLimit - usersOn;
                }
            }
            rs.close();
            limitCheck.close();
        } catch (Exception ex) {
            System.out.println("loginlimit error" + ex);
        }
        return ret;
    }

    public void reconnectWorld() {
        try {
            wli.isAvailable();
        } catch (RemoteException ex) {
            synchronized (worldReady) {
                worldReady = Boolean.FALSE;
            }
            synchronized (lwi) {
                synchronized (worldReady) {
                    if (worldReady) {
                        return;
                    }
                }
                System.out.println("Reconnecting to world server");
                synchronized (wli) {
                    try {
                        FileReader fileReader = new FileReader(System.getProperty("net.sf.odinms.login.config"));
                        initialProp.load(fileReader);
                        fileReader.close();
                        Registry registry = LocateRegistry.getRegistry(initialProp.getProperty("net.sf.odinms.world.host"),
                        Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
                        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
                        lwi = new LoginWorldInterfaceImpl();
                        wli = worldRegistry.registerLoginServer(initialProp.getProperty("net.sf.odinms.login.key"), lwi);
                        Properties dbProp = new Properties();
                        fileReader = new FileReader("db.properties");
                        dbProp.load(fileReader);
                        fileReader.close();
                        DatabaseConnection.setProps(dbProp);
                        DatabaseConnection.getConnection();
                        prop = wli.getWorldProperties();
                        userLimit = Integer.parseInt(prop.getProperty("net.sf.odinms.login.userlimit", "150"));
                        serverName = prop.getProperty("net.sf.odinms.world.serverName", "IntransigentMS");
                        eventMessage = prop.getProperty("net.sf.odinms.login.eventMessage", "Welcome to IntransigentMS!");
                        flag = Integer.parseInt(prop.getProperty("net.sf.odinms.login.flag", "0"));
                        maxCharacters = Integer.parseInt(prop.getProperty("net.sf.odinms.login.maxCharacters", "6"));
                        serverCheck = Boolean.parseBoolean(prop.getProperty("net.sf.odinms.login.serverCheck", "false"));
                        AutoReg = Boolean.parseBoolean(prop.getProperty("net.sf.odinms.login.AutoRegister", "false"));
                        AutoRegLimit = Byte.parseByte(prop.getProperty("net.sf.odinms.login.AutoRegisterLimit", "5"));
                        twoWorlds = Boolean.parseBoolean(prop.getProperty("net.sf.odinms.world.twoWorlds", "false"));
                        try {
                            fileReader = new FileReader("subnet.properties");
                            subnetInfo.load(fileReader);
                            fileReader.close();
                        } catch (Exception e) {
                            //System.out.println("Could not load subnet configuration. (RW) " + e);
                        }
                    } catch (Exception e) {
                        System.out.println("Reconnecting failed" + e);
                    }
                    worldReady = Boolean.TRUE;
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void run() {
        try {
            FileReader fileReader = new FileReader(System.getProperty("net.sf.odinms.login.config"));
            initialProp.load(fileReader);
            fileReader.close();
            Registry registry = LocateRegistry.getRegistry(initialProp.getProperty("net.sf.odinms.world.host"),
            Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
            worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
            lwi = new LoginWorldInterfaceImpl();
            wli = worldRegistry.registerLoginServer(initialProp.getProperty("net.sf.odinms.login.key"), lwi);
            Properties dbProp = new Properties();
            fileReader = new FileReader("db.properties");
            dbProp.load(fileReader);
            fileReader.close();
            DatabaseConnection.setProps(dbProp);
            DatabaseConnection.getConnection();
            prop = wli.getWorldProperties();
            userLimit = Integer.parseInt(prop.getProperty("net.sf.odinms.login.userlimit", "150"));
            serverName = prop.getProperty("net.sf.odinms.world.serverName", "IntransigentMS");
            eventMessage = prop.getProperty("net.sf.odinms.login.eventMessage", "Welcome to IntransigentMS!");
            flag = Integer.parseInt(prop.getProperty("net.sf.odinms.login.flag", "0"));
            maxCharacters = Integer.parseInt(prop.getProperty("net.sf.odinms.login.maxCharacters", "6"));
            serverCheck = Boolean.parseBoolean(prop.getProperty("net.sf.odinms.login.serverCheck", "false"));
            AutoReg = Boolean.parseBoolean(prop.getProperty("net.sf.odinms.login.AutoRegister", "false"));
            AutoRegLimit = Byte.parseByte(prop.getProperty("net.sf.odinms.login.AutoRegisterLimit", "5"));
            twoWorlds = Boolean.parseBoolean(prop.getProperty("net.sf.odinms.world.twoWorlds", "false"));
            try {
                fileReader = new FileReader("subnet.properties");
                subnetInfo.load(fileReader);
                fileReader.close();
            } catch (Exception e) {
                //System.out.println("Could not load subnet configuration. (Run) " + e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not connect to world server.", e);
        }
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        acceptor = new SocketAcceptor();
        SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
        TimerManager tMan = TimerManager.getInstance();
        tMan.start();
        loginInterval = Integer.parseInt(prop.getProperty("net.sf.odinms.login.interval"));
        tMan.register(LoginWorker.getInstance(), loginInterval);
        long rankingInterval = Long.parseLong(prop.getProperty("net.sf.odinms.login.ranking.interval"));
        tMan.register(new RankingWorker(), rankingInterval);
        try {
            acceptor.bind(new InetSocketAddress(PORT), new MapleServerHandler(PacketProcessor.getProcessor(PacketProcessor.Mode.LOGINSERVER)), cfg);
            System.out.println("Listening on port: " + PORT);
        } catch (IOException e) {
            System.out.println("Binding to port " + PORT + " failed: " + e);
        }
    }

    public void shutdown() {
        System.out.println("The Server is Shutting down.");
        try {
            worldRegistry.deregisterLoginServer(lwi);
        } catch (RemoteException ignored) {
        }
        TimerManager.getInstance().stop();
        System.exit(0);
    }

    public WorldLoginInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
        return wli;
    }

    public static void main(String args[]) {
        try {
            LoginServer.getInstance().run();
        } catch (Exception ex) {
            System.out.println("Error initializing loginserver: " + ex);
            ex.printStackTrace();
        }
    }

    @Override
    public int getLoginInterval() {
        return loginInterval;
    }

    public Properties getSubnetInfo() {
        return subnetInfo;
    }

    @Override
    public int getUserLimit() {
        return userLimit;
    }

    public String getServerName() {
        return serverName;
    }

    @Override
    public String getEventMessage() {
        return eventMessage;
    }

    @Override
    public int getFlag() {
        return flag;
    }

    public int getMaxCharacters() {
        return maxCharacters;
    }

    public Map<Integer, Integer> getLoad() {
        return load;
    }

    public void setLoad(Map<Integer, Integer> load) {
        this.load = load;
    }

    @Override
    public void setEventMessage(String newMessage) {
        this.eventMessage = newMessage;
    }

    @Override
    public void setFlag(int newflag) {
        flag = newflag;
    }

    @Override
    public int getNumberOfSessions() {
        return acceptor.getManagedSessions(new InetSocketAddress(PORT)).size();
    }

    @Override
    public void setUserLimit(int newLimit) {
        userLimit = newLimit;
    }

    public void setServerCheck(boolean set) {
        serverCheck = set;
    }

    public boolean isServerCheck() {
        return serverCheck;
    }

    public boolean AutoRegister() {
        return AutoReg;
    }

    public byte AutoRegLimit() {
        return AutoRegLimit;
    }

    public boolean twoWorldsActive() {
        return twoWorlds;
    }
}