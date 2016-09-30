package net.sf.odinms.net.channel;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleDisease;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.life.MobSkill;
import net.sf.odinms.server.life.MobSkillFactory;
import net.sf.odinms.server.maps.MapleMap;

public class PartyQuestMapInstance {
    private final PartyQuest partyQuest;
    private final MapleMap map;
    private final Map<String, Runnable> events = new HashMap<>(5, 0.75f);

    PartyQuestMapInstance(PartyQuest partyQuest, MapleMap map) {
        this.partyQuest = partyQuest;
        this.map = map;
    }

    public void dispose() {
        events.clear();
        partyQuest.removeMapInstance(this);
    }

    public MapleMap getMap() {
        return map;
    }

    public PartyQuest getPartyQuest() {
        return partyQuest;
    }

    public void addEvent(String name, Runnable event) {
        events.put(name, event);
    }

    /**
     * @param name
     * Name of event as invoked from outside. Cannot be <code>null</code>. <br />
     * <pre>
     *     name <br />
     *     ---------------- <br />
     *     playerHit <br />
     *     mobKilled <br />
     * </pre>
     * @param eventType
     * The "type" of what the event does (cannot be <code>null</code>): <br />
     * <pre>
     *     eventType       | curriedArgs <br />
     *     ----------------+-------------------------- <br />
     *     addPoints       | increment <br />
     *     warpAll         | mapId <br />
     *     startNpc        | npcId <br />
     *     setReactorState | reactor, state <br />
     *     debuffPlayers   | sourceId, level <br />
     *     buffPlayers     | sourceId, level <br />
     *     debuffMobs      | sourceId, level <br />
     *     buffMobs        | sourceId, level <br />
     *     killAll         | <br />
     *     spawnMob        | mobId, position <br />
     *     spawnMobs       | mobId, position, count <br />
     *     schedule        | runnable, delay <br />
     *     scheduleCycle   | runnable, period[, delay]
     * </pre>
     * @param curriedArgs
     * Constant arguments that are passed in every time the
     * event is invoked. Can be <code>null</code>.
     */
    public void addEvent(String name, String eventType, List<Object> curriedArgs) {
        Runnable event;
        try {
            switch (eventType) {
                case "addPoints":
                    event = () -> partyQuest.addPoints((Integer) curriedArgs.get(0));
                    break;
                case "warpAll":
                    event = () -> {
                        for (MapleCharacter p : getMap().getCharacters()) {
                            p.changeMap((Integer) curriedArgs.get(0));
                        }
                    };
                    break;
                case "debuffPlayers":
                    event = () -> {
                        int sourceId = (Integer) curriedArgs.get(0);
                        int level = (Integer) curriedArgs.get(1);
                        MobSkill ms = MobSkillFactory.getMobSkill(sourceId, level);
                        MapleDisease disease;
                        switch (sourceId) {
                            case 120:
                                disease = MapleDisease.SEAL;
                                break;
                            case 121:
                                disease = MapleDisease.DARKNESS;
                                break;
                            case 122:
                                disease = MapleDisease.WEAKEN;
                                break;
                            case 123:
                                disease = MapleDisease.STUN;
                                break;
                            case 124:
                                disease = MapleDisease.CURSE;
                                break;
                            case 125:
                                disease = MapleDisease.POISON;
                                break;
                            case 126:
                                disease = MapleDisease.SLOW;
                                break;
                            case 128:
                                disease = MapleDisease.SEDUCE;
                                break;
                            default:
                                System.out.println("Failed to apply debuff of skill ID " + sourceId + " and skill level " + level + " in PartyQuestMapInstance$.debuffPlayers()");
                                return;
                        }
                        for (MapleCharacter p : getMap().getCharacters()) {
                            p.giveDebuff(disease, ms);
                        }
                    };
                    break;
                case "startNpc":
                    event = () -> {
                        int npcId = (Integer) curriedArgs.get(0);
                        NPCScriptManager npcsm = NPCScriptManager.getInstance();
                        for (MapleCharacter p : getMap().getCharacters()) {
                            npcsm.start(p.getClient(), npcId);
                        }
                    };
                    break;
                default: // TODO: Add more event types
                    System.out.println("Failed to add event " + name + " with eventType " + eventType);
                    return;
            }
        } catch (ClassCastException cce) {
            String argsList = "\n";
            for (Object a : curriedArgs) {
                argsList += a.toString() + "\n";
            }
            System.out.println("Failed to cast a curried argument for eventType " + eventType + ": " + argsList);
            return;
        } catch (IndexOutOfBoundsException ioobe) {
            String argsList = "\n";
            for (Object a : curriedArgs) {
                argsList += a.toString() + "\n";
            }
            System.out.println("Index out of bounds for curried arguments for eventType " + eventType + ": " + argsList);
            return;
        }
        events.put(name, event);
    }

    public void removeEvent(String name) {
        events.remove(name);
    }

    public void invokeEvent(String name) {
        Runnable event = events.get(name);
        if (event != null) {
            event.run();
        }
    }
}