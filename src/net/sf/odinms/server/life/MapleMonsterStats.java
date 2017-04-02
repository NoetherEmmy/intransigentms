package net.sf.odinms.server.life;

import net.sf.odinms.tools.Pair;

import java.util.*;

public class MapleMonsterStats {
    private int exp;
    private int hp, mp;
    private int level;
    private int removeAfter;
    private boolean boss;
    private int PADamage;
    private boolean undead;
    private boolean ffaLoot;
    private String name;
    private final Map<String, Integer> animationTimes = new HashMap<>();
    private final Map<Element, ElementalEffectiveness> resistance = new HashMap<>();
    private final List<Integer> revives = new ArrayList<>(1);
    private byte tagColor;
    private byte tagBgColor;
    private final List<MobSkill> skills = new ArrayList<>();
    private boolean firstAttack;
    private int buffToGive;
    private boolean explosive;
    private int accuracy;
    private int avoid;
    private int wdef, mdef;

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = mp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getAccuracy() {
        return this.accuracy;
    }

    public void setAccuracy(int acc) {
        this.accuracy = acc;
    }

    public int getAvoid() {
        return this.avoid;
    }

    public void setAvoid(int avoid) {
        this.avoid = avoid;
    }

    public int getRemoveAfter() {
        return removeAfter;
    }

    public void setRemoveAfter(int removeAfter) {
        this.removeAfter = removeAfter;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setFfaLoot(boolean ffaLoot) {
        this.ffaLoot = ffaLoot;
    }

    public boolean isFfaLoot() {
        return ffaLoot;
    }

    public void setAnimationTime(String name, int delay) {
        animationTimes.put(name, delay);
    }

    public int getAnimationTime(String name) {
        Integer ret = animationTimes.get(name);
        return ret != null ? ret : 500;
    }

    public boolean isMobile() {
        return animationTimes.containsKey("move") || animationTimes.containsKey("fly");
    }

    public List<Integer> getRevives() {
        return revives;
    }

    public void setRevives(List<Integer> revives) {
        this.revives.clear();
        this.revives.addAll(revives);
        ((ArrayList) this.revives).trimToSize();
    }

    public void setUndead(boolean undead) {
        this.undead = undead;
    }

    public boolean getUndead() {
        return undead;
    }

    public void setEffectiveness(Element e, ElementalEffectiveness ee) {
        resistance.put(e, ee);
    }

    public ElementalEffectiveness getEffectiveness(Element e) {
        ElementalEffectiveness elementalEffectiveness = resistance.get(e);
        return elementalEffectiveness == null ? ElementalEffectiveness.NORMAL : elementalEffectiveness;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getTagColor() {
        return tagColor;
    }

    public void setTagColor(int tagColor) {
        this.tagColor = (byte) tagColor;
    }

    public byte getTagBgColor() {
        return tagBgColor;
    }

    public void setTagBgColor(int tagBgColor) {
        this.tagBgColor = (byte) tagBgColor;
    }

    public void setSkills(List<Pair<Integer, Integer>> skills, final int mobId) {
        this.skills.clear();
        skills.forEach(skill -> {
            MobSkill s = MobSkillFactory.getMobSkill(skill.getLeft(), skill.getRight());
            if (s == null) {
                System.err.println(
                    "Failed to get mob skill with ID " +
                        skill.getLeft() +
                        " and level " +
                        skill.getRight() +
                        ". Mob ID: " +
                        mobId
                );
                return;
            }
            this.skills.add(s);
        });
        ((ArrayList) this.skills).trimToSize();
    }

    public List<MobSkill> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public int getNoSkills() {
        return skills.size();
    }

    public boolean hasSkill(int skillId, int level) {
        return skills.contains(MobSkillFactory.getMobSkill(skillId, level));
    }

    public void setFirstAttack(boolean firstAttack) {
        this.firstAttack = firstAttack;
    }

    public boolean isFirstAttack() {
        return firstAttack;
    }

    public void setBuffToGive(int buff) {
        buffToGive = buff;
    }

    public int getBuffToGive() {
        return buffToGive;
    }

    public void setPADamage(int dmg) {
        PADamage = dmg;
    }

    public int getPADamage() {
        return PADamage;
    }

    public void setWdef(int wdef) {
        this.wdef = wdef;
    }

    public int getWdef() {
        return wdef;
    }

    public void setMdef(int mdef) {
        this.mdef = mdef;
    }

    public int getMdef() {
        return mdef;
    }

    public void setExplosive(boolean explosive) {
        this.explosive = explosive;
    }

    public boolean isExplosive() {
        return explosive;
    }
}
