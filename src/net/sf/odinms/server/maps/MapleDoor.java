package net.sf.odinms.server.maps;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.tools.MaplePacketCreator;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MapleDoor extends AbstractMapleMapObject {
    private final MapleCharacter owner;
    private final MapleMap town;
    private MaplePortal townPortal;
    private final MapleMap target;
    private final Point targetPosition;

    public MapleDoor(MapleCharacter owner, Point targetPosition) {
        super();
        this.owner = owner;
        target = owner.getMap();
        this.targetPosition = targetPosition;
        setPosition(this.targetPosition);
        town = target.getReturnMap();
        if (town == null) {
            System.err.println(
                "No return map for map " +
                    target.getId() +
                    ", new MapleDoor(MapleCharacter, Point)"
            );
        }
        townPortal = getFreePortal();
    }

    public MapleDoor(MapleDoor origDoor) {
        super();
        owner = origDoor.owner;
        town = origDoor.town;
        townPortal = origDoor.townPortal;
        target = origDoor.target;
        targetPosition = origDoor.targetPosition;
        townPortal = origDoor.townPortal;
        setPosition(townPortal.getPosition());
    }

    private MaplePortal getFreePortal() {
        List<MaplePortal> freePortals = new ArrayList<>();

        for (MaplePortal port : town.getPortals()) {
            if (port.getType() == 6) freePortals.add(port);
        }

        freePortals.sort(Comparator.comparingInt(MaplePortal::getId));

        for (MapleMapObject obj : town.getMapObjects()) {
            if (obj instanceof MapleDoor) {
                MapleDoor door = (MapleDoor) obj;
                if (
                    door.getOwner().getParty() != null &&
                    owner.getParty().containsMembers(new MaplePartyCharacter(door.getOwner()))
                ) {
                    freePortals.remove(door.getTownPortal());
                }
            }
        }

        if (freePortals.size() < 1) {
            System.err.println("No free portals on map " + town.getId());
        }

        return freePortals.iterator().next();
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (
            target.getId() == client.getPlayer().getMapId() ||
            owner == client.getPlayer() &&
            owner.getParty() == null
        ) {
            client
                .getSession()
                .write(
                    MaplePacketCreator.spawnDoor(
                        owner.getId(),
                        town.getId() == client.getPlayer().getMapId() ?
                            townPortal.getPosition() :
                            targetPosition,
                        true
                    )
                );
            if (
                owner.getParty() != null &&
                (owner == client.getPlayer() ||
                    owner.getParty().containsMembers(new MaplePartyCharacter(client.getPlayer()))
                )
            ) {
                client
                    .getSession()
                    .write(
                        MaplePacketCreator.partyPortal(
                            town.getId(),
                            target.getId(),
                            targetPosition
                        )
                    );
            }
            client
                .getSession()
                .write(
                    MaplePacketCreator.spawnPortal(
                        town.getId(),
                        target.getId(),
                        targetPosition
                    )
                );
        }
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        if (
            target.getId() == client.getPlayer().getMapId() ||
            owner == client.getPlayer() ||
            owner.getParty() != null &&
            owner.getParty().containsMembers(new MaplePartyCharacter(client.getPlayer()))
        ) {
            if (
                owner.getParty() != null &&
                (owner == client.getPlayer() ||
                    owner.getParty().containsMembers(new MaplePartyCharacter(client.getPlayer())))
            ) {
                client
                    .getSession()
                    .write(
                        MaplePacketCreator.partyPortal(
                            999999999,
                            999999999,
                            new Point(-1, -1)
                        )
                    );
            }
            client.getSession().write(MaplePacketCreator.removeDoor(owner.getId(), false));
            client.getSession().write(MaplePacketCreator.removeDoor(owner.getId(), true));
        }
    }

    public void warp(MapleCharacter chr, boolean toTown) {
        if (
            chr == owner ||
            owner.getParty() != null &&
            owner.getParty().containsMembers(new MaplePartyCharacter(chr))
        ) {
            if (!toTown) {
                chr.changeMap(target, targetPosition);
            } else {
                chr.changeMap(town, townPortal);
            }
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
