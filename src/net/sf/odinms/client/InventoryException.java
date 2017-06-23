package net.sf.odinms.client;

public class InventoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Creates a new instance of InventoryException */
    public InventoryException() {
        super();
    }

    public InventoryException(final String msg) {
        super(msg);
    }
}
