package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.*;
import net.sf.odinms.client.messages.ServernoticeMapleClientMessageCallback;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMist;
import net.sf.odinms.server.maps.MapleTVEffect;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
//import java.util.logging.Logger;

public class UseCashItemHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleCharacter player = c.getPlayer();
        final ServernoticeMapleClientMessageCallback cm = new ServernoticeMapleClientMessageCallback(1, c);
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final int itemType = itemId / 10000;
        final IItem item = player.getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null || item.getItemId() != itemId || item.getQuantity() < 1) {
            c.disconnect();
            c.getSession().close();
            return;
        }
        try {
            switch (itemType) {
                case 505: // AP/SP reset
                    if (itemId > 5050000) {
                        final int SPTo = slea.readInt();
                        final int SPFrom = slea.readInt();

                        final ISkill skillSPTo = SkillFactory.getSkill(SPTo);
                        final ISkill skillSPFrom = SkillFactory.getSkill(SPFrom);

                        final int maxlevel = skillSPTo.getMaxLevel();
                        final int curLevel = player.getSkillLevel(skillSPTo);
                        final int curLevelSPFrom = player.getSkillLevel(skillSPFrom);

                        if (curLevel + 1 <= maxlevel && curLevelSPFrom > 0) {
                            player.changeSkillLevel(skillSPFrom, curLevelSPFrom - 1, player.getMasterLevel(skillSPFrom));
                            player.changeSkillLevel(skillSPTo, curLevel + 1, player.getMasterLevel(skillSPTo));
                        }
                    } else {
                        final List<Pair<MapleStat, Integer>> statupdate = new ArrayList<>(2);
                        final int APTo = slea.readInt();
                        final int APFrom = slea.readInt();

                        switch (APFrom) {
                            case 64: // str
                                if (player.getStr() <= 4 || ((player.getJob().getId() / 100) == 1 && player.getStr() <= 35)) {
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    return;
                                }
                                player.setStr(player.getStr() - 1);
                                statupdate.add(new Pair<>(MapleStat.STR, player.getStr()));
                                break;
                            case 128: // dex
                                if (
                                    player.getDex() <= 4 ||
                                    (((player.getJob().getId() / 100) == 4 ||
                                    (player.getJob().getId() / 100) == 3) &&
                                    player.getDex() <= 25)
                                ) {
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    return;
                                }
                                player.setDex(player.getDex() - 1);
                                statupdate.add(new Pair<>(MapleStat.DEX, player.getDex()));
                                break;
                            case 256: // int
                                if (player.getInt() <= 4 || ((player.getJob().getId() / 100) == 2 && player.getInt() <= 20)) {
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    return;
                                }
                                player.setInt(player.getInt() - 1);
                                statupdate.add(new Pair<>(MapleStat.INT, player.getInt()));
                                break;
                            case 512: // luk
                                if (player.getLuk() <= 4 || ((player.getJob().getId() / 100) == 4 && player.getLuk() <= 35)) {
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    return;
                                }
                                player.setLuk(player.getLuk() - 1);
                                statupdate.add(new Pair<>(MapleStat.LUK, player.getLuk()));
                                break;
                            case 2048: // HP
                                player.dropMessage(1, "You may not use AP Resets on HP or MP.");
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            case 8192: // MP
                                player.dropMessage(1, "You may not use AP Resets on HP or MP.");
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            default:
                                c.getSession().write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true));
                                return;
                        }
                        boolean undo = false;
                        switch (APTo) {
                            case 64: // str
                                if (player.getStr() >= 30000) {
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    undo = true;
                                    break;
                                }
                                player.setStr(player.getStr() + 1);
                                statupdate.add(new Pair<>(MapleStat.STR, player.getStr()));
                                break;
                            case 128: // dex
                                if (player.getDex() >= 30000) {
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    undo = true;
                                    break;
                                }
                                player.setDex(player.getDex() + 1);
                                statupdate.add(new Pair<>(MapleStat.DEX, player.getDex()));
                                break;
                            case 256: // int
                                if (player.getInt() >= 30000) {
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    undo = true;
                                    break;
                                }
                                player.setInt(player.getInt() + 1);
                                statupdate.add(new Pair<>(MapleStat.INT, player.getInt()));
                                break;
                            case 512: // luk
                                if (player.getLuk() >= 30000) {
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    undo = true;
                                    break;
                                }
                                player.setLuk(player.getLuk() + 1);
                                statupdate.add(new Pair<>(MapleStat.LUK, player.getLuk()));
                                break;
                            case 2048: // hp
                                player.dropMessage(1, "You may not use AP Resets on HP or MP.");
                                c.getSession().write(MaplePacketCreator.enableActions());
                                undo = true;
                                break;
                            case 8192: // mp
                                player.dropMessage(1, "You may not use AP Resets on HP or MP.");
                                c.getSession().write(MaplePacketCreator.enableActions());
                                undo = true;
                                break;
                            default:
                                c.getSession().write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true));
                                undo = true;
                                break;
                        }
                        if (player.getJob().isA(MapleJob.BRAWLER) && player.getInt() < 450) {
                            player.changeSkillLevel(SkillFactory.getSkill(4111006), 0, player.getMasterLevelById(4111006));
                        }
                        if (undo) {
                            switch (APFrom) {
                                case 64: // str
                                    player.setStr(player.getStr() + 1);
                                    break;
                                case 128: // dex
                                    player.setDex(player.getDex() + 1);
                                    break;
                                case 256: // int
                                    player.setInt(player.getInt() + 1);
                                    break;
                                case 512: // luk
                                    player.setLuk(player.getLuk() + 1);
                                    break;
                            }
                            return;
                        }
                        c.getSession().write(MaplePacketCreator.updatePlayerStats(statupdate, true));
                    }
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                    break;
                case 506: // Hmmmm... NPE when double clicking...
                    final int tagType = itemId % 10;
                    IItem eq = null;
                    if (tagType == 0) { // Item tag.
                        final int equipSlot = slea.readShort();
                        if (equipSlot == 0) {
                            break;
                        }
                        eq = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) equipSlot);
                        eq.setOwner(player.getName());
                    } else if (tagType == 1) { // Sealing lock.
                        final byte type = (byte) slea.readInt();
                        if (type == 2) { // We can't do setLocked() for stars.
                            break;
                        }
                        final byte slot_ = (byte) slea.readInt();
                        eq = player.getInventory(MapleInventoryType.getByType(type)).getItem(slot_);
                        final Equip equip = (Equip) eq;
                        equip.setLocked((byte) 1);
                    }/* else if (tagType == 2) { // Incubator.
                    }*/
                    slea.readInt();
                    c.getSession().write(MaplePacketCreator.updateEquipSlot(eq));
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                    break;
                case 507:
                    if (player.getCanSmega() && player.getSmegaEnabled() && !player.getCheatTracker().Spam(3000, 3)) {
                        switch (itemId / 1000 % 10) {
                            case 1: // Megaphone
                                if (player.getLevel() >= 10) {
                                    player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(2, player.getName() + " : " + slea.readMapleAsciiString()));
                                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                                } else {
                                    player.dropMessage("You may not use this until you are level 10.");
                                }
                                break;
                            case 2: // Super megaphone
                                c.getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(3, c.getChannel(), player.getName() + " : " + slea.readMapleAsciiString(), (slea.readByte() != 0)).getBytes());
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                                break;
                            case 3: // Heart megaphone
                            case 4: // Skull megaphone
                                System.err.println("Unhandled megaphone packet: " + slea.getBytesRead());
                                System.err.println("Megaphone ID: " + itemId);
                                break;
                            case 5: // Maple TV
                                final int tvType = itemId % 10;
                                boolean megassenger = false;
                                boolean ear = false;
                                MapleCharacter victim = null;

                                if (tvType != 1) {
                                    if (tvType >= 3) {
                                        megassenger = true;
                                        if (tvType == 3) {
                                            slea.readByte();
                                        }
                                        ear = 1 == slea.readByte();
                                    } else if (tvType != 2) {
                                        slea.readByte();
                                    }
                                    if (tvType != 4) {
                                        final String name = slea.readMapleAsciiString();
                                        if (!name.isEmpty()) {
                                            final int channel = c.getChannelServer().getWorldInterface().find(name);
                                            if (channel == -1) {
                                                player.dropMessage(1, "Player could not be found.");
                                                break;
                                            }
                                            victim = net.sf.odinms.net.channel.ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(name);
                                        }
                                    }
                                }
                                final List<String> messages = new ArrayList<>();
                                final StringBuilder builder = new StringBuilder();
                                for (int i = 0; i < 5; ++i) {
                                    final String message = slea.readMapleAsciiString();
                                    if (megassenger) {
                                        builder.append(' ');
                                        builder.append(message); // builder.append(" "+message);
                                    }
                                    messages.add(message);
                                }
                                slea.readInt();
                                if (!MapleTVEffect.active) {
                                    if (megassenger) {
                                        c.getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(3, c.getChannel(), player.getName() + " : " + builder.toString(), ear).getBytes());
                                    }
                                    new MapleTVEffect(player, victim, messages, tvType);
                                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                                } else {
                                    player.dropMessage(1, "The Maple TV is already in use.");
                                }
                            }
                        } else {
                            cm.dropMessage("You have lost your megaphone privileges.");
                        }
                    break;
                case 539:
                    if (player.getCanSmega() && player.getSmegaEnabled() && !player.getCheatTracker().Spam(3000, 3)) {
                        final List<String> lines = new ArrayList<>();
                        for (int i = 0; i < 4; ++i) {
                            lines.add(slea.readMapleAsciiString());
                        }
                        c.getChannelServer().getWorldInterface().broadcastSMega(null, MaplePacketCreator.getAvatarMega(c.getPlayer(), c.getChannel(), itemId, lines, (slea.readByte() != 0)).getBytes());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);

                    } else {
                        cm.dropMessage("You have lost your megaphone privileges.");
                    }
                    break;
                case 520:
                    if (ii.getMeso(itemId) + player.getMeso() < Integer.MAX_VALUE) {
                        player.gainMeso(ii.getMeso(itemId), true, false, true);
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                        c.getSession().write(MaplePacketCreator.enableActions()); // do we really need this?
                    } else {
                        player.dropMessage(1, "Cannot hold anymore mesos.");
                    }
                    break;
                case 510:
                    player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.musicChange("Jukebox/Congratulation"), true);
                    break;
                case 512:
                    player.getMap().startMapEffect(ii.getMsg(itemId).replaceFirst("%s", player.getName()).replaceFirst("%s", slea.readMapleAsciiString()), itemId);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                    break;
                case 524:
                    for (int i = 0; i < 3; ++i) {
                        if (player.getPet(i) == null) break;
                        final MaplePet pet = player.getPet(i);
                        if (player.getInventory(MapleInventoryType.CASH).getItem(slot) != null) {
                            final int petID = pet.getItemId();
                            if (
                                itemId == 5240012 &&
                                petID >= 5000028 &&
                                petID <= 5000033 ||
                                itemId == 5240021 &&
                                petID >= 5000047 &&
                                petID <= 5000053 ||
                                itemId == 5240004 &&
                                (petID == 5000007 || petID == 5000023) ||
                                itemId == 5240006 &&
                                (
                                    petID == 5000003 ||
                                    petID == 5000007 ||
                                    petID >= 5000009 &&
                                    petID <= 5000010 ||
                                    petID == 5000012 ||
                                    petID == 5000044
                                )
                            ) {
                                pet.setFullness(100);
                                final int closeGain = 100 * c.getChannelServer().getPetExpRate();
                                if (pet.getCloseness() + closeGain > 30000) {
                                    pet.setCloseness(30000);
                                } else {
                                    pet.setCloseness(pet.getCloseness() + closeGain);
                                }
                                while (pet.getCloseness() >= ExpTable.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                                    pet.setLevel(pet.getLevel() + 1);
                                    c.getSession().write(MaplePacketCreator.showOwnPetLevelUp(player.getPetIndex(pet)));
                                    player.getMap().broadcastMessage(MaplePacketCreator.showPetLevelUp(c.getPlayer(), player.getPetIndex(pet)));
                                }
                                c.getSession().write(MaplePacketCreator.updatePet(pet, true));
                                player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.commandResponse(player.getId(), (byte) 1, 0, true, true), true);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                            } else if (pet.canConsume(itemId)) {
                                pet.setFullness(100);
                                final int closeGain = 100 * c.getChannelServer().getPetExpRate();
                                if (pet.getCloseness() + closeGain > 30000) {
                                    pet.setCloseness(30000);
                                } else {
                                    pet.setCloseness(pet.getCloseness() + closeGain);
                                }
                                while (pet.getCloseness() >= ExpTable.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                                    pet.setLevel(pet.getLevel() + 1);
                                    c.getSession().write(MaplePacketCreator.showOwnPetLevelUp(player.getPetIndex(pet)));
                                    player.getMap().broadcastMessage(MaplePacketCreator.showPetLevelUp(c.getPlayer(), player.getPetIndex(pet)));
                                }
                                c.getSession().write(MaplePacketCreator.updatePet(pet, true));
                                player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.commandResponse(player.getId(), (byte) 1, 0, true, true), true);
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                            }
                            c.getSession().write(MaplePacketCreator.enableActions());
                        } else {
                            break;
                        }
                    }
                    break;
                case 528:
                    if (itemId == 5281000) {
                        final Rectangle bounds = new Rectangle((int) player.getPosition().getX(), (int) player.getPosition().getY(), 1, 1);
                        final MapleStatEffect mse = new MapleStatEffect();
                        mse.setSourceId(2111003);
                        final MapleMist mist = new MapleMist(bounds, c.getPlayer(), mse);
                        player.getMap().spawnMist(mist, 10000, false, true);
                    }
                    break;
                case 509:
                    final String sendTo = slea.readMapleAsciiString();
                    final String msg = slea.readMapleAsciiString();
                    final int recipientId = MapleCharacter.getIdByName(sendTo, c.getWorld());
                    if (recipientId > -1) {
                        try {
                            player.sendNote(recipientId, msg);
                        } catch (final SQLException ex) {
                            //Logger.getLogger(UseCashItemHandler.class.getName()).log(Level.SEVERE, null, ex);
                            ex.printStackTrace();
                        }
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                    } else {
                        player.dropMessage(5, "This player was not found in the database.");
                    }
                    break;
                case 517:
                    final MaplePet pet = player.getPet(0);
                    if (pet != null) {
                        final String newName = slea.readMapleAsciiString();
                        if (newName.length() > 2 && newName.length() < 14) {
                            pet.setName(newName);
                            c.getSession().write(MaplePacketCreator.updatePet(pet, true));
                            c.getSession().write(MaplePacketCreator.enableActions());
                            player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.changePetName(c.getPlayer(), newName), true);
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                        } else {
                            cm.dropMessage("Names must be 3 ~ 13 characters.");
                        }
                    } else {
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                    break;
                case 537:
                    final String text = slea.readMapleAsciiString();
                    slea.readInt();
                    player.setChalkboard(text);
                    break;
                case 530:
                    ii.getItemEffect(itemId).applyTo(player);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                    break;
                case 504:
                    final byte rocktype = slea.readByte();
                    final boolean vip = itemId == 5041000;
                    if (rocktype == 0x00) {
                        final int mapId = slea.readInt();
                        final MapleMap map = c.getChannelServer().getMapFactory().getMap(mapId);
                        /*
                        if (c.getChannelServer().getMapFactory().getMap(mapId).getReturnMapId() == 999999999) {
                            player.changeMap(mapId);
                            System.out.print("Changed map, Map ID: " + mapId);
                        }
                        */
                        if (
                            mapId < 2000000 ||
                            map.hasFieldLimit(MapleMap.FieldLimit.MYSTIC_DOOR_LIMIT) ||
                            map.hasFieldLimit(MapleMap.FieldLimit.MIGRATE_LIMIT)
                        ) {
                            player.dropMessage(1, "The map you are trying to teleport to is forbidden.");
                            break;
                        }
                        if (isZipanguMap(mapId) && !isZipanguMap(player.getMapId())) {
                            player.setReturnMap(player.getMapId());
                        }
                        if (mapId / 10000000 == 27 && !canGoToTimeLaneMap(player, mapId)) break;
                        if (player.getPartyQuest() != null) {
                            player.leaveParty();
                        }
                        player.changeMap(mapId);
                    } else {
                        final String name = slea.readMapleAsciiString();
                        MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                        if (victim == null) {
                            final int channel = c.getChannelServer().getWorldInterface().find(name);
                            if (channel == -1) {
                                player.dropMessage(1, "That player is not online.");
                                break;
                            }
                            ChangeChannelHandler.changeChannel(channel, player.getClient());
                            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                        }
                        if (victim == null) { // Player dcs/ccs while you cc
                            player.dropMessage("System error!");
                            break;
                        } else {
                            final MapleMap map = victim.getMap();
                            if (map.getId() == 180000000) {
                                player.dropMessage(1, "You cannot use VIP teleport rocks to teleport to GM maps.");
                                break;
                            }
                            if (
                                map.getId() < 2000000 ||
                                map.hasFieldLimit(MapleMap.FieldLimit.MYSTIC_DOOR_LIMIT) ||
                                map.hasFieldLimit(MapleMap.FieldLimit.MIGRATE_LIMIT) ||
                                map.isPQMap()
                            ) {
                                player.dropMessage(1, "The map you are trying to teleport to is forbidden.");
                                break;
                            }
                            if (isZipanguMap(map.getId()) && !isZipanguMap(player.getMapId())) {
                                player.setReturnMap(player.getMapId());
                            }
                            if (map.getId() / 10000000 == 27 && !canGoToTimeLaneMap(player, map.getId())) break;
                            if (!victim.isGM() || player.isGM()) { // Should really handle this before the switch
                                //if ((player.getMap().getMapName().equals(victim.getMap().getMapName()) && !vip) || vip) {
                                if (player.getPartyQuest() != null) {
                                    player.leaveParty();
                                }
                                player.changeMap(map, map.findClosestSpawnpoint(victim.getPosition()));
                                /*
                                System.out.print("Good player TP, map: " + map.getId() + " spawn: " + map.findClosestSpawnpoint(victim.getPosition()).getName());
                                } else {
                                    player.dropMessage(1, "You cannot warp to this player because they are not on the same continent.");
                                    break;
                                }
                                */
                            } else {
                                player.dropMessage(1, "You cannot use VIP teleport rocks on a GM.");
                                break;
                                //MapleInventoryManipulator.addById(c, 5041000, (short) 1);
                            }
                        }
                    }
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                    break;
                default:
                    System.err.println("Non-existent cash item was used, item ID: " + itemId);
            }
            c.getSession().write(MaplePacketCreator.enableActions());
        } catch (final RemoteException re) {
            c.getChannelServer().reconnectWorld();
            System.err.println("REMOTE ERROR: " + re);
        }
    }

    private int rand(final int lbound, final int ubound) {
        return MapleCharacter.rand(lbound, ubound);
    }

    public static boolean isZipanguMap(final int mapId) {
        return
            mapId / 1000000 == 800 ||
            mapId / 1000000 == 801 ||
            mapId == 809000101 ||
            mapId == 809000201;
    }

    public static boolean canGoToTimeLaneMap(final MapleCharacter player, final int mapId) {
        final int[] questIds = {12000, 12001, 12002};
        final boolean canGo = false;
        switch (mapId) {
            case 270010000:
                if (player.completedCQuest(questIds[0])) {
                    return true;
                }
                player.dropMessage(
                    1,
                    "You may not go to that map prior to completing The One Who Wants To Walk Down Memory Lane."
                );
                return canGo;
            case 270010500:
                if (player.completedCQuest(questIds[1])) {
                    return true;
                }
                player.dropMessage(1, "You may not go to that map prior to completing Regrets Run Rampant.");
                return canGo;
            case 270020500:
                if (player.completedCQuest(questIds[2])) {
                    return true;
                }
                player.dropMessage(1, "You may not go to that map prior to completing Onward Unto Oblivion.");
                return canGo;
            case 270030500:
                if (player.completedCQuest(questIds[2])) {
                    return true;
                }
                player.dropMessage(1, "You may not go to that map prior to completing Onward Unto Oblivion.");
                return canGo;
            case 270040000:
                if (player.completedCQuest(questIds[2])) {
                    return true;
                }
                player.dropMessage(1, "You may not go to that map prior to completing Onward Unto Oblivion.");
                return canGo;
            default:
                final int mapStage = (mapId / 10000) % 10;
                if (mapStage == 1) {
                    if (player.completedCQuest(questIds[0])) {
                        return true;
                    }
                    player.dropMessage(
                        1,
                        "You may not go to that map prior to completing The One Who Wants To Walk Down Memory Lane."
                    );
                    return canGo;
                } else if (mapStage == 2) {
                    if (player.completedCQuest(questIds[1])) {
                        return true;
                    }
                    player.dropMessage(1, "You may not go to that map prior to completing Regrets Run Rampant.");
                    return canGo;
                } else if (mapStage > 2) {
                    if (player.completedCQuest(questIds[2])) {
                        return true;
                    }
                    player.dropMessage(1, "You may not go to that map prior to completing Onward Unto Oblivion.");
                    return canGo;
                }
                return true;
        }
    }
}
