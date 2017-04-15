package net.sf.odinms.server;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapleTrade {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MapleTrade.class);
    private MapleTrade partner;
    private final List<IItem> items = new ArrayList<>();
    private List<IItem> exchangeItems;
    private int meso, exchangeMeso;
    boolean visited = false, locked = false;
    private final MapleCharacter chr;
    private final byte number;

    public MapleTrade(final byte number, final MapleCharacter c) {
        chr = c;
        this.number = number;
    }

    private int getFee(final int meso) {
        final int fee = 0;
        if (meso >= 10000000) {
            return (int) Math.round(0.04d * meso);
        } else if (meso >= 5000000) {
            return (int) Math.round(0.03d * meso);
        } else if (meso >= 1000000) {
            return (int) Math.round(0.02d * meso);
        } else if (meso >= 100000) {
            return (int) Math.round(0.01d * meso);
        } else if (meso >= 50000) {
            return (int) Math.round(0.005d * meso);
        }
        return fee;
    }

    public void lock() {
        locked = true;
        //chr.getClient().getSession().write(MaplePacketCreator.getTradeConfirmation()); // own side shouldn't see other side whited
        partner.getChr().getClient().getSession().write(MaplePacketCreator.getTradeConfirmation());
    }

    public void complete1() {
        exchangeItems = partner.getItems();
        exchangeMeso = partner.getMeso();
    }

    public void complete2() {
        items.clear();
        meso = 0;
        for (final IItem item : exchangeItems) {
            MapleInventoryManipulator.addFromDrop(chr.getClient(), item, false);
        }
        if (exchangeMeso > 0) {
            chr.gainMeso(exchangeMeso - getFee(exchangeMeso), false, true, false);
        }
        exchangeMeso = 0;
        if (exchangeItems != null) {
            exchangeItems.clear();
        }
        chr.getClient().getSession().write(MaplePacketCreator.getTradeCompletion(number));
    }

    public void cancel() {
        for (final IItem item : items) {
            MapleInventoryManipulator.addFromDrop(chr.getClient(), item, false);
        }
        if (meso > 0) {
            chr.gainMeso(meso, false, true, false);
        }
        // Just to be on the safe side...
        meso = 0;
        items.clear();
        exchangeMeso = 0;
        if (exchangeItems != null) exchangeItems.clear();
        chr.getClient().getSession().write(MaplePacketCreator.getTradeCancel(number));
    }

    public boolean isLocked() {
        return locked;
    }

    public int getMeso() {
        return meso;
    }

    public void setMeso(final int meso) {
        if (locked) throw new RuntimeException("Trade is locked.");
        if (meso < 0) {
            log.info("[h4x] {} Trying to trade < 0 meso", chr.getName());
            return;
        }
        if (chr.getMeso() >= meso) {
            chr.gainMeso(-meso, false, true, false);
            this.meso += meso;
            chr.getClient().getSession().write(MaplePacketCreator.getTradeMesoSet((byte) 0, this.meso));
            if (partner != null) {
                partner.getChr().getClient().getSession().write(MaplePacketCreator.getTradeMesoSet((byte) 1, this.meso));
            }
        } else {
            AutobanManager.getInstance().addPoints(chr.getClient(), 1000, 0, "Trying to trade more mesos than in possession");
        }
    }

    public void addItem(final IItem item) {
        items.add(item);
        chr.getClient().getSession().write(MaplePacketCreator.getTradeItemAdd((byte) 0, item));
        if (partner != null) {
            partner.getChr().getClient().getSession().write(MaplePacketCreator.getTradeItemAdd((byte) 1, item));
        }
    }

    public void chat(final String message) {
        chr.getClient().getSession().write(MaplePacketCreator.shopChat(chr.getName() + " : " + message, 0));
        if (partner != null) {
            partner.getChr().getClient().getSession().write(MaplePacketCreator.shopChat(chr.getName() + " : " + message, 1));
        }
    }

    public MapleTrade getPartner() {
        return partner;
    }

    public void setPartner(final MapleTrade partner) {
        if (locked) {
            throw new RuntimeException("Trade is locked.");
        }
        this.partner = partner;
    }

    /*
    private void broadcast(MaplePacket packet) {
        chr.getClient().getSession().write(packet);
        if (partner != null) {
            partner.getChr().getClient().getSession().write(packet);
        }
    }
    */

    public MapleCharacter getChr() {
        return chr;
    }

    public List<IItem> getItems() {
        return new ArrayList<>(items);
    }

    public boolean fitsInInventory() {
        final MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        final Map<MapleInventoryType, Integer> neededSlots = new LinkedHashMap<>(4 * exchangeItems.size() / 3);
        for (final IItem item : exchangeItems) {
            final MapleInventoryType type = mii.getInventoryType(item.getItemId());
            neededSlots.merge(type, 1, (a, b) -> a + b);
        }
        for (final Map.Entry<MapleInventoryType, Integer> entry : neededSlots.entrySet()) {
            if (chr.getInventory(entry.getKey()).isFull(entry.getValue() - 1)) {
                return false;
            }
        }
        return true;
    }

    public void setVisited(final boolean visited) {
        this.visited = visited;
    }

    public boolean isVisited() {
        return visited;
    }

    public static void completeTrade(final MapleCharacter c) {
        c.getTrade().lock();
        final MapleTrade local = c.getTrade();
        final MapleTrade partner = local.getPartner();
        if (partner.isLocked()) {
            local.complete1();
            partner.complete1();
            // check for full inventories
            if (!local.fitsInInventory() || !partner.fitsInInventory()) {
                cancelTrade(c);
                c.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "There is not enough inventory space to complete the trade."));
                partner.getChr().getClient().getSession().write(MaplePacketCreator.serverNotice(5, "There is not enough inventory space to complete the trade."));
                return;
            }
            local.complete2();
            partner.complete2();
            partner.getChr().setTrade(null);
            c.setTrade(null);
        }
    }

    public static void cancelTrade(final MapleCharacter c) {
        c.getTrade().cancel();
        if (c.getTrade().getPartner() != null) {
            c.getTrade().getPartner().cancel();
            c.getTrade().getPartner().getChr().setTrade(null);
        }
        c.setTrade(null);
    }

    public static void startTrade(final MapleCharacter c) {
        if (c.getTrade() == null) {
            c.setTrade(new MapleTrade((byte) 0, c));
            c.getClient().getSession().write(MaplePacketCreator.getTradeStart(c.getClient(), c.getTrade(), (byte) 0));
        } else {
            c.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "You are already in a trade"));
        }
    }

    public static void inviteTrade(final MapleCharacter c1, final MapleCharacter c2) {
        if (c2.getTrade() == null) {
            c2.setTrade(new MapleTrade((byte) 1, c2));
            c2.getTrade().setPartner(c1.getTrade());
            c1.getTrade().setPartner(c2.getTrade());
            c2.getClient().getSession().write(MaplePacketCreator.getTradeInvite(c1));
        } else {
            c1.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "The other player is already trading with someone else."));
            cancelTrade(c1);
        }
    }

    public static synchronized void visitTrade(final MapleCharacter c1, final MapleCharacter c2) {
        if (
            c1.getTrade() != null &&
            !c1.getTrade().isVisited() &&
            c1.getTrade().getPartner().equals(c2.getTrade()) &&
            c2.getTrade() != null &&
            !c2.getTrade().isVisited() &&
            c2.getTrade().getPartner().equals(c1.getTrade())
        ) {
            c1.getTrade().setVisited(true);
            c2.getTrade().setVisited(true);
            c2.getClient().getSession().write(
                MaplePacketCreator.getTradePartnerAdd(c1)
            );
            c1.getClient().getSession().write(
                MaplePacketCreator.getTradeStart(
                    c1.getClient(),
                    c1.getTrade(),
                    (byte) 1
                )
            );
        } else {
            c1.getClient().getSession().write(
                MaplePacketCreator.serverNotice(
                    5,
                    "The other player has already closed the trade."
                )
            );
        }
    }

    public static void declineTrade(final MapleCharacter c) {
        final MapleTrade trade = c.getTrade();
        if (trade != null) {
            if (trade.getPartner() != null) {
                final MapleCharacter other = trade.getPartner().getChr();
                other.getTrade().cancel();
                other.setTrade(null);
                other.getClient().getSession().write(
                        MaplePacketCreator.serverNotice(5, c.getName() + " has declined your trade request"));

            }
            trade.cancel();
            c.setTrade(null);
        }
    }
}
