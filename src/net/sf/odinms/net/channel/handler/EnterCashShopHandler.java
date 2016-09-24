package net.sf.odinms.net.channel.handler;

import java.rmi.RemoteException;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.server.maps.SavedLocationType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class EnterCashShopHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        if (player.getClient().getChannelServer().CStoFM()) {
            if (!(c.getPlayer().isAlive()) || c.getPlayer().getMapId() == 100) {
                c.getPlayer().dropMessage("You can't enter the FM when you are dead.");
            } else {
                if (c.getPlayer().getMapId() != 910000000) {
                    c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET);
                    c.getPlayer().changeMap(c.getChannelServer().getMapFactory().getMap(910000000), c.getChannelServer().getMapFactory().getMap(910000000).getPortal("out00"));
                }
            }
            c.getSession().write(MaplePacketCreator.enableActions());
        } else if (c.getPlayer().getMapId() >= 1000 && c.getPlayer().getMapId() <= 1006) {
            c.getPlayer().dropMessage("You can't enter the cash shop while in a Monster Trial.");
            c.getSession().write(MaplePacketCreator.enableActions());
        } else {
            if (player.getNoPets() > 0) {
                player.unequipAllPets();
            }
            for (FakeCharacter fc : player.getFakeChars()) {
                player.getMap().removePlayer(fc.getFakeChar());
            }
            if (player.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                player.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            }
            try {
                WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
                wci.addBuffsToStorage(player.getId(), player.getAllBuffs());
                wci.addCooldownsToStorage(player.getId(), player.getAllCooldowns());
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
            player.getMap().removePlayer(player);
            c.getSession().write(MaplePacketCreator.warpCS(c, false));
            player.setInCS(true);
            c.getSession().write(MaplePacketCreator.enableCSUse0());
            c.getSession().write(MaplePacketCreator.enableCSUse1());
            c.getSession().write(MaplePacketCreator.enableCSUse2());
            c.getSession().write(MaplePacketCreator.enableCSUse3());
            c.getSession().write(MaplePacketCreator.showNXMapleTokens(player));
            c.getSession().write(MaplePacketCreator.sendWishList(player.getId(), false));
            player.saveToDB(true, true);
        }
    }
}
