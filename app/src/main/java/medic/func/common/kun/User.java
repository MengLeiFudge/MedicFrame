package medic.func.common.kun;

import lombok.Data;

import java.io.Serializable;

import static medic.core.Api.msgTime;
import static medic.core.Utils.getFibonacci;
import static medic.core.Utils.getRandomChineseStr;
import static medic.core.Utils.getRandomDistributionInt;
import static medic.core.Utils.getZeroHourTimestamp;

/**
 * @author MengLeiFudge
 */
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private long qq;
    private int season;
    private boolean openNewSeasonTip = false;
    private String name;
    private int level;
    private int atk = 1;
    private int def = 1;
    private int hp = 1;
    private int allSignInTimes = 0;
    private int weekSignInTimes = 0;
    private String lastSignInDate = "";
    /**
     * 好感度，最近七天操作次数，影响属性加成
     */
    private int[] favorite = new int[7];


    private int money = 0;
    private int gmk = 0;
    private int xlk = 0;
    private int ckk = 0;
    private int tzq = 0;


    // 何时更新，0-23某一个整点
    private int resetTime = 0;
    // 上次设置resetTime的时间
    private long lastSetTime = 0;
    // 上一次摸的时间
    private long mkTime = 0;
    // 已经摸的次数
    private int mkTimes = 0;
    private long jjTime = 0;
    private int jjTimes = 0;
    private long tzTime = 0;
    private int tzTimes = 0;

    User(long qq) {
        this.qq = qq;
        season = getSeason();
        this.name = getRandomChineseStr(1, 4);
        this.level = getRandomDistributionInt(100, 2000);
    }

    public void addMoney(int add) {
        this.money += add;
    }

    public void subMoney(int sub) {
        this.money -= sub;
    }

    public long mkTargetTime() {
        // region 获取不超过现在时间的最近的重置时间戳
        long reset = getZeroHourTimestamp(msgTime) + resetTime * 3600000;
        if (reset > msgTime) {
            // eg：现在是早上五点，重置时间是早上八点，则重置时间改为昨天早上八点
            reset -= 86400000;
        }
        // endregion 获取上一次重置时间戳
        if (mkTime < reset) {
            // 上一次在reset前，且现在在reset后（必然），则重置时间，返回msgTime
            mkTime = msgTime;
            mkTimes = 1;
            return msgTime;
        }
        // 上一次在reset后，判断时间是否到达目标时间
        long targetTime = mkTime + Math.min(getFibonacci(mkTimes + 1), 180) * 60000;
        if (targetTime > msgTime) {
            // 不满足条件，返回目标时间和下一个resetTime点中较小的一个
            return Math.min(targetTime, reset + 86400000);
        } else {
            // 满足条件，返回msgTime
            mkTime = msgTime;
            mkTimes++;
            return msgTime;
        }
    }

    public long jjTargetTime() {
        long reset = getZeroHourTimestamp(msgTime) + resetTime * 3600000;
        if (reset > msgTime) {
            reset -= 86400000;
        }
        if (jjTime < reset) {
            jjTime = msgTime;
            jjTimes = 1;
            return msgTime;
        }
        long targetTime = jjTime + Math.min(getFibonacci(jjTimes + 1), 180) * 60000;
        if (targetTime > msgTime) {
            return Math.min(targetTime, reset + 86400000);
        } else {
            jjTime = msgTime;
            jjTimes++;
            return msgTime;
        }
    }

    public long tzTargetTime() {
        long reset = getZeroHourTimestamp(msgTime) + resetTime * 3600000;
        if (reset > msgTime) {
            reset -= 86400000;
        }
        if (tzTime < reset) {
            tzTime = msgTime;
            tzTimes = 1;
            return msgTime;
        }
        long targetTime = tzTimes == 1 || tzTimes == 2 ?
                tzTime + 3600000 * 3 : tzTime + 3600000 * 24;
        if (targetTime > msgTime) {
            return Math.min(targetTime, reset + 86400000);
        } else {
            tzTime = msgTime;
            tzTimes++;
            return msgTime;
        }
    }

}