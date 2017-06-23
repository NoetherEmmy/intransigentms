package net.sf.odinms.server;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.maps.MapleGenericPortal;
import net.sf.odinms.server.maps.MapleMapPortal;

import java.awt.*;

public class PortalFactory {
    private int nextDoorPortal;

    public PortalFactory() {
        nextDoorPortal = 0x80;
    }

    public MaplePortal makePortal(final int type, final MapleData portal) {
        final MapleGenericPortal ret;
        if (type == MaplePortal.MAP_PORTAL) {
            ret = new MapleMapPortal();
        } else {
            ret = new MapleGenericPortal(type);
        }
        loadPortal(ret, portal);
        return ret;
    }

    private void loadPortal(final MapleGenericPortal portal, final MapleData portalData) {
        portal.setName(MapleDataTool.getString(portalData.getChildByPath("pn")));
        portal.setTarget(MapleDataTool.getString(portalData.getChildByPath("tn")));
        portal.setTargetMapId(MapleDataTool.getInt(portalData.getChildByPath("tm")));
        final int x = MapleDataTool.getInt(portalData.getChildByPath("x"));
        final int y = MapleDataTool.getInt(portalData.getChildByPath("y"));
        portal.setPosition(new Point(x, y));
        String script = MapleDataTool.getString("script", portalData, null);
        if (script != null && script.equals("")) {
            script = null;
        }
        portal.setScriptName(script);
        if (portal.getType() == MaplePortal.DOOR_PORTAL) {
            portal.setId(nextDoorPortal);
            nextDoorPortal++;
        } else {
            portal.setId(Integer.parseInt(portalData.getName()));
        }
    }
}
