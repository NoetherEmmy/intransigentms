package net.sf.odinms.net.world;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MapleParty implements Serializable {
    private static final long serialVersionUID = 9179541993413738569L;
    private MaplePartyCharacter leader;
    private final List<MaplePartyCharacter> members = new ArrayList<>();
    private int id;
    private int CP;
    private int team;
    private int totalCP;
    private boolean capture = false;
    private boolean waiting = false;
    private boolean challenging = false;
    private boolean challenged = false;
    private MapleParty challenger = null;
    private int points = 0;

    public MapleParty(int id, MaplePartyCharacter chrfor) {
        leader = chrfor;
        members.add(leader);
        this.id = id;
    }

    public boolean getCapture() {
        return capture;
    }

    public boolean getWaiting() {
        return waiting;
    }

    public boolean getChallenging() {
        return challenging;
    }

    public boolean getChallenged() {
        return challenged;
    }

    public MapleParty getChallenger() {
        return challenger;
    }

    public int getPoints() {
        return points;
    }

    public void setCapture(boolean c) {
        capture = c;
    }

    public void setWaiting(boolean w) {
        waiting = w;
    }

    public void setChallenging(boolean c) {
        challenging = c;
    }

    public void setChallenged(boolean c) {
        challenged = c;
    }

    public void setChallenger(MapleParty c) {
        challenger = c;
    }

    public void setPoints(int p) {
        points = p;
    }

    public boolean containsMembers(MaplePartyCharacter member) {
        return members.contains(member);
    }

    public void setLeader(MaplePartyCharacter victim) {
        leader = victim;
    }

    public void addMember(MaplePartyCharacter member) {
        members.add(member);
    }

    public void removeMember(MaplePartyCharacter member) {
        members.remove(member);
    }

    public void updateMember(MaplePartyCharacter member) {
        for (int i = 0; i < members.size(); ++i) {
            MaplePartyCharacter chr = members.get(i);
            if (chr.equals(member)) {
                members.set(i, member);
            }
        }
    }

    public MaplePartyCharacter getMemberById(int id) {
        for (MaplePartyCharacter chr : members) {
            if (chr.getId() == id) {
                return chr;
            }
        }
        return null;
    }

    public Collection<MaplePartyCharacter> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCP() {
        return CP;
    }

    public int getTeam() {
        return team;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void setCP(int cp) {
        CP = cp;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public void setTotalCP(int totalcp) {
        totalCP = totalcp;
    }

    public MaplePartyCharacter getLeader() {
        return leader;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MapleParty other = (MapleParty) obj;
        return id == other.id;
    }
}
