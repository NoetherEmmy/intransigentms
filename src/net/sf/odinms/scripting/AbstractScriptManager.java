package net.sf.odinms.scripting;

import net.sf.odinms.client.MapleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileReader;

public abstract class AbstractScriptManager {
    protected ScriptEngine engine;
    private final ScriptEngineManager sem;
    protected static final Logger log = LoggerFactory.getLogger(AbstractScriptManager.class);
    public static final String[] libs = {
        "ecma6-array-polyfill.min",
        "intransigentms-utils.min"
    };

    protected AbstractScriptManager() {
        sem = new ScriptEngineManager();
    }

    protected Invocable getInvocable(String path, final MapleClient c) {
        try {
            path = "scripts/" + path;
            engine = null;
            if (c != null) engine = c.getScriptEngine(path);
            if (engine == null) {
                final File scriptFile = new File(path);
                if (!scriptFile.exists()) {
                    System.err.println("path0: " + path);
                    return null;
                }
                engine = sem.getEngineByName("nashorn");
                if (c != null) c.setScriptEngine(path, engine);
                for (final String libName : libs) {
                    final FileReader libReader = new FileReader("scripts/" + libName + ".js");
                    engine.eval(libReader);
                }
                final FileReader fr = new FileReader(scriptFile);
                engine.eval(fr);
                fr.close();
            }
            return (Invocable) engine;
        } catch (final Exception e) {
            log.error("Error executing script. ", e);
            System.err.println("path1: " + path);
            return null;
        }
    }

    protected void resetContext(String path, final MapleClient c) {
        path = "scripts/" + path;
        c.removeScriptEngine(path);
    }
}
