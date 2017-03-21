package net.sf.odinms.server.quest;

import net.sf.odinms.client.MapleQuestStatus;
import net.sf.odinms.database.DatabaseConnection;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class MapleCustomQuest extends MapleQuest {
    public MapleCustomQuest(int id) {
        try {
            this.id = id;
            startActs = new ArrayList<>();
            completeActs = new ArrayList<>();
            startReqs = new ArrayList<>();
            completeReqs = new ArrayList<>();
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM questrequirements WHERE questid = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            MapleQuestRequirement req;
            MapleCustomQuestData data;
            while (rs.next()) {
                Blob blob = rs.getBlob("data");
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                    blob.getBytes(1, (int) blob.length())));
                data = (MapleCustomQuestData) ois.readObject();
                req = new MapleQuestRequirement(
                    this,
                    MapleQuestRequirementType.getByWZName(data.getName()),
                    data
                );
                MapleQuestStatus.Status status = MapleQuestStatus.Status.getById(rs.getInt("status"));
                if (status.equals(MapleQuestStatus.Status.NOT_STARTED)) {
                    startReqs.add(req);
                } else if (status.equals(MapleQuestStatus.Status.STARTED)) {
                    completeReqs.add(req);
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT * FROM questactions WHERE questid = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            MapleQuestAction act;
            while (rs.next()) {
                Blob blob = rs.getBlob("data");
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                    blob.getBytes(1, (int) blob.length())));
                data = (MapleCustomQuestData) ois.readObject();
                act = new MapleQuestAction(MapleQuestActionType.getByWZName(data.getName()), data, this);
                MapleQuestStatus.Status status = MapleQuestStatus.Status.getById(
                    rs.getInt("status"));
                if (status.equals(MapleQuestStatus.Status.NOT_STARTED)) {
                    startActs.add(act);
                } else if (status.equals(MapleQuestStatus.Status.STARTED)) {
                    completeActs.add(act);
                }
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            log.error("Error loading custom quest. ", e);
        }
    }
}
