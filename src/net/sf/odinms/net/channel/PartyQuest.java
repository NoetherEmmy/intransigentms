package net.sf.odinms.net.channel;

import java.util.List;
import java.util.LinkedList;
import net.sf.odinms.client.MapleCharacter;

public class PartyQuest {
    private final List<MapleCharacter> chars = new LinkedList<>();
    private final String name;
    private long timeStarted = 0;
    private long eventTime = 0;
    private int minPlayers;
    private int exitMapId;

    public PartyQuest(String name, int minPlayers, int exitMapId) {
        this.name = name;
        this.minPlayers = minPlayers;
        this.exitMapId = exitMapId;
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
        if (chars.size() < minPlayers) {
            dispose();
        } else {
            removePlayer(player);
        }
    }

    public void disbandParty() {
        dispose();
    }

    public void playerDisconnected(MapleCharacter player) {
        removePlayer(player);
        if (isLeader(player) || chars.size() < minPlayers) { // Check for party leader or party size less than minimum players.
            // Boot whole party and end.
            dispose();
        }
    }

    public void playerDead(MapleCharacter player) {
        unregisterPlayer(player);
    }

    public void dispose() {
        for (MapleCharacter char_ : chars) {
            char_.changeMap(exitMapId);
            unregisterPlayer(char_);
        }
        chars.clear();
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
}