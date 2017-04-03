package net.sf.odinms.server;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.PacketProcessor;
import net.sf.odinms.tools.MaplePacketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapleShop {
    private static final Set<Integer> rechargeableItems = new HashSet<>();
    private final int id, npcId;
    private final List<MapleShopItem> items;
    private static final Logger log = LoggerFactory.getLogger(PacketProcessor.class);

    static {
        for (int i = 2070000; i <= 2070018; ++i) {
            rechargeableItems.add(i);
        }
        rechargeableItems.add(2331000); // Blaze Capsule
        rechargeableItems.add(2332000); // Glaze Capsule
        rechargeableItems.remove(2070014);
        rechargeableItems.remove(2070017);
        for (int i = 2330000; i <= 2330005; ++i) {
            rechargeableItems.add(i);
        }
    }

    private MapleShop(int id, int npcId) {
        this.id = id;
        this.npcId = npcId;
        items = new ArrayList<>();
    }

    public void addItem(MapleShopItem item) {
        items.add(item);
    }

    public void sendShop(MapleClient c) {
        c.getPlayer().setShop(this);
        c.getSession().write(MaplePacketCreator.getNPCShop(c, npcId, items));
    }

    public void buy(MapleClient c, int itemId, short quantity) {
        MapleShopItem item = findById(itemId);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (item != null && item.getPrice() > 0) {
            if (c.getPlayer().getMeso() >= item.getPrice() * quantity) {
                if (MapleInventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                    if (!ii.isRechargable(itemId)) {
                        if (itemId >= 5000000 && itemId <= 5000100) {
                            int petId = MaplePet.createPet(itemId);
                            MapleInventoryManipulator.addById(c, petId, quantity, "Pet was purchased.");
                        } else {
                            MapleInventoryManipulator.addById(c, itemId, quantity);
                        }
                        c.getPlayer().gainMeso(-(item.getPrice() * quantity), false);
                    } else {
                        quantity = ii.getSlotMax(c, item.getItemId());
                        MapleInventoryManipulator.addById(c, itemId, quantity, "Rechargable item purchased.");
                        c.getPlayer().gainMeso(-(item.getPrice()), false);
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "Your inventory is full."));
                }
                c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0));
            } else {
                int token = 4000313;
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(token) != 0) {
                    int amount = c.getPlayer().getInventory(MapleInventoryType.CASH).countById(token);
                    int tokenvalue = 1000000000;
                    int value = amount * tokenvalue;
                    int cost = item.getPrice() * quantity;
                    if (c.getPlayer().getMeso() + value >= cost) {
                        int cardreduce = value - cost;
                        int diff = cardreduce + c.getPlayer().getMeso();
                        if (MapleInventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                            if (itemId >= 5000000 && itemId <= 5000100) {
                                int petId = MaplePet.createPet(itemId);
                                MapleInventoryManipulator.addById(c, petId, quantity, "Pet was purchased.");
                            } else {
                                MapleInventoryManipulator.addById(c, itemId, quantity);
                            }
                            c.getPlayer().gainMeso(diff, false);
                        } else {
                            c.getSession().write(MaplePacketCreator.serverNotice(1, "Your inventory is full."));
                        }
                        c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0));
                    }
                }
            }
        }
    }

    public void sell(MapleClient c, MapleInventoryType type, byte slot, short quantity) {
        if (quantity == 0) quantity = 1;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem item = c.getPlayer().getInventory(type).getItem(slot);
        if (item == null) return;
        if (ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId())) {
            quantity = item.getQuantity();
        }
        if (quantity < 0) return;
        short iQuant = item.getQuantity();
        if (quantity <= iQuant && iQuant > 0) {
            MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
            double price;
            if (ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId())) {
                price = ii.getWholePrice(item.getItemId()) / (double) ii.getSlotMax(c, item.getItemId());
            } else {
                price = ii.getPrice(item.getItemId());
            }
            int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
            if (price != -1 && recvMesos > 0) {
                c.getPlayer().gainMeso(recvMesos, false);
            }
            c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
            c.getPlayer().addBuyBack(item, quantity);
        }
    }

    public void recharge(MapleClient c, byte slot) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (item == null || (!ii.isThrowingStar(item.getItemId()) && !ii.isBullet(item.getItemId()))) {
            if (item != null && (!ii.isThrowingStar(item.getItemId()) || !ii.isBullet(item.getItemId()))) {
                log.warn(c.getPlayer().getName() + " is trying to recharge " + item.getItemId());
            }
            return;
        }
        short slotMax = ii.getSlotMax(c, item.getItemId());
        if (item.getQuantity() < 0) {
            log.warn(
                c.getPlayer().getName() +
                    " is trying to recharge " +
                    item.getItemId() +
                    " with quantity " +
                    item.getQuantity()
            );
        }
        if (item.getQuantity() < slotMax) {
            int price = (int) Math.round(ii.getPrice(item.getItemId()) * (slotMax - item.getQuantity()));
            if (c.getPlayer().getMeso() >= price) {
                item.setQuantity(slotMax);
                c.getSession().write(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, item));
                c.getPlayer().gainMeso(-price, false, true, false);
                c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
            }
        }
    }

    protected MapleShopItem findById(int itemId) {
        for (MapleShopItem item : items) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public static MapleShop createFromDB(int id, boolean isShopId) {
        MapleShop ret = null;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int shopId;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (isShopId) {
                ps = con.prepareStatement("SELECT * FROM shops WHERE shopid = ?");
            } else {
                ps = con.prepareStatement("SELECT * FROM shops WHERE npcid = ?");
            }
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                shopId = rs.getInt("shopid");
                ret = new MapleShop(shopId, rs.getInt("npcid"));
                rs.close();
                ps.close();
            } else {
                rs.close();
                ps.close();
                return null;
            }
            ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ? ORDER BY position ASC");
            ps.setInt(1, shopId);
            rs = ps.executeQuery();
            List<Integer> recharges = new ArrayList<>(rechargeableItems);
            while (rs.next()) {
                if (ii.isThrowingStar(rs.getInt("itemid")) || ii.isBullet(rs.getInt("itemid"))) {
                    MapleShopItem starItem =
                        new MapleShopItem((short) 1, rs.getInt("itemid"), rs.getInt("price"));
                    ret.addItem(starItem);
                    if (rechargeableItems.contains(starItem.getItemId())) {
                        recharges.remove(Integer.valueOf(starItem.getItemId()));
                    }
                } else {
                    ret.addItem(new MapleShopItem((short) 1000, rs.getInt("itemid"), rs.getInt("price")));
                }
            }
            for (Integer recharge : recharges) {
                ret.addItem(new MapleShopItem((short) 1000, recharge, 0));
            }
            rs.close();
            ps.close();
        } catch (SQLException sqle) {
            log.error("Could not load shop. ", sqle);
        }
        return ret;
    }

    public int getNpcId() {
        return npcId;
    }

    public int getId() {
        return id;
    }
}
