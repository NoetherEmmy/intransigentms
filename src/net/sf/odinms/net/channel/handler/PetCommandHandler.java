package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.Random;

public class PetCommandHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter p = c.getPlayer();
        p.resetAfkTime();
        //System.out.println(slea.toString());
        int petId = slea.readInt();
        int petIndex = p.getPetIndex(petId);
        MaplePet pet;
        if (petIndex == -1) {
            return;
        } else {
             pet = p.getPet(petIndex);
        }
        slea.readInt();
        slea.readByte();
        byte command = slea.readByte();
        PetCommand petCommand = PetDataFactory.getPetCommand(pet.getItemId(), (int) command);
        boolean success = false;
        Random rand = new Random();
        int random = rand.nextInt(101);
        if (random <= petCommand.getProbability()) {
            success = true;
            if (pet.getCloseness() < 30000) {
                int newCloseness =
                    pet.getCloseness() + petCommand.getIncrease() * c.getChannelServer().getPetExpRate();
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.getSession().write(MaplePacketCreator.showOwnPetLevelUp(p.getPetIndex(pet)));
                    p.getMap()
                     .broadcastMessage(
                         MaplePacketCreator.showPetLevelUp(
                             p,
                             p.getPetIndex(pet)
                         )
                     );
                }
                c.getSession().write(MaplePacketCreator.updatePet(pet, true));
            }
        }

        p.getMap()
         .broadcastMessage(
             p,
             MaplePacketCreator.commandResponse(
                 p.getId(),
                 command,
                 petIndex,
                 success,
                 false
             ),
             true
         );
    }
}
