package net.sf.odinms.server.maps;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.scripting.reactor.ReactorScriptManager;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;

import java.awt.*;

public class MapleReactor extends AbstractMapleMapObject {
    private final int rid;
    private final MapleReactorStats stats;
    private byte state;
    private int delay;
    private MapleMap map;
    private boolean alive;
    private String name;
    private boolean timerActive;
    private Integer trigger;
    //private static Logger log = LoggerFactory.getLogger(MapleReactor.class);

    public MapleReactor(final MapleReactorStats stats, final int rid) {
        this.stats = stats;
        this.rid = rid;
        alive = true;
    }

    public void setTimerActive(final boolean active) {
        this.timerActive = active;
    }

    public boolean isTimerActive() {
        return timerActive;
    }

    public int getReactorId() {
        return rid;
    }

    public void setState(final byte state) {
        this.state = state;
    }

    public byte getState() {
        return state;
    }

    public int getId() {
        return rid;
    }

    public void setDelay(final int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return delay;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.REACTOR;
    }

    public int getReactorType() {
        return stats.getType(state);
    }

    public void setMap(final MapleMap map) {
        this.map = map;
    }

    public MapleMap getMap() {
        return map;
    }

    public Pair<Integer, Integer> getReactItem() {
        return stats.getReactItem(state);
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(final boolean alive) {
        this.alive = alive;
    }

    @Override
    public void sendDestroyData(final MapleClient client) {
        client.getSession().write(makeDestroyData());
    }

    public MaplePacket makeDestroyData() {
        return MaplePacketCreator.destroyReactor(this);
    }

    @Override
    public void sendSpawnData(final MapleClient client) {
        client.getSession().write(makeSpawnData());
    }

    public MaplePacket makeSpawnData() {
        return MaplePacketCreator.spawnReactor(this);
    }

    public void delayedHitReactor(final MapleClient c, final long delay) {
        TimerManager.getInstance().schedule(() -> hitReactor(c), delay);
    }

    /** {@code hitReactor} command for item-triggered reactors. */
    public void hitReactor(final MapleClient c) {
        hitReactor(0, (short) 0, c);
    }

    public void hitReactor(final int charPos, final short stance, final MapleClient c) {
        if (trigger != null) {
            if (map.getPartyQuestInstance() != null) {
                map.getPartyQuestInstance().trigger(trigger);
            }
        }

        if (stats.getType(state) < 999 && stats.getType(state) != -1) {
            // Type 2 = only hit from right (kerning swamp plants), 00 is air left 02 is ground left
            if (!(stats.getType(state) == 2 && (charPos == 0 || charPos == 2))) {
                // Get next state
                state = stats.getNextState(state);

                if (stats.getNextState(state) == -1) { // End of reactor
                    if (stats.getType(state) < 100) { // Reactor broken
                        if (delay > 0) {
                            map.destroyReactor(getObjectId());
                        } else { // Trigger as normal
                            map.broadcastMessage(MaplePacketCreator.triggerReactor(this, stance));
                        }
                    } else { // Item-triggered on final step
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(this, stance));
                    }
                    ReactorScriptManager.getInstance().act(c, this);
                } else { // Reactor not broken yet
                    map.broadcastMessage(MaplePacketCreator.triggerReactor(this, stance));
                    if (state == stats.getNextState(state)) { // Current state = next state, looping reactor
                        ReactorScriptManager.getInstance().act(c, this);
                    }
                }
            }
        } else {
            state++;
            map.broadcastMessage(MaplePacketCreator.triggerReactor(this, stance));
            ReactorScriptManager.getInstance().act(c, this);
        }
    }

    public Rectangle getArea() {
        final int height = stats.getBR().y - stats.getTL().y;
        final int width  = stats.getBR().x - stats.getTL().x;
        final int origX  = getPosition().x + stats.getTL().x;
        final int origY  = getPosition().y + stats.getTL().y;

        return new Rectangle(origX, origY, width, height);
    }

    public void setTrigger(final int trigger) {
        this.trigger = trigger;
    }

    public int getTrigger() {
        return trigger;
    }

    public boolean isTrigger() {
        return trigger != null;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return
            "Reactor " +
                getObjectId() +
                " of id " +
                rid +
                " at position " +
                getPosition() +
                ", with state " +
                state +
                ", and type " +
                stats.getType(state);
    }
}
