package net.sf.odinms.scripting.npc;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.scripting.AbstractScriptManager;
import net.sf.odinms.tools.Pair;

import javax.script.Invocable;
import java.util.*;

public class NPCScriptManager extends AbstractScriptManager {
    private final Map<MapleClient, NPCConversationManager> cms = new HashMap<>();
    private final Map<MapleClient, NPCScript> scripts = new HashMap<>();
    private static final NPCScriptManager instance = new NPCScriptManager();
    private final Map<Pair<Integer, Integer>, Integer> npcTalk = new LinkedHashMap<>();

    public static synchronized NPCScriptManager getInstance() {
        return instance;
    }

    public void start(MapleClient c, int npc) {
        start(c, npc, null, null);
    }

    public void start(MapleClient c, int npc, String filename, MapleCharacter chr) {
        try {
            NPCConversationManager cm = new NPCConversationManager(c, npc, chr, filename);
            if (cms.containsKey(c)) {
                dispose(c);
                return;
            }
            cms.put(c, cm);
            Invocable iv = getInvocable("npc/" + npc + ".js", c);
            if (filename != null) {
                iv = getInvocable("npc/" + filename + ".js", c);
            }
            if (iv == null || NPCScriptManager.getInstance() == null) {
                if (iv == null) {
                    cm.sendOk("Hey, I hope you're having a good time on our server!");
                }
                cm.dispose();
                return;
            }
            addNpcTalkTimes(c.getPlayer().getId(), npc);
            engine.put("cm", cm);
            NPCScript ns = iv.getInterface(NPCScript.class);
            scripts.put(c, ns);
            iv.invokeFunction("start");
        } catch (NoSuchMethodException nsme) {
            System.err.println(
                "The `start` method appears to be missing from NPC " +
                    npc +
                    (filename != null ? ", filename " + filename + ".js" : "")
            );
            dispose(c);
            cms.remove(c);
        } catch (Exception e) {
            log.error("Error executing NPC script. NPC: " + npc + " Script: " + filename, e);
            dispose(c);
            cms.remove(c);
        }
    }

    public void action(MapleClient c, byte mode, byte type, int selection) {
        NPCScript ns = scripts.get(c);
        if (ns != null) {
            try {
                ns.action(mode, type, selection);
            } catch (Exception e) {
                log.error(
                    "Error executing NPC script. NPC: " +
                        (cms.containsKey(c) ? cms.get(c).getNpc() : "???") +
                        " Script: " +
                        (cms.containsKey(c) ? cms.get(c).getFileName() : "???"),
                    e
                );
                dispose(c);
            }
        }
    }

    public void dispose(NPCConversationManager cm) {
        cms.remove(cm.getC());
        scripts.remove(cm.getC());
        if (cm.getFileName() != null) {
            resetContext("npc/" + cm.getFileName() + ".js", cm.getC());
        } else {
            resetContext("npc/" + cm.getNpc() + ".js", cm.getC());
        }
    }

    public void dispose(MapleClient c) {
        NPCConversationManager npccm = cms.get(c);
        if (npccm != null) dispose(npccm);
    }

    public NPCConversationManager getCM(MapleClient c) {
        return cms.get(c);
    }

    public int getNpcTalkTimes(int chrid, int npc) {
        Pair<Integer, Integer> pplayer = new Pair<>(chrid, npc); // First time looks wrong
        if (!npcTalk.containsKey(pplayer)) {
            npcTalk.put(pplayer, 0);
        }
        return npcTalk.get(pplayer);
    }

    public void addNpcTalkTimes(int chrid, int npc) {
        Pair<Integer, Integer> pplayer = new Pair<>(chrid, npc);
        if (!npcTalk.containsKey(pplayer)) {
            npcTalk.put(pplayer, 0);
        }
        int talk = 1 + npcTalk.get(pplayer);
        npcTalk.remove(pplayer);
        npcTalk.put(pplayer, talk);
    }

    public void setNpcTalkTimes(int chrid, int npc, int amount) {
        Pair<Integer, Integer> pplayer = new Pair<>(chrid, npc);
        if (!npcTalk.containsKey(pplayer)) {
            npcTalk.put(pplayer, 0);
        }
        npcTalk.remove(pplayer);
        npcTalk.put(pplayer, amount);
    }

    public List<Integer> listTalkedNpcsByID(int chrid) {
        List<Integer> npcs = new ArrayList<>();
        for (final Pair<Integer, Integer> p : npcTalk.keySet()) {
            if (p.getLeft().equals(chrid)) {
                npcs.add(p.getRight());
            }
        }
        return npcs;
    }

    public List<Integer> listAllTalkedNpcs() {
        List<Integer> npcs = new ArrayList<>();
        for (final Pair<Integer, Integer> p : npcTalk.keySet()) {
            npcs.add(p.getRight());
        }
        return npcs;
    }

    public int talkedTimesByNpc(int npc) {
        int i = 0;
        for (final Pair<Integer, Integer> p : npcTalk.keySet()) {
            if (p.getRight().equals(npc)) i += npcTalk.get(p);
        }
        return i;
    }
}
