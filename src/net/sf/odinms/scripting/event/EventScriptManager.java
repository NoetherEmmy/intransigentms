package net.sf.odinms.scripting.event;

import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.scripting.AbstractScriptManager;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
//import java.util.logging.Logger;

public class EventScriptManager extends AbstractScriptManager {
    private class EventEntry {
        public final String script;
        public final Invocable iv;
        public final EventManager em;

        public EventEntry(final String script, final Invocable iv, final EventManager em) {
            this.script = script;
            this.iv = iv;
            this.em = em;
        }
    }

    private final Map<String, EventEntry> events = new LinkedHashMap<>();

    public EventScriptManager(final ChannelServer cserv, final String... scripts) {
        super();
        for (final String script : scripts) {
            if (!script.equals("")) {
                final Invocable iv = getInvocable("event/" + script + ".js", null);
                events.put(script, new EventEntry(script, iv, new EventManager(cserv, iv, script)));
            }
        }
    }

    public EventManager getEventManager(final String event) {
        final EventEntry entry = events.get(event);
        if (entry == null) return null;
        return entry.em;
    }

    public void init() {
        for (final EventEntry entry : events.values()) {
            try {
                ((ScriptEngine) entry.iv).put("em", entry.em);
                entry.iv.invokeFunction("init", (Object) null);
            } catch (ScriptException | NoSuchMethodException e) {
                //Logger.getLogger(EventScriptManager.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    public void cancel() {
        events.values().forEach(entry -> entry.em.cancel());
    }
}
