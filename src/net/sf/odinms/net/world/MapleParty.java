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
    private int id, CP, team, totalCP;
    private boolean capture = false;
    private boolean waiting = false;
    private boolean challenging = false;
    private boolean challenged = false;
    private MapleParty challenger;
    private int points;

    public MapleParty(final int id, final MaplePartyCharacter chrfor) {
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

    public void setCapture(final boolean c) {
        capture = c;
    }

    public void setWaiting(final boolean w) {
        waiting = w;
    }

    public void setChallenging(final boolean c) {
        challenging = c;
    }

    public void setChallenged(final boolean c) {
        challenged = c;
    }

    public void setChallenger(final MapleParty c) {
        challenger = c;
    }

    public void setPoints(final int p) {
        points = p;
    }

    public boolean containsMembers(final MaplePartyCharacter member) {
        return members.contains(member);
    }

    public void setLeader(final MaplePartyCharacter victim) {
        leader = victim;
    }

    public void addMember(final MaplePartyCharacter member) {
        members.add(member);
    }

    public void removeMember(final MaplePartyCharacter member) {
        members.remove(member);
    }

    public void updateMember(final MaplePartyCharacter member) {
        for (int i = 0; i < members.size(); ++i) {
            final MaplePartyCharacter chr = members.get(i);
            if (chr.equals(member)) members.set(i, member);
        }
    }

    public MaplePartyCharacter getMemberById(final int id) {
        for (final MaplePartyCharacter chr : members) {
            if (chr.getId() == id) return chr;
        }
        return null;
    }

    public Collection<MaplePartyCharacter> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
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

    public void setCP(final int cp) {
        CP = cp;
    }

    public void setTeam(final int team) {
        this.team = team;
    }

    public void setTotalCP(final int totalcp) {
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
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final MapleParty other = (MapleParty) obj;
        return id == other.id;
    }
}
