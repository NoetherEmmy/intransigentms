package net.sf.odinms.net.channel;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.net.channel.handler.ChangeChannelHandler;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.MapleMap;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PartyQuest {
    private final List<MapleCharacter> players = new ArrayList<>(6);
    private final Set<Integer> registeredPlayerIds = new LinkedHashSet<>(8, 0.8f);
    private final List<PartyQuestMapInstance> mapInstances = new ArrayList<>(3);
    private final String name;
    private long timeStarted, eventTime;
    private final int minPlayers, exitMapId, channel;
    private final AtomicInteger points = new AtomicInteger();
    private final Set<Integer> pqItems = new HashSet<>(4, 0.8f);
    private ScheduledFuture<?> disposeTask;

    public PartyQuest(final int channel, final String name, final int minPlayers, final int exitMapId) {
        this.channel = channel;
        this.name = name;
        this.minPlayers = minPlayers;
        this.exitMapId = exitMapId;
        ChannelServer.getInstance(this.channel).registerPartyQuest(this);
    }

    public String getName() {
        return name;
    }

    public List<MapleCharacter> getPlayers() {
        return players;
    }

    public int playerCount() {
        return players.size();
    }

    public Set<Integer> readRegisteredIds() {
        return new HashSet<>(registeredPlayerIds);
    }

    public boolean hasIdRegistered(final int playerId) {
        return registeredPlayerIds.contains(playerId);
    }

    /**
     * Intentionally package private;
     * use {@link #readMapInstances()} instead to get a copy
     * of the list publicly.
     */
    List<PartyQuestMapInstance> getMapInstances() {
        return mapInstances;
    }

    public List<PartyQuestMapInstance> readMapInstances() {
        return new ArrayList<>(mapInstances);
    }

    public PartyQuestMapInstance getMapInstance(final MapleMap map) {
        return mapInstances
            .stream()
            .filter(mi -> map == mi.getMap())
            .findAny()
            .orElse(null);
    }

    public PartyQuestMapInstance getMapInstance(final int mapId) {
        return mapInstances
            .stream()
            .filter(mi -> mapId == mi.getMap().getId())
            .findAny()
            .orElse(null);
    }

    public void registerMap(final int mapId) {
        registerMap(ChannelServer.getInstance(channel).getMapFactory().getMap(mapId));
    }

    public void registerMap(final MapleMap newMap) {
        if (newMap.getPartyQuestInstance() != null) {
            //throw new IllegalStateException("Attempting to register map that is currently in use.");
            System.err.println("Attempting to register map that is currently in use.");
        }
        newMap.resetReactors();
        final PartyQuestMapInstance newInstance = new PartyQuestMapInstance(this, newMap);
        newMap.registerPartyQuestInstance(newInstance);
        newInstance.invokeMethod("init");
        mapInstances.add(newInstance);
        mapInstances
            .stream()
            .filter(mi -> mi.getMap().getId() != newInstance.getMap().getId())
            .forEach(mi -> mi.invokeMethod("mapRegistered", newInstance));
    }

    public void unregisterMap(final int mapId) {
        unregisterMap(ChannelServer.getInstance(channel).getMapFactory().getMap(mapId));
    }

    public void unregisterMap(final MapleMap oldMap) {
        if (oldMap.getPartyQuestInstance() != null) {
            if (!mapInstances.contains(oldMap.getPartyQuestInstance())) {
                //throw new IllegalStateException("Attempting to deregister map that is not owned by the PartyQuest.");
                System.err.println("Attempting to deregister map that is not owned by the PartyQuest.");
            }
            oldMap.getPartyQuestInstance().dispose();
        }
    }

    public int getExitMapId() {
        return exitMapId;
    }

    public int getPoints() {
        return points.get();
    }

    public void setPoints(final int points) {
        final int pointChange = points - this.points.get();
        this.points.set(points);
        final String color;
        if (pointChange > 0) {
            color = "#b(+";
        } else if (pointChange < 0) {
            color = "#r(-";
        } else {
            color = "(";
        }
        players
            .stream()
            .filter(MapleCharacter::showPqPoints)
            .forEach(p -> p.sendHint("PQ points: #e" + this.points + "#n " + color + pointChange + ")#k"));
    }

    public void addPoints(final int increment) {
        points.addAndGet(increment);
        final String color;
        if (increment > 0) {
            color = "#b(+";
        } else if (increment < 0) {
            color = "#r(";
        } else {
            color = "(";
        }
        players
            .stream()
            .filter(MapleCharacter::showPqPoints)
            .forEach(p -> p.sendHint("PQ points: #e" + points + "#n " + color + increment + ")#k"));
    }

    public void addPqItem(final int id) {
        pqItems.add(id);
        ChannelServer.getInstance(this.channel).addPqItem(this, id);
    }

    public Set<Integer> readPqItems() {
        return new LinkedHashSet<>(pqItems);
    }

    public void registerPlayer(final MapleCharacter player) {
        players.add(player);
        player.setPartyQuest(this);
        registeredPlayerIds.add(player.getId());
    }

    private void unregisterPlayer(final MapleCharacter player) {
        pqItems.forEach(itemId -> MapleInventoryManipulator.removeAllById(player.getClient(), itemId, true));
        players.remove(player);
        player.setPartyQuest(null);
        registeredPlayerIds.remove(player.getId());
    }

    public void removePlayer(final MapleCharacter player, final boolean autoDispose) {
        unregisterPlayer(player);
        player.changeMap(exitMapId);
        if (autoDispose && playerCount() == 0) {
            dispose();
        }
    }

    public boolean registerParty(final MapleParty party) {
        for (final MaplePartyCharacter pc : party.getMembers()) {
            final MapleCharacter player =
                ChannelServer
                    .getInstance(channel)
                    .getPlayerStorage()
                    .getCharacterById(pc.getId());
            if (player == null) {
                players.clear();
                registeredPlayerIds.clear();
                return false;
            }
            registerPlayer(player);
        }
        return true;
    }

    public void leftParty(final MapleCharacter player) {
        removePlayer(player, false);
        if (players.size() < minPlayers) dispose();
    }

    public void disbandParty() {
        dispose();
    }

    public void playerDisconnected(final MapleCharacter player) {
        players.remove(player);
        if (isLeader(player) || players.size() < minPlayers) {
            dispose();
        }
    }

    public void playerDead(final MapleCharacter player) {
        unregisterPlayer(player);
        if (isLeader(player) || players.size() < minPlayers) {
            dispose();
        }
    }

    public void dispose() {
        players.forEach(p -> {
            try {
                if (p == null) return;
                pqItems.forEach(itemId ->
                    MapleInventoryManipulator.removeAllById(
                        p.getClient(),
                        itemId,
                        true
                    )
                );
                p.setPartyQuest(null);
                if (p.getMapId() != exitMapId) p.changeMap(exitMapId);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });

        registeredPlayerIds.clear();

        while (!mapInstances.isEmpty()) {
            try {
                mapInstances.get(0).dispose();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        pqItems.clear();
        ChannelServer.getInstance(channel).unregisterPartyQuest(name);
        if (disposeTask != null) disposeTask.cancel(false);
        disposeTask = null;
        players.forEach(p -> {
            try {
                if (p == null) return;
                p.setPartyQuest(null);
                if (p.getMapId() != exitMapId) p.changeMap(exitMapId);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
        players.clear();
    }

    public void startTimer(final long time) {
        timeStarted = System.currentTimeMillis();
        eventTime = time;
        if (disposeTask != null) disposeTask.cancel(false);
        disposeTask = TimerManager.getInstance().schedule(this::dispose, eventTime + 500L);
    }

    public boolean isTimerStarted() {
        return eventTime > 0L && timeStarted > 0L;
    }

    public long getTimeLeft() {
        return eventTime - (System.currentTimeMillis() - timeStarted);
    }

    public void cancelTimer() {
        timeStarted = 0;
        eventTime = 0;
        if (disposeTask != null) disposeTask.cancel(false);
        disposeTask = null;
    }

    public boolean isLeader(final MapleCharacter player) {
        return player.getParty() != null && player.getParty().getLeader().getId() == player.getId();
    }

    /** Returns <code>null</code> if no party leader is registered with this <code>PartyQuest</code> */
    public MapleCharacter getLeader() {
        return players.stream().filter(this::isLeader).findAny().orElse(null);
    }

    public void playerReconnected(final MapleCharacter player) {
        if (player.getClient().getChannel() != channel) {
            ChangeChannelHandler.changeChannel(channel, player.getClient());
        }

        if (!players.contains(player)) {
            registerPlayer(player);
        } else if (player.getPartyQuest() == null) {
            player.setPartyQuest(this);
        }

        final PartyQuestMapInstance pqmiMinPlayers = mapInstances.stream().reduce(null, (min, mi) -> {
            if (mi.getMap().playerCount() > 0) {
                if (min != null) {
                    if (mi.getMap().playerCount() < min.getMap().playerCount()) return mi;
                } else {
                    return mi;
                }
            }
            return min;
        });

        if (
            pqmiMinPlayers == null ||
            pqmiMinPlayers.getMap().playerCount() == 0 ||
            pqmiMinPlayers.getPlayers().get(0).getId() == player.getId()
        ) {
            dispose();
            if (player.getMapId() != exitMapId) {
                player.changeMap(exitMapId, 0);
            }
        } else {
            player.changeMap(pqmiMinPlayers.getMap().getId());
        }
    }

    public Map<PartyQuestMapInstance, Object> invokeInAllInstances(final String functionName) {
        return mapInstances.stream().collect(Collectors.toMap(mi -> mi, mi -> mi.invokeMethod(functionName)));
    }

    public Map<PartyQuestMapInstance, Object> invokeInAllInstances(final String functionName, final Object... args) {
        return mapInstances.stream().collect(Collectors.toMap(mi -> mi, mi -> mi.invokeMethod(functionName, args)));
    }
}
