package net.sf.odinms.net.channel;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import net.sf.odinms.server.maps.MapleMap;

public class PartyQuestMapInstance {
    private static final String SCRIPT_PATH = "scripts/pq/";
    private final PartyQuest partyQuest;
    private final MapleMap map;
    private final String path;
    private final Invocable invocable;

    PartyQuestMapInstance(PartyQuest partyQuest, MapleMap map) {
        this.partyQuest = partyQuest;
        this.map = map;
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
        this.path = SCRIPT_PATH + this.map.getId() + ".js";
        try {
            FileReader scriptFile = new FileReader(path);
            scriptEngine.eval(scriptFile);
            scriptFile.close();
            scriptEngine.put("mi", this);
            scriptEngine.put("pq", this.partyQuest);
            scriptEngine.put("map", this.map);
        } catch (FileNotFoundException fnfe) {
            System.out.println("PartyQuestMapInstance could not locate script at path: " + path);
        } catch (ScriptException se) {
            System.out.println("Error evaluating script in PartyQuestMapInstance at path " + path);
            se.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("Error reading script in PartyQuestMapInstance at path " + path);
            ioe.printStackTrace();
        } finally {
            this.invocable = (Invocable) scriptEngine;
        }
    }

    public void dispose() {
        partyQuest.getMapInstances().remove(this);
        map.unregisterPartyQuestInstance();
        invokeEvent("dispose");
    }

    public MapleMap getMap() {
        return map;
    }

    public PartyQuest getPartyQuest() {
        return partyQuest;
    }

    public void invokeEvent(String name) {
        try {
            invocable.invokeFunction(name);
        } catch (ScriptException se) {
            System.out.println("Error invoking " + name + "() in PartyQuestMapInstance at path " + path);
            se.printStackTrace();
        } catch (NoSuchMethodException ignored) {
        }
    }

    public void invokeEvent(String name, Object... args) {
        try {
            invocable.invokeFunction(name, args);
        } catch (ScriptException se) {
            System.out.println("Error invoking " + name + "() in PartyQuestMapInstance at path " + path);
            se.printStackTrace();
        } catch (NoSuchMethodException ignored) {
        }
    }
}