package net.sf.odinms.scripting;

import net.sf.odinms.client.MapleClient;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public abstract class AbstractScriptManager {
    protected ScriptEngine engine;
    private final ScriptEngineManager sem;
    //protected static final Logger log = LoggerFactory.getLogger(AbstractScriptManager.class);
    public static final String[] libs = {
        "ecma6-polyfill.min",
        "intransigentms-utils.min"
    };
    public static final Map<String, String> libContents = new LinkedHashMap<>(3);

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
                    if (libContents.containsKey(libName)) {
                        engine.eval(libContents.get(libName));
                    } else {
                        final String fileContents =
                            String.join(
                                "\n",
                                Files.readAllLines(
                                    Paths.get("scripts/" + libName + ".js"),
                                    StandardCharsets.UTF_8
                                )
                            );
                        libContents.put(libName, fileContents);
                        engine.eval(fileContents);
                    }
                }
                final FileReader fr = new FileReader(scriptFile);
                engine.eval(fr);
                fr.close();
            }
            return (Invocable) engine;
        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println("path1: " + path);
            return null;
        }
    }

    protected void resetContext(String path, final MapleClient c) {
        path = "scripts/" + path;
        c.removeScriptEngine(path);
    }

    public static void clearLibCache() {
        libContents.clear();
    }
}
