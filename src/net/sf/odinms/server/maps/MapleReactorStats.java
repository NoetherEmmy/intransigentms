package net.sf.odinms.server.maps;

import net.sf.odinms.tools.Pair;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MapleReactorStats {
    private Point tl, br;
    private final Map<Byte, StateData> stateInfo = new HashMap<>();

    /*
    public int getInfoId() {
        return infoId;
    }

    public void setInfoId(int infoId) {
        this.infoId = infoId;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
    */

    public void setTL(final Point tl) {
        this.tl = tl;
    }

    public void setBR(final Point br) {
        this.br = br;
    }

    public Point getTL() {
        return tl;
    }

    public Point getBR() {
        return br;
    }

    public void addState(final byte state, final int type, final Pair<Integer, Integer> reactItem, final byte nextState) {
        final StateData newState = new StateData(type, reactItem, nextState);
        stateInfo.put(state, newState);
    }

    public byte getNextState(final byte state) {
        final StateData nextState = stateInfo.get(state);
        if (nextState != null) return nextState.getNextState();
        return -1;
    }

    public int getType(final byte state) {
        final StateData nextState = stateInfo.get(state);
        if (nextState != null) return nextState.getType();
        return -1;
    }

    public Pair<Integer, Integer> getReactItem(final byte state) {
        final StateData nextState = stateInfo.get(state);
        if (nextState != null) return nextState.getReactItem();
        return null;
    }

    private class StateData {
        private final int type;
        private final Pair<Integer, Integer> reactItem;
        private final byte nextState;

        private StateData(final int type, final Pair<Integer, Integer> reactItem, final byte nextState) {
            this.type = type;
            this.reactItem = reactItem;
            this.nextState = nextState;
        }

        private int getType() {
            return type;
        }

        private byte getNextState() {
            return nextState;
        }

        private Pair<Integer, Integer> getReactItem() {
            return reactItem;
        }
    }
}
