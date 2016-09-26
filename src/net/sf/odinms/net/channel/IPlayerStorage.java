package net.sf.odinms.net.channel;

import java.util.Collection;
import net.sf.odinms.client.MapleCharacter;

public interface IPlayerStorage {

    MapleCharacter getCharacterByName(String name);

    MapleCharacter getCharacterById(int id);

    Collection<MapleCharacter> getAllCharacters();
}