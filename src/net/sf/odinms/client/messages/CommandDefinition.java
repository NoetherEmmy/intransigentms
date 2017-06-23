package net.sf.odinms.client.messages;

public class CommandDefinition {
    private final String command;
    private final int requiredLevel; // GM level

    public CommandDefinition(final String command, final int requiredLevel) {
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
