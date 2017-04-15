package net.sf.odinms.server.fourthjobquests;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.client.messages.ServernoticeMapleClientMessageCallback;
import net.sf.odinms.net.StringValueHolder;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.tools.MaplePacketCreator;

import java.util.Collection;

public class FourthJobQuestsPortalHandler {
    public enum FourthJobQuests implements StringValueHolder {
        RUSH("s4rush"),
        BERSERK("s4berserk");
        private final String name;

        FourthJobQuests(final String Newname) {
            this.name = Newname;
        }

        @Override
        public String getValue() {
            return name;
        }
    }

    private FourthJobQuestsPortalHandler() {
    }

    //c.getClient().getSession().write(MaplePacketCreator.enableActions());
    public static boolean handlePortal(final String name, final MapleCharacter c) {
        final ServernoticeMapleClientMessageCallback snmcmc =
            new ServernoticeMapleClientMessageCallback(5, c.getClient());
        if (name.equals(FourthJobQuests.RUSH.getValue())) {
            if (!checkPartyLeader(c) && !checkRush(c)) {
                snmcmc.dropMessage("You step into the portal, but it swiftly kicks you out.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
            }
            if (!checkPartyLeader(c) && checkRush(c)) {
                snmcmc.dropMessage("You're not the party leader.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            if (!checkRush(c)) {
                snmcmc.dropMessage("Someone in your party is not a 4th Job warrior.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            c.getClient()
             .getChannelServer()
             .getEventSM()
             .getEventManager("4jrush")
             .startInstance(c.getParty(), c.getMap());
            return true;
        } else if (name.equals(FourthJobQuests.BERSERK.getValue())) {
            if (!checkBerserk(c)) {
                snmcmc.dropMessage("The portal to the Forgotten Shrine is locked");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            c.getClient()
             .getChannelServer()
             .getEventSM()
             .getEventManager("4jberserk")
             .startInstance(c.getParty(), c.getMap());
            return true;
        }
        return false;
    }

    private static boolean checkRush(final MapleCharacter c) {
        final MapleParty csParty = c.getParty();
        if (csParty == null) return false;
        final Collection<MaplePartyCharacter> CsPartyMembers = csParty.getMembers();
        for (final MaplePartyCharacter mpc : CsPartyMembers) {
            if (!MapleJob.getById(mpc.getJobId()).isA(MapleJob.WARRIOR)) return false;
            if (
                !MapleJob.getById(mpc.getJobId()).isA(MapleJob.HERO) &&
                !MapleJob.getById(mpc.getJobId()).isA(MapleJob.PALADIN) &&
                !MapleJob.getById(mpc.getJobId()).isA(MapleJob.DARKKNIGHT)
            ) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkPartyLeader(final MapleCharacter c) {
        return c.getParty() != null && c.getParty().getLeader().getId() == c.getId();
    }

    private static boolean checkBerserk(final MapleCharacter c) {
        return c.haveItem(4031475, 1, false, true);
    }
}
