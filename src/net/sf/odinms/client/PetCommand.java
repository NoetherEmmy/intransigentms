package net.sf.odinms.client;

public class PetCommand {
    private final int petId, skillId, prob, inc;

    public PetCommand(final int petId, final int skillId, final int prob, final int inc) {
        this.petId = petId;
        this.skillId = skillId;
        this.prob = prob;
        this.inc = inc;
    }

    public int getPetId() {
        return petId;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getProbability() {
        return prob;
    }

    public int getIncrease() {
        return inc;
    }
}
