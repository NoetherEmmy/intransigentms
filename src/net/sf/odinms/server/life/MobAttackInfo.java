package net.sf.odinms.server.life;

public class MobAttackInfo {
    private int mpBurn, diseaseSkill, diseaseLevel, mpCon;

    public MobAttackInfo(final int mobId, final int attackId) {
    }

    public void setDeadlyAttack() {
        //this.isDeadlyAttack = isDeadlyAttack;
        final boolean isDeadlyAttack1 = false;
    }

    public boolean isDeadlyAttack() {
        //return isDeadlyAttack;
        return false;
    }

    public void setMpBurn(final int mpBurn) {
        this.mpBurn = mpBurn;
    }

    public int getMpBurn() {
        return mpBurn;
    }

    public void setDiseaseSkill(final int diseaseSkill) {
        this.diseaseSkill = diseaseSkill;
    }

    public int getDiseaseSkill() {
        return diseaseSkill;
    }

    public void setDiseaseLevel(final int diseaseLevel) {
        this.diseaseLevel = diseaseLevel;
    }

    public int getDiseaseLevel() {
        return diseaseLevel;
    }

    public void setMpCon(final int mpCon) {
        this.mpCon = mpCon;
    }

    public int getMpCon() {
        return mpCon;
    }
}
