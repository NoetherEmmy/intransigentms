package net.sf.odinms.net.world;

import net.sf.odinms.client.MapleCharacter;

import java.awt.*;
import java.io.Serializable;

public class MaplePartyCharacter implements Serializable {
    private static final long serialVersionUID = 6215463252132450750L;
    private final String name;
    private int id;
    private int level;
    private int channel;
    private int jobid;
    private int mapid;
    private int gender;
    private boolean married;
    private int doorTown = 999999999;
    private int doorTarget = 999999999;
    private Point doorPosition = new Point(0, 0);
    private boolean online;
    private boolean scpqFlag = false;
    private boolean isGM = false;

    public MaplePartyCharacter(MapleCharacter maplechar) {
        this.name = maplechar.getName();
        this.level = maplechar.getLevel();
        this.channel = maplechar.getClient().getChannel();
        this.id = maplechar.getId();
        this.jobid = maplechar.getJob().getId();
        this.mapid = maplechar.getMapId();
        this.online = true;
        if (!maplechar.getDoors().isEmpty()) {
            this.doorTown = maplechar.getDoors().get(0).getTown().getId();
            this.doorTarget = maplechar.getDoors().get(0).getTarget().getId();
            this.doorPosition = maplechar.getDoors().get(0).getTargetPosition();
        }
        this.gender = maplechar.getGender();
        this.married = maplechar.isMarried();
        this.scpqFlag = maplechar.isScpqFlagged();
        this.isGM = maplechar.isGM();
    }

    public MaplePartyCharacter() {
        this.name = "";
        this.id = 0;
        this.level = 1;
        this.channel = 1;
        this.jobid = 0;
        this.mapid = 0;
        this.gender = 0;
        this.married = false;
        this.doorTown = 999999999;
        this.doorTarget = 999999999;
        this.doorPosition = new Point(0, 0);
        this.online = false;
        this.scpqFlag = false;
        this.isGM = false;
    }

    public int getLevel() {
        return level;
    }

    public int getChannel() {
        return channel;
    }

    public boolean isScpqFlagged() {
        return scpqFlag;
    }

    public boolean isGM() {
        return isGM;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
	
    public boolean isMarried() {
        return married;
    }

    public int getGender() {
        return gender;
    }

    public int getMapid() {
        return mapid;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getJobId() {
        return jobid;
    }

    public int getDoorTown() {
        return doorTown;
    }

    public int getDoorTarget() {
        return doorTarget;
    }

    public Point getDoorPosition() {
        return doorPosition;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MaplePartyCharacter other = (MaplePartyCharacter) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}
