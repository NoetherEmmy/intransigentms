package net.sf.odinms.server.life;

public class MobAttackInfo {

    private int mpBurn;
    private int diseaseSkill;
    private int diseaseLevel;
    private int mpCon;

    public MobAttackInfo(int mobId, int attackId) {
    }

    public void setDeadlyAttack() {
        //this.isDeadlyAttack = isDeadlyAttack;
        boolean isDeadlyAttack1 = false;
    }

    public boolean isDeadlyAttack() {
        //return isDeadlyAttack;
        return false;
    }

    public void setMpBurn(int mpBurn) {
        this.mpBurn = mpBurn;
    }

    public int getMpBurn() {
        return mpBurn;
    }

    public void setDiseaseSkill(int diseaseSkill) {
        this.diseaseSkill = diseaseSkill;
    }

    public int getDiseaseSkill() {
        return diseaseSkill;
    }

    public void setDiseaseLevel(int diseaseLevel) {
        this.diseaseLevel = diseaseLevel;
    }

    public int getDiseaseLevel() {
        return diseaseLevel;
    }

    public void setMpCon(int mpCon) {
        this.mpCon = mpCon;
    }

    public int getMpCon() {
        return mpCon;
    }
}