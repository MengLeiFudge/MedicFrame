package medic.func.common.sakura.character.monsters;

import medic.func.common.sakura.character.Character;

import java.io.Serializable;

/**
 * @author MengLeiFudge
 */
public abstract class Monster extends Character implements Serializable {

    Monster(String name, int level, long maxHp, long maxMp, long defeatExp,
            int phyAtk, int magAtk,
            int phyDef, int magDef, int speed) {
        super(name, level, maxHp, maxMp, defeatExp,
                phyAtk, magAtk,
                phyDef, magDef, speed);
    }

}
