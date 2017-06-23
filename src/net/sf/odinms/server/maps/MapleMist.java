package net.sf.odinms.server.maps;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.tools.MaplePacketCreator;

import java.awt.*;

public class MapleMist extends AbstractMapleMapObject {
    private final Rectangle mistPosition;
    private final MapleCharacter owner;
    private final MapleStatEffect source;

    public MapleMist(final Rectangle mistPosition, final MapleCharacter owner, final MapleStatEffect source) {
        this.mistPosition = mistPosition;
        this.owner = owner;
        this.source = source;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MIST;
    }

    @Override
    public Point getPosition() {
        return mistPosition.getLocation();
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public ISkill getSourceSkill() {
        return SkillFactory.getSkill(source.getSourceId());
    }

    public Rectangle getBox() {
        return mistPosition;
    }

    @Override
    public void setPosition(final Point position) {
        throw new UnsupportedOperationException("MapleMist cannot be moved. ");
    }

    public MaplePacket makeDestroyData() {
        return MaplePacketCreator.removeMist(getObjectId());
    }

    @Override
    public void sendDestroyData(final MapleClient client) {
        client.getSession().write(makeDestroyData());
    }

    public MaplePacket makeSpawnData() {
        final int level = owner.getSkillLevel(SkillFactory.getSkill(source.getSourceId()));
        return MaplePacketCreator.spawnMist(getObjectId(), owner.getId(), source.getSourceId(), mistPosition, level);
    }

    public MaplePacket makeFakeSpawnData(final int level) {
        return MaplePacketCreator.spawnMist(getObjectId(), owner.getId(), source.getSourceId(), mistPosition, level);
    }

    @Override
    public void sendSpawnData(final MapleClient client) {
        client.getSession().write(makeSpawnData());
    }

    public boolean makeChanceResult() {
        return source.makeChanceResult();
    }
}
