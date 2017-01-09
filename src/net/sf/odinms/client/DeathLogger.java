package net.sf.odinms.client;

import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.EventLogger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class DeathLogger {
    private static final Path PATH = Paths.get("/etc/logs/death.log");
    private static final EventLogger LOGGER = new EventLogger(PATH);

    /**
     * This class cannot be instantiated; it is static.
     */
    private DeathLogger() {
    }

    /**
     * Logs info about a player's death.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @param p The player whose death is to be logged.
     * @param deathMap The ID of the map that the player died in.
     * @return <code>true</code> upon successful write,
     * <code>false</code> otherwise.
     */
    public static boolean logDeath(MapleCharacter p, int deathMap) {
        final List<String> toWrite = new ArrayList<>();
        Optional<List<Integer>> life = p.getLastPastLife();

        toWrite.add("================================================================================");
        toWrite.add(MapleClient.getLogMessage(p, ""));
        toWrite.add(
            "Time: " +
            System.currentTimeMillis() +
            ", channel: " +
            p.getClient().getChannel() +
            ", death map: " +
            deathMap
        );
        life.ifPresent(l -> {
            MapleMonster mob = MapleLifeFactory.getMonster(l.get(2));
            toWrite.add(
                "Level: " +
                l.get(0) +
                ", Job: " +
                MapleJob.getJobName(l.get(1)) +
                ", last damage source: " +
                (mob == null ? l.get(2) : mob.getName())
            );
        });
        toWrite.add("Mesos: " + p.getMeso() + ", new paragon level: " + p.getTotalParagonLevel());
        toWrite.add("Cheat summary: " + p.getCheatTracker().getSummary());
        toWrite.add("");

        return LOGGER.write(toWrite);
    }

    /**
     * Logs a list of items, for the purpose of logging all the items
     * that a player lost on death.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @param items A list of items to ge logged.
     * @param c The client of the player who lost the items.
     * @return <code>true</code> upon successful write,
     * <code>false</code> otherwise.
     */
    public static boolean logItems(List<IItem> items, MapleClient c) {
        final List<String> toWrite = new ArrayList<>();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        toWrite.add("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        toWrite.add(
            "Cleared items for " +
            c.getPlayer().getName() +
            " (Account: " +
            c.getAccountName() +
            "), at time " +
            System.currentTimeMillis()
        );
        toWrite.add("");

        items
            .stream()
            .sorted(Comparator.comparingInt(IItem::getType))
            .map(item -> {
                switch (item.getType()) {
                    case 1:
                        Equip eqp = (Equip) item;
                        StringBuilder statString = new StringBuilder();
                        statString.append(eqp.getItemId())
                                  .append(" ")
                                  .append(ii.getName(eqp.getItemId()))
                                  .append(";  ");
                        if (eqp.getAcc() > 0) {
                            statString.append("Accuracy: ").append(eqp.getAcc()).append(" ");
                        }
                        if (eqp.getAvoid() > 0) {
                            statString.append("Avoidability: ").append(eqp.getAvoid()).append(" ");
                        }
                        if (eqp.getStr() > 0) {
                            statString.append("Str: ").append(eqp.getStr()).append(" ");
                        }
                        if (eqp.getDex() > 0) {
                            statString.append("Dex: ").append(eqp.getDex()).append(" ");
                        }
                        if (eqp.getInt() > 0) {
                            statString.append("Int: ").append(eqp.getInt()).append(" ");
                        }
                        if (eqp.getLuk() > 0) {
                            statString.append("Luk: ").append(eqp.getLuk()).append(" ");
                        }
                        if (eqp.getHp() > 0) {
                            statString.append("MaxHP: ").append(eqp.getHp()).append(" ");
                        }
                        if (eqp.getMp() > 0) {
                            statString.append("MaxMP: ").append(eqp.getMp()).append(" ");
                        }
                        if (eqp.getJump() > 0) {
                            statString.append("Jump: ").append(eqp.getJump()).append(" ");
                        }
                        if (eqp.getSpeed() > 0) {
                            statString.append("Speed: ").append(eqp.getSpeed()).append(" ");
                        }
                        if (eqp.getWatk() > 0) {
                            statString.append("Attack: ").append(eqp.getWatk()).append(" ");
                        }
                        if (eqp.getMatk() > 0) {
                            statString.append("Magic Attack: ").append(eqp.getMatk()).append(" ");
                        }
                        if (eqp.getWdef() > 0) {
                            statString.append("W. def.: ").append(eqp.getWdef()).append(" ");
                        }
                        if (eqp.getMdef() > 0) {
                            statString.append("M. def.: ").append(eqp.getMdef()).append(" ");
                        }
                        statString.append("Slots: ").append(eqp.getUpgradeSlots());
                        return statString.toString();
                    case 2:
                        return
                            item.getItemId() +
                            " " +
                            ii.getName(item.getItemId()) +
                            ";  " +
                            "Quantity: " +
                            item.getQuantity();
                    default:
                        return "?????";
                }
            })
            .forEach(toWrite::add);

        toWrite.add("");
        
        return LOGGER.write(toWrite);
    }
}
