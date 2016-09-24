package net.sf.odinms.server.maps;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.tools.MaplePacketCreator;

public class MapleDoor extends AbstractMapleMapObject {

    private MapleCharacter owner;
    private MapleMap town;
    private MaplePortal townPortal;
    private MapleMap target;
    private Point targetPosition;

    public MapleDoor(MapleCharacter owner, Point targetPosition) {
        super();
        this.owner = owner;
        this.target = owner.getMap();
        this.targetPosition = targetPosition;
        setPosition(this.targetPosition);
        this.town = this.target.getReturnMap();
        this.townPortal = getFreePortal();
    }

    public MapleDoor(MapleDoor origDoor) {
        super();
        this.owner = origDoor.owner;
        this.town = origDoor.town;
        this.townPortal = origDoor.townPortal;
        this.target = origDoor.target;
        this.targetPosition = origDoor.targetPosition;
        this.townPortal = origDoor.townPortal;
        setPosition(this.townPortal.getPosition());
    }

    private MaplePortal getFreePortal() {
        List<MaplePortal> freePortals = new ArrayList<>();

        for (MaplePortal port : town.getPortals()) {
            if (port.getType() == 6) {
                freePortals.add(port);
            }
        }
        Collections.sort(freePortals, new Comparator<MaplePortal>() {

        @Override
        public int compare(MaplePortal o1, MaplePortal o2) {
            if (o1.getId() < o2.getId())
                return -1;
            else if (o1.getId() == o2.getId())
                return 0;
            else
                return 1;
            }

        });
        for (MapleMapObject obj : town.getMapObjects()) {
            if (obj instanceof MapleDoor) {
                MapleDoor door = (MapleDoor) obj;
                if (door.getOwner().getParty() != null &&
                    owner.getParty().containsMembers(new MaplePartyCharacter(door.getOwner()))) {
                    freePortals.remove(door.getTownPortal());
                }
            }
        }
        return freePortals.iterator().next();
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (target.getId() == client.getPlayer().getMapId() ||
            owner == client.getPlayer() && owner.getParty() == null) {
            client.getSession().write(MaplePacketCreator.spawnDoor(owner.getId(),
            town.getId() == client.getPlayer().getMapId() ? townPortal.getPosition() : targetPosition, true));
            if (owner.getParty() != null && (owner == client.getPlayer() || owner.getParty().containsMembers(new MaplePartyCharacter(client.getPlayer())))) {
                client.getSession().write(MaplePacketCreator.partyPortal(town.getId(), target.getId(), targetPosition));
            }
            client.getSession().write(MaplePacketCreator.spawnPortal(town.getId(), target.getId(), targetPosition));
        }
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        if (target.getId() == client.getPlayer().getMapId() || owner == client.getPlayer() ||
            owner.getParty() != null && owner.getParty().containsMembers(new MaplePartyCharacter(client.getPlayer()))) {
            if (owner.getParty() != null && (owner == client.getPlayer() || owner.getParty().containsMembers(new MaplePartyCharacter(client.getPlayer())))) {
                client.getSession().write(MaplePacketCreator.partyPortal(999999999, 999999999, new Point(-1, -1)));
            }
            client.getSession().write(MaplePacketCreator.removeDoor(owner.getId(), false));
            client.getSession().write(MaplePacketCreator.removeDoor(owner.getId(), true));
        }
    }

    public void warp(MapleCharacter chr, boolean toTown) {
        if (chr == owner || owner.getParty() != null && owner.getParty().containsMembers(new MaplePartyCharacter(chr))) {
            if (!toTown)
                chr.changeMap(target, targetPosition);
            else
                chr.changeMap(town, townPortal);
        } else {
            chr.getClient().getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public MapleMap getTown() {
        return town;
    }

    public MaplePortal getTownPortal() {
        return townPortal;
    }

    public MapleMap getTarget() {
        return target;
    }

    public Point getTargetPosition() {
        return targetPosition;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.DOOR;
    }
}