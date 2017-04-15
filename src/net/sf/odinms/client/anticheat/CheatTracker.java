package net.sf.odinms.client.anticheat;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.StringUtil;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

public class CheatTracker {
    private final Map<CheatingOffense, CheatingOffenseEntry> offenses =
        Collections.synchronizedMap(
            new LinkedHashMap<CheatingOffense, CheatingOffenseEntry>()
        );
    private final WeakReference<MapleCharacter> chr;
    private long regenHPSince;
    private long regenMPSince;
    private int numHPRegens;
    private int numMPRegens;
    private int numSequentialAttacks;
    private long lastAttackTime;
    private long lastDamage;
    private long takingDamageSince;
    private int numSequentialDamage;
    private long lastDamageTakenTime;
    private int numSequentialSummonAttack;
    private long summonSummonTime;
    private int numSameDamage;
    private long attackingSince;
    private Point lastMonsterMove;
    private int monsterMoveCount;
    private int attacksWithoutHit;
    private int numGotMissed;
    private int vac;
    private Boolean pickupComplete = Boolean.TRUE;
    private final long[] lastTime = new long[8];
    private final ScheduledFuture<?> invalidationTask;

    public CheatTracker(final MapleCharacter chr) {
        this.chr = new WeakReference<>(chr);
        invalidationTask = TimerManager.getInstance().register(new InvalidationTask(), 60000);
        takingDamageSince = attackingSince = regenMPSince = regenHPSince = System.currentTimeMillis();
        for (int i = 0; i < lastTime.length; ++i) {
            lastTime[i] = 0;
        }
    }

    /**
     * <ul>
     * <li>Type 0 - Save limit.</li>
     * <li>Type 1 - Contact GM.</li>
     * <li>Type 2 - Mesos drop.</li>
     * <li>Type 3 - Smega.</li>
     * <li>Type 4 - NPC.</li>
     * <li>Type 5 - Change map.</li>
     * <li>Type 6 - N/A.</li>
     * <li>Type 7 - Commands.</li>
     * </ul>
     *
     * @return Whether or not it's spam.
     */
    public synchronized boolean Spam(final int limit, int type) {
        if (type < 0 || lastTime.length <= type) {
            type = 1; // Default.
        }
        if (!chr.get().isGM()) {
            if (System.currentTimeMillis() < limit + lastTime[type]) {
                return true;
            }
        }
        lastTime[type] = System.currentTimeMillis();
        return false;
    }

    public boolean checkAttack(final int skillId) {
        numSequentialAttacks++;

        final long oldLastAttackTime = lastAttackTime;
        lastAttackTime = System.currentTimeMillis();
        final long attackTime = lastAttackTime - attackingSince;
        if (numSequentialAttacks > 3) {
            final int divisor;
            if (skillId == 3121004 || skillId == 5221004) { // Hurricane.
                divisor = 30;
            } else {
                divisor = 300;
            }
            if (attackTime / divisor < numSequentialAttacks) {
                registerOffense(CheatingOffense.FASTATTACK);
                return false;
            }
        }
        if (lastAttackTime - oldLastAttackTime > 1500) {
            attackingSince = lastAttackTime;
            numSequentialAttacks = 0;
        }
        return true;
    }

    public void checkTakeDamage() {
        numSequentialDamage++;
        final long oldLastDamageTakenTime = lastDamageTakenTime;
        lastDamageTakenTime = System.currentTimeMillis();
        final long timeBetweenDamage = lastDamageTakenTime - takingDamageSince;
        if (timeBetweenDamage / 500 < numSequentialDamage) {
            registerOffense(CheatingOffense.FAST_TAKE_DAMAGE);
        }
        if (lastDamageTakenTime - oldLastDamageTakenTime > 4500) {
            takingDamageSince = lastDamageTakenTime;
            numSequentialDamage = 0;
        }
    }

    public int checkDamage(final long dmg) {
        if (dmg > 1 && lastDamage == dmg) {
            numSameDamage++;
        } else {
            lastDamage = dmg;
            numSameDamage = 0;
        }
        return numSameDamage;
    }

    public void checkMoveMonster(final Point pos) {
        if (pos.equals(lastMonsterMove)) {
            monsterMoveCount++;
            if (monsterMoveCount > 15) {
                registerOffense(CheatingOffense.MOVE_MONSTERS);
            }
        } else {
            lastMonsterMove = pos;
            monsterMoveCount = 1;
        }
    }

    public boolean checkHPRegen() {
        numHPRegens++;
        if ((System.currentTimeMillis() - regenHPSince) / 10000 < numHPRegens) {
            registerOffense(CheatingOffense.FAST_HP_REGEN);
            return false;
        }
        return true;
    }

    public void resetHPRegen() {
        regenHPSince = System.currentTimeMillis();
        numHPRegens = 0;
    }

    public boolean checkMPRegen() {
        numMPRegens++;
        final long allowedRegens = (System.currentTimeMillis() - regenMPSince) / 10000;
        // System.out.println(numMPRegens + "/" + allowedRegens);
        if (allowedRegens < numMPRegens) {
            registerOffense(CheatingOffense.FAST_MP_REGEN);
            return false;
        }
        return true;
    }

    public void resetMPRegen() {
        regenMPSince = System.currentTimeMillis();
        numMPRegens = 0;
    }

    public void resetSummonAttack() {
        summonSummonTime = System.currentTimeMillis();
        numSequentialSummonAttack = 0;
    }

    public boolean checkSummonAttack() {
        numSequentialSummonAttack++;
        final long allowedAttacks = (System.currentTimeMillis() - summonSummonTime) / 2000 + 1;
        if (allowedAttacks < numSequentialAttacks) {
            registerOffense(CheatingOffense.FAST_SUMMON_ATTACK);
            return false;
        }
        return true;
    }

    public void checkPickupAgain() {
        synchronized (pickupComplete) {
            if (pickupComplete) {
                pickupComplete = Boolean.FALSE;
            } else {
                registerOffense(CheatingOffense.TUBI);
            }
        }
    }

    public void pickupComplete() {
        synchronized (pickupComplete) {
            pickupComplete = Boolean.TRUE;
        }
    }

    public int getAttacksWithoutHit() {
        return attacksWithoutHit;
    }

    public void setAttacksWithoutHit(final int attacksWithoutHit) {
        this.attacksWithoutHit = attacksWithoutHit;
    }

    public int getNumGotMissed() {
        return numGotMissed;
    }

    public void setNumGotMissed(final int ngm) {
        numGotMissed = ngm;
    }

    public void incrementNumGotMissed() {
        numGotMissed++;
    }

    public void registerOffense(final CheatingOffense offense) {
        registerOffense(offense, null);
    }

    public void registerOffense(final CheatingOffense offense, final String param) {
        final MapleCharacter chrhardref = chr.get();
        if (chrhardref == null || !offense.isEnabled()) return;
        CheatingOffenseEntry entry = offenses.get(offense);
        if (entry != null && entry.isExpired()) {
            expireEntry(entry);
            entry = null;
        }
        if (entry == null) entry = new CheatingOffenseEntry(offense, chrhardref);
        if (param != null) entry.setParam(param);
        entry.incrementCount();
        if (offense.shouldAutoban(entry.getCount())) {
            AutobanManager
                .getInstance()
                .autoban(
                    chrhardref.getClient(),
                    StringUtil.makeEnumHumanReadable(offense.name())
                );
        }
        offenses.put(offense, entry);
        CheatingOffensePersister.getInstance().persistEntry(entry);
    }

    public void expireEntry(final CheatingOffenseEntry coe) {
        offenses.remove(coe.getOffense());
    }

    public int getPoints() {
        int ret = 0;
        final CheatingOffenseEntry[] offensesCopy;
        synchronized (offenses) {
            offensesCopy = offenses.values().toArray(new CheatingOffenseEntry[offenses.size()]);
        }
        for (final CheatingOffenseEntry entry : offensesCopy) {
            if (entry.isExpired()) {
                expireEntry(entry);
            } else {
                ret += entry.getPoints();
            }
        }
        return ret;
    }

    public Map<CheatingOffense, CheatingOffenseEntry> getOffenses() {
        return Collections.unmodifiableMap(offenses);
    }

    public String getSummary() {
        final StringBuilder ret = new StringBuilder();
        final List<CheatingOffenseEntry> offenseList;
        synchronized (offenses) {
            offenseList =
                offenses
                    .values()
                    .stream()
                    .filter(entry -> !entry.isExpired())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        offenseList.sort((o1, o2) -> {
            final int thisVal = o1.getPoints();
            final int anotherVal = o2.getPoints();
            return (thisVal < anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
        });
        final int to = Math.min(offenseList.size(), 4);
        for (int x = 0; x < to; ++x) {
            ret.append(StringUtil.makeEnumHumanReadable(offenseList.get(x).getOffense().name()));
            ret.append(": ");
            ret.append(offenseList.get(x).getCount());
            if (x != to - 1) ret.append(' ');
        }
        return ret.toString();
    }

    public void dispose() {
        invalidationTask.cancel(false);
    }

    public void incrementVac() {
        vac++;
    }

    public int getVac() {
        return vac;
    }

    private class InvalidationTask implements Runnable {
        @Override
        public void run() {
            final CheatingOffenseEntry[] offenses_copy;
            synchronized (offenses) {
                offenses_copy = offenses.values().toArray(new CheatingOffenseEntry[offenses.size()]);
            }
            for (final CheatingOffenseEntry offense : offenses_copy) {
                if (offense.isExpired()) expireEntry(offense);
            }
            if (chr.get() == null) dispose();
        }
    }
}
