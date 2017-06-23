package net.sf.odinms.client.anticheat;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.server.TimerManager;

import java.sql.*;
import java.util.LinkedHashSet;
import java.util.Set;

public class CheatingOffensePersister {
    //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CheatingOffensePersister.class);
    private static final CheatingOffensePersister INSTANCE = new CheatingOffensePersister();
    private final Set<CheatingOffenseEntry> toPersist = new LinkedHashSet<>();

    private CheatingOffensePersister() {
        TimerManager.getInstance().register(new PersistingTask(), 61000);
    }

    public static CheatingOffensePersister getInstance() {
        return INSTANCE;
    }

    public void persistEntry(final CheatingOffenseEntry coe) {
        synchronized (toPersist) {
            toPersist.remove(coe);
            toPersist.add(coe);
        }
    }

    public class PersistingTask implements Runnable {
        @Override
        public void run() {
            final CheatingOffenseEntry[] offenses;
            synchronized (toPersist) {
                offenses = toPersist.toArray(new CheatingOffenseEntry[toPersist.size()]);
                toPersist.clear();
            }

            final Connection con = DatabaseConnection.getConnection();
            try {
                final PreparedStatement insertps =
                    con.prepareStatement(
                        "INSERT INTO cheatlog (cid, offense, count, lastoffensetime, param) VALUES (?, ?, ?, ?, ?)"
                    );
                final PreparedStatement updateps =
                    con.prepareStatement(
                        "UPDATE cheatlog SET count = ?, lastoffensetime = ?, param = ? WHERE id = ?"
                    );
                for (final CheatingOffenseEntry offense : offenses) {
                    final String parm = offense.getParam() == null ? "" : offense.getParam();
                    if (offense.getDbId() == -1) {
                        insertps.setInt(1, offense.getChrfor().getId());
                        insertps.setString(2, offense.getOffense().name());
                        insertps.setInt(3, offense.getCount());
                        insertps.setTimestamp(4, new Timestamp(offense.getLastOffenseTime()));
                        insertps.setString(5, parm);
                        insertps.executeUpdate();
                        final ResultSet rs = insertps.getGeneratedKeys();
                        if (rs.next()) {
                            offense.setDbId(rs.getInt(1));
                        }
                        rs.close();
                    } else {
                        updateps.setInt(1, offense.getCount());
                        updateps.setTimestamp(2, new Timestamp(offense.getLastOffenseTime()));
                        updateps.setString(3, parm);
                        updateps.setInt(4, offense.getDbId());
                        updateps.executeUpdate();
                    }
                }
                insertps.close();
                updateps.close();
            } catch (final SQLException sqle) {
                sqle.printStackTrace();
            }
        }
    }
}
