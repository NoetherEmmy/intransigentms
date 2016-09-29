package net.sf.odinms.net.channel;

import java.util.List;
import java.util.LinkedList;
import net.sf.odinms.client.MapleCharacter;

public class PartyQuest {
    private final List<MapleCharacter> chars = new LinkedList<>();
    private final String name;
    private long timeStarted = 0;
    private long eventTime = 0;

    public PartyQuest(String name) {
        this.name = name;
    }

    public void registerPlayer(MapleCharacter player) {
        chars.add(player);
        player.setPartyQuest(this);
    }

    public void unregisterPlayer(MapleCharacter player) {
        chars.remove(player);
        player.setPartyQuest(null);
    }

    public void leftParty(MapleCharacter player) {
    }

    public void disbandParty() {
    }

    public void playerDisconnected(MapleCharacter player) {
        if (isLeader(player) || playerparty.size() < minPlayers) { // Check for party leader or party size less than minimum players.
            // Boot whole party and end
            var party = eim.getPlayers();
            for (var i = 0; i < party.size(); i++) {
                if (party.get(i).equals(player)) {
                    removePlayer(eim, player); // Sets map only. Cant possible changeMap because player is offline.
                } else {
                    playerExit(eim, party.get(i)); // Removes all other characters from the instance.
                }
            }
            eim.dispose();
        } else { // non leader.
            removePlayer(eim, player); // Sets map only. Cant possible changeMap because player is offline.
        }
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
        return (player.getParty().getLeader().getId() == player.getId());
    }
}