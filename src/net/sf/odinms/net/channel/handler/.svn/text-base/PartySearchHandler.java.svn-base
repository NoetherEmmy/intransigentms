package net.sf.odinms.net.channel.handler;

import java.util.ArrayList;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class PartySearchHandler extends AbstractMaplePacketHandler {
    //All bolleans as for now, all is useless... just for testing somehow, but I changed method while making
    boolean All = false, Beginner = false,
            AllWarriors = false, Warrior1 = false, Warrior2 = false, Warrior3 = false,
            AllMagician = false, Magician1 = false, Magician2 = false, Magician3 = false,
            AllPirate = false, Pirate1 = false, Pirate2 = false,
            AllThief = false, Thief1 = false, Thief2 = false,
            AllBowman = false, Bowman1 = false, Bowman2 = false;
    ArrayList<Integer> boxsumconstructor = new ArrayList<Integer>();

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        int min = slea.readInt();
        int max = slea.readInt();
        int person = slea.readInt();
        int box = slea.readInt();
        System.out.println("MINIMUM PLAYERS: " + min);
        System.out.println("MAXIMUM PLAYERS: " + max);
        System.out.println("AMOUNT OF PLAYERS: " + person);
        System.out.println("BOX VALUE: " + box);
        String binary = Integer.toBinaryString(box);
        String reverse = "";
        System.out.println("Binary: " + binary);
        for (int z = 0; z < binary.length(); z++) {
            reverse = binary.charAt(z) + reverse;
        }
        System.out.println("Total Reverse: " + reverse);
        char letters;
        for (int z = 0; z < reverse.length(); z++) {
            letters = reverse.charAt(z);
            System.out.println("1 By 1 for jobs Current one is : " + letters);
            isJob(letters,z,c);
        }
    }

    public void isJob(char binary, int times, MapleClient c) {
        times++;
        if (binary == 1) {
            if (times == 1) {
                All = true;
                System.out.println("All have been choosen");
            } else if (times == 2) {
                Beginner = true;
                System.out.println("Oh Noes, it's Beginners!!");
            } else if (times == 3) {
                AllWarriors = true;
                System.out.println("All Warriors are here");
            } else if (times == 4) {
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Fighter!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Crusader!!");
                } else {
                    System.out.println("Oh Noes, it's a Hero!!");
                }
                Warrior1 = true;
            } else if (times == 5) {
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Page!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a White Knight!!");
                } else {
                    System.out.println("Oh Noes, it's a Paladin!!");
                }
                Warrior2 = true;
            } else if (times == 6) {
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Spearman!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Dragon Knight!!");
                } else {
                    System.out.println("Oh Noes, it's a Dark Knight!!");
                }
                Warrior3 = true;
            } else if (times == 7) {
                AllMagician = true;
                System.out.println("All Magicians are here");
            } else if (times == 8) {
                Magician1 = true;
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Ice Lighting Wizard!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Ice Lighting Mage!!");
                } else {
                    System.out.println("Oh Noes, it's a Ice Lighting ArchMage!!");
                }
            } else if (times == 9) {
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Fire Poison Wizard!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Fire Poison Mage!!");
                } else {
                    System.out.println("Oh Noes, it's a Fire Posion ArchMage!!");
                }
                Magician2 = true;
            } else if (times == 10) {
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Cleric!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Priest!!");
                } else {
                    System.out.println("Oh Noes, it's a Bishop!!");
                }
                Magician3 = true;
            } else if (times == 11) {
                AllPirate = true;
                System.out.println("All Pirate are here");
            } else if (times == 12) {
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Gunslinger!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Outlaw!!");
                } else {
                    System.out.println("Oh Noes, it's a Corsair!!");
                }
                Pirate1 = true;
            } else if (times == 13) {
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Brawler!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Marauder!!");
                } else {
                    System.out.println("Oh Noes, it's a Buccaneer!!");
                }
                Pirate2 = true;
            } else if (times == 14) {
                System.out.println("All Thiefs are here");
                AllThief = true;
            } else if (times == 15) {
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Assasin!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Hermit!!");
                } else {
                    System.out.println("Oh Noes, it's a Night Lord!!");
                }
                Thief1 = true;
            } else if (times == 16) {
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Bandit!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Chief Bandit!!");
                } else {
                    System.out.println("Oh Noes, it's a Shadower!!");
                }
                Thief2 = true;
            } else if (times == 17) {
                AllBowman = true;
                System.out.println("All Bowmans are here");
            } else if (times == 18) {
                Bowman1 = true;
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Hunter!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Ranger!!");
                } else {
                    System.out.println("Oh Noes, it's a Bow Master!!");
                }
            } else if (times == 19) {
                Bowman2 = true;
                if (c.getPlayer().getLevel() < 70) {
                    System.out.println("Oh Noes, it's a Crossbow Man!!");
                } else if (c.getPlayer().getLevel() < 120) {
                    System.out.println("Oh Noes, it's a Sniper!!");
                } else {
                    System.out.println("Oh Noes, it's a Marksman!!");
                }
            }
        }
    }
}