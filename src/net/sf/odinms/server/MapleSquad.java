package net.sf.odinms.server;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.List;

public class MapleSquad {
    private final MapleCharacter leader;
    private final List<MapleCharacter> members = new ArrayList<>();
    private final List<MapleCharacter> bannedMembers = new ArrayList<>();
    private final int ch;
    private int status;

    public MapleSquad(int ch, MapleCharacter leader) {
        this.leader = leader;
        this.members.add(leader);
        this.ch = ch;
        this.status = 1;
    }

    public MapleCharacter getLeader() {
        return leader;
    }

    public boolean containsMember(MapleCharacter member) {
        for (MapleCharacter mmbr : members) {
            if (mmbr.getId() == member.getId()) return true;
        }
        return false;
    }

    public boolean isBanned(MapleCharacter member) {
        for (MapleCharacter banned : bannedMembers) {
            if (banned.getId() == member.getId()) return true;
        }
        return false;
    }

    public List<MapleCharacter> getMembers() {
        return members;
    }

    public int getSquadSize() {
        return members.size();
    }

    public boolean addMember(MapleCharacter member) {
        if (isBanned(member)) return false;
        members.add(member);
        MaplePacket packet = MaplePacketCreator.serverNotice(5, member.getName() + " has joined the fight!");
        leader.getClient().getSession().write(packet);
        return true;
    }

    public void banMember(MapleCharacter member, boolean ban) {
        int index = -1;
        for (MapleCharacter mmbr : members) {
            if (mmbr.getId() == member.getId()) {
                index = members.indexOf(mmbr);
            }
        }
        members.remove(index);
        if (ban) bannedMembers.add(member);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void clear() {
        members.clear();
        bannedMembers.clear();
    }

    public boolean equals(MapleSquad other) {
        if (other.ch == ch) {
            if (other.leader.getId() == leader.getId()) return true;
        }
        return false;
    }
}
