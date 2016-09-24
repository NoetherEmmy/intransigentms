package net.sf.odinms.scripting.quest;

public interface QuestScript {
    public void start(byte mode, byte type, int selection);
    public void end(byte mode, byte type, int selection);
}