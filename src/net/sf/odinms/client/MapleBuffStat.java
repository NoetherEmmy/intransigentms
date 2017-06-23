package net.sf.odinms.client;

import net.sf.odinms.net.LongValueHolder;

import java.io.Serializable;

public enum MapleBuffStat implements LongValueHolder, Serializable {
    MORPH(0x2L),
    RECOVERY(0x4L),
    MAPLE_WARRIOR(0x8L),
    STANCE(0x10L),
    SHARP_EYES(0x20L),
    MANA_REFLECTION(0x40L),
    SHADOW_CLAW(0x100L),
    INFINITY(0x200L),
    HOLY_SHIELD(0x400L),
    HAMSTRING(0x800L),
    BLIND(0x1000L),
    CONCENTRATE(0x2000L),
    ECHO_OF_HERO(0x8000L),
    GHOST_MORPH(0x20000L),
    DASH(0x30000000L),
    MONSTER_RIDING(0x40000000L),
    WATK(0x100000000L),
    WDEF(0x200000000L),
    MATK(0x400000000L),
    MDEF(0x800000000L),
    ACC(0x1000000000L),
    AVOID(0x2000000000L),
    HANDS(0x4000000000L),
    SPEED(0x8000000000L),
    JUMP(0x10000000000L),
    MAGIC_GUARD(0x20000000000L),
    DARKSIGHT(0x40000000000L),
    BOOSTER(0x80000000000L),
    SPEED_INFUSION(0x80000000000L),
    POWERGUARD(0x100000000000L),
    HYPERBODYHP(0x200000000000L),
    HYPERBODYMP(0x400000000000L),
    INVINCIBLE(0x800000000000L),
    SOULARROW(0x1000000000000L),
    STUN(0x2000000000000L),
    POISON(0x4000000000000L),
    SEAL(0x8000000000000L),
    DARKNESS(0x10000000000000L),
    COMBO(0x20000000000000L),
    SUMMON(0x20000000000000L),
    WK_CHARGE(0x40000000000000L),
    DRAGONBLOOD(0x80000000000000L),
    HOLY_SYMBOL(0x100000000000000L),
    MESOUP(0x200000000000000L),
    SHADOWPARTNER(0x400000000000000L),
    //0x8000000000000
    PICKPOCKET(0x800000000000000L),
    PUPPET(0x800000000000000L),
    MESOGUARD(0x1000000000000000L),
    WEAKEN(0x4000000000000000L),
    //SWITCH_CONTROLS(0x8000000000000L),
    ;
    static final long serialVersionUID = 0L;
    private final long i;

    MapleBuffStat(final long i) {
        this.i = i;
    }

    @Override
    public long getValue() {
        return i;
    }
}
