package net.sf.odinms.server.life;

import net.sf.odinms.client.*;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.scripting.event.EventInstanceManager;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonsterInformationProvider.DropEntry;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.ArrayMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class MapleMonster extends AbstractLoadedMapleLife {

    private MapleMonsterStats stats;
    private MapleMonsterStats overrideStats;
    private int hp;
    private int mp;
    private WeakReference<MapleCharacter> controller = new WeakReference<>(null);
    private boolean controllerHasAggro,  controllerKnowsAboutAggro;
    private final Collection<AttackerEntry> attackers = new ArrayList<>();
    private EventInstanceManager eventInstance = null;
    private final Collection<MonsterListener> listeners = new ArrayList<>();
    private MapleCharacter highestDamageChar;
    private final Map<MonsterStatus, MonsterStatusEffect> stati = new LinkedHashMap<>();
    private final List<MonsterStatusEffect> activeEffects = new ArrayList<>();
    private MapleMap map;
    private int VenomMultiplier = 0;
    private boolean fake = false;
    private boolean dropsDisabled = false;
    private final List<Pair<Integer, Integer>> usedSkills = new ArrayList<>();
    private final Map<Pair<Integer, Integer>, Integer> skillsUsed = new HashMap<>();
    private final List<MonsterStatus> monsterBuffs = new ArrayList<>();
    private final Map<Element, ElementalEffectiveness> addedEffectiveness = new HashMap<>();
    private boolean isaflame = false;
    private ScheduledFuture<?> flameSchedule = null;
    private ScheduledFuture<?> cancelFlameTask = null;
    private final Map<Element, ElementalEffectiveness> originalEffectiveness = new HashMap<>();
    private ScheduledFuture<?> cancelEffectivenessTask = null;
    public final AtomicInteger dropShareCount = new AtomicInteger(0);
    private ScheduledFuture<?> otherMobHitCheckTask = null;

    public MapleMonster(int id, MapleMonsterStats stats) {
        super(id);
        initWithStats(stats);
    }

    public MapleMonster(MapleMonster monster) {
        super(monster);
        initWithStats(monster.stats);
    }

    private void initWithStats(MapleMonsterStats stats) {
        setStance(5);
        this.stats = stats;
        hp = stats.getHp();
        mp = stats.getMp();
    }

    public void disableDrops() {
        this.dropsDisabled = true;
    }

    public boolean dropsDisabled() {
        return dropsDisabled;
    }

    public void setMap(MapleMap map) {
        this.map = map;
    }

    public int getDrop() {
        MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        int lastAssigned = -1;
        int minChance = 1;
        List<DropEntry> dl = mi.retrieveDropChances(getId());
        for (DropEntry d : dl) {
            if (d.chance > minChance) {
                minChance = d.chance;
            }
        }
        for (DropEntry d : dl) {
            d.assignedRangeStart = lastAssigned + 1;
            d.assignedRangeLength = (int) Math.ceil(((double) 1 / (double) d.chance) * minChance);
            lastAssigned += d.assignedRangeLength;
        }
        Random r = new Random();
        int c = r.nextInt(minChance);
        for (DropEntry d : dl) {
            if (c >= d.assignedRangeStart && c < (d.assignedRangeStart + d.assignedRangeLength)) {
                return d.itemId;
            }
        }
        return -1;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        if (overrideStats != null) {
            return overrideStats.getHp();
        }
        return stats.getHp();
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public int getMaxMp() {
        if (overrideStats != null) {
            return overrideStats.getMp();
        }
        return stats.getMp();
    }

    public int getExp() {
        if (overrideStats != null) {
            return overrideStats.getExp();
        }
        return stats.getExp();
    }

    public void startOtherMobHitChecking(final Runnable ifHit, long period, long delay) {
        if (otherMobHitCheckTask != null) {
            return;
        }
        otherMobHitCheckTask = TimerManager.getInstance().register(() -> {
            if (getMap().getMapObjectsInRange(getPosition(), 1000.0d, Collections.singletonList(MapleMapObjectType.MONSTER)).size() > 1) {
                ifHit.run();
            }
        }, period, delay);
    }

    public void stopOtherMobHitChecking() {
        if (otherMobHitCheckTask == null) {
            return;
        }
        otherMobHitCheckTask.cancel(false);
        otherMobHitCheckTask = null;
    }

    public int getLevel() {
        return stats.getLevel();
    }
    
    public int getAccuracy() {
        return stats.getAccuracy();
    }
    
    public int getAvoid() {
        return stats.getAvoid();
    }

    public int getRemoveAfter() {
        return stats.getRemoveAfter();
    }

    public int getVenomMulti() {
        return this.VenomMultiplier;
    }

    public void setVenomMulti(int multiplier) {
        this.VenomMultiplier = multiplier;
    }

    public boolean isBoss() {
        return stats.isBoss() || getId() == 8810018;
    }

    public boolean isExplosive() {
        return stats.isExplosive();
    }

    public boolean isFfaLoot() {
        return stats.isFfaLoot();
    }

    public int getAnimationTime(String name) {
        return stats.getAnimationTime(name);
    }

    public List<Integer> getRevives() {
        return stats.getRevives();
    }

    public void setOverrideStats(MapleMonsterStats overrideStats) {
        this.overrideStats = overrideStats;
    }

    public byte getTagColor() {
        return stats.getTagColor();
    }

    public byte getTagBgColor() {
        return stats.getTagBgColor();
    }

    public boolean getUndead() {
        return stats.getUndead();
    }

    public void damage(MapleCharacter from, int damage, boolean updateAttackTime) {
        AttackerEntry attacker;
        if (from.getParty() != null) {
            attacker = new PartyAttackerEntry(from.getParty().getId(), from.getClient().getChannelServer());
        } else {
            attacker = new SingleAttackerEntry(from, from.getClient().getChannelServer());
        }
        boolean replaced = false;
        for (AttackerEntry aentry : attackers) {
            if (aentry.equals(attacker)) {
                attacker = aentry;
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            attackers.add(attacker);
        }
        int rDamage = Math.max(0, Math.min(damage, this.hp));
        attacker.addDamage(from, rDamage, updateAttackTime);
        this.hp -= rDamage;
        int remhppercentage = (int) Math.ceil((this.hp * 100.0) / getMaxHp());
        if (remhppercentage < 1) {
            remhppercentage = 1;
        }
        long okTime = System.currentTimeMillis() - 4000;
        if (hasBossHPBar()) {
            from.getMap().broadcastMessage(makeBossHPBarPacket(), getPosition());
        } else if (!isBoss()) {
            for (AttackerEntry mattacker : attackers) {
                for (AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                    if (cattacker.getAttacker().getMap() == from.getMap()) {
                        if (cattacker.getLastAttackTime() >= okTime) {
                            cattacker.getAttacker().getClient().getSession().write(MaplePacketCreator.showMonsterHP(getObjectId(), remhppercentage));
                        }
                    }
                }
            }
        }
    }

    public void heal(int hp, int mp) {
        int hp2Heal = getHp() + hp;
        int mp2Heal = getMp() + mp;
        if (hp2Heal >= getMaxHp()) {
            hp2Heal = getMaxHp();
        }
        if (mp2Heal >= getMaxMp()) {
            mp2Heal = getMaxMp();
        }
        setHp(hp2Heal);
        setMp(mp2Heal);
        getMap().broadcastMessage(MaplePacketCreator.healMonster(getObjectId(), hp));
    }

    public boolean isAttackedBy(MapleCharacter chr) {
        for (AttackerEntry aentry : attackers) {
            if (aentry.contains(chr)) {
                return true;
            }
        }
        return false;
    }

    private void giveExpToCharacter(MapleCharacter attacker, int exp, boolean highestDamage, int numExpSharers) {
        if (getId() == 9500196) { // Ghost
            exp = 0;
        }
        if (attacker.getLevel() <= this.getLevel() + 20) {
            this.dropShareCount.incrementAndGet();
        }
        if (highestDamage) {
            if (eventInstance != null) {
                eventInstance.monsterKilled(attacker, this);
            }
            highestDamageChar = attacker;
        }
        if (attacker.getHp() > 0) {
            long personalExp = exp;
            if (exp > 0) {
                Integer holySymbol = attacker.getBuffedValue(MapleBuffStat.HOLY_SYMBOL);
                if (holySymbol != null) {
                    if (numExpSharers == 1) {
                        personalExp *= 1.0 + (holySymbol.doubleValue() / 500.0);
                    } else {
                        personalExp *= 1.0 + (holySymbol.doubleValue() / 100.0);
                    }
                }
                //
                if (numExpSharers > 1) {
                    personalExp *= 1.0 + (0.1 * (numExpSharers - 1));
                }
                //
                double mltpercent = 1.0d;
                for (MonsterStatusEffect mse : this.activeEffects) {
                    if (mse.getSkill().getId() == 4121003 || mse.getSkill().getId() == 4221003) {
                        int percent = mse.getStati().get(MonsterStatus.SHOWDOWN) + 10;
                        double tempmltpercent = 1.0 + percent / 100.0;
                        if (tempmltpercent > mltpercent) {
                            mltpercent = tempmltpercent;
                        }
                    }
                }
                if (mltpercent != 1.0d) {
                    personalExp = (long) (personalExp * mltpercent);
                }
            }

            personalExp *= attacker.getAbsoluteXp();
            personalExp = (long) (((double) personalExp) * attacker.getRelativeXp(this.getLevel()));

            while (personalExp > Integer.MAX_VALUE) {
                attacker.gainExp(Integer.MAX_VALUE, true, false, highestDamage, false);
                personalExp -= Integer.MAX_VALUE;
            }
            if (attacker.getMap().getId() != 2000) {
                attacker.gainExp((int) personalExp, true, false, highestDamage, false);
            }
            attacker.mobKilled(this.getId());
            if (attacker.getPartyQuest() != null) {
                attacker.getPartyQuest().getMapInstance(getMap()).invokeMethod("mobKilled", this, attacker);
            }
        }
    }

    public MapleCharacter killBy(MapleCharacter killer) {
        long totalBaseExpL = this.getExp() * ChannelServer.getInstance(killer.getClient().getChannel()).getExpRate() * killer.getClient().getPlayer().hasEXPCard();
        int totalBaseExp = (int) (Math.min(Integer.MAX_VALUE, totalBaseExpL));
        AttackerEntry highest = null;
        int highdamage = 0;
        for (AttackerEntry attackEntry : attackers) {
            if (attackEntry.getDamage() > highdamage) {
                highest = attackEntry;
                highdamage = attackEntry.getDamage();
            }
        }
        for (AttackerEntry attackEntry : attackers) {
            int baseExp = (int) Math.ceil(totalBaseExp * ((double) attackEntry.getDamage() / getMaxHp()));
            attackEntry.killedMob(killer.getMap(), baseExp, attackEntry == highest, this.isBoss());
        }
        if (this.getController() != null) {
            getController().getClient().getSession().write(MaplePacketCreator.stopControllingMonster(this.getObjectId()));
            getController().stopControllingMonster(this);
        }
        final List<Integer> toSpawn = this.getRevives();
        if (toSpawn != null) {
            final MapleMap reviveMap = killer.getMap();
            TimerManager.getInstance().schedule(() -> {
                for (Integer mid : toSpawn) {
                    MapleMonster mob = MapleLifeFactory.getMonster(mid);
                    if (mob != null) {
                        if (eventInstance != null) {
                            eventInstance.registerMonster(mob);
                        }
                        mob.setPosition(getPosition());
                        if (dropsDisabled()) {
                            mob.disableDrops();
                        }
                        reviveMap.spawnRevives(mob);
                    }
                }
            }, this.getAnimationTime("die1"));
        }
        if (eventInstance != null) {
            eventInstance.unregisterMonster(this);
        }
        for (MonsterListener listener : listeners.toArray(new MonsterListener[listeners.size()])) {
            listener.monsterKilled(this);
        }
        MapleCharacter ret = highestDamageChar;
        highestDamageChar = null;
        return ret;
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    public MapleCharacter getController() {
        return controller.get();
    }

    public void setController(MapleCharacter controller) {
        this.controller = new WeakReference<>(controller);
    }

    public void switchController(MapleCharacter newController, boolean immediateAggro) {
        MapleCharacter controllers = getController();
        if (controllers == newController) {
            return;
        }
        if (controllers != null) {
            controllers.stopControllingMonster(this);
            controllers.getClient().getSession().write(MaplePacketCreator.stopControllingMonster(getObjectId()));
        }
        newController.controlMonster(this, immediateAggro);
        setController(newController);
        if (immediateAggro) {
            setControllerHasAggro(true);
        }
        setControllerKnowsAboutAggro(false);
    }

    public void addListener(MonsterListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MonsterListener listener) {
        listeners.remove(listener);
    }

    public boolean isControllerHasAggro() {
        if (fake) {
            return false;
        }
        return controllerHasAggro;
    }

    public void setControllerHasAggro(boolean controllerHasAggro) {
        if (fake) {
            return;
        }
        this.controllerHasAggro = controllerHasAggro;
    }

    public boolean isControllerKnowsAboutAggro() {
        if (fake) {
            return false;
        }
        return controllerKnowsAboutAggro;
    }

    public void setControllerKnowsAboutAggro(boolean controllerKnowsAboutAggro) {
        if (fake) {
            return;
        }
        this.controllerKnowsAboutAggro = controllerKnowsAboutAggro;
    }

    public MaplePacket makeBossHPBarPacket() {
        return MaplePacketCreator.showBossHP(getId(), getHp(), getMaxHp(), getTagColor(), getTagBgColor());
    }

    public boolean hasBossHPBar() {
        return (isBoss() && getTagColor() > 0) || isHT();
    }

    private boolean isHT() {
        return this.getId() == 8810018;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (!isAlive() || client.getPlayer().isFake()) {
            return;
        }
        if (isFake()) {
            client.getSession().write(MaplePacketCreator.spawnFakeMonster(this, 0));
        } else {
            client.getSession().write(MaplePacketCreator.spawnMonster(this, false));
        }
        if (!stati.isEmpty()) {
            for (MonsterStatusEffect mse : activeEffects) {
                MaplePacket packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), mse.getStati(), mse.getSkill().getId(), false, 0);
                client.getSession().write(packet);
            }
        }
        if (hasBossHPBar()) {
            client.getSession().write(makeBossHPBarPacket());
        }
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.killMonster(getObjectId(), false));
    }

    @Override
    public String toString() {
        return getName() + "(" + getId() + ") at " + getPosition().x + "/" + getPosition().y + " with " + getHp() + "/" + getMaxHp() +
                "hp, " + getMp() + "/" + getMaxMp() + " mp (alive: " + isAlive() + " oid: " + getObjectId() + ")";
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MONSTER;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public boolean isMobile() {
        return stats.isMobile();
    }

    public ElementalEffectiveness getEffectiveness(Element e) {
        if (!activeEffects.isEmpty() && stati.get(MonsterStatus.DOOM) != null) {
            return ElementalEffectiveness.NORMAL;
        }
        return stats.getEffectiveness(e);
    }
    
    public ElementalEffectiveness getAddedEffectiveness(Element e) {
        if (!activeEffects.isEmpty() && stati.get(MonsterStatus.DOOM) != null) {
            return ElementalEffectiveness.NORMAL;
        }
        if (addedEffectiveness.get(e) == null) {
            return ElementalEffectiveness.NORMAL;
        } else {
            return addedEffectiveness.get(e);
        }
    }

    public boolean applyStatus(MapleCharacter from, final MonsterStatusEffect status, boolean poison, long duration) {
        return applyStatus(from, status, poison, duration, false);
    }

    public boolean applyStatus(MapleCharacter from, final MonsterStatusEffect status, boolean poison, long duration, boolean venom) {
        switch (stats.getEffectiveness(status.getSkill().getElement())) {
            case IMMUNE:
            case STRONG:
                if (status.getSkill().getElement() != Element.POISON) {
                    return false;
                }
                break;
            case NORMAL:
            case WEAK:
                break;
            default:
                throw new RuntimeException("Unknown elemental effectiveness: " + stats.getEffectiveness(status.getSkill().getElement()));
        }
        ElementalEffectiveness effectiveness;
        switch (status.getSkill().getId()) {
            case 2111006:
                effectiveness = stats.getEffectiveness(Element.POISON);
                if (effectiveness == ElementalEffectiveness.IMMUNE) {
                    return false;
                }
                break;
            case 2211006:
                effectiveness = stats.getEffectiveness(Element.ICE);
                if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                    return false;
                }
                break;
            case 4120005:
            case 4220005:
                effectiveness = stats.getEffectiveness(Element.POISON);
                if (effectiveness == ElementalEffectiveness.IMMUNE) {
                    return false;
                }
                break;
        }
        if (poison && getHp() <= 1) {
            return false;
        }
        //
        if (isBoss() && !(status.getStati().containsKey(MonsterStatus.SPEED))) {
            switch (status.getSkill().getId()) {
                case 2101003:
                case 2101005:
                case 2111003:
                case 2111004:
                    return false;
                default:
                    break;
            }
        }
        if (isBoss() && (status.getStati().containsKey(MonsterStatus.STUN) || status.getStati().containsKey(MonsterStatus.FREEZE) || status.getStati().containsKey(MonsterStatus.DOOM) || status.getStati().containsKey(MonsterStatus.SHADOW_WEB) || status.getStati().containsKey(MonsterStatus.SEAL))) {
            return false;
        }
        if (isBoss() && status.getStati().containsKey(MonsterStatus.MATK)) {
            status.getStati().put(MonsterStatus.MATK, status.getStati().get(MonsterStatus.MATK) / 2);
        }
        if (isBoss() && status.getStati().containsKey(MonsterStatus.WATK)) {
            status.getStati().put(MonsterStatus.WATK, status.getStati().get(MonsterStatus.WATK) / 2);
        }
        //
        for (MonsterStatus stat : status.getStati().keySet()) {
            MonsterStatusEffect oldEffect = stati.get(stat);
            if (oldEffect != null) {
                oldEffect.removeActiveStatus(stat);
                if (oldEffect.getStati().isEmpty()) {
                    oldEffect.getCancelTask().cancel(false);
                    oldEffect.cancelPoisonSchedule();
                    activeEffects.remove(oldEffect);
                }
            }
        }
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            if (isAlive()) {
                MaplePacket packet;
                synchronized (status) {
                    packet = MaplePacketCreator.cancelMonsterStatus(getObjectId(), status.getStati());
                }
                map.broadcastMessage(packet, getPosition());
                if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                    getController().getClient().getSession().write(packet);
                }
            }
            synchronized (activeEffects) {
                activeEffects.remove(status);
            }
            synchronized (stati) {
                for (MonsterStatus stat : status.getStati().keySet()) {
                    stati.remove(stat);
                }
            }
            setVenomMulti(0);
            status.cancelPoisonSchedule();
        };
        if (poison) {
            int minPoisonDamage, maxPoisonDamage;
            int poisonLevel = from.getSkillLevel(status.getSkill());
            if (status.getSkill().getId() == 2111006) {
                int poisonBasicAtk = status.getSkill().getEffect(poisonLevel).getMatk();
                double poisonMastery = ((double) status.getSkill().getEffect(poisonLevel).getMastery() * 5.0 + 10.0) / 100.0;
                int matk = from.getTotalMagic();
                int _int = from.getInt();
                ISkill eleAmp = SkillFactory.getSkill(2110001);
                double eleAmpMulti = (double) eleAmp.getEffect(from.getSkillLevel(eleAmp)).getY() / 100.0;
                
                minPoisonDamage = (int) ((((matk * matk) / 1000 + matk * poisonMastery * 0.9) / 30 + _int / 200) * poisonBasicAtk * eleAmpMulti);
                maxPoisonDamage = (int) ((((matk * matk) / 1000 + matk) / 30 + _int / 200) * poisonBasicAtk * eleAmpMulti);
            } else {
                minPoisonDamage = (int) (getMaxHp() / (70.0 - poisonLevel) + 0.999);
                maxPoisonDamage = minPoisonDamage;
            }
            if (stats.getEffectiveness(Element.POISON) == ElementalEffectiveness.STRONG) { // 1/2 damage to those that are strong vs. poison
                minPoisonDamage = minPoisonDamage / 2;
                maxPoisonDamage = maxPoisonDamage / 2;
            }
            if (status.getSkill().getId() == 2111006) {
                status.setValue(MonsterStatus.POISON, 0);
            } else {
                status.setValue(MonsterStatus.POISON, maxPoisonDamage);
            }
            // System.out.print("minPoisonDamage, maxPoisonDamage: " + minPoisonDamage + ", " + maxPoisonDamage + "\n");
            status.setPoisonSchedule(timerManager.register(new PoisonTask(minPoisonDamage, maxPoisonDamage, from, status, cancelTask, false), 1000, 1000));
        } else if (venom) {
            if (from.getJob() == MapleJob.NIGHTLORD || from.getJob() == MapleJob.SHADOWER) {
                int poisonLevel, matk;
                if (from.getJob() == MapleJob.NIGHTLORD) {
                    poisonLevel = from.getSkillLevel(SkillFactory.getSkill(4120005));
                    if (poisonLevel <= 0) {
                        return false;
                    }
                    matk = SkillFactory.getSkill(4120005).getEffect(poisonLevel).getMatk();
                } else if (from.getJob() == MapleJob.SHADOWER) {
                    poisonLevel = from.getSkillLevel(SkillFactory.getSkill(4220005));
                    if (poisonLevel <= 0) {
                        return false;
                    }
                    matk = SkillFactory.getSkill(4220005).getEffect(poisonLevel).getMatk();
                } else {
                    return false;
                }
                Random r = new Random();
                int luk = from.getLuk();
                int maxDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.2 * luk * matk));
                int minDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.1 * luk * matk));
                int gap = maxDmg - minDmg;
                if (gap == 0) {
                    gap = 1;
                }
                int poisonDamage = 0;
                for (int i = 0; i < getVenomMulti(); ++i) {
                    poisonDamage = poisonDamage + (r.nextInt(gap) + minDmg);
                }
                if (stats.getEffectiveness(Element.POISON) == ElementalEffectiveness.STRONG) { // 1/2 damage to those that are strong vs. poison
                    poisonDamage /= 2;
                }
                poisonDamage = Math.min(Short.MAX_VALUE, poisonDamage);
                status.setValue(MonsterStatus.POISON, poisonDamage);
                status.setPoisonSchedule(timerManager.register(new PoisonTask(poisonDamage, poisonDamage, from, status, cancelTask, false), 1000, 1000));
            } else {
                return false;
            }
        } else if (status.getSkill().getId() == 4111003) {
            int webDamage = (int) (getMaxHp() / 50.0 + 0.999);
            if (stats.getEffectiveness(Element.POISON) == ElementalEffectiveness.STRONG) { // 1/2 damage to those that are strong vs. poison
                webDamage /= 2;
            }
            status.setPoisonSchedule(timerManager.schedule(new PoisonTask(webDamage, webDamage, from, status, cancelTask, true), 3500));
        }
        for (MonsterStatus stat : status.getStati().keySet()) {
            stati.put(stat, status);
            //System.out.print("stati.put(" + stat.toString() + ", " + status.toString() + ");\n");
        }
        activeEffects.add(status);
        int animationTime = status.getSkill().getAnimationTime();
        MaplePacket packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), status.getStati(), status.getSkill().getId(), false, 0);
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().getSession().write(packet);
        }
        ScheduledFuture<?> schedule = timerManager.schedule(cancelTask, duration + animationTime);
        status.setCancelTask(schedule);
        return true;
    }
    
    //
    public boolean applyFlame(MapleCharacter from, ISkill skill, long duration, boolean charge) {
        cancelCancelFlameTask();
        cancelFlameSchedule();
        
        ElementalEffectiveness effectiveness = stats.getEffectiveness(skill.getElement());
        double damagemultiplier;
        switch (effectiveness) {
            case IMMUNE:
                return false;
            case STRONG:
                damagemultiplier = 0.5;
                break;
            case NORMAL:
                damagemultiplier = 1.0;
                break;
            case WEAK:
                damagemultiplier = 1.5;
                break;
            default:
                throw new RuntimeException("Unknown elemental effectiveness: " + stats.getEffectiveness(skill.getElement()));
        }
        
        //System.out.print("damagemultiplier: " + damagemultiplier + "\n");
        
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            setIsAflame(false);
            cancelFlameSchedule();
        };
        
        int minFlameDamage, maxFlameDamage;
        int flameLevel = from.getSkillLevel(skill);
        switch (skill.getId()) {
            case 5211004:
                {
                    int flameBasicAtk = (int) (((double) skill.getEffect(flameLevel).getDamage() + (charge ? 40.0 : 0.0)) / 100.0);
                    double afterBurnMultiplier = 0.4 + (double) flameLevel * 0.02;
                    double flameMastery = (10.0 + 5.0 * SkillFactory.getSkill(5200000).getEffect(from.getSkillLevel(SkillFactory.getSkill(5200000))).getMastery()) / 100.0;
                    int watk = from.getTotalWatk();
                    int primary = (int) (from.getDex() * 3.6);
                    int secondary = from.getStr();
                    minFlameDamage = (int) (((primary * 0.9 * flameMastery + (double) secondary) * (double) watk / 100.0) * flameBasicAtk * afterBurnMultiplier);
                    maxFlameDamage = (int) (((double) (primary + secondary) * (double) watk / 100.0) * flameBasicAtk * afterBurnMultiplier);
                    break;
                }
            case 2121003:
                {
                    double flameMastery = (10.0 + 5.0 * skill.getEffect(from.getSkillLevel(skill)).getMastery()) / 100.0;
                    maxFlameDamage = (int) (((from.getTotalMagic() * from.getTotalMagic() / 1000.0 + from.getTotalMagic()) / 30.0 + from.getInt() / 200.0) * skill.getEffect(flameLevel).getMatk());
                    minFlameDamage = (int) (((from.getTotalMagic() * from.getTotalMagic() / 1000.0 + from.getTotalMagic() * flameMastery * 0.9) / 30.0 + from.getInt() / 200.0) * skill.getEffect(flameLevel).getMatk());
                    break;
                }
            default:
                // more flamey-esque skills?
                return false;
        }
        minFlameDamage = (int) ((double) minFlameDamage * damagemultiplier);
        maxFlameDamage = (int) ((double) maxFlameDamage * damagemultiplier);
        setIsAflame(true);
        setFlameSchedule(timerManager.register(new FlameTask(minFlameDamage, maxFlameDamage, from, cancelTask), 1000, 1000));
        ScheduledFuture<?> schedule = timerManager.schedule(cancelTask, duration);
        setCancelFlameTask(schedule);
        return true;
    }
    
    public boolean isAflame() {
        return this.isaflame;
    }
    
    public void setIsAflame(boolean ia) {
        this.isaflame = ia;
    }
    
    public void setFlameSchedule(ScheduledFuture<?> fs) {
        this.flameSchedule = fs;
    }
    
    public void cancelFlameSchedule() {
        if (flameSchedule != null) {
            flameSchedule.cancel(false);
        }
    }
    
    public void setCancelFlameTask(ScheduledFuture<?> cft) {
        this.cancelFlameTask = cft;
    }
    
    public ScheduledFuture<?> getCancelFlameTask() {
        return this.cancelFlameTask;
    }
    
    public void cancelCancelFlameTask() {
        if (this.cancelFlameTask != null) {
            this.cancelFlameTask.cancel(false);
        }
    }
    
    public void setTempEffectiveness(Element e, ElementalEffectiveness ee, int duration) {
        cancelEffectivenessSchedule();
        if (this.originalEffectiveness.containsKey(e)) {
            this.setEffectiveness(e, this.originalEffectiveness.get(e));
        } else {
            this.originalEffectiveness.put(e, this.getEffectiveness(e));
        }
        
        if (this.getEffectiveness(e) != ElementalEffectiveness.IMMUNE) {
            this.setEffectiveness(e, ee);
        } else if (ee == ElementalEffectiveness.WEAK) {
            this.setEffectiveness(e, ElementalEffectiveness.NORMAL);
        } else if (ee == ElementalEffectiveness.NORMAL) {
            this.setEffectiveness(e, ElementalEffectiveness.STRONG);
        } else {
            this.setEffectiveness(e, ee);
        }
        
        final Element thiselement = e;
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = () -> setEffectiveness(thiselement, originalEffectiveness.get(thiselement));
        
        ScheduledFuture<?> schedule = timerManager.schedule(cancelTask, duration);
        setCancelEffectivenessTask(schedule);
    }
    
    public void cancelEffectivenessSchedule() {
        if (cancelEffectivenessTask != null) {
            cancelEffectivenessTask.cancel(false); // true
        }
    }
    
    public void setCancelEffectivenessTask(ScheduledFuture<?> cet) {
        this.cancelEffectivenessTask = cet;
    }
    
    public ScheduledFuture<?> getCancelEffectivenessTask() {
        return this.cancelEffectivenessTask;
    }
    //

    public void applyMonsterBuff(final MonsterStatus status, final int x, int skillId, long duration, MobSkill skill) {
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            if (isAlive()) {
                MaplePacket packet = MaplePacketCreator.cancelMonsterStatus(getObjectId(), Collections.singletonMap(status, x));
                map.broadcastMessage(packet, getPosition());
                if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                    getController().getClient().getSession().write(packet);
                }
                removeMonsterBuff(status);
            }
        };
        MaplePacket packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), Collections.singletonMap(status, x), skillId, true, 0, skill);
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().getSession().write(packet);
        }
        timerManager.schedule(cancelTask, duration);
        addMonsterBuff(status);

    }

    public void addMonsterBuff(MonsterStatus status) {
        this.monsterBuffs.add(status);
    }

    public void removeMonsterBuff(MonsterStatus status) {
        this.monsterBuffs.remove(status);
    }

    public boolean isBuffed(MonsterStatus status) {
        return this.monsterBuffs.contains(status);
    }
    
    public void setEffectiveness(Element e, ElementalEffectiveness ee) {
        stats.setEffectiveness(e, ee);
        addedEffectiveness.put(e, ee);
    }

    public void setFake(boolean fake) {
        this.fake = fake;
    }

    public boolean isFake() {
        return fake;
    }

    public MapleMap getMap() {
        return map;
    }

    public List<Pair<Integer, Integer>> getSkills() {
        return this.stats.getSkills();
    }

    public boolean hasSkill(int skillId, int level) {
        return stats.hasSkill(skillId, level);
    }

    public boolean canUseSkill(MobSkill toUse) {
        if (toUse == null) {
            return false;
        }
        for (Pair<Integer, Integer> skill : usedSkills) {
            if (skill.getLeft() == toUse.getSkillId() && skill.getRight() == toUse.getSkillLevel()) {
                return false;
            }
        }
        if (toUse.getLimit() > 0) {
            if (this.skillsUsed.containsKey(new Pair<>(toUse.getSkillId(), toUse.getSkillLevel()))) {
                int times = this.skillsUsed.get(new Pair<>(toUse.getSkillId(), toUse.getSkillLevel()));
                if (times >= toUse.getLimit()) {
                    return false;
                }
            }
        }
        if (toUse.getSkillId() == 200) {
            Collection<MapleMapObject> mmo = getMap().getMapObjects();
            int i = 0;
            for (MapleMapObject mo : mmo) {
                if (mo.getType() == MapleMapObjectType.MONSTER) {
                    i++;
                }
            }
            if (i > 100) {
                return false;
            }
        }
        return true;
    }

    public void usedSkill(final int skillId, final int level, long cooltime) {
        this.usedSkills.add(new Pair<>(skillId, level));
        if (this.skillsUsed.containsKey(new Pair<>(skillId, level))) {
            int times = this.skillsUsed.get(new Pair<>(skillId, level)) + 1;
            this.skillsUsed.remove(new Pair<>(skillId, level));
            this.skillsUsed.put(new Pair<>(skillId, level), times);
        } else {
            this.skillsUsed.put(new Pair<>(skillId, level), 1);
        }
        final MapleMonster mons = this;
        TimerManager tMan = TimerManager.getInstance();
        tMan.schedule(
                () -> mons.clearSkill(skillId, level), cooltime);
    }

    public void clearSkill(int skillId, int level) {
        int index = -1;
        for (Pair<Integer, Integer> skill : usedSkills) {
            if (skill.getLeft() == skillId && skill.getRight() == level) {
                index = usedSkills.indexOf(skill);
                break;
            }
        }
        if (index != -1) {
            usedSkills.remove(index);
        }
    }

    public int getNoSkills() {
        return this.stats.getNoSkills();
    }

    public boolean isFirstAttack() {
        return this.stats.isFirstAttack();
    }

    public int getBuffToGive() {
        return this.stats.getBuffToGive();
    }

    public List<MonsterStatus> getMonsterBuffs() {
        return monsterBuffs;
    }

    private final class PoisonTask implements Runnable {
        private final int minPoisonDamage;
        private final int maxPoisonDamage;
        private final MapleCharacter chr;
        private final MonsterStatusEffect status;
        private final Runnable cancelTask;
        private final boolean shadowWeb;
        private final MapleMap map;
        private PoisonTask(int minPoisonDamage, int maxPoisonDamage, MapleCharacter chr, MonsterStatusEffect status, Runnable cancelTask, boolean shadowWeb) {
            this.minPoisonDamage = minPoisonDamage;
            this.maxPoisonDamage = maxPoisonDamage;
            //System.out.print("this.minPoisonDamage, this.maxPoisonDamage: " + this.minPoisonDamage + ", " + this.maxPoisonDamage + "\n");
            this.chr = chr;
            this.status = status;
            this.cancelTask = cancelTask;
            this.shadowWeb = shadowWeb;
            this.map = chr.getMap();
        }
        @Override
        public void run() {
            int damage;
            if (minPoisonDamage == maxPoisonDamage) {
                damage = maxPoisonDamage;
            } else {
                damage = (int) (minPoisonDamage + StrictMath.random() * (maxPoisonDamage - minPoisonDamage + 1));
            }
            if (damage >= hp) {
                damage = hp - 1;
                if (!shadowWeb) {
                    try {
                        try {
                            cancelTask.run();
                        } catch (ArrayIndexOutOfBoundsException ignored) {
                        }
                        status.getCancelTask().cancel(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // This is a hack-fix that needs a bit more attention
                    }
                }
            }
            if (hp > 1 && damage > 0) {
                damage(chr, damage, false);
                if (shadowWeb || minPoisonDamage != maxPoisonDamage) {
                    map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), damage), getPosition());
                }
            }
        }
    }
    
    private final class FlameTask implements Runnable {
        private final int minFlameDamage;
        private final int maxFlameDamage;
        private final MapleCharacter chr;
        private final Runnable cancelTask;
        private final MapleMap map;
        private FlameTask(int minFlameDamage, int maxFlameDamage, MapleCharacter chr, Runnable cancelTask) {
            this.minFlameDamage = minFlameDamage;
            this.maxFlameDamage = maxFlameDamage;
            this.chr = chr;
            this.cancelTask = cancelTask;
            this.map = chr.getMap();
        }
        
        @Override
        public void run() {
            int damage;
            boolean docancel = false;
            if (minFlameDamage == maxFlameDamage) {
                damage = maxFlameDamage;
            } else {
                damage = (int) (minFlameDamage + Math.random() * (maxFlameDamage - minFlameDamage + 1));
            }
            if (damage >= hp) {
                docancel = true;
            }
            if (damage > 0) {
                map.damageMonster(chr, MapleMonster.this, damage);
                map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), damage), getPosition());
            }
            if (docancel) {
                try {
                    MapleMonster.this.cancelCancelFlameTask();
                    MapleMonster.this.cancelFlameSchedule();
                } catch (Exception e) {
                    e.printStackTrace(); //
                }
            }
        }
    }

    public String getName() {
        return stats.getName();
    }

    private class AttackingMapleCharacter {
        private final MapleCharacter attacker;
        private long lastAttackTime;
        public AttackingMapleCharacter(MapleCharacter attacker, long lastAttackTime) {
            super();
            this.attacker = attacker;
            this.lastAttackTime = lastAttackTime;
        }
        public long getLastAttackTime() {
            return lastAttackTime;
        }
        public void setLastAttackTime(long lastAttackTime) {
            this.lastAttackTime = lastAttackTime;
        }
        public MapleCharacter getAttacker() {
            return attacker;
        }
    }

    private interface AttackerEntry {
        List<AttackingMapleCharacter> getAttackers();
        void addDamage(MapleCharacter from, int damage, boolean updateAttackTime);
        int getDamage();
        boolean contains(MapleCharacter chr);
        void killedMob(MapleMap map, int baseExp, boolean mostDamage, boolean isboss);
    }

    private class SingleAttackerEntry implements AttackerEntry {
        private int damage;
        private final int chrid;
        private long lastAttackTime;
        private final ChannelServer cserv;
        public SingleAttackerEntry(MapleCharacter from, ChannelServer cserv) {
            this.chrid = from.getId();
            this.cserv = cserv;
        }

        @Override
        public void addDamage(MapleCharacter from, int damage, boolean updateAttackTime) {
            if (chrid == from.getId()) {
                this.damage += damage;
            } else {
                throw new IllegalArgumentException("Not the attacker of this entry");
            }
            if (updateAttackTime) {
                lastAttackTime = System.currentTimeMillis();
            }
        }

        @Override
        public List<AttackingMapleCharacter> getAttackers() {
            MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(chrid);
            if (chr != null) {
                return Collections.singletonList(new AttackingMapleCharacter(chr, lastAttackTime));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean contains(MapleCharacter chr) {
            return chrid == chr.getId();
        }

        @Override
        public int getDamage() {
            return damage;
        }

        @Override
        public void killedMob(MapleMap map, int baseExp, boolean mostDamage, boolean isboss) {
            MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(chrid);
            if (chr != null && chr.getMap() == map) {
                giveExpToCharacter(chr, baseExp, mostDamage, 1);
            }
        }

        @Override
        public int hashCode() {
            return chrid;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SingleAttackerEntry other = (SingleAttackerEntry) obj;
            return chrid == other.chrid;
        }
    }

    private static class OnePartyAttacker {
        public MapleParty lastKnownParty;
        public int damage;
        public long lastAttackTime;
        public OnePartyAttacker(MapleParty lastKnownParty, int damage) {
            super();
            this.lastKnownParty = lastKnownParty;
            this.damage = damage;
            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    private class PartyAttackerEntry implements AttackerEntry {
        private int totDamage;
        private final Map<Integer, OnePartyAttacker> attackers;
        private final int partyid;
        private final ChannelServer cserv;
        public PartyAttackerEntry(int partyid, ChannelServer cserv) {
            this.partyid = partyid;
            this.cserv = cserv;
            attackers = new HashMap<>(6);
        }
        @Override
        public List<AttackingMapleCharacter> getAttackers() {
            List<AttackingMapleCharacter> ret = new ArrayList<>(attackers.size());
            for (Entry<Integer, OnePartyAttacker> entry : attackers.entrySet()) {
                MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(entry.getKey());
                if (chr != null) {
                    ret.add(new AttackingMapleCharacter(chr, entry.getValue().lastAttackTime));
                }
            }
            return ret;
        }
        private Map<MapleCharacter, OnePartyAttacker> resolveAttackers() {
            Map<MapleCharacter, OnePartyAttacker> ret = new HashMap<>(attackers.size());
            for (Entry<Integer, OnePartyAttacker> aentry : attackers.entrySet()) {
                MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(aentry.getKey());
                if (chr != null) {
                    ret.put(chr, aentry.getValue());
                }
            }
            return ret;
        }
        @Override
        public boolean contains(MapleCharacter chr) {
            return attackers.containsKey(chr.getId());
        }
        @Override
        public int getDamage() {
            return totDamage;
        }
        @Override
        public void addDamage(MapleCharacter from, int damage, boolean updateAttackTime) {
            OnePartyAttacker oldPartyAttacker = attackers.get(from.getId());
            if (oldPartyAttacker != null) {
                oldPartyAttacker.damage += damage;
                oldPartyAttacker.lastKnownParty = from.getParty();
                if (updateAttackTime) {
                    oldPartyAttacker.lastAttackTime = System.currentTimeMillis();
                }
            } else {
                OnePartyAttacker onePartyAttacker = new OnePartyAttacker(from.getParty(), damage);
                attackers.put(from.getId(), onePartyAttacker);
                if (!updateAttackTime) {
                    onePartyAttacker.lastAttackTime = 0;
                }
            }
            totDamage += damage;
        }

        @Override
        public void killedMob(MapleMap map, int baseExp, boolean mostDamage, boolean isboss) {
            Map<MapleCharacter, OnePartyAttacker> attackers_ = resolveAttackers();
            MapleCharacter highest = null;
            int highestDamage = 0;
            Map<MapleCharacter, Integer> expMap = new ArrayMap<>(6);
            for (Entry<MapleCharacter, OnePartyAttacker> attacker : attackers_.entrySet()) {
                MapleParty party = attacker.getValue().lastKnownParty;
                double averagePartyLevel = 0;
                List<MapleCharacter> expApplicable = new ArrayList<>();
                for (MaplePartyCharacter partychar : party.getMembers()) {
                    if (attacker.getKey().getLevel() - partychar.getLevel() <= 5 || getLevel() - partychar.getLevel() <= 5) {
                        MapleCharacter pchr = cserv.getPlayerStorage().getCharacterByName(partychar.getName());
                        if (pchr != null) {
                            if (pchr.isAlive() && pchr.getMap() == map) {
                                expApplicable.add(pchr);
                                averagePartyLevel += pchr.getLevel();
                            }
                        }
                    }
                }
                double expBonus = 1.0;
                if (expApplicable.size() > 1) {
                    expBonus = 1.10 + 0.05 * expApplicable.size();
                    averagePartyLevel /= expApplicable.size();
                }
                int iDamage = attacker.getValue().damage;
                if (iDamage > highestDamage) {
                    highest = attacker.getKey();
                    highestDamage = iDamage;
                }
                double innerBaseExp = baseExp * ((double) iDamage / totDamage);
                double expFraction = (innerBaseExp * expBonus) / (expApplicable.size() + 1);
                for (MapleCharacter expReceiver : expApplicable) {
                    Integer oexp = expMap.get(expReceiver);
                    int iexp;
                    if (oexp == null) {
                        iexp = 0;
                    } else {
                        iexp = oexp;
                    }
                    double expWeight = (expReceiver == attacker.getKey() ? 2.0 : 1.0);
                    double levelMod = expReceiver.getLevel() / averagePartyLevel;
                    if (levelMod > 1.0 || this.attackers.containsKey(expReceiver.getId())) {
                        levelMod = 1.0;
                    }
                    iexp += (int) Math.round(expFraction * expWeight * levelMod);
                    expMap.put(expReceiver, iexp);
                }
            }
            for (Entry<MapleCharacter, Integer> expReceiver : expMap.entrySet()) {
                boolean white = mostDamage && expReceiver.getKey() == highest;
                if (highest != null && !isboss) {
                    if (expReceiver.getKey().getLevel() >= highest.getLevel() - 5) {
                        giveExpToCharacter(expReceiver.getKey(), expReceiver.getValue(), white, expMap.size());
                        if (expReceiver.getKey().getId() == highest.getId()) {
                            expReceiver.getKey().updateLastKillOnMap();
                        }
                    } else if (expReceiver.getKey().getLevel() >= highest.getLevel() - 40 && expReceiver.getKey().lastKillOnMapWithin(12)) {
                        // EXP receiver is within +inf/-40 lvls of killer and has killed a mob in map within the last 12 sec.
                        giveExpToCharacter(expReceiver.getKey(), expReceiver.getValue(), white, expMap.size());
                    }
                } else {
                    giveExpToCharacter(expReceiver.getKey(), expReceiver.getValue(), white, expMap.size());
                    expReceiver.getKey().updateLastKillOnMap();
                }
            }
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + partyid;
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PartyAttackerEntry other = (PartyAttackerEntry) obj;
            return partyid == other.partyid;
        }
    }

    public int getPADamage() {
        return stats.getPADamage();
    }
}
