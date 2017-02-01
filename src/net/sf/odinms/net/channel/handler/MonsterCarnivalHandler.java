package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;

public class MonsterCarnivalHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int tab = slea.readByte();
        int num = slea.readByte();
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.playerSummoned(c.getPlayer().getName(), tab, num));
        if (tab == 0) {
            MapleMonster mob = MapleLifeFactory.getMonster(getMonsterIdByNum(num));
            //c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, randomizePosition(c.getPlayer().getMapId()));
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, randomizePosition(c.getPlayer().getMapId(), 1));
        }
    }

    public Point randomizePosition(int mapid, int team) {
        int posx = 0;
        int posy = 0;
        if (mapid == 980000301) { // Room 3 iirc
            posy = 162;
            if (team == 0) { // Maple red goes left
                posx = rand(-1554, -151);
            } else { // Maple blue goes right
                posx = rand(148, 1571);
            }
        }
        return new Point(posx, posy);
    }

    /**
     * <ul>
     * <li>1 - Brown Teddy - 3000005</li>
     * <li>2 - Bloctopus - 3230302</li>
     * <li>3 - Ratz - 3110102</li>
     * <li>4 - Chronos - 3230306</li>
     * <li>5 - Toy Trojan - 3230305</li>
     * <li>6 - Tick-Tock - 4230113</li>
     * <li>7 - Robo - 4230111</li>
     * <li>8 - King Bloctopus - 3230103</li>
     * <li>9 - Master Chronos - 4230115</li>
     * <li>10 - Rombot - 4130103</li>
     * </ul>
     */
    public int getMonsterIdByNum(int num) {
        int mid;
        num++;
        switch (num) {
            case 1:
                mid = 3000005;
                break;
            case 2:
                mid = 3230302;
                break;
            case 3:
                mid = 3110102;
                break;
            case 4:
                mid = 3230306;
                break;
            case 5:
                mid = 3230305;
                break;
            case 6:
                mid = 4230113;
                break;
            case 7:
                mid = 4230111;
                break;
            case 8:
                mid = 3230103;
                break;
            case 9:
                mid = 4230115;
                break;
            case 10:
                mid = 4130103;
                break;
            default:
                mid = 210100;
                break;
        }
        return mid;
    }

    private static int rand(int lbound, int ubound) {
        return (int) ((Math.random() * (ubound - lbound + 1)) + lbound);
    }
}
