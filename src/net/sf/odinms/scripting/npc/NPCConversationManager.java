package net.sf.odinms.scripting.npc;

import net.sf.odinms.client.*;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
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
import java.io.File;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NPCConversationManager extends AbstractPlayerInteraction {
    private final MapleClient c;
    private final int npc;
    private String fileName = null;
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
            getPlayer().dropMessage(6, "!!! This NPC is broken. @gm someone and tell them you got this message. If no one is online, just remember for later. !!!");
            dispose();
        } else {
            getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 4, text, ""));
        }
    }

    public void sendStyle(String text, int styles[]) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkStyle(npc, text, styles));
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkNum(npc, text, def, min, max));
    }

    public void sendGetText(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkText(npc, text));
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return this.getText;
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
        Map<ISkill, MapleCharacter.SkillEntry> skills = getPlayer().getSkills();
        for (Entry<ISkill, MapleCharacter.SkillEntry> skill : skills.entrySet()) {
            getPlayer().changeSkillLevel(skill.getKey(), 0, 0);
        }
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

    @SuppressWarnings("static-access")
    public void setSkin(int color) {
        getPlayer().setSkinColor(c.getPlayer().getSkinColor().getById(color));
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public int getStory() {
        return getPlayer().getStory();
    }

    public int getStoryPoints() {
        return getPlayer().getStoryPoints();
    }

    public int getOffenseStory() {
        return getPlayer().getOffenseStory();
    }

    public int getBuffStory() {
        return getPlayer().getBuffStory();
    }

    public void startCQuest(int id) {
        getPlayer().getCQuest().loadQuest(id);
        getPlayer().setQuestId(id);
        getPlayer().resetQuestKills();
        if (id == 0) {
            getPlayer().sendHint("#eQuest canceled.");
        } else {
            getPlayer().sendHint("#eQuest start: " + getPlayer().getCQuest().getTitle());
        }
    }

    public boolean onQuest() {
        return getPlayer().getQuestId() > 0;
    }

    public boolean onQuest(int questid) {
        return getPlayer().getQuestId() == questid;
    }

    public boolean canComplete() {
        return getPlayer().canComplete();
    }

    public String selectQuest(int questid, String msg) {
        String intro = msg + "\r\n\r\n#fUI/UIWindow.img/QuestIcon/3/0#\r\n#L0#";
        String selection = "#k[" + (getPlayer().getQuestId() == 0 ? "#rAvailable" : (getPlayer().getQuestId() == questid && !canComplete()) ? "#dIn progress" : "#gComplete") + "#k]";
        return intro + selection + " #e" + getPlayer().getCQuest().loadTitle(questid);
    }

    public String showReward(String msg) {
        final StringBuilder sb = new StringBuilder();
        sb.append(msg);
        sb.append("\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n\r\n");
        sb.append("#fUI/UIWindow.img/QuestIcon/8/0#  ").append(MapleCharacter.makeNumberReadable(getPlayer().getCQuest().getExpReward())).append("\r\n");
        sb.append("#fUI/UIWindow.img/QuestIcon/7/0#  ").append(MapleCharacter.makeNumberReadable(getPlayer().getCQuest().getMesoReward())).append("\r\n");
        getPlayer().getCQuest().readItemRewards().entrySet().forEach(reward ->
            sb.append("\r\n#i").append(reward.getKey()).append("#  ").append(MapleCharacter.makeNumberReadable(reward.getValue()))
        );
        return sb.toString();
    }

    public void rewardPlayer(int story, int storypoints) {
        MapleCQuests q = getPlayer().getCQuest();
        int questId = q.getId();
        getPlayer().addStory(story);
        getPlayer().addStoryPoints(storypoints);
        gainExp(q.getExpReward());
        gainMeso(q.getMesoReward());
        q.readItemRewards().entrySet().forEach(reward -> gainItem(reward.getKey(), reward.getValue().shortValue()));
        q.readItemsToCollect()
         .entrySet()
         .forEach(collected -> gainItem(collected.getKey(), (short) -collected.getValue().getLeft()));
        c.getSession().write(MaplePacketCreator.playSound("Dojan/clear"));
        c.getSession().write(MaplePacketCreator.showEffect("dojang/end/clear"));
        startCQuest(0);
        if (completedAllQuests() && (questId / 1000 == 1 || questId / 1000 == 2) && !c.getPlayer().completedAllQuests()) {
            MapleInventoryManipulator.addById(c, 3992027, (short) 1);
            getPlayer().dropMessage(6,
                  "Congratulations on finishing all of the IntransigentQuests! "
                + "For completing them all, you have been awarded with a single Red Candle. "
                + "At level 110+ you may use this candle to revive yourself once, and avoid permanent death."
            );
            c.getPlayer().setCompletedAllQuests(true);
        }
    }

    public void fourthRewardPlayer(int offensestory, int buffstory) {
        final MapleCQuests q = getPlayer().getCQuest();
        getPlayer().addOffenseStory(offensestory);
        getPlayer().addBuffStory(buffstory);
        gainExp(q.getExpReward());
        gainMeso(q.getMesoReward());
        q.readItemRewards().entrySet().forEach(reward -> gainItem(reward.getKey(), reward.getValue().shortValue()));
        q.readItemsToCollect()
         .entrySet()
         .forEach(collected -> gainItem(collected.getKey(), (short) -collected.getValue().getLeft()));
        c.getSession().write(MaplePacketCreator.playSound("Dojan/clear"));
        c.getSession().write(MaplePacketCreator.showEffect("dojang/end/clear"));
        startCQuest(0);
    }

    public void rewardPlayer() {
        rewardPlayer(1, 1);
    }

    public String showQuestProgress() {
        final MapleCQuests q = getPlayer().getCQuest();
        final StringBuilder sb = new StringBuilder();
        sb.append("Your quest info for:\r\n#e#r").append(q.getTitle()).append("#n#k\r\n");
        
        if (q.requiresMonsterTargets()) sb.append("\r\n#eMonster targets:#n \r\n");
        q.readMonsterTargets().entrySet().forEach(target ->
            sb.append(target.getValue().getRight())
              .append(": ")
              .append(getPlayer().getQuestKills(target.getKey()) >= target.getValue().getLeft() ? "#g" : "#r")
              .append(getPlayer().getQuestKills(target.getKey()))
              .append(" #k/ ")
              .append(target.getValue().getLeft())
              .append("\r\n")
        );
        sb.append("\r\n");

        if (q.requiresItemCollection()) sb.append("\r\n#eItems to collect:#n \r\n");
        q.readMonsterTargets().entrySet().forEach(toCollect ->
            sb.append(toCollect.getValue().getRight())
              .append(": ")
              .append(getPlayer().getQuestKills(toCollect.getKey()) >= toCollect.getValue().getLeft() ? "#g" : "#r")
              .append(getPlayer().getQuestKills(toCollect.getKey()))
              .append(" #k/ ")
              .append(toCollect.getValue().getLeft())
              .append("\r\n")
        );
        sb.append("\r\n");
        
        sb.append("#eQuest NPC: #n\r\n#d").append(q.getNPC()).append("#k\r\n");
        sb.append("#eQuest info: #n\r\n").append(q.getInfo());
        
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
                return "You haven't completed your task yet! You can look it up by typing #b@questinfo#k in the chat.\r\nDo you want to cancel this quest?";
        }
        return "";
    }

    public String showNextQuest() {
        String s = "";
        MapleCQuests q = new MapleCQuests();
        if (getPlayer().getCQuest().questExists(getPlayer().getStory() + 1000)) {
            q.loadQuest(getPlayer().getStory() + 1000);
            s += "#e" + q.getTitle() + "#n\r\nNPC contact: #b" + q.getNPC() + "#k";
        }
        if (getPlayer().getCQuest().questExists(getPlayer().getStoryPoints() + 2000)) {
            q.loadQuest(getPlayer().getStoryPoints() + 2000);
            if (s.length() > 1) {
                s += "\r\n\r\n";
            }
            s += "#e" + q.getTitle() + "#n\r\nNPC contact: #b" + q.getNPC() + "#k";
        }
        return s;
    }
    
    public boolean completedAllQuests() {
        if (getPlayer().getCQuest().questExists(getPlayer().getStory() + 1000)) {
            return false;
        } else {
            return !getPlayer().getCQuest().questExists(getPlayer().getStoryPoints() + 2000);
        }
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
        } else if (ii.isArrowForBow(itemid) || ii.isArrowForCrossBow(itemid) || ii.isBullet(itemid) || ii.isThrowingStar(itemid)) {
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
        if (getPlayer().getMapId() >= 1000 && getPlayer().getMapId() <= 1006) {
            if (getPlayer().getHp() > 0) {
                return true;
            }
        }
        return false;
    }
    
    public boolean canEnterMonsterTrial() {
        if (System.currentTimeMillis() - getPlayer().getLastTrialTime() >= (long) 7200000) {
            if (getPlayer().getLevel() >= 20) {
                return true;
            }
        }
        return false;
    }
    
    public int getMonsterTrialCooldown() {
        long timesincelast = System.currentTimeMillis() - getPlayer().getLastTrialTime();
        double inminutes = timesincelast / 60000.0;
        inminutes = Math.floor(inminutes);
        return 120 - (int) inminutes;
    }
    
    public int getMonsterTrialPoints() {
        return getPlayer().getMonsterTrialPoints();
    }
    
    public int getTier() {
        return getPlayer().getMonsterTrialTier();
    }
    
    public int getTierPoints(int tier) {
        if (tier < 1) {
            return 0;
        }
        return (int) (getTierPoints(tier - 1) + (10 * Math.pow(tier + 1, 2)) * (Math.floor(tier * 1.5) + 3));
    }
    
    public int calculateLevelGroup(int level) {
	    int lg = 0;
        if (level >= 20) {
            lg++;
        }
        if (level >= 26) {
            lg++;
        }
        if (level >= 31) {
            lg++;
        }
        if (level >= 41) {
            lg++;
        }
        if (level >= 51) {
            lg++;
        }
        if (level >= 71) {
            lg++;
        }
        if (level >= 91) {
            lg++;
        }
        if (level >= 101) {
            lg++;
        }
        if (level >= 121) {
            lg++;
        }
        if (level >= 141) {
            lg++;
        }
        if (level >= 161) {
            lg++;
        }
	    return lg;
    }
    
    public boolean enterMonsterTrial() {
        final int monsters[][] = { /* (In this array) 1st dimension: Corresponds to level group of player (-1 to adjust for level group not being zero-based)
                                                2nd dimension: Monster IDs for that level group as ints, with a digit added at the end of the decimal
                                                               representation corresponding to how many should be spawned. Thus the raw magnitudes of
                                                               these ints are ~1 order of magnitude larger than the actual IDs would be. */
            {22200002, 32200001, 95001692, 95001683}, // 20 - 25
            {95001693, 32200002, 32200011, 95001701}, // 26 - 30
            {32200012, 93000031, 42200011, 95001702, 95003281}, // 31 - 40
            {42200012, 95003281, 52200021, 95001771, 51201001, 95003301}, // 41 - 50
            {52200011, 95001771, 52200031, 95003341, 95001761, 61301011, 62200001, 62200011}, // 51 - 70
            {62200011, 52200012, 71304002, 71304012, 71304022, 93000121, 95001731, 95001741, 63000051, 72200001, 71103003, 90010041}, // 71 - 90
            {82200021, 82200001, 81301001, 72200021, 93000121, 95001731, 95001741, 94100141}, // 91 - 100
            {82200011, 94002051, 93001191, 93000391, 95003351, 94001201, 81700001, 81600001}, // 101 - 120
            {81500001, 93000281, 93000891, 93000901, 93000311, 93000321}, // 121 - 140
            {81800001, 81800011, 94005361, 85000011}, // 141 - 160
            {85200001, 94005751, 94000141, 94001211}  // 161+
        };
        MapleMap map;
        for (int mapid = 1000; mapid <= 1006; ++mapid) {
            map = c.getChannelServer().getMapFactory().getMap(mapid);
            if (map.playerCount() == 0) {
                map.killAllMonsters(false);
                getPlayer().setTrialReturnMap(getPlayer().getMapId());
                getPlayer().changeMap(map, map.getPortal(0));
                final int returnmapid = getPlayer().getTrialReturnMap();
                final MapleCharacter player = getPlayer();
                TimerManager tMan = TimerManager.getInstance();
                final Runnable endTrialTask = () -> {
                    if (player.getMapId() >= 1000 && player.getMapId() <= 1006) {
                        player.changeMap(returnmapid, 0);
                    }
                };
                getPlayer().getClient().getSession().write(MaplePacketCreator.getClock(30 * 60)); // 30 minutes
                int monsterchoices[] = monsters[calculateLevelGroup(getPlayer().getLevel()) - 1];
                int monsterchoice = monsterchoices[(int) Math.floor(Math.random() * monsterchoices.length)];
                int monsterid = (int) Math.floor((double) monsterchoice / 10.0);
                int monstercount = monsterchoice % 10;
                Point monsterspawnpoint = new Point(-336, -11); // (-336, 101)
                for (int i = 0; i < monstercount; ++i) {
                    try {
                        map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(monsterid), monsterspawnpoint);
                    } catch (NullPointerException npe) {
                        System.out.println("Player " + player.getName() + " booted from Monster Trials; monsterchoice, monsterid: " + monsterchoice + ", " + monsterid);
                        npe.printStackTrace();
                        tMan.schedule(() -> {
                            player.changeMap(returnmapid, 0);
                            player.dropMessage("There was an error loading your Monster Trial! Tell a GM about this and try again.");
                        }, 500);
                        return true;
                    }
                }
                tMan.schedule(endTrialTask, 30 * 60 * 1000); // 30 minutes
                getPlayer().setLastTrialTime(System.currentTimeMillis());
                return true;
            }
        }
        return false;
    }
    
    public boolean enterBossMap(int bossid) {
        int mapId = 3000;
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
                                   .filter(p -> p != null)
                                   .collect(Collectors.toList());
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
                    40 * 60 * 1000,
                    new Predicate<MapleCharacter>() {
                        @Override
                        public boolean test(MapleCharacter mc) {
                            return mc.getMapId() == 3000;
                        }
                    }
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
        if (getPlayer().getHp() > 0) {
            if (getPlayer().getMap().mobCount() < 1) {
                return true;
            }
        }
        return false;
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
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE deathcount > ? && gm < 1 ORDER BY deathcount DESC");
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
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE highestlevelachieved > ? && gm < 1 ORDER BY highestlevelachieved DESC");
            ps.setInt(1, 120);
            ResultSet rs = ps.executeQuery();
            int i = -1;
            while (rs.next()) {
                i++;
                if (i < top) {
                    r += rs.getString("name") + ": #b" + rs.getInt("highestlevelachieved") + "#k\r\n";
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
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE suicides > ? && gm < 1 ORDER BY suicides DESC");
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
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE totalparagonlevel > ? && gm < 1 ORDER BY totalparagonlevel DESC");
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
            PreparedStatement ps = con.prepareStatement("SELECT * FROM pastlives WHERE level >= ? ORDER BY characterid");
            ps.setInt(1, level);
            ResultSet rs = ps.executeQuery();
            Map<Integer, Integer> idstodeaths = new HashMap<>();
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
            if ((curChar.getEventInstance() == null && c.getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                if ((curChar.getPartyQuest() == null && c.getPlayer().getPartyQuest() == null) || curChar.getPartyQuest() == getPlayer().getPartyQuest()) {
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
        MapleSquad squad = new MapleSquad(c.getChannel(), getPlayer());
        if (getSquadState(type) == 0) {
            c.getChannelServer().addMapleSquad(squad, type);
        } else {
            return null;
        }
        return squad;
    }

    public MapleCharacter getSquadMember(MapleSquadType type, int index) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        MapleCharacter ret = null;
        if (squad != null) {
            ret = squad.getMembers().get(index);
        }
        return ret;
    }

    public int getSquadState(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            return squad.getStatus();
        } else {
            return 0;
        }
    }

    public void setSquadState(MapleSquadType type, int state) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.setStatus(state);
        }
    }

    public boolean checkSquadLeader(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        return squad != null && squad.getLeader().getId() == getPlayer().getId();
    }

    public void removeMapleSquad(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            if (squad.getLeader().getId() == getPlayer().getId()) {
                squad.clear();
                c.getChannelServer().removeMapleSquad(squad, type);
            }
        }
    }

    public int numSquadMembers(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        int ret = 0;
        if (squad != null) {
            ret = squad.getSquadSize();
        }
        return ret;
    }

    public boolean isSquadMember(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        boolean ret = false;
        if (squad.containsMember(getPlayer())) {
            ret = true;
        }
        return ret;
    }

    public void addSquadMember(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.addMember(getPlayer());
        }
    }

    public void removeSquadMember(MapleSquadType type, MapleCharacter member, boolean ban) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.banMember(member, ban);
        }
    }

    public void removeSquadMember(MapleSquadType type, int index, boolean ban) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            MapleCharacter player = squad.getMembers().get(index);
            squad.banMember(player, ban);
        }
    }

    public boolean canAddSquadMember(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        return squad != null && !squad.isBanned(getPlayer());
    }

    public void warpSquadMembers(MapleSquadType type, int mapId) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapId);
        if (squad != null) {
            if (checkSquadLeader(type)) {
                for (MapleCharacter member : squad.getMembers()) {
                    member.changeMap(map, map.getPortal(0));
                }
            }
        }
    }

    public String searchItem(String item) {
        StringBuilder message = new StringBuilder("Choose the item you want:");
        getPlayer().getMap().broadcastMessage(getPlayer(), MaplePacketCreator.showJobChange(getPlayer().getId()), false);
        for (Pair<Integer, String> itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
            if (itemPair.getRight().toLowerCase().contains(item.toLowerCase())) {
                message.append("\r\n#L").append(itemPair.getLeft()).append("##i").append(itemPair.getLeft()).append("# - #b").append(itemPair.getRight()).append("#k#l");
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
        return net.sf.odinms.client.MapleRing.createRing(ringId, getPlayer(), partner);
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
            getPlayer().dropMessage(1, "You have gained " + amount + " fame.");
        } else {
            getPlayer().dropMessage(1, "You have lost " + amount + " fame.");
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
            if (char2.getParty() == getPlayer().getParty()) {
                inMap++;
            }
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
            getPlayer().modifyCSPoints(1, -1 * amount);
            getPlayer().dropMessage(5, "Your account has been charged for " + amount + " NX.");
            return true;
        } else if (getNx(2) >= amount) {
            getPlayer().modifyCSPoints(2, -1 * amount);
            getPlayer().dropMessage(5, "Your account has been charged for " + amount + " NX.");
            return true;
        } else if (getNx(3) >= amount) {
            getPlayer().modifyCSPoints(3, -1 * amount);
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
        return -1; // wrong input
    }

    public void addBuddyCapacity(int capacity) {
        getPlayer().addBuddyCapacity(capacity);
    }

    public void clearKeys() {
        getPlayer().setDefaultKeyMap();
    }

    public void scheduleWarp(int delay, int mapid) {
        final int fmapid = mapid;
        TimerManager.getInstance().schedule(() -> getPlayer().changeMap(fmapid), delay * 1000);
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

    public boolean createMarriage(String partner_) {
        MapleCharacter partner = getCharByName(partner_);
        if (partner == null) {
            return false;
        }
        partner.setMarried(true);
        getPlayer().setMarried(true);
        partner.setPartnerId(getPlayer().getId());
        getPlayer().setPartnerId(partner.getId());
        if (partner.getGender() > 0) {
            Marriage.createMarriage(getPlayer(), partner);
        } else {
            Marriage.createMarriage(partner, getPlayer());
        }
        return true;
    }

    public boolean createEngagement(String partner_) {
        MapleCharacter partner = getCharByName(partner_);
        if (partner == null) {
            return false;
        }
        if (partner.getGender() > 0) {
            Marriage.createEngagement(getPlayer(), partner);
        } else {
            Marriage.createEngagement(partner, getPlayer());
        }
        return true;
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

    public Equip getEquipById(int id) { // Can do getEquipById(2349823).setStr(545); etc.
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
            getPlayer().getClient().getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(type, message).getBytes());
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
    }

    public void setClan(int set) {
        getPlayer().setClan(set);
        try {
            getPlayer().getClient().getChannelServer().getWorldInterface().broadcastToClan((getPlayer().getName() + " has entered the clan! Give them a nice welcome.").getBytes(), set);
        } catch (RemoteException e) {
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
            for (MapleCharacter chr : channel.getPartyMembers(getPlayer().getParty())) {
                if (chr != null) {
                    chars.add(chr);
                }
            }
        }
        return chars;
    }

    public MapleCharacter getSender() {
        return this.chr;
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
                System.out.println("Failed to apply debuff of skill ID " + debuff + " and skill level " + level + " to player " + getPlayer().getName() + " from NPC with ID " + this.getNpc() + ". Function: giveDebuff");
                return;
        }
        getPlayer().giveDebuff(disease, ms);
    }
    
    public String equipList(MapleClient c, int lb1, int lb2, int lb3, int lb4) {
        StringBuilder str = new StringBuilder();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<String> stra = new ArrayList<>();
        List<Integer> leavebehind = new ArrayList<>();
        leavebehind.add(lb1);
        leavebehind.add(lb2);
        leavebehind.add(lb3);
        leavebehind.add(lb4);
        Equip tempitem;
        ArrayList<IItem> equiplist = new ArrayList<>();
        for (IItem equipitem : equip.list()) {
            equiplist.add(equipitem);
        }
        
        // This sorts the items by their position in the inventory
        Collections.sort(equiplist, (o1, o2) -> (int) Math.signum(o1.getPosition() - o2.getPosition()));
        
        for (IItem equipitem : equiplist) {
            tempitem = (Equip) equipitem;
            if (!leavebehind.contains((int) equipitem.getPosition())) {
                if(tempitem.getStr() != 0 || tempitem.getDex() != 0 || tempitem.getInt() != 0 || tempitem.getLuk() != 0 || tempitem.getHp() != 0 || tempitem.getMp() != 0 || tempitem.getWatk() != 0 || tempitem.getMatk() != 0 || tempitem.getWdef() != 0 || tempitem.getMdef() != 0 || tempitem.getAcc() != 0 || tempitem.getAvoid() != 0 || tempitem.getSpeed() != 0 || tempitem.getJump() != 0 || tempitem.getUpgradeSlots() != 0) {
                    stra.add("#L" + equipitem.getPosition() + "##v" + equipitem.getItemId() + "##l");
                }
            }
        }
        if (!stra.isEmpty()) {
            for (String strb : stra) {
                str.append(strb);
            }
            return str.toString();
        } else {
            return "";
        }
    }

    public String plainEquipList() {
        return plainEquipList(4);
    }
    
    public String plainEquipList(int itemsPerLine) {
        StringBuilder str = new StringBuilder();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<String> stra = new ArrayList<>();
        ArrayList<IItem> equiplist = new ArrayList<>(equip.list());

        // This sorts the items by their position in the inventory
        Collections.sort(equiplist, (o1, o2) -> (int) Math.signum(o1.getPosition() - o2.getPosition()));

        int itemsInLine = 0;
        for (IItem equipitem : equiplist) {
            stra.add("#L" + equipitem.getPosition() + "##i" + equipitem.getItemId() + "##l");
            itemsInLine++;
            if (itemsPerLine > 0 && itemsInLine >= itemsPerLine) {
                stra.add("\r\n");
                itemsInLine = 0;
            }
        }
        if (!stra.isEmpty()) {
            for (String strb : stra) {
                str.append(strb);
            }
            return str.toString();
        } else {
            return "";
        }
    }
    
    public void sellSlotRange(byte start, byte end) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        double price;
        IItem item;
        int recvMesos;
        for (byte i = start; i <= end; ++i) {
            item = equip.getItem(i);
            if (item != null) {
                MapleInventoryManipulator.removeFromSlot(c, MapleItemInformationProvider.getInstance().getInventoryType(item.getItemId()), i, (short) 1, false);
                price = ii.getPrice(item.getItemId());
                recvMesos = (int) Math.max(Math.ceil(price), 0.0d);
                if (price >= 0.0d && recvMesos > 0) {
                    c.getPlayer().gainMeso(recvMesos, true);
                }
            }
        }
    }
    
    public String cashEquipList() {
        StringBuilder str = new StringBuilder();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<String> stra = new ArrayList<>();
        Equip tempitem;
        ArrayList<IItem> equiplist = new ArrayList<>();
        for (IItem equipitem : equip.list()) {
            equiplist.add(equipitem);
        }
        
        // This sorts the items by their position in the inventory
        Collections.sort(equiplist, (o1, o2) -> (int) Math.signum(o1.getPosition() - o2.getPosition()));
        
        for (IItem equipitem : equiplist) {
            tempitem = (Equip) equipitem;
            if (!(tempitem.getStr() != 0 || tempitem.getDex() != 0 || tempitem.getInt() != 0 || tempitem.getLuk() != 0 || tempitem.getHp() != 0 || tempitem.getMp() != 0 || tempitem.getWatk() != 0 || tempitem.getMatk() != 0 || tempitem.getWdef() != 0 || tempitem.getMdef() != 0 || tempitem.getAcc() != 0 || tempitem.getAvoid() != 0 || tempitem.getSpeed() != 0 || tempitem.getJump() != 0 || tempitem.getUpgradeSlots() != 0)) {
                stra.add("#L" + equipitem.getItemId() + "##v" + equipitem.getItemId() + "##l ");
            }
        }
        if (!stra.isEmpty()) {
            for (String strb : stra) {
                str.append(strb);
            }
            return str.toString();
        } else {
            return "";
        }
    }
    
    public void clearItems(MapleClient c) {
        clearItems(c, -1, -1, -1, -1);
    }
    
    public void clearItems(MapleClient c, int lb1, int lb2, int lb3, int lb4) {
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        MapleInventory use = c.getPlayer().getInventory(MapleInventoryType.USE);
        MapleInventory etc = c.getPlayer().getInventory(MapleInventoryType.ETC);
        Equip tempitem;
        List<Integer> leavebehind = new ArrayList<>();
        leavebehind.add(lb1);
        leavebehind.add(lb2);
        leavebehind.add(lb3);
        leavebehind.add(lb4);
        int i = 0;
        byte[] equipslots = new byte[equip.list().size()];
        for (IItem equipitem : equip.list()) {
            tempitem = (Equip) equipitem;
            if (!leavebehind.contains((int) equipitem.getPosition())) {
                if (tempitem.getStr() != 0 || tempitem.getDex() != 0 || tempitem.getInt() != 0 || tempitem.getLuk() != 0 || tempitem.getHp() != 0 || tempitem.getMp() != 0 || tempitem.getWatk() != 0 || tempitem.getMatk() != 0 || tempitem.getWdef() != 0 || tempitem.getMdef() != 0 || tempitem.getAcc() != 0 || tempitem.getAvoid() != 0 || tempitem.getSpeed() != 0 || tempitem.getJump() != 0 || tempitem.getUpgradeSlots() != 0 || (tempitem.getItemId() >= 1902000 && tempitem.getItemId() < 1920000) /* Mounts/saddles */) {
                    equipslots[i] = equipitem.getPosition();
                    i++;
                }
            }
        }
        
        for (byte j = 0; j < i; ++j) {
            MapleInventoryManipulator.removeFromSlot(c, MapleItemInformationProvider.getInstance().getInventoryType(equip.getItem(equipslots[j]).getItemId()), equipslots[j], (short) 1, false);
        }
        i = 0;
        byte[] useslots = new byte[use.list().size()];
        for (IItem useitem : use.list()) {
            useslots[i] = useitem.getPosition();
            i++;
        }
        for (byte j = 0; j < i; ++j) {
            MapleInventoryManipulator.removeFromSlot(c, MapleItemInformationProvider.getInstance().getInventoryType(use.getItem(useslots[j]).getItemId()), useslots[j], use.getItem(useslots[j]).getQuantity(), false);
        }
        i = 0;
        byte[] etcslots = new byte[etc.list().size()];
        for (IItem etcitem : etc.list()) {
            etcslots[i] = etcitem.getPosition();
            i++;
        }
        for (byte j = 0; j < i; ++j) {
            MapleInventoryManipulator.removeFromSlot(c, MapleItemInformationProvider.getInstance().getInventoryType(etc.getItem(etcslots[j]).getItemId()), etcslots[j], etc.getItem(etcslots[j]).getQuantity(), false);
        }
    }
    
    public void stripEquips(MapleClient c) {
        c.getPlayer().unequipEverything();
    }
    
    public void resetMaxHpMp(MapleClient c) {
        c.getPlayer().updateSingleStat(MapleStat.HP, 50);
        c.getPlayer().updateSingleStat(MapleStat.MP, 5);
        c.getPlayer().updateSingleStat(MapleStat.MAXHP, 50);
        c.getPlayer().updateSingleStat(MapleStat.MAXMP, 5);
        c.getPlayer().setHp(50);
        c.getPlayer().setMp(5);
        c.getPlayer().setMaxHp(50);
        c.getPlayer().setMaxMp(5);
    }
    
    public void spawnMonster(int mobid) {
        getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(mobid), getPlayer().getPosition());
    }
    
    public void spawnMonsterInMap(int mobid) {
        spawnMonsterInMap(mobid, getPlayer().getMapId());
    }
    
    public void spawnMonsterInMap(int mobid, int mapid) {
        MapleMap map;
        map = c.getChannelServer().getMapFactory().getMap(mapid);
        map.spawnMonster(MapleLifeFactory.getMonster(mobid));
    }
    
    public void spawnMonsterInMap(MapleMonster mob, int mapid) {
        MapleMap map;
        map = c.getChannelServer().getMapFactory().getMap(mapid);
        map.spawnMonster(mob);
    }
    
    public void spawnMonsterInMapAtPos(MapleMonster mob, int mapid, int x, int y) {
        MapleMap map;
        map = c.getChannelServer().getMapFactory().getMap(mapid);
        map.spawnMonsterOnGroudBelow(mob, new Point(x, y));
    }
    
    public MapleMonster getMonster(int mobid) {
        return MapleLifeFactory.getMonster(mobid);
    }
    
    public void spawnHT() {
        MapleMonster ht = MapleLifeFactory.getMonster(8810026);
        MapleMap map;
        map = c.getChannelServer().getMapFactory().getMap(240060200);
        map.spawnMonsterOnGroudBelow(ht, new Point(89, 290));
        map.killMonster(ht, getPlayer(), false);
        map.broadcastMessage(MaplePacketCreator.serverNotice(0, "As the cave shakes and rattles, here comes Horntail."));
    }
    
    public String listConsume(char initial) {
        StringBuilder sb = new StringBuilder();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<Pair<Integer, String>> consumelist = ii.getAllConsume(initial);
        
        if (consumelist.isEmpty()) {
            return "There are no use items with the selected initial letter.";
        }
        
        Collections.sort(consumelist, (o1, o2) -> {
            int comparison = o1.getRight().compareToIgnoreCase(o2.getRight());
            if (comparison < 0) {
                return -1;
            } else {
                if (comparison > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        
        for (Pair<Integer, String> itempair : consumelist) {
            sb.append("#L").append(itempair.getLeft()).append("#");
            sb.append("#i").append(itempair.getLeft()).append("# ");
            sb.append(itempair.getRight()).append("#l\r\n");
        }
        return sb.toString();
    }
    
    public String listEqp(char initial) {
        StringBuilder sb = new StringBuilder();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<Pair<Integer, String>> eqplist = ii.getAllEqp(initial);
        
        if (eqplist.isEmpty()) {
            return "There are no equipment items with the selected initial letter.";
        }
        
        Collections.sort(eqplist, (o1, o2) -> {
            int comparison = o1.getRight().compareToIgnoreCase(o2.getRight());
            if (comparison < 0) {
                return -1;
            } else {
                if (comparison > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        
        for (Pair<Integer, String> itempair : eqplist) {
            sb.append("#L").append(itempair.getLeft()).append("#");
            sb.append("#i").append(itempair.getLeft()).append("# ");
            sb.append(itempair.getRight()).append("#l\r\n");
        }
        return sb.toString();
    }
    
    public String listEtc(char initial) {
        StringBuilder sb = new StringBuilder();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<Pair<Integer, String>> etclist = ii.getAllEtc(initial);
        
        if (etclist.isEmpty()) {
            return "There are no etc items with the selected initial letter.";
        }
        
        Collections.sort(etclist, (o1, o2) -> {
            int comparison = o1.getRight().compareToIgnoreCase(o2.getRight());
            if (comparison < 0) {
                return -1;
            } else {
                if (comparison > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        
        for (Pair<Integer, String> itempair : etclist) {
            sb.append("#L").append(itempair.getLeft()).append("#");
            sb.append("#i").append(itempair.getLeft()).append("# ");
            sb.append(itempair.getRight()).append("#l\r\n");
        }
        return sb.toString();
    }
    
    public String whoDrops(int searchid) {
        String dropstring = "";
        try {
            List<String> retMobs = new ArrayList<>();
            MapleData data;
            MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
            data = dataProvider.getData("Mob.img");
            List<Pair<Integer, String>> mobPairList = new ArrayList<>();
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT monsterid FROM monsterdrops WHERE itemid = ?");
            ps.setInt(1, searchid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int mobn = rs.getInt("monsterid");
                for (MapleData mobIdData : data.getChildren()) {
                    int mobIdFromData = Integer.parseInt(mobIdData.getName());
                    String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                    mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
                }
                for (Pair<Integer, String> mobPair : mobPairList) {
                    if (mobPair.getLeft() == (mobn) && !retMobs.contains(mobPair.getRight())) {
                        retMobs.add(mobPair.getRight());
                    }
                }
            }
            rs.close();
            ps.close();
            if (!retMobs.isEmpty()) {
                for (String singleRetMob : retMobs) {
                    dropstring += singleRetMob + "\r\n";
                }
            } else {
                return "No mobs drop this item.";
            }
        } catch (SQLException e) {
            System.out.print("cm.whoDrops() failed with SQLException: " + e);
            dropstring = "#dcm.whoDrops()#k failed with SQLException:\r\n\r\n#r" + e + "#k";
        }
        return dropstring;
    }
    
    public boolean canGetDailyPrizes() {
        boolean ret = true;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement charps = con.prepareStatement("SELECT id FROM characters WHERE accountid = ?");
            charps.setInt(1, getPlayer().getAccountID());
            ResultSet charrs = charps.executeQuery();
            while (charrs.next()) {
                int charid = charrs.getInt("id");
                if (charid == getPlayer().getId()) {
                    continue;
                }
                PreparedStatement invps = con.prepareStatement("SELECT itemid FROM inventoryitems WHERE characterid = ?");
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
        } catch (SQLException e) {
            System.out.print("cm.canGetDailyPrizes() failed with SQLException: " + e);
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
            // valid time is in the future
            if (validtime.get(Calendar.DAY_OF_MONTH) == currenttime.get(Calendar.DAY_OF_MONTH) && validtime.get(Calendar.MONTH) == currenttime.get(Calendar.MONTH)) {
                return 0;
            } else {
                return 1;
            }
        } else if (validtime.compareTo(currenttime) < 0) {
            // valid time is in the past
            if (validtime.get(Calendar.DAY_OF_MONTH) == currenttime.get(Calendar.DAY_OF_MONTH) && validtime.get(Calendar.MONTH) == currenttime.get(Calendar.MONTH)) {
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
        time = time / (1000 * 60 * 60);
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
        TimerManager.getInstance().schedule(() -> player.setInvincible(false), 10 * 1000);
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
            long cooldowntime = (long) 3600000 - (180000 * resurrectionlevel);
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
        } catch (SQLException se) {
            return 0;
        }
        return mesos;
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
            int redeemed = 1;
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
        } catch (SQLException e) {
            System.out.print("cm.getBetaTester() failed with SQLException: " + e);
            return false;
        }
    }

    public void removeHiredMerchantItem(boolean tempItem, int itemId) {
        String Table = "";
        if (tempItem) Table = "temp";
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("DELETE FROM hiredmerchant" + Table + " WHERE itemid = ? AND ownerid = ? LIMIT 1");
            ps.setInt(1, itemId);
            ps.setInt(2, getPlayer().getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            System.out.print("cm.removeHiredMerchantItem() failed with SQLException: " + se);
        }
    }

    public boolean getHiredMerchantItems(boolean tempTable) {
        boolean temp = false;
        String Table = "";
        if (tempTable) {
            Table = "temp";
            temp = true;
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM hiredmerchant" + Table + " WHERE ownerid = ?");
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
                        removeHiredMerchantItem(temp, spItem.getItemId());
                    } else {
                        rs.close();
                        ps.close();
                        return false;
                    }
                } else {
                    Item spItem = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    spItem.setOwner(rs.getString("owner"));
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    MapleInventoryType type = ii.getInventoryType(spItem.getItemId());
                    if (!getPlayer().getInventory(type).isFull()) {
                        MapleInventoryManipulator.addFromDrop(c, spItem, true);
                        removeHiredMerchantItem(temp, spItem.getItemId());
                    } else {
                        rs.close();
                        ps.close();
                        return false;
                    }
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
            return false;
        }
        return true;
    }
}
