package net.sf.odinms.scripting.quest;

public interface QuestScript {
    void start(byte mode, byte type, int selection);

    void end(byte mode, byte type, int selection);
}
