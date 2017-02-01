package net.sf.odinms.client.messages;

public class StringMessageCallback implements MessageCallback {
    final StringBuilder ret = new StringBuilder();

    @Override
    public void dropMessage(String message) {
        ret.append(message);
        ret.append("\n");
    }

    @Override
    public String toString() {
        return ret.toString();
    }
}
