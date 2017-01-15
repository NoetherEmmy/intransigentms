package net.sf.odinms.tools;

import net.sf.odinms.client.*;
import net.sf.odinms.server.MapleItemInformationProvider;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DeathLogReader {
    private static DeathLogReader instance;
    private static File f;
    private static final Map<String, List<List<String>>> itemLogCache = new HashMap<>(3, 0.8f);

    private DeathLogReader(File file) {
        if (file == null) {
            throw new NullPointerException("Passing in null file to DeathLogReader");
        }
        f = file;
    }

    public static synchronized DeathLogReader getInstance() {
        if (instance != null) {
            return instance;
        }
        instance = new DeathLogReader(new File("death.log"));
        return instance;
    }

    public synchronized List<IItem> readDeathItems(String playerName) throws IOException, RuntimeException {
        return readDeathItems(playerName, 0, false);
    }

    public synchronized List<IItem> readDeathItems(String playerName, int offset, boolean useCache) throws IOException, RuntimeException {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset for readDeathItems may not be negative");
        }

        playerName = playerName.toLowerCase();
        List<String> deathItemList;
        if (useCache) {
            if (itemLogCache.containsKey(playerName)) {
                List<List<String>> deathItemLists = itemLogCache.get(playerName);
                if (offset < deathItemLists.size()) {
                    deathItemList = deathItemLists.get(deathItemLists.size() - 1 - offset);
                } else {
                    throw new IndexOutOfBoundsException(
                        "Offset of " +
                            offset +
                            " too large for number of cached logged deaths for player " +
                            MapleCharacterUtil.makeMapleReadable(playerName)
                    );
                }
            } else {
                throw new RuntimeException(
                    "No player with the name " +
                        MapleCharacterUtil.makeMapleReadable(playerName) +
                        " has their death items cached"
                );
            }
        } else {
            Scanner scanner = new Scanner(f);
            Pattern namePattern = Pattern.compile("(?i)Cleared items for " + playerName + " \\(Account: .*");
            Pattern itemPattern = Pattern.compile("[0-9]{7} [^;].*");
            List<List<String>> deathItemLists = new ArrayList<>(3);

            boolean gotName = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (!gotName) {
                    if (namePattern.matcher(line).matches()) {
                        gotName = true;
                        deathItemLists.add(new ArrayList<>());
                        scanner.nextLine();
                    }
                } else {
                    if (itemPattern.matcher(line).matches()) {
                        deathItemLists.get(deathItemLists.size() - 1).add(line);
                    } else {
                        gotName = false;
                    }
                }
            }

            itemLogCache.put(playerName, deathItemLists);
            if (offset < deathItemLists.size()) {
                deathItemList = deathItemLists.get(deathItemLists.size() - 1 - offset);
            } else {
                throw new IndexOutOfBoundsException(
                    "Offset of " +
                        offset +
                        " too large for number of logged deaths for player " +
                        MapleCharacterUtil.makeMapleReadable(playerName)
                );
            }
        }

        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Pattern statPattern = Pattern.compile("[a-zA-Z][^:]*: [0-9]+");
        return
            deathItemList
                .stream()
                .map(s -> {
                    int itemId = Integer.parseInt(s.substring(0, 7));

                    MapleInventoryType itemType = ii.getInventoryType(itemId);
                    Item item = null;
                    Equip equip = null;
                    if (
                        itemType.equals(MapleInventoryType.EQUIP) &&
                        !ii.isThrowingStar(itemId) &&
                        !ii.isBullet(itemId)
                    ) {
                        equip = (Equip) ii.getEquipById(itemId);
                    } else {
                        item = new Item(itemId, (byte) 0, (short) 1, -1);
                    }

                    String statsString = s.split(";")[1];
                    Matcher statMatcher = statPattern.matcher(statsString);
                    while (statMatcher.find()) {
                        String stat = statMatcher.group();
                        String[] statPair = stat.split(": ");

                        short value = Short.parseShort(statPair[1]);
                        switch (statPair[0]) {
                            case "Quantity":
                                if (item != null) {
                                    item.setQuantity(value);
                                } else {
                                    equip.setQuantity(value);
                                }
                                break;
                            case "Slots":
                                equip.setUpgradeSlots((byte) value);
                                break;
                            case "Accuracy":
                                equip.setAcc(value);
                                break;
                            case "Avoidability":
                                equip.setAvoid(value);
                                break;
                            case "Str":
                                equip.setStr(value);
                                break;
                            case "Dex":
                                equip.setDex(value);
                                break;
                            case "Int":
                                equip.setInt(value);
                                break;
                            case "Luk":
                                equip.setLuk(value);
                                break;
                            case "MaxHP":
                                equip.setHp(value);
                                break;
                            case "MaxMP":
                                equip.setMp(value);
                                break;
                            case "Jump":
                                equip.setJump(value);
                                break;
                            case "Speed":
                                equip.setSpeed(value);
                                break;
                            case "Attack":
                                equip.setWatk(value);
                                break;
                            case "Magic Attack":
                                equip.setMatk(value);
                                break;
                            case "W. def.":
                                equip.setWdef(value);
                                break;
                            case "M. def.":
                                equip.setMdef(value);
                                break;
                        }
                    }

                    if (item != null) {
                        return item;
                    } else {
                        return equip;
                    }
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public synchronized void clearCache() {
        itemLogCache.clear();
    }
}
