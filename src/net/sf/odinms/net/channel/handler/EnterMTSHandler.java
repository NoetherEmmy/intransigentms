package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.Equip;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.server.MTSItemInfo;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.server.maps.SavedLocationType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EnterMTSHandler extends AbstractMaplePacketHandler {
    private static final Logger log = LoggerFactory.getLogger(DistributeSPHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getClient().getChannelServer().MTtoFM()) {
            if (!(c.getPlayer().isAlive()) || c.getPlayer().getMapId() == 100) {
                c.getPlayer().dropMessage("You can't enter the FM when you are dead.");
            } else if (c.getPlayer().getMapId() >= 1000 && c.getPlayer().getMapId() <= 1006) {
                c.getPlayer().dropMessage("You can't enter the FM while in a Monster Trial.");
            } else if (c.getPlayer().getMapId() == 2000) {
                c.getPlayer().dropMessage("You can't enter the FM from The Back Room. If you've fallen off the map, use @mapfix.");
            } else if (c.getPlayer().getMapId() == 3000) {
                c.getPlayer().dropMessage("You can't enter the FM while in a Boss Map.");
            } else if (c.getPlayer().getMap().isPQMap()) {
                c.getPlayer().dropMessage("You can't enter the FM while in a party quest.");
            } else if (c.getPlayer().getMapId() == 105) {
                c.getPlayer().dropMessage("You can't enter the FM while in someone's mind.");
            } else if (c.getPlayer().getMapId() != 910000000) {
                c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET);
                c.getPlayer().changeMap(910000000);
            }
            c.getSession().write(MaplePacketCreator.enableActions());
        } else {
            if (c.getPlayer().getNoPets() > 0) {
                c.getPlayer().unequipAllPets();
            }
            for (FakeCharacter fc : c.getPlayer().getFakeChars()) {
                c.getPlayer().getMap().removePlayer(fc.getFakeChar());
            }
            if (c.getPlayer().getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            }
            try {
                WorldChannelInterface wci = ChannelServer.getInstance(c.getChannel()).getWorldInterface();
                wci.addBuffsToStorage(c.getPlayer().getId(), c.getPlayer().getAllBuffs());
                wci.addCooldownsToStorage(c.getPlayer().getId(), c.getPlayer().getAllCooldowns());
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }

            c.getPlayer().getMap().removePlayer(c.getPlayer());
            c.getSession().write(MaplePacketCreator.warpMTS(c));
            c.getPlayer().setInMTS(true);
            c.getSession().write(MaplePacketCreator.enableMTS());
            c.getSession().write(MaplePacketCreator.MTSWantedListingOver(0, 0));
            c.getSession().write(MaplePacketCreator.showMTSCash(c.getPlayer()));

            List<MTSItemInfo> items = new ArrayList<>();
            int pages = 0;
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM mts_items WHERE tab = 1 AND transfer = 0 ORDER BY id DESC LIMIT ?, 16");
                ps.setInt(1, 0);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getInt("type") != 1) {
                        Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                        i.setOwner(rs.getString("owner"));
                        items.add(new MTSItemInfo(i, rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1d), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                    } else {
                        Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                        equip.setOwner(rs.getString("owner"));
                        equip.setQuantity((short) 1);
                        equip.setAcc((short) rs.getInt("acc"));
                        equip.setAvoid((short) rs.getInt("avoid"));
                        equip.setDex((short) rs.getInt("dex"));
                        equip.setHands((short) rs.getInt("hands"));
                        equip.setHp((short) rs.getInt("hp"));
                        equip.setInt((short) rs.getInt("int"));
                        equip.setJump((short) rs.getInt("jump"));
                        equip.setLuk((short) rs.getInt("luk"));
                        equip.setMatk((short) rs.getInt("matk"));
                        equip.setMdef((short) rs.getInt("mdef"));
                        equip.setMp((short) rs.getInt("mp"));
                        equip.setSpeed((short) rs.getInt("speed"));
                        equip.setStr((short) rs.getInt("str"));
                        equip.setWatk((short) rs.getInt("watk"));
                        equip.setWdef((short) rs.getInt("wdef"));
                        equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                        equip.setLocked((byte) rs.getInt("locked"));
                        equip.setLevel((byte) rs.getInt("level"));
                        items.add(new MTSItemInfo(equip, rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1d), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                    }
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items");
                rs = ps.executeQuery();
                if (rs.next()) {
                    pages = (int) Math.ceil(rs.getInt(1) / 16);
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                log.error("Err1: " + e);
            }

            c.getSession().write(MaplePacketCreator.sendMTS(items, 1, 0, 0, pages));
            c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
            c.getSession().write(MaplePacketCreator.NotYetSoldInv(getNotYetSold(c.getPlayer().getId())));
            c.getPlayer().saveToDB(true, true);
        }
    }

    public List<MTSItemInfo> getNotYetSold(int cid) {
        List<MTSItemInfo> items = new ArrayList<>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        ResultSet rs;
        try {
            ps = con.prepareStatement("SELECT * FROM mts_items WHERE seller = ? AND transfer = 0 ORDER BY id DESC");
            ps.setInt(1, cid);

            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") != 1) {
                    Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    i.setOwner(rs.getString("owner"));
                    items.add(new MTSItemInfo(i, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                } else {
                    Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                    equip.setOwner(rs.getString("owner"));
                    equip.setQuantity((short) 1);
                    equip.setAcc((short) rs.getInt("acc"));
                    equip.setAvoid((short) rs.getInt("avoid"));
                    equip.setDex((short) rs.getInt("dex"));
                    equip.setHands((short) rs.getInt("hands"));
                    equip.setHp((short) rs.getInt("hp"));
                    equip.setInt((short) rs.getInt("int"));
                    equip.setJump((short) rs.getInt("jump"));
                    equip.setLuk((short) rs.getInt("luk"));
                    equip.setMatk((short) rs.getInt("matk"));
                    equip.setMdef((short) rs.getInt("mdef"));
                    equip.setMp((short) rs.getInt("mp"));
                    equip.setSpeed((short) rs.getInt("speed"));
                    equip.setStr((short) rs.getInt("str"));
                    equip.setWatk((short) rs.getInt("watk"));
                    equip.setWdef((short) rs.getInt("wdef"));
                    equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                    equip.setLocked((byte) rs.getInt("locked"));
                    equip.setLevel((byte) rs.getInt("level"));
                    items.add(new MTSItemInfo(equip, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            log.error("Err8: " + e);
        }
        return items;
    }

    public List<MTSItemInfo> getTransfer(int cid) {
        List<MTSItemInfo> items = new ArrayList<>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        ResultSet rs;
        try {
            ps = con.prepareStatement("SELECT * FROM mts_items WHERE transfer = 1 AND seller = ? ORDER BY id DESC");
            ps.setInt(1, cid);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") != 1) {
                    Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    i.setOwner(rs.getString("owner"));
                    items.add(new MTSItemInfo(i, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                } else {
                    Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                    equip.setOwner(rs.getString("owner"));
                    equip.setQuantity((short) 1);
                    equip.setAcc((short) rs.getInt("acc"));
                    equip.setAvoid((short) rs.getInt("avoid"));
                    equip.setDex((short) rs.getInt("dex"));
                    equip.setHands((short) rs.getInt("hands"));
                    equip.setHp((short) rs.getInt("hp"));
                    equip.setInt((short) rs.getInt("int"));
                    equip.setJump((short) rs.getInt("jump"));
                    equip.setLuk((short) rs.getInt("luk"));
                    equip.setMatk((short) rs.getInt("matk"));
                    equip.setMdef((short) rs.getInt("mdef"));
                    equip.setMp((short) rs.getInt("mp"));
                    equip.setSpeed((short) rs.getInt("speed"));
                    equip.setStr((short) rs.getInt("str"));
                    equip.setWatk((short) rs.getInt("watk"));
                    equip.setWdef((short) rs.getInt("wdef"));
                    equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                    equip.setLocked((byte) rs.getInt("locked"));
                    equip.setLevel((byte) rs.getInt("level"));
                    items.add(new MTSItemInfo(equip, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            log.error("Err7: " + e);
        }
        return items;
    }
}
