package net.sf.odinms.net.channel;

import java.util.List;
import java.util.LinkedList;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.maps.MapleMap;

public class PartyQuest {
    private final List<MapleCharacter> players = new LinkedList<>();
    private final List<PartyQuestMapInstance> mapInstances = new LinkedList<>();
    private final String name;
    private long timeStarted = 0;
    private long eventTime = 0;
    private final int minPlayers;
    private final int exitMapId;
    private final int channel;
    private int points;

    public PartyQuest(int channel, String name, int minPlayers, int exitMapId) {
        this.channel = channel;
        this.name = name;
        this.minPlayers = minPlayers;
        this.exitMapId = exitMapId;
        this.points = 0;
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

    public PartyQuestMapInstance getMapInstance(MapleMap map) {
        for (PartyQuestMapInstance pqmi : this.mapInstances) {
            if (map == pqmi.getMap()) {
                return pqmi;
            }
        }
        return null;
    }

    public PartyQuestMapInstance getMapInstance(int mapId) {
        for (PartyQuestMapInstance pqmi : this.mapInstances) {
            if (mapId == pqmi.getMap().getId()) {
                return pqmi;
            }
        }
        return null;
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
        newInstance.invokeEvent("init");
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
        int pointChange = points - this.points;
        this.points = points;
        String color;
        if (pointChange > 0) {
            color = "#b(+";
        } else if (pointChange < 0) {
            color = "#r(-";
        } else {
            color = "(";
        }
        for (MapleCharacter p : players) {
            if (p.showPqPoints()) {
                p.sendHint("PQ points: #e" + this.points + "#n " + color + pointChange + ")#k");
            }
        }
    }

    public void addPoints(int increment) {
        this.points += increment;
        String color;
        if (increment > 0) {
            color = "#b(+";
        } else if (increment < 0) {
            color = "#r(-";
        } else {
            color = "(";
        }
        for (MapleCharacter p : players) {
            if (p.showPqPoints()) {
                p.sendHint("PQ points: #e" + this.points + "#n " + color + increment + ")#k");
            }
        }
    }

    public void registerPlayer(MapleCharacter player) {
        players.add(player);
        player.setPartyQuest(this);
    }

    private void unregisterPlayer(MapleCharacter player) {
        players.remove(player);
        player.setPartyQuest(null);
    }

    private void removePlayer(MapleCharacter player) {
        unregisterPlayer(player);
        player.changeMap(exitMapId);
    }

    public void registerParty(MapleParty party, MapleMap map) {
        for (MaplePartyCharacter pc : party.getMembers()) {
            MapleCharacter player = map.getCharacterById(pc.getId());
            registerPlayer(player);
        }
    }

    public void leftParty(MapleCharacter player) {
        removePlayer(player);
        if (players.size() < minPlayers) {
            dispose();
        }
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
    }

    public void dispose() {
        for (MapleCharacter p : players) {
            p.changeMap(exitMapId);
            p.setPartyQuest(null);
        }
        players.clear();
        for (PartyQuestMapInstance pqmi : mapInstances) {
            pqmi.dispose();
        }
        mapInstances.clear();
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

    public void playerReconnected(MapleCharacter player) {
        if (!players.contains(player)) {
            registerPlayer(player);
        } else if (player.getPartyQuest() == null) {
            player.setPartyQuest(this);
        }
        PartyQuestMapInstance pqmiMinPlayers = mapInstances.isEmpty() ? null : mapInstances.get(0);
        for (int i = 1; i < mapInstances.size(); ++i) {
            PartyQuestMapInstance mi = mapInstances.get(i);
            if (mi != null && mi.getMap().playerCount() > 0) {
                if (pqmiMinPlayers != null) {
                    if (mi.getMap().playerCount() < pqmiMinPlayers.getMap().playerCount()) {
                        pqmiMinPlayers = mi;
                    }
                } else {
                    pqmiMinPlayers = mi;
                }
            }
        }
        if (pqmiMinPlayers == null || pqmiMinPlayers.getMap().playerCount() == 0) {
            dispose();
        } else {
            player.changeMap(pqmiMinPlayers.getMap().getId());
        }
    }

    public void invokeInAllInstances(String functionName) {
        for (PartyQuestMapInstance pqmi : mapInstances) {
            pqmi.invokeEvent(functionName);
        }
    }

    public void invokeInAllInstances(String functionName, Object... args) {
        for (PartyQuestMapInstance pqmi : mapInstances) {
            pqmi.invokeEvent(functionName, args);
        }
    }
}