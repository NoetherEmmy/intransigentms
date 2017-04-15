package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ExpTable;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.Random;

public class PetFoodHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        if (c.getPlayer().getNoPets() == 0) {
            return;
        }
        int slot = 0;
        final MaplePet[] pets = c.getPlayer().getPets();
        for (int i = 0; i < 3; ++i) {
            if (pets[i] != null) {
                if (pets[i].getFullness() < 100) {
                    slot = i;
                }
            } else {
                break;
            }
        }
        final MaplePet pet = c.getPlayer().getPet(slot);
        slea.readInt();
        slea.readShort();
        final int itemId = slea.readInt();
        if (c.getPlayer().haveItem(itemId, 1, false, true)) {
            final boolean gainCloseness = new Random().nextInt(101) <= 50;
            int newFullness = pet.getFullness() + 30;
            if (pet.getFullness() < 100) {
                if (newFullness > 100) {
                    newFullness = 100;
                }
                pet.setFullness(newFullness);
                if (gainCloseness && pet.getCloseness() < 30000) {
                    int newCloseness = pet.getCloseness() + c.getChannelServer().getPetExpRate();
                    if (newCloseness > 30000) {
                        newCloseness = 30000;
                    }
                    pet.setCloseness(newCloseness);
                    if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                        pet.setLevel(pet.getLevel() + 1);
                        c.getSession().write(MaplePacketCreator.showOwnPetLevelUp(c.getPlayer().getPetIndex(pet)));
                        c.getPlayer()
                         .getMap()
                         .broadcastMessage(
                             MaplePacketCreator.showPetLevelUp(
                                 c.getPlayer(),
                                 c.getPlayer().getPetIndex(pet)
                             )
                         );
                    }
                }
            } else {
                if (gainCloseness) {
                    int newCloseness = pet.getCloseness() - (c.getChannelServer().getPetExpRate());
                    if (newCloseness < 0) {
                        newCloseness = 0;
                    }
                    pet.setCloseness(newCloseness);
                    if (newCloseness < ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                        pet.setLevel(pet.getLevel() - 1);
                    }
                }
            }
            c.getSession().write(MaplePacketCreator.updatePet(pet, true));
            c.getPlayer()
             .getMap()
             .broadcastMessage(
                 c.getPlayer(),
                 MaplePacketCreator.commandResponse(
                     c.getPlayer().getId(),
                     (byte) 1,
                     slot,
                     true,
                     true
                 ),
                 true
             );
            MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemId, 1, true, false);
        }
    }
}
