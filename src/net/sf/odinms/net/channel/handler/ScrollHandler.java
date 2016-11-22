package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.client.IEquip.ScrollResult;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ScrollHandler extends AbstractMaplePacketHandler {
    private static final Logger log = LoggerFactory.getLogger(ScrollHandler.class);

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.readInt();
        byte slot = (byte) slea.readShort();
        byte dst = (byte) slea.readShort();
        byte ws = (byte) slea.readShort();
        boolean whiteScroll = false;
        boolean legendarySpirit = false;
        if ((ws & 2) == 2) {
            whiteScroll = true;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IEquip toScroll = (IEquip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        ISkill LegendarySpirit = SkillFactory.getSkill(1003);
        if (c.getPlayer().getSkillLevel(LegendarySpirit) > 0 && dst >= 0) {
            legendarySpirit = true;
            toScroll = (IEquip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        byte oldLevel = toScroll.getLevel();
        if (toScroll.getUpgradeSlots() < 1) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        MapleInventory useInventory = c.getPlayer().getInventory(MapleInventoryType.USE);
        IItem scroll = useInventory.getItem(slot);
        IItem wscroll = null;

        List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if (!scrollReqs.isEmpty() && !scrollReqs.contains(toScroll.getItemId())) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }

        if (whiteScroll) {
            wscroll = useInventory.findById(2340000);
            if (wscroll == null || wscroll.getItemId() != 2340000) {
                whiteScroll = false;
                log.info("[h4x] Player {} is trying to scroll with non-existent white scroll", new Object[]{c.getPlayer().getName()});
            }
        }
        if (scroll.getItemId() != 2049100 && scroll.getItemId() != 2049122 && scroll.getItemId() != 2049004 && !ii.isCleanSlate(scroll.getItemId())) {
            if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
                log.info("[h4x] Player {} is trying to scroll {} with {}, which should not work.", new Object[]{
                    c.getPlayer().getName(), toScroll.getItemId(), scroll.getItemId()
                });
                return;
            }
        }

        if (scroll.getQuantity() <= 0) {
            throw new InventoryException("<= 0 quantity when scrolling");
        }
        IEquip scrolled = (IEquip) ii.scrollEquipWithId(c, toScroll, scroll.getItemId(), whiteScroll);
        ScrollResult scrollSuccess = IEquip.ScrollResult.FAIL; // fail

        if (scrolled == null) {
            scrollSuccess = IEquip.ScrollResult.CURSE;
        } else if (scrolled.getLevel() > oldLevel || (ii.isCleanSlate(scroll.getItemId()) && scrolled.getLevel() == oldLevel + 1) ||
                (scroll.getItemId() == 2049004 && scrolled.getUpgradeSlots() == ((Equip) ii.getEquipById(scrolled.getItemId())).getUpgradeSlots())) {
            scrollSuccess = IEquip.ScrollResult.SUCCESS;
        }
        useInventory.removeItem(scroll.getPosition(), (short) 1, false);

        if (whiteScroll) {
            useInventory.removeItem(wscroll.getPosition(), (short) 1, false);
            if (wscroll.getQuantity() < 1) {
                c.getSession().write(MaplePacketCreator.clearInventoryItem(MapleInventoryType.USE, wscroll.getPosition(), false));
            } else {
                c.getSession().write(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, wscroll));
            }
        }
        if (scrollSuccess == IEquip.ScrollResult.CURSE) {
            c.getSession().write(MaplePacketCreator.scrolledItem(scroll, toScroll, true));
            if (dst < 0) {
                c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else {
            c.getSession().write(MaplePacketCreator.scrolledItem(scroll, scrolled, false));
        }
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit));
        if (dst < 0 && (scrollSuccess == IEquip.ScrollResult.SUCCESS || scrollSuccess == IEquip.ScrollResult.CURSE)) {
            c.getPlayer().equipChanged();
        }
    }
}
