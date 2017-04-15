package net.sf.odinms.scripting.event;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapFactory;

import javax.script.ScriptException;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventInstanceManager {
    private final List<MapleCharacter> chars = new ArrayList<>();
    private final List<MapleMonster> mobs = new ArrayList<>();
    private final Map<MapleCharacter, Integer> killCount = new HashMap<>();
    private EventManager em;
    private MapleMapFactory mapFactory;
    private final String name;
    private final Properties props = new Properties();
    private long timeStarted, eventTime;

    public EventInstanceManager(final EventManager em, final String name) {
        this.em = em;
        this.name = name;
        mapFactory =
            new MapleMapFactory(
                MapleDataProviderFactory.getDataProvider(
                    new File(
                        System.getProperty("net.sf.odinms.wzpath") + "/Map.wz"
                    )
                ),
                MapleDataProviderFactory.getDataProvider(
                    new File(
                        System.getProperty("net.sf.odinms.wzpath") + "/String.wz"
                    )
                )
            );
        mapFactory.setChannel(em.getChannelServer().getChannel());
    }

    public void registerPlayer(final MapleCharacter chr) {
        if (chr != null) {
            try {
                chars.add(chr);
                chr.setEventInstance(this);
                em.getIv().invokeFunction("playerEntry", this, chr);
            } catch (ScriptException | NoSuchMethodException e) {
                Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    public void startEventTimer(final long time) {
        timeStarted = System.currentTimeMillis();
        eventTime = time;
    }

    public boolean isTimerStarted() {
        return eventTime > 0L && timeStarted > 0L;
    }

    public long getTimeLeft() {
        return eventTime - (System.currentTimeMillis() - timeStarted);
    }

    public void registerParty(final MapleParty party, final MapleMap map) {
        for (final MaplePartyCharacter pc : party.getMembers()) {
            final MapleCharacter c = map.getCharacterById(pc.getId());
            registerPlayer(c);
        }
    }

    public void unregisterPlayer(final MapleCharacter chr) {
        chars.remove(chr);
        if (chr != null) chr.setEventInstance(null);
    }

    public int getPlayerCount() {
        return chars.size();
    }

    public List<MapleCharacter> getPlayers() {
        return new ArrayList<>(chars);
    }

    public void registerMonster(final MapleMonster mob) {
        mobs.add(mob);
        mob.setEventInstance(this);
    }

    public void unregisterMonster(final MapleMonster mob) {
        mobs.remove(mob);
        mob.setEventInstance(null);
        if (mobs.isEmpty()) {
            try {
                em.getIv().invokeFunction("allMonstersDead", this);
            } catch (ScriptException | NoSuchMethodException e) {
                Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    public void playerKilled(final MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerDead", this, chr);
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public boolean revivePlayer(final MapleCharacter chr) {
        try {
            final Object b = em.getIv().invokeFunction("playerRevive", this, chr);
            if (b instanceof Boolean) {
                return (Boolean) b;
            }
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
        }
        return true;
    }

    public void playerDisconnected(final MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerDisconnected", this, chr);
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void monsterKilled(final MapleCharacter chr, final MapleMonster mob) {
        try {
            Integer kc = killCount.get(chr);
            final int inc = ((Double) em.getIv().invokeFunction("monsterValue", this, mob.getId())).intValue();
            if (kc == null) {
                kc = inc;
            } else {
                kc += inc;
            }
            killCount.put(chr, kc);
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public int getKillCount(final MapleCharacter chr) {
        final Integer kc = killCount.get(chr);
        if (kc == null) {
            return 0;
        } else {
            return kc;
        }
    }

    public void dispose() {
        chars.clear();
        mobs.clear();
        killCount.clear();
        mapFactory = null;
        if (em != null) em.disposeInstance(name);
        em = null;
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public void schedule(final String methodName, final long delay) {
        TimerManager.getInstance().schedule(() -> {
            try {
                em.getIv().invokeFunction(methodName, EventInstanceManager.this);
            } catch (final NullPointerException ignored) {
            } catch (ScriptException | NoSuchMethodException e) {
                Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, e);
            }
        }, delay);
    }

    public String getName() {
        return name;
    }

    public void killByCount(final MapleCharacter player) {
        try {
            em.getIv().invokeFunction("addKillCount", this, player);
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void saveWinner(final MapleCharacter chr) {
        try {
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps =
                con.prepareStatement(
                    "INSERT INTO eventstats (event, instance, characterid, channel) VALUES (?, ?, ?, ?)"
                );
            ps.setString(1, em.getName());
            ps.setString(2, name);
            ps.setInt(3, chr.getId());
            ps.setInt(4, chr.getClient().getChannel());
            ps.executeUpdate();
        } catch (final SQLException sqle) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, sqle);
        }
    }

    public MapleMap getMapInstance(final int mapId) {
        final boolean wasLoaded = mapFactory.isMapLoaded(mapId);
        final MapleMap map = mapFactory.getMap(mapId);
        // in case reactors need shuffling and we are actually loading the map
        if (!wasLoaded) {
            if (em.getProperty("shuffleReactors") != null && em.getProperty("shuffleReactors").equals("true")) {
                map.shuffleReactors();
            }
        }
        return map;
    }

    public void setProperty(final String key, final String value) {
        props.setProperty(key, value);
    }

    @SuppressWarnings("UnusedParameters")
    public Object setProperty(final String key, final String value, final boolean prev) {
        return props.setProperty(key, value);
    }

    public String getProperty(final String key) {
        return props.getProperty(key);
    }

    public void leftParty(final MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("leftParty", this, chr);
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void disbandParty() {
        try {
            em.getIv().invokeFunction("disbandParty", this);
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    // Separate function to warp players to a "finish" map, if applicable
    public void finishPQ() {
        try {
            em.getIv().invokeFunction("clearPQ", this);
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void removePlayer(final MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerExit", this, chr);
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public boolean isLeader(final MapleCharacter chr) {
        return (chr.getParty().getLeader().getId() == chr.getId());
    }
}
