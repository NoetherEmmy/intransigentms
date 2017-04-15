package net.sf.odinms.client.status;

import net.sf.odinms.client.ISkill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
//import net.sf.odinms.tools.ArrayMap;

public class MonsterStatusEffect {
    private final Map<MonsterStatus, Integer> stati;
    private final ISkill skill;
    private final boolean monsterSkill;
    private ScheduledFuture<?> cancelTask;
    private ScheduledFuture<?> poisonSchedule;

    public MonsterStatusEffect(final Map<MonsterStatus, Integer> stati, final ISkill skill, final boolean monsterSkill) {
        this.stati = new ConcurrentHashMap<>(stati); // ArrayMap
        this.skill = skill;
        this.monsterSkill = monsterSkill;
    }

    public Map<MonsterStatus, Integer> getStati() {
        return stati;
    }

    public Integer setValue(final MonsterStatus status, final Integer newVal) {
        return stati.put(status, newVal);
    }

    public ISkill getSkill() {
        return skill;
    }

    public boolean isMonsterSkill() {
        return monsterSkill;
    }

    public ScheduledFuture<?> getCancelTask() {
        return cancelTask;
    }

    public void setCancelTask(final ScheduledFuture<?> cancelTask) {
        this.cancelTask = cancelTask;
    }

    public void removeActiveStatus(final MonsterStatus stat) {
        stati.remove(stat);
    }

    public void setPoisonSchedule(final ScheduledFuture<?> poisonSchedule) {
        this.poisonSchedule = poisonSchedule;
    }

    public void cancelPoisonSchedule() {
        if (poisonSchedule != null) poisonSchedule.cancel(false);
    }
}
