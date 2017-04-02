package net.sf.odinms.server;

import net.sf.odinms.client.*;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.provider.*;
import net.sf.odinms.tools.Pair;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class MapleItemInformationProvider {
    private static MapleItemInformationProvider instance;
    protected final MapleDataProvider itemData, equipData, stringData;
    protected final MapleData cashStringData, consumeStringData, eqpStringData,
                              etcStringData,  insStringData,     petStringData;
    protected final Map<Integer, MapleInventoryType> inventoryTypeCache = new HashMap<>();
    protected final Map<Integer, Short> slotMaxCache = new HashMap<>();
    protected final Map<Integer, MapleStatEffect> itemEffects = new HashMap<>();
    protected final Map<Integer, Map<String, Integer>> equipStatsCache = new HashMap<>();
    protected final Map<Integer, Boolean> cashCache = new HashMap<>();
    protected final Map<Integer, Equip> equipCache = new HashMap<>();
    protected final Map<Integer, Double> priceCache = new HashMap<>();
    protected final Map<Integer, Integer> wholePriceCache = new HashMap<>();
    protected final Map<Integer, Integer> projectileWatkCache = new HashMap<>();
    protected final Map<Integer, String> nameCache = new LinkedHashMap<>();
    protected final Map<Integer, String> descCache = new HashMap<>();
    protected final Map<Integer, String> msgCache = new HashMap<>();
    protected final Map<Integer, Boolean> dropRestrictionCache = new HashMap<>();
    protected final Map<Integer, Boolean> pickupRestrictionCache = new HashMap<>();
    protected final Map<Integer, Integer> getMesoCache = new HashMap<>();
    protected final Map<Integer, Integer> getExpCache = new HashMap<>();
    protected final Map<Integer, Boolean> isQuestItemCache = new HashMap<>();
    private static final Random rand = new Random();
    private static final List<List<Integer>> maleFaceCache = new ArrayList<>();
    private static final List<List<Integer>> femaleFaceCache = new ArrayList<>();
    private static final List<List<Integer>> ungenderedFaceCache = new ArrayList<>();
    private static final List<List<Integer>> maleHairCache = new ArrayList<>();
    private static final List<List<Integer>> femaleHairCache = new ArrayList<>();
    private static final List<List<Integer>> ungenderedHairCache = new ArrayList<>();
    private static boolean facesCached = false;
    private static boolean hairsCached = false;
    private static final Map<String, Integer> cashEquips = new LinkedHashMap<>();
    private static boolean cashEquipsCached = false;
    private boolean namesCached = false;
    private static final List<Integer> chairCache = new ArrayList<>();
    private static boolean chairsCached = false;

    /** Creates a new instance of MapleItemInformationProvider */
    protected MapleItemInformationProvider() {
        itemData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Item.wz"));
        equipData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Character.wz"));
        stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz"));
        cashStringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")).getData("Cash.img");
        consumeStringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")).getData("Consume.img");
        eqpStringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")).getData("Eqp.img");
        etcStringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")).getData("Etc.img");
        insStringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")).getData("Ins.img");
        petStringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")).getData("Pet.img");
    }

    public static MapleItemInformationProvider getInstance() {
        if (instance == null) instance = new MapleItemInformationProvider();
        return instance;
    }

    /** Returns the inventory type for the specified item ID. */
    public MapleInventoryType getInventoryType(int itemId) {
        if (inventoryTypeCache.containsKey(itemId)) return inventoryTypeCache.get(itemId);
        MapleInventoryType ret;
        String idStr = "0" + String.valueOf(itemId);
        // First look in items
        MapleDataDirectoryEntry root = itemData.getRoot();
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            // We should have .img files here beginning with the first 4 IID
            for (MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
                    ret = MapleInventoryType.getByWZName(topDir.getName());
                    inventoryTypeCache.put(itemId, ret);
                    return ret;
                } else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
                    ret = MapleInventoryType.getByWZName(topDir.getName());
                    inventoryTypeCache.put(itemId, ret);
                    return ret;
                }
            }
        }
        // Not found? Maybe it's an equip
        root = equipData.getRoot();
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            for (MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr + ".img")) {
                    ret = MapleInventoryType.EQUIP;
                    inventoryTypeCache.put(itemId, ret);
                    return ret;
                }
            }
        }
        ret = MapleInventoryType.UNDEFINED;
        inventoryTypeCache.put(itemId, ret);
        return ret;
    }

    public List<Pair<Integer, String>> getAllConsume() {
        return getAllConsume('\0');
    }

    public List<Pair<Integer, String>> getAllConsume(char initial) {
        List<Pair<Integer, String>> itemPairs = new ArrayList<>();
        MapleData itemsData;

        itemsData = stringData.getData("Consume.img");

        if (Character.isLetter(initial)) {
            initial = Character.toUpperCase(initial);
            for (MapleData itemFolder : itemsData.getChildren()) {
                boolean hasIcon = false;
                String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                if (Character.toUpperCase(itemName.charAt(0)) == initial) {
                    int itemId = Integer.parseInt(itemFolder.getName());
                    MapleData item = getItemData(itemId);
                    if (item == null) continue;
                    MapleData info = item.getChildByPath("info");
                    if (info == null) continue;
                    for (MapleData data : info.getChildren()) {
                        if (data.getName().equalsIgnoreCase("icon")) {
                            hasIcon = true;
                            break;
                        }
                    }
                    if (hasIcon) itemPairs.add(new Pair<>(itemId, itemName));
                }
            }
        } else if (initial == '#') {
            for (MapleData itemFolder : itemsData.getChildren()) {
                boolean hasicon = false;
                String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                if (!Character.isLetter(itemName.charAt(0))) {
                    int itemId = Integer.parseInt(itemFolder.getName());
                    MapleData item = getItemData(itemId);
                    if (item == null) continue;
                    MapleData info = item.getChildByPath("info");
                    if (info == null) continue;
                    for (MapleData data : info.getChildren()) {
                        if (data.getName().equalsIgnoreCase("icon")) {
                            hasicon = true;
                            break;
                        }
                    }
                    if (hasicon) itemPairs.add(new Pair<>(itemId, itemName));
                }
            }
        } else {
            for (MapleData itemFolder : itemsData.getChildren()) {
                boolean hasicon = false;
                String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                int itemId = Integer.parseInt(itemFolder.getName());
                MapleData item = getItemData(itemId);
                if (item == null) continue;
                MapleData info = item.getChildByPath("info");
                if (info == null) continue;
                for (MapleData data : info.getChildren()) {
                    if (data.getName().equalsIgnoreCase("icon")) {
                        hasicon = true;
                        break;
                    }
                }
                if (hasicon) itemPairs.add(new Pair<>(itemId, itemName));
            }
        }
        return itemPairs;
    }

    public Pair<Integer, String> getConsumeByName(String searchstring) {
        Pair<Integer, String> ret = null;
        MapleData itemsData;

        itemsData = stringData.getData("Consume.img");

        if (searchstring.isEmpty()) return null;
        searchstring = searchstring.toUpperCase();
        for (MapleData itemFolder : itemsData.getChildren()) {
            boolean hasicon = false;
            String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
            if (itemName.toUpperCase().startsWith(searchstring)) {
                int itemId = Integer.parseInt(itemFolder.getName());
                MapleData item = getItemData(itemId);
                if (item == null) continue;
                MapleData info = item.getChildByPath("info");
                if (info == null) continue;
                for (MapleData data : info.getChildren()) {
                    if (data.getName().equalsIgnoreCase("icon")) {
                        hasicon = true;
                        break;
                    }
                }
                if (hasicon) {
                    if (ret == null) {
                        ret = new Pair<>(itemId, itemName);
                    } else if (ret.getRight().length() > itemName.length()) {
                        ret = new Pair<>(itemId, itemName);
                    }
                }
            }
        }

        return ret;
    }

    public List<Pair<Integer, String>> getAllEqp() {
        return getAllEqp('\0');
    }

    public List<Pair<Integer, String>> getAllEqp(char initial) {
        List<Pair<Integer, String>> itemPairs = new ArrayList<>();
        MapleData itemsData;

        itemsData = stringData.getData("Eqp.img").getChildByPath("Eqp");
        if (Character.isLetter(initial)) {
            initial = Character.toUpperCase(initial);
            for (MapleData eqpType : itemsData.getChildren()) {
                for (MapleData itemFolder : eqpType.getChildren()) {
                    boolean hasicon = false;
                    String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                    if (Character.toUpperCase(itemName.charAt(0)) == initial) {
                        int itemId = Integer.parseInt(itemFolder.getName());
                        MapleData item = getItemData(itemId);
                        if (item == null) continue;
                        MapleData info = item.getChildByPath("info");
                        if (info == null) continue;
                        for (MapleData data : info.getChildren()) {
                            if (data.getName().equalsIgnoreCase("icon")) {
                                hasicon = true;
                                break;
                            }
                        }
                        if (hasicon) itemPairs.add(new Pair<>(itemId, itemName));
                    }
                }
            }
        } else if (initial == '#') {
            for (MapleData eqpType : itemsData.getChildren()) {
                for (MapleData itemFolder : eqpType.getChildren()) {
                    boolean hasicon = false;
                    String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                    if (!Character.isLetter(itemName.charAt(0))) {
                        int itemId = Integer.parseInt(itemFolder.getName());
                        MapleData item = getItemData(itemId);
                        if (item == null) continue;
                        MapleData info = item.getChildByPath("info");
                        if (info == null) continue;
                        for (MapleData data : info.getChildren()) {
                            if (data.getName().equalsIgnoreCase("icon")) {
                                hasicon = true;
                                break;
                            }
                        }
                        if (hasicon) itemPairs.add(new Pair<>(itemId, itemName));
                    }
                }
            }
        } else {
            for (MapleData eqpType : itemsData.getChildren()) {
                for (MapleData itemFolder : eqpType.getChildren()) {
                    boolean hasicon = false;
                    String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                    int itemId = Integer.parseInt(itemFolder.getName());
                    MapleData item = getItemData(itemId);
                    if (item == null) continue;
                    MapleData info = item.getChildByPath("info");
                    if (info == null) continue;
                    for (MapleData data : info.getChildren()) {
                        if (data.getName().equalsIgnoreCase("icon")) {
                            hasicon = true;
                            break;
                        }
                    }
                    if (hasicon) itemPairs.add(new Pair<>(itemId, itemName));
                }
            }
        }
        return itemPairs;
    }

    public Pair<Integer, String> getEqpByName(String searchstring) {
        Pair<Integer, String> ret = null;
        MapleData itemsData;

        itemsData = stringData.getData("Eqp.img").getChildByPath("Eqp");

        if (searchstring.isEmpty()) return null;
        searchstring = searchstring.toUpperCase();
        for (MapleData eqpType : itemsData.getChildren()) {
            for (MapleData itemFolder : eqpType.getChildren()) {
                boolean hasicon = false;
                String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                if (itemName.toUpperCase().startsWith(searchstring)) {
                    int itemId = Integer.parseInt(itemFolder.getName());
                    MapleData item = getItemData(itemId);
                    if (item == null) continue;
                    MapleData info = item.getChildByPath("info");
                    if (info == null) continue;
                    for (MapleData data : info.getChildren()) {
                        if (data.getName().equalsIgnoreCase("icon")) {
                            hasicon = true;
                            break;
                        }
                    }
                    if (hasicon) {
                        if (ret == null) {
                            ret = new Pair<>(itemId, itemName);
                        } else if (ret.getRight().length() > itemName.length()) {
                            ret = new Pair<>(itemId, itemName);
                        }
                    }
                }
            }
        }
        return ret;
    }

    public List<Pair<Integer, String>> getAllEtc() {
        return getAllEtc('\0');
    }

    public List<Pair<Integer, String>> getAllEtc(char initial) {
        List<Pair<Integer, String>> itemPairs = new ArrayList<>();
        MapleData itemsData;

        itemsData = stringData.getData("Etc.img").getChildByPath("Etc");
        if (Character.isLetter(initial)) {
            initial = Character.toUpperCase(initial);
            for (MapleData itemFolder : itemsData.getChildren()) {
                boolean hasicon = false;
                String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                if (Character.toUpperCase(itemName.charAt(0)) == initial) {
                    int itemId = Integer.parseInt(itemFolder.getName());
                    MapleData item = getItemData(itemId);
                    if (item == null) continue;
                    MapleData info = item.getChildByPath("info");
                    if (info == null) continue;
                    for (MapleData data : info.getChildren()) {
                        if (data.getName().equalsIgnoreCase("icon")) {
                            hasicon = true;
                            break;
                        }
                    }
                    if (hasicon) itemPairs.add(new Pair<>(itemId, itemName));
                }
            }
        } else if (initial == '#') {
            for (MapleData itemFolder : itemsData.getChildren()) {
                boolean hasicon = false;
                String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                if (!Character.isLetter(itemName.charAt(0))) {
                    int itemId = Integer.parseInt(itemFolder.getName());
                    MapleData item = getItemData(itemId);
                    if (item == null) continue;
                    MapleData info = item.getChildByPath("info");
                    if (info == null) continue;
                    for (MapleData data : info.getChildren()) {
                        if (data.getName().equalsIgnoreCase("icon")) {
                            hasicon = true;
                            break;
                        }
                    }
                    if (hasicon) itemPairs.add(new Pair<>(itemId, itemName));
                }
            }
        } else {
            for (MapleData itemFolder : itemsData.getChildren()) {
                boolean hasicon = false;
                String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                int itemId = Integer.parseInt(itemFolder.getName());
                MapleData item = getItemData(itemId);
                if (item == null) continue;
                MapleData info = item.getChildByPath("info");
                if (info == null) continue;
                for (MapleData data : info.getChildren()) {
                    if (data.getName().equalsIgnoreCase("icon")) {
                        hasicon = true;
                        break;
                    }
                }
                if (hasicon) itemPairs.add(new Pair<>(itemId, itemName));
            }
        }
        return itemPairs;
    }

    public Pair<Integer, String> getEtcByName(String searchstring) {
        Pair<Integer, String> ret = null;
        MapleData itemsData;

        itemsData = stringData.getData("Etc.img").getChildByPath("Etc");

        if (searchstring.isEmpty()) return null;
        searchstring = searchstring.toUpperCase();
        for (MapleData itemFolder : itemsData.getChildren()) {
            boolean hasIcon = false;
            String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
            if (itemName.toUpperCase().startsWith(searchstring)) {
                int itemId = Integer.parseInt(itemFolder.getName());
                MapleData item = getItemData(itemId);
                if (item == null) continue;
                MapleData info = item.getChildByPath("info");
                if (info == null) continue;
                for (MapleData data : info.getChildren()) {
                    if (data.getName().equalsIgnoreCase("icon")) {
                        hasIcon = true;
                        break;
                    }
                }
                if (hasIcon) {
                    if (ret == null) {
                        ret = new Pair<>(itemId, itemName);
                    } else if (ret.getRight().length() > itemName.length()) {
                        ret = new Pair<>(itemId, itemName);
                    }
                }
            }
        }

        return ret;
    }

    public Map<Integer, String> getAllItems() {
        if (namesCached) return nameCache;

        MapleData itemsData;

        itemsData = stringData.getData("Cash.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            int itemId = Integer.parseInt(itemFolder.getName());
            String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
            nameCache.put(itemId, itemName);
        }

        itemsData = stringData.getData("Consume.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            int itemId = Integer.parseInt(itemFolder.getName());
            String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
            nameCache.put(itemId, itemName);
        }

        itemsData = stringData.getData("Eqp.img").getChildByPath("Eqp");
        for (MapleData eqpType : itemsData.getChildren()) {
            for (MapleData itemFolder : eqpType.getChildren()) {
                int itemId = Integer.parseInt(itemFolder.getName());
                String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
                nameCache.put(itemId, itemName);
            }
        }

        itemsData = stringData.getData("Etc.img").getChildByPath("Etc");
        for (MapleData itemFolder : itemsData.getChildren()) {
            int itemId = Integer.parseInt(itemFolder.getName());
            String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
            nameCache.put(itemId, itemName);
        }

        itemsData = stringData.getData("Ins.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            int itemId = Integer.parseInt(itemFolder.getName());
            String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
            nameCache.put(itemId, itemName);
        }

        itemsData = stringData.getData("Pet.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            int itemId = Integer.parseInt(itemFolder.getName());
            String itemName = MapleDataTool.getString("name", itemFolder, "NO-NAME");
            nameCache.put(itemId, itemName);
        }

        namesCached = true;
        return nameCache;
    }

    protected MapleData getStringData(int itemId) {
        String cat = "null";
        MapleData theData;
        if (itemId >= 5010000) {
            theData = cashStringData;
        } else if (itemId >= 2000000 && itemId < 3000000) {
            theData = consumeStringData;
        } else if (itemId >= 1010000 && itemId < 1040000 || itemId >= 1122000 && itemId < 1123000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Accessory";
        } else if (itemId >= 1000000 && itemId < 1010000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Cap";
        } else if (itemId >= 1102000 && itemId < 1103000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Cape";
        } else if (itemId >= 1040000 && itemId < 1050000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Coat";
        } else if (itemId >= 20000 && itemId < 22000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Face";
        } else if (itemId >= 1080000 && itemId < 1090000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Glove";
        } else if (itemId >= 30000 && itemId < 32000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Hair";
        } else if (itemId >= 1050000 && itemId < 1060000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Longcoat";
        } else if (itemId >= 1060000 && itemId < 1070000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Pants";
        } else if (itemId >= 1802000 && itemId < 1810000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "PetEquip";
        } else if (itemId >= 1112000 && itemId < 1120000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Ring";
        } else if (itemId >= 1092000 && itemId < 1100000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Shield";
        } else if (itemId >= 1070000 && itemId < 1080000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Shoes";
        } else if (itemId >= 1900000 && itemId < 2000000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Taming";
        } else if (itemId >= 1300000 && itemId < 1800000) {
            theData = eqpStringData.getChildByPath("Eqp");
            cat = "Weapon";
        } else if (itemId == 4280000 || itemId == 4280001) {
            theData = etcStringData;
        } else if (itemId >= 4000000 && itemId < 5000000) {
            theData = etcStringData.getChildByPath("Etc");
        } else if (itemId >= 3000000 && itemId < 4000000) {
            theData = insStringData;
        } else if (itemId >= 5000000 && itemId < 5010000) {
            theData = petStringData;
        } else {
            return null;
        }
        if (cat.equalsIgnoreCase("null")) {
            return theData.getChildByPath(String.valueOf(itemId));
        } else {
            return theData.getChildByPath(cat + "/" + itemId);
        }
    }

    protected MapleData getItemData(int itemId) {
        MapleData ret;
        String idStr = "0" + String.valueOf(itemId);
        MapleDataDirectoryEntry root = itemData.getRoot();
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            // We should have .img files here beginning with the first 4 IID
            for (MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
                    ret = itemData.getData(topDir.getName() + "/" + iFile.getName());
                    if (ret == null) return null;
                    ret = ret.getChildByPath(idStr);
                    return ret;
                } else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
                    return itemData.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        root = equipData.getRoot();
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            for (MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr + ".img")) {
                    return equipData.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        return null;
    }

    public List<Integer> getChairIds() {
        if (!chairsCached) {
            MapleDataDirectoryEntry root = itemData.getRoot();
            for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
                if (!topDir.getName().equals("Install")) continue;
                for (MapleDataFileEntry iFile : topDir.getFiles()) {
                    if (!iFile.getName().contains("301")) continue;
                    MapleData chairData = itemData.getData(topDir.getName() + "/" + iFile.getName());
                    chairData
                        .getChildren()
                        .stream()
                        .map(child -> Integer.parseInt(child.getName()))
                        .sorted()
                        .forEachOrdered(chairCache::add);
                }
            }
            chairsCached = true;
        }
        return chairCache;
    }

    /** Called by Coco NPC the first time someone starts a conversation with them. */
    public void cacheFaceData() {
        if (!facesCached) {
            MapleDataDirectoryEntry root = equipData.getRoot();
            for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
                if (topDir.getName().equals("Face")) {
                    topDir.getFiles().stream().map(MapleDataEntry::getName).sorted().forEachOrdered((iFile) -> {
                        final Integer id = Integer.parseInt(iFile.substring(3, 8)); // Gets just the ID w/o leading zeroes.
                        List<List<Integer>> faceLists;
                        if (id >= 20000 && id < 30000) { // Just in case.
                            if ((id / 1000) % 10 == 0) {
                                faceLists = maleFaceCache;
                            } else if ((id / 1000) % 10 == 1) {
                                faceLists = femaleFaceCache;
                            } else {
                                faceLists = ungenderedFaceCache;
                            }
                            List<Integer> faceList = null;
                            if (!faceLists.isEmpty()) {
                                faceList = faceLists.get(faceLists.size() - 1);
                            }
                            // Match condition is that all decimal places EXCEPT for the hundreds place match.
                            if (
                                faceList != null &&
                                (
                                    faceList.size() < 300 ||
                                    faceList.stream().anyMatch(x ->
                                        Integer.valueOf(x - (x % 1000 / 100 * 100))
                                               .equals(id - (id % 1000 / 100 * 100))
                                    )
                                )
                            ) {
                                faceList.add(id);
                            } else {
                                List<Integer> newFaceList = new ArrayList<>();
                                newFaceList.add(id);
                                faceLists.add(newFaceList);
                            }
                        }
                    });
                    break;
                }
            }
            facesCached = true;
        }
    }

    /** Called by Coco NPC the first time someone starts a conversation with them. */
    public void cacheHairData() {
        if (!hairsCached) {
            MapleDataDirectoryEntry root = equipData.getRoot();
            for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
                if (topDir.getName().equals("Hair")) {
                    topDir.getFiles().stream().map(MapleDataEntry::getName).sorted().forEachOrdered((iFile) -> {
                        final Integer id = Integer.parseInt(iFile.substring(3, 8)); // Gets just the ID w/o leading zeroes.
                        List<List<Integer>> hairLists;
                        if (id >= 30000 && id < 40000) { // Filtering out IDs that cannot be rendered by the client.
                            if ((id / 1000) % 10 == 0) {
                                hairLists = maleHairCache;
                            } else if ((id / 1000) % 10 == 1) {
                                hairLists = femaleHairCache;
                            } else {
                                hairLists = ungenderedHairCache;
                            }
                            List<Integer> hairList = null;
                            if (!hairLists.isEmpty()) {
                                hairList = hairLists.get(hairLists.size() - 1);
                            }
                            if (
                                hairList != null &&
                                (
                                    hairList.size() < 300 ||
                                    hairList.stream().anyMatch(x -> Integer.valueOf(x / 10).equals(id / 10))
                                )
                            ) {
                                hairList.add(id);
                            } else {
                                List<Integer> newHairList = new ArrayList<>();
                                newHairList.add(id);
                                hairLists.add(newHairList);
                            }
                        }
                    });
                    break;
                }
            }
            hairsCached = true;
        }
    }

    public List<List<Integer>> getFaceData(int gender) {
        switch (gender) {
            case 0:
                return maleFaceCache;
            case 1:
                return femaleFaceCache;
            default:
                return ungenderedFaceCache;
        }
    }

    public List<List<Integer>> getHairData(int gender) {
        switch (gender) {
            case 0:
                return maleHairCache;
            case 1:
                return femaleHairCache;
            default:
                return ungenderedHairCache;
        }
    }

    public List<Integer> getFaceData(int gender, int index) {
        switch (gender) {
            case 0:
                return maleFaceCache.get(index);
            case 1:
                return femaleFaceCache.get(index);
            default:
                return ungenderedFaceCache.get(index);
        }
    }

    public List<Integer> getHairData(int gender, int index) {
        switch (gender) {
            case 0:
                return maleHairCache.get(index);
            case 1:
                return femaleHairCache.get(index);
            default:
                return ungenderedHairCache.get(index);
        }
    }

    public int getFaceListCount(int gender) {
        switch (gender) {
            case 0:
                return maleFaceCache.size();
            case 1:
                return femaleFaceCache.size();
            default:
                return ungenderedFaceCache.size();
        }
    }

    public int getHairListCount(int gender) {
        switch (gender) {
            case 0:
                return maleHairCache.size();
            case 1:
                return femaleHairCache.size();
            default:
                return ungenderedHairCache.size();
        }
    }

    public List<Integer> getAllColors(int id) {
        List<Integer> ret = new ArrayList<>();
        List<List<Integer>> cache;
        if (id >= 20000 && id < 30000) { // Face
            final int match = id - (id % 1000 / 100 * 100); // Hundreds place set to zero.
            switch ((id / 1000) % 10) {
                case 0:
                    cache = maleFaceCache;
                    break;
                case 1:
                    cache = femaleFaceCache;
                    break;
                default:
                    cache = ungenderedFaceCache;
            }
            for (List<Integer> faceList : cache) {
                if (faceList.contains(id)) {
                    ret.addAll(
                        faceList.stream()
                                .filter(x ->
                                    Integer.valueOf(x - (x % 1000 / 100 * 100)).equals(match)
                                )
                                .collect(Collectors.toCollection(ArrayList::new))
                    );
                    break;
                }
            }
        } else { // Hair
            final int match = id / 10;
            switch ((id / 1000) % 10) {
                case 0:
                    cache = maleHairCache;
                    break;
                case 1:
                    cache = femaleHairCache;
                    break;
                default:
                    cache = ungenderedHairCache;
            }
            for (List<Integer> hairList : cache) {
                if (hairList.contains(id)) {
                    ret.addAll(
                        hairList.stream()
                                .filter(x ->
                                    Integer.valueOf(x / 10).equals(match)
                                )
                                .collect(Collectors.toCollection(ArrayList::new))
                    );
                    break;
                }
            }
        }
        return ret;
    }

    /** Returns the maximum of items in one slot. */
    public short getSlotMax(MapleClient c, int itemId) {
        if (slotMaxCache.containsKey(itemId)) return slotMaxCache.get(itemId);
        short ret = 0;
        MapleData item = getItemData(itemId);
        if (item != null) {
            MapleData smEntry = item.getChildByPath("info/slotMax");
            if (smEntry == null) {
                if (getInventoryType(itemId).getType() == MapleInventoryType.EQUIP.getType()) {
                    ret = 1;
                } else {
                    ret = 100;
                }
            } else {
                /*
                if (isThrowingStar(itemId) || isBullet(itemId) || (MapleDataTool.getInt(smEntry) == 0)) {
                    ret = 1;
                }
                */
                ret = (short) MapleDataTool.getInt(smEntry);
                if (isThrowingStar(itemId)) {
                    ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(4100000)) * 10;
                } else {
                    ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(5200000)) * 10;
                }

            }
        }
        if (!isThrowingStar(itemId) && !isBullet(itemId)) slotMaxCache.put(itemId, ret);
        return ret;
    }

    public int getMeso(int itemId) {
        if (getMesoCache.containsKey(itemId)) return getMesoCache.get(itemId);
        MapleData item = getItemData(itemId);
        if (item == null) return -1;
        int pEntry;
        MapleData pData = item.getChildByPath("info/meso");
        if (pData == null) return -1;
        pEntry = MapleDataTool.getInt(pData);

        getMesoCache.put(itemId, pEntry);
        return pEntry;
    }

    public int getExpCache(int itemId) {
        if (getExpCache.containsKey(itemId)) return getExpCache.get(itemId);
        MapleData item = getItemData(itemId);
        if (item == null) return 0;
        int pEntry;
        MapleData pData = item.getChildByPath("spec/exp");
        if (pData == null) return 0;
        pEntry = MapleDataTool.getInt(pData);

        getExpCache.put(itemId, pEntry);
        return pEntry;
    }

    public int getWholePrice(int itemId) {
        if (wholePriceCache.containsKey(itemId)) return wholePriceCache.get(itemId);
        MapleData item = getItemData(itemId);
        if (item == null) return -1;

        int pEntry;
        MapleData pData = item.getChildByPath("info/price");
        if (pData == null) return -1;
        pEntry = MapleDataTool.getInt(pData);

        wholePriceCache.put(itemId, pEntry);
        return pEntry;
    }

    public double getPrice(int itemId) {
        if (priceCache.containsKey(itemId)) return priceCache.get(itemId);
        MapleData item = getItemData(itemId);
        if (item == null) return -1.0d;

        double pEntry;
        MapleData pData = item.getChildByPath("info/unitPrice");
        if (pData != null) {
            try {
                pEntry = MapleDataTool.getDouble(pData);
            } catch (Exception e) {
                pEntry = (double) MapleDataTool.getInt(pData);
            }
        } else {
            pData = item.getChildByPath("info/price");
            if (pData == null) {
                return -1.0d;
            }
            pEntry = (double) MapleDataTool.getInt(pData);
        }

        priceCache.put(itemId, pEntry);
        return pEntry;
    }

    protected Map<String, Integer> getEquipStats(int itemId) {
        if (equipStatsCache.containsKey(itemId)) return equipStatsCache.get(itemId);
        Map<String, Integer> ret = new LinkedHashMap<>();
        MapleData item = getItemData(itemId);
        if (item == null) return null;
        MapleData info = item.getChildByPath("info");
        if (info == null) return null;
        for (MapleData data : info.getChildren()) {
            if (data.getName().startsWith("inc")) {
                ret.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
            }
        }
        ret.put("tuc", MapleDataTool.getInt("tuc", info, 0));
        ret.put("reqLevel", MapleDataTool.getInt("reqLevel", info, 0));
        ret.put("cursed", MapleDataTool.getInt("cursed", info, 0));
        ret.put("success", MapleDataTool.getInt("success", info, 0));
        equipStatsCache.put(itemId, ret);
        return ret;
    }

    public boolean isCash(int itemId) {
        if (cashCache.containsKey(itemId)) return cashCache.get(itemId);
        MapleData item = getItemData(itemId);
        MapleData info = item.getChildByPath("info");
        if (info == null) {
            cashCache.put(itemId, false);
            return false;
        }
        boolean cash = MapleDataTool.getInt("cash", info, 0) == 1;
        cashCache.put(itemId, cash);
        return cash;
    }

    public void cacheCashEquips() {
        if (!cashEquipsCached) {
            final List<String> excludes = Arrays.asList("Afterimage", "TamingMob", "Face", "Hair", "PetEquip");
            MapleDataDirectoryEntry root = equipData.getRoot();
            root.getSubdirectories().forEach((topDir) -> {
                if (!excludes.contains(topDir.getName())) {
                    topDir.getFiles().forEach((iFile) -> {
                        int id = Integer.parseInt(iFile.getName().substring(1, 8));
                        if (isCash(id)) {
                            cashEquips.put(getName(id), id);
                        }
                    });
                }
            });
            cashEquipsCached = true;
        }
    }

    public List<Map.Entry<String, Integer>> getCashEquipEntries(final int type) {
        return
            cashEquips
                .entrySet()
                .stream()
                .filter(e -> e.getValue() / 10000 == type)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public boolean cashEquipExists(int id) {
        return cashEquips.containsValue(id);
    }

    public int singleCashEquipSearch(String query) {
        if (query.length() < 1) return 0;
        query = query.toUpperCase();
        Map.Entry<String, Integer> bestMatch = null;
        for (Map.Entry<String, Integer> e : cashEquips.entrySet()) {
            if (e.getKey() == null) continue;
            String newKey = e.getKey().toUpperCase();
            String bestKey = bestMatch == null ? null : bestMatch.getKey().toUpperCase();
            if ((bestKey == null &&  newKey.contains(query))
                                 || (bestKey != null && newKey.contains(query)
                                 && (newKey.indexOf(query) < bestKey.indexOf(query)
                                 || (newKey.indexOf(query) == bestKey.indexOf(query)
                                 &&  newKey.length() < bestKey.length())))) {
                bestMatch = e;
            }
        }
        return bestMatch == null || !bestMatch.getKey().toUpperCase().contains(query) ? 0 : bestMatch.getValue();
    }

    public List<Integer> cashEquipSearch(String query) {
        if (query.length() < 1) return Collections.emptyList();
        query = query.toUpperCase();
        final List<String> splitQuery = Arrays.stream(query.split("\\s+"))
                                              .distinct()
                                              .collect(Collectors.toCollection(ArrayList::new));
        splitQuery.remove("");
        if (splitQuery.isEmpty()) return Collections.emptyList();
        return cashEquips
                .entrySet()
                .stream()
                .filter(e -> {
                    if (e.getKey() == null || e.getValue() == null) return false;
                    String name = e.getKey().toUpperCase();
                    for (String token : splitQuery) {
                        if (name.contains(token)) return true;
                    }
                    return false;
                })
                .sorted((e1, e2) -> {
                    String name1 = e1.getKey().toUpperCase();
                    String name2 = e2.getKey().toUpperCase();
                    if (name1.equals(name2)) return 0;
                    int tokenCount1 = 0;
                    int tokenCount2 = 0;
                    int combinedIndices1 = 0;
                    int combinedIndices2 = 0;
                    for (String token : splitQuery) {
                        if (name1.contains(token)) {
                            tokenCount1++;
                            combinedIndices1 += name1.indexOf(token);
                        }
                        if (name2.contains(token)) {
                            tokenCount2++;
                            combinedIndices2 += name2.indexOf(token);
                        }
                    }
                    if (tokenCount1 != tokenCount2) return tokenCount2 - tokenCount1;
                    if (combinedIndices1 != combinedIndices2) return combinedIndices1 - combinedIndices2;
                    if (name1.length() != name2.length()) return name1.length() - name2.length();
                    return name1.compareTo(name2);
                })
                .map(Entry::getValue)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<Integer> cashEquipsByType(final int type) {
        return cashEquips.values().stream().filter((id) -> {
            int t = id / 10000;
            return t == type;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<Integer> cashEquipsByType(final int lowerType, final int upperType) {
        return cashEquips.values().stream().filter((id) -> {
            int type = id / 10000;
            return type >= lowerType && type <= upperType;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    public int getReqLevel(int itemId) {
        final Integer req = getEquipStats(itemId).get("reqLevel");
        return req == null ? 0 : req;
    }

    public List<Integer> getScrollReqs(int itemId) {
        List<Integer> ret = new ArrayList<>();
        MapleData data = getItemData(itemId);
        data = data.getChildByPath("req");
        if (data == null) return ret;
        for (MapleData req : data.getChildren()) {
            ret.add(MapleDataTool.getInt(req));
        }
        return ret;
    }

    public boolean isWeapon(int itemId) {
        return itemId >= 1302000 && itemId < 1492024;
    }

    public MapleWeaponType getWeaponType(int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        switch (cat) {
            case 30:
                return MapleWeaponType.SWORD1H;
            case 31:
                return MapleWeaponType.AXE1H;
            case 32:
                return MapleWeaponType.BLUNT1H;
            case 33:
                return MapleWeaponType.DAGGER;
            case 37:
                return MapleWeaponType.WAND;
            case 38:
                return MapleWeaponType.STAFF;
            case 40:
                return MapleWeaponType.SWORD2H;
            case 41:
                return MapleWeaponType.AXE2H;
            case 42:
                return MapleWeaponType.BLUNT2H;
            case 43:
                return MapleWeaponType.SPEAR;
            case 44:
                return MapleWeaponType.POLE_ARM;
            case 45:
                return MapleWeaponType.BOW;
            case 46:
                return MapleWeaponType.CROSSBOW;
            case 47:
                return MapleWeaponType.CLAW;
            case 48:
                return MapleWeaponType.KNUCKLE;
            case 49:
                return MapleWeaponType.GUN;
        }
        return MapleWeaponType.NOT_A_WEAPON;
    }

    public boolean isShield(int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        return cat == 9;
    }

    public boolean isEquip(int itemId) {
        return itemId / 1000000 == 1;
    }

    public boolean isCleanSlate(int scrollId) {
        switch (scrollId) {
            case 2049000:
            case 2049001:
            case 2049002:
            case 2049003:
                return true;
        }
        return false;

    }

    public IItem scrollEquipWithId(MapleClient c, IItem equip, int scrollId, boolean usingWhiteScroll) {
        boolean noFail = false;
        if (c.getPlayer().haveItem(2022118, 1, false, true)) noFail = true;

        boolean isGM = false;
        if (c.getPlayer().isGM()) isGM = true;

        if (equip instanceof Equip) {
            Equip nEquip = (Equip) equip;
            Map<String, Integer> stats = getEquipStats(scrollId);
            Map<String, Integer> eqstats = getEquipStats(equip.getItemId());
            if (
                (nEquip.getUpgradeSlots() > 0 || isCleanSlate(scrollId) || scrollId == 2049004) &&
                Math.ceil(Math.random() * 100.0d) <= stats.get("success") ||
                isGM ||
                noFail
            ) {
                switch (scrollId) {
                    case 2049000:
                    case 2049001:
                    case 2049002:
                    case 2049003:
                        if (nEquip.getLevel() + nEquip.getUpgradeSlots() < eqstats.get("tuc")) {
                            byte newSlots = (byte) (nEquip.getUpgradeSlots() + 1);
                            nEquip.setUpgradeSlots(newSlots);
                        }
                        break;
                    case 2049100:
                    case 2049101:
                    case 2049102:
                        int increase = Math.ceil(Math.random() * 100.0d) <= 50 ? -1 : 1;
                        if (nEquip.getStr() > 0) {
                            short newStat = (short) (nEquip.getStr() + Math.ceil(Math.random() * 5.0d) * increase);
                            nEquip.setStr(newStat);
                        }
                        if (nEquip.getDex() > 0) {
                            short newStat = (short) (nEquip.getDex() + Math.ceil(Math.random() * 5.0d) * increase);
                            nEquip.setDex(newStat);
                        }
                        if (nEquip.getInt() > 0) {
                            short newStat = (short) (nEquip.getInt() + Math.ceil(Math.random() * 5.0d) * increase);
                            nEquip.setInt(newStat);
                        }
                        if (nEquip.getLuk() > 0) {
                            short newStat = (short) (nEquip.getLuk() + Math.ceil(Math.random() * 5.0d) * increase);
                            nEquip.setLuk(newStat);
                        }
                        if (nEquip.getWatk() > 0) {
                            short newStat = (short) (nEquip.getWatk() + Math.ceil(Math.random() * 5.0d) * increase);
                            nEquip.setWatk(newStat);
                        }
                        if (nEquip.getWdef() > 0) {
                            short newStat = (short) (nEquip.getWdef() + Math.ceil(Math.random() * 50.0d) * increase);
                            nEquip.setWdef(newStat);
                        }
                        if (nEquip.getMatk() > 0) {
                            short newStat = (short) (nEquip.getMatk() + Math.ceil(Math.random() * 5.0d) * increase);
                            nEquip.setMatk(newStat);
                        }
                        if (nEquip.getMdef() > 0) {
                            short newStat = (short) (nEquip.getMdef() + Math.ceil(Math.random() * 50.0d) * increase);
                            nEquip.setMdef(newStat);
                        }
                        if (nEquip.getAcc() > 0) {
                            short newStat = (short) (nEquip.getAcc() + Math.ceil(Math.random() * 5.0d) * increase);
                            nEquip.setAcc(newStat);
                        }
                        if (nEquip.getAvoid() > 0) {
                            short newStat = (short) (nEquip.getAvoid() + Math.ceil(Math.random() * 50.0d) * increase);
                            nEquip.setAvoid(newStat);
                        }
                        if (nEquip.getSpeed() > 0) {
                            short newStat = (short) (nEquip.getSpeed() + Math.ceil(Math.random() * 5.0d) * increase);
                            nEquip.setSpeed(newStat);
                        }
                        if (nEquip.getJump() > 0) {
                            short newStat = (short) (nEquip.getJump() + Math.ceil(Math.random() * 5.0d) * increase);
                            nEquip.setJump(newStat);
                        }
                        if (nEquip.getHp() > 0) {
                            short newStat = (short) (nEquip.getHp() + Math.ceil(Math.random() * 50.0d) * increase);
                            nEquip.setHp(newStat);
                        }
                        if (nEquip.getMp() > 0) {
                            short newStat = (short) (nEquip.getMp() + Math.ceil(Math.random() * 50.0d) * increase);
                            nEquip.setMp(newStat);
                        }
                        break;
                    case 2049004: // Innocence Scroll 60%
                        Map<String, Integer> innoStats = getEquipStats(nEquip.getItemId());
                        nEquip.setStr(innoStats.getOrDefault("STR", 0).shortValue());
                        nEquip.setDex(innoStats.getOrDefault("DEX", 0).shortValue());
                        nEquip.setInt(innoStats.getOrDefault("INT", 0).shortValue());
                        nEquip.setLuk(innoStats.getOrDefault("LUK", 0).shortValue());
                        nEquip.setWatk(innoStats.getOrDefault("PAD", 0).shortValue());
                        nEquip.setWdef(innoStats.getOrDefault("PDD", 0).shortValue());
                        nEquip.setMatk(innoStats.getOrDefault("MAD", 0).shortValue());
                        nEquip.setMdef(innoStats.getOrDefault("MDD", 0).shortValue());
                        nEquip.setAcc(innoStats.getOrDefault("ACC", 0).shortValue());
                        nEquip.setAvoid(innoStats.getOrDefault("EVA", 0).shortValue());
                        nEquip.setSpeed(innoStats.getOrDefault("Speed", 0).shortValue());
                        nEquip.setJump(innoStats.getOrDefault("Jump", 0).shortValue());
                        nEquip.setHp(innoStats.getOrDefault("MHP", 0).shortValue());
                        nEquip.setMp(innoStats.getOrDefault("MMP", 0).shortValue());
                        nEquip.setUpgradeSlots(innoStats.getOrDefault("tuc", 0).byteValue());
                        nEquip.setLevel((byte) 0);
                        break;
                    default:
                        for (Map.Entry<String, Integer> stat : stats.entrySet()) {
                            if (stat.getKey().equals("STR")) {
                                nEquip.setStr((short) (nEquip.getStr() + stat.getValue()));
                            } else if (stat.getKey().equals("DEX")) {
                                nEquip.setDex((short) (nEquip.getDex() + stat.getValue()));
                            } else if (stat.getKey().equals("INT")) {
                                nEquip.setInt((short) (nEquip.getInt() + stat.getValue()));
                            } else if (stat.getKey().equals("LUK")) {
                                nEquip.setLuk((short) (nEquip.getLuk() + stat.getValue()));
                            } else if (stat.getKey().equals("PAD")) {
                                nEquip.setWatk((short) (nEquip.getWatk() + stat.getValue()));
                            } else if (stat.getKey().equals("PDD")) {
                                nEquip.setWdef((short) (nEquip.getWdef() + stat.getValue()));
                            } else if (stat.getKey().equals("MAD")) {
                                nEquip.setMatk((short) (nEquip.getMatk() + stat.getValue()));
                            } else if (stat.getKey().equals("MDD")) {
                                nEquip.setMdef((short) (nEquip.getMdef() + stat.getValue()));
                            } else if (stat.getKey().equals("ACC")) {
                                nEquip.setAcc((short) (nEquip.getAcc() + stat.getValue()));
                            } else if (stat.getKey().equals("EVA")) {
                                nEquip.setAvoid((short) (nEquip.getAvoid() + stat.getValue()));
                            } else if (stat.getKey().equals("Speed")) {
                                nEquip.setSpeed((short) (nEquip.getSpeed() + stat.getValue()));
                            } else if (stat.getKey().equals("Jump")) {
                                nEquip.setJump((short) (nEquip.getJump() + stat.getValue()));
                            } else if (stat.getKey().equals("MHP")) {
                                nEquip.setHp((short) (nEquip.getHp() + stat.getValue()));
                            } else if (stat.getKey().equals("MMP")) {
                                nEquip.setMp((short) (nEquip.getMp() + stat.getValue()));
                            }
                        }
                        break;
                }

                if (noFail && !isGM) {
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, 2022118, 1, true, false);
                }

                if (!isCleanSlate(scrollId) && scrollId != 2049004) {
                    if (!isGM) {
                        nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                    }
                    nEquip.setLevel((byte) (nEquip.getLevel() + 1));
                }
            } else {
                if (!usingWhiteScroll && !isCleanSlate(scrollId) && nEquip.getUpgradeSlots() > 0) {
                    nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                }
                if (Math.ceil(1.0d + Math.random() * 100.0d) < stats.get("cursed")) return null;
            }
        }
        return equip;
    }

    public Equip getEquipByIdAsEquip(int itemId) {
        return (Equip) getEquipById(itemId);
    }

    public IItem getEquipById(int equipId) {
        return getEquipById(equipId, -1);
    }

    public IItem getEquipById(int equipId, int ringId) {
        if (equipCache.containsKey(equipId)) return equipCache.get(equipId).copy();
        Equip nEquip;
        nEquip = new Equip(equipId, (byte) 0, ringId);
        nEquip.setQuantity((short) 1);
        Map<String, Integer> stats = getEquipStats(equipId);
        if (stats != null) {
            for (Map.Entry<String, Integer> stat : stats.entrySet()) {
                final String k = stat.getKey();
                final short val = (short) stat.getValue().intValue();
                switch (k) {
                    case "STR":
                        nEquip.setStr(val);
                        break;
                    case "DEX":
                        nEquip.setDex(val);
                        break;
                    case "INT":
                        nEquip.setInt(val);
                        break;
                    case "LUK":
                        nEquip.setLuk(val);
                        break;
                    case "PAD":
                        nEquip.setWatk(val);
                        break;
                    case "PDD":
                        nEquip.setWdef(val);
                        break;
                    case "MAD":
                        nEquip.setMatk(val);
                        break;
                    case "MDD":
                        nEquip.setMdef(val);
                        break;
                    case "ACC":
                        nEquip.setAcc(val);
                        break;
                    case "EVA":
                        nEquip.setAvoid(val);
                        break;
                    case "Speed":
                        nEquip.setSpeed(val);
                        break;
                    case "Jump":
                        nEquip.setJump(val);
                        break;
                    case "MHP":
                        nEquip.setHp(val);
                        break;
                    case "MMP":
                        nEquip.setMp(val);
                        break;
                    case "tuc":
                        nEquip.setUpgradeSlots((byte) stat.getValue().intValue());
                        break;
                }
            }
        }
        equipCache.put(equipId, nEquip);
        return nEquip.copy();
    }

    private short getRandStat(short defaultValue, int maxRange, short additionalStats) {
        if (defaultValue == 0) return 0;
        // Vary no more than ceil of 10% of stat
        int lMaxRange = (int) Math.min(Math.ceil(defaultValue * 0.1d), maxRange);
        return (short) (defaultValue - lMaxRange + Math.floor(rand.nextDouble() * (lMaxRange * 2 + additionalStats)));
    }

    public Equip randomizeStats(MapleClient c, Equip equip) {
        short x = 1;
        ChannelServer cserv = c.getChannelServer();
        if (cserv.isGodlyItems() && Math.ceil(Math.random() * 100.0d) <= cserv.getGodlyItemRate()) {
            x = cserv.getItemMultiplier();
        }

        equip.setStr(getRandStat(equip.getStr(), 5, x));
        equip.setDex(getRandStat(equip.getDex(), 5, x));
        equip.setInt(getRandStat(equip.getInt(), 5, x));
        equip.setLuk(getRandStat(equip.getLuk(), 5, x));
        equip.setMatk(getRandStat(equip.getMatk(), 5, x));
        equip.setWatk(getRandStat(equip.getWatk(), 5, x));
        equip.setAcc(getRandStat(equip.getAcc(), 5, x));
        equip.setAvoid(getRandStat(equip.getAvoid(), 5, x));
        equip.setJump(getRandStat(equip.getJump(), 5, x));
        equip.setSpeed(getRandStat(equip.getSpeed(), 5, x));
        equip.setWdef(getRandStat(equip.getWdef(), 10, x));
        equip.setMdef(getRandStat(equip.getMdef(), 10, x));
        equip.setHp(getRandStat(equip.getHp(), 10, x));
        equip.setMp(getRandStat(equip.getMp(), 10, x));
        return equip;
    }

    public Equip hardcoreItem(Equip equip, short stat) {
        equip.setStr(stat);
        equip.setDex(stat);
        equip.setInt(stat);
        equip.setLuk(stat);
        equip.setMatk(stat);
        equip.setWatk(stat);
        equip.setAcc(stat);
        equip.setAvoid(stat);
        equip.setJump(stat);
        equip.setSpeed(stat);
        equip.setWdef(stat);
        equip.setMdef(stat);
        equip.setHp(stat);
        equip.setMp(stat);
        return equip;
    }

    public MapleStatEffect getItemEffect(int itemId) {
        MapleStatEffect ret = itemEffects.get(itemId);
        if (ret == null) {
            MapleData item = getItemData(itemId);
            if (item == null) return null;
            MapleData spec = item.getChildByPath("spec");
            ret = MapleStatEffect.loadItemEffectFromData(spec, itemId);
            itemEffects.put(itemId, ret);
        }
        return ret;
    }

    public int[][] getSummonMobs(int itemId) {
        MapleData data = getItemData(itemId);
        int theInt = data.getChildByPath("mob").getChildren().size();
        int[][] mobs2spawn = new int[theInt][2];
        for (int x = 0; x < theInt; ++x) {
            mobs2spawn[x][0] = MapleDataTool.getIntConvert("mob/" + x + "/id", data);
            mobs2spawn[x][1] = MapleDataTool.getIntConvert("mob/" + x + "/prob", data);
        }
        return mobs2spawn;
    }

    public boolean isThrowingStar(int itemId) {
        return itemId >= 2070000 && itemId < 2080000;
    }

    public boolean isBullet(int itemId) {
        int id = itemId / 10000;
        return id == 233;
    }

    public boolean isRechargable(int itemId) {
        int id = itemId / 10000;
        return id == 233 || id == 207;
    }

    public boolean isOverall(int itemId) {
        return itemId >= 1050000 && itemId < 1060000;
    }

    public boolean isPet(int itemId) {
        return itemId >= 5000000 && itemId <= 5000100;
    }

    public boolean isArrowForCrossBow(int itemId) {
        return itemId >= 2061000 && itemId < 2062000;
    }

    public boolean isArrowForBow(int itemId) {
        return itemId >= 2060000 && itemId < 2061000;
    }

    public boolean isTwoHanded(int itemId) {
        switch (getWeaponType(itemId)) {
            case AXE2H:
            case BLUNT2H:
            case BOW:
            case CLAW:
            case CROSSBOW:
            case POLE_ARM:
            case SPEAR:
            case SWORD2H:
            case GUN:
            case KNUCKLE:
                return true;
            default:
                return false;
        }
    }

    public boolean isTownScroll(int itemId) {
        return (itemId >= 2030000 && itemId < 2030020);
    }

    public boolean isGun(int itemId) {
        return itemId >= 1492000 && itemId <= 1492024;
    }

    public int getWatkForProjectile(int itemId) {
        Integer atk = projectileWatkCache.get(itemId);
        if (atk != null) return atk;
        MapleData data = getItemData(itemId);
        atk = MapleDataTool.getInt("info/incPAD", data, 0);
        projectileWatkCache.put(itemId, atk);
        return atk;
    }

    public boolean canScroll(int scrollid, int itemid) {
        int scrollCategoryQualifier = (scrollid / 100) % 100;
        int itemCategoryQualifier = (itemid / 10000) % 100;
        return scrollCategoryQualifier == itemCategoryQualifier;
    }

    public String getName(int itemId) {
        if (nameCache.containsKey(itemId)) return nameCache.get(itemId);
        MapleData strings = getStringData(itemId);
        if (strings == null) return null;
        String ret = MapleDataTool.getString("name", strings, null);
        nameCache.put(itemId, ret);
        return ret;
    }

    public String getDesc(int itemId) {
        if (descCache.containsKey(itemId)) return descCache.get(itemId);
        MapleData strings = getStringData(itemId);
        if (strings == null) return null;
        String ret = MapleDataTool.getString("desc", strings, null);
        descCache.put(itemId, ret);
        return ret;
    }

    public String getMsg(int itemId) {
        if (msgCache.containsKey(itemId)) return msgCache.get(itemId);
        MapleData strings = getStringData(itemId);
        if (strings == null) return null;
        String ret = MapleDataTool.getString("msg", strings, null);
        msgCache.put(itemId, ret);
        return ret;
    }

    public boolean isDropRestricted(int itemId) {
        if (dropRestrictionCache.containsKey(itemId)) return dropRestrictionCache.get(itemId);

        MapleData data = getItemData(itemId);

        boolean bRestricted = MapleDataTool.getIntConvert("info/tradeBlock", data, 0) == 1;
        if (!bRestricted) bRestricted = MapleDataTool.getIntConvert("info/quest", data, 0) == 1;
        dropRestrictionCache.put(itemId, bRestricted);

        return bRestricted;
    }

    public boolean isPickupRestricted(int itemId) {
        if (pickupRestrictionCache.containsKey(itemId)) return pickupRestrictionCache.get(itemId);

        MapleData data = getItemData(itemId);
        boolean bRestricted = MapleDataTool.getIntConvert("info/only", data, 0) == 1;

        pickupRestrictionCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public Map<String, Integer> getSkillStats(int itemId, double playerJob) {
        Map<String, Integer> ret = new LinkedHashMap<>();
        MapleData item = getItemData(itemId);
        if (item == null) return null;
        MapleData info = item.getChildByPath("info");
        if (info == null) return null;
        for (MapleData data : info.getChildren()) {
            if (data.getName().startsWith("inc")) {
                ret.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
            }
        }
        ret.put("masterLevel", MapleDataTool.getInt("masterLevel", info, 0));
        ret.put("reqSkillLevel", MapleDataTool.getInt("reqSkillLevel", info, 0));
        ret.put("success", MapleDataTool.getInt("success", info, 0));

        MapleData skill = info.getChildByPath("skill");
        int curskill;
        int size = skill.getChildren().size();
        for (int i = 0; i < size; ++i) {
            curskill = MapleDataTool.getInt(Integer.toString(i), skill, 0);
            if (curskill == 0) break;
            double skillJob = Math.floor(curskill / 10000);
            if (skillJob == playerJob) {
                ret.put("skillid", curskill);
                break;
            }
        }

        ret.putIfAbsent("skillid", 0);

        return ret;
    }

    public List<Integer> petsCanConsume(int itemId) {
        List<Integer> ret = new ArrayList<>();
        MapleData data = getItemData(itemId);
        int curPetId;
        int size = data.getChildren().size();
        for (int i = 0; i < size; ++i) {
            curPetId = MapleDataTool.getInt("spec/" + Integer.toString(i), data, 0);
            if (curPetId == 0) break;
            ret.add(curPetId);
        }
        return ret;
    }

    public boolean isQuestItem(int itemId) {
        if (isQuestItemCache.containsKey(itemId)) return isQuestItemCache.get(itemId);
        MapleData data = getItemData(itemId);
        boolean questItem = MapleDataTool.getIntConvert("info/quest", data, 0) == 1;
        isQuestItemCache.put(itemId, questItem);
        return questItem;
    }
}
