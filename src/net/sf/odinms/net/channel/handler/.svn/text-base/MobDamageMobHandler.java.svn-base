package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class MobDamageMobHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int attackerOid = slea.readInt();
        int cid = slea.readInt();
        int damagedOid = slea.readInt();
        MapleMonster damaged = c.getPlayer().getMap().getMonsterByOid(damagedOid);
        MapleMonster attacker = c.getPlayer().getMap().getMonsterByOid(attackerOid);
        if (damaged == null || attacker == null) {
            return;
        }
        int damage = (int) (Math.random() * (damaged.getMaxHp() / 13 + attacker.getPADamage() * 10)) * 2 + 500;
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.MobDamageMob(damaged, damage), damaged.getPosition());
    }
}
