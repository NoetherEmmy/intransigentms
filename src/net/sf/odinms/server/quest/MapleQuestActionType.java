package net.sf.odinms.server.quest;

public enum MapleQuestActionType {
    UNDEFINED(-1),
    EXP(0),
    ITEM(1),
    NEXTQUEST(2),
    MESO(3),
    QUEST(4),
    SKILL(5),
    FAME(6),
    BUFF(7);
    final byte type;

    MapleQuestActionType(int type) {
        this.type = (byte)type;
    }

    public byte getType() {
        return type;
    }

    public static MapleQuestActionType getByType(byte type) {
        for (MapleQuestActionType l : MapleQuestActionType.values()) {
            if (l.getType() == type) return l;
        }
        return null;
    }

    public static MapleQuestActionType getByWZName(String name) {
        switch (name) {
            case "exp":
                return EXP;
            case "money":
                return MESO;
            case "item":
                return ITEM;
            case "skill":
                return SKILL;
            case "nextQuest":
                return NEXTQUEST;
            case "pop":
                return FAME;
            case "buffItemID":
                return BUFF;
            default:
                return UNDEFINED;
        }
    }
}
