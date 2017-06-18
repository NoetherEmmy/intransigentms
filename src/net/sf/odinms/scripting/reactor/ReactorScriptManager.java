package net.sf.odinms.scripting.reactor;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.scripting.AbstractScriptManager;
import net.sf.odinms.server.life.MapleMonsterInformationProvider.DropEntry;
import net.sf.odinms.server.maps.MapleReactor;

import javax.script.Invocable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactorScriptManager extends AbstractScriptManager {
    private static final ReactorScriptManager instance = new ReactorScriptManager();
    private final Map<Integer, List<DropEntry>> drops = new HashMap<>();

    public static synchronized ReactorScriptManager getInstance() {
        return instance;
    }

    public void act(final MapleClient c, final MapleReactor reactor) {
        try {
            final ReactorActionManager rm = new ReactorActionManager(c, reactor);
            final Invocable iv = getInvocable("reactor/" + reactor.getId() + ".js", c);
            if (iv == null) return;
            engine.put("rm", rm);
            final ReactorScript rs = iv.getInterface(ReactorScript.class);
            rs.act();
        } catch (final Exception e) {
            log.error("Error executing reactor script: " + reactor.getId(), e);
        }
    }

    public List<DropEntry> getDrops(final int rid) {
        List<DropEntry> ret = drops.get(rid);
        if (ret == null) {
            ret = new ArrayList<>(5);
            try {
                final Connection con = DatabaseConnection.getConnection();
                final PreparedStatement ps =
                    con.prepareStatement(
                        "SELECT itemid, chance FROM reactordrops WHERE reactorid = ? AND chance >= 0"
                    );
                ps.setInt(1, rid);
                final ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ret.add(new DropEntry(rs.getInt("itemid"), rs.getInt("chance")));
                }
                rs.close();
                ps.close();
            } catch (final Exception e) {
                log.error("Could not retrieve drops for reactor " + rid, e);
            }
            drops.put(rid, ret);
        }
        return ret;
    }

    public void clearDrops() {
        drops.clear();
    }
}
