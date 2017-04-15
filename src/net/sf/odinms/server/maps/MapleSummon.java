package net.sf.odinms.server.maps;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.tools.MaplePacketCreator;

import java.awt.*;

public class MapleSummon extends AbstractAnimatedMapleMapObject {
    private final MapleCharacter owner;
    private final int skillLevel, skill;
    private int hp;
    private final SummonMovementType movementType;

    public MapleSummon(final MapleCharacter owner, final int skill, final Point pos, final SummonMovementType movementType) {
        super();
        this.owner = owner;
        this.skill = skill;
        this.skillLevel = owner.getSkillLevel(SkillFactory.getSkill(skill));
        if (skillLevel == 0) {
            throw new RuntimeException("Trying to create a summon for a char without the skill");
        }
        this.movementType = movementType;
        setPosition(pos);
    }

    @Override
    public void sendSpawnData(final MapleClient client) {
        client.getSession().write(MaplePacketCreator.spawnSpecialMapObject(this, skillLevel, false));
    }

    @Override
    public void sendDestroyData(final MapleClient client) {
        client.getSession().write(MaplePacketCreator.removeSpecialMapObject(this, false));
    }

    public MapleCharacter getOwner() {
        return this.owner;
    }

    public int getSkill() {
        return this.skill;
    }

    public int getHP() {
        return this.hp;
    }

    public void addHP(final int delta) {
        this.hp += delta;
    }

    public SummonMovementType getMovementType() {
        return movementType;
    }

    public boolean isPuppet() {
        return (skill == 3111002 || skill == 3211002 || skill == 5211001);
    }

    public boolean isSummon() {
        return (skill == 2311006 || skill == 2321003 || skill == 2121005 || skill == 2221005 || skill == 5211002);
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    @Override
    public MapleMapObjectType getType() {
            return MapleMapObjectType.SUMMON;
    }
}
