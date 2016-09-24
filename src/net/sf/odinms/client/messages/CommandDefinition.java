package net.sf.odinms.client.messages;

public class CommandDefinition {

    private String command;
    private int requiredLevel; // GM level.

    public CommandDefinition(String command, int requiredLevel) {
        this.command = command;
        this.requiredLevel = requiredLevel;
    }

    public String getCommand() {
        return command;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }
}