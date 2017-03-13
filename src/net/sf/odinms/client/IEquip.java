package net.sf.odinms.client;

public interface IEquip extends IItem {
    enum ScrollResult {
        SUCCESS, FAIL, CURSE
    }

    byte getUpgradeSlots();

    byte getLocked();

    byte getLevel();

    int getRingId();

    short getStr();

    short getDex();

    short getInt();

    short getLuk();

    short getHp();

    short getMp();

    short getWatk();

    short getMatk();

    short getWdef();

    short getMdef();

    short getAcc();

    short getAvoid();

    short getHands();

    short getSpeed();

    short getJump();
}
