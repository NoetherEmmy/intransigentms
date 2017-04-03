package net.sf.odinms.server.PlayerInteraction;

import net.sf.odinms.client.Equip;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.maps.AbstractMapleMapObject;
import net.sf.odinms.tools.MaplePacketCreator;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class PlayerInteractionManager extends AbstractMapleMapObject implements IPlayerInteractionManager {
    private final String ownerName;
    private final int ownerId;
    private final byte type;
    private String description = "";
    private final short capacity;
    protected final MapleCharacter[] visitors = new MapleCharacter[3];
    protected final List<MaplePlayerShopItem> items = new ArrayList<>();

    public PlayerInteractionManager(MapleCharacter owner, int type, String desc, int capacity) {
        this.setPosition(owner.getPosition());
        this.ownerName = owner.getName();
        this.ownerId = owner.getId();
        this.type = (byte) type;
        this.capacity = (short) capacity;
        this.description = desc;
    }

    @Override
    public void broadcast(MaplePacket packet, boolean toOwner) {
        for (MapleCharacter visitor : visitors) {
            if (visitor != null) {
                visitor.getClient().getSession().write(packet);
            }
        }
        if (toOwner) {
            MapleCharacter pOwner = null;
            if (getShopType() == 2) {
                pOwner = ((MaplePlayerShop) this).getMCOwner();
            } else if (getShopType() == 3 || getShopType() == 4) {
                pOwner = ((MapleMiniGame) this).getOwner();
            }
            if (pOwner != null) {
                pOwner.getClient().getSession().write(packet);
            }
        }
    }

    @Override
    public void removeVisitor(MapleCharacter visitor) {
        int slot = getVisitorSlot(visitor);
        boolean shouldUpdate = getFreeSlot() == -1;
        if (slot > -1) {
            visitors[slot] = null;
            broadcast(MaplePacketCreator.shopVisitorLeave(slot + 1), true);
            if (shouldUpdate) {
                if (getShopType() == 1) {
                    ((HiredMerchant) this)
                        .getMap()
                        .broadcastMessage(
                            MaplePacketCreator.updateHiredMerchant(
                                (HiredMerchant) this
                            )
                        );
                } else {
                    ((MaplePlayerShop) this)
                        .getMCOwner()
                        .getMap()
                        .broadcastMessage(
                            MaplePacketCreator.sendInteractionBox(
                                ((MaplePlayerShop) this).getMCOwner()
                            )
                        );
                }
            }
        }
    }

    public void saveItems() {
        try {
            tempItems(true, true);
        } catch (SQLException ex) {
            System.err.println("Error saving " + ownerName + " items: " + ex);
        }
    }

    public void tempItemsUpdate() {
        try {
            tempItems(true, false);
        } catch (SQLException ex) {
            System.err.println("Error saving " + ownerName + " temporary items: " + ex);
        }
    }

    public void tempItems(boolean overwrite, boolean saveItems) throws SQLException {
        PreparedStatement ps;
        String table = "temp";
        if (saveItems) {
            table = "";
        } else {
            ps = DatabaseConnection.getConnection().prepareStatement(
                "DELETE FROM hiredmerchanttemp WHERE ownerid = ?"
            );
            ps.setInt(1, ownerId);
            ps.executeUpdate();
            ps.close();
        }
        if (overwrite) {
            for (MaplePlayerShopItem pItems : items) {
                if (pItems.getBundles() > 0) {
                    if (pItems.getItem().getType() == 1) {
                        ps = DatabaseConnection.getConnection().prepareStatement(
                            "INSERT INTO ? (ownerid, itemid, quantity, upgradeslots, level, str, dex, `int`, " +
                                "luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, owner, type) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)"
                        );
                        Equip eq = (Equip) pItems.getItem();
                        ps.setInt(3, eq.getItemId());
                        ps.setInt(4, 1);
                        ps.setInt(5, eq.getUpgradeSlots());
                        ps.setInt(6, eq.getLevel());
                        ps.setInt(7, eq.getStr());
                        ps.setInt(8, eq.getDex());
                        ps.setInt(9, eq.getInt());
                        ps.setInt(10, eq.getLuk());
                        ps.setInt(11, eq.getHp());
                        ps.setInt(12, eq.getMp());
                        ps.setInt(13, eq.getWatk());
                        ps.setInt(14, eq.getMatk());
                        ps.setInt(15, eq.getWdef());
                        ps.setInt(16, eq.getMdef());
                        ps.setInt(17, eq.getAcc());
                        ps.setInt(18, eq.getAvoid());
                        ps.setInt(19, eq.getHands());
                        ps.setInt(20, eq.getSpeed());
                        ps.setInt(21, eq.getJump());
                        ps.setString(22, eq.getOwner());
                    } else {
                        ps = DatabaseConnection.getConnection().prepareStatement(
                            "INSERT INTO ? (ownerid, itemid, quantity, owner, type) VALUES (?, ?, ?, ?, 0)"
                        );
                        ps.setInt(3, pItems.getItem().getItemId());
                        ps.setInt(4, pItems.getBundles());
                        ps.setString(5, pItems.getItem().getOwner());
                    }
                    ps.setString(1, "hiredmerchant" + table);
                    ps.setInt(2, ownerId);
                    ps.executeUpdate();
                    ps.close();
                }
            }
        }
    }

    @Override
    public void addVisitor(MapleCharacter visitor) {
        int i = getFreeSlot();
        if (i <= -1) return;
        broadcast(MaplePacketCreator.shopVisitorAdd(visitor, i + 1), true);
        visitors[i] = visitor;
        if (getFreeSlot() == -1) {
            if (getShopType() == 1) {
                ((HiredMerchant) this)
                    .getMap()
                    .broadcastMessage(
                        MaplePacketCreator.updateHiredMerchant(
                            (HiredMerchant) this
                        )
                    );
            } else {
                MapleCharacter pOwner = null;
                if (getShopType() == 2) {
                    pOwner = ((MaplePlayerShop) this).getMCOwner();
                } else if (getShopType() == 3 || getShopType() == 4) {
                    pOwner = ((MapleMiniGame) this).getOwner();
                }
                if (pOwner != null) {
                    pOwner.getMap().broadcastMessage(MaplePacketCreator.sendInteractionBox(pOwner));
                }
            }
        }
    }

    @Override
    public int getVisitorSlot(MapleCharacter visitor) {
        for (int i = 0; i < capacity; ++i) {
            if (visitors[i] == visitor) return i;
        }
        return -1;
    }

    @Override
    public void removeAllVisitors(int error, int type) {
        for (int i = 0; i < capacity; ++i) {
            if (visitors[i] != null) {
                if (type != -1) {
                    visitors[i]
                        .getClient()
                        .getSession()
                        .write(
                            MaplePacketCreator.shopErrorMessage(
                                error,
                                type
                            )
                        );
                }
                visitors[i].setInteraction(null);
                visitors[i] = null;
            }
        }
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public MapleCharacter[] getVisitors() {
        return visitors;
    }

    @Override
    public List<MaplePlayerShopItem> getItems() {
        return items;
    }

    @Override
    public void addItem(MaplePlayerShopItem item) {
        items.add(item);
        tempItemsUpdate();
    }

    @Override
    public boolean removeItem(int itemId) {
        synchronized (items) {
            MaplePlayerShopItem toRemove = null;
            for (MaplePlayerShopItem i : items) {
                if (i.getItem().getItemId() == itemId) {
                    toRemove = i;
                    break;
                }
            }
            if (toRemove != null) {
                items.remove(toRemove);
                tempItemsUpdate();
                return true;
            }
            return false;
        }
    }

    @Override
    public void removeFromSlot(int slot) {
        items.remove(slot);
    }

    @Override
    public int getFreeSlot() {
        for (int i = 0; i < 3; ++i) {
            if (visitors[i] == null) return i;
        }
        return -1;
    }

    @Override
    public byte getItemType() {
        return type;
    }

    @Override
    public boolean isOwner(MapleCharacter chr) {
        return chr.getId() == ownerId && chr.getName().equals(ownerName);
    }

    public boolean returnItems(MapleClient c) {
        for (MaplePlayerShopItem item : items) {
            if (item.getBundles() > 0) {
                IItem nItem = item.getItem();
                nItem.setQuantity(item.getBundles());
                if (MapleInventoryManipulator.addFromDrop(c, nItem)) {
                    item.setBundles((short) 0);
                } else {
                    return true;
                }
            }
        }
        return false;
    }
}
