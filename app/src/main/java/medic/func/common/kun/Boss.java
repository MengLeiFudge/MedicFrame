package medic.func.common.kun;

import medic.core.Utils;

import java.io.Serializable;

import static medic.core.Utils.getRandomDouble;
import static medic.core.Utils.getRandomInt;
import static medic.func.common.kun.Rank.getMaxLevel;

/**
 * @author MengLeiFudge
 */
public class Boss implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int level;
    private int atk;
    private int def;
    private int hp;

    Boss() {
        newBoss();
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public int getAtk() {
        return atk;
    }

    public int getDef() {
        return def;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    /**
     * 以所有群等级最高者为基础，生成新的boss
     * 如果无人上榜，boss等级默认5000级
     */
    public void newBoss() {
        level = Math.max(5000, getMaxLevel());
        name = Utils.getRandomChineseStr(1, 4);
        atk = (int) (level * getRandomDouble(1.6, 2.8));
        def = (int) (level * getRandomDouble(0.8, 1.4));
        hp = level * getRandomInt(8000, 14000);
    }
}
