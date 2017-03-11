package net.sf.odinms.net.login.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.Random;

//import net.sf.odinms.database.DatabaseConnection;
/*
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.net.login.LoginWorker;
import net.sf.odinms.tools.MaplePacketCreator;
*/
/*
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
*/

public class GuestLoginHandler implements MaplePacketHandler {
    @Override
    public boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        /*
        Connection con = DatabaseConnection.getConnection();
        Random r = new Random();
        c.getSession().write(MaplePacketCreator.sendGuestTOS(r.nextInt(999999)));
        int guestid = 1;
        int loginok;
        do {
            try {
                PreparedStatement ps = con.prepareStatement("SELECT password, guest FROM accounts WHERE name = ?");
                ps.setString(1, "GUEST" + guestid);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getInt("guest") == 1) {
                        loginok = c.login("GUEST" + guestid, rs.getString("password"), false);
                    } else {
                        loginok = 3;
                    }
                } else {
                    ps.close();
                    String password = createRandomPass();
                    ps = con.prepareStatement("INSERT INTO accounts (name, password, guest) VALUES (?, ?, 1)");
                    ps.setString(1, "GUEST" + guestid);
                    ps.setString(2, password);
                    ps.executeUpdate();
                    ps.close();
                    loginok = c.login("GUEST" + guestid, password, false);
                }
                ps.close();
                rs.close();
            } catch (SQLException se) {
                se.printStackTrace();
                return;
            }
            if (loginok == 3 || loginok == 7 || loginok == 4) { // Already logged in / banned.
                if (LoginServer.getInstance().isServerCheck()) {
                    c.getSession().close();
                    return;
                } else {
                    guestid++;
                }
            }
        } while (loginok != 0);
        c.deleteAllCharacters();
        c.setAccountName("GUEST" + guestid);
        c.setGuest(true);
        LoginWorker.getInstance().registerClient(c);
        */
    }

    private String createRandomPass() {
        Random r = new Random();
        int passwordlength = r.nextInt(5) + 5;
        StringBuilder sb = new StringBuilder();
        char[] sLetters = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        char[] cLetters = "abcdefghijklmnopqrstuvwxyz".toUpperCase().toCharArray(); // LOL GOD IM LAZY
        char[] numbers = "0123456789".toCharArray();
        for (int i = 0; i < passwordlength; ++i) {
            switch (r.nextInt(3)) {
                case 0:
                    sb.append(sLetters[r.nextInt(sLetters.length)]);
                    break;
                case 1:
                    sb.append(cLetters[r.nextInt(cLetters.length)]);
                    break;
                case 2:
                    sb.append(numbers[r.nextInt(numbers.length)]);
                    break;
            }
        }
        return sb.toString();
    }
}
