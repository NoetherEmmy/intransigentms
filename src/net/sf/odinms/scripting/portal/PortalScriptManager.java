package net.sf.odinms.scripting.portal;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.scripting.AbstractScriptManager;
import net.sf.odinms.server.MaplePortal;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class PortalScriptManager {
    //private static final Logger log = LoggerFactory.getLogger(PortalScriptManager.class);
    private static final PortalScriptManager instance = new PortalScriptManager();
    private final Map<String, PortalScript> scripts = new LinkedHashMap<>();
    private final ScriptEngineFactory sef;

    private PortalScriptManager() {
        final ScriptEngineManager sem = new ScriptEngineManager();
        sef = sem.getEngineByName("nashorn").getFactory();
    }

    public static PortalScriptManager getInstance() {
        return instance;
    }

    private PortalScript getPortalScript(final String scriptName) {
        if (scripts.containsKey(scriptName)) {
            return scripts.get(scriptName);
        }
        final File scriptFile = new File("scripts/portal/" + scriptName + ".js");
        if (!scriptFile.exists()) {
            scripts.put(scriptName, null);
            return null;
        }
        FileReader fr = null;
        final ScriptEngine portal = sef.getScriptEngine();
        try {
            for (final String libName : AbstractScriptManager.libs) {
                if (AbstractScriptManager.libContents.containsKey(libName)) {
                    portal.eval(AbstractScriptManager.libContents.get(libName));
                } else {
                    final String fileContents =
                        String.join(
                            "\n",
                            Files.readAllLines(
                                Paths.get("scripts/" + libName + ".js"),
                                StandardCharsets.UTF_8
                            )
                        );
                    AbstractScriptManager.libContents.put(libName, fileContents);
                    portal.eval(fileContents);
                }
            }
            fr = new FileReader(scriptFile);
            final CompiledScript compiled = ((Compilable) portal).compile(fr);
            compiled.eval();
        } catch (ScriptException | IOException e) {
            System.err.println("THROW from script " + scriptName + ".js");
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (final IOException ioe) {
                    System.err.println("ERROR CLOSING");
                    ioe.printStackTrace();
                }
            }
        }
        final PortalScript script = ((Invocable) portal).getInterface(PortalScript.class);
        scripts.put(scriptName, script);
        return script;
    }

    // Nashorn is thread-safe so this should be fine without synchronization.
    public boolean executePortalScript(final MaplePortal portal, final MapleClient c) {
        final PortalScript script = getPortalScript(portal.getScriptName());
        return script != null && script.enter(new PortalPlayerInteraction(c, portal));
    }

    public void clearScripts() {
        scripts.clear();
    }
}
