package net.sf.odinms.scripting.npc;

public interface NPCScript {
    void start();

    void action(byte mode, byte type, int selection);
}
