package net.sf.odinms.server.PlayerInteraction;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;

import java.util.List;

public interface IPlayerInteractionManager {

    byte HIRED_MERCHANT = 1;
    byte PLAYER_SHOP = 2;
    byte MATCH_CARD = 3;
    byte OMOK = 4;

    void broadcast(MaplePacket packet, boolean toOwner);

    void addVisitor(MapleCharacter visitor);

    void removeVisitor(MapleCharacter visitor);

    int getVisitorSlot(MapleCharacter visitor);

    void removeAllVisitors(int error, int type);

    void buy(MapleClient c, int item, short quantity);

    void closeShop(boolean saveItems);

    String getOwnerName();

    int getOwnerId();

    String getDescription();

    MapleCharacter[] getVisitors();

    List<MaplePlayerShopItem> getItems();

    void addItem(MaplePlayerShopItem item);

    boolean removeItem(int item);

    void removeFromSlot(int slot);

    int getFreeSlot();

    byte getItemType();

    boolean isOwner(MapleCharacter chr);

    byte getShopType();
}