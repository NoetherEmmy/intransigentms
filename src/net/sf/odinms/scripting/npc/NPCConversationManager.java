package net.sf.odinms.scripting.npc;

import net.sf.odinms.client.*;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.scripting.AbstractPlayerInteraction;
import net.sf.odinms.scripting.event.EventManager;
import net.sf.odinms.server.*;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MobSkill;
import net.sf.odinms.server.life.MobSkillFactory;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;

import java.awt.*;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NPCConversationManager extends AbstractPlayerInteraction {
    private final MapleClient c;
    private final int npc;
    private final String fileName;
    private String getText;
    private final MapleCharacter chr;

    public NPCConversationManager(MapleClient c, int npc, MapleCharacter chr, String fileName) {
        super(c);
        this.c = c;
        this.npc = npc;
        this.chr = chr;
        this.fileName = fileName;
    }

    public void dispose() {
        NPCScriptManager.getInstance().dispose(this);
    }

    public void sendNext(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01"));
    }

    public void sendPrev(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00"));
    }

    public void sendNextPrev(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01"));
    }

    public void sendOk(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00"));
    }

    public void sendYesNo(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 1, text, ""));
    }

    public void sendAcceptDecline(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0x0C, text, ""));
    }

    public void sendSimple(String text) {
        if (!text.contains("#L")) {
            getPlayer().dropMessage(
                6,
                "!!! This NPC is broken. @gm someone and tell them you got this message. " +
                    "If no one is online, just remember for later. !!!"
            );
            dispose();
        } else {
            getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 4, text, ""));
        }
    }

    public void sendStyle(String text, int[] styles) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkStyle(npc, text, styles));
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkNum(npc, text, def, min, max));
    }

    public void sendGetText(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkText(npc, text));
    }

    public void setGetText(String text) {
        getText = text;
    }

    public String getText() {
        return getText;
    }

    public void openShop(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(getClient());
    }

    @Override
    public void openNpc(int id) {
        dispose();
        NPCScriptManager.getInstance().start(getClient(), id);
    }

    public void changeJob(MapleJob job) {
        getPlayer().changeJob(job, true);
        int reqlv;
        if (job.getId() % 100 == 0) {
            reqlv = 10;
            if (job.getId() == 200) {
                reqlv = 8;
            }
        } else {
            return;
        }
        if (getPlayer().getLevel() > reqlv) {
            getPlayer().setRemainingSp(getPlayer().getRemainingSp() + ((getPlayer().getLevel() - reqlv) * 3));
            getPlayer().updateSingleStat(MapleStat.AVAILABLESP, getPlayer().getRemainingSp());
        }
    }

    public MapleJob getJob() {
        return getPlayer().getJob();
    }

    public void startQuest(int id) {
        MapleQuest.getInstance(id).start(getPlayer(), npc);
    }

    public void completeQuest(int id) {
        MapleQuest.getInstance(id).complete(getPlayer(), npc);
    }

    public void forfeitQuest(int id) {
        MapleQuest.getInstance(id).forfeit(getPlayer());
    }

    /**
     * use getPlayer().getMeso() instead
     * @return player's meso count
     */
    @Deprecated
    public int getMeso() {
        return getPlayer().getMeso();
    }

    public void gainMeso(int gain) {
        getPlayer().gainMeso(gain, true, false, true);
    }

    public void gainExp(int gain) {
        getPlayer().gainExp(gain, true, true);
    }

    public int getNpc() {
        return npc;
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * use getPlayer().getLevel() instead
     * @return level of player
     */
    @Deprecated
    public int getLevel() {
        return getPlayer().getLevel();
    }

    public void unequipEverything() {
        getPlayer().unequipEverything();
    }

    public void teachSkill(int id, int level, int masterlevel) {
        getPlayer().changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public void clearSkills() {
        getPlayer().getSkills().keySet().forEach(skill ->
            getPlayer().changeSkillLevel(skill, 0, 0)
        );
    }

    /**
     * Use getPlayer() instead (for consistency with MapleClient)
     * @return the player in the conversation
     */
    @Deprecated
    public MapleCharacter getChar() {
        return getPlayer();
    }

    public MapleClient getC() {
        return getClient();
    }

    public void rechargeStars() {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem stars = getPlayer().getInventory(MapleInventoryType.USE).getItem((byte) 1);
        if (ii.isThrowingStar(stars.getItemId()) || ii.isBullet(stars.getItemId())) {
            stars.setQuantity(ii.getSlotMax(getClient(), stars.getItemId()));
            getC().getSession().write(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, stars));
        }
    }

    public EventManager getEventManager(String event) {
        return getClient().getChannelServer().getEventSM().getEventManager(event);
    }

    public void showEffect(String effect) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect(effect));
    }

    public void playSound(String sound) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound(sound));
    }

    @Override
    public String toString() {
        return "Conversation with NPC: " + npc;
    }

    public void updateBuddyCapacity(int capacity) {
        getPlayer().setBuddyCapacity(capacity);
    }

    public int getBuddyCapacity() {
        return getPlayer().getBuddyCapacity();
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor(MapleSkinColor.getById(color));
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public boolean startCQuest(int id) {
        return getPlayer().loadCQuest(id);
    }

    public boolean onQuest(int questId) {
        return questId != 0 && getPlayer().getCQuestById(questId) != null;
    }

    public boolean canComplete(int questId) {
        return getPlayer().canCompleteCQuest(questId);
    }

    public String selectQuest(int questId, String msg) {
        String intro = msg + "\r\n\r\n#fUI/UIWindow.img/QuestIcon/3/0#\r\n#L0#";
        String selection =
            "#k[" +
            (!getPlayer().isOnCQuest(questId) ?
                "#rAvailable" :
                (!canComplete(questId) ?
                    "#dIn progress" :
                    "#gComplete")) +
            "#k]";
        return intro + selection + " #e" + MapleCQuests.loadQuest(questId).getTitle();
    }

    public String showReward(int questId, String msg) {
        final StringBuilder sb = new StringBuilder();
        final MapleCQuests q = getPlayer().getCQuestById(questId).getQuest();
        sb.append(msg);
        sb.append("\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n\r\n");
        sb.append("#fUI/UIWindow.img/QuestIcon/8/0#  ")
          .append(MapleCharacter.makeNumberReadable(q.getExpReward()))
          .append("\r\n");
        sb.append("#fUI/UIWindow.img/QuestIcon/7/0#  ")
          .append(MapleCharacter.makeNumberReadable(q.getMesoReward()))
          .append("\r\n");
        q.readItemRewards().forEach((itemId, count) ->
            sb.append("\r\n#i")
              .append(itemId)
              .append("#  ")
              .append(MapleCharacter.makeNumberReadable(count))
        );
        return sb.toString();
    }

    public void rewardPlayer(int questId) {
        getPlayer().completeCQuest(questId);
    }

    public boolean forfeitCQuestById(int questId) {
        return getPlayer().forfeitCQuestById(questId);
    }

    public String showQuestProgress() {
        final StringBuilder sb = new StringBuilder();
        byte currSlot = (byte) 1;
        for (final CQuest cQuest : getPlayer().getCQuests()) {
            sb.append("Quest info for slot #r").append(currSlot).append("#k:\r\n");
            if (cQuest != null && cQuest.getQuest().getId() != 0) {
                sb.append("\t\t#e")
                  .append(cQuest.getQuest().getTitle())
                  .append("#n\r\n\r\n");

                cQuest.getQuest().readMonsterTargets().forEach((mobId, qtyAndName) -> {
                    int killed = cQuest.getQuestKills(mobId);
                    int outOf = qtyAndName.getLeft();
                    sb.append("\t\t")
                      .append(qtyAndName.getRight())
                      .append("s killed: ")
                      .append(killed >= outOf ? "#g" : "#d")
                      .append(killed)
                      .append("#k/")
                      .append(outOf)
                      .append("\r\n");
                });
                sb.append("\r\n");

                cQuest.getQuest().readItemsToCollect().forEach((itemId, qtyAndName) -> {
                    int collected = getPlayer().getQuestCollected(itemId);
                    int outOf = qtyAndName.getLeft();
                    sb.append("\t\t")
                      .append(qtyAndName.getRight())
                      .append("s collected: ")
                      .append(collected >= outOf ? "#g" : "#d")
                      .append(collected)
                      .append("#k/")
                      .append(outOf)
                      .append("\r\n");
                });
                sb.append("\r\n");

                cQuest.getQuest().readOtherObjectives().forEach((name, count) -> {
                    int completed = cQuest.getObjectiveProgress(name);
                    int outOf = count;
                    sb.append("\t\t")
                      .append(name)
                      .append(": ")
                      .append(completed >= outOf ? "#g" : "#d")
                      .append(completed)
                      .append("#k/")
                      .append(outOf)
                      .append("\r\n");
                });
                sb.append("\r\n");

                if (cQuest.getQuest().hasIdenticalStartEnd()) {
                    sb.append("\t\t#eQuest NPC: #n#d")
                      .append(cQuest.getQuest().getStartNpc())
                      .append("#k\r\n");
                } else {
                    sb.append("\t\t#eQuest start NPC: #n#d")
                      .append(cQuest.getQuest().getStartNpc())
                      .append("#k\r\n");
                    sb.append("\t\t#eQuest end NPC: #n#g")
                      .append(cQuest.getQuest().getEndNpc())
                      .append("#k\r\n");
                }
                sb.append("\t\tQuest effective level: #r")
                  .append(
                      cQuest.getEffectivePlayerLevel() > 0 ?
                          cQuest.getEffectivePlayerLevel() :
                          getPlayer().getLevel()
                  )
                  .append("#k\r\n");
                String[] questInfoSplit = cQuest.getQuest().getInfo().split(" ");
                StringBuilder questInfo = new StringBuilder("\t\t");
                int currLineLength = 0;
                for (final String word : questInfoSplit) {
                    if (word.length() < 1) continue;
                    if (currLineLength == 0) {
                        questInfo.append(word);
                        currLineLength += word.length();
                    } else if (currLineLength + word.length() > 45) {
                        questInfo
                            .append("\r\n\t\t")
                            .append(word);
                        currLineLength = word.length();
                    } else {
                        questInfo
                            .append(' ')
                            .append(word);
                        currLineLength += word.length() + 1;
                    }
                }
                sb.append("\t\t#eQuest info: #n\r\n\r\n")
                  .append(questInfo)
                  .append("\r\n\r\n");
            } else {
                sb.append("\t\tThere's no quest in this slot.\r\n\r\n");
            }
            currSlot++;
        }
        return sb.toString();
    }

    public String randomText(int type) {
        switch (type) {
            case 1:
                return "Hey, what's up?";
            case 2:
                return "Very well done.";
            case 3:
                return "Aw, what a shame.";
            case 4:
                return "You already accepted: ";
            case 5:
                return ". Do you want to cancel it?";
            case 6:
                return "Quest complete!";
            case 7:
                return "You haven't completed your task yet! " +
                       "You can look it up by typing #b@questinfo#k in the chat.\r\n" +
                       "Do you want to cancel this quest?";
            case 8:
                return "Looks like you can't start this quest yet.\r\n\r\n" +
                       "Maybe all your quest slots or full, or you haven't " +
                       "completed all the pre-requisites?";
            case 9:
                return "There was a problem forfeiting the quest. " +
                       "Looks like you didn't have this quest active after all.";
        }
        return "";
    }

    public String getAllItemStats(int itemid) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem item = ii.getEquipById(itemid);
        Equip eqp;
        String itemstats;
        itemstats = "Req. level: #b" + ii.getReqLevel(itemid) + "#k\r\n";
        if (ii.isWeapon(itemid)) {
            itemstats += "Type: #b" + ii.getWeaponType(itemid).toString() + "#k\r\n";
        } else if (ii.isArrowForBow(itemid)) {
            itemstats += "Type: #b" + "Arrow for bow" + "#k\r\n";
        } else if (ii.isArrowForCrossBow(itemid)) {
            itemstats += "Type: #b" + "Arrow for crossbow" + "#k\r\n";
        } else if (ii.isBullet(itemid)) {
            itemstats += "Type: #b" + "Bullet" + "#k\r\n";
        } else if (ii.isOverall(itemid)) {
            itemstats += "Type: #b" + "Overall" + "#k\r\n";
        } else if (ii.isPet(itemid)) {
            itemstats += "Type: #b" + "Pet" + "#k\r\n";
        } else if (ii.isQuestItem(itemid)) {
            itemstats += "Type: #b" + "Quest item" + "#k\r\n";
        } else if (ii.isShield(itemid)) {
            itemstats += "Type: #b" + "Shield" + "#k\r\n";
        } else if (ii.isThrowingStar(itemid)) {
            itemstats += "Type: #b" + "Throwing star" + "#k\r\n";
        } else if (ii.isTownScroll(itemid)) {
            itemstats += "Type: #b" + "Town scroll" + "#k\r\n";
        }
        if (ii.isEquip(itemid)) {
            eqp = (Equip) item;
            if (eqp.getAcc() > 0) {
                itemstats += "Accuracy: #b" + eqp.getAcc() + "#k\r\n";
            }
            if (eqp.getAvoid() > 0) {
                itemstats += "Avoidability: #b" + eqp.getAvoid() + "#k\r\n";
            }
            if (eqp.getStr() > 0) {
                itemstats += "Str: #b" + eqp.getStr() + "#k\r\n";
            }
            if (eqp.getDex() > 0) {
                itemstats += "Dex: #b" + eqp.getDex() + "#k\r\n";
            }
            if (eqp.getInt() > 0) {
                itemstats += "Int: #b" + eqp.getInt() + "#k\r\n";
            }
            if (eqp.getLuk() > 0) {
                itemstats += "Luk: #b" + eqp.getLuk() + "#k\r\n";
            }
            if (eqp.getHp() > 0) {
                itemstats += "MaxHP: #b" + eqp.getHp() + "#k\r\n";
            }
            if (eqp.getMp() > 0) {
                itemstats += "MaxMP: #b" + eqp.getMp() + "#k\r\n";
            }
            if (eqp.getJump() > 0) {
                itemstats += "Jump: #b" + eqp.getJump() + "#k\r\n";
            }
            if (eqp.getSpeed() > 0) {
                itemstats += "Speed: #b" + eqp.getSpeed() + "#k\r\n";
            }
            if (eqp.getWatk() > 0) {
                itemstats += "Attack: #b" + eqp.getWatk() + "#k\r\n";
            }
            if (eqp.getMatk() > 0) {
                itemstats += "Magic Attack: #b" + eqp.getMatk() + "#k\r\n";
            }
            if (eqp.getWdef() > 0) {
                itemstats += "W. def: #b" + eqp.getWdef() + "#k\r\n";
            }
            if (eqp.getMdef() > 0) {
                itemstats += "M. def: #b" + eqp.getMdef() + "#k\r\n";
            }
            itemstats += "Slots: #b" + eqp.getUpgradeSlots() + "#k";
        } else if (ii.isArrowForBow(itemid) ||
                   ii.isArrowForCrossBow(itemid) ||
                   ii.isBullet(itemid) ||
                   ii.isThrowingStar(itemid)) {
            itemstats += "Attack: #b" + ii.getWatkForProjectile(itemid) + "#k\r\n";
        } else {
            itemstats += "Description: #b" + ii.getDesc(itemid) + "#k\r\n";
        }

        return itemstats;
    }

    public void damagePlayer(int dmg, boolean lethal) {
        if (lethal) {
            getPlayer().setHp(getPlayer().getHp() - dmg);
        } else {
            if (getPlayer().getHp() > dmg) {
                getPlayer().setHp(getPlayer().getHp() - dmg);
            } else {
                dmg = getPlayer().getHp() - 1;
                getPlayer().setHp(1);
            }
        }
        getPlayer().updateSingleStat(MapleStat.HP, getPlayer().getHp());
        getPlayer().dropMessage(5, "You've lost " + dmg + " HP.");
    }

    public boolean isInMonsterTrial() {
        return
            getPlayer().getMapId() >= 1000 &&
            getPlayer().getMapId() <= 1006 &&
            getPlayer().getHp() > 0;
    }

    public boolean canEnterMonsterTrial() {
        return
            System.currentTimeMillis() - getPlayer().getLastTrialTime() >= 7200000L &&
            getPlayer().getLevel() >= 20;
    }

    public int getMonsterTrialCooldown() {
        final long timeSinceLast = System.currentTimeMillis() - getPlayer().getLastTrialTime();
        return 120 - (int) (timeSinceLast / 60000.0d);
    }

    public int getMonsterTrialPoints() {
        return getPlayer().getMonsterTrialPoints();
    }

    public int getTier() {
        return getPlayer().getMonsterTrialTier();
    }

    public int getTierPoints(int tier) {
        if (tier < 1) return 0;
        return (int) (getTierPoints(tier - 1) + (10 * Math.pow(tier + 1, 2)) * (Math.floor(tier * 1.5d) + 3));
    }

    public int calculateLevelGroup(int level) {
        int lg = 0;
        if (level >= 20)  lg++;
        if (level >= 26)  lg++;
        if (level >= 31)  lg++;
        if (level >= 41)  lg++;
        if (level >= 51)  lg++;
        if (level >= 71)  lg++;
        if (level >= 91)  lg++;
        if (level >= 101) lg++;
        if (level >= 121) lg++;
        if (level >= 141) lg++;
        if (level >= 161) lg++;
        if (level >= 201) lg++;
        return lg;
    }

    public boolean enterMonsterTrial() {
         /*
          * 1st dimension: Corresponds to level group of player (-1 to adjust for level group not being zero-based)
          * 2nd dimension: Monster IDs for that level group as ints, with a digit added at the end of the decimal
          *                representation corresponding to how many should be spawned. Thus the raw magnitudes of
          *                these ints are ~1 order of magnitude larger than the actual IDs would be.
          */
        final int[][] monsters = {
            {22200002, 32200001, 95001692, 95001683}, // 20 - 25
            {95001693, 32200002, 32200011, 95001701}, // 26 - 30
            {32200012, 93000031, 42200011, 95001702, 95003281}, // 31 - 40
            {42200012, 95003281, 52200021, 95001771, 51201001, 95003301}, // 41 - 50
            {52200011, 95001771, 52200031, 95003341, 95001761, 61301011, 62200001, 62200011}, // 51 - 70
            {62200011, 52200012, 71304002, 71304012, 71304022, 93000121, 95001731,
                95001741, 63000051, 72200001, 71103003, 90010041, 72200031, 82200101}, // 71 - 90
            {82200021, 82200001, 81301001, 72200021, 93000121,
                95001731, 95001741, 94100141, 82200101, 82200091, 82200121, 82200141}, // 91 - 100
            {82200013, 94002052, 93001192,
                93000392, 95003352, 94001201, 81700003, 81600003, 82200041, 82200141}, // 101 - 120
            {81500001, 93000281, 93000891, 93000901, 93000311, 93000321, 82200051, 82200061}, // 121 - 140
            {81800001, 81800011, 94005362, 85000011}, // 141 - 160
            {85200001, 94005751, 94000141}, // 161 - 200
            {94005752, 94000142, 94006591, 94001211}  // 201 - 250
        };

        for (int mapid = 1000; mapid <= 1006; ++mapid) {
            final MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
            if (map.playerCount() == 0) {
                map.killAllMonsters(false);
                getPlayer().setTrialReturnMap(getPlayer().getMapId());
                getPlayer().changeMap(map, map.getPortal(0));
                final int returnMapId = getPlayer().getTrialReturnMap();
                final MapleCharacter player = getPlayer();
                TimerManager tMan = TimerManager.getInstance();
                final Runnable endTrialTask = () -> {
                    if (player.getMapId() >= 1000 && player.getMapId() <= 1006) {
                        player.changeMap(returnMapId, 0);
                    }
                };
                getPlayer().getClient().getSession().write(MaplePacketCreator.getClock(30 * 60)); // 30 minutes
                final int[] monsterChoices = monsters[calculateLevelGroup(getPlayer().getLevel()) - 1];
                final int monsterChoice = monsterChoices[(int) (Math.random() * monsterChoices.length)];
                final int monsterId = monsterChoice / 10;
                final int monsterCount = monsterChoice % 10;
                final Point monsterSpawnpoint = new Point(-336, -11); // (-336, 101)
                for (int i = 0; i < monsterCount; ++i) {
                    try {
                        map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(monsterId), monsterSpawnpoint);
                    } catch (NullPointerException npe) {
                        System.err.println(
                            "Player " +
                            player.getName() +
                            " booted from Monster Trials; monsterChoice, monsterId: " +
                            monsterChoice +
                            ", " +
                            monsterId
                        );
                        npe.printStackTrace();
                        tMan.schedule(() -> {
                            player.changeMap(returnMapId, 0);
                            player.dropMessage(
                                "There was an error loading your Monster Trial! Tell a GM about this and try again."
                            );
                        }, 500L);
                        return true;
                    }
                }
                tMan.schedule(endTrialTask, 30L * 60L * 1000L); // 30 minutes
                getPlayer().setLastTrialTime(System.currentTimeMillis());
                return true;
            }
        }
        return false;
    }

    public boolean enterBossMap(int bossid) {
        final int mapId = 3000;
        MapleMap map;
        List<MapleCharacter> partyMembers = new ArrayList<>(6);
        for (MaplePartyCharacter pm : getPlayer().getParty().getMembers()) {
            if (pm.isOnline()) partyMembers.add(
                c.getChannelServer()
                 .getPlayerStorage()
                 .getCharacterById(pm.getId())
            );
        }
        partyMembers = partyMembers.stream()
                                   .filter(Objects::nonNull)
                                   .collect(Collectors.toCollection(ArrayList::new));
        map = c.getChannelServer().getMapFactory().getMap(mapId);
        if (map.playerCount() == 0) {
            map.killAllMonsters(false);
            map.clearDrops();
            for (MapleCharacter partyMember : partyMembers) {
                partyMember.setBossReturnMap(partyMember.getMapId());
            }
            warpParty(mapId);

            partyMembers.forEach(pm ->
                pm.setForcedWarp(
                    pm.getBossReturnMap(),
                    40L * 60L * 1000L,
                    mc -> mc.getMapId() == mapId
                )
            );

            for (MapleCharacter p : partyMembers) {
                p.getClient()
                 .getSession()
                 .write(MaplePacketCreator.getClock(40 * 60)); // 40 minutes
            }
            Point monsterspawnpos = new Point(-474, -304); // (-474, -204)
            try {
                map.spawnMonsterOnGroundBelow(
                    MapleLifeFactory.getMonster(bossid),
                    monsterspawnpos
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }

    public boolean canLeaveMonsterTrial() {
        return getPlayer().getHp() > 0 && getPlayer().getMap().mobCount() < 1;
    }

    public int getMonsterTrialReturnMap() {
        return getPlayer().getTrialReturnMap();
    }

    public void gainMonsterTrialPoints(int points) {
        getPlayer().setMonsterTrialPoints(getMonsterTrialPoints() + points);
    }

    public void setTier(int tier) {
        getPlayer().setMonsterTrialTier(tier);
    }

    public String getDeathCountRanking(int top) {
        String r = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM characters WHERE deathcount > ? && gm < 1 ORDER BY deathcount DESC"
            );
            ps.setInt(1, 0);
            ResultSet rs = ps.executeQuery();
            int i = -1;
            while (rs.next()) {
                i++;
                if (i < top) {
                    r += rs.getString("name") + ": #b" + rs.getInt("deathcount") + "#k\r\n";
                } else {
                    break;
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    public String getHighestLevelAchievedRanking(int top) {
        String r = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM characters " +
                "WHERE highestlevelachieved > ? && gm < 1 ORDER BY highestlevelachieved DESC"
            );
            ps.setInt(1, 120);
            ResultSet rs = ps.executeQuery();
            int i = -1;
            while (rs.next()) {
                i++;
                if (i < top) {
                    r += rs.getString("name") +
                            ": #b" +
                            rs.getInt("highestlevelachieved") +
                            "#k\r\n";
                } else {
                    break;
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    public String getSuicidesRanking(int top) {
        String r = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM characters WHERE suicides > ? && gm < 1 ORDER BY suicides DESC"
            );
            ps.setInt(1, 1);
            ResultSet rs = ps.executeQuery();
            int i = -1;
            while (rs.next()) {
                i++;
                if (i < top) {
                    r += rs.getString("name") + ": #b" + rs.getInt("suicides") + "#k\r\n";
                } else {
                    break;
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    public String getParagonLevelRanking(int top) {
        String r = "";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM characters WHERE totalparagonlevel > ? && gm < 1 ORDER BY totalparagonlevel DESC"
            );
            ps.setInt(1, 121);
            ResultSet rs = ps.executeQuery();
            int i = -1;
            while (rs.next()) {
                i++;
                if (i < top) {
                    int totalparagonlevel = rs.getInt("totalparagonlevel");
                    r += rs.getString("name") + ": #b" + totalparagonlevel + "#k\r\n";
                } else {
                    break;
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    /*
    public String getDeathRankingByLevel(int level) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM pastlives WHERE level >= ? ORDER BY characterid"
            );
            ps.setInt(1, level);
            ResultSet rs = ps.executeQuery();
            Map<Integer, Integer> idstodeaths = new LinkedHashMap<>();
            while (rs.next()) {
                int id = rs.getInt("characterid");
                int lvl = rs.getInt("level");
                if ()
                temppastlife.add(rs.getInt("level"));
                temppastlife.add(rs.getInt("job"));
                temppastlife.add(rs.getInt("lastdamagesource"));
                ret.pastlives.add(temppastlife);
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    */

    public String getSkillNameById(int skillid) {
        return SkillFactory.getSkillName(skillid);
    }

    public void warpParty(int mapId) {
        warpParty(mapId, 0, 0);
    }

    public void warpParty(int mapId, int exp, int meso) {
        for (MaplePartyCharacter chr_ : getPlayer().getParty().getMembers()) {
            if (chr_ == null || !chr_.isOnline()) continue;
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr_.getName());
            if (curChar == null || curChar.isDead() || curChar.getMapId() == 100) continue;
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null) ||
                    curChar.getEventInstance() == getPlayer().getEventInstance()) {
                if ((curChar.getPartyQuest() == null && getPlayer().getPartyQuest() == null) ||
                        curChar.getPartyQuest() == getPlayer().getPartyQuest()) {
                    curChar.changeMap(mapId);
                    if (exp > 0) {
                        curChar.gainExp(exp, true, false, true);
                    }
                    if (meso > 0) {
                        curChar.gainMeso(meso, true);
                    }
                }
            }
        }
    }

    public int itemQuantity(int itemid) {
        return getPlayer().getItemQuantity(itemid, false);
    }

    public MapleSquad createMapleSquad(MapleSquadType type) {
        final MapleSquad squad = new MapleSquad(c.getChannel(), getPlayer());
        if (getSquadState(type) == 0) {
            c.getChannelServer().addMapleSquad(squad, type);
        } else {
            return null;
        }
        return squad;
    }

    public MapleCharacter getSquadMember(MapleSquadType type, int index) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        return squad != null ? squad.getMembers().get(index) : null;
    }

    public int getSquadState(MapleSquadType type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        return squad != null ? squad.getStatus() : 0;
    }

    public void setSquadState(MapleSquadType type, int state) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) squad.setStatus(state);
    }

    public boolean checkSquadLeader(MapleSquadType type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        return squad != null && squad.getLeader().getId() == getPlayer().getId();
    }

    public void removeMapleSquad(MapleSquadType type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null && squad.getLeader().getId() == getPlayer().getId()) {
            squad.clear();
            c.getChannelServer().removeMapleSquad(squad, type);
        }
    }

    public int numSquadMembers(MapleSquadType type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        return squad != null ? squad.getSquadSize() : 0;
    }

    public boolean isSquadMember(MapleSquadType type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        return squad.containsMember(getPlayer());
    }

    public void addSquadMember(MapleSquadType type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.addMember(getPlayer());
        }
    }

    public void removeSquadMember(MapleSquadType type, MapleCharacter member, boolean ban) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.banMember(member, ban);
        }
    }

    public void removeSquadMember(MapleSquadType type, int index, boolean ban) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            MapleCharacter player = squad.getMembers().get(index);
            squad.banMember(player, ban);
        }
    }

    public boolean canAddSquadMember(MapleSquadType type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        return squad != null && !squad.isBanned(getPlayer());
    }

    public void warpSquadMembers(MapleSquadType type, int mapId) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        final MapleMap map = c.getChannelServer().getMapFactory().getMap(mapId);
        if (squad != null && checkSquadLeader(type)) {
            for (MapleCharacter member : squad.getMembers()) {
                member.changeMap(map, map.getPortal(0));
            }
        }
    }

    public String searchItem(String item) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        StringBuilder message = new StringBuilder("Choose the item you want:");
        getPlayer().getMap().broadcastMessage(
            getPlayer(),
            MaplePacketCreator.showJobChange(getPlayer().getId()),
            false
        );
        for (Map.Entry<Integer, String> itemEntry : ii.getAllItems().entrySet()) {
            if (itemEntry.getValue().toLowerCase().contains(item.toLowerCase())) {
                message.append("\r\n#L")
                       .append(itemEntry.getKey())
                       .append("##i")
                       .append(itemEntry.getKey())
                       .append("# - #b")
                       .append(itemEntry.getValue())
                       .append("#k#l");
            }
        }
        if (!message.toString().contains("#L")) {
            return "No items found.";
        }
        return message.toString();
    }

    public int makeRing(String partner, int ringId) {
        return makeRing(getCharByName(partner), ringId);
    }

    public int makeRing(MapleCharacter partner, int ringId) {
        return MapleRing.createRing(ringId, getPlayer(), partner);
    }

    public void resetReactors() {
        getPlayer().getMap().resetReactors();
    }

    public void displayGuildRanks() {
        MapleGuild.displayGuildRanks(getClient(), npc);
    }

    public boolean sendMessage(String recipient, String message) {
        MapleCharacter chr_ = getCharByName(recipient);
        if (chr_ != null) {
            chr_.dropMessage(6, getPlayer().getName() + ": " + message);
            return true;
        }
        return false;
    }

    public void gainFame(int amount) {
        getPlayer().addFame(amount);
        if (amount > 0) {
            getPlayer().dropMessage(5, "You have gained " + amount + " fame.");
        } else {
            getPlayer().dropMessage(5, "You have lost " + amount + " fame.");
        }
    }

    public void maxSkills() {
        getPlayer().maxAllSkills();
    }

    public int getSkillLevel(int skillid) {
        return getPlayer().getSkillLevel(SkillFactory.getSkill(skillid));
    }

    public void giveBuff(int skillid) {
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        MapleStatEffect statEffect = mii.getItemEffect(skillid);
        statEffect.applyTo(getPlayer());
    }

    public int partyMembersInMap() {
        int inMap = 0;
        for (MapleCharacter char2 : getPlayer().getMap().getCharacters()) {
            if (char2.getParty() == getPlayer().getParty()) inMap++;
        }
        return inMap;
    }

    public void modifyNx(int amount) {
        getPlayer().modifyCSPoints(1, amount);
        if (amount > 0) {
            getPlayer().dropMessage(5, "You have gained " + amount + " NX.");
        } else {
            getPlayer().dropMessage(5, "You have lost " + amount + " NX.");
        }
    }

    public int getNx(int type) {
        return getPlayer().getCSPoints(type);
    }

    public boolean buyWithNx(int amount) {
        if (getNx(1) >= amount) {
            getPlayer().modifyCSPoints(1, -amount);
            getPlayer().dropMessage(5, "Your account has been charged for " + amount + " NX.");
            return true;
        } else if (getNx(2) >= amount) {
            getPlayer().modifyCSPoints(2, -amount);
            getPlayer().dropMessage(5, "Your account has been charged for " + amount + " NX.");
            return true;
        } else if (getNx(3) >= amount) {
            getPlayer().modifyCSPoints(3, -amount);
            getPlayer().dropMessage(5, "Your account has been charged for " + amount + " NX.");
            return true;
        }
        return false;
    }

    public int getTime(String type) {
        Calendar cal = Calendar.getInstance();
        if (type.startsWith("d")) {
            return cal.get(Calendar.DAY_OF_WEEK);
        } else if (type.startsWith("h")) {
            return cal.get(Calendar.HOUR_OF_DAY);
        } else if (type.startsWith("m")) {
            return cal.get(Calendar.MINUTE);
        } else if (type.startsWith("s")) {
            return cal.get(Calendar.SECOND);
        }
        return -1; // Wrong input
    }

    public void addBuddyCapacity(int capacity) {
        getPlayer().addBuddyCapacity(capacity);
    }

    public void clearKeys() {
        getPlayer().setDefaultKeyMap();
    }

    public void scheduleWarp(int delay, int mapid) {
        final int fmapid = mapid;
        TimerManager.getInstance().schedule(() -> getPlayer().changeMap(fmapid), delay * 1000L);
    }

    public void startClock(int limit, int endMap) {
        getPlayer().getMap().addMapTimer(limit, endMap);
    }

    public MapleCharacter getCharByName(String name) {
        try {
            return c.getChannelServer().getPlayerStorage().getCharacterByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    public void warpAllInMap(int mapid) {
        warpAllInMap(mapid, 0);
    }

    public void warpAllInMap(int mapid, int portal) {
        for (MapleCharacter mch : getPlayer().getMap().getCharacters()) {
            if (mch.getEventInstance() != null) {
                mch.getEventInstance().unregisterPlayer(mch);
            }
            mch.changeMap(mapid, portal);
        }
    }

    public boolean createMarriage(String partnerName) {
        MapleCharacter partner = getCharByName(partnerName);
        if (partner == null) return false;
        partner.setMarried(true);
        getPlayer().setMarried(true);
        partner.setPartnerId(getPlayer().getId());
        getPlayer().setPartnerId(partner.getId());
        if (partner.getGender() > 0) {
            return Marriage.createMarriage(getPlayer(), partner);
        }
        return Marriage.createMarriage(partner, getPlayer());
    }

    public boolean createEngagement(String partnerName) {
        MapleCharacter partner = getCharByName(partnerName);
        if (partner == null) return false;
        if (partner.getGender() > 0) {
            return Marriage.createEngagement(getPlayer(), partner);
        }
        return Marriage.createEngagement(partner, getPlayer());
    }

    public void divorceMarriage() {
        getPlayer().setPartnerId(0);
        getPlayer().setMarried(false);
        Marriage.divorceMarriage(getPlayer());
    }

    public void changeKeyBinding(int key, byte type, int action) {
        MapleKeyBinding newbinding = new MapleKeyBinding(type, action);
        getPlayer().changeKeybinding(key, newbinding);
    }

    /**
     * @param id ID of the equipment item.
     * @return The item, as an instance of {@link Equip}.
     */
    public Equip getEquipById(int id) {
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(id);
        return (Equip) getPlayer().getInventory(type).findById(id);
    }

    public int getNpcTalkTimes() {
        return NPCScriptManager.getInstance().getNpcTalkTimes(getPlayer().getId(), npc);
    }

    public void setNpcTalkTimes(int amount) {
        NPCScriptManager.getInstance().setNpcTalkTimes(getPlayer().getId(), npc, amount);
    }

    public int talkedTimesByNpc() {
        return NPCScriptManager.getInstance().talkedTimesByNpc(npc);
    }

    public boolean makeProItem(int id, int hardcore) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem item = ii.getEquipById(id);
        MapleInventoryType type = ii.getInventoryType(id);
        if (type.equals(MapleInventoryType.EQUIP)) {
            MapleInventoryManipulator.addFromDrop(c, ii.hardcoreItem((Equip) item, (short) hardcore));
            return true;
        } else {
            return false;
        }
    }

    public boolean isGuest() {
        return c.isGuest();
    }

    public void broadcastMessage(int type, String message) {
        try {
            getPlayer()
                .getClient()
                .getChannelServer()
                .getWorldInterface()
                .broadcastMessage(null, MaplePacketCreator.serverNotice(type, message).getBytes());
        } catch (RemoteException re) {
            c.getChannelServer().reconnectWorld();
        }
    }

    public void setClan(int set) {
        getPlayer().setClan(set);
        try {
            getPlayer()
                .getClient()
                .getChannelServer()
                .getWorldInterface()
                .broadcastToClan(
                    (getPlayer().getName() + " has entered the clan! Give them a nice welcome.")
                        .getBytes(),
                    set
                );
        } catch (RemoteException re) {
            c.getChannelServer().reconnectWorld();
        }
        c.getChannelServer().addToClan(getPlayer());
    }

    public String getAllOnlineNamesFromClan(int set) {
        StringBuilder sb = new StringBuilder();
        for (MapleCharacter names : c.getChannelServer().getClanHolder().getAllOnlinePlayersFromClan(set)) {
            sb.append(names.getName()).append("\r\n");
        }
        return sb.toString();
    }

    public String getAllOfflineNamesFromClan(int set) {
        StringBuilder sb = new StringBuilder();
        for (String names : c.getChannelServer().getClanHolder().getAllOfflinePlayersFromClan(set)) {
            sb.append(names).append("\r\n");
        }
        return sb.toString();
    }

    public int getOfflineClanCount(int clan) {
        return ClanHolder.countOfflineByClan(clan);
    }

    public int getOnlineClanCount(int clan) {
        try {
            return c.getChannelServer().getWorldInterface().onlineClanMembers(clan);
        } catch (RemoteException re) {
            c.getChannelServer().reconnectWorld();
        }
        return -1;
    }

    public String getJobById(int id) {
        return MapleJob.getJobName(id);
    }

    public List<MapleCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new ArrayList<>();
        for (ChannelServer channel : ChannelServer.getAllInstances()) {
            for (MapleCharacter p : channel.getPartyMembers(getPlayer().getParty())) {
                if (p != null) {
                    chars.add(p);
                }
            }
        }
        return chars;
    }

    public MapleCharacter getSender() {
        return chr;
    }

    public boolean hasTemp() {
        return !getPlayer().hasMerchant() && getPlayer().tempHasItems();
    }

    public void giveBuff(int buff, int level) {
        SkillFactory.getSkill(buff).getEffect(level).applyTo(getPlayer());
    }

    public void giveDebuff(int debuff, int level) {
        MobSkill ms = MobSkillFactory.getMobSkill(debuff, level);
        MapleDisease disease;
        switch (debuff) {
            case 120:
                disease = MapleDisease.SEAL;
                break;
            case 121:
                disease = MapleDisease.DARKNESS;
                break;
            case 122:
                disease = MapleDisease.WEAKEN;
                break;
            case 123:
                disease = MapleDisease.STUN;
                break;
            case 124: // Curse
                disease = MapleDisease.CURSE;
                break;
            case 125:
                disease = MapleDisease.POISON;
                break;
            case 126: // Slow
                disease = MapleDisease.SLOW;
                break;
            case 128: // Seduce
                disease = MapleDisease.SEDUCE;
                break;
            default:
                System.err.println(
                    "Failed to apply debuff of skill ID " +
                        debuff +
                        " and skill level " +
                        level +
                        " to player " +
                        getPlayer().getName() +
                        " from NPC with ID " +
                        npc +
                        ". Function: NPCConversationManager#giveDebuff"
                );
                return;
        }
        getPlayer().giveDebuff(disease, ms);
    }

    public String equipList(final int... leaveBehind_) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final StringBuilder str = new StringBuilder();
        final MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        final List<IItem> equipList = new ArrayList<>(equip.list());
        final Set<Integer> leaveBehind =
            IntStream
                .of(leaveBehind_)
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));

        // This sorts the items by their position in the inventory.
        equipList.sort(Comparator.comparingInt(IItem::getPosition));

        for (final IItem equipItem : equipList) {
            if (equipItem.getItemId() / 10000 == 111) continue; // Can't keep rings
            if (!leaveBehind.contains((int) equipItem.getPosition())) {
                if (!ii.isCash(equipItem.getItemId())) {
                    str.append("#L")
                       .append(equipItem.getPosition())
                       .append("##v")
                       .append(equipItem.getItemId())
                       .append("##l");
                }
            }
        }

        return str.toString();
    }

    public String plainEquipList() {
        return plainEquipList(4);
    }

    public String plainEquipList(int itemsPerLine) {
        return plainInventoryList(MapleInventoryType.EQUIP, itemsPerLine);
    }

    public String plainInventoryList(MapleInventoryType type) {
        return plainInventoryList(type, 4);
    }

    public String plainInventoryList(MapleInventoryType type, int itemsPerLine) {
        StringBuilder str = new StringBuilder();
        MapleInventory inv = c.getPlayer().getInventory(type);
        List<IItem> itemList = new ArrayList<>(inv.list());

        // This sorts the items by their position in the inventory
        itemList.sort(Comparator.comparingInt(IItem::getPosition));

        int itemsInLine = 0;
        for (IItem item : itemList) {
            str.append("#L")
               .append(item.getPosition())
               .append("##i")
               .append(item.getItemId())
               .append("##l");
            itemsInLine++;
            if (itemsPerLine > 0 && itemsInLine >= itemsPerLine) {
                str.append("\r\n");
                itemsInLine = 0;
            }
        }

        return str.toString();
    }

    public void sellSlotRange(byte start, byte end) {
        sellSlotRange(start, end, MapleInventoryType.EQUIP);
    }

    public void sellSlotRange(byte start, byte end, MapleInventoryType type) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleInventory inv = c.getPlayer().getInventory(type);
        double price;
        IItem item;
        int recvMesos;
        for (byte i = start; i <= end; ++i) {
            item = inv.getItem(i);
            if (item != null) {
                int itemId = item.getItemId();
                c.getPlayer().addBuyBack(item, item.getQuantity());
                int realQty = ii.isThrowingStar(itemId) || ii.isBullet(itemId) ? 1 : item.getQuantity();
                MapleInventoryManipulator.removeFromSlot(
                    c,
                    MapleItemInformationProvider.getInstance().getInventoryType(itemId),
                    i,
                    (short) 1,
                    false
                );
                price = ii.getPrice(itemId);
                recvMesos = (int) Math.max(Math.ceil(price), 0.0d);
                if (price >= 0.0d && recvMesos > 0) {
                    c.getPlayer().gainMeso(recvMesos * realQty, true);
                }
            }
        }
    }

    public String cashEquipList() {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final StringBuilder str = new StringBuilder();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        MapleInventory cash = c.getPlayer().getInventory(MapleInventoryType.CASH);

        equip.list()
             .stream()
             .sorted()
             .map(IItem::getItemId)
             .filter(ii::isCash)
             .forEachOrdered(i -> str.append("#L").append(i).append("##v").append(i).append("##l "));

        if (str.length() > 0) str.append("\r\n\r\n");

        cash.list()
            .stream()
            .sorted()
            .map(IItem::getItemId)
            .forEachOrdered(i -> str.append("#L").append(i).append("##v").append(i).append("##l "));

        return str.toString();
    }

    public void clearItems(final int... leaveBehind_) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        final MapleInventory use = getPlayer().getInventory(MapleInventoryType.USE);
        final MapleInventory etc = getPlayer().getInventory(MapleInventoryType.ETC);
        final Set<Integer> leaveBehind =
            IntStream
                .of(leaveBehind_)
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
        final List<IItem> cleared = new LinkedList<>(); // Linked list for a damn reason
        final Set<Integer> keep = new HashSet<Integer>() {{
            add(1002419); /* Mark of the Beta */ add(1002475); /* Mark of the Gamma */
        }};
        final Set<Integer> cashRemove = new HashSet<Integer>() {{
            add(1112500); /* Ring of Transcendence */ add(1112501); /* Ring of David */
        }};

        int i = 0;
        final byte[] equipSlots = new byte[equip.list().size()];
        for (final IItem equipItem : equip.list()) {
            if (!leaveBehind.contains((int) equipItem.getPosition())) {
                if (
                    (!ii.isCash(equipItem.getItemId()) ||
                        cashRemove.contains(equipItem.getItemId())) &&
                    !keep.contains(equipItem.getItemId())
                ) {
                    equipSlots[i] = equipItem.getPosition();
                    i++;
                }
            }
        }
        for (byte j = 0; j < i; ++j) {
            IItem item = equip.getItem(equipSlots[j]);
            cleared.add(item.copy());
            MapleInventoryManipulator.removeFromSlot(
                c,
                ii.getInventoryType(item.getItemId()),
                equipSlots[j],
                (short) 1,
                false
            );
        }

        i = 0;
        final byte[] useslots = new byte[use.list().size()];
        for (final IItem useitem : use.list()) {
            useslots[i] = useitem.getPosition();
            i++;
        }
        for (byte j = 0; j < i; ++j) {
            IItem item = use.getItem(useslots[j]);
            cleared.add(item.copy());
            MapleInventoryManipulator.removeFromSlot(
                c,
                ii.getInventoryType(item.getItemId()),
                useslots[j],
                item.getQuantity(),
                false
            );
        }

        i = 0;
        final byte[] etcslots = new byte[etc.list().size()];
        for (final IItem etcitem : etc.list()) {
            etcslots[i] = etcitem.getPosition();
            i++;
        }
        for (byte j = 0; j < i; ++j) {
            IItem item = etc.getItem(etcslots[j]);
            cleared.add(item.copy());
            MapleInventoryManipulator.removeFromSlot(
                c,
                ii.getInventoryType(item.getItemId()),
                etcslots[j],
                item.getQuantity(),
                false
            );
        }

        if (!DeathLogger.logItems(cleared, c)) {
            System.err.println(
                "There was an error logging " +
                    getPlayer().getName() +
                    "'s items lost on death."
            );
        }
    }

    public void stripEquips() {
        getPlayer().unequipEverything();
    }

    public void resetMaxHpMp() {
        final MapleCharacter p = getPlayer();
        p.setHp(50);
        p.updateSingleStat(MapleStat.HP, 50);
        p.setMp(5);
        p.updateSingleStat(MapleStat.MP, 5);
        p.setMaxHp(50);
        p.updateSingleStat(MapleStat.MAXHP, 50);
        p.setMaxMp(5);
        p.updateSingleStat(MapleStat.MAXMP, 5);
    }

    public void spawnMonster(int mobid) {
        getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(mobid), getPlayer().getPosition());
    }

    public void spawnMonsterInMap(int mobid) {
        spawnMonsterInMap(mobid, getPlayer().getMapId());
    }

    public void spawnMonsterInMap(int mobid, int mapid) {
        final MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        map.spawnMonster(MapleLifeFactory.getMonster(mobid));
    }

    public void spawnMonsterInMap(MapleMonster mob, int mapid) {
        final MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        map.spawnMonster(mob);
    }

    public void spawnMonsterInMapAtPos(MapleMonster mob, int mapid, int x, int y) {
        final MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        map.spawnMonsterOnGroudBelow(mob, new Point(x, y));
    }

    public MapleMonster getMonster(int mobid) {
        return MapleLifeFactory.getMonster(mobid);
    }

    public void spawnHT() {
        final MapleMonster ht = MapleLifeFactory.getMonster(8810026);
        final MapleMap map = c.getChannelServer().getMapFactory().getMap(240060200);
        map.spawnMonsterOnGroudBelow(ht, new Point(89, 290));
        map.killMonster(ht, getPlayer(), false);
        map.broadcastMessage(
            MaplePacketCreator.serverNotice(0, "As the cave shakes and rattles, here comes Horntail.")
        );
    }

    public String listConsume(char initial) {
        final StringBuilder sb = new StringBuilder();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final List<Pair<Integer, String>> consumeList = ii.getAllConsume(initial);

        if (consumeList.isEmpty()) {
            return "There are no use items with the selected initial letter.";
        }

        consumeList.sort((o1, o2) -> o1.getRight().compareToIgnoreCase(o2.getRight()));

        for (Pair<Integer, String> itempair : consumeList) {
            sb.append("#L").append(itempair.getLeft()).append('#');
            sb.append("#i").append(itempair.getLeft()).append("# ");
            sb.append(itempair.getRight()).append("#l\r\n");
        }
        return sb.toString();
    }

    public String listEqp(char initial) {
        StringBuilder sb = new StringBuilder();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<Pair<Integer, String>> eqpList = ii.getAllEqp(initial);

        if (eqpList.isEmpty()) {
            return "There are no equipment items with the selected initial letter.";
        }

        eqpList.sort((o1, o2) -> o1.getRight().compareToIgnoreCase(o2.getRight()));

        for (Pair<Integer, String> itempair : eqpList) {
            sb.append("#L").append(itempair.getLeft()).append('#');
            sb.append("#i").append(itempair.getLeft()).append("# ");
            sb.append(itempair.getRight()).append("#l\r\n");
        }
        return sb.toString();
    }

    public String listEtc(char initial) {
        StringBuilder sb = new StringBuilder();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<Pair<Integer, String>> etcList = ii.getAllEtc(initial);

        if (etcList.isEmpty()) {
            return "There are no etc items with the selected initial letter.";
        }

        etcList.sort((o1, o2) -> o1.getRight().compareToIgnoreCase(o2.getRight()));

        for (Pair<Integer, String> itempair : etcList) {
            sb.append("#L").append(itempair.getLeft()).append('#');
            sb.append("#i").append(itempair.getLeft()).append("# ");
            sb.append(itempair.getRight()).append("#l\r\n");
        }
        return sb.toString();
    }

    public String whoDrops(int searchid) {
        final StringBuilder dropString_ = new StringBuilder();
        try {
            final Set<String> retMobs = new LinkedHashSet<>();
            final Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps =
                con.prepareStatement(
                    "SELECT monsterid FROM monsterdrops WHERE itemid = ?"
                );
            ps.setInt(1, searchid);
            final ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                final int mobId = rs.getInt("monsterid");
                final MapleMonster mob = MapleLifeFactory.getMonster(mobId);
                if (mob != null) {
                    retMobs.add(mob.getName());
                }
            }
            rs.close();
            ps.close();
            if (!retMobs.isEmpty()) {
                for (final String singleRetMob : retMobs) {
                    dropString_.append(singleRetMob).append("\r\n");
                }
            } else {
                return "No mobs drop this item.";
            }
        } catch (SQLException sqle) {
            System.err.print("NPCConversationManager#whoDrops failed: " + sqle);
            dropString_
                .delete(0, dropString_.length())
                .append("NPCConversationManager#whoDrops failed:\r\n\r\n#r")
                .append(sqle)
                .append("#k");
        }
        return dropString_.toString();
    }

    public boolean canGetDailyPrizes() {
        boolean ret = true;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement charps =
                con.prepareStatement(
                    "SELECT id FROM characters WHERE accountid = ?"
                );
            charps.setInt(1, getPlayer().getAccountID());
            ResultSet charrs = charps.executeQuery();
            while (charrs.next()) {
                int charid = charrs.getInt("id");
                if (charid == getPlayer().getId()) {
                    continue;
                }
                PreparedStatement invps = con.prepareStatement(
                    "SELECT itemid FROM inventoryitems WHERE characterid = ?"
                );
                invps.setInt(1, charid);
                ResultSet invrs = invps.executeQuery();
                while (invrs.next()) {
                    int itemid = invrs.getInt("itemid");
                    if (itemid >= 3990010 && itemid <= 3990016) {
                        ret = false;
                        break;
                    }
                }
                invrs.close();
                invps.close();
            }
            charrs.close();
            charps.close();
        } catch (SQLException sqle) {
            System.err.print("NPCConversationManager#canGetDailyPrizes failed: " + sqle);
        }

        return ret;
    }

    public int dailyPrizeStatus() {
        Calendar validtime = Calendar.getInstance();
        validtime.setTime(getPlayer().getLastDailyPrize());
        validtime.add(Calendar.DATE, 1);
        Calendar currenttime = Calendar.getInstance();
        currenttime.setTimeInMillis(System.currentTimeMillis());
        if (validtime.compareTo(currenttime) > 0) {
            // Valid time is in the future
            if (
                validtime.get(Calendar.DAY_OF_MONTH) == currenttime.get(Calendar.DAY_OF_MONTH) &&
                validtime.get(Calendar.MONTH) == currenttime.get(Calendar.MONTH)
            ) {
                return 0;
            } else {
                return 1;
            }
        } else if (validtime.compareTo(currenttime) < 0) {
            // Valid time is in the past
            if (
                validtime.get(Calendar.DAY_OF_MONTH) == currenttime.get(Calendar.DAY_OF_MONTH) &&
                validtime.get(Calendar.MONTH) == currenttime.get(Calendar.MONTH)
            ) {
                return 0;
            } else {
                return -1;
            }
        } else {
            return 0;
        }
    }

    public int timeUntilNextPrize() {
        Calendar validtime = Calendar.getInstance();
        validtime.setTime(getPlayer().getLastDailyPrize());
        validtime.add(Calendar.DATE, 1);
        validtime.set(Calendar.HOUR_OF_DAY, 0);
        validtime.set(Calendar.MINUTE, 0);
        validtime.set(Calendar.SECOND, 0);
        validtime.set(Calendar.MILLISECOND, 0);
        long time = validtime.getTimeInMillis() - System.currentTimeMillis();
        time = time / (1000L * 60L * 60L);
        return (int) time;
    }

    public void updateLastDailyPrize() {
        Calendar currenttime = Calendar.getInstance();
        currenttime.setTimeInMillis(System.currentTimeMillis());
        getPlayer().setLastDailyPrize(currenttime.getTime());
    }

    public void revive() {
        final MapleCharacter player = getPlayer();
        player.setInvincible(true); // 10 second invincibility
        TimerManager.getInstance().schedule(() -> player.setInvincible(false), 10L * 1000L);
        player.setHp(player.getMaxHp(), false);
        player.setMp(player.getMaxMp());
        player.updateSingleStat(MapleStat.HP, player.getMaxHp());
        player.updateSingleStat(MapleStat.MP, player.getMaxMp());
        player.incrementDeathPenaltyAndRecalc(5);
        player.setExp(0);
        player.updateSingleStat(MapleStat.EXP, 0);
        player.getClient().getSession().write(MaplePacketCreator.getClock(0));
    }

    public boolean selfResurrect() {
        MapleCharacter p = getPlayer();
        ISkill resurrection = SkillFactory.getSkill(2321006);
        int resurrectionlevel = p.getSkillLevel(resurrection);
        if (resurrectionlevel > 0 && !p.skillIsCooling(2321006) && itemQuantity(4031485) > 0) {
            gainItem(4031485, (short) -1);
            p.setHp(p.getMaxHp(), false);
            p.setMp(p.getMaxMp());
            p.updateSingleStat(MapleStat.HP, p.getMaxHp());
            p.updateSingleStat(MapleStat.MP, p.getMaxMp());
            long cooldowntime = 3600000L - (180000L * (long) resurrectionlevel);
            p.giveCoolDowns(2321006, System.currentTimeMillis(), cooldowntime, true);
            p.incrementDeathPenaltyAndRecalc(5);
            p.setExp(0);
            p.updateSingleStat(MapleStat.EXP, 0);
            p.getClient().getSession().write(MaplePacketCreator.getClock(0));
            return true;
        } else {
            return false;
        }
    }

    public int getHiredMerchantMesos() {
        Connection con = DatabaseConnection.getConnection();
        int mesos;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT MerchantMesos FROM characters WHERE id = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            rs.next();
            mesos = rs.getInt("MerchantMesos");
            rs.close();
            ps.close();
            return mesos;
        } catch (SQLException sqle) {
            return 0;
        }
    }

    public void setHiredMerchantMesos(int set) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET MerchantMesos = ? WHERE id = ?");
            ps.setInt(1, set);
            ps.setInt(2, getPlayer().getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ignored) {
        }
    }

    public boolean getBetaTester() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT redeemed FROM betatesters WHERE name = ?");
            ps.setString(1, c.getAccountName());
            ResultSet rs = ps.executeQuery();
            int redeemed = -1;
            while (rs.next()) {
                redeemed = rs.getInt("redeemed");
            }
            rs.close();
            ps.close();
            if (redeemed == 0) {
                ps = con.prepareStatement("UPDATE betatesters SET redeemed = 1 WHERE name = ?");
                ps.setString(1, c.getAccountName());
                ps.executeUpdate();
                ps.close();
            }
            return redeemed == 0;
        } catch (SQLException sqle) {
            System.err.println("NPCConversationManager#getBetaTester failed: " + sqle);
            return false;
        }
    }

    public boolean getGammaTester() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT redeemed FROM gammatesters WHERE name = ?");
            ps.setString(1, c.getAccountName());
            ResultSet rs = ps.executeQuery();
            int redeemed = -1;
            while (rs.next()) {
                redeemed = rs.getInt("redeemed");
            }
            rs.close();
            ps.close();
            if (redeemed == 0) {
                ps = con.prepareStatement("UPDATE gammatesters SET redeemed = 1 WHERE name = ?");
                ps.setString(1, c.getAccountName());
                ps.executeUpdate();
                ps.close();
            }
            return redeemed == 0;
        } catch (SQLException sqle) {
            System.err.println("NPCConversationManager#getGammaTester failed: " + sqle);
            return false;
        }
    }

    public int betaTester() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT redeemed FROM betatesters WHERE name = ?");
            ps.setString(1, c.getAccountName());
            ResultSet rs = ps.executeQuery();
            int redeemed = -1;
            while (rs.next()) {
                redeemed = rs.getInt("redeemed");
            }
            rs.close();
            ps.close();
            return redeemed;
        } catch (SQLException sqle) {
            System.err.print("NPCConversationManager#betaTester failed: " + sqle);
            return -1;
        }
    }

    public int gammaTester() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT redeemed FROM gammatesters WHERE name = ?");
            ps.setString(1, c.getAccountName());
            ResultSet rs = ps.executeQuery();
            int redeemed = -1;
            while (rs.next()) {
                redeemed = rs.getInt("redeemed");
            }
            rs.close();
            ps.close();
            return redeemed;
        } catch (SQLException sqle) {
            System.err.print("NPCConversationManager#gammaTester failed: " + sqle);
            return -1;
        }
    }

    public void removeHiredMerchantItem(boolean tempItem, int itemId) {
        String table = tempItem ? "temp" : "";
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement(
                "DELETE FROM " + ("hiredmerchant" + table) + " WHERE itemid = ? AND ownerid = ? LIMIT 1"
            );
            ps.setInt(1, itemId);
            ps.setInt(2, getPlayer().getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException sqle) {
            System.err.print("NPCConversationManager#removeHiredMerchantItem failed: " + sqle);
        }
    }

    public boolean getHiredMerchantItems(boolean tempTable) {
        String table = tempTable ? "temp" : "";
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM " + ("hiredmerchant" + table) + " WHERE `ownerid` = ?"
            );
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") == 1) {
                    Equip spItem = new Equip(rs.getInt("itemid"), (byte) 0, -1);
                    spItem.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                    spItem.setLevel((byte) rs.getInt("level"));
                    spItem.setStr((short) rs.getInt("str"));
                    spItem.setDex((short) rs.getInt("dex"));
                    spItem.setInt((short) rs.getInt("int"));
                    spItem.setLuk((short) rs.getInt("luk"));
                    spItem.setHp((short) rs.getInt("hp"));
                    spItem.setMp((short) rs.getInt("mp"));
                    spItem.setWatk((short) rs.getInt("watk"));
                    spItem.setMatk((short) rs.getInt("matk"));
                    spItem.setWdef((short) rs.getInt("wdef"));
                    spItem.setMdef((short) rs.getInt("mdef"));
                    spItem.setAcc((short) rs.getInt("acc"));
                    spItem.setAvoid((short) rs.getInt("avoid"));
                    spItem.setHands((short) rs.getInt("hands"));
                    spItem.setSpeed((short) rs.getInt("speed"));
                    spItem.setJump((short) rs.getInt("jump"));
                    spItem.setOwner(rs.getString("owner"));
                    if (!getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                        MapleInventoryManipulator.addFromDrop(c, spItem, true);
                        removeHiredMerchantItem(tempTable, spItem.getItemId());
                    } else {
                        rs.close();
                        ps.close();
                        return false;
                    }
                } else {
                    Item spItem = new Item(
                        rs.getInt("itemid"),
                        (byte) 0,
                        (short) rs.getInt("quantity")
                    );
                    spItem.setOwner(rs.getString("owner"));
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    MapleInventoryType type = ii.getInventoryType(spItem.getItemId());
                    if (!getPlayer().getInventory(type).isFull()) {
                        MapleInventoryManipulator.addFromDrop(c, spItem, true);
                        removeHiredMerchantItem(tempTable, spItem.getItemId());
                    } else {
                        rs.close();
                        ps.close();
                        return false;
                    }
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return false;
        }
        return true;
    }
}
