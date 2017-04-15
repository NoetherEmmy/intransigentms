package net.sf.odinms.database;

public class DatabaseException extends RuntimeException {
    private static final long serialVersionUID = -420103154764822555L;

    /** Creates a new instance of DatabaseException */
    public DatabaseException() {
    }

    public DatabaseException(final String msg) {
        super(msg);
    }

    public DatabaseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
