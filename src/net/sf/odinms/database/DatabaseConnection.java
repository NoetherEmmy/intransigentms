package net.sf.odinms.database;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public final class DatabaseConnection {
    private static final ThreadLocal<Connection> con = new ThreadLocalConnection();
    //private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);
    private static Properties props;

    public static Connection getConnection() {
        if (props == null) {
            throw new RuntimeException("DatabaseConnection not initialized");
        }
        return con.get();
    }

    public static boolean isInitialized() {
        return props != null;
    }

    public static void setProps(final Properties aProps) {
        props = aProps;
    }

    public static void closeAll() throws SQLException {
        for (final Connection con : ThreadLocalConnection.allConnections) {
            con.close();
        }
    }

    private static class ThreadLocalConnection extends ThreadLocal<Connection> {
        public static final Collection<Connection> allConnections = new ArrayList<>();

        @Override
        protected Connection initialValue() {
            final String driver = props.getProperty("driver");
            final String url = props.getProperty("url");
            final String user = props.getProperty("user");
            final String password = props.getProperty("password");
            try {
                Class.forName(driver); // Touch the MySQL driver.
            } catch (final ClassNotFoundException e) {
                System.err.println("ERROR");
                e.printStackTrace();
            }
            try {
                final Connection con = DriverManager.getConnection(url, user, password);
                allConnections.add(con);
                return con;
            } catch (final SQLException e) {
                System.err.println("ERROR");
                e.printStackTrace();
                return null;
            }
        }
    }
}
