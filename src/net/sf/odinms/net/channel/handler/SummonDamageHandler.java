package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SummonDamageHandler extends AbstractMaplePacketHandler {

    public class SummonAttackEntry {

        private final int monsterOid;
        private final int damage;

        public SummonAttackEntry(int monsterOid, int damage) {
            super();
            this.monsterOid = monsterOid;
            this.damage = damage;
        }

        public int getMonsterOid() {
            return monsterOid;
        }

        public int getDamage() {
            return damage;
        }
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        MapleCharacter player = c.getPlayer();
        Collection<MapleSummon> summons = player.getSummons().values();
        MapleSummon summon = null;
        for (MapleSummon sum : summons) {
            if (sum.getObjectId() == oid) {
                summon = sum;
            }
        }
        if (summon == null) {
            summons.removeIf(s -> s == null);
            player.getMap().removeMapObject(oid);
            return;
        }
        ISkill summonSkill = SkillFactory.getSkill(summon.getSkill());
        MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
        slea.skip(5);
        List<SummonAttackEntry> allDamage = new ArrayList<>();
        int numAttacked = slea.readByte();
        player.getCheatTracker().checkSummonAttack();
        for (int x = 0; x < numAttacked; ++x) {
            int monsterOid = slea.readInt(); // Attacked oid.
            slea.skip(14);
            int damage = slea.readInt();
            allDamage.add(new SummonAttackEntry(monsterOid, damage));
        }
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        if (summonSkill.getId() == 2311006 || summonSkill.getId() == 2321003) { // Summon Dragon or Bahamut
            double percentperlevel;
            if (summonSkill.getId() == 2311006) {
                percentperlevel = 0.05;
            } else {
                percentperlevel = 0.07;
            }
            int min = player.calculateMinBaseDamage(player);
            int max = player.calculateMaxBaseDamage(player.getTotalWatk());
            int basedmg;
            double skilllevelmultiplier = 1.5 + player.getSkillLevel(summonSkill) * percentperlevel;
            for (int i = 0; i < allDamage.size(); ++i) {
                basedmg = min + (int) (Math.random() * (double) (max - min + 1));
                int thisdmg = allDamage.get(i).getDamage();
                thisdmg += (int) (basedmg * skilllevelmultiplier);
                allDamage.set(i, new SummonAttackEntry(allDamage.get(i).getMonsterOid(), thisdmg));
                c.getSession().write(MaplePacketCreator.damageMonster(allDamage.get(i).getMonsterOid(), thisdmg));
            }
        }
        player.getMap().broadcastMessage(player, MaplePacketCreator.summonAttack(player.getId(), summonSkill.getId(), summon.getStance(), allDamage), summon.getPosition());
        
        for (SummonAttackEntry attackEntry : allDamage) {
            int damage = attackEntry.getDamage();
            player.getMap().broadcastMessage(MaplePacketCreator.damageMonster(attackEntry.getMonsterOid(), attackEntry.getDamage()), summon.getPosition());
            /*
            if (summonSkill.getId() == 2311006) { // Summon Dragon
                int min = player.calculateMinBaseDamage(player);
                int max = player.calculateMaxBaseDamage(player.getTotalWatk());
                int basedmg = min + (int) (Math.random() * (double) (max - min + 1));
                // (150 + skillLevel * 5)% damage
                double skilllevelmultiplier = (double) 1.5 + (double) (player.getSkillLevel(summonSkill) * 0.05);
                damage += (int) (basedmg * skilllevelmultiplier);
            } else if (summonSkill.getId() == 2321003) { // Bahamut
                int min = player.calculateMinBaseDamage(player);
                int max = player.calculateMaxBaseDamage(player.getTotalWatk());
                int basedmg = min + (int) (Math.random() * (double) (max - min + 1));
                // (150 + skillLevel * 7)% damage
                double skilllevelmultiplier = (double) 1.5 + (double) (player.getSkillLevel(summonSkill) * 0.07);
                damage += (int) (basedmg * skilllevelmultiplier);
            }
            */
            MapleMonster target = player.getMap().getMonsterByOid(attackEntry.getMonsterOid());
            if (target != null) {
                if (damage >= 100000000) {
                    AutobanManager.getInstance().autoban
                    (player.getClient(),"XSource| " + player.getName() + "'s summon dealt " + damage + " to monster " + target.getId() + ".");
                }
                if (damage > 0 && !summonEffect.getMonsterStati().isEmpty()) {
                    if (summonEffect.makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(summonEffect.getMonsterStati(), summonSkill, false);
                        target.applyStatus(player, monsterStatusEffect, summonEffect.isPoison(), 4000);
                    }
                }
                player.getMap().damageMonster(player, target, damage);
                player.checkMonsterAggro(target);
            }
        }
    }
}