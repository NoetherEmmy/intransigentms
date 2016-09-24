package net.sf.odinms.client;

public class Item implements IItem {

    private int id;
    private byte position;
    private short quantity;
    private int petid;
    private String owner = "";

    public Item(int id, byte position, short quantity) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.petid = -1;
    }

    public Item(int id, byte position, short quantity, int petid) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.petid = petid;
    }

    public IItem copy() {
        Item ret = new Item(id, position, quantity, petid);
        ret.owner = owner;
        return ret;
    }

    public void setPosition(byte position) {
        this.position = position;
    }

    public void setQuantity(short quantity) {
        this.quantity = quantity;
    }

    @Override
    public int getItemId() {
        return id;
    }

    @Override
    public byte getPosition() {
        return position;
    }

    @Override
    public short getQuantity() {
        return quantity;
    }

    @Override
    public byte getType() {
        return IItem.ITEM;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public int getPetId() {
        return petid;
    }

    @Override
    public int compareTo(IItem other) {
        if (Math.abs(position) < Math.abs(other.getPosition())) {
            return -1;
        } else if (Math.abs(position) == Math.abs(other.getPosition())) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public String toString() {
        return "Item: " + id + " quantity: " + quantity;
    }
}
 
