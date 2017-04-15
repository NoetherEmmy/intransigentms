package net.sf.odinms.server.quest;

public enum MapleQuestRequirementType {
    UNDEFINED(-1),
    JOB(0),
    ITEM(1),
    QUEST(2),
    MIN_LEVEL(3),
    MAX_LEVEL(4),
    END_DATE(5),
    MOB(6),
    NPC(7),
    FIELD_ENTER(8),
    INTERVAL(9),
    SCRIPT(10),
    PET(11),
    MIN_PET_TAMENESS(12);

    public MapleQuestRequirementType getITEM() {
        return ITEM;
    }

    final byte type;

    MapleQuestRequirementType(final int type) {
        this.type = (byte)type;
    }

    public byte getType() {
        return type;
    }

    public static MapleQuestRequirementType getByType(final byte type) {
        for (final MapleQuestRequirementType l : MapleQuestRequirementType.values()) {
            if (l.getType() == type) return l;
        }
        return null;
    }

    public static MapleQuestRequirementType getByWZName(final String name) {
        switch (name) {
            case "job":
                return JOB;
            case "quest":
                return QUEST;
            case "item":
                return ITEM;
            case "lvmin":
                return MIN_LEVEL;
            case "lvmax":
                return MAX_LEVEL;
            case "end":
                return END_DATE;
            case "mob":
                return MOB;
            case "npc":
                return NPC;
            case "fieldEnter":
                return FIELD_ENTER;
            case "interval":
                return INTERVAL;
            case "startscript":
                return SCRIPT;
            case "endscript":
                return SCRIPT;
            case "pet":
                return PET;
            case "pettamenessmin":
                return MIN_PET_TAMENESS;
            default:
                return UNDEFINED;
        }
    }
}
