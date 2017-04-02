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
        for (CQuestStatus cqs : CQuestStatus.values()) {
            if (cqs.getValue() == val) return cqs;
        }
        return null;
    }

    /**
     * <ul>
     * <li>pure?: yes</li>
     * <li>nullable?: no</li>
     * </ul>
     *
     * @param cqc1 The first {@code CQuestStatus} to be compared.
     * @param cqc2 The second {@code CQuestStatus} to be compared.
     * @throws NullPointerException when {@code cqc1 == null || cqc2 == null}.
     * @return The maximum of the two arguments.
     */
    public static CQuestStatus max(CQuestStatus cqc1, CQuestStatus cqc2) {
        return cqc1.getValue() >= cqc2.getValue() ? cqc1 : cqc2;
    }
}
