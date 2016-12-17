package net.sf.odinms.net.channel;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleReactor;
import net.sf.odinms.server.maps.MapleReactorFactory;
import net.sf.odinms.tools.Direction;
import net.sf.odinms.tools.Vect;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PartyQuestMapInstance {
    private static final String SCRIPT_PATH = "scripts/pq/";
    private final PartyQuest partyQuest;
    private final MapleMap map;
    private final String path;
    private Invocable invocable;
    private final Map<MapleCharacter, Map<String, Object>> playerPropertyMap;
    private int levelLimit = 0;
    private boolean listeningForPlayerMovement = false;
    private final Map<Integer, Obstacle> obstacles = new HashMap<>(10, 0.8f);
    private final Map<Integer, Trigger> triggers = new HashMap<>(10, 0.8f);
    private final Map<MapleCharacter, Point> lastPointsHeard;
    private final Set<Integer> disabledSkills = new HashSet<>(5, 0.8f);
    private int currentObstacleId = 0;
    private int currentTriggerId = 0;

    PartyQuestMapInstance(PartyQuest partyQuest, MapleMap map) {
        this.partyQuest = partyQuest;
        this.map = map;
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
        path = SCRIPT_PATH + this.map.getId() + ".js";
        try {
            FileReader scriptFile = new FileReader(path);
            scriptEngine.eval(scriptFile);
            scriptFile.close();
            scriptEngine.put("mi",  this);
            scriptEngine.put("pq",  this.partyQuest);
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
        playerPropertyMap = new HashMap<>(getPlayers().size() + 1, 0.9f);
        this.getPlayers().forEach(p -> playerPropertyMap.put(p, new HashMap<>(4)));
        lastPointsHeard = new HashMap<>(getPlayers().size() + 1, 0.9f);
    }

    public void dispose() {
        enableAllSkills();
        partyQuest.getMapInstances().remove(this);
        map.unregisterPartyQuestInstance();
        playerPropertyMap.clear();
        lastPointsHeard.clear();
        removeAllTriggers();
        removeAllObstacles();
        invokeMethod("dispose");
        invocable = null;
    }

    public MapleMap getMap() {
        return map;
    }

    public PartyQuest getPartyQuest() {
        return partyQuest;
    }
    
    public List<MapleCharacter> getPlayers() {
        return partyQuest.getPlayers();
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
            System.out.println("Error invoking " + name + "(...) in PartyQuestMapInstance at path " + path);
            se.printStackTrace();
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    public void setPlayerProperty(MapleCharacter player, String property, Object value) {
        if (!getPlayers().contains(player)) return;
        if (!playerPropertyMap.containsKey(player)) {
            playerPropertyMap.put(player, new HashMap<>(4));
        }
        playerPropertyMap.get(player).put(property, value);
    }

    public void setPlayerPropertyIfNotSet(MapleCharacter player, String property, Object value) {
        if (!getPlayers().contains(player)) return;
        if (!playerPropertyMap.containsKey(player)) {
            playerPropertyMap.put(player, new HashMap<>(4));
        }
        playerPropertyMap.get(player).putIfAbsent(property, value);
    }

    public void setPropertyForAll(final String property, final Object value) {
        getPlayers().forEach(p -> {
            if (!playerPropertyMap.containsKey(p)) {
                playerPropertyMap.put(p, new HashMap<>(4));
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
        return playerPropertyMap.entrySet()
                                .stream()
                                .filter(e -> e.getValue().containsKey(property))
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toSet());
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

    public void heardPlayerMovement(final MapleCharacter player, final Point position) {
        final Point lastPointHeard = lastPointsHeard.get(player);
        obstacles.values()
                 .stream()
                 .filter(o ->
                     o.isClosed() &&
                     o.getRect().contains(position) &&
                     (lastPointHeard == null || !o.getRect().contains(lastPointHeard)))
                 .findAny()
                 .ifPresent(o -> {
                     if (lastPointHeard == null) {
                         player.changeMap(map, map.findClosestSpawnpoint(position));
                     } else {
                         Direction dir = o.simpleCollision(lastPointHeard, position);
                         if (dir != null) {
                             MaplePortal to = map.findClosestSpawnpointInDirection(position, dir);
                             if (to != null) {
                                 player.changeMap(map, to);
                             }
                         }
                     }
                     invokeMethod("playerHitObstacle", player, o);
                 });
        lastPointsHeard.put(player, position);
    }

    public int registerObstacle(int reactorId, Point closed) {
        obstacles.put(currentObstacleId, new Obstacle(reactorId, closed, null, true));
        return currentObstacleId++;
    }

    public int registerObstacle(int reactorId, Point closed, Point open) {
        obstacles.put(currentObstacleId, new Obstacle(reactorId, closed, open, true));
        return currentObstacleId++;
    }
    
    public int registerObstacle(int reactorId, Point closed, boolean defaultClosed) {
        obstacles.put(currentObstacleId, new Obstacle(reactorId, closed, null, defaultClosed));
        return currentObstacleId++;
    }
    
    public int registerObstacle(int reactorId, Point closed, Point open, boolean defaultClosed) {
        obstacles.put(currentObstacleId, new Obstacle(reactorId, closed, open, defaultClosed));
        return currentObstacleId++;
    }

    public int registerObstacle(int reactorId, Point closed, Point open, boolean defaultClosed, Collection<Direction> directions) {
        obstacles.put(currentObstacleId, new Obstacle(reactorId, closed, open, defaultClosed, directions));
        return currentObstacleId++;
    }

    public Obstacle getObstacle(int obsId) {
        return obstacles.get(obsId);
    }
    
    public List<Integer> registeredObstacleIds() {
        return obstacles.keySet().stream().sorted().collect(Collectors.toList());
    }
    
    public Map<Integer, Obstacle> readObstacles() {
        return new HashMap<>(obstacles);
    }
    
    public void toggleObstacle(int obsId) {
        obstacles.get(obsId).toggle();
    }
    
    public void closeObstacle(int obsId) {
        obstacles.get(obsId).close();
    }
    
    public void openObstacle(int obsId) {
        obstacles.get(obsId).open();
    }
    
    public void resetObstacle(int obsId) {
        obstacles.get(obsId).reset();
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

    /** Returns <code>true</code> if an <code>Obstacle</code> was removed, <code>false</code>
     ** if no such <code>Obstacle</code> existed and thus no changes were made. */
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

    public int registerTrigger(int reactorId, Point position, int obsId) {
        triggers.put(currentTriggerId, new Trigger(currentTriggerId, reactorId, position, obsId));
        return currentTriggerId++;
    }
    
    public int registerTrigger(int reactorId, Point position, Runnable action) {
        triggers.put(currentTriggerId, new Trigger(currentTriggerId, reactorId, position, action));
        return currentTriggerId++;
    }

    public int registerTrigger(int reactorId, Point position, int obsId, PartyQuestMapInstance pqmi) {
        triggers.put(currentTriggerId, new Trigger(currentTriggerId, reactorId, position, obsId, pqmi));
        return currentTriggerId++;
    }
    
    public Trigger getTrigger(int triggerId) {
        return triggers.get(triggerId);
    }
    
    public void trigger(int triggerId) {
        if (triggers.containsKey(triggerId)) {
            triggers.get(triggerId).trigger();
        }
    }
    
    public void triggerByReactorId(final int reactorId) {
        triggers.values().stream().filter(t -> t.getReactorId() == reactorId).forEach(Trigger::trigger);
    }

    /** Returns <code>true</code> if a <code>Trigger</code> was removed, <code>false</code>
     ** if no such <code>Trigger</code> existed and thus no changes were made. */
    public boolean removeTrigger(int triggerId) {
        if (!triggers.containsKey(triggerId)) return false;
        triggers.get(triggerId).dispose();
        triggers.remove(triggerId);
        return true;
    }

    public void removeAllTriggers() {
        triggers.values().forEach(Trigger::dispose);
        triggers.clear();
    }
    
    public void disableSkill(final int skillId) {
        disabledSkills.add(skillId);
        getPlayers().forEach(p -> {
            p.dispelSkill(skillId);
            p.giveCoolDowns(skillId, System.currentTimeMillis(), 10000000, true);
        });
    }
    
    public void enableSkill(final int skillId) {
        if (disabledSkills.remove(skillId)) {
            getPlayers().forEach(p -> p.removeCooldown(skillId));
        }
    }
    
    public void enableAllSkills() {
        disabledSkills.forEach(ds -> getPlayers().forEach(p -> p.removeCooldown(ds)));
        disabledSkills.clear();
    }
    
    public Set<Integer> readDisabledSkills() {
        return Collections.unmodifiableSet(disabledSkills);
    }
    
    public void reloadScript() {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            FileReader scriptFile = new FileReader(path);
            scriptEngine.eval(scriptFile);
            scriptFile.close();
            scriptEngine.put("mi",  this);
            scriptEngine.put("pq",  this.partyQuest);
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
    }

    public class Obstacle {
        private final MapleReactor reactor;
        private final int reactorId;
        private final Point closed;
        private final Point open;
        private final MapleMap map;
        private final Set<Direction> directions;
        private boolean defaultClosed;

        /** Pass in <code>open = null</code> to make the reactor destroyed
         ** instead of moved when opened.
         **
         ** <code>closed</code> cannot be null. */
        Obstacle(int reactorId, Point closed, Point open, boolean defaultClosed, Collection<Direction> directions) {
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

        /** Constructs new <code>Obstacle</code> with a default of all directions
         ** (<code>UP, DOWN, LEFT, RIGHT</code>). 
         **
         ** Pass in <code>open = null</code> to make the reactor destroyed
         ** instead of moved when opened.
         **
         ** <code>closed</code> cannot be null. */
        Obstacle(int reactorId, Point closed, Point open, boolean defaultClosed) {
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

            this.directions = new HashSet<>(
                Arrays.asList(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
            );
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

        public Set<Direction> collision(Point prevLocation, Point location) {
            if (!getRect().contains(location) || getRect().contains(prevLocation)) {
                return Collections.emptySet();
            }
            /*
             * Downward line is from top-left corner to bottom-right corner,
             *   upward line is from bottom-left corner to top-right corner.
             *  > 0 = true, < 0 = false, 0 = falls on the line.
             */
            int aboveDownwardLine = location.y - (-getRect().height / getRect().width * (location.x - getRect().x) + getRect().y);
            int aboveUpwardLine   = location.y -  (getRect().height / getRect().width * (location.x - getRect().x) + getRect().y - getRect().height);

            final Vect direction;
            if (aboveUpwardLine == 0 || aboveDownwardLine == 0) {
                direction = new Vect(aboveDownwardLine != 0 ? aboveDownwardLine : -aboveUpwardLine,
                                     aboveUpwardLine   != 0 ? aboveUpwardLine   :  aboveDownwardLine);
            } else if (Integer.signum(aboveDownwardLine) == Integer.signum(aboveUpwardLine)) {
                direction = new Vect(0, aboveUpwardLine);
            } else {
                direction = new Vect(aboveDownwardLine, 0);
            }
            return directions.stream()
                             .filter(d -> !direction.directionalProj(d.unitVect()).isZero())
                             .collect(Collectors.toSet());
        }

        /** Nullable */
        public Direction simpleCollision(Point prevLocation, Point location) {
            if (!getRect().contains(location) || getRect().contains(prevLocation)) {
                return null;
            }
            return location.x > prevLocation.x ? Direction.LEFT : Direction.RIGHT;
        }

        public boolean isOpen() {
            return !reactor.isAlive() || reactor.getPosition().equals(open);
        }

        public boolean isClosed() {
            return !isOpen();
        }

        public boolean isDefaultClosed() {
            return defaultClosed;
        }

        /** Gets rectangular bounding box of the Obstacle reactor as an instance of Rectangle. */
        public Rectangle getRect() {
            return reactor.getArea();
        }

        public int getReactorId() {
            return reactorId;
        }
        
        public MapleMap getMap() {
            return map;
        }
        
        public void setDefaultClosed(boolean dc) {
            defaultClosed = dc;
        }

        /** <pre><code>
         * if (reactor.isAlive()) {
         *     map.destroyReactor(reactor.getObjectId());
         * }
         *  </code></pre> */
        public void dispose() {
            if (reactor.isAlive()) {
                map.destroyReactor(reactor.getObjectId());
            }
        }
    }
    
    public class Trigger {
        private final int id;
        private final MapleReactor reactor;
        private final int reactorId;
        private final Point position;
        private final MapleMap map;
        private Runnable action;
        
        public Trigger(int id, int reactorId, Point position, Runnable action) {
            this.id = id;
            map = PartyQuestMapInstance.this.getMap();
            this.reactorId = reactorId;
            this.position = position;
            this.action = action;

            reactor = new MapleReactor(MapleReactorFactory.getReactor(reactorId), reactorId);
            reactor.setTrigger(this.id);
            reactor.setDelay(-1);
            reactor.setPosition(this.position);
            map.spawnReactor(reactor);
        }

        /** Creates a new <code>Trigger</code> whose <code>action</code> (executed upon being triggered)
         ** is toggling the <code>Obstacle</code> specified by ID as the last argument.
         **
         ** @throws IllegalStateException when there is no such <code>Obstacle</code> with the given ID
         ** registered with this <code>Trigger</code>'s <code>PartyQuestMapInstance</code> */
        public Trigger(int id, int reactorId, Point position, int obsId) {
            final Obstacle obs = PartyQuestMapInstance.this.getObstacle(obsId);
            if (obs == null) {
                throw new IllegalStateException(
                    "No Obstacle with the ID of " +
                        obsId +
                        " registered with this Trigger's PartyQuestMapInstance."
                );
            }

            this.id = id;
            map = PartyQuestMapInstance.this.getMap();
            this.reactorId = reactorId;
            this.position = position;
            this.action = obs::toggle;

            reactor = new MapleReactor(MapleReactorFactory.getReactor(reactorId), reactorId);
            reactor.setTrigger(this.id);
            reactor.setDelay(-1);
            reactor.setPosition(this.position);
            map.spawnReactor(reactor);
        }

        /** Creates a new <code>Trigger</code> whose <code>action</code> (executed upon being triggered)
         ** is toggling the <code>Obstacle</code> specified by its ID and associated PartyQuestMapInstance.
         **
         ** @throws IllegalStateException when there is no such <code>Obstacle</code> with the given ID
         ** registered with the specified <code>PartyQuestMapInstance</code> */
        public Trigger(int id, int reactorId, Point position, int obsId, PartyQuestMapInstance pqmi) {
            final Obstacle obs = pqmi.getObstacle(obsId);
            if (obs == null) {
                throw new IllegalStateException(
                    "No Obstacle with the ID of " +
                        obsId +
                        " registered with the specified PartyQuestMapInstance."
                );
            }

            this.id = id;
            map = PartyQuestMapInstance.this.getMap();
            this.reactorId = reactorId;
            this.position = position;
            this.action = obs::toggle;

            reactor = new MapleReactor(MapleReactorFactory.getReactor(reactorId), reactorId);
            reactor.setTrigger(this.id);
            reactor.setDelay(-1);
            reactor.setPosition(this.position);
            map.spawnReactor(reactor);
        }
        
        public void trigger() {
            action.run();
        }
        
        public void setAction(Runnable action) {
            this.action = action;
        }

        /** Gets rectangular bounding box of the Trigger reactor as an instance of Rectangle. */
        public Rectangle getRect() {
            return reactor.getArea();
        }

        public int getReactorId() {
            return reactorId;
        }

        public MapleMap getMap() {
            return map;
        }
        
        public int getId() {
            return id;
        }

        /** <pre><code>
         * if (reactor.isAlive()) {
         *     map.destroyReactor(reactor.getObjectId());
         * }
         *  </code></pre> */
        public void dispose() {
            if (reactor.isAlive()) {
                map.destroyReactor(reactor.getObjectId());
            }
        }
    }
}
