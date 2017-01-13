package net.sf.odinms.client;

public enum MapleWeaponType {
    NOT_A_WEAPON(0.0d),
    BOW(3.4d),
    CLAW(3.6d),
    DAGGER(4.0d),
    CROSSBOW(3.6d),
    AXE1H(4.4d),
    SWORD1H(4.0d),
    BLUNT1H(4.4d),
    AXE2H(4.8d),
    SWORD2H(4.6d),
    BLUNT2H(4.8d),
    POLE_ARM(5.0d),
    SPEAR(5.0d),
    STAFF(3.6d),
    WAND(3.6d),
    KNUCKLE(4.0d),
    GUN(5.0d);
    private final double damageMultiplier;

    MapleWeaponType(double maxDamageMultiplier) {
        this.damageMultiplier = maxDamageMultiplier;
    }

    public double getMaxDamageMultiplier() {
        return damageMultiplier;
    }
}
