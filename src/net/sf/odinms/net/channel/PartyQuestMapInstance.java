package net.sf.odinms.net.channel;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleReactor;
import net.sf.odinms.server.maps.MapleReactorFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

public class PartyQuestMapInstance {
    private static final String SCRIPT_PATH = "scripts/pq/";
    private final PartyQuest partyQuest;
    private final MapleMap map;
    private final String path;
    private final Invocable invocable;
    private final Map<MapleCharacter, Map<String, Object>> playerPropertyMap;
    private int levelLimit = 0;
    private boolean listeningForPlayerMovement = false;
    private final Map<Integer, Obstacle> obstacles = new HashMap<>(10, 0.8f);
    private final Map<MapleCharacter, Point> lastPointsHeard;

    PartyQuestMapInstance(PartyQuest partyQuest, MapleMap map) {
        this.partyQuest = partyQuest;
        this.map = map;
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
        path = SCRIPT_PATH + this.map.getId() + ".js";
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
            invocable = (Invocable) scriptEngine;
        }
        playerPropertyMap = new HashMap<>(partyQuest.getPlayers().size() + 1, 0.9f);
        this.partyQuest.getPlayers().forEach(p -> playerPropertyMap.put(p, new HashMap<>(4, 0.75f)));
        lastPointsHeard = new HashMap<>(partyQuest.getPlayers().size() + 1, 0.9f);
    }

    public void dispose() {
        partyQuest.getMapInstances().remove(this);
        map.unregisterPartyQuestInstance();
        playerPropertyMap.clear();
        removeAllObstacles();
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
        if (!partyQuest.getPlayers().contains(player)) return;
        if (!playerPropertyMap.containsKey(player)) {
            playerPropertyMap.put(player, new HashMap<>(4, 0.75f));
        }
        playerPropertyMap.get(player).put(property, value);
    }

    public void setPlayerPropertyIfNotSet(MapleCharacter player, String property, Object value) {
        if (!partyQuest.getPlayers().contains(player)) return;
        if (!playerPropertyMap.containsKey(player)) {
            playerPropertyMap.put(player, new HashMap<>(4, 0.75f));
        }
        playerPropertyMap.get(player).putIfAbsent(property, value);
    }

    public void setPropertyForAll(final String property, final Object value) {
        partyQuest.getPlayers().forEach(p -> {
            if (!playerPropertyMap.containsKey(p)) {
                playerPropertyMap.put(p, new HashMap<>(4, 0.75f));
            }
            playerPropertyMap.get(p).put(property, value);
        });
    }

    public Object getPlayerProperty(MapleCharacter player, String property) {
        return playerPropertyMap.containsKey(player) ? playerPropertyMap.get(player).get(property) : null;
    }

    public boolean playerHasProperty(MapleCharacter player, String property) {
        return playerPropertyMap.containsKey(player) && playerPropertyMap.get(player).containsKey(property);
    }

    public boolean propertyExists(final String property) {
        return playerPropertyMap.values().stream().anyMatch(m -> m.containsKey(property));
    }

    public Set<MapleCharacter> playersWithProperty(final String property) {
        return playerPropertyMap.entrySet().stream().filter(e -> e.getValue().containsKey(property)).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public void setLevelLimit(int ll) {
        levelLimit = ll;
    }

    public int getLevelLimit() {
        return levelLimit;
    }

    public boolean isListeningForPlayerMovement() {
        return listeningForPlayerMovement;
    }

    public void listenForPlayerMovement() {
        listeningForPlayerMovement = true;
    }

    public void stopListeningForPlayerMovement() {
        listeningForPlayerMovement = false;
    }

    public void heardPlayerMovement(MapleCharacter player, Point position) {
        for (Obstacle o : obstacles.values()) {
            if (o.getRect().contains(position)) {
                final Point lastPointHeard = lastPointsHeard.get(player);
                if (lastPointHeard == null) {
                    player.changeMap(map, map.findClosestSpawnpoint(position));
                } else {
                    //player.changeMap(map, map.findClosestSpawnpointInQuadrant(subtractPoints(lastPointHeard, position)));
                }
                invokeMethod("playerHitObstacle", player, o);
                return;
            }
        }
    }

    public void registerObstacle(int obsId, int reactorId, Point closed, Point open, boolean defaultClosed) {
        //obstacles.put(obsId, new Obstacle(reactorId, closed, open, defaultClosed));
    }

    public void registerObstacle(int obsId, int reactorId, Point closed, boolean defaultClosed) {
        //obstacles.put(obsId, new Obstacle(reactorId, closed, null, defaultClosed));
    }

    public Obstacle getObstacle(int obsId) {
        return obstacles.get(obsId);
    }

    public void openAllObstacles() {
        obstacles.values().forEach(Obstacle::open);
    }

    public void closeAllObstacles() {
        obstacles.values().forEach(Obstacle::close);
    }

    public void toggleAllObstacles() {
        obstacles.values().forEach(Obstacle::toggle);
    }

    public void resetAllObstacles() {
        obstacles.values().forEach(Obstacle::reset);
    }

    public boolean removeObstacle(int obsId) {
        if (!obstacles.containsKey(obsId)) return false;
        obstacles.get(obsId).dispose();
        obstacles.remove(obsId);
        return true;
    }

    public void removeAllObstacles() {
        obstacles.values().forEach(Obstacle::dispose);
        obstacles.clear();
    }

    public class Obstacle {
        private final MapleReactor reactor;
        private final int reactorId;
        private final Point closed;
        private final Point open;
        private final MapleMap map;
        private final Set<ObstacleDirection> directions;
        private boolean defaultClosed;

        /** Pass in <code>open = null</code> to make the reactor destroyed
         ** instead of moved when opened.
         **
         ** <code>closed</code> cannot be null. */
        Obstacle(int reactorId, Point closed, Point open, boolean defaultClosed, Collection<ObstacleDirection> directions) {
            map = PartyQuestMapInstance.this.getMap();
            this.reactorId = reactorId;
            this.closed = closed;
            this.open = open;
            this.defaultClosed = defaultClosed;

            reactor = new MapleReactor(MapleReactorFactory.getReactor(reactorId), reactorId);
            reactor.setDelay(-1);
            if (this.open != null) {
                reactor.setPosition(this.defaultClosed ? this.closed : this.open);
                map.spawnReactor(reactor);
            } else {
                reactor.setPosition(this.closed);
                if (this.defaultClosed) {
                    map.spawnReactor(reactor);
                }
            }

            this.directions = new HashSet<>(directions);
        }

        public void open() {
            if (reactor.isAlive()) {
                map.destroyReactor(reactor.getObjectId());
            }
            if (open != null) {
                reactor.setPosition(open);
                reactor.setState((byte) 0);
                reactor.setAlive(true);
                map.spawnReactor(reactor);
            }
        }

        public void close() {
            reactor.setPosition(closed);
            if (!reactor.isAlive()) {
                reactor.setState((byte) 0);
                reactor.setAlive(true);
                map.spawnReactor(reactor);
            }
        }

        public void toggle() {
            if (isOpen()) {
                close();
            } else {
                open();
            }
        }

        public void reset() {
            if (defaultClosed) {
                close();
            } else {
                open();
            }
        }

        public boolean checkCollision(Point prevLocation, Point location) {
            if (!getRect().contains(location) || getRect().contains(prevLocation)) {
                // Not inside the obstacle at all, or already was inside obstacle
                return false;
            }
            // Downward line is from top-left corner to bottom-right corner,
            //   upward line is from bottom-left corner to top-right corner.
            // > 0 = true, < 0 = false, 0 = falls on the line.
            int aboveUpwardLine   =  location.y - (getRect().height / getRect().width * (location.x - getRect().x) + getRect().y - getRect().height);
            int aboveDownwardLine = location.y - (-getRect().height / getRect().width * (location.x - getRect().x) + getRect().y);
            if (true) {
                return this.defaultClosed;
            }
            return false;
        }

        public boolean isOpen() {
            return !reactor.isAlive() || reactor.getPosition().equals(open);
        }

        public boolean isDefaultClosed() {
            return defaultClosed;
        }

        public Rectangle getRect() {
            return reactor.getArea();
        }

        public int getReactorId() {
            return reactorId;
        }

        public void dispose() {
            if (reactor.isAlive()) {
                map.destroyReactor(reactor.getObjectId());
            }
        }
    }

    public enum ObstacleDirection {
                       UP  (new Point(0,  1)),
        LEFT (new Point(-1, 0)), RIGHT (new Point(1, 0)),
                      DOWN (new Point(0, -1));

        private final Point unitVector;
        private final static Map<Point, ObstacleDirection> map =
                 stream(ObstacleDirection.values())
                .collect(toMap(od -> od.unitVector, od -> od));

        ObstacleDirection(Point unitVector) {
            this.unitVector = unitVector;
        }

        public static ObstacleDirection valueOf(Point od) {
            return map.get(new Point(Integer.signum(od.x), Integer.signum(od.y)));
        }
    }

    /** Returns a new point (<b>not</b> null) equal to
     ** {@code a + b} as if by vector addition. */
    public static Point addPoints(Point a, Point b) {
        return new Point(a.x + b.x, a.y + b.y);
    }

    /** Returns a new point (<b>not</b> null) equal to
     ** {@code a - b} as if by vector subtraction. */
    public static Point subtractPoints(Point a, Point b) {
        return new Point(a.x - b.x, a.y - b.y);
    }
}
