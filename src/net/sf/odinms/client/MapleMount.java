package net.sf.odinms.client;

import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;

import java.util.concurrent.ScheduledFuture;

public class MapleMount {
    private int itemId, skillId, tiredness, exp, level;
    private ScheduledFuture<?> tirednessSchedule;
    private final MapleCharacter owner;
    private boolean active = true;

    public MapleMount(MapleCharacter owner, int id, int skillId) {
        this.itemId = id;
        this.skillId = skillId;
        this.level = 1;
        this.owner = owner;
    }

    public int getItemId() {
        return itemId;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getId() {
        switch (itemId) {
            case 1902000:
                return 1;
            case 1902001:
                return 2;
            case 1902002:
                return 3;
            case 1932000:
                return 4;
            case 1902008:
            case 1902009:
                return 5;
            default:
                return 0;
        }
    }

    public int getTiredness() {
        return tiredness;
    }

    public int getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }

    public void setTiredness(int newTiredness) {
        tiredness = newTiredness;
        if (tiredness < 0) {
            tiredness = 0;
        }
    }

    public void increaseTiredness() {
        tiredness++;
        owner.getMap().broadcastMessage(MaplePacketCreator.updateMount(owner.getId(), this, false));
        if (tiredness > 100) {
            owner.dispelSkill(1004);
        }
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public void startSchedule() {
        tirednessSchedule = TimerManager.getInstance().register(this::increaseTiredness, 60000, 60000);
    }

    public void cancelSchedule() {
        if (tirednessSchedule != null) {
            tirednessSchedule.cancel(false);
        }
    }

    public void setActive(boolean set) {
        active = set;
    }

    public boolean isActive() {
        return active;
    }
}
