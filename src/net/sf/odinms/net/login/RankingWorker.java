package net.sf.odinms.net.login;

import net.sf.odinms.client.MapleJob;
import net.sf.odinms.database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RankingWorker implements Runnable {
    private Connection con;
    private long lastUpdate = System.currentTimeMillis();
    private static final Logger log = LoggerFactory.getLogger(RankingWorker.class);

    public void run() {
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            updateRanking(null);
            updateRanking(MapleJob.BEGINNER);
            updateRanking(MapleJob.WARRIOR);
            updateRanking(MapleJob.MAGICIAN);
            updateRanking(MapleJob.BOWMAN);
            updateRanking(MapleJob.THIEF);
            updateRanking(MapleJob.PIRATE);
            con.commit();
            con.setAutoCommit(true);
            lastUpdate = System.currentTimeMillis();
        } catch (SQLException sqle) {
            try {
                con.rollback();
                con.setAutoCommit(true);
                log.warn("Could not update rankings", sqle);
            } catch (SQLException sqle2) {
                log.error("Could not rollback unfinished ranking transaction", sqle2);
            }
        }
    }

    private void updateRanking(MapleJob job) throws SQLException {
        String sqlCharSelect =
            "SELECT c.id, " +
                (job != null ? "c.jobRank, c.jobRankMove" : "c.rank, c.rankMove") +
                ", a.lastlogin AS lastlogin, a.loggedin FROM " +
                "characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id WHERE c.gm = 0 ";
        if (job != null) {
            sqlCharSelect += "AND c.job DIV 100 = ? ";
        }
        sqlCharSelect += "ORDER BY c.reborns DESC , c.level DESC , c.exp DESC , c.fame DESC , c.meso DESC";
        PreparedStatement charSelect = con.prepareStatement(sqlCharSelect);
        if (job != null) {
            charSelect.setInt(1, job.getId() / 100);
        }
        ResultSet rs = charSelect.executeQuery();
        PreparedStatement ps =
            con.prepareStatement(
                "UPDATE characters SET " +
                    (job != null ? "jobRank = ?, jobRankMove = ? " : "rank = ?, rankMove = ? ") +
                    "WHERE id = ?"
            );
        int rank = 0;
        while (rs.next()) {
            int rankMove = 0;
            rank++;
            if (rs.getLong("lastlogin") < lastUpdate || rs.getInt("loggedin") > 0) {
                rankMove = rs.getInt((job != null ? "jobRankMove" : "rankMove"));
            }
            rankMove += rs.getInt((job != null ? "jobRank" : "rank")) - rank;
            ps.setInt(1, rank);
            ps.setInt(2, rankMove);
            ps.setInt(3, rs.getInt("id"));
            ps.executeUpdate();
        }
        rs.close();
        charSelect.close();
        ps.close();
    }
}
