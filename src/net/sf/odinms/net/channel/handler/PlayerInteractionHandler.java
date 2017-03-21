package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.PlayerInteraction.*;
import net.sf.odinms.server.PlayerInteraction.MapleMiniGame.MiniGameType;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.StringUtil;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.Arrays;

public class PlayerInteractionHandler extends AbstractMaplePacketHandler {
    private enum Action {
        CREATE(0),
        INVITE(2),
        DECLINE(3),
        VISIT(4),
        CHAT(6),
        EXIT(0xA),
        OPEN(0xB),
        SET_ITEMS(0xE),
        SET_MESO(0xF),
        CONFIRM(0x10),
        ADD_ITEM(0x14),
        BUY(0x15),
        REMOVE_ITEM(0x19),
        BAN_PLAYER(0x1A),
        PUT_ITEM(0x1F),
        MERCHANT_BUY(0x20),
        TAKE_ITEM_BACK(0x24),
        MAINTENANCE_OFF(0x25),
        MERCHANT_ORGANIZE(0x26),
        CLOSE_MERCHANT(0x27),
        REQUEST_TIE(44),
        ANSWER_TIE(45),
        GIVE_UP(46),
        EXIT_AFTER_GAME(50),
        CANCEL_EXIT(51),
        READY(52),
        UN_READY(53),
        MOVE_OMOK(58),
        START(55),
        SKIP(57),
        SELECT_CARD(62);
        final byte code;

        Action(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //System.out.println(slea);
        byte mode = slea.readByte();
        if (mode == Action.CREATE.getCode()) {
            byte createType = slea.readByte();
            if (createType == 3) { // Trade
                MapleTrade.startTrade(c.getPlayer());
            } else {
                if (c.getPlayer().getChalkboard() != null) return;
                if (createType == 1 || createType == 2) {
                    String desc = slea.readMapleAsciiString();
                    String pass = null; // ???
                    if (slea.readByte() == 1) {
                        pass = slea.readMapleAsciiString(); // ???
                    }
                    int type = slea.readByte();
                    IPlayerInteractionManager game = new MapleMiniGame(c.getPlayer(), type, desc);
                    c.getPlayer().setInteraction(game);
                    if (createType == 1) {
                        ((MapleMiniGame) game).setGameType(MiniGameType.OMOK);
                    } else {
                        if (type == 0) {
                            ((MapleMiniGame) game).setMatchesToWin(6);
                        } else if (type == 1) {
                            ((MapleMiniGame) game).setMatchesToWin(10);
                        } else if (type == 2) {
                            ((MapleMiniGame) game).setMatchesToWin(15);
                        }
                        ((MapleMiniGame) game).setGameType(MiniGameType.MATCH_CARDS);
                    }
                    c.getPlayer().getMap().addMapObject((PlayerInteractionManager) game);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendInteractionBox(c.getPlayer()));
                } else if (createType == 4 || createType == 5) { // Shops
                    if (!c.getPlayer().hasMerchant() && c.getPlayer().tempHasItems()) {
                        c.getPlayer().dropMessage(1, "Speak with Frederick to get your saved items.");
                        return;
                    }
                    if (
                        !c.getPlayer().getMap().getMapObjectsInRange(
                            c.getPlayer().getPosition(),
                            19500.0d,
                            Arrays.asList(
                                MapleMapObjectType.SHOP,
                                MapleMapObjectType.HIRED_MERCHANT
                            )
                        ).isEmpty() ||
                        c.getPlayer().getMapId() < 910000001 ||
                        c.getPlayer().getMapId() > 910000022
                    ) {
                        c.getPlayer().dropMessage(1, "You may not establish a store here.");
                        return;
                    }
                    String desc = slea.readMapleAsciiString();
                    slea.skip(3);
                    int itemId = slea.readInt();
                    IPlayerInteractionManager shop;
                    if (c.getPlayer().haveItem(itemId, 1, false, true)) {
                        if (createType == 4) {
                            shop = new MaplePlayerShop(c.getPlayer(), itemId, desc);
                        } else {
                            shop = new HiredMerchant(c.getPlayer(), itemId, desc);
                        }
                        c.getPlayer().setInteraction(shop);
                        c.getSession().write(MaplePacketCreator.getInteraction(c.getPlayer(), true));
                    } else {
                        AutobanManager
                            .getInstance()
                            .autoban(
                                c,
                                "Merchant Shop: Attempt to open a shop without the item."
                            );
                    }
                } else {
                    System.err.println(
                        "Unhandled PLAYER_INTERACTION packet:\n" +
                            slea.getBytesRead() +
                            "\n" +
                            createType
                    );
                }
            }
        } else if (mode == Action.INVITE.getCode()) {
            int otherPlayer = slea.readInt();
            MapleCharacter otherChar = c.getPlayer().getMap().getCharacterById(otherPlayer);
            MapleTrade.inviteTrade(c.getPlayer(), otherChar);
        } else if (mode == Action.DECLINE.getCode()) {
            MapleTrade.declineTrade(c.getPlayer());
        } else if (mode == Action.VISIT.getCode()) {
            if (c.getPlayer().getTrade() != null && c.getPlayer().getTrade().getPartner() != null) {
                MapleTrade.visitTrade(c.getPlayer(), c.getPlayer().getTrade().getPartner().getChr());
            } else {
                int oid = slea.readInt();
                MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
                if (ob instanceof IPlayerInteractionManager && c.getPlayer().getInteraction() == null) {
                    IPlayerInteractionManager ips = (IPlayerInteractionManager) ob;
                    if (ips.getShopType() == 1) {
                        HiredMerchant merchant = (HiredMerchant) ips;
                        if (merchant.isOwner(c.getPlayer())) {
                            merchant.setOpen(false);
                            merchant.broadcast(MaplePacketCreator.shopErrorMessage(0x0D, 1), false);
                            merchant.removeAllVisitors((byte) 16, (byte) 0);
                            c.getPlayer().setInteraction(ips);
                            c.getSession().write(MaplePacketCreator.getInteraction(c.getPlayer(), false));
                            return;
                        } else if (!merchant.isOpen()) {
                            c.getPlayer().dropMessage(1, "This shop is in maintenance, please come by later.");
                            return;
                        }
                    } else if (ips.getShopType() == 2) {
                        if (((MaplePlayerShop) ips).isBanned(c.getPlayer().getName())) {
                            c.getPlayer().dropMessage(1, "You have been banned from this store.");
                            return;
                        }
                    }
                    if (ips.getFreeSlot() == -1) {
                        c.getSession().write(MaplePacketCreator.getMiniBoxFull());
                        return;
                    }
                    c.getPlayer().setInteraction(ips);
                    ips.addVisitor(c.getPlayer());
                    c.getSession().write(MaplePacketCreator.getInteraction(c.getPlayer(), false));
                }
            }
        } else if (mode == Action.CHAT.getCode()) { // Chat
            if (c.getPlayer().getTrade() != null) {
                c.getPlayer().getTrade().chat(
                    StringUtil.cleanForClientDisplay(
                        slea.readMapleAsciiString(),
                        256
                    )
                );
            } else if (c.getPlayer().getInteraction() != null) {
                IPlayerInteractionManager ips = c.getPlayer().getInteraction();
                String message = StringUtil.cleanForClientDisplay(slea.readMapleAsciiString(), 256);
                CommandProcessor.getInstance().processCommand(c, message);
                ips.broadcast(
                    MaplePacketCreator.shopChat(
                        c.getPlayer().getName() +
                            " : " +
                            message,
                        ips.isOwner(c.getPlayer()) ? 0 : ips.getVisitorSlot(c.getPlayer()) + 1
                    ),
                    true
                );
            }
        } else if (mode == Action.EXIT.getCode()) {
            if (c.getPlayer().getTrade() != null) {
                MapleTrade.cancelTrade(c.getPlayer());
            } else {
                IPlayerInteractionManager ips = c.getPlayer().getInteraction();
                c.getPlayer().setInteraction(null);
                if (ips != null) {
                    if (ips.isOwner(c.getPlayer())) {
                        if (ips.getShopType() == 2) {
                            boolean save = false;
                            for (MaplePlayerShopItem items : ips.getItems()) {
                                if (items.getBundles() > 0) {
                                    IItem item = items.getItem();
                                    item.setQuantity((short) (items.getBundles() * item.getQuantity()));
                                    if (MapleInventoryManipulator.addFromDrop(c, item)) {
                                        items.setBundles((short) 0);
                                    } else {
                                        save = true;
                                        break;
                                    }
                                }
                            }
                            ips.removeAllVisitors(3, 1);
                            ips.closeShop(save);
                        } else if (ips.getShopType() == 1) {
                            c.getSession().write(MaplePacketCreator.shopVisitorLeave(0));
                        } else if (ips.getShopType() == 3 || ips.getShopType() == 4) {
                            ips.removeAllVisitors(3, 1);
                        }
                    } else {
                        ips.removeVisitor(c.getPlayer());
                    }
                }
            }
        } else if (mode == Action.OPEN.getCode()) {
            IPlayerInteractionManager shop = c.getPlayer().getInteraction();
            if (shop != null && shop.isOwner(c.getPlayer())) {
                c.getPlayer().getMap().addMapObject((PlayerInteractionManager) shop);
                if (shop.getShopType() == 1) {
                    HiredMerchant merchant = (HiredMerchant) shop;
                    c.getPlayer().setHasMerchant(true);
                    merchant.setOpen(true);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnHiredMerchant(merchant));
                    c.getPlayer().setInteraction(null);
                } else if (shop.getShopType() == 2) {
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendInteractionBox(c.getPlayer()));
                }
                slea.readByte();
            }
        } else if (mode == Action.READY.getCode()) {
            c.getPlayer().getInteraction().broadcast(MaplePacketCreator.getMiniGameReady(), true);
        } else if (mode == Action.UN_READY.getCode()) {
            c.getPlayer().getInteraction().broadcast(MaplePacketCreator.getMiniGameUnReady(), true);
        } else if (mode == Action.START.getCode()) {
            MapleMiniGame game = (MapleMiniGame) c.getPlayer().getInteraction();
            if (game.getGameType() == MiniGameType.OMOK) {
                game.broadcast(MaplePacketCreator.getMiniGameStart(game.getLoser()), true);
            } else if (game.getGameType() == MiniGameType.MATCH_CARDS) {
                game.shuffleList();
                game.broadcast(MaplePacketCreator.getMatchCardStart(game), true);
            }
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendInteractionBox(game.getOwner()));
            game.setStarted(true);
        } else if (mode == Action.GIVE_UP.getCode()) {
            MapleMiniGame game = (MapleMiniGame) c.getPlayer().getInteraction();
            if (game.getGameType() == MiniGameType.OMOK) {
                game.broadcast(
                    MaplePacketCreator.getMiniGameForfeit(
                        game,
                        game.isOwner(c.getPlayer()) ? 0 : 1
                    ),
                    true
                );
            } else if (game.getGameType() == MiniGameType.MATCH_CARDS) {
                if (game.isOwner(c.getPlayer())) {
                    game.broadcast(MaplePacketCreator.getMiniGameWin(game, 1), true);
                } else {
                    game.broadcast(MaplePacketCreator.getMiniGameWin(game, 0), true);
                }
            }
        } else if (mode == Action.REQUEST_TIE.getCode()) {
            MapleMiniGame game = (MapleMiniGame) c.getPlayer().getInteraction();
            if (game.isOwner(c.getPlayer())) {
                game.getVisitors()[0].getClient().getSession().write(MaplePacketCreator.getMiniGameRequestTie());
            } else {
                game.getOwner().getClient().getSession().write(MaplePacketCreator.getMiniGameRequestTie());
            }
        } else if (mode == Action.ANSWER_TIE.getCode()) {
            MapleMiniGame game = (MapleMiniGame) c.getPlayer().getInteraction();
            if (slea.readByte() == 1) {
                game.broadcast(MaplePacketCreator.getMiniGameTie(game), true);
            } else {
                game.broadcast(MaplePacketCreator.getMiniGameDenyTie(), true);
            }
        } else if (mode == Action.SKIP.getCode()) {
            IPlayerInteractionManager game = c.getPlayer().getInteraction();
            game.broadcast(MaplePacketCreator.getMiniGameSkipTurn(game.isOwner(c.getPlayer()) ? 0 : 1), true);
        } else if (mode == Action.MOVE_OMOK.getCode()) {
            int x = slea.readInt(); // x point
            int y = slea.readInt(); // y point
            int type = slea.readByte(); // Piece (1 or 2; Owner has one piece,
                                        // visitor has another, it switches every game.)
            ((MapleMiniGame) c.getPlayer().getInteraction()).setPiece(x, y, type, c.getPlayer());
        } else if (mode == Action.SELECT_CARD.getCode()) {
            int turn = slea.readByte(); // 1st turn = 1; 2nd turn = 0
            int slot = slea.readByte(); // Slot
            MapleMiniGame game = (MapleMiniGame) c.getPlayer().getInteraction();
            int firstslot = game.getFirstSlot();
            if (turn == 1) {
                game.setFirstSlot(slot);
                game.broadcast(
                    MaplePacketCreator.getMatchCardSelect(
                        turn,
                        slot,
                        firstslot,
                        turn
                    ),
                    !game.isOwner(c.getPlayer())
                );
            } else if (game.getCardId(firstslot + 1) == game.getCardId(slot + 1)) {
                if (game.isOwner(c.getPlayer())) {
                    game.broadcast(MaplePacketCreator.getMatchCardSelect(turn, slot, firstslot, 2), true);
                    game.setOwnerPoints();
                } else {
                    game.broadcast(MaplePacketCreator.getMatchCardSelect(turn, slot, firstslot, 3), true);
                    game.setVisitorPoints();
                }
            } else {
                game.broadcast(
                    MaplePacketCreator.getMatchCardSelect(
                        turn,
                        slot,
                        firstslot,
                        game.isOwner(c.getPlayer()) ? 0 : 1
                    ),
                    true
                );
            }
        } else if (mode == Action.SET_MESO.getCode()) {
            c.getPlayer().getTrade().setMeso(slea.readInt());
        } else if (mode == Action.SET_ITEMS.getCode()) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleInventoryType ivType = MapleInventoryType.getByType(slea.readByte());
            IItem item = c.getPlayer().getInventory(ivType).getItem((byte) slea.readShort());
            long checkq = slea.readShort();
            short quantity = (short)(int)checkq;
            byte targetSlot = slea.readByte();
            if (c.getPlayer().getTrade() != null && item != null) {
                if (checkq > 4000) {
                    AutobanManager.getInstance().autoban(c, "Packet editing while in trade.");
                }
                if (
                    (quantity <= item.getQuantity() && quantity >= 0) ||
                    ii.isThrowingStar(item.getItemId()) ||
                    ii.isBullet(item.getItemId())
                ) {
                    if (
                        (!c.getChannelServer().allowUndroppablesDrop() && ii.isDropRestricted(item.getItemId())) ||
                        ii.isCash(item.getItemId())
                    ) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    IItem tradeItem = item.copy();
                    if (ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId())) {
                        tradeItem.setQuantity(item.getQuantity());
                        MapleInventoryManipulator.removeFromSlot(
                            c,
                            ivType,
                            item.getPosition(),
                            item.getQuantity(),
                            true
                        );
                    } else {
                        tradeItem.setQuantity(quantity);
                        MapleInventoryManipulator.removeFromSlot(c, ivType, item.getPosition(), quantity, true);
                    }
                    tradeItem.setPosition(targetSlot);
                    c.getPlayer().getTrade().addItem(tradeItem);
                }
            }
        } else if (mode == Action.CONFIRM.getCode()) {
            MapleTrade.completeTrade(c.getPlayer());
        } else if (mode == Action.ADD_ITEM.getCode() || mode == Action.PUT_ITEM.getCode()) {
            MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
            byte slot = (byte) slea.readShort();
            short bundles = slea.readShort();
            short perBundle = slea.readShort();
            int price = slea.readInt();
            IItem ivItem = c.getPlayer().getInventory(type).getItem(slot);
            IItem sellItem;
            if (ivItem != null) {
                sellItem = ivItem.copy();
            } else {
                return;
            }
            sellItem.setQuantity(perBundle);
            MaplePlayerShopItem item = new MaplePlayerShopItem(sellItem, bundles, price);
            IPlayerInteractionManager shop = c.getPlayer().getInteraction();
            long checkquantity = bundles * perBundle;
            int checkiquantity = bundles * perBundle;
            short checksmquantity = (short) (bundles * perBundle);
            //System.out.print("shop == null, shop.isOwner(c.getPlayer()), ivItem.getQuantity(), bundles, perBundle: " + (shop == null) + ", " + (shop != null ? shop.isOwner(c.getPlayer()) : "N/A") + ", " + ivItem.getQuantity() + ", " + bundles + ", " + perBundle + "\n");
            if (shop != null && shop.isOwner(c.getPlayer())) {
                if (ivItem.getQuantity() >= bundles * perBundle) {
                    if (price < 0) {
                        AutobanManager.getInstance().autoban(c, "Packet editing, adding a negative priced item.");
                        return;
                    }
                    if (
                        bundles <= 0 ||
                        perBundle <= 0 ||
                        checkquantity > 20000 ||
                        checksmquantity < 0 ||
                        checkiquantity < 0 ||
                        checkiquantity > 20000
                    ) {
                        AutobanManager
                            .getInstance()
                            .autoban(
                                c,
                                "Packet edited player shop item: " +
                                    sellItem.getItemId()
                            );
                        return;
                    }
                    if (bundles > 100 || perBundle > 4000) {
                        c.getPlayer()
                         .dropMessage(
                             1,
                             "You are trying to insert too many bundles at once, or too many items per bundle."
                         );
                        return;
                    }
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    if (ii.isThrowingStar(ivItem.getItemId()) || ii.isBullet(ivItem.getItemId())) {
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, ivItem.getQuantity(), true);
                    } else {
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, (short) (bundles * perBundle), true);
                    }
                    shop.addItem(item);
                    c.getSession().write(MaplePacketCreator.shopItemUpdate(shop));
                }
            }
        } else if (mode == Action.BUY.getCode() || mode == Action.MERCHANT_BUY.getCode()) {
            int item = slea.readByte();
            short quantity = slea.readShort();
            IPlayerInteractionManager shop = c.getPlayer().getInteraction();
            shop.buy(c, item, quantity);
            shop.broadcast(MaplePacketCreator.shopItemUpdate(shop), true);
        } else if (mode == Action.TAKE_ITEM_BACK.getCode() || mode == Action.REMOVE_ITEM.getCode()) {
            int slot = slea.readShort();
            IPlayerInteractionManager shop = c.getPlayer().getInteraction();
            if (shop != null && shop.isOwner(c.getPlayer())) {
                MaplePlayerShopItem item;
                try {
                    item = shop.getItems().get(slot);
                } catch (IndexOutOfBoundsException ioobe) {
                    return;
                }
                if (item.getBundles() > 0) {
                    IItem iitem = item.getItem();
                    iitem.setQuantity((short) (item.getBundles() * iitem.getQuantity()));
                    MapleInventoryManipulator.addFromDrop(c, iitem);
                }
                shop.removeFromSlot(slot);
                c.getSession().write(MaplePacketCreator.shopItemUpdate(shop));
            }
        } else if (mode == Action.CLOSE_MERCHANT.getCode()) {
            IPlayerInteractionManager merchant = c.getPlayer().getInteraction();
            if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(c.getPlayer())) {
                boolean save = false;
                for (MaplePlayerShopItem items : merchant.getItems()) {
                    if (items.getBundles() > 0) {
                        IItem item = items.getItem();
                        item.setQuantity((short) (items.getBundles() * item.getQuantity()));
                        if (MapleInventoryManipulator.addFromDrop(c, item)) {
                            items.setBundles((short) 0);
                        } else {
                            save = true;
                            break;
                        }
                    }
                }
                c.getSession().write(MaplePacketCreator.shopErrorMessage(0x10, 0));
                merchant.closeShop(save);
                c.getPlayer().setInteraction(null);
                c.getPlayer().setHasMerchant(false);
            }
        } else if (mode == Action.MAINTENANCE_OFF.getCode()) {
            HiredMerchant merchant = (HiredMerchant) c.getPlayer().getInteraction();
            if (merchant != null && merchant.isOwner(c.getPlayer())) {
                merchant.setOpen(true);
                merchant.tempItemsUpdate();
            }
        } else if (mode == Action.BAN_PLAYER.getCode()) {
            IPlayerInteractionManager imps = c.getPlayer().getInteraction();
            if (imps != null && imps.isOwner(c.getPlayer())) {
                ((MaplePlayerShop) imps).banPlayer(slea.readMapleAsciiString());
            }
        } else if (mode == Action.MERCHANT_ORGANIZE.getCode()) {
            IPlayerInteractionManager imps = c.getPlayer().getInteraction();
            if (!imps.isOwner(c.getPlayer())) {
                AutobanManager
                    .getInstance()
                    .autoban(
                        c.getPlayer().getClient(),
                        "Attempting to manipulate player shop that they do not own."
                    );
                return;
            }
            for (int i = 0; i < imps.getItems().size(); ++i) {
                if (imps.getItems().get(i).getBundles() == 0) {
                    imps.removeFromSlot(i);
                }
            }
            c.getSession().write(MaplePacketCreator.shopItemUpdate(imps));
        }
    }
}
