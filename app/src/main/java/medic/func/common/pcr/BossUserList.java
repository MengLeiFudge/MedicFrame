package medic.func.common.pcr;

import lombok.Data;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static medic.core.Api.msgTime;
import static medic.func.common.pcr.Apply.ApplyType;
import static medic.func.common.pcr.Atk.AtkType;
import static medic.func.common.pcr.Boss.BOSS_NUM;
import static medic.func.common.pcr.Boss.Stage;
import static medic.func.common.pcr.PcrUtils.getLastFiveHour;
import static medic.func.common.pcr.PcrUtils.getUserList;

/**
 * @author MengLeiFudge
 */
@Data
public class BossUserList implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long group;

    /**
     * boss预约列表.
     */
    private final ArrayList<ArrayList<ArrayList<Long>>> appointLists = new ArrayList<>();

    BossUserList(long group) {
        this.group = group;
        for (int i = 0; i < Stage.values().length; i++) {
            ArrayList<ArrayList<Long>> list = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                list.add(new ArrayList<>());
            }
            appointLists.add(list);
        }
    }

    /**
     * 今日排刀列表，每日5点清空.
     * <p>
     * ps1: 未出的补偿刀同样也会清空。
     * <p>
     * ps2: 不考虑5点前进，5点后结算的情况。
     */
    private final ArrayList<Apply> applyList = new ArrayList<>();
    /**
     * 从当期会战开始的所有出刀列表.
     */
    private final ArrayList<Atk> atkList = new ArrayList<>();

    /**
     * 将一个排刀变为该刀对于用户的描述.
     * <p>
     * 普通刀记为1刀，尾刀和补偿刀都记为0.5刀。
     *
     * @param apply 要获取描述的排刀
     * @param user  排了这刀的用户
     * @param boss  boss
     * @return 该刀描述
     */
    public String getApplyDescribe(Apply apply, User user, Boss boss) {
        // 今日排刀可能性：普通(-补偿)-普通(-补偿)-普通(-补偿)
        List<Apply> list = getTodayApplyList(user);
        if (!list.contains(apply) || apply.isFinished()) {
            return "该排刀不是此人今日未出排刀，需检查代码";
        }
        // 第一刀排刀至apply这段区间（包括第一刀排刀和apply）内，所有普通刀的个数
        int beforeCommonApplyNum = 0;
        for (int i = 0; i <= list.indexOf(apply); i++) {
            if (list.get(i).getApplyType() == ApplyType.COMMON) {
                beforeCommonApplyNum++;
            }
        }
        String s = "第" + beforeCommonApplyNum + "刀";
        if (apply.getApplyType() == ApplyType.COMPENSATE) {
            s += "补偿刀";
        }
        if (apply.isOnTree()) {
            s += "（挂树ing）";
        } else if (apply.getApplyType() == ApplyType.COMPENSATE) {
            // 补偿刀只考虑 出刀/挂树 两种状态
            s += "（可出刀）";
        } else {
            // 普通刀考虑 出刀/挂树/等待/倒二/倒一 五种状态
            // upperIndex 表示 击杀boss所需的刀数-1
            // upperIndex 为 0，表示预估伤害大于当前血量，可以击杀boss，即击杀boss还需要一刀
            int upperIndex = 1;
            while (upperIndex * boss.getExpectedDam() < boss.getNowHp()) {
                upperIndex++;
            }
            upperIndex--;
            // index 表示该刀在 所有未出的普通排刀组成的列表 中的位置
            // upperIndex 为 0，表示该刀是列表中第一刀
            list = getUnionTodayNotFinishApplyListSortByType();
            list.removeIf(o -> o.getApplyType() == ApplyType.COMPENSATE);
            int index = list.indexOf(apply);
            if (index <= upperIndex) {
                s += "（预计倒数第" + (upperIndex - index + 1) + "刀）";
            } else {
                s += "（建议等待）";
            }
        }
        return s;
    }

    List<Apply> getTodayApplyList(User user) {
        List<Apply> list = new ArrayList<>();
        for (Apply apply : applyList) {
            if (apply.getQq() == user.getQq()) {
                list.add(apply);
            }
        }
        return list;
    }

    Apply getTodayLastApply(User user) {
        List<Apply> list = getTodayApplyList(user);
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(list.size() - 1);
        }
    }

    Atk getTodayLastAtk(User user) {
        Atk atk = null;
        for (Atk atk0 : atkList) {
            if (atk0.getTime() >= getLastFiveHour(msgTime) && atk0.getQq() == user.getQq()) {
                atk = atk0;
            }
        }
        return atk;
    }


    Apply getTodayFirstNotFinishApply(User user) {
        for (Apply apply : getTodayApplyList(user)) {
            if (!apply.isFinished()) {
                return apply;
            }
        }
        return null;
    }

    int getLastApplyCommonIndexOfThisUser(User user) {
        List<Apply> list = getTodayApplyList(user);
        int i = 0;
        for (Apply apply : list) {
            i += apply.getApplyType().toInt();
        }
        return i;
    }


    boolean isOnTree(User user) {
        Apply apply = getTodayFirstNotFinishApply(user);
        if (apply == null) {
            return false;
        }
        return apply.isOnTree();
    }

    String getTodayAtkNum(User user) {
        double num = 0;
        for (Atk atk : atkList) {
            if (atk.getTime() >= getLastFiveHour(msgTime) && atk.getQq() == user.getQq()) {
                num += atk.getAtkType().toDouble();
            }
        }
        return new DecimalFormat("0.#").format(num);
    }

    String getPeriodTheoreticalMaxAtkNum(User user) {
        double num = 0;
        for (Atk atk : atkList) {
            if (atk.getQq() == user.getQq()) {
                num += atk.getAtkType().toDouble();
            }
        }
        return new DecimalFormat("0.#").format(num);
    }

    int getComAtkNum(User user, Stage stage, int bossIndex) {
        int num = 0;
        for (Atk atk : atkList) {
            if (atk.getQq() == user.getQq() && atk.getAtkType() == AtkType.COMMON
                    && atk.getBossStage() == stage && atk.getBossIndex() == bossIndex) {
                num++;
            }
        }
        return num;
    }

    private int getComAtkSumDam0(User user, Stage stage, int bossIndex) {
        int sumDamage = 0;
        for (Atk atk : atkList) {
            if (atk.getQq() == user.getQq() && atk.getAtkType() == AtkType.COMMON
                    && atk.getBossStage() == stage && atk.getBossIndex() == bossIndex) {
                sumDamage += atk.getDamage();
            }
        }
        return sumDamage;
    }

    private static DecimalFormat df = new DecimalFormat("0.00%");

    private String getOverallAtkLevelDescribe(double d) {
        if (d < -0.5) {
            return "暂无";
        } else {
            // 78.91%（-21.09%）
            // 123.45%（+23.45%）
            return df.format(d) +
                    "（" +
                    (d >= 1.0 ? "+" : "-") +
                    df.format(Math.abs(d - 1.0)) +
                    "）";
        }
    }

    public String getPeriodAtkDescribe(User user, Stage stage, int bossIndex) {
        int userComAtkNum = getComAtkNum(user, stage, bossIndex);
        int userComAtkSumDam = getComAtkSumDam0(user, stage, bossIndex);
        double userComAtkAvgDam = userComAtkNum == 0 ? 0.0 : (double) userComAtkSumDam / userComAtkNum;
        int unionComAtkAvgDam = getUnionComAtkAvgDam0(stage, bossIndex);
        double rate = unionComAtkAvgDam == 0 ? 1.0 : userComAtkAvgDam / unionComAtkAvgDam;
        /*
        return userComAtkNum + "刀，均伤" +
                new DecimalFormat("0,000,000").format(userComAtkAvgDam) +
                "（" + new DecimalFormat("0.00%").format(Math.abs(rate)) + "）";
         */
        return stage + BOSS_NUM[bossIndex - 1] + "王" + userComAtkNum + "刀，均伤" +
                new DecimalFormat("0,000,000").format(userComAtkAvgDam) +
                "，出刀质量" + getOverallAtkLevelDescribe(rate);
    }

    public String getOverallAtkLevelDescribe(User user) {
        return getOverallAtkLevelDescribe(getOverallAtkLevel(user));
    }

    public double getOverallAtkLevel(User user) {
        int allAtkNum = 0;
        double allRate = 0.0;
        for (Stage stage : Stage.toSortedList()) {
            for (int bossIndex = 1; bossIndex <= 5; bossIndex++) {
                int userComAtkNum = getComAtkNum(user, stage, bossIndex);
                int userComAtkSumDam = getComAtkSumDam0(user, stage, bossIndex);
                double userComAtkAvgDam = userComAtkNum == 0 ? 0.0 : (double) userComAtkSumDam / userComAtkNum;
                int unionComAtkAvgDam = getUnionComAtkAvgDam0(stage, bossIndex);
                double rate = unionComAtkAvgDam == 0 ? 1.0 : userComAtkAvgDam / unionComAtkAvgDam;
                allAtkNum += userComAtkNum;
                allRate += rate * userComAtkNum;
            }
        }
        return allAtkNum == 0 ? -1.0 : allRate / allAtkNum;
    }


    private int getUnionComAtkAvgDam0(Stage stage, int bossIndex) {
        int num = 0;
        int sumDamage = 0;
        for (Atk atk : atkList) {
            if (atk.getAtkType() == AtkType.COMMON
                    && atk.getBossStage() == stage && atk.getBossIndex() == bossIndex) {
                num++;
                sumDamage += atk.getDamage();
            }
        }
        return num == 0 ? 0 : sumDamage / num;
    }

    List<Apply> getUnionTodayNotFinishApplyListSortByType() {
        List<Apply> list = new ArrayList<>();
        for (Apply apply : applyList) {
            if (!apply.isFinished()) {
                list.add(apply);
            }
        }
        list.sort(Comparator.comparingInt(o -> o.getApplyType().toInt()));
        return list;
    }

    boolean isUnionTodayAtkListEmpty() {
        for (Atk atk : atkList) {
            if (atk.getTime() >= getLastFiveHour(msgTime)) {
                return false;
            }
        }
        return true;
    }

    public List<User> getAllUserSortedByTodayAtkNum() {
        List<User> list = getUserList(group);
        list.sort(Comparator.comparingDouble(o -> Double.parseDouble(getTodayAtkNum(o))));
        return list;
    }

    String getUnionTodayAtkNum() {
        double num = 0;
        for (Atk atk : atkList) {
            if (atk.getTime() >= getLastFiveHour(msgTime)) {
                num += atk.getAtkType().toDouble();
            }
        }
        return new DecimalFormat("0.#").format(num);
    }

    public List<User> getAllUserSortedByAllAtkNum() {
        List<User> list = getUserList(group);
        list.sort(Comparator.comparingDouble(o -> Double.parseDouble(getPeriodTheoreticalMaxAtkNum(o))));
        return list;
    }

    String getUnionPeriodAtkNum() {
        double num = 0;
        for (Atk atk : atkList) {
            num += atk.getAtkType().toDouble();
        }
        return new DecimalFormat("0.#").format(num);
    }

    /**
     * 个人本期理论最大出刀数.
     */
    int getPeriodTheoreticalMaxAtkNum() {
        // 天数 * 3
        Set<Long> timeSet = new HashSet<>();
        for (Atk atk : atkList) {
            timeSet.add(getLastFiveHour(atk.getTime()));
        }
        return timeSet.size() * 3;
    }

    /**
     * 行会本期理论最大出刀数.
     */
    int getUnionPeriodTheoreticalMaxAtkNum() {
        // 用户数 * (天数 * 3)
        return getUserList(group).size() * getPeriodTheoreticalMaxAtkNum();
    }

    void addAppoint(long qq, Stage targetStage, int bossIndex) {
        List<Long> list = appointLists.get(targetStage.getIndex()).get(bossIndex - 1);
        if (!list.contains(qq)) {
            list.add(qq);
        }
    }

    List<Long> getAppointUserList(Boss boss) {
        return appointLists.get(boss.getStage().getIndex()).get(boss.getIndex() - 1);
    }

    void cancelAllAppoint(long qq) {
        for (int i = 0; i < Stage.values().length; i++) {
            for (int j = 0; j < 5; j++) {
                appointLists.get(i).get(j).removeIf(o -> o == qq);
            }
        }
    }

    String getNextAppoint(long qq, Boss boss) {
        Boss boss1 = new Boss(group);
        boss1.setLoop(boss.getLoop());
        boss1.setIndex(boss.getIndex());
        boss1.subHp(Integer.MAX_VALUE);
        while (boss1.getLoop() < 100) {
            List<Long> list = getAppointUserList(boss1);
            for (long qq0 : list) {
                if (qq == qq0) {
                    return boss1.getStage() + "（" + boss1.getLoop() + "周目），" + boss1.getName();
                }
            }
            boss1.subHp(Integer.MAX_VALUE);
        }
        return "无";
    }
}
