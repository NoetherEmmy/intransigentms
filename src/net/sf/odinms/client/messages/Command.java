package net.sf.odinms.client.messages;

import net.sf.odinms.client.MapleClient;

public interface Command {
    CommandDefinition[] getDefinition();
    void execute(MapleClient c, MessageCallback mc, String[] splittedLine) throws Exception;
}
