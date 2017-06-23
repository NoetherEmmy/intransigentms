package net.sf.odinms.server.maps;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.scripting.portal.PortalScriptManager;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.fourthjobquests.FourthJobQuestsPortalHandler;
import net.sf.odinms.tools.MaplePacketCreator;

import java.awt.*;

public class MapleGenericPortal implements MaplePortal {
    private String name, target;
    private Point position;
    private int targetmap;
    private final int type;
    private int id;
    private String scriptName;
    private boolean portalState;

    public MapleGenericPortal(final int type) {
        this.type = type;
        portalState = OPEN;
    }

    @Override
    public void setPortalState(final boolean state) {
        this.portalState = state;
    }

    @Override
    public boolean getPortalState() {
        return portalState;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Point getPosition() {
        return position;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public int getTargetMapId() {
        return targetmap;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setPosition(final Point position) {
        this.position = position;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    public void setTargetMapId(final int targetmapid) {
        this.targetmap = targetmapid;
    }

    @Override
    public void setScriptName(final String scriptName) {
        this.scriptName = scriptName;
    }

    @Override
    public void enterPortal(final MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        final double distanceSq = position.distanceSq(player.getPosition());
        if (distanceSq > 22500) {
            player.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL, "D" + Math.sqrt(distanceSq));
        }

        boolean changed = false;
        if (scriptName != null) {
            if (!FourthJobQuestsPortalHandler.handlePortal(scriptName, player)) {
                changed = PortalScriptManager.getInstance().executePortalScript(this, c);
            }
        } else if (targetmap != 999999999) {
            final MapleMap to;
            if (player.getEventInstance() == null) {
                to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetmap);
            } else {
                to = player.getEventInstance().getMapInstance(targetmap);
            }
            MaplePortal pto = to.getPortal(target);
            if (pto == null) {
                pto = to.getPortal(0);
            }
            player.changeMap(to, pto);
            changed = true;
        }
        if (!changed) {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }
}
