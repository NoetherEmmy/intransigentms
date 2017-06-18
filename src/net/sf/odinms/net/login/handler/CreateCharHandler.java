package net.sf.odinms.net.login.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class CreateCharHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final String name = slea.readMapleAsciiString();
        final int face = slea.readInt();
        final int hair = slea.readInt();
        final int hairColor = slea.readInt();
        final int skinColor = slea.readInt();
        final int top = slea.readInt();
        final int bottom = slea.readInt();
        final int shoes = slea.readInt();
        final int weapon = slea.readInt();
        final int gender = slea.readByte();
        final int str = slea.readByte();
        final int dex = slea.readByte();
        final int _int = slea.readByte();
        final int luk = slea.readByte();

        final MapleCharacter newchar = MapleCharacter.getDefault(c);
        newchar.setWorld(c.getWorld());
        newchar.setFace(face);
        newchar.setHair(hair + hairColor);
        newchar.setGender(gender);
        newchar.setStr(4);
        newchar.setDex(4);
        newchar.setInt(4);
        newchar.setLuk(4);
        newchar.setRemainingAp(9);
        newchar.setName(name, false);
        newchar.setSkinColor(MapleSkinColor.getById(skinColor));

        final MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        final IItem eq_top = MapleItemInformationProvider.getInstance().getEquipById(top);
        eq_top.setPosition((byte) -5);
        equip.addFromDB(eq_top);
        final IItem eq_bottom = MapleItemInformationProvider.getInstance().getEquipById(bottom);
        eq_bottom.setPosition((byte) -6);
        equip.addFromDB(eq_bottom);
        final IItem eq_shoes = MapleItemInformationProvider.getInstance().getEquipById(shoes);
        eq_shoes.setPosition((byte) -7);
        equip.addFromDB(eq_shoes);
        final IItem eq_weapon = MapleItemInformationProvider.getInstance().getEquipById(weapon);
        eq_weapon.setPosition((byte) -11);
        equip.addFromDB(eq_weapon);
        final IItem pWeap = MapleItemInformationProvider.getInstance().getEquipById(1092161);
        pWeap.setPosition((byte) -110);
        equip.addFromDB(pWeap);

        boolean charok = true;

        final int totstats = str + dex + _int + luk;
        if (totstats != 25 || str < 4 || dex < 4 || _int < 4 || luk < 4) {
            charok = false;
        }
        if (gender == 0) {
            if (face != 20000 && face != 20001 && face != 20002) {
                charok = false;
            }
            if (hair != 30000 && hair != 30020 && hair != 30030) {
                charok = false;
            }
            if (top != 1040002 && top != 1040006 && top != 1040010) {
                charok = false;
            }
            if (bottom != 1060006 && bottom != 1060002) {
                charok = false;
            }
        } else if (gender == 1) {
            if (face != 21000 && face != 21001 && face != 21002) {
                charok = false;
            }
            if (hair != 31000 && hair != 31040 && hair != 31050) {
                charok = false;
            }
            if (top != 1041002 && top != 1041006 && top != 1041010 && top != 1041011) {
                charok = false;
            }
            if (bottom != 1061002 && bottom != 1061008) {
                charok = false;
            }
        } else {
            charok = false;
        }
        if (skinColor < 0 || skinColor > 3) {
            charok = false;
        }
        if (weapon != 1302000 && weapon != 1322005 && weapon != 1312004) {
            charok = false;
        }
        if (shoes != 1072001 && shoes != 1072005 && shoes != 1072037 && shoes != 1072038) {
            charok = false;
        }
        if (hairColor != 0 && hairColor != 2 && hairColor != 3 && hairColor != 7) {
            charok = false;
        }
        if (!MapleCharacterUtil.isNameLegal(name)) {
            charok = false;
        }
        if (MapleCharacterUtil.hasSymbols(name)) {
            charok = false;
        }
        if (name.length() < 3 || name.length() > 12) {
            charok = false;
        }
        newchar.setTrueDamage(true);

        if (charok && MapleCharacterUtil.canCreateChar(name, c.getWorld())) {
            newchar.saveToDB(false, true);
            c.getSession().write(MaplePacketCreator.addNewCharEntry(newchar, true));
        } else {
            System.err.println(MapleClient.getLogMessage(c, "Trying to create a character with a name: " + name));
        }
    }
}
