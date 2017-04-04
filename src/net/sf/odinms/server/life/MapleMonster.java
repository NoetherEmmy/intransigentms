package net.sf.odinms.server.life;

import net.sf.odinms.client.*;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.scripting.event.EventInstanceManager;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonsterInformationProvider.DropEntry;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.ArrayMap;
import net.sf.odinms.tools.MaplePacketCreator;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MapleMonster extends AbstractLoadedMapleLife {
    public static final int MAX_BLEED_COUNT = 4;
    private MapleMonsterStats stats, overrideStats;
    private int hp, mp;
    private WeakReference<MapleCharacter> controller = new WeakReference<>(null);
    private boolean controllerHasAggro, controllerKnowsAboutAggro;
    private final Collection<AttackerEntry> attackers = new ArrayList<>(5);
    private EventInstanceManager eventInstance;
    private final List<MonsterListener> listeners = new ArrayList<>(2);
    private MapleCharacter highestDamageChar;
    private final Map<MonsterStatus, MonsterStatusEffect> stati = new LinkedHashMap<>(8);
    private final List<MonsterStatusEffect> activeEffects = Collections.synchronizedList(new ArrayList<>(5));
    private MapleMap map;
    private int venomMultiplier;
    private boolean fake = false;
    private boolean dropsDisabled = false;
    private final List<MobSkill> usedSkills = new ArrayList<>(5);
    private final Map<MobSkill, Integer> skillsUsed = new HashMap<>(8);
    private final Map<MonsterStatus, Integer> monsterBuffs = new HashMap<>(3);
    private final Map<Element, ElementalEffectiveness> addedEffectiveness = new HashMap<>(2, 0.7f);
    private boolean isAflame = false;
    private ScheduledFuture<?> flameSchedule;
    private ScheduledFuture<?> cancelFlameTask;
    private final Map<Element, ElementalEffectiveness> originalEffectiveness = new HashMap<>(2, 0.7f);
    private ScheduledFuture<?> cancelEffectivenessTask;
    public final AtomicInteger dropShareCount = new AtomicInteger();
    private ScheduledFuture<?> otherMobHitCheckTask;
    private long firstHit;
    private long lastHit;
    private double vulnerability = 1.0d;
    private ScheduledFuture<?> cancelVulnerabilityTask;
    private final Set<Integer> thieves = new HashSet<>(3);
    private boolean isPanicked = false;
    private ScheduledFuture<?> panicSchedule;
    private ScheduledFuture<?> cancelPanicTask;
    private final AtomicInteger coma = new AtomicInteger();
    private final Map<Integer, Integer> anatomicalThreats = new LinkedHashMap<>(2);
    private final List<BleedSchedule> bleeds = new LinkedList<>(); // Purposely LinkedList
    private final AtomicInteger runningBleedId = new AtomicInteger();

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
        dropsDisabled = true;
    }

    public boolean dropsDisabled() {
        return dropsDisabled;
    }

    public void setMap(MapleMap map) {
        this.map = map;
    }

    public int getDrop(final MapleCharacter owner) {
        final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        int lastAssigned = -1, minChance = 1;
        final List<DropEntry> dl =
            mi.retrieveDropChances(getId())
              .stream()
              .filter(de -> {
                  if (de.questId < 1) return true;
                  try {
                      MapleQuest quest = MapleQuest.getInstance(de.questId);
                      return
                          quest == null ||
                          owner.getQuest(quest).getStatus().equals(MapleQuestStatus.Status.STARTED);
                  } catch (Exception e) {
                      return false;
                  }
              })
              .collect(Collectors.toCollection(ArrayList::new));
        for (final DropEntry d : dl) {
            if (d.chance > minChance) minChance = d.chance;
        }
        for (final DropEntry d : dl) {
            d.assignedRangeStart = lastAssigned + 1;
            d.assignedRangeLength = (int) Math.ceil((1.0d / (double) d.chance) * (double) minChance);
            lastAssigned += d.assignedRangeLength;
        }
        final Random r = new Random();
        final int c = r.nextInt(minChance);
        for (final DropEntry d : dl) {
            if (c >= d.assignedRangeStart && c < d.assignedRangeStart + d.assignedRangeLength) {
                return d.itemId;
            }
        }
        return -1;
    }

    public int getSingleDrop() {
        final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        long cumulativeChances = 0L;
        final List<DropEntry> dropEntries = mi.retrieveDropChances(getId());
        for (final DropEntry d : dropEntries) {
            if (d.questId > 0) continue;
            cumulativeChances += (long) (Integer.MAX_VALUE / d.chance);
        }
        final long roll = (long) (Math.random() * cumulativeChances);
        long chanceLadder = 0L;
        for (final DropEntry d : dropEntries) {
            if (d.questId > 0) continue;
            chanceLadder += (long) (Integer.MAX_VALUE / d.chance);
            if (chanceLadder > roll) return d.itemId;
        }
        return dropEntries.get(dropEntries.size() - 1).itemId;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return overrideStats != null ? overrideStats.getHp() : stats.getHp();
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        if (mp < 0) mp = 0;
        this.mp = mp;
    }

    public int getMaxMp() {
        return overrideStats != null ? overrideStats.getMp() : stats.getMp();
    }

    public int getExp() {
        return overrideStats != null ? overrideStats.getExp() : stats.getExp();
    }

    public void startOtherMobHitChecking(final Runnable ifHit, long period, long delay) {
        if (otherMobHitCheckTask != null) return;
        otherMobHitCheckTask = TimerManager.getInstance().register(() -> {
            if (map.getMapObjectsInRange(getPosition(), 1000.0d, MapleMapObjectType.MONSTER).size() > 1) {
                ifHit.run();
            }
        }, period, delay);
    }

    public void stopOtherMobHitChecking() {
        if (otherMobHitCheckTask == null) return;
        otherMobHitCheckTask.cancel(false);
        otherMobHitCheckTask = null;
    }

    public boolean hasThieved(int charId) {
        return thieves.contains(charId);
    }

    public int thieve(int charId) {
        if (hasThieved(charId)) return -1;
        thieves.add(charId);
        return getSingleDrop();
    }

    public Set<Integer> readThieves() {
        return new HashSet<>(thieves);
    }

    public int getLevel() {
        return stats.getLevel();
    }

    public int getAccuracyBase() {
        return stats.getAccuracy();
    }

    public int getAccuracy() {
        return stats.getAccuracy() + getBuffedValue(MonsterStatus.ACC);
    }

    public int getAvoidBase() {
        return stats.getAvoid();
    }

    public int getAvoid() {
        return stats.getAvoid() + getBuffedValue(MonsterStatus.AVOID);
    }

    public int getRemoveAfter() {
        return stats.getRemoveAfter();
    }

    public int getVenomMulti() {
        return venomMultiplier;
    }

    public void setVenomMulti(int multiplier) {
        venomMultiplier = multiplier;
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

    public void applyVulnerability(double vuln, long duration) {
        if (cancelVulnerabilityTask != null && !cancelVulnerabilityTask.isDone()) {
            cancelVulnerabilityTask.cancel(false);
        }
        vulnerability = vuln;
        cancelVulnerabilityTask = TimerManager.getInstance().schedule(() -> vulnerability = 1.0d, duration);
    }

    public double getVulnerabilityBase() {
        return vulnerability;
    }

    public double getAnatomicalVulnerability() {
        final int vulnCounter =
            anatomicalThreats
                .values()
                .stream()
                .mapToInt(at -> at - 2)
                .filter(at -> at > 0)
                .sum();
        return
            vulnCounter > 0 ?
                (101.0d + Math.log(vulnCounter * vulnCounter * vulnCounter)) / 100.0d :
                1.0d;
    }

    public double getVulnerability() {
        return vulnerability * getAnatomicalVulnerability();
    }

    public void anatomicalStrike(int charId) {
        anatomicalThreats.merge(charId, 1, Integer::sum);
    }

    public void damage(MapleCharacter from, int damage, boolean updateAttackTime) {
        if (firstHit < 1L) firstHit = System.currentTimeMillis();

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
        if (!replaced) attackers.add(attacker);
        int rDamage = Math.max(0, Math.min(damage, hp));
        attacker.addDamage(from, rDamage, updateAttackTime);
        hp -= rDamage;
        int remhppercentage = (int) Math.ceil(((double) hp * 100.0d) / (double) getMaxHp());
        if (remhppercentage < 1) {
            remhppercentage = 1;
        }
        long okTime = System.currentTimeMillis() - 4000L;
        if (hasBossHPBar()) {
            from.getMap().broadcastMessage(makeBossHPBarPacket(), getPosition());
        } else if (!isBoss()) {
            for (AttackerEntry mattacker : attackers) {
                for (AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                    if (cattacker.getAttacker().getMap() == from.getMap()) {
                        if (cattacker.getLastAttackTime() >= okTime) {
                            cattacker
                                .getAttacker()
                                .getClient()
                                .getSession()
                                .write(
                                    MaplePacketCreator.showMonsterHP(
                                        getObjectId(),
                                        remhppercentage
                                    )
                                );
                        }
                    }
                }
            }
        }
        lastHit = System.currentTimeMillis();
    }

    public void heal(int hp, int mp) {
        int hp2Heal = this.hp + hp;
        int mp2Heal = this.mp + mp;
        if (hp2Heal >= getMaxHp()) {
            hp2Heal = getMaxHp();
        }
        if (mp2Heal >= getMaxMp()) {
            mp2Heal = getMaxMp();
        }
        setHp(hp2Heal);
        setMp(mp2Heal);
        map.broadcastMessage(MaplePacketCreator.healMonster(getObjectId(), hp));
    }

    public boolean isAttackedBy(final MapleCharacter chr) {
        return attackers.stream().anyMatch(a -> a.contains(chr));
    }

    private void giveExpToCharacter(MapleCharacter attacker, int exp, boolean highestDamage, int numExpSharers) {
        if (getId() == 9500196) { // Ghost
            exp = 0;
        }
        if (getLevel() >= 100 || attacker.getLevel() <= getLevel() + 20) {
            dropShareCount.incrementAndGet();
        }

        if (highestDamage) {
            if (eventInstance != null) {
                eventInstance.monsterKilled(attacker, this);
            }
            if (attacker.getPartyQuest() != null) {
                if (attacker.getPartyQuest().getMapInstance(map) != null) {
                    attacker.getPartyQuest().getMapInstance(map).invokeMethod("mobKilled", this, attacker);
                }
            }
            highestDamageChar = attacker;
        }

        if (attacker.getHp() <= 0) return;

        long personalExp = exp;
        if (exp > 0) {
            Integer holySymbol = attacker.getBuffedValue(MapleBuffStat.HOLY_SYMBOL);
            if (holySymbol != null) {
                if (numExpSharers <= 1) {
                    personalExp *= 1.0d + (holySymbol.doubleValue() / 500.0d);
                } else {
                    personalExp *= 1.0d + (holySymbol.doubleValue() / 100.0d);
                }
            }

            if (numExpSharers > 1) {
                personalExp *= 1.0d + (0.25d * ((double) numExpSharers - 1.0d));
            }

            double mltpercent = 1.0d;
            synchronized (activeEffects) {
                for (MonsterStatusEffect mse : activeEffects) {
                    if (mse.getSkill().getId() == 4121003 || mse.getSkill().getId() == 4221003) {
                        int percent = mse.getStati().get(MonsterStatus.SHOWDOWN) + 10;
                        double tempmltpercent = 1.0d + (double) percent / 100.0d;
                        if (tempmltpercent > mltpercent) {
                            mltpercent = tempmltpercent;
                        }
                    }
                }
            }

            if (mltpercent != 1.0d) {
                personalExp = (long) (personalExp * mltpercent);
            }
        }

        personalExp *= attacker.getAbsoluteXp();
        personalExp = (long) ((double) personalExp * attacker.getRelativeXp(getLevel()));

        while (personalExp > Integer.MAX_VALUE) {
            attacker.gainExp(Integer.MAX_VALUE, true, false, highestDamage, false);
            personalExp -= Integer.MAX_VALUE;
        }
        if (attacker.getMap().getId() != 2000) {
            attacker.gainExp((int) personalExp, true, false, highestDamage, false);
        }
        attacker.mobKilled(getId());
    }

    public MapleCharacter killBy(MapleCharacter killer) {
        // * killer.getClient().getPlayer().hasEXPCard()
        long totalBaseExpL = getExp() * ChannelServer.getInstance(killer.getClient().getChannel()).getExpRate();
        int totalBaseExp = (int) Math.min(Integer.MAX_VALUE, totalBaseExpL);
        AttackerEntry highest = null;
        int highDamage = 0;
        for (AttackerEntry attackEntry : attackers) {
            if (attackEntry.getDamage() > highDamage) {
                highest = attackEntry;
                highDamage = attackEntry.getDamage();
            }
        }
        for (AttackerEntry attackEntry : attackers) {
            int baseExp = (int) Math.ceil((double) totalBaseExp * ((double) attackEntry.getDamage() / (double) getMaxHp()));
            attackEntry.killedMob(killer.getMap(), baseExp, attackEntry == highest, isBoss());
        }
        if (getController() != null) {
            getController()
                .getClient()
                .getSession()
                .write(
                    MaplePacketCreator.stopControllingMonster(getObjectId())
                );
            getController().stopControllingMonster(this);
        }
        final List<Integer> toSpawn = getRevives();
        if (toSpawn != null) {
            final MapleMap reviveMap = killer.getMap();
            TimerManager.getInstance().schedule(() -> {
                for (Integer mid : toSpawn) {
                    MapleMonster mob = MapleLifeFactory.getMonster(mid);
                    if (mob == null) continue;
                    if (eventInstance != null) eventInstance.registerMonster(mob);
                    mob.setPosition(getPosition());
                    if (dropsDisabled()) mob.disableDrops();
                    reviveMap.spawnRevives(mob);
                }
            }, getAnimationTime("die1"));
        }
        if (eventInstance != null) {
            eventInstance.unregisterMonster(this);
        }
        synchronized (listeners) {
            for (MonsterListener listener : listeners) { //.toArray(new MonsterListener[listeners.size()])
                listener.monsterKilled(this);
            }
        }
        final MapleCharacter ret = highestDamageChar;
        highestDamageChar = null;
        return ret;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public MapleCharacter getController() {
        return controller.get();
    }

    public void setController(MapleCharacter controller) {
        this.controller = new WeakReference<>(controller);
    }

    public void switchController(MapleCharacter newController, boolean immediateAggro) {
        MapleCharacter controllers = getController();
        if (controllers == newController) return;
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
        return !fake && controllerHasAggro;
    }

    public void setControllerHasAggro(boolean controllerHasAggro) {
        if (fake) return;
        this.controllerHasAggro = controllerHasAggro;
    }

    public boolean isControllerKnowsAboutAggro() {
        return !fake && controllerKnowsAboutAggro;
    }

    public void setControllerKnowsAboutAggro(boolean controllerKnowsAboutAggro) {
        if (fake) return;
        this.controllerKnowsAboutAggro = controllerKnowsAboutAggro;
    }

    public MaplePacket makeBossHPBarPacket() {
        return MaplePacketCreator.showBossHP(getId(), hp, getMaxHp(), getTagColor(), getTagBgColor());
    }

    public boolean hasBossHPBar() {
        return (isBoss() && getTagColor() > 0) || isHT();
    }

    private boolean isHT() {
        return getId() == 8810018;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (!isAlive() || client.getPlayer().isFake()) return;
        if (fake) {
            client.getSession().write(MaplePacketCreator.spawnFakeMonster(this, 0));
        } else {
            client.getSession().write(MaplePacketCreator.spawnMonster(this, false));
        }
        if (!stati.isEmpty()) {
            synchronized (activeEffects) {
                for (MonsterStatusEffect mse : activeEffects) {
                    MaplePacket packet =
                        MaplePacketCreator.applyMonsterStatus(
                            getObjectId(),
                            mse.getStati(),
                            mse.getSkill().getId(),
                            false,
                            0
                        );
                    client.getSession().write(packet);
                }
            }
        }
        if (hasBossHPBar()) client.getSession().write(makeBossHPBarPacket());
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.killMonster(getObjectId(), false));
    }

    @Override
    public String toString() {
        return
            getName() + "(" + getId() + ") at " + getPosition().x + "/" + getPosition().y + " with " + hp +
            "/" + getMaxHp() + "hp, " + mp + "/" + getMaxMp() + " mp (alive: " + isAlive() + " oid: " +
            getObjectId() + ")";
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
        return
            !activeEffects.isEmpty() && stati.get(MonsterStatus.DOOM) != null ?
                ElementalEffectiveness.NORMAL :
                stats.getEffectiveness(e);
    }

    public ElementalEffectiveness getAddedEffectiveness(Element e) {
        return
            !activeEffects.isEmpty() && stati.get(MonsterStatus.DOOM) != null ?
                ElementalEffectiveness.NORMAL :
                addedEffectiveness.getOrDefault(e, ElementalEffectiveness.NORMAL);
    }

    public boolean applyStatus(MapleCharacter from, final MonsterStatusEffect status, boolean poison, long duration) {
        return applyStatus(from, status, poison, duration, false);
    }

    public boolean applyStatus(MapleCharacter from,
                               final MonsterStatusEffect status,
                               boolean poison,
                               long duration,
                               boolean venom) {
        switch (stats.getEffectiveness(status.getSkill().getElement())) {
            case IMMUNE:
            case STRONG:
                if (status.getSkill().getElement() != Element.POISON) return false;
                break;
            case NORMAL:
            case WEAK:
                break;
            default:
                throw new RuntimeException(
                    "Unknown elemental effectiveness: " +
                        stats.getEffectiveness(status.getSkill().getElement())
                );
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
        if (poison && hp <= 1) return false;

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
        if (
            isBoss() &&
            (
                status.getStati().containsKey(MonsterStatus.STUN) ||
                status.getStati().containsKey(MonsterStatus.FREEZE) ||
                status.getStati().containsKey(MonsterStatus.DOOM) ||
                status.getStati().containsKey(MonsterStatus.SHADOW_WEB) ||
                status.getStati().containsKey(MonsterStatus.SEAL)
            )
        ) {
            return false;
        }
        if (isBoss() && status.getStati().containsKey(MonsterStatus.MATK)) {
            status.getStati().put(MonsterStatus.MATK, status.getStati().get(MonsterStatus.MATK) / 2);
        }
        if (isBoss() && status.getStati().containsKey(MonsterStatus.WATK)) {
            status.getStati().put(MonsterStatus.WATK, status.getStati().get(MonsterStatus.WATK) / 2);
        }

        for (MonsterStatus stat : status.getStati().keySet()) {
            MonsterStatusEffect oldEffect = stati.get(stat);
            if (oldEffect != null) {
                oldEffect.removeActiveStatus(stat);
                if (oldEffect.getStati().isEmpty()) {
                    if (oldEffect.getCancelTask() != null) {
                        oldEffect.getCancelTask().cancel(false);
                    }
                    oldEffect.cancelPoisonSchedule();
                    activeEffects.remove(oldEffect);
                }
            }
        }

        final TimerManager tMan = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            if (isAlive()) {
                MaplePacket packet;
                synchronized (status) {
                    packet = MaplePacketCreator.cancelMonsterStatus(getObjectId(), status.getStati().keySet());
                }
                map.broadcastMessage(packet, getPosition());
                if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                    getController().getClient().getSession().write(packet);
                }
            }
            activeEffects.remove(status);
            synchronized (stati) {
                for (MonsterStatus stat : status.getStati().keySet()) {
                    stati.remove(stat);
                }
            }
            setVenomMulti(0);
            status.cancelPoisonSchedule();
        };
        if (!isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
            if (poison) {
                int minPoisonDamage, maxPoisonDamage;
                int poisonLevel = from.getSkillLevel(status.getSkill());
                if (status.getSkill().getId() == 2111006) {
                    int poisonBasicAtk = status.getSkill().getEffect(poisonLevel).getMatk();
                    double poisonMastery = ((double) status.getSkill().getEffect(poisonLevel).getMastery() * 5.0d + 10.0d) / 100.0d;
                    int matk = from.getTotalMagic();
                    int _int = from.getTotalInt();
                    ISkill eleAmp = SkillFactory.getSkill(2110001);
                    int eleAmpLevel = from.getSkillLevel(eleAmp);
                    double eleAmpMulti;
                    if (eleAmpLevel > 0) {
                        eleAmpMulti = (double) eleAmp.getEffect(from.getSkillLevel(eleAmp)).getY() / 100.0d;
                    } else {
                        eleAmpMulti = 1.0d;
                    }

                    minPoisonDamage = (int) ((((matk * matk) / 1000.0d + matk * poisonMastery * 0.9d) / 30.0d + _int / 200.0d) * poisonBasicAtk * eleAmpMulti);
                    maxPoisonDamage = (int) ((((matk * matk) / 1000.0d + matk) / 30.0d + _int / 200.0d) * poisonBasicAtk * eleAmpMulti);
                } else {
                    minPoisonDamage = (int) (getMaxHp() / (70.0d - poisonLevel) + 0.999d);
                    maxPoisonDamage = minPoisonDamage;
                }

                ElementalEffectiveness ee = addedEffectiveness.get(Element.POISON);
                if (ee == null) ee = stats.getEffectiveness(Element.POISON);
                double multiplier = getVulnerability();
                if (ee != null) {
                    switch (ee) {
                        case WEAK:
                            multiplier *= 1.5d;
                            break;
                        case STRONG:
                            multiplier *= 0.5d;
                            break;
                        case IMMUNE:
                            multiplier *= 0.0d;
                            break;
                    }
                }
                if (multiplier != 1.0d) {
                    minPoisonDamage *= multiplier;
                    maxPoisonDamage *= multiplier;
                }

                if (status.getSkill().getId() == 2111006) {
                    status.setValue(MonsterStatus.POISON, 0);
                } else {
                    status.setValue(MonsterStatus.POISON, maxPoisonDamage);
                }
                status.setPoisonSchedule(
                    tMan.register(
                        new PoisonTask(
                            minPoisonDamage,
                            maxPoisonDamage,
                            from,
                            status,
                            cancelTask,
                            false
                        ),
                        1000L,
                        1000L
                    )
                );
            } else if (venom) {
                if (from.getJob() == MapleJob.NIGHTLORD || from.getJob() == MapleJob.SHADOWER) {
                    int poisonLevel, matk;
                    if (from.getJob() == MapleJob.NIGHTLORD) {
                        poisonLevel = from.getSkillLevel(SkillFactory.getSkill(4120005));
                        if (poisonLevel <= 0) return false;
                        matk = SkillFactory.getSkill(4120005).getEffect(poisonLevel).getMatk();
                    } else if (from.getJob() == MapleJob.SHADOWER) {
                        poisonLevel = from.getSkillLevel(SkillFactory.getSkill(4220005));
                        if (poisonLevel <= 0) return false;
                        matk = SkillFactory.getSkill(4220005).getEffect(poisonLevel).getMatk();
                    } else {
                        return false;
                    }
                    Random r = new Random();
                    int luk = from.getTotalLuk();
                    int maxDmg = (int) Math.ceil(0.2d * luk * matk);
                    int minDmg = (int) Math.ceil(0.1d * luk * matk);

                    ElementalEffectiveness ee = addedEffectiveness.get(Element.POISON);
                    if (ee == null) ee = stats.getEffectiveness(Element.POISON);
                    double multiplier = getVulnerability();
                    if (ee != null) {
                        switch (ee) {
                            case WEAK:
                                multiplier *= 1.5d;
                                break;
                            case STRONG:
                                multiplier *= 0.5d;
                                break;
                            case IMMUNE:
                                multiplier *= 0.0d;
                                break;
                        }
                    }
                    if (multiplier != 1.0d) {
                        minDmg *= multiplier;
                        maxDmg *= multiplier;
                    }

                    int gap = maxDmg - minDmg;
                    if (gap == 0) gap = 1;
                    int poisonDamage = 0;
                    for (int i = 0; i < venomMultiplier; ++i) {
                        poisonDamage = poisonDamage + (r.nextInt(gap) + minDmg);
                    }

                    status.setValue(MonsterStatus.POISON, poisonDamage);
                    status.setPoisonSchedule(
                        tMan.register(
                            new PoisonTask(
                                poisonDamage,
                                poisonDamage,
                                from,
                                status,
                                cancelTask,
                                false
                            ),
                            1000L,
                            1000L
                        )
                    );
                } else {
                    return false;
                }
            } else if (status.getSkill().getId() == 4111003) {
                int webDamage = (int) (getMaxHp() / 50.0d + 0.999d);

                ElementalEffectiveness ee = addedEffectiveness.get(Element.POISON);
                if (ee == null) ee = stats.getEffectiveness(Element.POISON);
                double multiplier = getVulnerability();
                if (ee != null) {
                    switch (ee) {
                        case WEAK:
                            multiplier *= 1.5d;
                            break;
                        case STRONG:
                            multiplier *= 0.5d;
                            break;
                        case IMMUNE:
                            multiplier *= 0.0d;
                            break;
                    }
                }
                if (multiplier != 1.0d) {
                    webDamage *= multiplier;
                    webDamage *= multiplier;
                }

                status.setPoisonSchedule(
                    tMan.schedule(
                        new PoisonTask(
                            webDamage,
                            webDamage,
                            from,
                            status,
                            cancelTask,
                            true
                        ),
                        3500
                    )
                );
            }
        }
        for (MonsterStatus stat : status.getStati().keySet()) {
            stati.put(stat, status);
        }
        activeEffects.add(status);
        int animationTime = status.getSkill().getAnimationTime();
        MaplePacket packet =
            MaplePacketCreator.applyMonsterStatus(
                getObjectId(),
                status.getStati(),
                status.getSkill().getId(),
                false,
                0
            );
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().getSession().write(packet);
        }
        ScheduledFuture<?> schedule = tMan.schedule(cancelTask, duration + animationTime);
        status.setCancelTask(schedule);
        return true;
    }

    public boolean applyFlame(MapleCharacter from, ISkill skill, long duration, boolean charge) {
        cancelCancelFlameTask();
        cancelFlameSchedule();

        if (isBuffed(MonsterStatus.MAGIC_IMMUNITY)) return false;
        ElementalEffectiveness effectiveness = addedEffectiveness.get(Element.FIRE);
        if (effectiveness == null) effectiveness = stats.getEffectiveness(Element.FIRE);
        double damageMultiplier = getVulnerability();
        if (effectiveness != null) {
            switch (effectiveness) {
                case IMMUNE:
                    return false;
                case STRONG:
                    damageMultiplier *= 0.5d;
                    break;
                case NORMAL:
                    break;
                case WEAK:
                    damageMultiplier *= 1.5d;
                    break;
            }
        }

        TimerManager tMan = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            setIsAflame(false);
            cancelFlameSchedule();
        };

        int minFlameDamage, maxFlameDamage, tickTime;
        int flameLevel = from.getSkillLevel(skill);
        switch (skill.getId()) {
            case 5211004: { // Flamethrower
                tickTime = 1000;
                double flameBasicAtk = ((double) skill.getEffect(flameLevel).getDamage() + (charge ? 40.0d : 0.0d)) / 100.0d;
                double afterBurnMultiplier = flameLevel != 30 ? 0.4d + (double) flameLevel * 0.02d : 1.0d;
                double eleBoostMultiplier = 1.0d;
                if (from.getSkillLevel(5220001) > 0) {
                    MapleStatEffect eleBoost = SkillFactory.getSkill(5220001).getEffect(from.getSkillLevel(5220001));
                    eleBoostMultiplier += (eleBoost.getDamage() + eleBoost.getX()) / 100.0d;
                }

                minFlameDamage = (int) ((double) from.calculateMinBaseDamage() * flameBasicAtk * afterBurnMultiplier * eleBoostMultiplier);
                maxFlameDamage = (int) ((double) from.calculateMaxBaseDamage() * flameBasicAtk * afterBurnMultiplier * eleBoostMultiplier);
                break;
            }
            case 2121003: { // Fire Demon
                tickTime = 600;
                double flameMastery = (10.0d + 5.0d * skill.getEffect(from.getSkillLevel(skill)).getMastery()) / 100.0d;
                maxFlameDamage = (int) (((from.getTotalMagic() * from.getTotalMagic() / 1000.0d + from.getTotalMagic()) / 30.0d + from.getTotalInt() / 200.0d) * skill.getEffect(flameLevel).getMatk());
                minFlameDamage = (int) (((from.getTotalMagic() * from.getTotalMagic() / 1000.0d + from.getTotalMagic() * flameMastery * 0.9d) / 30.0d + from.getTotalInt() / 200.0d) * skill.getEffect(flameLevel).getMatk());
                break;
            }
            case 5111006: { // Shockwave
                tickTime = 1000;
                ISkill fistMastery = SkillFactory.getSkill(5100001);
                int fistMasteryLevel = from.getSkillLevel(fistMastery);
                double flameMastery = fistMasteryLevel > 0 ? ((double) fistMastery.getEffect(fistMasteryLevel).getMastery() * 5.0d + 10.0d) / 100.0d : 0.1d;
                maxFlameDamage = (int) (((from.getTotalMagic() * from.getTotalMagic() / 1000.0d + from.getTotalMagic()) / 30.0d + from.getTotalInt() / 200.0d) * (skill.getEffect(flameLevel).getDamage() / 3.0d));
                minFlameDamage = (int) (((from.getTotalMagic() * from.getTotalMagic() / 1000.0d + from.getTotalMagic() * flameMastery * 0.9d) / 30.0d + from.getTotalInt() / 200.0d) * (skill.getEffect(flameLevel).getDamage() / 3.0d));
                break;
            }
            case 3121006: { // Phoenix
                tickTime = 1000;
                double skillMulti = (5.0d * (double) flameLevel) / 100.0d;
                minFlameDamage = (int) (skillMulti * from.calculateMinBaseDamage());
                maxFlameDamage = (int) (skillMulti * from.calculateMaxBaseDamage());
                break;
            }
            default:
                // More flamey-esque skills?
                return false;
        }
        minFlameDamage = (int) ((double) minFlameDamage * damageMultiplier);
        maxFlameDamage = (int) ((double) maxFlameDamage * damageMultiplier);
        setIsAflame(true);
        setFlameSchedule(tMan.register(new FlameTask(minFlameDamage, maxFlameDamage, from, cancelTask), tickTime, tickTime));
        ScheduledFuture<?> schedule = tMan.schedule(cancelTask, duration);
        setCancelFlameTask(schedule);
        return true;
    }

    public boolean isAflame() {
        return isAflame;
    }

    public void setIsAflame(boolean ia) {
        isAflame = ia;
    }

    public void setFlameSchedule(ScheduledFuture<?> fs) {
        flameSchedule = fs;
    }

    public void cancelFlameSchedule() {
        if (flameSchedule != null) flameSchedule.cancel(false);
    }

    public void setCancelFlameTask(ScheduledFuture<?> cft) {
        cancelFlameTask = cft;
    }

    public ScheduledFuture<?> getCancelFlameTask() {
        return cancelFlameTask;
    }

    public void cancelCancelFlameTask() {
        if (cancelFlameTask != null) cancelFlameTask.cancel(false);
    }

    public boolean applyBleed(MapleCharacter from, ISkill skill, long duration) {
        if (isBuffed(MonsterStatus.WEAPON_IMMUNITY)) return false;

        TimerManager tMan = TimerManager.getInstance();

        final int bleedId = runningBleedId.getAndIncrement();
        final Runnable cancelTask = () -> cancelBleedSchedule(bleedId);

        int minBleedDamage, maxBleedDamage, tickTime;
        int bleedLevel = from.getSkillLevel(skill);
        switch (skill.getId()) {
            case 4211002: { // Assaulter
                tickTime = 500;
                int str = from.getTotalStr();
                int d = skill.getEffect(bleedLevel).getDamage();
                minBleedDamage = str * d / 64;
                maxBleedDamage = str * d / 48;
                break;
            }
            default:
                // More bleed skills?
                return false;
        }
        addBleedSchedule(
            new BleedSchedule(
                tMan.register(
                    new BleedTask(minBleedDamage, maxBleedDamage, from, bleedId),
                    tickTime,
                    tickTime
                ),
                tMan.schedule(cancelTask, duration),
                bleedId
            )
        );
        return true;
    }

    public boolean isBleeding() {
        return !bleeds.isEmpty();
    }

    public void addBleedSchedule(final BleedSchedule bleedSchedule) {
        synchronized (bleeds) {
            while (bleeds.size() >= MAX_BLEED_COUNT) {
                bleeds.remove(0).dispose();
            }
            bleeds.add(bleedSchedule);
        }
    }

    public void cancelBleedSchedule(int bleedScheduleId) {
        synchronized (bleeds) {
            int i = 0;
            for (BleedSchedule bleed : bleeds) {
                if (bleed.getId() == bleedScheduleId) break;
                i++;
            }
            if (i < bleeds.size()) {
                bleeds.remove(i).dispose();
            }
        }
    }

    public int bleedCount() {
        return bleeds.size();
    }

    public void stopBleeding() {
        bleeds.clear();
    }

    public void setTempEffectiveness(Element e, ElementalEffectiveness ee, int duration) {
        cancelEffectivenessSchedule();
        if (originalEffectiveness.containsKey(e)) {
            setEffectiveness(e, originalEffectiveness.get(e));
        } else {
            originalEffectiveness.put(e, getEffectiveness(e));
        }

        if (getEffectiveness(e) != ElementalEffectiveness.IMMUNE) {
            setEffectiveness(e, ee);
        } else if (ee == ElementalEffectiveness.WEAK) {
            setEffectiveness(e, ElementalEffectiveness.NORMAL);
        } else if (ee == ElementalEffectiveness.NORMAL) {
            setEffectiveness(e, ElementalEffectiveness.STRONG);
        } else {
            setEffectiveness(e, ee);
        }

        final Element thisElement = e;
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = () -> setEffectiveness(thisElement, originalEffectiveness.get(thisElement));

        ScheduledFuture<?> schedule = timerManager.schedule(cancelTask, duration);
        setCancelEffectivenessTask(schedule);
    }

    public void cancelEffectivenessSchedule() {
        if (cancelEffectivenessTask != null) {
            cancelEffectivenessTask.cancel(false); // true
        }
    }

    public void setCancelEffectivenessTask(ScheduledFuture<?> cet) {
        cancelEffectivenessTask = cet;
    }

    public ScheduledFuture<?> getCancelEffectivenessTask() {
        return cancelEffectivenessTask;
    }

    public long getFirstHit() {
        return firstHit;
    }

    public long getLastHit() {
        return lastHit;
    }

    public long allHitsDuration() {
        return lastHit - firstHit;
    }

    public double avgIncomingDpm() {
        if (allHitsDuration() == 0) return 0.0d;
        return (double) (getMaxHp() - Math.max(hp, 0)) / ((double) allHitsDuration() / 60000.0d);
    }

    public void applyMonsterBuff(final MonsterStatus status,
                                 final int x,
                                 int skillId,
                                 long duration,
                                 MobSkill skill) {
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            if (isAlive()) {
                MaplePacket packet = MaplePacketCreator.cancelMonsterStatus(
                    getObjectId(),
                    status
                );
                map.broadcastMessage(packet, getPosition());
                if (getController() != null && !getController().isMapObjectVisible(this)) {
                    getController().getClient().getSession().write(packet);
                }
                removeMonsterBuff(status);
            }
        };
        MaplePacket packet = MaplePacketCreator.applyMonsterStatus(
            getObjectId(),
            Collections.singletonMap(status, x),
            skillId,
            true,
            0,
            skill
        );
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().getSession().write(packet);
        }
        timerManager.schedule(cancelTask, duration);
        addMonsterBuff(status, x);
    }

    public void addMonsterBuff(MonsterStatus status, int x) {
        monsterBuffs.put(status, x);
    }

    public void removeMonsterBuff(MonsterStatus status) {
        monsterBuffs.remove(status);
    }

    public boolean isBuffed() {
        return !monsterBuffs.isEmpty();
    }

    public boolean isBuffed(MonsterStatus status) {
        return monsterBuffs.containsKey(status);
    }

    public int getBuffedValue(MonsterStatus status) {
        Integer val = monsterBuffs.get(status);
        return val == null ? 0 : val;
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

    public List<MobSkill> getSkills() {
        return stats.getSkills();
    }

    public boolean hasSkill(int skillId, int level) {
        return stats.hasSkill(skillId, level);
    }

    public boolean canUseSkill(MobSkill toUse) {
        if (toUse == null) return false;
        synchronized (usedSkills) {
            for (MobSkill skill : usedSkills) {
                if (skill.equals(toUse)) return false;
            }
        }
        if (toUse.getLimit() > 0 && skillsUsed.containsKey(toUse)) {
            int times = skillsUsed.get(toUse);
            if (times >= toUse.getLimit()) return false;
        }
        return !(toUse.getSkillId() == 200 && map.mobCount() > 100);
    }

    public void usedSkill(final int skillId, final int level, long cooltime) {
        final MobSkill skill = MobSkillFactory.getMobSkill(skillId, level);
        if (skill == null) {
            System.err.println(
                "skill == null in MapleMonster#usedSkill, mob ID: " +
                    getId() +
                    ", skill ID: " +
                    skillId +
                    ", skill level: " +
                    level
            );
            return;
        }
        usedSkills.add(skill);
        skillsUsed.merge(skill, 1, Integer::sum);
        TimerManager.getInstance().schedule(() -> clearSkill(skill), cooltime);
    }

    public void clearSkill(int skillId, int level) {
        clearSkill(MobSkillFactory.getMobSkill(skillId, level));
    }

    public void clearSkill(MobSkill skill) {
        usedSkills.remove(skill);
    }

    public int getNoSkills() {
        return stats.getNoSkills();
    }

    public boolean isFirstAttack() {
        return stats.isFirstAttack();
    }

    public int getBuffToGive() {
        return stats.getBuffToGive();
    }

    public Map<MonsterStatus, Integer> readMonsterBuffs() {
        return new HashMap<>(monsterBuffs);
    }

    public boolean panic(MapleCharacter from, ISkill skill, long duration) {
        cancelCancelPanicTask();
        cancelPanicSchedule();

        TimerManager tMan = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            setIsPanicked(false);
            cancelPanicSchedule();
        };

        int tickTime, minPanicDamage, maxPanicDamage;
        double proc;
        int panicLevel = from.getSkillLevel(skill);
        switch (skill.getId()) {
            case 1111003:   // Panic: Sword
            case 1111004: { // Panic: Axe
                tickTime = 250;
                proc = 0.125d;
                final int pad = getPADamage();
                minPanicDamage = pad * pad / 4;
                maxPanicDamage = pad * pad / 3;
                break;
            }
            default:
                // More panic skills?
                return false;
        }
        setIsPanicked(true);
        setPanicSchedule(
            tMan.register(
                new PanicTask(minPanicDamage, maxPanicDamage, proc, from, cancelTask),
                tickTime,
                tickTime
            )
        );
        ScheduledFuture<?> schedule = tMan.schedule(cancelTask, duration);
        setCancelPanicTask(schedule);
        return true;
    }

    public boolean isPanicked() {
        return isPanicked;
    }

    public void setIsPanicked(boolean ip) {
        isPanicked = ip;
    }

    public void setPanicSchedule(ScheduledFuture<?> ps) {
        panicSchedule = ps;
    }

    public void cancelPanicSchedule() {
        if (panicSchedule != null) panicSchedule.cancel(false);
    }

    public void setCancelPanicTask(ScheduledFuture<?> cpt) {
        cancelPanicTask = cpt;
    }

    public ScheduledFuture<?> getCancelPanicTask() {
        return cancelPanicTask;
    }

    public void cancelCancelPanicTask() {
        if (cancelPanicTask != null) cancelPanicTask.cancel(false);
    }

    public int reduceComa() {
        return coma.accumulateAndGet(1, (curr, update) -> Math.max(curr - update, 0));
    }

    public int increaseComa() {
        return coma.incrementAndGet();
    }

    /** Sets the value of {@code coma} to its current value, or {@code c}, whichever is greater. */
    public int softSetComa(int c) {
        return coma.accumulateAndGet(c, Math::max);
    }

    public void setComa(int c) {
        coma.set(c);
    }

    public int getComa() {
        return coma.get();
    }

    private final class PoisonTask implements Runnable {
        private final int minPoisonDamage, maxPoisonDamage;
        private final MapleCharacter chr;
        private final MonsterStatusEffect status;
        private final Runnable cancelTask;
        private final boolean shadowWeb;
        private final MapleMap map;

        private PoisonTask(int minPoisonDamage,
                           int maxPoisonDamage,
                           MapleCharacter chr,
                           MonsterStatusEffect status,
                           Runnable cancelTask,
                           boolean shadowWeb) {
            this.minPoisonDamage = minPoisonDamage;
            this.maxPoisonDamage = maxPoisonDamage;
            this.chr = chr;
            this.status = status;
            this.cancelTask = cancelTask;
            this.shadowWeb = shadowWeb;
            this.map = chr.getMap();
        }

        @Override
        public void run() {
            int damage;
            if (isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
                try {
                    try {
                        cancelTask.run();
                    } catch (IndexOutOfBoundsException ignored) {
                    }
                    status.getCancelTask().cancel(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            if (minPoisonDamage == maxPoisonDamage) {
                damage = (int) (minPoisonDamage - getMdef() * 0.6d * (1.0d + 0.01d * Math.max(getLevel() - chr.getLevel(), 0.0d)));
                damage = Math.max(1, damage);
            } else {
                int localMinDmg = (int) (minPoisonDamage - getMdef() * 0.6d * (1.0d + 0.01d * Math.max(getLevel() - chr.getLevel(), 0.0d)));
                localMinDmg = Math.max(1, localMinDmg);
                int localMaxDmg = (int) (maxPoisonDamage - getMdef() * 0.5d * (1.0d + 0.01d * Math.max(getLevel() - chr.getLevel(), 0.0d)));
                localMaxDmg = Math.max(1, localMaxDmg);
                damage = (int) (localMinDmg + Math.random() * (localMaxDmg - localMinDmg + 1.0d));
            }
            if (damage >= hp) {
                damage = hp - 1;
                if (!shadowWeb) {
                    try {
                        try {
                            cancelTask.run();
                        } catch (IndexOutOfBoundsException ignored) {
                        }
                        status.getCancelTask().cancel(false);
                    } catch (Exception e) {
                        e.printStackTrace();
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
            if (isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
                damage = 0;
                docancel = true;
            } else if (minFlameDamage == maxFlameDamage) {
                damage = (int) (minFlameDamage - getMdef() * 0.6d * (1.0d + 0.01d * Math.max(getLevel() - chr.getLevel(), 0.0d)));
                damage = Math.max(1, damage);
            } else {
                int localMinDmg = (int) (minFlameDamage - getMdef() * 0.6d * (1.0d + 0.01d * Math.max(getLevel() - chr.getLevel(), 0.0d)));
                localMinDmg = Math.max(1, localMinDmg);
                int localMaxDmg = (int) (maxFlameDamage - getMdef() * 0.5d * (1.0d + 0.01d * Math.max(getLevel() - chr.getLevel(), 0.0d)));
                localMaxDmg = Math.max(1, localMaxDmg);
                damage = (int) (localMinDmg + Math.random() * (localMaxDmg - localMinDmg + 1));
            }
            if (damage >= hp) docancel = true;
            if (damage > 0) {
                map.damageMonster(chr, MapleMonster.this, damage);
                map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), damage), getPosition());
            }
            if (docancel) {
                try {
                    cancelCancelFlameTask();
                    cancelFlameSchedule();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final class BleedTask implements Runnable {
        private final int minBleedDamage;
        private final int maxBleedDamage;
        private final MapleCharacter chr;
        private final int bleedScheduleId;
        private final MapleMap map;

        private BleedTask(int minBleedDamage, int maxBleedDamage, MapleCharacter chr, int bleedScheduleId) {
            this.minBleedDamage = minBleedDamage;
            this.maxBleedDamage = maxBleedDamage;
            this.chr = chr;
            this.bleedScheduleId = bleedScheduleId;
            this.map = chr.getMap();
        }

        @Override
        public void run() {
            int damage;
            boolean docancel = false;
            if (isBuffed(MonsterStatus.WEAPON_IMMUNITY)) {
                damage = 0;
                docancel = true;
            } else if (minBleedDamage == maxBleedDamage) {
                damage = (int) (minBleedDamage - getWdef() * 0.6d * (1.0d + 0.01d * Math.max(getLevel() - chr.getLevel(), 0.0d)));
                damage = Math.max(1, damage);
            } else {
                int localMinDmg = (int) (minBleedDamage - getWdef() * 0.6d * (1.0d + 0.01d * Math.max(getLevel() - chr.getLevel(), 0.0d)));
                localMinDmg = Math.max(1, localMinDmg);
                int localMaxDmg = (int) (maxBleedDamage - getWdef() * 0.5d * (1.0d + 0.01d * Math.max(getLevel() - chr.getLevel(), 0.0d)));
                localMaxDmg = Math.max(1, localMaxDmg);
                damage = (int) (localMinDmg + Math.random() * (localMaxDmg - localMinDmg + 1));
            }
            if (damage >= hp) docancel = true;
            if (damage > 0) {
                map.damageMonster(chr, MapleMonster.this, damage);
                map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), damage), getPosition());
            }
            if (docancel) cancelBleedSchedule(bleedScheduleId);
        }
    }

    private final class BleedSchedule {
        private final ScheduledFuture<?> bleedTask;
        private final ScheduledFuture<?> cancelTask;
        private final int id;

        public BleedSchedule(ScheduledFuture<?> bleedTask, ScheduledFuture<?> cancelTask, int id) {
            this.bleedTask = bleedTask;
            this.cancelTask = cancelTask;
            this.id = id;
        }

        public ScheduledFuture<?> getBleedTask() {
            return bleedTask;
        }

        public ScheduledFuture<?> getCancelTask() {
            return cancelTask;
        }

        public int getId() {
            return id;
        }

        public void dispose() {
            cancelTask.cancel(false);
            bleedTask.cancel(false);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (getClass() != o.getClass()) return false;
            final BleedSchedule other = (BleedSchedule) o;
            return id == other.id;
        }
    }

    private final class PanicTask implements Runnable {
        private final int minPanicDamage;
        private final int maxPanicDamage;
        private final double proc;
        private final MapleCharacter chr;
        private final Runnable cancelTask;
        private final MapleMap map;
        private final Random rand;

        private PanicTask(int minPanicDamage, int maxPanicDamage, double proc, MapleCharacter chr, Runnable cancelTask) {
            this.minPanicDamage = minPanicDamage;
            this.maxPanicDamage = maxPanicDamage;
            this.proc = proc;
            this.chr = chr;
            this.cancelTask = cancelTask;
            this.map = chr.getMap();
            rand = new Random();
        }

        @Override
        public void run() {
            // Only run if proc'd:
            if (rand.nextDouble() > proc) return;
            boolean doCancel = false;
            // Damage self:
            int selfDamage =
                isBuffed(MonsterStatus.WEAPON_IMMUNITY) ?
                    1 :
                    minPanicDamage + rand.nextInt(maxPanicDamage - minPanicDamage + 1) - 10 * getWdef();
            if (selfDamage >= hp) doCancel = true;
            if (selfDamage > 0) {
                map.damageMonster(chr, MapleMonster.this, selfDamage);
                map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), selfDamage), getPosition());
            }
            // Then damage up to 6 nearby mobs:
            map.getMapObjectsInRange(
                getPosition(),
                40000.0d,
                MapleMapObjectType.MONSTER
            )
            .stream()
            .map(mmo -> (MapleMonster) mmo)
            .limit(6L)
            .forEach(m -> {
                int damage =
                    m.isBuffed(MonsterStatus.WEAPON_IMMUNITY) ?
                        1 :
                        minPanicDamage + rand.nextInt(maxPanicDamage - minPanicDamage + 1) - 10 * m.getWdef();
                if (damage > 0) {
                    map.damageMonster(chr, m, damage);
                    map.broadcastMessage(MaplePacketCreator.damageMonster(m.getObjectId(), damage), m.getPosition());
                }
            });
            // Cancel if the mob is dead:
            if (doCancel) {
                try {
                    cancelCancelPanicTask();
                    cancelPanicSchedule();
                } catch (Exception e) {
                    e.printStackTrace();
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
            chrid = from.getId();
            this.cserv = cserv;
        }

        @Override
        public void addDamage(MapleCharacter from, int damage, boolean updateAttackTime) {
            if (chrid == from.getId()) {
                this.damage += damage;
            } else {
                throw new IllegalArgumentException("Not the attacker of this entry");
            }
            if (updateAttackTime) lastAttackTime = System.currentTimeMillis();
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
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
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
            attackers = new LinkedHashMap<>(8, 0.8125f);
        }

        @Override
        public List<AttackingMapleCharacter> getAttackers() {
            List<AttackingMapleCharacter> ret = new ArrayList<>(attackers.size());
            for (Map.Entry<Integer, OnePartyAttacker> entry : attackers.entrySet()) {
                MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(entry.getKey());
                if (chr != null) {
                    ret.add(new AttackingMapleCharacter(chr, entry.getValue().lastAttackTime));
                }
            }
            return ret;
        }

        private Map<MapleCharacter, OnePartyAttacker> resolveAttackers() {
            Map<MapleCharacter, OnePartyAttacker> ret = new LinkedHashMap<>(attackers.size());
            for (Map.Entry<Integer, OnePartyAttacker> aentry : attackers.entrySet()) {
                MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(aentry.getKey());
                if (chr != null) ret.put(chr, aentry.getValue());
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
                    onePartyAttacker.lastAttackTime = 0L;
                }
            }
            totDamage += damage;
        }

        @Override
        public void killedMob(MapleMap map, int baseExp, boolean mostDamage, boolean isBoss) {
            Map<MapleCharacter, OnePartyAttacker> attackers_ = resolveAttackers();
            MapleCharacter highest = null;
            int highestDamage = 0;
            Map<MapleCharacter, Integer> expMap = new ArrayMap<>(6);
            for (Map.Entry<MapleCharacter, OnePartyAttacker> attacker : attackers_.entrySet()) {
                final MapleParty party = attacker.getValue().lastKnownParty;
                double averagePartyLevel = 0.0d;
                List<MapleCharacter> expApplicable = new ArrayList<>(6);
                for (MaplePartyCharacter partyChar : party.getMembers()) {
                    //if (
                    //    attacker.getKey().getLevel() - partyChar.getLevel() <= 15 ||
                    //    getLevel() - partyChar.getLevel() <= 15
                    //) {
                    MapleCharacter pchr = cserv.getPlayerStorage().getCharacterByName(partyChar.getName());
                    if (pchr != null) {
                        if (pchr.isAlive() && pchr.getMap() == map) {
                            expApplicable.add(pchr);
                            averagePartyLevel += (double) pchr.getLevel();
                        }
                    }
                    //}
                }

                double expBonus = 1.0d;
                if (expApplicable.size() > 1) {
                    expBonus = 1.1d + 0.05d * (double) expApplicable.size();
                    averagePartyLevel /= (double) expApplicable.size();
                }

                int iDamage = attacker.getValue().damage;
                if (iDamage > highestDamage) {
                    highest = attacker.getKey();
                    highestDamage = iDamage;
                }

                double innerBaseExp = (double) baseExp * (double) iDamage / (double) totDamage;
                double expFraction = innerBaseExp * expBonus / ((double) expApplicable.size() + 1.0d);
                for (MapleCharacter expReceiver : expApplicable) {
                    Integer oexp = expMap.get(expReceiver);
                    int iexp = oexp == null ? 0 : oexp;
                    double expWeight = expReceiver == attacker.getKey() ? 2.0d : 1.0d;
                    double levelMod = (double) expReceiver.getLevel() / averagePartyLevel;
                    if (levelMod > 1.0d || attackers.containsKey(expReceiver.getId())) {
                        levelMod = 1.0d;
                    }
                    iexp += (int) Math.round(expFraction * expWeight * levelMod);
                    expMap.put(expReceiver, iexp);
                }
            }

            for (Map.Entry<MapleCharacter, Integer> expReceiver : expMap.entrySet()) {
                boolean white = mostDamage && expReceiver.getKey() == highest;
                if (highest != null && !isBoss) {
                    if (expReceiver.getKey().getLevel() >= highest.getLevel() - 15) {
                        giveExpToCharacter(expReceiver.getKey(), expReceiver.getValue(), white, expMap.size());
                        if (expReceiver.getKey().getId() == highest.getId()) {
                            expReceiver.getKey().updateLastKillOnMap();
                        }
                    } else if (
                        expReceiver.getKey().getLevel() >= highest.getLevel() - 60 &&
                        expReceiver.getKey().lastKillOnMapWithin(16)
                    ) {
                        // EXP receiver is within +inf/-60 lvls of killer and
                        // has killed a mob in map within the last 16 sec.
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (getClass() != o.getClass()) return false;
            final PartyAttackerEntry other = (PartyAttackerEntry) o;
            return partyid == other.partyid;
        }
    }

    public int getPADamageBase() {
        return stats.getPADamage();
    }

    public int getPADamage() {
        final int base = isBuffed(MonsterStatus.DOOM) ? 27 /* PAD of Blue Snail */ : stats.getPADamage();
        return
            base +
                getBuffedValue(MonsterStatus.WEAPON_ATTACK_UP) +
                getBuffedValue(MonsterStatus.WATK);
    }

    public int getWdefBase() {
        return stats.getWdef();
    }

    public int getWdef() {
        return
            stats.getWdef() +
                getBuffedValue(MonsterStatus.WEAPON_DEFENSE_UP) +
                getBuffedValue(MonsterStatus.WDEF);
    }

    public int getMdefBase() {
        return stats.getMdef();
    }

    public int getMdef() {
        return
            stats.getMdef() +
                getBuffedValue(MonsterStatus.MAGIC_DEFENSE_UP) +
                getBuffedValue(MonsterStatus.MDEF);
    }
}
