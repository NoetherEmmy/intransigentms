package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.MTSItemInfo;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.HexTool;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class MTSHandler extends AbstractMaplePacketHandler {
    private static final Logger log = LoggerFactory.getLogger(MTSHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().inMTS()) return;
        if (slea.available() > 0) {
            byte op = slea.readByte();
            if (op == 2) { // Place item for sale.
                byte itemtype = slea.readByte();
                int itemid = slea.readInt();
                slea.readShort();
                slea.skip(7);
                short stars = 1;
                if (itemtype == 1) {
                    slea.skip(32); // Item stats, but we don't need them (can be exploited!).
                } else {
                    stars = slea.readShort();
                }
                slea.readMapleAsciiString(); // Owner.
                if (itemtype == 1) {
                    slea.readInt();
                } else {
                    slea.readShort();
                }
                byte slot = 1;
                short quantity = 1;

                if (itemtype != 1) {
                    if (itemid / 10000 == 207 || itemid / 10000 == 233) {
                        slea.skip(8);
                    }
                    slot = (byte) slea.readInt();
                } else {
                    quantity = (short) slea.readInt();
                }
                if (itemtype == 1) {
                    slea.readShort();
                }
                if (itemtype != 1) {
                    if (itemid / 10000 == 207 || itemid / 10000 == 233) {
                        quantity = stars;
                        slea.skip(4);
                    } else {
                        quantity = (short) slea.readInt();
                    }
                } else {
                    slot = (byte) slea.readInt();
                }
                if (itemtype == 1) {
                    slea.readInt();
                }
                int price = slea.readInt();
                if (itemtype == 1) {
                    quantity = 1;
                }
                if (price < 11 || price > 1001337 || quantity < 1) {
                    c.getSession().write(MaplePacketCreator.enableMTS());
                    return;
                }
                MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
                IItem i = c.getPlayer().getInventory(type).getItem(slot).copy();
                if (i.getQuantity() > 0 && c.getPlayer().getMeso() >= 1337) {
                    Connection con = DatabaseConnection.getConnection();
                    try {
                        PreparedStatement ps;

                        Calendar calendar = Calendar.getInstance();
                        int year;
                        int month;
                        int day;
                        int oldmax = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                        int oldday = calendar.get(Calendar.DAY_OF_MONTH) + 7;
                        if (oldmax < oldday) {
                            if (calendar.get(Calendar.MONTH) + 2 > 12) {
                                year = calendar.get(Calendar.YEAR) + 1;
                                month = 1;
                                calendar.set(year, month, 1);
                                day = oldday - oldmax;
                            } else {
                                month = calendar.get(Calendar.MONTH) + 2;
                                year = calendar.get(Calendar.YEAR);
                                calendar.set(year, month, 1);
                                day = oldday - oldmax;
                            }
                        } else {
                            day = calendar.get(Calendar.DAY_OF_MONTH) + 7;
                            month = calendar.get(Calendar.MONTH);
                            year = calendar.get(Calendar.YEAR);
                        }
                        String date = year + "-";
                        if (month < 10) {
                            date += "0" + month + "-";
                        } else {
                            date += month + "-";
                        }
                        if (day < 10) {
                            date += "0" + day;
                        } else {
                            date += day + "";
                        }
                        ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items WHERE seller = ?");
                        ps.setInt(1, c.getPlayer().getId());
                        ResultSet rs = ps.executeQuery();
                        rs.next();
                        if (rs.getInt(1) >= 10) {
                            c.getPlayer().dropMessage(1, "You cannot sell over 10 items");
                            c.getSession().write(MaplePacketCreator.enableActions());
                            rs.close();
                            ps.close();
                            return;
                        }
                        if (i.getType() == 2) {
                            Item item = (Item) i;
                            ps = con.prepareStatement(
                                "INSERT INTO mts_items (tab, type, itemid, quantity, seller, price, owner, sellername, sell_ends) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                            );
                            ps.setInt(1, 1);
                            ps.setInt(2, 2);
                            ps.setInt(3, item.getItemId());
                            ps.setInt(4, quantity);
                            ps.setInt(5, c.getPlayer().getId());
                            ps.setInt(6, price);
                            ps.setString(7, item.getOwner());
                            ps.setString(8, c.getPlayer().getName());
                            ps.setString(9, date);
                        } else {
                            Equip equip = (Equip) i;
                            ps = con.prepareStatement("INSERT INTO mts_items (tab, type, itemid, quantity, seller, price, upgradeslots, level, str, dex, `int`, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, locked, owner, sellername, sell_ends) VALUES " +
                            "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                            ps.setInt(1, 1);
                            ps.setInt(2, type.getType());
                            ps.setInt(3, equip.getItemId());
                            ps.setInt(4, quantity);
                            ps.setInt(5, c.getPlayer().getId());
                            ps.setInt(6, price);
                            ps.setInt(7, equip.getUpgradeSlots());
                            ps.setInt(8, equip.getLevel());
                            ps.setInt(9, equip.getStr());
                            ps.setInt(10, equip.getDex());
                            ps.setInt(11, equip.getInt());
                            ps.setInt(12, equip.getLuk());
                            ps.setInt(13, equip.getHp());
                            ps.setInt(14, equip.getMp());
                            ps.setInt(15, equip.getWatk());
                            ps.setInt(16, equip.getMatk());
                            ps.setInt(17, equip.getWdef());
                            ps.setInt(18, equip.getMdef());
                            ps.setInt(19, equip.getAcc());
                            ps.setInt(20, equip.getAvoid());
                            ps.setInt(21, equip.getHands());
                            ps.setInt(22, equip.getSpeed());
                            ps.setInt(23, equip.getJump());
                            ps.setInt(24, equip.getLocked());
                            ps.setString(25, equip.getOwner());
                            ps.setString(26, c.getPlayer().getName());
                            ps.setString(27, date);
                        }
                        ps.executeUpdate();
                        ps.close();
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                    } catch (SQLException e) {
                        log.error("SQLErr4: " + e);
                    }

                    c.getPlayer().gainMeso(-1337, false);
                    c.getSession().write(getMTS(1, 0, 0));
                    c.getSession().write(MaplePacketCreator.enableMTS());
                    c.getSession().write(MaplePacketCreator.MTSConfirmSell());
                    c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
                    c.getSession().write(MaplePacketCreator.NotYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                }
            } else if (op == 3) { // send offer for wanted item
            } else if (op == 4) { // list wanted item
                int itemid = slea.readInt();
                int price = slea.readInt();
                int quantity = slea.readInt();
                slea.readShort();
                String message = slea.readMapleAsciiString();
            } else if (op == 5) { // change page/tab
                //tabs
                //1 = for sale
                //2 = wanted
                //3 = auction
                //4 = my page
                //5 = guide
                //types (wanted & auction)
                //0 = all
                //1 = equip
                //2 = use
                //3 = setup
                //4 = etc
                //types (my page)
                //0 = cart
                //1 = offers
                //2 = history
                //3 = auction
                int tab = slea.readInt();
                int type = slea.readInt();
                int page = slea.readInt();
                c.getPlayer().changeTab(tab);
                c.getPlayer().changeType(type);
                c.getPlayer().changePage(page);
                if (tab == 4 && type == 0) {
                    c.getSession().write(getCart(c.getPlayer().getId()));
                } else {
                    c.getSession().write(getMTS(tab, type, page));
                }
                c.getSession().write(MaplePacketCreator.enableMTS());
                c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
                c.getSession().write(MaplePacketCreator.NotYetSoldInv(getNotYetSold(c.getPlayer().getId())));
            } else if (op == 6) { //search
                int tab = slea.readInt();
                int type = slea.readInt();
                slea.readInt();
                int ci = slea.readInt();

                String search = slea.readMapleAsciiString();
                c.getSession().write(MaplePacketCreator.enableMTS());
                c.getSession().write(MaplePacketCreator.enableActions());
                c.getSession().write(getMTSSearch(tab, type, ci, search, c.getPlayer().getCurrentPage()));
                c.getSession().write(MaplePacketCreator.showMTSCash(c.getPlayer()));
                c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
                c.getSession().write(MaplePacketCreator.NotYetSoldInv(getNotYetSold(c.getPlayer().getId())));
            } else if (op == 7) { //cancel sale
                int id = slea.readInt(); //id of the item
                Connection con = DatabaseConnection.getConnection();
                try {
                    PreparedStatement ps = con.prepareStatement("UPDATE mts_items SET transfer = 1 WHERE id = ? AND seller = ?");
                    ps.setInt(1, id);
                    ps.setInt(2, c.getPlayer().getId());
                    ps.executeUpdate();
                    ps.close();

                    ps = con.prepareStatement("DELETE FROM mts_cart WHERE itemid = ?");
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException e) {
                    log.error("SQLErr4: " + e);
                }
                c.getSession().write(MaplePacketCreator.enableMTS());
                c.getSession().write(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(), c.getPlayer().getCurrentPage()));
                c.getSession().write(MaplePacketCreator.NotYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
            } else if (op == 8) { //transfer item from transfer inv.
                int id = slea.readInt(); //id of the item
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps;
                ResultSet rs;
                try {
                    ps = con.prepareStatement("SELECT * FROM mts_items WHERE seller = ? AND transfer = 1  AND id= ? ORDER BY id DESC");
                    ps.setInt(1, c.getPlayer().getId());
                    ps.setInt(2, id);

                    rs = ps.executeQuery();
                    if (rs.next()) {
                        IItem i;
                        if (rs.getInt("type") != 1) {
                            Item ii = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                            ii.setOwner(rs.getString("owner"));
                            ii.setPosition(c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(rs.getInt("itemid"))).getNextFreeSlot());
                            i = ii.copy();
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
                            equip.setPosition(c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(rs.getInt("itemid"))).getNextFreeSlot());
                            i = equip.copy();
                        }

                        PreparedStatement pse = con.prepareStatement("DELETE FROM mts_items WHERE id = ? AND seller = ? AND transfer = 1");
                        pse.setInt(1, id);
                        pse.setInt(2, c.getPlayer().getId());
                        pse.executeUpdate();
                        pse.close();

                        MapleInventoryManipulator.addFromDrop(c, i, false);

                        c.getSession().write(MaplePacketCreator.enableMTS());
                        c.getSession().write(getCart(c.getPlayer().getId()));
                        c.getSession().write(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(), c.getPlayer().getCurrentPage()));
                        c.getSession().write(MaplePacketCreator.MTSConfirmTransfer(i.getQuantity(), i.getPosition()));
                        c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
                        c.getPlayer().saveToDB(true, true);
                    }
                    rs.close();
                    ps.close();
                } catch (SQLException e) {
                    log.error("Err8: " + e);
                }
            } else if (op == 9) { //add to cart
                int id = slea.readInt(); //id of the item
                Connection con = DatabaseConnection.getConnection();
                try {
                    PreparedStatement ps1 = con.prepareStatement("SELECT * FROM mts_items WHERE id = ? AND seller <> ?");
                    ps1.setInt(1, id);//Previene que agregues al cart tus propios items
                    ps1.setInt(2, c.getPlayer().getId());
                    ResultSet rs1 = ps1.executeQuery();
                    if (rs1.next()) {
                        PreparedStatement ps = con.prepareStatement("SELECT * FROM mts_cart WHERE cid = ? AND itemid = ?");
                        ps.setInt(1, c.getPlayer().getId());
                        ps.setInt(2, id);
                        ResultSet rs = ps.executeQuery();
                        if (!rs.next()) {
                            PreparedStatement pse = con.prepareStatement("INSERT INTO mts_cart (cid, itemid) VALUES (?, ?)");
                            pse.setInt(1, c.getPlayer().getId());
                            pse.setInt(2, id);
                            pse.executeUpdate();
                            rs.close();
                            pse.close();
                            ps.close();
                        //c.getSession().write(MaplePacketCreator.addToCartMessage(false, false));
                        } else {
                            rs.close();
                            ps.close();
                        }
                    // c.getSession().write(MaplePacketCreator.addToCartMessage(true, false));
                    }
                    rs1.close();
                    ps1.close();
                } catch (SQLException e) {
                    log.error("SqlErr12: ", e);
                }
            /*c.getSession().write(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(), c.getPlayer().getCurrentPage()));
            c.getSession().write(MaplePacketCreator.enableMTS());
            c.getSession().write(MaplePacketCreator.enableActions());
            c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
            c.getSession().write(MaplePacketCreator.NotYetSoldInv(getNotYetSold(c.getPlayer().getId())));*/
            } else if (op == 10) { //delete from cart
                int id = slea.readInt(); //id of the item
                Connection con = DatabaseConnection.getConnection();
                try {
                    PreparedStatement ps = con.prepareStatement("DELETE FROM mts_cart WHERE itemid = ? AND cid = ?");
                    ps.setInt(1, id);
                    ps.setInt(2, c.getPlayer().getId());
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException e) {
                    log.error("SqlErr12: ", e);
                }
            // c.getSession().write(MaplePacketCreator.addToCartMessage(false, true));
             /*   c.getSession().write(getCart(c.getPlayer().getId()));
            c.getSession().write(MaplePacketCreator.enableMTS());
            c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
            c.getSession().write(MaplePacketCreator.NotYetSoldInv(getNotYetSold(c.getPlayer().getId())));*/
            } else if (op == 12) { //put item up for auction
            } else if (op == 12) { //cancel wanted cart thing
            } else if (op == 14) { //buy auction item now
            } else if (op == 16) { //buy
                //transaction fees: 100NX + 10% of item price
                int id = slea.readInt(); //id of the item

                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps;
                ResultSet rs;
                try {
                    ps = con.prepareStatement("SELECT * FROM mts_items WHERE id = ? ORDER BY id DESC");
                    ps.setInt(1, id);

                    rs = ps.executeQuery();
                    if (rs.next()) {
                        int price = rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1d); // Taxes
                        if (c.getPlayer().getCSPoints(1) >= price) {
                            boolean alwaysnull = true;
                            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                                MapleCharacter victim = cserv.getPlayerStorage().getCharacterById(rs.getInt("seller"));
                                if (victim != null) {
                                    victim.modifyCSPoints(1, rs.getInt("price"));
                                    alwaysnull = false;
                                }
                            }
                            if (alwaysnull) {
                                PreparedStatement pse = con.prepareStatement("SELECT accountid FROM characters WHERE id = ?");
                                pse.setInt(1, rs.getInt("seller"));
                                ResultSet rse = pse.executeQuery();
                                if (rse.next()) {
                                    PreparedStatement psee = con.prepareStatement("UPDATE accounts SET paypalNX = paypalNX + ? WHERE id = ?");
                                    psee.setInt(1, rs.getInt("price"));
                                    psee.setInt(2, rse.getInt("accountid"));
                                    psee.executeUpdate();
                                    psee.close();
                                }
                                rse.close();
                                pse.close();
                            }
                            PreparedStatement pse = con.prepareStatement("UPDATE mts_items SET seller = ?, transfer = 1 WHERE id = ?");
                            pse.setInt(1, c.getPlayer().getId());
                            pse.setInt(2, id);
                            pse.executeUpdate();
                            pse.close();

                            pse = con.prepareStatement("DELETE FROM mts_cart WHERE itemid = ?");
                            pse.setInt(1, id);
                            pse.executeUpdate();
                            pse.close();

                            c.getPlayer().modifyCSPoints(1, -price);
                            c.getSession().write(MaplePacketCreator.enableMTS());
                            c.getSession().write(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(), c.getPlayer().getCurrentPage()));
                            c.getSession().write(MaplePacketCreator.MTSConfirmBuy());
                            c.getSession().write(MaplePacketCreator.showMTSCash(c.getPlayer()));
                            c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
                            c.getSession().write(MaplePacketCreator.NotYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                            c.getSession().write(MaplePacketCreator.enableActions());
                        } else {
                            c.getSession().write(MaplePacketCreator.MTSFailBuy());
                        }
                    }
                    rs.close();
                    ps.close();
                } catch (SQLException e) {
                    c.getSession().write(MaplePacketCreator.MTSFailBuy());
                    log.error("Err8: " + e);
                }
            } else if (op == 17) { //buy from cart
                int id = slea.readInt(); //id of the item

                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps;
                ResultSet rs;
                try {
                    ps = con.prepareStatement("SELECT * FROM mts_items WHERE id = ? ORDER BY id DESC");
                    ps.setInt(1, id);

                    rs = ps.executeQuery();
                    if (rs.next()) {
                        int price = rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1d);
                        if (c.getPlayer().getCSPoints(1) >= price) {
                            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                                MapleCharacter victim = cserv.getPlayerStorage().getCharacterById(rs.getInt("seller"));
                                if (victim != null) {
                                    victim.modifyCSPoints(1, rs.getInt("price"));
                                } else {
                                    PreparedStatement pse = con.prepareStatement("SELECT accountid FROM characters WHERE id = ?");
                                    pse.setInt(1, rs.getInt("seller"));
                                    ResultSet rse = pse.executeQuery();
                                    if (rse.next()) {
                                        PreparedStatement psee = con.prepareStatement("UPDATE accounts SET paypalNX = paypalNX + ? WHERE id = ?");
                                        psee.setInt(1, rs.getInt("price"));
                                        psee.setInt(2, rse.getInt("accountid"));
                                        psee.executeUpdate();
                                        psee.close();
                                    }
                                    rse.close();
                                    pse.close();
                                }
                            }

                            PreparedStatement pse = con.prepareStatement("UPDATE mts_items SET seller = ?, transfer = 1 WHERE id = ?");
                            pse.setInt(1, c.getPlayer().getId());
                            pse.setInt(2, id);
                            pse.executeUpdate();
                            pse.close();

                            pse = con.prepareStatement("DELETE FROM mts_cart WHERE itemid = ?");
                            pse.setInt(1, id);
                            pse.executeUpdate();
                            pse.close();

                            c.getPlayer().modifyCSPoints(1, -price);
                            c.getSession().write(getCart(c.getPlayer().getId()));
                            c.getSession().write(MaplePacketCreator.enableMTS());
                            c.getSession().write(MaplePacketCreator.MTSConfirmBuy());
                            c.getSession().write(MaplePacketCreator.showMTSCash(c.getPlayer()));
                            c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
                            c.getSession().write(MaplePacketCreator.NotYetSoldInv(getNotYetSold(c.getPlayer().getId())));
                        } else {
                            c.getSession().write(MaplePacketCreator.MTSFailBuy());
                        }
                    }
                    rs.close();
                    ps.close();
                } catch (SQLException e) {
                    c.getSession().write(MaplePacketCreator.MTSFailBuy());
                    log.error("Err8: " + e);
                }
            } else {
                log.info("Unhandled OP(MTS): " + op + " Packet:\n" + HexTool.toString(slea));
            }
        } else {
            c.getSession().write(MaplePacketCreator.showMTSCash(c.getPlayer()));
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

    public MaplePacket getCart(int cid) {
        List<MTSItemInfo> items = new ArrayList<>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        ResultSet rs;
        int pages = 0;
        try {
            ps = con.prepareStatement("SELECT * FROM mts_cart WHERE cid = ? ORDER BY id DESC");
            ps.setInt(1, cid);

            rs = ps.executeQuery();
            while (rs.next()) {
                PreparedStatement pse = con.prepareStatement("SELECT * FROM mts_items WHERE id = ?");
                pse.setInt(1, rs.getInt("itemid"));
                ResultSet rse = pse.executeQuery();
                if (rse.next()) {
                    if (rse.getInt("type") != 1) {
                        Item i = new Item(rse.getInt("itemid"), (byte) 0, (short) rse.getInt("quantity"));
                        i.setOwner(rse.getString("owner"));
                        items.add(new MTSItemInfo(i, rse.getInt("price"), rse.getInt("id"), rse.getInt("seller"), rse.getString("sellername"), rse.getString("sell_ends")));
                    } else {
                        Equip equip = new Equip(rse.getInt("itemid"), (byte) rse.getInt("position"), -1);
                        equip.setOwner(rse.getString("owner"));
                        equip.setQuantity((short) 1);
                        equip.setAcc((short) rse.getInt("acc"));
                        equip.setAvoid((short) rse.getInt("avoid"));
                        equip.setDex((short) rse.getInt("dex"));
                        equip.setHands((short) rse.getInt("hands"));
                        equip.setHp((short) rse.getInt("hp"));
                        equip.setInt((short) rse.getInt("int"));
                        equip.setJump((short) rse.getInt("jump"));
                        equip.setLuk((short) rse.getInt("luk"));
                        equip.setMatk((short) rse.getInt("matk"));
                        equip.setMdef((short) rse.getInt("mdef"));
                        equip.setMp((short) rse.getInt("mp"));
                        equip.setSpeed((short) rse.getInt("speed"));
                        equip.setStr((short) rse.getInt("str"));
                        equip.setWatk((short) rse.getInt("watk"));
                        equip.setWdef((short) rse.getInt("wdef"));
                        equip.setUpgradeSlots((byte) rse.getInt("upgradeslots"));
                        equip.setLocked((byte) rse.getInt("locked"));
                        equip.setLevel((byte) rse.getInt("level"));
                        items.add(new MTSItemInfo(equip, rse.getInt("price"), rse.getInt("id"), rse.getInt("seller"), rse.getString("sellername"), rse.getString("sell_ends")));
                    }
                }
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT COUNT(*) FROM mts_cart WHERE cid = ?");
            ps.setInt(1, cid);
            rs = ps.executeQuery();
            if (rs.next()) {
                pages = rs.getInt(1) / 16;
                if (rs.getInt(1) % 16 > 0) {
                    pages += 1;
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            log.error("Err8: " + e);
        }
        return MaplePacketCreator.sendMTS(items, 4, 0, 0, pages);
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

    public MaplePacket getMTS(int tab, int type, int page) {
        List<MTSItemInfo> items = new ArrayList<>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        ResultSet rs;
        int pages = 0;
        try {
            if (type != 0) {
                ps = con.prepareStatement("SELECT * FROM mts_items WHERE tab = ? AND type = ? AND transfer = 0 ORDER BY id DESC LIMIT ?, 16");
            } else {
                ps = con.prepareStatement("SELECT * FROM mts_items WHERE tab = ? AND transfer = 0 ORDER BY id DESC LIMIT ?, 16");
            }
            ps.setInt(1, tab);
            if (type != 0) {
                ps.setInt(2, type);
                ps.setInt(3, page * 16);
            } else {
                ps.setInt(2, page * 16);
            }
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

            if (type != 0) {
                ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items WHERE tab = ? AND type = ? AND transfer = 0");
            } else {
                ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items WHERE tab = ? AND transfer = 0");
            }
            ps.setInt(1, tab);
            if (type != 0) {
                ps.setInt(2, type);
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                pages = rs.getInt(1) / 16;
                if (rs.getInt(1) % 16 > 0) {
                    pages += 1;
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            log.error("Err6: " + e);
        }
        return MaplePacketCreator.sendMTS(items, tab, type, page, pages);
    }

    public MaplePacket getMTSSearch(int tab, int type, int cOi, String search, int page) {
        List<MTSItemInfo> items = new ArrayList<>();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        String listaitems = "";
        if (MapleCharacterUtil.hasSymbols(search) || search.length() > 10) {
            search = "disable";
        }
        if (cOi != 0) {
            List<String> retItems = new ArrayList<>();

            for (Map.Entry<Integer, String> itemEntry : ii.getAllItems().entrySet()) {
                if (itemEntry.getValue().toLowerCase().contains(search.toLowerCase())) {
                    retItems.add(" itemid=" + itemEntry.getKey() + " OR ");
                }
            }
            listaitems += " AND (";
            if (!retItems.isEmpty()) {
                for (String singleRetItem : retItems) {
                    listaitems += singleRetItem;
                }
                listaitems += " itemid=0 )";
            }
        } else {
            listaitems = " AND sellername LIKE CONCAT('%','" + search + "', '%')";
        }

        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        ResultSet rs;
        int pages = 0;
        try {
            if (type != 0) {
                ps = con.prepareStatement(
                    "SELECT * FROM mts_items WHERE tab = ? " +
                        listaitems +
                        " AND type = ? AND transfer = 0 ORDER BY id DESC LIMIT ?, 16"
                );
            } else {
                ps = con.prepareStatement(
                    "SELECT * FROM mts_items WHERE tab = ? " +
                        listaitems +
                        " AND transfer = 0 ORDER BY id DESC LIMIT ?, 16"
                );
            }
            ps.setInt(1, tab);
            if (type != 0) {
                ps.setInt(2, type);
                ps.setInt(3, page * 16);
            } else {
                ps.setInt(2, page * 16);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") != 1) {
                    Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    i.setOwner(rs.getString("owner"));
                    items.add(
                        new MTSItemInfo(
                            i,
                            rs.getInt("price"),
                            rs.getInt("id"),
                            rs.getInt("seller"),
                            rs.getString("sellername"),
                            rs.getString("sell_ends")
                        )
                    );
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
                    items.add(
                        new MTSItemInfo(
                            equip,
                            rs.getInt("price"),
                            rs.getInt("id"),
                            rs.getInt("seller"),
                            rs.getString("sellername"),
                            rs.getString("sell_ends")
                        )
                    );
                }
            }
            rs.close();
            ps.close();
            if (type != 0) {
                ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM mts_items WHERE tab = ? " +
                        listaitems +
                        " AND type = ? AND transfer = 0"
                );
            } else {
                ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM mts_items WHERE tab = ? " +
                        listaitems +
                        " AND transfer = 0"
                );
            }
            ps.setInt(1, tab);
            if (type != 0) ps.setInt(2, type);
            rs = ps.executeQuery();
            if (rs.next()) {
                pages = rs.getInt(1) / 16;
                if (rs.getInt(1) % 16 > 0) pages += 1;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            log.error("Err6: " + e);
        }
        return MaplePacketCreator.sendMTS(items, tab, type, page, pages);
    }
}
