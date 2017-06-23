package net.sf.odinms.client;

public class BuddylistEntry {
    private final String name;
    private final int cid;
    private int channel;
    private boolean visible;

    /**
     * @param channel should be -1 if the buddy is offline
     */
    public BuddylistEntry(final String name, final int characterId, final int channel, final boolean visible) {
        super();
        this.name = name;
        this.cid = characterId;
        this.channel = channel;
        this.visible = visible;
    }

    /**
     * @return the channel the character is on. If the character is offline returns -1.
     */
    public int getChannel() {
        return channel;
    }

    public void setChannel(final int channel) {
        this.channel = channel;
    }

    public boolean isOnline() {
        return channel >= 0;
    }

    public void setOffline() {
        channel = -1;
    }

    public String getName() {
        return name;
    }

    public int getCharacterId() {
        return cid;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + cid;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final BuddylistEntry other = (BuddylistEntry) obj;
        return cid == other.cid;
    }
}
