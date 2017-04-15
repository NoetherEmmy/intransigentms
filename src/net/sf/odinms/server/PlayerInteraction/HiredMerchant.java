package net.sf.odinms.server.PlayerInteraction;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

public class HiredMerchant extends PlayerInteractionManager {
    private boolean open;
    public final ScheduledFuture<?> schedule;
    private final MapleMap map;
    private final int itemId;

    public HiredMerchant(final MapleCharacter owner, final int itemId, final String desc) {
        super(owner, itemId % 10, desc, 3);
        this.itemId = itemId;
        this.map = owner.getMap();
        this.schedule =
            TimerManager.getInstance().schedule(
                () -> HiredMerchant.this.closeShop(true),
                1000L * 60L * 60L * 24L
            );
    }

    @Override
    public byte getShopType() {
        return IPlayerInteractionManager.HIRED_MERCHANT;
    }

    @Override
    public void buy(final MapleClient c, final int item, final short quantity) {
        final MaplePlayerShopItem pItem = items.get(item);
        if (pItem.getBundles() > 0) {
            synchronized (items) {
                final IItem newItem = pItem.getItem().copy();
                newItem.setQuantity((short) (quantity * newItem.getQuantity()));
                if (c.getPlayer().getMeso() >= pItem.getPrice() * quantity) {
                    if (quantity > 0 && pItem.getBundles() >= quantity && pItem.getBundles() > 0) {
                        if (MapleInventoryManipulator.addFromDrop(c, newItem)) {
                            final Connection con = DatabaseConnection.getConnection();
                            try {
                                final PreparedStatement ps =
                                    con.prepareStatement(
                                        "UPDATE characters SET MerchantMesos = MerchantMesos + ? WHERE id = ?"
                                    );
                                ps.setInt(1, pItem.getPrice() * quantity);
                                ps.setInt(2, getOwnerId());
                                ps.executeUpdate();
                                ps.close();
                            } catch (final SQLException se) {
                                se.printStackTrace();
                            }
                            c.getPlayer().gainMeso(-pItem.getPrice() * quantity, false);
                            pItem.setBundles((short) (pItem.getBundles() - quantity));
                            tempItemsUpdate();
                        } else {
                            c.getPlayer().dropMessage(1, "Your inventory is full.");
                        }
                    } else {
                        AutobanManager.getInstance().autoban(c, "Attempted to merchant dupe.");
                    }
                } else {
                    c.getPlayer().dropMessage(1, "You do not have enough mesos.");
                }
            }
        }
    }

    @Override
    public void closeShop(final boolean saveItems) {
        map.removeMapObject(this);
        map.broadcastMessage(MaplePacketCreator.destroyHiredMerchant(getOwnerId()));
        try {
            final PreparedStatement ps =
                DatabaseConnection
                    .getConnection()
                    .prepareStatement(
                        "UPDATE characters SET HasMerchant = 0 WHERE id = ?"
                    );
            ps.setInt(1, getOwnerId());
            ps.executeUpdate();
            ps.close();
            tempItems(false, false);
            if (saveItems) {
                saveItems();
            }
        } catch (final SQLException sqle) {
            sqle.printStackTrace();
        }

        ChannelServer
            .getAllInstances()
            .stream()
            .map(cs -> cs.getPlayerStorage().getCharacterById(getOwnerId()))
            .filter(Objects::nonNull)
            .findAny()
            .ifPresent(owner -> owner.setHasMerchant(false, false));

        schedule.cancel(false);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(final boolean set) {
        open = set;
    }

    public MapleMap getMap() {
        return map;
    }

    public int getItemId() {
        return itemId;
    }

    @Override
    public void sendDestroyData(final MapleClient client) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.HIRED_MERCHANT;
    }

    @Override
    public void sendSpawnData(final MapleClient client) {
        client.getSession().write(MaplePacketCreator.spawnHiredMerchant(this));
    }
}
