package net.sf.odinms.server.maps;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.tools.MockIOSession;

public class FakeCharacter {
    private final MapleCharacter ch, owner;
    private boolean follow = true;

    public FakeCharacter(final MapleCharacter player, final int id) {
        final MapleCharacter clone = new MapleCharacter();
        clone.setFake();
        clone.setHair(player.getHair());
        clone.setFace(player.getFace());
        clone.setSkinColor(player.getSkinColor());
        clone.setName(player.getName(), false);
        clone.setID(id + 100000);
        clone.setLevel(player.getLevel());
        clone.setJob(player.getJob().getId());
        clone.setMap(player.getMap());
        clone.setPosition(player.getPosition());
        clone.silentGiveBuffs(player.getAllBuffs());
        for (final IItem equip : player.getInventory(MapleInventoryType.EQUIPPED)) {
            clone.getInventory(MapleInventoryType.EQUIPPED).addFromDB(equip);
        }
        for (final IItem equip : player.getInventory(MapleInventoryType.EQUIP)) {
            clone.getInventory(MapleInventoryType.EQUIP).addFromDB(equip);
        }
        player.getMap().addBotPlayer(clone);
        clone.setClient(new MapleClient(null, null, new MockIOSession()));
        ch = clone;
        owner = player;
    }

    public MapleCharacter getFakeChar() {
        return ch;
    }

    public boolean follow() {
        return follow;
    }

    public void setFollow(final boolean set) {
        follow = set;
    }

    public MapleCharacter getOwner() {
        return owner;
    }
}
