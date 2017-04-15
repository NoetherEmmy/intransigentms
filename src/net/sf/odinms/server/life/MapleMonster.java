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

    public MapleMonster(final int id, final MapleMonsterStats stats) {
        super(id);
        initWithStats(stats);
    }

    public MapleMonster(final MapleMonster monster) {
        super(monster);
        initWithStats(monster.stats);
    }

    private void initWithStats(final MapleMonsterStats stats) {
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

    public void setMap(final MapleMap map) {
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
        return dl.stream()
                 .filter(d -> c >= d.assignedRangeStart && c < d.assignedRangeStart + d.assignedRangeLength)
                 .findFirst()
                 .map(d -> d.itemId)
                 .orElse(-1);
    }

    public int getSingleDrop() {
        final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        final List<DropEntry> dropEntries = mi.retrieveDropChances(getId());
        final long cumulativeChances =
            dropEntries
                .stream()
                .filter(d -> d.questId <= 0)
                .mapToLong(d -> (long) (Integer.MAX_VALUE / d.chance))
                .sum();
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

    public void setHp(final int hp) {
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

    public void startOtherMobHitChecking(final Runnable ifHit, final long period, final long delay) {
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

    public boolean hasThieved(final int charId) {
        return thieves.contains(charId);
    }

    public int thieve(final int charId) {
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

    public void setVenomMulti(final int multiplier) {
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

    public int getAnimationTime(final String name) {
        return stats.getAnimationTime(name);
    }

    public List<Integer> getRevives() {
        return stats.getRevives();
    }

    public void setOverrideStats(final MapleMonsterStats overrideStats) {
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

    public void applyVulnerability(final double vuln, final long duration) {
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

    public void anatomicalStrike(final int charId) {
        anatomicalThreats.merge(charId, 1, Integer::sum);
    }

    public void damage(final MapleCharacter from, final int damage, final boolean updateAttackTime) {
        if (firstHit < 1L) firstHit = System.currentTimeMillis();

        AttackerEntry attacker;
        if (from.getParty() != null) {
            attacker = new PartyAttackerEntry(from.getParty().getId(), from.getClient().getChannelServer());
        } else {
            attacker = new SingleAttackerEntry(from, from.getClient().getChannelServer());
        }
        boolean replaced = false;
        for (final AttackerEntry aentry : attackers) {
            if (aentry.equals(attacker)) {
                attacker = aentry;
                replaced = true;
                break;
            }
        }
        if (!replaced) attackers.add(attacker);
        final int rDamage = Math.max(0, Math.min(damage, hp));
        attacker.addDamage(from, rDamage, updateAttackTime);
        hp -= rDamage;
        int remhppercentage = (int) Math.ceil(((double) hp * 100.0d) / (double) getMaxHp());
        if (remhppercentage < 1) {
            remhppercentage = 1;
        }
        final long okTime = System.currentTimeMillis() - 4000L;
        if (hasBossHPBar()) {
            from.getMap().broadcastMessage(makeBossHPBarPacket(), getPosition());
        } else if (!isBoss()) {
            for (final AttackerEntry mattacker : attackers) {
                for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
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

    public void heal(final int hp, final int mp) {
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

    private void giveExpToCharacter(final MapleCharacter attacker, int exp, final boolean highestDamage, final int numExpSharers) {
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
            final Integer holySymbol = attacker.getBuffedValue(MapleBuffStat.HOLY_SYMBOL);
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
                for (final MonsterStatusEffect mse : activeEffects) {
                    if (mse.getSkill().getId() == 4121003 || mse.getSkill().getId() == 4221003) {
                        final int percent = mse.getStati().get(MonsterStatus.SHOWDOWN) + 10;
                        final double tempmltpercent = 1.0d + (double) percent / 100.0d;
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

    public MapleCharacter killBy(final MapleCharacter killer) {
        // * killer.getClient().getPlayer().hasEXPCard()
        final long totalBaseExpL = getExp() * ChannelServer.getInstance(killer.getClient().getChannel()).getExpRate();
        final int totalBaseExp = (int) Math.min(Integer.MAX_VALUE, totalBaseExpL);
        AttackerEntry highest = null;
        int highDamage = 0;
        for (final AttackerEntry attackEntry : attackers) {
            if (attackEntry.getDamage() > highDamage) {
                highest = attackEntry;
                highDamage = attackEntry.getDamage();
            }
        }
        for (final AttackerEntry attackEntry : attackers) {
            final int baseExp = (int) Math.ceil((double) totalBaseExp * ((double) attackEntry.getDamage() / (double) getMaxHp()));
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
            final MapleMap reviveMap = map;
            TimerManager.getInstance().schedule(() -> {
                for (final Integer mid : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(mid);
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
            for (final MonsterListener listener : listeners) { //.toArray(new MonsterListener[listeners.size()])
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

    public void setController(final MapleCharacter controller) {
        this.controller = new WeakReference<>(controller);
    }

    public void switchController(final MapleCharacter newController, final boolean immediateAggro) {
        final MapleCharacter controllers = getController();
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

    public void addListener(final MonsterListener listener) {
        listeners.add(listener);
    }

    public void removeListener(final MonsterListener listener) {
        listeners.remove(listener);
    }

    public boolean isControllerHasAggro() {
        return !fake && controllerHasAggro;
    }

    public void setControllerHasAggro(final boolean controllerHasAggro) {
        if (fake) return;
        this.controllerHasAggro = controllerHasAggro;
    }

    public boolean isControllerKnowsAboutAggro() {
        return !fake && controllerKnowsAboutAggro;
    }

    public void setControllerKnowsAboutAggro(final boolean controllerKnowsAboutAggro) {
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
    public void sendSpawnData(final MapleClient client) {
        if (!isAlive() || client.getPlayer().isFake()) return;
        if (fake) {
            client.getSession().write(MaplePacketCreator.spawnFakeMonster(this, 0));
        } else {
            client.getSession().write(MaplePacketCreator.spawnMonster(this, false));
        }
        if (!stati.isEmpty()) {
            synchronized (activeEffects) {
                for (final MonsterStatusEffect mse : activeEffects) {
                    final MaplePacket packet =
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
    public void sendDestroyData(final MapleClient client) {
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

    public void setEventInstance(final EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public boolean isMobile() {
        return stats.isMobile();
    }

    public ElementalEffectiveness getEffectiveness(final Element e) {
        return
            !activeEffects.isEmpty() && stati.get(MonsterStatus.DOOM) != null ?
                ElementalEffectiveness.NORMAL :
                stats.getEffectiveness(e);
    }

    public ElementalEffectiveness getAddedEffectiveness(final Element e) {
        return
            !activeEffects.isEmpty() && stati.get(MonsterStatus.DOOM) != null ?
                ElementalEffectiveness.NORMAL :
                addedEffectiveness.getOrDefault(e, ElementalEffectiveness.NORMAL);
    }

    public boolean applyStatus(final MapleCharacter from, final MonsterStatusEffect status, final boolean poison, final long duration) {
        return applyStatus(from, status, poison, duration, false);
    }

    public boolean applyStatus(final MapleCharacter from,
                               final MonsterStatusEffect status,
                               final boolean poison,
                               final long duration,
                               final boolean venom) {
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
        final ElementalEffectiveness effectiveness;
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

        for (final MonsterStatus stat : status.getStati().keySet()) {
            final MonsterStatusEffect oldEffect = stati.get(stat);
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
                final MaplePacket packet;
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
                for (final MonsterStatus stat : status.getStati().keySet()) {
                    stati.remove(stat);
                }
            }
            setVenomMulti(0);
            status.cancelPoisonSchedule();
        };
        if (!isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
            if (poison) {
                int minPoisonDamage, maxPoisonDamage;
                final int poisonLevel = from.getSkillLevel(status.getSkill());
                if (status.getSkill().getId() == 2111006) {
                    final int poisonBasicAtk = status.getSkill().getEffect(poisonLevel).getMatk();
                    final double poisonMastery = ((double) status.getSkill().getEffect(poisonLevel).getMastery() * 5.0d + 10.0d) / 100.0d;
                    final int matk = from.getTotalMagic();
                    final int _int = from.getTotalInt();
                    final ISkill eleAmp = SkillFactory.getSkill(2110001);
                    final int eleAmpLevel = from.getSkillLevel(eleAmp);
                    final double eleAmpMulti;
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
                    final int poisonLevel;
                    final int matk;
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
                    final Random r = new Random();
                    final int luk = from.getTotalLuk();
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
        for (final MonsterStatus stat : status.getStati().keySet()) {
            stati.put(stat, status);
        }
        activeEffects.add(status);
        final int animationTime = status.getSkill().getAnimationTime();
        final MaplePacket packet =
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
        final ScheduledFuture<?> schedule = tMan.schedule(cancelTask, duration + animationTime);
        status.setCancelTask(schedule);
        return true;
    }

    public boolean applyFlame(final MapleCharacter from, final ISkill skill, final long duration, final boolean charge) {
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

        final TimerManager tMan = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            setIsAflame(false);
            cancelFlameSchedule();
        };

        int minFlameDamage;
        int maxFlameDamage;
        final int tickTime;
        final int flameLevel = from.getSkillLevel(skill);
        switch (skill.getId()) {
            case 5211004: { // Flamethrower
                tickTime = 1000;
                final double flameBasicAtk = ((double) skill.getEffect(flameLevel).getDamage() + (charge ? 40.0d : 0.0d)) / 100.0d;
                final double afterBurnMultiplier = flameLevel != 30 ? 0.4d + (double) flameLevel * 0.02d : 1.0d;
                double eleBoostMultiplier = 1.0d;
                if (from.getSkillLevel(5220001) > 0) {
                    final MapleStatEffect eleBoost = SkillFactory.getSkill(5220001).getEffect(from.getSkillLevel(5220001));
                    eleBoostMultiplier += (eleBoost.getDamage() + eleBoost.getX()) / 100.0d;
                }

                minFlameDamage = (int) ((double) from.calculateMinBaseDamage() * flameBasicAtk * afterBurnMultiplier * eleBoostMultiplier);
                maxFlameDamage = (int) ((double) from.calculateMaxBaseDamage() * flameBasicAtk * afterBurnMultiplier * eleBoostMultiplier);
                break;
            }
            case 2121003: { // Fire Demon
                tickTime = 600;
                final double flameMastery = (10.0d + 5.0d * skill.getEffect(from.getSkillLevel(skill)).getMastery()) / 100.0d;
                maxFlameDamage = (int) (((from.getTotalMagic() * from.getTotalMagic() / 1000.0d + from.getTotalMagic()) / 30.0d + from.getTotalInt() / 200.0d) * skill.getEffect(flameLevel).getMatk());
                minFlameDamage = (int) (((from.getTotalMagic() * from.getTotalMagic() / 1000.0d + from.getTotalMagic() * flameMastery * 0.9d) / 30.0d + from.getTotalInt() / 200.0d) * skill.getEffect(flameLevel).getMatk());
                break;
            }
            case 5111006: { // Shockwave
                tickTime = 1000;
                final ISkill fistMastery = SkillFactory.getSkill(5100001);
                final int fistMasteryLevel = from.getSkillLevel(fistMastery);
                final double flameMastery = fistMasteryLevel > 0 ? ((double) fistMastery.getEffect(fistMasteryLevel).getMastery() * 5.0d + 10.0d) / 100.0d : 0.1d;
                maxFlameDamage = (int) (((from.getTotalMagic() * from.getTotalMagic() / 1000.0d + from.getTotalMagic()) / 30.0d + from.getTotalInt() / 200.0d) * (skill.getEffect(flameLevel).getDamage() / 3.0d));
                minFlameDamage = (int) (((from.getTotalMagic() * from.getTotalMagic() / 1000.0d + from.getTotalMagic() * flameMastery * 0.9d) / 30.0d + from.getTotalInt() / 200.0d) * (skill.getEffect(flameLevel).getDamage() / 3.0d));
                break;
            }
            case 3121006: { // Phoenix
                tickTime = 1000;
                final double skillMulti = (5.0d * (double) flameLevel) / 100.0d;
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
        final ScheduledFuture<?> schedule = tMan.schedule(cancelTask, duration);
        setCancelFlameTask(schedule);
        return true;
    }

    public boolean isAflame() {
        return isAflame;
    }

    public void setIsAflame(final boolean ia) {
        isAflame = ia;
    }

    public void setFlameSchedule(final ScheduledFuture<?> fs) {
        flameSchedule = fs;
    }

    public void cancelFlameSchedule() {
        if (flameSchedule != null) flameSchedule.cancel(false);
    }

    public void setCancelFlameTask(final ScheduledFuture<?> cft) {
        cancelFlameTask = cft;
    }

    public ScheduledFuture<?> getCancelFlameTask() {
        return cancelFlameTask;
    }

    public void cancelCancelFlameTask() {
        if (cancelFlameTask != null) cancelFlameTask.cancel(false);
    }

    public boolean applyBleed(final MapleCharacter from, final ISkill skill, final long duration) {
        if (isBuffed(MonsterStatus.WEAPON_IMMUNITY)) return false;

        final TimerManager tMan = TimerManager.getInstance();

        final int bleedId = runningBleedId.getAndIncrement();
        final Runnable cancelTask = () -> cancelBleedSchedule(bleedId);

        final int minBleedDamage;
        final int maxBleedDamage;
        final int tickTime;
        final int bleedLevel = from.getSkillLevel(skill);
        switch (skill.getId()) {
            case 4211002: { // Assaulter
                tickTime = 500;
                final int str = from.getTotalStr();
                final int d = skill.getEffect(bleedLevel).getDamage();
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

    public void cancelBleedSchedule(final int bleedScheduleId) {
        synchronized (bleeds) {
            int i = 0;
            for (final BleedSchedule bleed : bleeds) {
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

    public void setTempEffectiveness(final Element e, final ElementalEffectiveness ee, final int duration) {
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

        final TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = () -> setEffectiveness(e, originalEffectiveness.get(e));

        final ScheduledFuture<?> schedule = timerManager.schedule(cancelTask, duration);
        setCancelEffectivenessTask(schedule);
    }

    public void cancelEffectivenessSchedule() {
        if (cancelEffectivenessTask != null) {
            cancelEffectivenessTask.cancel(false); // true
        }
    }

    public void setCancelEffectivenessTask(final ScheduledFuture<?> cet) {
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
                                 final int skillId,
                                 final long duration,
                                 final MobSkill skill) {
        final TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            if (isAlive()) {
                final MaplePacket packet = MaplePacketCreator.cancelMonsterStatus(
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
        final MaplePacket packet = MaplePacketCreator.applyMonsterStatus(
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

    public void addMonsterBuff(final MonsterStatus status, final int x) {
        monsterBuffs.put(status, x);
    }

    public void removeMonsterBuff(final MonsterStatus status) {
        monsterBuffs.remove(status);
    }

    public boolean isBuffed() {
        return !monsterBuffs.isEmpty();
    }

    public boolean isBuffed(final MonsterStatus status) {
        return monsterBuffs.containsKey(status);
    }

    public int getBuffedValue(final MonsterStatus status) {
        final Integer val = monsterBuffs.get(status);
        return val == null ? 0 : val;
    }

    public void setEffectiveness(final Element e, final ElementalEffectiveness ee) {
        stats.setEffectiveness(e, ee);
        addedEffectiveness.put(e, ee);
    }

    public void setFake(final boolean fake) {
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

    public boolean hasSkill(final int skillId, final int level) {
        return stats.hasSkill(skillId, level);
    }

    public boolean canUseSkill(final MobSkill toUse) {
        if (toUse == null) return false;
        synchronized (usedSkills) {
            for (final MobSkill skill : usedSkills) {
                if (skill.equals(toUse)) return false;
            }
        }
        if (toUse.getLimit() > 0 && skillsUsed.containsKey(toUse)) {
            final int times = skillsUsed.get(toUse);
            if (times >= toUse.getLimit()) return false;
        }
        return !(toUse.getSkillId() == 200 && map.mobCount() > 100);
    }

    public void usedSkill(final int skillId, final int level, final long cooltime) {
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

    public void clearSkill(final int skillId, final int level) {
        clearSkill(MobSkillFactory.getMobSkill(skillId, level));
    }

    public void clearSkill(final MobSkill skill) {
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

    public boolean panic(final MapleCharacter from, final ISkill skill, final long duration) {
        cancelCancelPanicTask();
        cancelPanicSchedule();

        final TimerManager tMan = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            setIsPanicked(false);
            cancelPanicSchedule();
        };

        final int tickTime;
        final int minPanicDamage;
        final int maxPanicDamage;
        final double proc;
        final int panicLevel = from.getSkillLevel(skill);
        switch (skill.getId()) {
            case 1111003:   // Panic: Sword
            case 1111004: { // Panic: Axe
                tickTime = 125;
                proc = 0.0625d;
                final int pad = getPADamage();
                minPanicDamage = pad * pad / 6;
                maxPanicDamage = pad * pad / 4;
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
        final ScheduledFuture<?> schedule = tMan.schedule(cancelTask, duration);
        setCancelPanicTask(schedule);
        return true;
    }

    public boolean isPanicked() {
        return isPanicked;
    }

    public void setIsPanicked(final boolean ip) {
        isPanicked = ip;
    }

    public void setPanicSchedule(final ScheduledFuture<?> ps) {
        panicSchedule = ps;
    }

    public void cancelPanicSchedule() {
        if (panicSchedule != null) panicSchedule.cancel(false);
    }

    public void setCancelPanicTask(final ScheduledFuture<?> cpt) {
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
    public int softSetComa(final int c) {
        return coma.accumulateAndGet(c, Math::max);
    }

    public void setComa(final int c) {
        coma.set(c);
    }

    public int getComa() {
        return coma.get();
    }

    public boolean hasComa() {
        return coma.get() > 0;
    }

    private final class PoisonTask implements Runnable {
        private final int minPoisonDamage, maxPoisonDamage;
        private final MapleCharacter chr;
        private final MonsterStatusEffect status;
        private final Runnable cancelTask;
        private final boolean shadowWeb;
        private final MapleMap map;

        private PoisonTask(final int minPoisonDamage,
                           final int maxPoisonDamage,
                           final MapleCharacter chr,
                           final MonsterStatusEffect status,
                           final Runnable cancelTask,
                           final boolean shadowWeb) {
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
                    } catch (final IndexOutOfBoundsException ignored) {
                    }
                    status.getCancelTask().cancel(false);
                } catch (final Exception e) {
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
                        } catch (final IndexOutOfBoundsException ignored) {
                        }
                        status.getCancelTask().cancel(false);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (hp > 1 && damage > 0) {
                if (shadowWeb || minPoisonDamage != maxPoisonDamage) {
                    map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), damage), getPosition());
                }
                damage(chr, damage, false);
            }
        }
    }

    private final class FlameTask implements Runnable {
        private final int minFlameDamage;
        private final int maxFlameDamage;
        private final MapleCharacter chr;
        private final Runnable cancelTask;
        private final MapleMap map;

        private FlameTask(final int minFlameDamage, final int maxFlameDamage, final MapleCharacter chr, final Runnable cancelTask) {
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
                map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), damage), getPosition());
                map.damageMonster(chr, MapleMonster.this, damage);
            }
            if (docancel) {
                try {
                    cancelCancelFlameTask();
                    cancelFlameSchedule();
                } catch (final Exception e) {
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

        private BleedTask(final int minBleedDamage, final int maxBleedDamage, final MapleCharacter chr, final int bleedScheduleId) {
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
                map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), damage), getPosition());
                map.damageMonster(chr, MapleMonster.this, damage);
            }
            if (docancel) cancelBleedSchedule(bleedScheduleId);
        }
    }

    private final class BleedSchedule {
        private final ScheduledFuture<?> bleedTask;
        private final ScheduledFuture<?> cancelTask;
        private final int id;

        public BleedSchedule(final ScheduledFuture<?> bleedTask, final ScheduledFuture<?> cancelTask, final int id) {
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
        public boolean equals(final Object o) {
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

        private PanicTask(final int minPanicDamage, final int maxPanicDamage, final double proc, final MapleCharacter chr, final Runnable cancelTask) {
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
            final int selfDamage =
                isBuffed(MonsterStatus.WEAPON_IMMUNITY) ?
                    1 :
                    Math.max(
                        minPanicDamage + rand.nextInt(maxPanicDamage - minPanicDamage + 1) - (int) (8.0d * getWdef() * Math.log(getWdef())),
                        1
                    );
            if (selfDamage >= hp) doCancel = true;
            if (selfDamage > 0) {
                map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), selfDamage), getPosition());
                map.damageMonster(chr, MapleMonster.this, selfDamage);
            }
            // Then damage up to 6 nearby mobs:
            map.getMapObjectsInRange(
                getPosition(),
                40000.0d,
                MapleMapObjectType.MONSTER
            )
            .stream()
            .map(mmo -> (MapleMonster) mmo)
            .filter(m -> m != null && !MapleMonster.this.equals(m))
            .limit(6L)
            .forEach(m -> {
                final int damage =
                    m.isBuffed(MonsterStatus.WEAPON_IMMUNITY) ?
                        1 :
                        Math.max(
                            minPanicDamage + rand.nextInt(maxPanicDamage - minPanicDamage + 1) - (int) (8.0d * m.getWdef() * Math.log(m.getWdef())),
                            1
                        );
                if (damage > 0) {
                    map.broadcastMessage(MaplePacketCreator.damageMonster(m.getObjectId(), damage), m.getPosition());
                    map.damageMonster(chr, m, damage);
                }
            });
            // Cancel if the mob is dead:
            if (doCancel) {
                try {
                    cancelCancelPanicTask();
                    cancelPanicSchedule();
                } catch (final Exception e) {
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

        public AttackingMapleCharacter(final MapleCharacter attacker, final long lastAttackTime) {
            super();
            this.attacker = attacker;
            this.lastAttackTime = lastAttackTime;
        }

        public long getLastAttackTime() {
            return lastAttackTime;
        }

        public void setLastAttackTime(final long lastAttackTime) {
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

        public SingleAttackerEntry(final MapleCharacter from, final ChannelServer cserv) {
            chrid = from.getId();
            this.cserv = cserv;
        }

        @Override
        public void addDamage(final MapleCharacter from, final int damage, final boolean updateAttackTime) {
            if (chrid == from.getId()) {
                this.damage += damage;
            } else {
                throw new IllegalArgumentException("Not the attacker of this entry");
            }
            if (updateAttackTime) lastAttackTime = System.currentTimeMillis();
        }

        @Override
        public List<AttackingMapleCharacter> getAttackers() {
            final MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(chrid);
            if (chr != null) {
                return Collections.singletonList(new AttackingMapleCharacter(chr, lastAttackTime));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean contains(final MapleCharacter chr) {
            return chrid == chr.getId();
        }

        @Override
        public int getDamage() {
            return damage;
        }

        @Override
        public void killedMob(final MapleMap map, final int baseExp, final boolean mostDamage, final boolean isboss) {
            final MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(chrid);
            if (chr != null && chr.getMap() == map) {
                giveExpToCharacter(chr, baseExp, mostDamage, 1);
            }
        }

        @Override
        public int hashCode() {
            return chrid;
        }

        @Override
        public boolean equals(final Object obj) {
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

        public OnePartyAttacker(final MapleParty lastKnownParty, final int damage) {
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

        public PartyAttackerEntry(final int partyid, final ChannelServer cserv) {
            this.partyid = partyid;
            this.cserv = cserv;
            attackers = new LinkedHashMap<>(8, 0.8125f);
        }

        @Override
        public List<AttackingMapleCharacter> getAttackers() {
            final List<AttackingMapleCharacter> ret = new ArrayList<>(attackers.size());
            for (final Map.Entry<Integer, OnePartyAttacker> entry : attackers.entrySet()) {
                final MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(entry.getKey());
                if (chr != null) {
                    ret.add(new AttackingMapleCharacter(chr, entry.getValue().lastAttackTime));
                }
            }
            return ret;
        }

        private Map<MapleCharacter, OnePartyAttacker> resolveAttackers() {
            final Map<MapleCharacter, OnePartyAttacker> ret = new LinkedHashMap<>(attackers.size());
            for (final Map.Entry<Integer, OnePartyAttacker> aentry : attackers.entrySet()) {
                final MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(aentry.getKey());
                if (chr != null) ret.put(chr, aentry.getValue());
            }
            return ret;
        }

        @Override
        public boolean contains(final MapleCharacter chr) {
            return attackers.containsKey(chr.getId());
        }

        @Override
        public int getDamage() {
            return totDamage;
        }

        @Override
        public void addDamage(final MapleCharacter from, final int damage, final boolean updateAttackTime) {
            final OnePartyAttacker oldPartyAttacker = attackers.get(from.getId());
            if (oldPartyAttacker != null) {
                oldPartyAttacker.damage += damage;
                oldPartyAttacker.lastKnownParty = from.getParty();
                if (updateAttackTime) {
                    oldPartyAttacker.lastAttackTime = System.currentTimeMillis();
                }
            } else {
                final OnePartyAttacker onePartyAttacker = new OnePartyAttacker(from.getParty(), damage);
                attackers.put(from.getId(), onePartyAttacker);
                if (!updateAttackTime) {
                    onePartyAttacker.lastAttackTime = 0L;
                }
            }
            totDamage += damage;
        }

        @Override
        public void killedMob(final MapleMap map, final int baseExp, final boolean mostDamage, final boolean isBoss) {
            final Map<MapleCharacter, OnePartyAttacker> attackers_ = resolveAttackers();
            MapleCharacter highest = null;
            int highestDamage = 0;
            final Map<MapleCharacter, Integer> expMap = new ArrayMap<>(6);
            for (final Map.Entry<MapleCharacter, OnePartyAttacker> attacker : attackers_.entrySet()) {
                final MapleParty party = attacker.getValue().lastKnownParty;
                double averagePartyLevel = 0.0d;
                final List<MapleCharacter> expApplicable = new ArrayList<>(6);
                for (final MaplePartyCharacter partyChar : party.getMembers()) {
                    //if (
                    //    attacker.getKey().getLevel() - partyChar.getLevel() <= 15 ||
                    //    getLevel() - partyChar.getLevel() <= 15
                    //) {
                    final MapleCharacter pchr = cserv.getPlayerStorage().getCharacterByName(partyChar.getName());
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

                final int iDamage = attacker.getValue().damage;
                if (iDamage > highestDamage) {
                    highest = attacker.getKey();
                    highestDamage = iDamage;
                }

                final double innerBaseExp = (double) baseExp * (double) iDamage / (double) totDamage;
                final double expFraction = innerBaseExp * expBonus / ((double) expApplicable.size() + 1.0d);
                for (final MapleCharacter expReceiver : expApplicable) {
                    final Integer oexp = expMap.get(expReceiver);
                    int iexp = oexp == null ? 0 : oexp;
                    final double expWeight = expReceiver == attacker.getKey() ? 2.0d : 1.0d;
                    double levelMod = (double) expReceiver.getLevel() / averagePartyLevel;
                    if (levelMod > 1.0d || attackers.containsKey(expReceiver.getId())) {
                        levelMod = 1.0d;
                    }
                    iexp += (int) Math.round(expFraction * expWeight * levelMod);
                    expMap.put(expReceiver, iexp);
                }
            }

            for (final Map.Entry<MapleCharacter, Integer> expReceiver : expMap.entrySet()) {
                final boolean white = mostDamage && expReceiver.getKey() == highest;
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
        public boolean equals(final Object o) {
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
