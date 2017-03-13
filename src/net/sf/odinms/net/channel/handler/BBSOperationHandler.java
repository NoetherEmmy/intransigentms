
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BBSOperationHandler extends AbstractMaplePacketHandler {
    private static final Logger log = LoggerFactory.getLogger(BBSOperationHandler.class);

    private String correctLength(String in, int maxSize) {
        if (in.length() > maxSize) return in.substring(0, maxSize);
        return in;
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        if (!MapleGuild.ENABLE_BBS) {
            c.getSession()
             .write(
                 MaplePacketCreator.serverNotice(
                     1,
                     "Your server administrator has currently disabled Guild BBS."
                 )
             );
            return;
        }

        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        byte mode = slea.readByte();
        int localthreadid = 0;

        switch (mode) {
            case 0:
                boolean bEdit = slea.readByte() == 1;
                if (bEdit) {
                    localthreadid = slea.readInt();
                }
                boolean bNotice = slea.readByte() == 1;
                String title = correctLength(slea.readMapleAsciiString(), 25);
                String text = correctLength(slea.readMapleAsciiString(), 600);
                int icon = slea.readInt();
                if (icon >= 0x64 && icon <= 0x6a) {
                    if (!c.getPlayer().haveItem(5290000 + icon - 0x64, 1, false, true)) {
                        return;
                    }
                } else if (!(icon >= 0 && icon <= 2)) {
                    return;
                }
                if (!bEdit) {
                    newBBSThread(c, title, text, icon, bNotice);
                } else {
                    editBBSThread(c, title, text, icon, localthreadid);
                }
                break;
            case 1: // Delete a thread.
                localthreadid = slea.readInt();
                deleteBBSThread(c, localthreadid);
                break;
            case 2: // List threads.
                int start = slea.readInt();
                listBBSThreads(c, start * 10);
                break;
            case 3: // List thread + reply, followed by id (int).
                localthreadid = slea.readInt();
                displayThread(c, localthreadid);
                break;
            case 4: // Reply.
                localthreadid = slea.readInt();
                text = correctLength(slea.readMapleAsciiString(), 25);
                newBBSReply(c, localthreadid, text);
                break;
            case 5: // Delete reply.
                localthreadid = slea.readInt();
                int replyid = slea.readInt();
                deleteBBSReply(c, replyid);
                break;
            default:
                log.warn("Unhandled BBS mode: " + mode);
                break;
        }
    }

    public static void listBBSThreads(MapleClient c, int start) {
        int gid = c.getPlayer().getGuildId();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps =
                con.prepareStatement(
                    "SELECT * FROM bbs_threads WHERE guildid = ? ORDER BY localthreadid DESC"
                );
            ps.setInt(1, gid);
            ResultSet rs = ps.executeQuery();
            c.getSession().write(MaplePacketCreator.BBSThreadList(rs, start));
            rs.close();
            ps.close();
        } catch (SQLException sqle) {
            log.error("SQLException: " + sqle.getLocalizedMessage(), sqle);
        }
    }

    public static void newBBSReply(MapleClient client, int localthreadid, String text) {
        MapleCharacter mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps =
                con.prepareStatement(
                    "SELECT threadid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?"
                );
            ps.setInt(1, mc.getGuildId());
            ps.setInt(2, localthreadid);
            ResultSet threadRS = ps.executeQuery();
            if (!threadRS.next()) {
                threadRS.close();
                ps.close();
                return;
            }
            int threadid = threadRS.getInt("threadid");
            threadRS.close();
            ps.close();

            ps = con.prepareStatement(
                "INSERT INTO bbs_replies " +
                    "(`threadid`, `postercid`, `timestamp`, `content`) VALUES " +
                    "(?, ?, ?, ?)"
            );
            ps.setInt(1, threadid);
            ps.setInt(2, mc.getId());
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, text);
            ps.execute();
            ps.close();

            ps = con.prepareStatement(
                "UPDATE bbs_threads SET replycount = replycount + 1 WHERE threadid = ?"
            );
            ps.setInt(1, threadid);
            ps.execute();
            ps.close();
            displayThread(client, localthreadid);
        } catch (SQLException sqle) {
            log.error("SQLException: " + sqle.getLocalizedMessage(), sqle);
        }
    }

    public static void editBBSThread(MapleClient client, String title, String text, int icon, int localthreadid) {
        MapleCharacter c = client.getPlayer();
        if (c.getGuildId() <= 0) return;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps =
                con.prepareStatement(
                    "UPDATE bbs_threads SET `name` = ?, `timestamp` = ?, " +
                        "`icon` = ?, " +
                        "`startpost` = ? WHERE guildid = ? AND localthreadid = ? AND (postercid = ? OR ?)"
                );
            ps.setString(1, title);
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, icon);
            ps.setString(4, text);
            ps.setInt(5, c.getGuildId());
            ps.setInt(6, localthreadid);
            ps.setInt(7, c.getId());
            ps.setBoolean(8, c.getGuildRank() <= 2);
            ps.execute();
            ps.close();
            displayThread(client, localthreadid);
        } catch (SQLException sqle) {
            log.error("SQLException: " + sqle.getLocalizedMessage(), sqle);
        }

    }

    public static void newBBSThread(MapleClient client, String title, String text, int icon, boolean bNotice) {
        MapleCharacter c = client.getPlayer();
        if (c.getGuildId() <= 0) return;
        int nextId = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (!bNotice) {
                ps = con.prepareStatement(
                    "SELECT MAX(localthreadid) AS lastLocalId FROM bbs_threads WHERE guildid = ?"
                );
                ps.setInt(1, c.getGuildId());
                ResultSet rs = ps.executeQuery();
                rs.next();
                nextId = rs.getInt("lastLocalId") + 1;
                rs.close();
                ps.close();
            }
            ps = con.prepareStatement(
                "INSERT INTO bbs_threads " +
                    "(`postercid`, `name`, `timestamp`, `icon`, `startpost`, " +
                    "`guildid`, `localthreadid`) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?)"
            );
            ps.setInt(1, c.getId());
            ps.setString(2, title);
            ps.setLong(3, System.currentTimeMillis());
            ps.setInt(4, icon);
            ps.setString(5, text);
            ps.setInt(6, c.getGuildId());
            ps.setInt(7, nextId);
            ps.execute();
            ps.close();
            displayThread(client, nextId);
        } catch (SQLException se) {
            log.error("SQLException: " + se.getLocalizedMessage(), se);
        }

    }

    public static void deleteBBSThread(MapleClient client, int localthreadid) {
        MapleCharacter mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }

        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps =
                con.prepareStatement(
                    "SELECT threadid, postercid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?"
                );
            ps.setInt(1, mc.getGuildId());
            ps.setInt(2, localthreadid);
            ResultSet threadRS = ps.executeQuery();
            if (!threadRS.next()) {
                threadRS.close();
                ps.close();
                return;
            }
            if (mc.getId() != threadRS.getInt("postercid") && mc.getGuildRank() > 2) {
                // But let all masters/JRs delete
                threadRS.close();
                ps.close();
                return;
            }
            int threadid = threadRS.getInt("threadid");

            threadRS.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM bbs_replies WHERE threadid = ?");
            ps.setInt(1, threadid);
            ps.execute();
            ps.close();
            ps = con.prepareStatement("DELETE FROM bbs_threads WHERE threadid = ?");
            ps.setInt(1, threadid);
            ps.execute();
            ps.close();
        } catch (SQLException se) {
            log.error("SQLException: " + se.getLocalizedMessage(), se);
        }
    }

    public static void deleteBBSReply(MapleClient client, int replyid) {
        MapleCharacter mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        int threadid;
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps =
                con.prepareStatement(
                    "SELECT postercid, threadid FROM bbs_replies WHERE replyid = ?"
                );
            ps.setInt(1, replyid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return;
            }
            if (mc.getId() != rs.getInt("postercid") && mc.getGuildRank() > 2) {
                rs.close();
                ps.close();
                return;
            }
            threadid = rs.getInt("threadid");
            rs.close();
            ps.close();
            ps = con.prepareStatement(
                "DELETE FROM bbs_replies WHERE replyid = ?"
            );
            ps.setInt(1, replyid);
            ps.execute();
            ps.close();
            ps = con.prepareStatement(
                "UPDATE bbs_threads SET replycount = replycount - 1 WHERE threadid = ?"
            );
            ps.setInt(1, threadid);
            ps.execute();
            ps.close();
            displayThread(client, threadid, false);
        } catch (SQLException sqle) {
            log.error("SQLException: " + sqle.getLocalizedMessage(), sqle);
        }
    }

    public static void displayThread(MapleClient client, int threadid) {
        displayThread(client, threadid, true);
    }

    public static void displayThread(MapleClient client, int threadid, boolean bIsThreadIdLocal) {
        MapleCharacter mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps =
                con.prepareStatement(
                    "SELECT * FROM bbs_threads WHERE guildid = ? AND " +
                        (bIsThreadIdLocal ? "local" : "") +
                        "threadid = ?"
                );
            ps.setInt(1, mc.getGuildId());
            ps.setInt(2, threadid);
            ResultSet threadRS = ps.executeQuery();
            if (!threadRS.next()) {
                threadRS.close();
                ps.close();
                return;
            }
            ResultSet repliesRS = null;
            PreparedStatement ps2 = null;
            if (threadRS.getInt("replycount") > 0) {
                ps2 = con.prepareStatement("SELECT * FROM bbs_replies WHERE threadid = ?");
                ps2.setInt(1, !bIsThreadIdLocal ? threadid : threadRS.getInt("threadid"));
                repliesRS = ps2.executeQuery();
                // The lack of repliesRS.next() is intentional.
            }
            client
                .getSession()
                .write(
                    MaplePacketCreator.showThread(
                        bIsThreadIdLocal ? threadid : threadRS.getInt("localthreadid"),
                        threadRS,
                        repliesRS
                    )
                );
            threadRS.close();
            ps.close();
            if (ps2 != null) {
                ps2.close();
                repliesRS.close();
            }
        } catch (SQLException se) {
            log.error("SQLException: " + se.getLocalizedMessage(), se);
        } catch (RuntimeException re) {
            log.error(
                "The number of reply rows does not match the replycount in thread. Thread Id = " +
                    re.getMessage(),
                re
            );
            try {
                PreparedStatement ps =
                    con.prepareStatement(
                        "DELETE FROM bbs_threads WHERE threadid = ?"
                    );
                ps.setInt(1, Integer.parseInt(re.getMessage()));
                ps.execute();
                ps.close();
                ps = con.prepareStatement(
                    "DELETE FROM bbs_replies WHERE threadid = ?"
                );
                ps.setInt(1, Integer.parseInt(re.getMessage()));
                ps.execute();
                ps.close();
            } catch (Exception ignored) {
            }
        }
    }
}
