package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.PlayerNPCs;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class NPCTalkHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final int oid = slea.readInt();
        slea.readInt();
        final MapleMapObject obj = c.getPlayer().getMap().getMapObject(oid);
        if (obj instanceof MapleNPC) {
            final MapleNPC npc = (MapleNPC) obj;
            if (NPCScriptManager.getInstance() != null)
                NPCScriptManager.getInstance().dispose(c);
            if (!c.getPlayer().getCheatTracker().Spam(1000, 4)) {
                if (npc.getId() == 9010009) {
                    if (c.isGuest()) {
                        c.getPlayer().dropMessage(1, "Duey is not available to guests.");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    c.getSession()
                     .write(
                         MaplePacketCreator.sendDuey(
                             (byte) 8,
                             DueyHandler.loadItems(c.getPlayer())
                         )
                     );
                } else if (npc.hasShop()) {
                    // Destroy the old shop if one exists
                    if (c.getPlayer().getShop() != null) {
                        c.getPlayer().setShop(null);
                        c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 20));
                    }
                    npc.sendShop(c);
                } else {
                    if (c.getCM() != null || c.getQM() != null) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    NPCScriptManager.getInstance().start(c, npc.getId());
                // NPCMoreTalkHandler.npc = npc.getId();
                // 0 = next button
                // 1 = yes no
                // 2 = accept decline
                // 5 = select a link
                // c.getSession().write(MaplePacketCreator.getNPCTalk(npc.getId(), (byte) 0,
                // "Yoo! I'm #p" + npc.getId() + "#, I can warp you.", "00 01"));
                }
            }
        } else if (obj instanceof PlayerNPCs) {
            final PlayerNPCs npc = (PlayerNPCs) obj;
            NPCScriptManager.getInstance().start(c, npc.getId(), npc.getName(), null);
        }
    }
}
