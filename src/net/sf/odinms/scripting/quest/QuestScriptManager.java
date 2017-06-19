package net.sf.odinms.scripting.quest;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.scripting.AbstractScriptManager;

import javax.script.Invocable;
import java.util.HashMap;
import java.util.Map;

public class QuestScriptManager extends AbstractScriptManager {
    private final Map<MapleClient, QuestActionManager> qms = new HashMap<>();
    private final Map<MapleClient, QuestScript> scripts = new HashMap<>();
    private static final QuestScriptManager instance = new QuestScriptManager();

    public static synchronized QuestScriptManager getInstance() {
        return instance;
    }

    public void start(final MapleClient c, final int npc, final int quest) {
        try {
            final QuestActionManager qm = new QuestActionManager(c, npc, quest, true);
            if (qms.containsKey(c)) return;
            qms.put(c, qm);
            final Invocable iv = getInvocable("quest/" + quest + ".js", c);
            if (iv == null) {
                qm.dispose();
                return;
            }
            engine.put("qm", qm);
            final QuestScript qs = iv.getInterface(QuestScript.class);
            scripts.put(c, qs);
            qs.start((byte) 1, (byte) 0, 0);
        } catch (final Exception e) {
            System.err.println("Error executing quest script. (" + quest + ")");
            e.printStackTrace();
            dispose(c);
        }
    }

    public void start(final MapleClient c, final byte mode, final byte type, final int selection) {
        final QuestScript qs = scripts.get(c);
        if (qs != null) {
            try {
                qs.start(mode, type, selection);
            } catch (final Exception e) {
                System.err.println("Error executing quest script. (" + c.getQM().getQuest() + ")");
                e.printStackTrace();
                dispose(c);
            }
        }
    }

    public void end(final MapleClient c, final int npc, final int quest) {
        try {
            final QuestActionManager qm = new QuestActionManager(c, npc, quest, false);
            if (qms.containsKey(c)) return;
            qms.put(c, qm);
            final Invocable iv = getInvocable("quest/" + quest + ".js", c);
            if (iv == null) {
                qm.dispose();
                return;
            }
            engine.put("qm", qm);
            final QuestScript qs = iv.getInterface(QuestScript.class);
            scripts.put(c, qs);
            qs.end((byte) 1, (byte) 0, 0);
        } catch (final Exception e) {
            System.err.println("Error executing quest script. (" + quest + ")");
            e.printStackTrace();
            dispose(c);
        }
    }

    public void end(final MapleClient c, final byte mode, final byte type, final int selection) {
        final QuestScript qs = scripts.get(c);
        if (qs != null) {
            try {
                qs.end(mode, type, selection);
            } catch (final Exception e) {
                System.err.println("Error executing quest script. (" + c.getQM().getQuest() + ")");
                e.printStackTrace();
                dispose(c);
            }
        }
    }

    public void dispose(final QuestActionManager qm, final MapleClient c) {
        qms.remove(c);
        scripts.remove(c);
        resetContext("quest/" + qm.getQuest() + ".js", c);
    }

    public void dispose(final MapleClient c) {
        final QuestActionManager qm = qms.get(c);
        if (qm != null) dispose(qm, c);
    }

    public QuestActionManager getQM(final MapleClient c) {
        return qms.get(c);
    }

    public void clearScripts() {
        scripts.clear();
    }
}
