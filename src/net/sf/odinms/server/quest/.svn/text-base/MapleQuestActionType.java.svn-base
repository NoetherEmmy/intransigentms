package net.sf.odinms.server.quest;

public enum MapleQuestActionType {
    UNDEFINED(-1), EXP(0), ITEM(1), NEXTQUEST(2), MESO(3), QUEST(4), SKILL(5), FAME(6), BUFF(7);

    final byte type;

    private MapleQuestActionType(int type) {
        this.type = (byte)type;
    }

    public byte getType() {
        return type;
    }

    public static MapleQuestActionType getByType(byte type) {
        for (MapleQuestActionType l : MapleQuestActionType.values()) {
            if (l.getType() == type) {
                return l;
            }
        }
        return null;
    }

    public static MapleQuestActionType getByWZName(String name) {
        if (name.equals("exp")) return EXP;
        else if (name.equals("money")) return MESO;
        else if (name.equals("item")) return ITEM;
        else if (name.equals("skill")) return SKILL;
        else if (name.equals("nextQuest")) return NEXTQUEST;
        else if (name.equals("pop")) return FAME;
        else if (name.equals("buffItemID")) return BUFF;
        else return UNDEFINED;
    }
}