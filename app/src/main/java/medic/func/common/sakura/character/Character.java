package medic.func.common.sakura.character;

import lombok.Data;

import java.io.Serializable;

/**
 * @author MengLeiFudge
 */
@Data
public abstract class Character implements Serializable {
    private static final long serialVersionUID = 1L;

    private Character() {
    }

    /**
     * 人物使用该模板构建.
     *
     * @param name 角色昵称
     */
    protected Character(String name) {
        this.name = name;
        this.level = 1;
        hp = maxHp = 100;
        mp = maxMp = 100;
        defeatExp = 100;
        phyAtk = 20;
        magAtk = 20;
        phyDef = 5;
        magDef = 5;
        speed = 100;
    }

    /**
     * 怪物使用该模板构建.
     *
     * @param name
     * @param level
     * @param maxHp
     * @param maxMp
     * @param defeatExp
     * @param phyAtk
     * @param magAtk
     * @param phyDef
     * @param magDef
     * @param speed
     */
    protected Character(String name, int level, long maxHp, long maxMp, long defeatExp,
                        int phyAtk, int magAtk,
                        int phyDef, int magDef, int speed) {
        this.name = name;
        this.level = level;
        this.hp = this.maxHp = maxHp;
        this.mp = this.maxMp = maxMp;
        this.defeatExp = defeatExp;
        this.phyAtk = phyAtk;
        this.magAtk = magAtk;
        this.phyDef = phyDef;
        this.magDef = magDef;
        this.speed = speed;
    }

    private String name;
    private int level;
    private long hp;
    private long maxHp;
    private long mp;
    private long maxMp;
    /**
     * 被击败时，对方可以获得的经验
     */
    private long defeatExp;

    private int phyAtk;
    private double phyCritRate = 0.0;
    private double phyCritEffect = 2.0;
    private int magAtk;
    private double magCritRate = 0.0;
    private double magCritEffect = 2.0;

    private int phyDef;
    private int magDef;
    ///**
    // * 命中率，范围0.0-1.0
    // */
    //private double hitRate;
    ///**
    // * 闪避率，范围0.0-1.0
    // */
    //private double dodgeRate;
    /**
     * 速度，根据该项得到双方命中、闪避几率
     */
    private int speed;

    /**
     * 金木水火土日月灵，人物本身只有属性有效度，没有属性
     */
    private double metal = 1;
    private double wood = 1;
    private double water = 1;
    private double fire = 1;
    private double earth = 1;
    private double sun = 1;
    private double moon = 1;
    private double soul = 1.5;

}
