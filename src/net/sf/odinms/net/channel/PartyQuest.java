package net.sf.odinms.net.channel;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.maps.MapleMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PartyQuest {
    private final List<MapleCharacter> players = new ArrayList<>(6);
    private final List<PartyQuestMapInstance> mapInstances = new ArrayList<>(3);
    private final String name;
    private long timeStarted = 0;
    private long eventTime = 0;
    private final int minPlayers;
    private final int exitMapId;
    private final int channel;
    private int points;
    private final List<Integer> pqItems = new ArrayList<>(4);

    public PartyQuest(int channel, String name, int minPlayers, int exitMapId) {
        this.channel = channel;
        this.name = name;
        this.minPlayers = minPlayers;
        this.exitMapId = exitMapId;
        points = 0;
        ChannelServer.getInstance(this.channel).registerPartyQuest(this);
    }

    public String getName() {
        return name;
    }

    public List<MapleCharacter> getPlayers() {
        return players;
    }

    public List<PartyQuestMapInstance> getMapInstances() {
        return mapInstances;
    }

    public PartyQuestMapInstance getMapInstance(final MapleMap map) {
        return mapInstances.stream().filter(mi -> map == mi.getMap()).findAny().orElse(null);
    }

    public PartyQuestMapInstance getMapInstance(final int mapId) {
        return mapInstances.stream().filter(mi -> mapId == mi.getMap().getId()).findAny().orElse(null);
    }

    public void registerMap(int mapId) {
        MapleMap newMap = ChannelServer.getInstance(channel).getMapFactory().getMap(mapId);
        if (newMap.playerCount() > 0 || newMap.getPartyQuestInstance() != null) {
            throw new IllegalStateException("Attempting to register map that is currently in use.");
        }
        newMap.resetReactors();
        PartyQuestMapInstance newInstance = new PartyQuestMapInstance(this, newMap);
        newMap.registerPartyQuestInstance(newInstance);
        mapInstances.remove(newInstance);
        newInstance.invokeMethod("init");
    }

    public void unregisterMap(int mapId) {
        MapleMap oldMap = ChannelServer.getInstance(channel).getMapFactory().getMap(mapId);
        if (!this.mapInstances.contains(oldMap.getPartyQuestInstance())) {
            throw new IllegalStateException("Attempting to deregister map that is not owned by the PartyQuest.");
        }
        oldMap.getPartyQuestInstance().dispose();
    }

    public int getExitMapId() {
        return exitMapId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        final int pointChange = points - this.points;
        this.points = points;
        final String color;
        if (pointChange > 0) {
            color = "#b(+";
        } else if (pointChange < 0) {
            color = "#r(-";
        } else {
            color = "(";
        }
        players.stream().filter(MapleCharacter::showPqPoints).forEach(p -> p.sendHint("PQ points: #e" + this.points + "#n " + color + pointChange + ")#k"));
    }

    public void addPoints(final int increment) {
        this.points += increment;
        final String color;
        if (increment > 0) {
            color = "#b(+";
        } else if (increment < 0) {
            color = "#r(-";
        } else {
            color = "(";
        }
        players.stream().filter(MapleCharacter::showPqPoints).forEach(p -> p.sendHint("PQ points: #e" + this.points + "#n " + color + increment + ")#k"));
    }

    public void addPqItem(int id) {
        pqItems.add(id);
    }

    public List<Integer> getPqItems() {
        return pqItems;
    }

    public void registerPlayer(MapleCharacter player) {
        players.add(player);
        player.setPartyQuest(this);
    }

    private void unregisterPlayer(final MapleCharacter player) {
        pqItems.forEach(itemId -> MapleInventoryManipulator.removeAllById(player.getClient(), itemId, true));
        players.remove(player);
        player.setPartyQuest(null);
    }

    private void removePlayer(MapleCharacter player) {
        unregisterPlayer(player);
        player.changeMap(exitMapId);
    }

    public void registerParty(MapleParty party) {
        party.getMembers().forEach(pc -> {
            final MapleCharacter player = ChannelServer.getInstance(this.channel).getPlayerStorage().getCharacterById(pc.getId());
            registerPlayer(player);
        });
    }

    public void leftParty(MapleCharacter player) {
        removePlayer(player);
        if (players.size() < minPlayers) dispose();
    }

    public void disbandParty() {
        dispose();
    }

    public void playerDisconnected(MapleCharacter player) {
        removePlayer(player);
        if (isLeader(player) || players.size() < minPlayers) {
            dispose();
        }
    }

    public void playerDead(MapleCharacter player) {
        unregisterPlayer(player);
        if (isLeader(player) || players.size() < minPlayers) {
            dispose();
        }
    }

    public void dispose() {
        players.forEach(p -> {
            pqItems.forEach(itemId -> MapleInventoryManipulator.removeAllById(p.getClient(), itemId, true));
            p.changeMap(exitMapId);
            p.setPartyQuest(null);
        });
        players.clear();
        mapInstances.forEach(PartyQuestMapInstance::dispose);
        mapInstances.clear();
        pqItems.clear();
        ChannelServer.getInstance(channel).unregisterPartyQuest(name);
    }

    public void startTimer(long time) {
        timeStarted = System.currentTimeMillis();
        eventTime = time;
    }

    public boolean isTimerStarted() {
        return eventTime > 0 && timeStarted > 0;
    }

    public long getTimeLeft() {
        return eventTime - (System.currentTimeMillis() - timeStarted);
    }

    public void cancelTimer() {
        timeStarted = 0;
        eventTime = 0;
    }

    public boolean isLeader(MapleCharacter player) {
        return player.getParty().getLeader().getId() == player.getId();
    }

    /** Returns <code>null</code> if no party leader is registered with this <code>PartyQuest</code> */
    public MapleCharacter getLeader() {
        return players.stream().filter(this::isLeader).findAny().orElse(null);
    }

    public void playerReconnected(MapleCharacter player) {
        if (!players.contains(player)) {
            registerPlayer(player);
        } else if (player.getPartyQuest() == null) {
            player.setPartyQuest(this);
        }

        PartyQuestMapInstance pqmiMinPlayers = mapInstances.stream().reduce(null, (min, mi) -> {
            if (mi.getMap().playerCount() > 0) {
                if (min != null) {
                    if (mi.getMap().playerCount() < min.getMap().playerCount()) {
                        return mi;
                    }
                } else {
                    return mi;
                }
            }
            return min;
        });
        if (pqmiMinPlayers == null || pqmiMinPlayers.getMap().playerCount() == 0) {
            dispose();
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
