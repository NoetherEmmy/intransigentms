package net.sf.odinms.net.channel;

import net.sf.odinms.client.MapleCharacter;

import java.util.Collection;

public interface IPlayerStorage {
    MapleCharacter getCharacterByName(String name);

    MapleCharacter getCharacterById(int id);

    Collection<MapleCharacter> getAllCharacters();
}
