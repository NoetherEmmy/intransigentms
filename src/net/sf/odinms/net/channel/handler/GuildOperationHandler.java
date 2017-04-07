package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildResponse;
import net.sf.odinms.tools.HexTool;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GuildOperationHandler extends AbstractMaplePacketHandler {
    private boolean isGuildNameAcceptable(String name) {
        if (name.length() < 3 || name.length() > 12) return false;
        for (int i = 0; i < name.length(); ++i) {
            if (!Character.isLowerCase(name.charAt(i)) && !Character.isUpperCase(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void respawnPlayer(MapleCharacter mc) {
        mc.getMap().broadcastMessage(mc, MaplePacketCreator.removePlayerFromMap(mc.getId()), false);
        mc.getMap().broadcastMessage(mc, MaplePacketCreator.spawnPlayerMapobject(mc), false);
        if (mc.getNoPets() > 0) {
            for (MaplePet pet : mc.getPets()) {
                if (pet == null) continue;
                mc.getMap().broadcastMessage(mc, MaplePacketCreator.showPet(mc, pet, false, false), false);
            }
        }
    }

    private class Invited {
        public final String name;
        public final int gid;
        public final long expiration;

        public Invited(String n, int id) {
            name = n.toLowerCase();
            gid = id;
            expiration = System.currentTimeMillis() + 60L * 60L * 1000L; // 1 hr. expiration
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Invited)) return false;
            Invited oth = (Invited) other;
            return gid == oth.gid && name.equals(oth.name);
        }
    }

    private final List<Invited> invited = new ArrayList<>();
    private long nextPruneTime = System.currentTimeMillis() + 20L * 60L * 1000L;

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        // Prune away any expired guild requests.
        if (System.currentTimeMillis() >= nextPruneTime) {
            Iterator<Invited> itr = invited.iterator();
            Invited inv;
            while (itr.hasNext()) {
                inv = itr.next();
                if (System.currentTimeMillis() >= inv.expiration) itr.remove();
            }

            nextPruneTime = System.currentTimeMillis() + 20L * 60L * 1000L;
        }
        MapleCharacter mc = c.getPlayer();
        byte type = slea.readByte();
        switch (type) {
            case 0x02:
                // Guild creation.
                if (mc.getGuildId() > 0 || mc.getMapId() != 200000301) {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "You cannot create a new Guild while in one."));
                    return;
                }
                if (mc.getMeso() < MapleGuild.CREATE_GUILD_COST) {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "You do not have enough mesos to create a Guild."));
                    return;
                }
                String guildName = slea.readMapleAsciiString();
                if (!isGuildNameAcceptable(guildName)) {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "The Guild name you have chosen is not accepted."));
                    return;
                }
                int gid;
                try {
                    gid = c.getChannelServer().getWorldInterface().createGuild(mc.getId(), guildName);
                } catch (RemoteException re) {
                    System.err.println("Exception occurred during guild creation: " + re);
                    c.getSession().write(MaplePacketCreator.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    return;
                }
                if (gid == 0) {
                    c.getSession().write(MaplePacketCreator.genericGuildMessage((byte) 0x1c));
                    return;
                }
                mc.gainMeso(-MapleGuild.CREATE_GUILD_COST, true, false, true);
                mc.setGuildId(gid);
                mc.setGuildRank(1);
                mc.saveGuildStatus();
                c.getSession().write(MaplePacketCreator.showGuildInfo(mc));
                c.getSession().write(MaplePacketCreator.serverNotice(1, "You have successfully created a Guild."));
                respawnPlayer(mc);
                break;
            case 0x05:
                // Invitation.
                if (mc.getGuildId() <= 0 || mc.getGuildRank() > 2) { // 1 = guild master, 2 = jr.
                    System.err.println("[hax] " + mc.getName() + " used guild invitation when s/he isn't allowed.");
                    return;
                }
                String name = slea.readMapleAsciiString();
                MapleGuildResponse mgr = MapleGuild.sendInvite(c, name);
                if (mgr != null) {
                    c.getSession().write(mgr.getPacket());
                } else {
                    Invited inv = new Invited(name, mc.getGuildId());
                    if (!invited.contains(inv)) invited.add(inv);
                }
                break;
            case 0x06:
                // Accepted guild invitation.
                //System.out.println(slea.toString());
                if (mc.getGuildId() > 0) {
                    System.err.println("[hax] " + mc.getName() + " attempted to join a guild when s/he is already in one.");
                    return;
                }
                gid = slea.readInt();
                int cid = slea.readInt();

                if (cid != mc.getId()) {
                    System.err.println("[hax] " + mc.getName() + " attempted to join a guild with a different character id.");
                    return;
                }
                name = mc.getName().toLowerCase();
                Iterator<Invited> itr = invited.iterator();
                boolean bOnList = false;
                while (itr.hasNext()) {
                    Invited inv = itr.next();
                    if (gid == inv.gid && name.equals(inv.name)) {
                        bOnList = true;
                        itr.remove();
                        break;
                    }
                }
                if (!bOnList) {
                    System.err.println("[hax] " + mc.getName() + " is trying to join a guild that never invited him/her (or that the invitation has expired)");
                    return;
                }
                mc.setGuildId(gid);
                mc.setGuildRank(5);
                int s;
                try {
                    s = c.getChannelServer().getWorldInterface().addGuildMember(mc.getMGC());
                } catch (RemoteException re) {
                    System.err.println("Exception occurred while attempting to add character to guild: " + re);
                    c.getSession().write(MaplePacketCreator.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    mc.setGuildId(0);
                    return;
                }
                if (s == 0) {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "The Guild you are trying to join is already full."));
                    mc.setGuildId(0);
                    return;
                }
                c.getSession().write(MaplePacketCreator.showGuildInfo(mc));
                mc.saveGuildStatus();
                respawnPlayer(mc);
                break;
            case 0x07:
                // Leaving.
                cid = slea.readInt();
                name = slea.readMapleAsciiString();
                if (cid != mc.getId() || !name.equals(mc.getName()) || mc.getGuildId() <= 0) {
                    System.err.println("[hax] " + mc.getName() + " tried to quit guild under the name \"" + name + "\" and current guild id of " + mc.getGuildId() + ".");
                    return;
                }
                try {
                    c.getChannelServer().getWorldInterface().leaveGuild(mc.getMGC());
                } catch (RemoteException re) {
                    System.err.println("Exception occurred while attempting to leave guild: " + re);
                    c.getSession().write(MaplePacketCreator.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    return;
                }
                c.getSession().write(MaplePacketCreator.showGuildInfo(null));
                mc.setGuildId(0);
                mc.saveGuildStatus();
                respawnPlayer(mc);
                break;
            case 0x08:
                // Expel.
                cid = slea.readInt();
                name = slea.readMapleAsciiString();
                if (mc.getGuildRank() > 2 || mc.getGuildId() <= 0) {
                    System.err.println("[hax] " + mc.getName() + " is trying to expel without rank 1 or 2.");
                    return;
                }
                try {
                    c.getChannelServer().getWorldInterface().expelMember(mc.getMGC(), name, cid);
                } catch (RemoteException re) {
                    System.err.println("Exception occurred while attempting to change rank: " + re);
                    c.getSession().write(MaplePacketCreator.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    return;
                }
                break;
            case 0x0D:
                // Guild rank titles change.
                if (mc.getGuildId() <= 0 || mc.getGuildRank() != 1) {
                    System.err.println("[hax] " + mc.getName() + " tried to change guild rank titles when s/he does not have permission.");
                    return;
                }
                String[] ranks = new String[5];
                for (int i = 0; i < 5; ++i)
                    ranks[i] = slea.readMapleAsciiString();
                try {
                    c.getChannelServer().getWorldInterface().changeRankTitle(mc.getGuildId(), ranks);
                } catch (RemoteException re) {
                    System.err.println("Exception occurred changing rank title: " + re);
                    c.getSession().write(MaplePacketCreator.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    return;
                }
                break;
            case 0x0E:
                // Rank change.
                cid = slea.readInt();
                byte newRank = slea.readByte();
                if (mc.getGuildRank() > 2 || (newRank <= 2 && mc.getGuildRank() != 1) || mc.getGuildId() <= 0) {
                    System.err.println("[hax] " + mc.getName() + " is trying to change rank outside of his/her permissions.");
                    return;
                }
                if (newRank <= 1 || newRank > 5) return;
                try {
                    c.getChannelServer().getWorldInterface().changeRank(mc.getGuildId(), cid, newRank);
                } catch (RemoteException re) {
                    System.err.println("Exception occurred while attempting to change rank: " + re);
                    c.getSession().write(MaplePacketCreator.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    return;
                }
                break;
            case 0x0F:
                // Guild emblem change.
                if (mc.getGuildId() <= 0 || mc.getGuildRank() != 1 || mc.getMapId() != 200000301) {
                    System.err.println("[hax] " + mc.getName() + " tried to change guild emblem without being the guild leader.");
                    return;
                }
                if (mc.getMeso() < MapleGuild.CHANGE_EMBLEM_COST) {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "You do not have enough mesos to create a Guild."));
                    return;
                }
                short bg = slea.readShort();
                byte bgcolor = slea.readByte();
                short logo = slea.readShort();
                byte logocolor = slea.readByte();
                try {
                    c.getChannelServer().getWorldInterface().setGuildEmblem(mc.getGuildId(), bg, bgcolor, logo, logocolor);
                } catch (RemoteException re) {
                    System.err.println("Exception occurred attempting to set guild emblem: " + re);
                    c.getSession().write(MaplePacketCreator.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    return;
                }
                mc.gainMeso(-MapleGuild.CHANGE_EMBLEM_COST, true, false, true);
                respawnPlayer(mc);
                // c.getSession().write(MaplePacketCreator.serverNotice(1, "Your Guild's emblem has been changed."));
                break;
            case 0x10:
                // Guild notice change.
                if (mc.getGuildId() <= 0 || mc.getGuildRank() > 2) {
                    System.err.println("[hax] " + mc.getName() + " tried to change guild notice while not in a guild.");
                    return;
                }
                String notice = slea.readMapleAsciiString();
                if (notice.length() > 100) return;
                try {
                    c.getChannelServer().getWorldInterface().setGuildNotice(mc.getGuildId(), notice);
                } catch (RemoteException re) {
                    System.err.println("Exception occurred attempting to set guild notice: " + re);
                    c.getSession().write(
                        MaplePacketCreator.serverNotice(
                            5,
                            "Unable to connect to the World Server. Please try again later."
                        )
                    );
                    return;
                }
                break;
            default:
                System.err.println("Unhandled GUILD_OPERATION packet, type = " + type + ", remaining:");
                System.err.println(HexTool.toString(slea));
                break;
        }
    }
}
