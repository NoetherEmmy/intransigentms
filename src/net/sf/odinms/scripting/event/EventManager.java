package net.sf.odinms.scripting.event;

import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.MapleMap;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
//import java.util.logging.Logger;

public class EventManager {
    private final Invocable iv;
    private final ChannelServer cserv;
    private final Map<String, EventInstanceManager> instances = new LinkedHashMap<>();
    private final Properties props = new Properties();
    private final String name;

    public EventManager(final ChannelServer cserv, final Invocable iv, final String name) {
        this.iv = iv;
        this.cserv = cserv;
        this.name = name;
    }

    public void cancel() {
        try {
            iv.invokeFunction("cancelSchedule", (Object) null);
        } catch (final ScriptException | NoSuchMethodException ex) {
            //Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    public void schedule(final String methodName, final long delay) {
        schedule(methodName, null, delay);
    }

    public void schedule(final String methodName, final EventInstanceManager eim, final long delay) {
        TimerManager.getInstance().schedule(() -> {
            try {
                iv.invokeFunction(methodName, eim);
            } catch (final ScriptException | NoSuchMethodException ex) {
                //Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }, delay);
    }

    public ScheduledFuture<?> scheduleAtTimestamp(final String methodName, final long timestamp) {
        return TimerManager.getInstance().scheduleAtTimestamp(() -> {
            try {
                iv.invokeFunction(methodName, (Object) null);
            } catch (final ScriptException | NoSuchMethodException ex) {
                //Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }, timestamp);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(final String methodName, final long delay) {
        return TimerManager.getInstance().register(() -> {
            try {
                iv.invokeFunction(methodName, (Object) null);
            } catch (final ScriptException | NoSuchMethodException ex) {
                //Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }, delay);
    }

    public ChannelServer getChannelServer() {
        return cserv;
    }

    public EventInstanceManager getInstance(final String name) {
        return instances.get(name);
    }

    public Collection<EventInstanceManager> getInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public EventInstanceManager newInstance(final String name) {
        final EventInstanceManager ret = new EventInstanceManager(this, name);
        instances.put(name, ret);
        return ret;
    }

    public void disposeInstance(final String name) {
        try {
            iv.invokeFunction("dispose", (Object) null);
        } catch (final ScriptException | NoSuchMethodException ex) {
            //Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        instances.remove(name);
    }

    public Invocable getIv() {
        return iv;
    }

    public void setProperty(final String key, final String value) {
        props.setProperty(key, value);
    }

    public String getProperty(final String key) {
        return props.getProperty(key);
    }

    public String getName() {
        return name;
    }

    // PQ method: starts a PQ
    public void startInstance(final MapleParty party, final MapleMap map) {
        try {
            final EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", (Object) null));
            eim.registerParty(party, map);
        } catch (final ScriptException | NoSuchMethodException ex) {
            //Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    // non-PQ method for starting instance
    public void startInstance(final EventInstanceManager eim) {
        try {
            iv.invokeFunction("setup", eim);
        } catch (final ScriptException | NoSuchMethodException ex) {
            //Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }
}
