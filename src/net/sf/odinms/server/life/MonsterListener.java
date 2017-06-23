package net.sf.odinms.server.life;

public interface MonsterListener {
    /**
     * @param monster The monster that was killed.
     */
    void monsterKilled(MapleMonster monster);
}
