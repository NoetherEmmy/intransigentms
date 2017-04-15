package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;
import java.util.List;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class DistributeAPHandler extends AbstractMaplePacketHandler {
    //private static final Logger log = LoggerFactory.getLogger(DistributeAPHandler.class);

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final List<Pair<MapleStat, Integer>> statupdate = new ArrayList<>(2);
        c.getSession().write(MaplePacketCreator.updatePlayerStats(statupdate, true));
        slea.readInt();
        final int update = slea.readInt();
        if (c.getPlayer().getRemainingAp() > 0) {
            switch (update) {
                case 64: // Str
                    if (c.getPlayer().getStr() >= 30000) {
                        return;
                    }
                    c.getPlayer().setStr(c.getPlayer().getStr() + 1);
                    statupdate.add(new Pair<>(MapleStat.STR, c.getPlayer().getStr()));
                    break;
                case 128: // Dex
                    if (c.getPlayer().getDex() >= 30000) {
                        return;
                    }
                    c.getPlayer().setDex(c.getPlayer().getDex() + 1);
                    statupdate.add(new Pair<>(MapleStat.DEX, c.getPlayer().getDex()));
                    break;
                case 256: // Int
                    if (c.getPlayer().getInt() >= 30000) {
                        return;
                    }
                    c.getPlayer().setInt(c.getPlayer().getInt() + 1);
                    statupdate.add(new Pair<>(MapleStat.INT, c.getPlayer().getInt()));
                    break;
                case 512: // Luk
                    if (c.getPlayer().getLuk() >= 30000) {
                        return;
                    }
                    c.getPlayer().setLuk(c.getPlayer().getLuk() + 1);
                    statupdate.add(new Pair<>(MapleStat.LUK, c.getPlayer().getLuk()));
                    break;
                case 2048: // HP
                    int maxHP = c.getPlayer().getMaxHp();
                    if (c.getPlayer().getHpApUsed() >= 10000 || maxHP == 30000) {
                        return;
                    }
                    final ISkill improvingMaxHP;
                    final int improvingMaxHPLevel;
                    if (c.getPlayer().getJob().isA(MapleJob.BEGINNER)) {
                        maxHP += rand(48, 52);
                    } else if (c.getPlayer().getJob().isA(MapleJob.WARRIOR)) {
                        improvingMaxHP = SkillFactory.getSkill(1000001);
                        improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                        if (improvingMaxHPLevel >= 1) {
                            maxHP += rand(28, 32) + improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                        } else {
                            maxHP += rand(28, 32);
                        }
                    } else if (c.getPlayer().getJob().isA(MapleJob.MAGICIAN)) {
                        maxHP += rand(48, 52);
                    } else if (c.getPlayer().getJob().isA(MapleJob.BOWMAN)) {
                        maxHP += rand(48, 52);
                    } else if (c.getPlayer().getJob().isA(MapleJob.THIEF)) {
                        maxHP += rand(48, 52);
                    } else if (c.getPlayer().getJob().isA(MapleJob.PIRATE)) {
                        improvingMaxHP = SkillFactory.getSkill(5100000);
                        improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                        if (improvingMaxHPLevel >= 1) {
                            maxHP += rand(46, 50) + improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                        } else {
                            if (c.getPlayer().getJob().equals(MapleJob.GUNSLINGER) || c.getPlayer().getJob().equals(MapleJob.OUTLAW) || c.getPlayer().getJob().equals(MapleJob.CORSAIR)) {
                                maxHP += rand(48, 52);
                            } else {
                                maxHP += rand(46, 50);
                            }
                        }
                    }
                    maxHP = Math.min(30000, maxHP);
                    c.getPlayer().setHpApUsed(c.getPlayer().getHpApUsed() + 1);
                    c.getPlayer().setMaxHp(maxHP);
                    statupdate.add(new Pair<>(MapleStat.MAXHP, maxHP));
                    break;
                case 8192: // MP
                    int maxMP = c.getPlayer().getMaxMp();
                    if (c.getPlayer().getMpApUsed() >= 10000 || c.getPlayer().getMaxMp() == 30000) {
                        return;
                    }
                    if (c.getPlayer().getJob().isA(MapleJob.BEGINNER)) {
                        maxMP += rand(6, 8);
                    } else if (c.getPlayer().getJob().isA(MapleJob.WARRIOR)) {
                        maxMP += rand(2, 4);
                    } else if (c.getPlayer().getJob().isA(MapleJob.MAGICIAN)) {
                        final ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
                        final int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                        if (improvingMaxMPLevel >= 1) {
                            maxMP += rand(41, 47) + improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                        } else {
                            maxMP += rand(41, 47);
                        }
                    } else if (c.getPlayer().getJob().isA(MapleJob.BOWMAN)) {
                        maxMP += rand(10, 12);
                    } else if (c.getPlayer().getJob().isA(MapleJob.THIEF)) {
                        maxMP += rand(10, 12);
                    } else if (c.getPlayer().getJob().isA(MapleJob.PIRATE)) {
                        maxMP += rand(10, 12);
                    }
                    maxMP = Math.min(30000, maxMP);
                    c.getPlayer().setMpApUsed(c.getPlayer().getMpApUsed() + 1);
                    c.getPlayer().setMaxMp(maxMP);
                    statupdate.add(new Pair<>(MapleStat.MAXMP, maxMP));
                    break;
                default:
                    c.getSession().write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true));
                    return;
            }
            c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - 1);
            statupdate.add(new Pair<>(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp()));
            c.getSession().write(MaplePacketCreator.updatePlayerStats(statupdate, true));
        }/* else {
            //AutobanManager.getInstance().addPoints(c, 334, 120000, "Trying to distribute AP to " + update + " without having any");
            // I've commented out the following log print because it only (?) shows up because the client I use has Tubi
            //log.info("[h4x] Player {} is distributing AP to {} without having any", c.getPlayer().getName(), Integer.valueOf(update));
        }*/
    }

    private static int rand(final int lbound, final int ubound) {
        return (int) ((Math.random() * (ubound - lbound + 1)) + lbound);
    }
}
