package net.sf.odinms.net.channel;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.server.maps.MapleMap;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PartyQuestMapInstance {
    private static final String SCRIPT_PATH = "scripts/pq/";
    private final PartyQuest partyQuest;
    private final MapleMap map;
    private final String path;
    private final Invocable invocable;
    private final Map<MapleCharacter, Map<String, Object>> playerPropertyMap = new HashMap<>(6, 0.9f);
    private int levelLimit = 0;

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
        for (MapleCharacter p : this.partyQuest.getPlayers()) {
            this.playerPropertyMap.put(p, new HashMap<>(3, 0.75f));
        }
    }

    public void dispose() {
        partyQuest.getMapInstances().remove(this);
        map.unregisterPartyQuestInstance();
        playerPropertyMap.clear();
        invokeMethod("dispose");
    }

    public MapleMap getMap() {
        return map;
    }

    public PartyQuest getPartyQuest() {
        return partyQuest;
    }

    public Object invokeMethod(String name) {
        try {
            return invocable.invokeFunction(name);
        } catch (ScriptException se) {
            System.out.println("Error invoking " + name + "() in PartyQuestMapInstance at path " + path);
            se.printStackTrace();
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    public Object invokeMethod(String name, Object... args) {
        try {
            return invocable.invokeFunction(name, args);
        } catch (ScriptException se) {
            System.out.println("Error invoking " + name + "() in PartyQuestMapInstance at path " + path);
            se.printStackTrace();
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    public void setPlayerProperty(MapleCharacter player, String property, Object value) {
        if (!partyQuest.getPlayers().contains(player)) {
            return;
        }
        if (!playerPropertyMap.containsKey(player)) {
            playerPropertyMap.put(player, new HashMap<>(3, 0.75f));
        }
        playerPropertyMap.get(player).put(property, value);
    }

    public void setPlayerPropertyIfNotSet(MapleCharacter player, String property, Object value) {
        if (!partyQuest.getPlayers().contains(player)) {
            return;
        }
        if (!playerPropertyMap.containsKey(player)) {
            playerPropertyMap.put(player, new HashMap<>(3, 0.75f));
        }
        playerPropertyMap.get(player).putIfAbsent(property, value);
    }

    public void setPropertyForAll(String property, Object value) {
        for (MapleCharacter p : partyQuest.getPlayers()) {
            if (!playerPropertyMap.containsKey(p)) {
                playerPropertyMap.put(p, new HashMap<>(3, 0.75f));
            }
            playerPropertyMap.get(p).put(property, value);
        }
    }

    public Object getPlayerProperty(MapleCharacter player, String property) {
        if (!playerPropertyMap.containsKey(player)) {
            return null;
        }
        return playerPropertyMap.get(player).get(property);
    }

    public boolean playerHasProperty(MapleCharacter player, String property) {
        return playerPropertyMap.containsKey(player) && playerPropertyMap.get(player).containsKey(property);
    }

    public boolean propertyExists(String property) {
        for (Map<String, Object> propMap : playerPropertyMap.values()) {
            for (String propName : propMap.keySet()) {
                if (propName.equals(property)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<MapleCharacter> playersWithProperty(String property) {
        Set<MapleCharacter> ret = new HashSet<>(3, 0.7f);
        for (Map.Entry<MapleCharacter, Map<String, Object>> entry : playerPropertyMap.entrySet()) {
            for (String propName : entry.getValue().keySet()) {
                if (propName.equals(property)) {
                    ret.add(entry.getKey());
                    break;
                }
            }
        }
        return ret;
    }

    public void setLevelLimit(int ll) {
        levelLimit = ll;
    }

    public int getLevelLimit() {
        return levelLimit;
    }
}
