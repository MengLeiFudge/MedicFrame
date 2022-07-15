package medic.func.common.sakura.character.player;

import lombok.Data;
import lombok.EqualsAndHashCode;
import medic.func.common.sakura.character.Character;
import medic.func.common.sakura.items.Item;
import medic.func.common.sakura.skills.Skill;
import medic.func.common.sakura.tasks.Task;

import java.io.Serializable;
import java.util.List;

/**
 * @author MengLeiFudge
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Player extends Character implements Serializable {
    private static final long serialVersionUID = 1L;

    public Player(long qq) {
        super("unknown");
        this.qq = qq;
        this.maxExp = getExp(1, zhiType);
    }

    private long qq;
    private long exp = 0;
    private long maxExp;
    private ZhiType zhiType = ZhiType.未转职;

    private enum ZhiType {
        // 111
        未转职(1.0),
        战士(1.0),
        法师(1.0),
        牧师(1.0);

        double expRatio;

        ZhiType(double expRatio) {
            this.expRatio = expRatio;
        }
    }

    private enum ZhuanType {
        /**
         * 等级上限99
         */
        零转,
        /**
         * 等级上限139
         */
        一转,
        /**
         * 等级上限189
         */
        二转,
        /**
         * 等级上限200
         */
        三转
    }

    private enum JieType {
        /**
         * 1-19级
         */
        零阶,
        /**
         * 20-39级
         */
        一阶,
        /**
         * 40-69级
         */
        二阶,
        /**
         * 70-99级
         */
        三阶,
        /**
         * 100-139级
         */
        四阶,
        /**
         * 140-189级
         */
        五阶,
        /**
         * 190-200级
         */
        六阶
    }

    private ZhuanType zhuanType = ZhuanType.零转;
    private JieType jieType = JieType.零阶;

    public enum DianType {
        // 属性。
        LI,
        ZHI,
        TI,
        MIN,
        MEI
    }

    /**
     * 当前属性.
     */
    private int liliang;
    private int zhili;
    private int tizhi;
    private int minjie;
    private int meili;

    /**
     * 当前剩余可分配点数.
     */
    private int dian;

    /**
     * 玩家自己分配的属性.
     */
    private int liliangAdd;
    private int zhiliAdd;
    private int tizhiAdd;
    private int minjieAdd;
    private int meiliAdd;

    private List<Item> items;
    private long money;
    private List<Skill> skills;
    private List<Task> tasks;

    private Item head;
    private Item neck;
    private Item body;
    private Item leftWrist;
    private Item rightWrist;
    private Item leftHand;
    private Item rightHand;
    private Item hands;
    private Item[] fingers = new Item[10];

    public void addExp(long exp) {
        this.exp += exp;
    }

    public static long getExp(int level, ZhiType zhi) {
        double k1 = 1;
        double k2 = 82.857143;
        double k3 = 1714.2857;
        double c = 3000;
        return (long) ((k1 * level * level * level + k2 * level * level + k3 * level + c) * 0.3 * zhi.expRatio);
    }

    public void levelUp() {
        while (exp >= maxExp) {
            exp -= maxExp;
            setLevel(getLevel() + 1);
            maxExp = getExp(getLevel(), zhiType);

            setHp(getLevel() * 100L);
            setMaxHp(getLevel() * 100L);
            setMp(getLevel() * 100L);
            setMaxMp(getLevel() * 100L);

            setDian(getDian() + 5);
            setLiliang(getLiliang() + 1);
            setZhili(getZhili() + 1);
            setTizhi(getTizhi() + 1);
            setMinjie(getMinjie() + 1);
            setMeili(getMeili() + 1);
        }
    }

    public void addMoney(long num) {
        this.money += num;
    }

    private long lastSignInTime = 0L;
    private int weekSignInTimes = 0;
    private int monthSignInTimes = 0;

    private State state = State.COMMON;

    enum State {
        COMMON,
        FUBEN,
        JJC,
        XIUXI,
    }


}

