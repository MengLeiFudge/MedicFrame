package medic.func.common.pcr;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MengLeiFudge
 */
@Data
public class Boss implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long group;

    Boss(long group) {
        this.group = group;
    }

    /**
     * 当前是第几周目.
     */
    private int loop = 1;
    /**
     * 当前是第几只，范围是1-5.
     */
    private int index = 1;

    protected static final String[] BOSS_NUM = {"一", "二", "三", "四", "五"};

    /**
     * 返回当前名字.
     *
     * @return 当前名字
     */
    public String getName() {
        return BOSS_NUM[index - 1] + "王";
    }

    public enum Stage {
        // 第一阶段，水
        FIRST,
        // 第二阶段，正常
        SECOND,
        // 第三阶段，难得一匹
        THIRD;

        @Override
        public String toString() {
            if (this == FIRST) {
                return "A面";
            } else if (this == SECOND) {
                return "B面";
            } else {
                return "C面";
            }
        }

        public int getIndex() {
            if (this == FIRST) {
                return 0;
            } else if (this == SECOND) {
                return 1;
            } else {
                return 2;
            }
        }

        public static List<Stage> toSortedList() {
            List<Stage> list = new ArrayList<>();
            list.add(FIRST);
            list.add(SECOND);
            list.add(THIRD);
            return list;
        }
    }

    /**
     * 返回当前是哪个阶段.
     *
     * @return 当前阶段
     */
    public Stage getStage() {
        return getStage(loop);
    }

    /**
     * 返回下周目是哪个阶段.
     *
     * @return 下周目对应阶段
     */
    public Stage getNextStage() {
        return getStage(loop + 1);
    }

    private Stage getStage(int loop) {
        if (loop <= 3) {
            return Stage.FIRST;
        } else if (loop <= 10) {
            return Stage.SECOND;
        } else {
            return Stage.THIRD;
        }
    }

    /**
     * 当前剩余血量.
     */
    private int nowHp = getMaxHp();

    private static final int[][] MAX_HP = {
            {6000000, 8000000, 10000000, 12000000, 15000000},
            {6000000, 8000000, 10000000, 12000000, 15000000},
            {6000000, 8000000, 10000000, 12000000, 15000000},
            {17000000, 18000000, 20000000, 21000000, 23000000},
            {85000000, 90000000, 95000000, 100000000, 110000000},
    };

    /**
     * 返回当前血量上限.
     *
     * @return 当前血量上限
     */
    public int getMaxHp() {
        return MAX_HP[getStage().getIndex()][index - 1];
    }

    private final int[][] expectedDam = {
            {2200000, 2200000, 1800000, 1800000, 2000000},
            {2000000, 2000000, 1600000, 1700000, 2000000},
            {1500000, 1800000, 1500000, 1600000, 1500000},
            {1500000, 1800000, 1500000, 1600000, 1500000},
            {1500000, 1800000, 1500000, 1600000, 1500000},
    };

    /**
     * 返回当前boss的预估伤害.
     *
     * @return 当前boss的预估伤害
     */
    public int getExpectedDam() {
        return expectedDam[getStage().getIndex()][index - 1];
    }

    /**
     * 返回指定boss的预估伤害.
     *
     * @param stage   阶段
     * @param bossNum 第几只
     * @return 指定boss的预估伤害
     */
    public int getExpectedDam(Stage stage, int bossNum) {
        return expectedDam[stage.getIndex()][bossNum - 1];
    }

    /**
     * 设置预估伤害.
     *
     * @param stage       阶段
     * @param bossNum     第几只
     * @param expectedDam 预估伤害
     */
    public void setExpectedDam(Stage stage, int bossNum, int expectedDam) {
        this.expectedDam[stage.getIndex()][bossNum - 1] = expectedDam;
    }

    public void subHp(int dam) {
        if (dam < nowHp) {
            nowHp -= dam;
            return;
        }
        if (index != 5) {
            index++;
        } else {
            loop++;
            index = 1;
        }
        nowHp = getMaxHp();
    }

    public void addHp(int lastDam) {
        if (nowHp != getMaxHp()) {
            nowHp += lastDam;
            return;
        }
        if (index != 1) {
            index--;
        } else {
            loop--;
            index = 5;
        }
        nowHp = lastDam;
    }
}
