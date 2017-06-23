package net.sf.odinms.client;

public class CharacterNameAndId {
    private final int id;
    private final String name;

    public CharacterNameAndId(final int id, final String name) {
        super();
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
