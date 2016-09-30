package net.sf.odinms.net.channel;

import java.util.List;
import java.util.LinkedList;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.server.maps.MapleMap;

public class PartyQuest {
    private final List<MapleCharacter> chars = new LinkedList<>();
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
        return this.name;
    }

    public List<MapleCharacter> getChars() {
        return this.chars;
    }

    public List<PartyQuestMapInstance> getMapInstances() {
        return this.mapInstances;
    }

    public PartyQuestMapInstance getMapInstance(MapleMap map) {
        for (PartyQuestMapInstance pqmi : this.mapInstances) {
            if (map == pqmi.getMap()) {
                return pqmi;
            }
        }
        return null;
    }

    public PartyQuestMapInstance getMapInstanceByMapId(int mapId) {
        for (PartyQuestMapInstance pqmi : this.mapInstances) {
            if (mapId == pqmi.getMap().getId()) {
                return pqmi;
            }
        }
        return null;
    }

    private void registerMapInstance(PartyQuestMapInstance pqmi) {
        this.mapInstances.add(pqmi);
    }

    public void removeMapInstance(PartyQuestMapInstance pqmi) {
        this.mapInstances.remove(pqmi);
    }

    private void unregisterMapInstance(PartyQuestMapInstance pqmi) {
        pqmi.dispose();
    }

    public void registerMap(int mapId) {
        MapleMap newMap = ChannelServer.getInstance(channel).getMapFactory().getMap(mapId);
        if (newMap.playerCount() > 0 || newMap.getPartyQuestInstance() != null) {
            throw new IllegalStateException("Attempting to register map that is currently in use.");
        }
        newMap.resetReactors();
        PartyQuestMapInstance newInstance = new PartyQuestMapInstance(this, newMap);
        newMap.registerPartyQuestInstance(newInstance);
        registerMapInstance(newInstance);
    }

    public void unregisterMap(int mapId) {
        MapleMap oldMap = ChannelServer.getInstance(channel).getMapFactory().getMap(mapId);
        if (!this.mapInstances.contains(oldMap.getPartyQuestInstance())) {
            throw new IllegalStateException("Attempting to deregister map that is not owned by the PartyQuest.");
        }
        unregisterMapInstance(oldMap.getPartyQuestInstance());
        oldMap.unregisterPartyQuestInstance();
    }

    public int getExitMapId() {
        return this.exitMapId;
    }

    public int getPoints() {
        return this.points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void addPoints(int increment) {
        this.points += increment;
    }

    public void registerPlayer(MapleCharacter player) {
        chars.add(player);
        player.setPartyQuest(this);
    }

    private void unregisterPlayer(MapleCharacter player) {
        chars.remove(player);
        player.setPartyQuest(null);
    }

    private void removePlayer(MapleCharacter player) {
        unregisterPlayer(player);
        player.changeMap(exitMapId);
    }

    public void leftParty(MapleCharacter player) {
        removePlayer(player);
        if (chars.size() < minPlayers) {
            dispose();
        }
    }

    public void disbandParty() {
        dispose();
    }

    public void playerDisconnected(MapleCharacter player) {
        removePlayer(player);
        if (isLeader(player) || chars.size() < minPlayers) {
            dispose();
        }
    }

    public void playerDead(MapleCharacter player) {
        unregisterPlayer(player);
    }

    public void dispose() {
        for (MapleCharacter char_ : chars) {
            char_.changeMap(exitMapId);
            char_.setPartyQuest(null);
        }
        chars.clear();
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

    public boolean isLeader(MapleCharacter player) {
        return player.getParty().getLeader().getId() == player.getId();
    }

    public void playerReconnected(MapleCharacter player) {
        if (!chars.contains(player)) {
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
}