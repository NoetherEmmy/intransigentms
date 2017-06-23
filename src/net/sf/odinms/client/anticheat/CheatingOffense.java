package net.sf.odinms.client.anticheat;

public enum CheatingOffense {
    FASTATTACK(1, 60000, 300),
    MOVE_MONSTERS,
    TUBI,
    FAST_HP_REGEN,
    FAST_MP_REGEN(1, 60000, 500),
    SAME_DAMAGE(10, 300000, 20),
    ATTACK_WITHOUT_GETTING_HIT,
    HIGH_DAMAGE(10, 300000L),
    ATTACK_FARAWAY_MONSTER(5),
    REGEN_HIGH_HP(50),
    REGEN_HIGH_MP(50),
    ITEMVAC(5),
    SHORT_ITEMVAC(2),
    USING_FARAWAY_PORTAL(30, 300000),
    FAST_TAKE_DAMAGE(1),
    FAST_MOVE(1, 60000),
    HIGH_JUMP(1, 60000),
    MISMATCHING_BULLETCOUNT(50),
    ETC_EXPLOSION(50, 300000),
    FAST_SUMMON_ATTACK,
    ATTACKING_WHILE_DEAD(10, 300000),
    USING_UNAVAILABLE_ITEM(10, 300000),
    FAMING_SELF(10, 300000),
    FAMING_UNDER_15(10, 300000),
    EXPLODING_NONEXISTANT,
    SUMMON_HACK,
    HEAL_ATTACKING_UNDEAD(1, 60000, 5),
    COOLDOWN_HACK(10, 300000, 10);
    private final int points;
    private final long validityDuration;
    private final int autobancount;
    private boolean enabled = true;

    public int getPoints() {
        return points;
    }

    public long getValidityDuration() {
        return validityDuration;
    }

    public boolean shouldAutoban(final int count) {
        return autobancount != -1 && count > autobancount;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    CheatingOffense() {
        this(1);
    }

    CheatingOffense(final int points) {
        this(points, 60000);
    }

    CheatingOffense(final int points, final long validityDuration) {
        this(points, validityDuration, -1);
    }

    CheatingOffense(final int points, final long validityDuration, final int autobancount) {
        this(points, validityDuration, autobancount, true);
    }

    CheatingOffense(final int points, final long validityDuration, final int autobancount, final boolean enabled) {
        this.points = points;
        this.validityDuration = validityDuration;
        this.autobancount = autobancount;
        this.enabled = enabled;
    }
}
