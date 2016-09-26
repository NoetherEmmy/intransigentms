package net.sf.odinms.server.maps;

import java.awt.Point;
import java.awt.Rectangle;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.tools.MaplePacketCreator;

public class MapleMist extends AbstractMapleMapObject {
    private final Rectangle mistPosition;
    private final MapleCharacter owner;
    private final MapleStatEffect source;

    public MapleMist(Rectangle mistPosition, MapleCharacter owner, MapleStatEffect source) {
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
    public void setPosition(Point position) {
        throw new UnsupportedOperationException();
    }

    public MaplePacket makeDestroyData() {
        return MaplePacketCreator.removeMist(getObjectId());
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(makeDestroyData());
    }

    public MaplePacket makeSpawnData() {
        int level = owner.getSkillLevel(SkillFactory.getSkill(source.getSourceId()));
        return MaplePacketCreator.spawnMist(getObjectId(), owner.getId(), source.getSourceId(), mistPosition, level);
    }

    public MaplePacket makeFakeSpawnData(int level) {
        return MaplePacketCreator.spawnMist(getObjectId(), owner.getId(), source.getSourceId(), mistPosition, level);
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().write(makeSpawnData());
    }

    public boolean makeChanceResult() {
        return source.makeChanceResult();
    }
}