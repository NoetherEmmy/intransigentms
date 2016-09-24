package net.sf.odinms.client;

public enum MapleJob {
    BEGINNER(0),
    WARRIOR(100),
    FIGHTER(110),
    CRUSADER(111),
    HERO(112),
    PAGE(120),
    WHITEKNIGHT(121),
    PALADIN(122),
    SPEARMAN(130),
    DRAGONKNIGHT(131),
    DARKKNIGHT(132),
    MAGICIAN(200),
    FP_WIZARD(210),
    FP_MAGE(211),
    FP_ARCHMAGE(212),
    IL_WIZARD(220),
    IL_MAGE(221),
    IL_ARCHMAGE(222),
    CLERIC(230),
    PRIEST(231),
    BISHOP(232),
    BOWMAN(300),
    HUNTER(310),
    RANGER(311),
    BOWMASTER(312),
    CROSSBOWMAN(320),
    SNIPER(321),
    CROSSBOWMASTER(322),
    THIEF(400),
    ASSASSIN(410),
    HERMIT(411),
    NIGHTLORD(412),
    BANDIT(420),
    CHIEFBANDIT(421),
    SHADOWER(422),
    PIRATE(500),
    BRAWLER(510),
    MARAUDER(511),
    BUCCANEER(512),
    GUNSLINGER(520),
    OUTLAW(521),
    CORSAIR(522),
    GM(900),
    SUPERGM(910);
    final int jobid;

    private MapleJob(int id) {
        jobid = id;
    }

    public int getId() {
        return jobid;
    }

    public static MapleJob getById(int id) {
        for (MapleJob l : MapleJob.values()) {
            if (l.getId() == id) {
                return l;
            }
        }
        return null;
    }

    public static MapleJob getBy5ByteEncoding(int encoded) {
        switch (encoded) {
            case 2:
                return WARRIOR;
            case 4:
                return MAGICIAN;
            case 8:
                return BOWMAN;
            case 16:
                return THIEF;
            case 32:
                return PIRATE;
            default:
                return BEGINNER;
        }
    }

    public boolean isA(MapleJob basejob) {
        return getId() >= basejob.getId() && getId() / 100 == basejob.getId() / 100;
    }
    
    public static String getJobName(int id) {
        switch (id) {
            case 0: return "Beginner";
            case 100: return "Warrior";
            case 110: return "Fighter";
            case 111: return "Crusader";
            case 112: return "Hero";
            case 120: return "Page";
            case 121: return "White Knight";
            case 122: return "Paladin";
            case 130: return "Spearman";
            case 131: return "Dragon Knight";
            case 132: return "Dark Knight";
            case 200: return "Magician";
            case 210: return "Fire/Poison Wizard";
            case 211: return "Fire/Posion Mage";
            case 212: return "Fire/Poison Archmage";
            case 220: return "Ice/Lightning Wizard";
            case 221: return "Ice/Lightning Mage";
            case 222: return "Ice/Lightning Archmage";
            case 230: return "Cleric";
            case 231: return "Priest";
            case 232: return "Bishop";
            case 300: return "Bowman";
            case 310: return "Hunter";
            case 320: return "Crossbowman";
            case 311: return "Ranger";
            case 321: return "Sniper";
            case 312: return "Bowmaster";
            case 322: return "Marksman";
            case 400: return "Thief";
            case 410: return "Assassin";
            case 420: return "Bandit";
            case 411: return "Hermit";
            case 421: return "Bandit";
            case 412: return "Night Lord";
            case 422: return "Shadower";
            case 500: return "Pirate";
            case 510: return "Brawler";
            case 511: return "Marauder";
            case 512: return "Buccaneer";
            case 520: return "Gunslinger";
            case 521: return "Outlaw";
            case 522: return "Corsair";
            case 900: return "GM";
            case 910: return "Super GM";
            default: return "";
        }
    }
}
