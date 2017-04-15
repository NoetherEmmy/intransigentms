package net.sf.odinms.net.world;

import net.sf.odinms.client.MapleCharacter;

import java.io.Serializable;

public class MapleMessengerCharacter implements Serializable {
    private static final long serialVersionUID = 6215463252132450750L;
    private final String name;
    private int id;
    private int channel;
    private boolean online;
    private int position;

    public MapleMessengerCharacter(final MapleCharacter maplechar) {
        this.name = maplechar.getName();
        this.channel = maplechar.getClient().getChannel();
        this.id = maplechar.getId();
        this.online = true;
        this.position = 0;
    }

    public MapleMessengerCharacter(final MapleCharacter maplechar, final int position) {
        this.name = maplechar.getName();
        this.channel = maplechar.getClient().getChannel();
        this.id = maplechar.getId();
        this.online = true;
        this.position = position;
    }

    public MapleMessengerCharacter() {
        this.name = "";
        //default values for everything o.o
    }

    public int getChannel() {
        return channel;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(final boolean online) {
        this.online = online;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        final MapleMessengerCharacter other = (MapleMessengerCharacter) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
