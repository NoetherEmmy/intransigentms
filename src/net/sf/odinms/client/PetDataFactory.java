package net.sf.odinms.client;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.tools.Pair;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PetDataFactory {
    private static final MapleDataProvider dataRoot =
        MapleDataProviderFactory.getDataProvider(
            new File(
                System.getProperty("net.sf.odinms.wzpath") + "/Item.wz"
            )
        );
    private static final Map<Pair<Integer, Integer>, PetCommand> petCommands = new HashMap<>();
    private static final Map<Integer, Integer> petHunger = new HashMap<>();

    public static PetCommand getPetCommand(final int petId, final int skillId) {
        PetCommand ret = petCommands.get(new Pair<>(petId, skillId));
        if (ret != null) return ret;
        synchronized (petCommands) {
            // Check if someone else that's also synchronized has loaded the skill by now.
            ret = petCommands.get(new Pair<>(petId, skillId));
            if (ret == null) {
                final MapleData skillData = dataRoot.getData("Pet/" + petId + ".img");
                int prob = 0, inc = 0;
                if (skillData != null) {
                    prob = MapleDataTool.getInt("interact/" + skillId + "/prob", skillData, 0);
                    inc = MapleDataTool.getInt("interact/" + skillId + "/inc", skillData, 0);
                }
                ret = new PetCommand(petId, skillId, prob, inc);
                petCommands.put(new Pair<>(petId, skillId), ret);
            }
            return ret;
        }
    }

    public static int getHunger(final int petId) {
        Integer ret = petHunger.get(petId);
        if (ret != null) return ret;
        synchronized (petHunger) {
            ret = petHunger.get(petId);
            if (ret == null) {
                final MapleData hungerData = dataRoot.getData("Pet/" + petId + ".img").getChildByPath("info/hungry");
                ret = MapleDataTool.getInt(hungerData, 1);
                petHunger.put(petId, ret);
            }
            return ret;
        }
    }
}
