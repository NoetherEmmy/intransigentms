package net.sf.odinms.scripting;

import net.sf.odinms.client.MapleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileReader;

//import java.io.IOException;
//import java.nio.file.Files;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//import javax.script.ScriptException;

public abstract class AbstractScriptManager {

    protected ScriptEngine engine;
    private final ScriptEngineManager sem;

    protected static final Logger log = LoggerFactory.getLogger(AbstractScriptManager.class);

    protected AbstractScriptManager() {
        sem = new ScriptEngineManager();
    }

    protected Invocable getInvocable(String path, MapleClient c) {
        try {
            path = "scripts/" + path;
            engine = null;
            if (c != null) {
                engine = c.getScriptEngine(path);
            }
            if (engine == null) {
                File scriptFile = new File(path);
                if (!scriptFile.exists()) {
                    System.out.print("path0: " + path + "\n");
                    return null;
                }
                engine = sem.getEngineByName("javascript");
                if (c != null) {
                    c.setScriptEngine(path, engine);
                }
                FileReader fr = new FileReader(scriptFile);
                engine.eval(fr);
                fr.close();
            }
            return (Invocable) engine;
        } catch (Exception e) {
            log.error("Error executing script.", e);
            System.out.print("path1: " + path + "\n");
            return null;
        }
    }

    protected void resetContext(String path, MapleClient c) {
        path = "scripts/" + path;
        c.removeScriptEngine(path);
    }
}