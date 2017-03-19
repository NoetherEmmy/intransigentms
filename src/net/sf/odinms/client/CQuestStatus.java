package net.sf.odinms.client;

import net.sf.odinms.net.IntValueHolder;

public enum CQuestStatus implements IntValueHolder {
    NONE(-1),
    IN_PROGRESS(0),
    UNIMPRESSED(1),
    ADVENTURESOME(2),
    VALIANT(3),
    FEARLESS(4);
    private final int i;

    CQuestStatus(int i) {
        this.i = i;
    }

    @Override
    public int getValue() {
        return i;
    }

    public static CQuestStatus getByValue(int val) {
        for (CQuestStatus cqc : CQuestStatus.values()) {
            if (cqc.getValue() == val) return cqc;
        }
        return null;
    }

    public static CQuestStatus max(CQuestStatus cqc1, CQuestStatus cqc2) {
        return cqc1.getValue() >= cqc2.getValue() ? cqc1 : cqc2;
    }
}
