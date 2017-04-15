package net.sf.odinms.net.world;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MapleMessenger implements Serializable {
    private static final long serialVersionUID = 9179541993413738569L;
    private final List<MapleMessengerCharacter> members = new ArrayList<>();
    private int id;
    private boolean pos0 = false;
    private boolean pos1 = false;
    @SuppressWarnings("unused")
    private boolean pos2 = false;


    public MapleMessenger(final int id, final MapleMessengerCharacter chrfor) {
        this.members.add(chrfor);
        final int position = getLowestPosition();
        chrfor.setPosition(position);
        this.id = id;
    }

    public boolean containsMembers (final MapleMessengerCharacter member) {
        return members.contains(member);
    }

    public void addMember (final MapleMessengerCharacter member) {
        members.add(member);
        final int position = getLowestPosition();
        member.setPosition(position);
    }

    public void removeMember (final MapleMessengerCharacter member) {
        final int position = member.getPosition();
        if (position == 0) {
            pos0 = false;
        }
        else if (position == 1) {
            pos1 = false;
        }
        else if (position == 2) {
            pos2 = false;
        }
        members.remove(member);
    }

    public void silentRemoveMember (final MapleMessengerCharacter member) {
        members.remove(member);
    }

    public void silentAddMember (final MapleMessengerCharacter member, final int position) {
        members.add(member);
        member.setPosition(position);
    }

    public void updateMember(final MapleMessengerCharacter member) {
        for (int i = 0; i < members.size(); ++i) {
            final MapleMessengerCharacter chr = members.get(i);
            if (chr.equals(member)) {
                members.set(i, member);
            }
        }
    }

    public Collection<MapleMessengerCharacter> getMembers () {
        return Collections.unmodifiableList(members);
    }

    public int getLowestPosition() {
        int position;
        if (pos0) {
            if (pos1) {
                this.pos2 = true;
                return 2;
            } else {
                this.pos1 = true;
                return 1;
            }
        } else {
            this.pos0 = true;
            return 0;
        }
    }

    public int getPositionByName(final String name) {
        for (final MapleMessengerCharacter messengerchar : members) {
            if (messengerchar.getName().equals(name)) {
                return messengerchar.getPosition();
            }
        }
        return 4;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MapleMessenger other = (MapleMessenger) obj;
        return id == other.id;
    }
}
