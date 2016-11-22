package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.PlayerInteraction.HiredMerchant;
import net.sf.odinms.server.PlayerInteraction.IPlayerInteractionManager;
import net.sf.odinms.server.PlayerInteraction.MaplePlayerShop;
import net.sf.odinms.server.PublicChatHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.rmi.RemoteException;

public class ChangeChannelHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int channel = slea.readByte() + 1;
        changeChannel(channel, c);
    }

    public static void changeChannel(int channel, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        if (!player.isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (player.getPartyQuest() != null) {
            if (!player.getMap().isPQMap()) {
                player.getPartyQuest().playerDisconnected(player);
                player.setPartyQuest(null);
            } else {
                player.dropMessage(5, "You may not change channels while in " + player.getPartyQuest().getName() + ".");
            }
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (PublicChatHandler.getPublicChatHolder().containsKey(player.getId())) {
            PublicChatHandler.getPublicChatHolder().remove(player.getId());
            PublicChatHandler.getPublicChatHolder().put(player.getId(), channel);
        }
        String ip = ChannelServer.getInstance(c.getChannel()).getIP(channel);
        String[] socket = ip.split(":");
        if (player.getTrade() != null) {
            MapleTrade.cancelTrade(player);
        }
        player.cancelMagicDoor();
        IPlayerInteractionManager ips = c.getPlayer().getInteraction();
        if (ips != null) {
            if (ips.isOwner(c.getPlayer())) {
                if (ips.getShopType() == 2) {
                    ips.removeAllVisitors(3, 1);
                    ips.closeShop(((MaplePlayerShop) ips).returnItems(c));
                } else if (ips.getShopType() == 1) {
                    c.getSession().write(MaplePacketCreator.shopVisitorLeave(0));
                    if (ips.getItems().isEmpty()) {
                        ips.removeAllVisitors(3, 1);
                        ips.closeShop(((HiredMerchant) ips).returnItems(c));
                    }
                } else if (ips.getShopType() == 3 || ips.getShopType() == 4) {
                    ips.removeAllVisitors(3, 1);
                }
            } else {
                ips.removeVisitor(c.getPlayer());
            }
        }
        if (!player.getDiseases().isEmpty()) {
            player.cancelAllDebuffs();
        }
        if (player.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
            player.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
        }
        if (player.getBuffedValue(MapleBuffStat.SUMMON) != null) {
            player.cancelEffectFromBuffStat(MapleBuffStat.SUMMON);
        }
        if (player.getBuffedValue(MapleBuffStat.PUPPET) != null) {
            player.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
        }
        if (player.getBuffedValue(MapleBuffStat.MORPH) != null) {
            player.cancelEffectFromBuffStat(MapleBuffStat.MORPH);
        }
        if (player.getBuffedValue(MapleBuffStat.COMBO) != null) {
            player.cancelEffectFromBuffStat(MapleBuffStat.COMBO);
        }
        try {
            WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
            wci.addBuffsToStorage(player.getId(), player.getAllBuffs());
            wci.addCooldownsToStorage(player.getId(), player.getAllCooldowns());
            if (player.getMessenger() != null) {
                MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
                wci.silentLeaveMessenger(player.getMessenger().getId(), messengerplayer);
            }
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
        player.saveToDB(true, true);
        if (player.getCheatTracker() != null) {
            player.getCheatTracker().dispose();
        }
        player.getMap().removePlayer(player);
        c.getChannelServer().removePlayer(player);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
        try {
            c.getSession().write(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
