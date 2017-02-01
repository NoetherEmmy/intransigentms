package net.sf.odinms.net.login.handler;

import net.sf.odinms.client.LoginCrypto;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacketHandler;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.net.login.LoginWorker;
import net.sf.odinms.server.AutoRegister;
import net.sf.odinms.tools.KoreanDateUtil;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.Calendar;

public class LoginPasswordHandler implements MaplePacketHandler {
    // private static Logger log = LoggerFactory.getLogger(LoginPasswordHandler.class);
    @Override
    public boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String login = slea.readMapleAsciiString();
        String pwd = slea.readMapleAsciiString();
        //
        pwd = LoginCrypto.hexSha1(pwd);
        //

        c.setAccountName(login);

        int loginok = 0;
        boolean ipBan = c.hasBannedIP();
        boolean macBan = c.hasBannedMac();
        if (AutoRegister.getAccountExists(login)) {
            loginok = c.login(login, pwd, ipBan || macBan);
        } else if (LoginServer.getInstance().AutoRegister() && (!ipBan && !macBan)) {
            AutoRegister.createAccount(login, pwd, c.getSession().getRemoteAddress().toString());
            if (AutoRegister.success) {
                loginok = c.login(login, pwd, ipBan || macBan);
            }
        } else loginok = c.login(login, pwd, ipBan || macBan);
        Calendar tempBannedTill = c.getTempBanCalendar();
        if (loginok == 0 && (ipBan || macBan)) {
            loginok = 3;
            if (macBan) {
                String[] ipSplit = c.getSession().getRemoteAddress().toString().split(":");
                MapleCharacter.ban(ipSplit[0], "Enforcing account ban, account " + login, false);
            }
        }
        if (loginok == 3) {
            c.getSession().write(MaplePacketCreator.getPermBan(c.getBanReason()));
            return;
        } else if (loginok != 0) {
            c.getSession().write(MaplePacketCreator.getLoginFailed(loginok));
            return;
        } else if ((tempBannedTill != null) && (tempBannedTill.getTimeInMillis() != 0)) {
            long tempban = KoreanDateUtil.getTempBanTimestamp(tempBannedTill.getTimeInMillis());
            byte reason = c.getBanReason();
            c.getSession().write(MaplePacketCreator.getTempBan(tempban, reason));
            return;
        }
        if (c.isGm()) {
            LoginWorker.getInstance().registerGMClient(c);
        } else {
            LoginWorker.getInstance().registerClient(c);
        }
    }
}
