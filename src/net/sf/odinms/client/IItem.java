package net.sf.odinms.client;

public interface IItem extends Comparable<IItem> {
    int ITEM = 2;
    int EQUIP = 1;

    byte getType();

    byte getPosition();

    int getItemId();

    int getPetId();

    short getQuantity();

    String getOwner();

    IItem copy();

    void setOwner(String owner);

    void setPosition(byte position);

    void setQuantity(short quantity);
}
