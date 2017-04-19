package net.sf.odinms.client;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.Element;

import java.util.*;

public class Skill implements ISkill {
    private static final int[] VSKILL_CLASSES = {230, 232, 300, 310, 311, 312, 511};
    private static final int[][] VSKILL_IDS = {
        {1200001},
        {1221002},
        {4001334},
        {1300000, 1301004, 1311003, 420000, 4201002},
        {1310000, 4001003, 4220002},
        {1311001, 4211002, 4220005},
        {4111006}
    };
    private final int id;
    private final List<MapleStatEffect> effects = new ArrayList<>();
    private Element element;
    private int animationTime;
    private final Map<Integer, Integer> requirements = new LinkedHashMap<>(3);
    private final Set<Integer> vskillJobs = new HashSet<>(2);

    private Skill(final int id) {
        super();
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    public static Skill loadFromData(final int id, final MapleData data) {
        final Skill ret = new Skill(id);
        boolean isBuff = false;
        final int skillType = MapleDataTool.getInt("skillType", data, -1);
        final String elem = MapleDataTool.getString("elemAttr", data, null);
        if (elem != null) {
            ret.element = Element.getFromChar(elem.charAt(0));
        } else {
            ret.element = Element.NEUTRAL;
        }
        final MapleData effect = data.getChildByPath("effect");
        if (skillType != -1) {
            if (skillType == 2) isBuff = true;
        } else {
            final MapleData action = data.getChildByPath("action");
            final MapleData hit = data.getChildByPath("hit");
            final MapleData ball = data.getChildByPath("ball");
            isBuff = effect != null && hit == null && ball == null;
            isBuff |= action != null && MapleDataTool.getString("0", action, "").equals("alert2");
            switch (id) {
                case 1121006: // rush
                case 1221007: // rush
                case 1321003: // rush
                case 1311005: // sacrifice
                case 2121001: // big bang
                case 2221001: // big bang
                case 2321001: // big bang
                case 2111002: // explosion
                case 2111003: // poison mist
                case 2301002: // heal
                case 3110001: // mortal blow
                case 3210001: // mortal blow
                case 4101005: // drain
                case 4111003: // shadow web
                case 4201004: // steal
                case 4221006: // smokescreen
                case 9101000: // heal + dispel
                case 5201006: // Recoil Shot
                case 5111004: // Energy Drain
                case 1121001: // monster magnet
                case 1221001: // monster magnet
                case 1321001: // monster magnet
                    isBuff = false;
                    break;
                case 1001: // recovery
                case 1002: // nimble feet
                case 1004: // monster riding
                case 1005: // echo of hero
                case 1001003: // iron body
                case 1101004: // sword booster
                case 1201004: // sword booster
                case 1101005: // axe booster
                case 1201005: // bw booster
                case 1301004: // spear booster
                case 1301005: // polearm booster
                case 3101002: // bow booster
                case 3201002: // crossbow booster
                case 4101003: // claw booster
                case 4201002: // dagger booster
                case 1101007: // power guard
                case 1201007: // power guard
                case 1101006: // rage
                case 1301006: // iron will
                case 1301007: // hyperbody
                case 1111002: // combo attack
                case 1211006: // blizzard charge bw
                case 1211004: // fire charge bw
                case 1211008: // lightning charge bw
                case 1221004: // divine charge bw
                case 1211003: // fire charge sword
                case 1211005: // ice charge sword
                case 1211007: // thunder charge sword
                case 1221003: // holy charge sword
                case 1311008: // dragon blood
                case 1121000: // maple warrior
                case 1221000: // maple warrior
                case 1321000: // maple warrior
                case 2121000: // maple warrior
                case 2221000: // maple warrior
                case 2321000: // maple warrior
                case 3121000: // maple warrior
                case 3221000: // maple warrior
                case 4121000: // maple warrior
                case 4221000: // maple warrior
                case 1121002: // power stance
                case 1221002: // power stance
                case 1321002: // power stance
                case 1121010: // enrage
                case 1321007: // beholder
                case 1320008: // beholder healing
                case 1320009: // beholder buff
                case 2001002: // magic guard
                case 2001003: // magic armor
                case 2101001: // meditation
                case 2201001: // meditation
                case 2301003: // invincible
                case 2301004: // bless
                case 2111005: // spell booster
                case 2211005: // spell booster
                case 2311003: // holy symbol
                case 2311006: // summon dragon
                case 2121004: // infinity
                case 2221004: // infinity
                case 2321004: // infinity
                case 2321005: // holy shield
                case 2121005: // elquines
                case 2221005: // ifrit
                case 2321003: // bahamut
                case 3121006: // phoenix
                case 3221005: // frostprey
                case 3111002: // puppet
                case 3211002: // puppet
                case 3111005: // silver hawk
                case 3211005: // golden eagle
                case 3001003: // focus
                case 3101004: // soul arrow bow
                case 3201004: // soul arrow crossbow
                case 3121002: // sharp eyes
                case 3221002: // sharp eyes
                case 3121008: // concentrate
                case 3221006: // blind
                case 4001003: // dark sight
                case 4101004: // haste
                case 4201003: // haste
                case 4111001: // meso up
                case 4111002: // shadow partner
                case 4121006: // shadow stars
                case 4211003: // pick pocket
                case 4211005: // meso guard
                case 5111005: // Transformation (Marauder)
                case 5121003: // Super Transformation (Viper)
                case 5220002: // wrath of the octopi
                case 5211001: // Pirate octopus summon
                case 5211002: // Pirate bird summon
                case 5221006: // Battleship
                case 9001000: // haste
                case 9101001: // super haste
                case 9101002: // holy symbol
                case 9101003: // bless
                case 9101004: // hide
                case 9101008: // hyper body
                case 1121011: // hero's will
                case 1221012: // hero's will
                case 1321010: // hero's will
                case 2321009: // hero's will
                case 2221008: // hero's will
                case 2121008: // hero's will
                case 3121009: // hero's will
                case 3221008: // hero's will
                case 4121009: // hero's will
                case 4221008: // hero's will
                case 2101003: // slow
                case 2201003: // slow
                case 2111004: // seal
                case 2211004: // seal
                case 1111007: // armor crash
                case 1211009: // magic crash
                case 1311007: // power crash
                case 2311005: // doom
                case 2121002: // mana reflection
                case 2221002: // mana reflection
                case 2321002: // mana reflection
                case 2311001: // dispel
                case 1201006: // threaten
                case 4121004: // ninja ambush
                case 4221004: // ninja ambush
                case 4121003: // taunt
                case 4221003: // taunt
                    isBuff = true;
                    break;
            }
        }
        for (final MapleData level : data.getChildByPath("level")) {
            final MapleStatEffect statEffect = MapleStatEffect.loadSkillEffectFromData(level, id, isBuff);
            ret.effects.add(statEffect);
        }

        final MapleData reqData = data.getChildByPath("req");
        if (reqData != null) {
            for (final MapleData req : reqData) {
                ret.requirements.put(
                    Integer.parseInt(req.getName()),
                    MapleDataTool.getInt(req.getName(), reqData, 0)
                );
            }
        }

        ret.animationTime = 0;
        if (effect != null) {
            for (final MapleData effectEntry : effect) {
                ret.animationTime += MapleDataTool.getIntConvert("delay", effectEntry, 0);
            }
        }

        for (int i = 0, len = VSKILL_IDS.length; i < len; ++i) {
            for (final int vskillId : VSKILL_IDS[i]) {
                if (id != vskillId) continue;
                ret.vskillJobs.add(VSKILL_CLASSES[i]);
                break;
            }
        }

        return ret;
    }

    @Override
    public MapleStatEffect getEffect(final int level) {
        return effects.get(level - 1);
    }

    @Override
    public int getMaxLevel() {
        return effects.size();
    }

    @Override
    public boolean canBeLearnedBy(final MapleJob job) {
        final int jid = job.getId();
        final int skillForJob = id / 10000;
        return
            !(jid / 100 != skillForJob / 100 && skillForJob / 100 != 0) &&
            (skillForJob / 10) % 10 <= (jid / 10) % 10 &&
            skillForJob % 10 <= jid % 10;
    }

    @Override
    public boolean isFourthJob() {
        return ((id / 10000) % 10) == 2;
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public int getAnimationTime() {
        return animationTime;
    }

    @Override
    public boolean isBeginnerSkill() {
        final String idString = String.valueOf(id);
        return idString.length() == 4 || idString.length() == 1;
    }

    @Override
    public boolean isGMSkill() {
        return id > 9000000;
    }

    @Override
    public Map<Integer, Integer> getRequirements() {
        return requirements;
    }

    @Override
    public Set<Integer> getVskillJobs() {
        return vskillJobs;
    }

    @Override
    public boolean isVskill() {
        return !vskillJobs.isEmpty();
    }
}
