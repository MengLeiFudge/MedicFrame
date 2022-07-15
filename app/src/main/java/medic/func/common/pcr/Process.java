package medic.func.common.pcr;

import medic.core.FuncProcess;
import medic.core.Main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static medic.core.Api.MsgSource;
import static medic.core.Api.addImg;
import static medic.core.Api.changeAllSourceToPrivate;
import static medic.core.Api.getAtQQ;
import static medic.core.Api.getNick;
import static medic.core.Api.getRobotQQ;
import static medic.core.Api.group;
import static medic.core.Api.isAdmin;
import static medic.core.Api.msgSource;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.AUTHOR_NAME;
import static medic.core.Utils.AUTHOR_QQ;
import static medic.core.Utils.ERR_LONG;
import static medic.core.Utils.deleteIfExists;
import static medic.core.Utils.getFile;
import static medic.func.common.pcr.Apply.ApplyType;
import static medic.func.common.pcr.Atk.AtkType;
import static medic.func.common.pcr.Boss.BOSS_NUM;
import static medic.func.common.pcr.Boss.Stage;
import static medic.func.common.pcr.PcrUtils.getBoss;
import static medic.func.common.pcr.PcrUtils.getBossUserList;
import static medic.func.common.pcr.PcrUtils.getRankImg;
import static medic.func.common.pcr.PcrUtils.getRootDir;
import static medic.func.common.pcr.PcrUtils.getUser;
import static medic.func.common.pcr.PcrUtils.getUserList;
import static medic.func.common.pcr.PcrUtils.save;
import static medic.func.common.pcr.PcrUtils.updateIfOverFiveHour;

/**
 * @author MengLeiFudge
 */
public class Process extends FuncProcess {
    public Process(Main.Func func) {
        super(func);
        updateIfOverFiveHour();
        menuList.add("pcr");
    }

    @Override
    public void menu() {
        send("标识 [g]群聊可用 [p]私聊可用\n" +
                "说明 [a]仅Bot管理员可用\n" +
                "[gp]boss：查看boss信息\n" +
                "[g]预约：预约指定boss，如【预约3】【预约a3】\n" +
                "[g]取消预约：取消所有预约\n" +
                "[g]排刀(@xxx)：申请出刀，可申请多次直至上限\n" +
                "[g]取消(@xxx)：取消所有未出的排刀\n" +
                "[gp]顺序/列表：查看当前排刀顺序\n" +
                "[g]sl(@xxx)：每日首次使用会修改sl状态，非首次提示已使用sl\n" +
                "[g]字符+伤害/击杀(@xxx)：记录出刀的伤害\n" +
                "[g]撤销(@xxx)：撤销上一个出刀\n" +
                "[g]上树(@xxx)：会长也救不了你.jpg"
        );
        sleep(300);
        send("[gp]查询/查看(@xxx)：查看出刀情况、sl状态等\n" +
                "[gp]出刀/质量：查看综合出刀质量排行\n" +
                "[gp]漏刀：查看今日漏刀情况\n" +
                "[gp]总刀：查看历史漏刀情况\n" +
                "[gp]伤害预估/预估伤害：查看管理员设置的常规伤害上限\n" +
                "[a][g]设置指定boss+单次伤害预估：用于调整排刀的描述，" +
                "【设置a3 200w】表示a面3王伤害预估改为200w，" +
                "【设置c5 1500000】同理\n" +
                "[a][g]重置pcr@Bot：重置会战情况，仅保留伤害预估设定值\n" +
                "[gp]rank1/2/3：【rank1】前卫rank表，2中3后"
        );
    }

    @Override
    public boolean process() {
        if (!textMsg.contains("@")) {
            if (textMsg.matches("(?i)boss")) {
                showBossInfo();
                return true;
            } else if (textMsg.matches("预约[1-5]")) {
                if (msgSource != MsgSource.GROUP) {
                    send("请在群中发送该指令！");
                    return true;
                }
                appoint(Integer.parseInt(textMsg.substring(2, 3)));
                return true;
            } else if (textMsg.matches("预约[ABCabc][1-5]")) {
                if (msgSource != MsgSource.GROUP) {
                    send("请在群中发送该指令！");
                    return true;
                }
                Stage stage;
                char c = textMsg.charAt(2);
                if (c == 'A' || c == 'a') {
                    stage = Stage.FIRST;
                } else if (c == 'B' || c == 'b') {
                    stage = Stage.SECOND;
                } else {
                    stage = Stage.THIRD;
                }
                appoint(stage, Integer.parseInt(textMsg.substring(3, 4)));
                return true;
            } else if (textMsg.matches("取消预约")) {
                if (msgSource != MsgSource.GROUP) {
                    send("请在群中发送该指令！");
                    return true;
                }
                cancelAllAppoint();
                return true;
            } else if (textMsg.matches("排刀")) {
                if (msgSource != MsgSource.GROUP) {
                    send("请在群中发送该指令！");
                    return true;
                }
                apply(qq);
                return true;
            } else if (textMsg.matches("取消")) {
                if (msgSource != MsgSource.GROUP) {
                    send("请在群中发送该指令！");
                    return true;
                }
                cancelAllApply(qq);
                return true;
            } else if (textMsg.matches("顺序|列表")) {
                showApplyInfo();
                return true;
            } else if (textMsg.matches("(?i)sl")) {
                if (msgSource != MsgSource.GROUP) {
                    send("请在群中发送该指令！");
                    return true;
                }
                useSl(qq);
                return true;
            } else if (textMsg.matches("[^0-9+我Rr][0-9]+")) {
                if (msgSource != MsgSource.GROUP) {
                    send("请在群中发送该指令！");
                    return true;
                }
                atk(qq, Integer.parseInt(textMsg.substring(1)));
                return true;
            } else if (textMsg.matches("击杀")) {
                if (msgSource != MsgSource.GROUP) {
                    send("请在群中发送该指令！");
                    return true;
                }
                atk(qq, Integer.MAX_VALUE);
                return true;
            } else if (textMsg.matches("撤销")) {
                if (msgSource != MsgSource.GROUP) {
                    send("请在群中发送该指令！");
                    return true;
                }
                cancelLastAtk(qq);
                return true;
            } else if (textMsg.matches("上树")) {
                if (msgSource != MsgSource.GROUP) {
                    send("请在群中发送该指令！");
                    return true;
                }
                goOnTree(qq);
                return true;
            } else if (textMsg.matches("查询|查看")) {
                showMemberInfo(qq);
                return true;
            } else if (textMsg.matches("出刀|质量")) {
                overallAtkLevel();
                return true;
            } else if (textMsg.matches("漏刀")) {
                showTodayLackAtkInfo();
                return true;
            } else if (textMsg.matches("总刀")) {
                showAllLackAtkInfo();
                return true;
            } else if (textMsg.matches("预估伤害|伤害预估")) {
                showExpectedDam();
                return true;
            } else if (textMsg.matches("((?i)rank)[1-3]")) {
                rankImg(textMsg.substring(4));
                return true;
            } else if (isAdmin) {
                if (textMsg.matches("设置[ABCabc][1-5] +[0-9]+[Ww]?")) {
                    if (msgSource != MsgSource.GROUP) {
                        send("请在群中发送该指令！");
                        return true;
                    }
                    Stage stage;
                    char c = textMsg.charAt(2);
                    if (c == 'A' || c == 'a') {
                        stage = Stage.FIRST;
                    } else if (c == 'B' || c == 'b') {
                        stage = Stage.SECOND;
                    } else {
                        stage = Stage.THIRD;
                    }
                    String[] s = textMsg.split("\\D+");
                    int bossNum = Integer.parseInt(s[1]);
                    int expectedDam = Integer.parseInt(s[2]);
                    if (textMsg.endsWith("W") || textMsg.endsWith("w")) {
                        expectedDam *= 10000;
                    }
                    setExpectedDam(stage, bossNum, expectedDam);
                    return true;
                }
            }
        } else {
            if (getAtQQ() == ERR_LONG || msgSource != MsgSource.GROUP) {
                return false;
            }
            if (textMsg.matches("排刀@.+")) {
                apply(getAtQQ());
                return true;
            } else if (textMsg.matches("取消@.+")) {
                cancelAllApply(getAtQQ());
                return true;
            } else if (textMsg.matches("(?i)sl@.+")) {
                useSl(getAtQQ());
                return true;
            } else if (textMsg.matches("[^0-9+我Rr][0-9]+@.+")) {
                atk(getAtQQ(), Integer.parseInt(textMsg.split("\\D+")[1]));
                return true;
            } else if (textMsg.matches("击杀@.+")) {
                atk(getAtQQ(), Integer.MAX_VALUE);
                return true;
            } else if (textMsg.matches("撤销@.+")) {
                cancelLastAtk(getAtQQ());
                return true;
            } else if (textMsg.matches("上树@.+")) {
                goOnTree(getAtQQ());
                return true;
            } else if (textMsg.matches("(查询|查看)@.+")) {
                showMemberInfo(getAtQQ());
                return true;
            } else if (isAdmin) {
                if (textMsg.matches("(?i)重置pcr@.+")) {
                    if (getAtQQ() == getRobotQQ()) {
                        clear();
                    } else {
                        send(qq, "删除本群pcr信息需艾特Bot！");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * boss信息.
     */
    private void showBossInfo() {
        Boss boss = getBoss(group);
        send(boss.getStage() + boss.getName() + "\n"
                + "Hp：" + boss.getNowHp() + " / " + boss.getMaxHp() + "\n"
                + "当前" + boss.getLoop() + "周目，" + "下一周目为" + boss.getNextStage());
    }

    /**
     * 预约.
     */
    private void appoint(int bossIndex) {
        BossUserList list = getBossUserList(group);
        for (Stage stage : Stage.toSortedList()) {
            list.addAppoint(qq, stage, bossIndex);
        }
        send(qq, "已预约" + BOSS_NUM[bossIndex - 1] + "王！\n" +
                "到该boss会有私聊消息提醒，如果收不到请加我为好友~");
        save(list);
    }

    private void appoint(Stage stage, int bossIndex) {
        BossUserList list = getBossUserList(group);
        list.addAppoint(qq, stage, bossIndex);
        send(qq, "已预约" + stage + BOSS_NUM[bossIndex - 1] + "王！\n" +
                "到该boss会有私聊消息提醒，如果收不到请加我为好友~");
        save(list);
    }

    /**
     * 取消所有预约.
     */
    private void cancelAllAppoint() {
        BossUserList list = getBossUserList(group);
        list.cancelAllAppoint(qq);
        send(qq, "已取消你的所有预约！");
        save(list);
    }

    /**
     * 排刀.
     */
    private void apply(long userQq) {
        boolean b = userQq == qq;
        User user = getUser(group, userQq);
        BossUserList list = getBossUserList(group);
        Boss boss = getBoss(group);
        // 排刀结果描述有四种：挂树、出完三刀、排满、未排满
        if (list.isOnTree(user)) {
            send(qq, (b ? "你" : "TA") + "正在树上，无法排刀！");
            return;
        }
        if (list.getTodayAtkNum(user).equals("3")) {
            send(qq, (b ? "你" : "TA") + "已经出过三刀啦！\n明天再来排吧√");
            return;
        }
        Apply newApply = null;
        String s;
        Apply lastApply = list.getTodayLastApply(user);
        int lastApplyCommonIndex = list.getLastApplyCommonIndexOfThisUser(user);
        if (lastApplyCommonIndex == 3
                && (lastApply.getApplyType() == ApplyType.COMPENSATE || !lastApply.isFinished())) {
            // 排满要求：最后排刀是3补偿 / 最后排刀是3普通且未出
            s = (b ? "你" : "TA") + "的所有排刀均已在排刀列表中！\n\n";
        } else if (lastApply == null || !lastApply.isFinished()) {
            newApply = new Apply(userQq, ApplyType.COMMON);
            s = "已将" + (b ? "你" : "TA") + "的第" + (lastApplyCommonIndex + 1) + "刀加入排刀列表。\n\n";
        } else {
            // 根据最后的出刀情况判断增加普通刀还是补偿刀
            // 因为此处 lastApply.isFinished() == true，故 lastAtk 一定不为 null
            Atk lastAtk = list.getTodayLastAtk(user);
            if (lastAtk.getAtkType() == AtkType.KILL) {
                newApply = new Apply(userQq, ApplyType.COMPENSATE);
                s = "已将" + (b ? "你" : "TA") + "的第" + lastApplyCommonIndex + "刀补偿刀加入排刀列表。\n\n";
            } else {
                newApply = new Apply(userQq, ApplyType.COMMON);
                s = "已将" + (b ? "你" : "TA") + "的第" + (lastApplyCommonIndex + 1) + "刀加入排刀列表。\n\n";
            }
        }
        if (newApply != null) {
            list.getApplyList().add(newApply);
        }
        // 取消所有boss的预约
        list.cancelAllAppoint(userQq);
        save(list);
        s += "下一刀情况：\n" + list.getApplyDescribe(list.getTodayFirstNotFinishApply(user), user, boss);
        send(qq, s);
    }

    /**
     * 取消今日所有未出的排刀.
     */
    private void cancelAllApply(long userQq) {
        boolean b = userQq == qq;
        User user = getUser(group, userQq);
        BossUserList list = getBossUserList(group);
        list.getApplyList().removeIf(o -> o.getQq() == user.getQq() && !o.isFinished() && !o.isOnTree());
        send(qq, "已取消" + (b ? "你" : "TA") + "的所有排刀！");
        save(list);
    }

    /**
     * 当前出刀顺序.
     */
    private void showApplyInfo() {
        BossUserList list = getBossUserList(group);
        List<Apply> applyList = getBossUserList(group).getUnionTodayNotFinishApplyListSortByType();
        if (applyList.isEmpty()) {
            send("当前无人排刀！");
            return;
        }
        Boss boss = getBoss(group);
        StringBuilder sb = new StringBuilder("当前排刀：\n");
        for (int i = 0; i < applyList.size(); i++) {
            Apply apply = applyList.get(i);
            User user = getUser(group, apply.getQq());
            if (i % 10 != 0) {
                sb.append("\n");
            }
            sb.append(i + 1).append(".").append(getNick(apply.getQq())).append("\n")
                    .append(list.getApplyDescribe(apply, user, boss));
            if (i % 10 == 9) {
                send(sb.toString());
                sleep(300);
                sb = new StringBuilder();
            }
        }
        String s = sb.toString();
        if (!"".equals(s)) {
            send(s);
        }
    }

    /**
     * 使用sl.
     */
    private void useSl(long userQq) {
        boolean b = userQq == qq;
        User user = getUser(group, userQq);
        if (!user.isHaveSl()) {
            send(qq, (b ? "你" : "TA") + "今日已经用过SL了！");
            return;
        }
        user.setHaveSl(false);
        send(qq, (b ? "你" : "TA") + "今日第一次使用SL！\n已将今日SL状态改为已使用。\n祝好运！");
        save(user);
    }

    /**
     * 出刀.
     */
    private void atk(long userQq, int damage) {
        boolean b = userQq == qq;
        User user = getUser(group, userQq);
        Boss boss = getBoss(group);
        BossUserList list = getBossUserList(group);
        Apply apply = list.getTodayFirstNotFinishApply(user);
        // 是否有排刀
        if (apply == null) {
            send(qq, (b ? "你" : "TA") + "还没排刀呢！");
            return;
        }
        apply.setFinished(true);
        // 修改boss情况，并添加出刀信息
        Atk atk;
        int nowHp = boss.getNowHp();
        StringBuilder sb = new StringBuilder();
        Boss oldBoss = new Boss(group);
        oldBoss.setLoop(boss.getLoop());
        oldBoss.setIndex(boss.getIndex());
        if (damage >= nowHp) {
            damage = nowHp;
            atk = new Atk(apply, nowHp, boss.getLoop(),
                    boss.getIndex(), boss.getStage(), true);
            // 把树上除自己以外的所有人清掉
            list.getApplyList().removeIf(o -> o.isOnTree() && o.getQq() != user.getQq());
            if (apply.isOnTree()) {
                apply.setOnTree(false);
                sb.append(b ? "你" : "代刀完毕，TA").append("从树上掉下，并对").append(boss.getName())
                        .append("造成了").append(damage).append("点伤害，击杀了boss！");
            } else {
                sb.append(b ? "你" : "代刀完毕，TA").append("对").append(boss.getName())
                        .append("造成了").append(damage).append("点伤害，击杀了boss！");
            }
            if (apply.getApplyType() == ApplyType.COMMON) {
                // 在该排刀后面紧跟的位置插入补偿刀
                Apply compensateApply = new Apply(user.getQq(), ApplyType.COMPENSATE);
                list.getApplyList().add(list.getApplyList().indexOf(apply) + 1, compensateApply);
                sb.append("\n产生了一个尾刀！");
            } else {
                sb.append("\n你是憨批吗？补偿刀击杀boss不会产生新的补偿刀！");
            }
        } else {
            atk = new Atk(apply, damage, boss.getLoop(),
                    boss.getIndex(), boss.getStage(), false);
            if (apply.isOnTree()) {
                apply.setOnTree(false);
                sb.append(b ? "你" : "代刀完毕，TA").append("从树上掉下，并对").append(boss.getName())
                        .append("造成了").append(damage).append("点伤害！");
            } else {
                sb.append(b ? "你" : "代刀完毕，TA").append("对").append(boss.getName())
                        .append("造成了").append(damage).append("点伤害！");
            }
        }
        boss.subHp(damage);
        list.getAtkList().add(atk);
        apply = list.getTodayFirstNotFinishApply(user);
        if (apply == null) {
            sb.append("\n\n").append(b ? "你" : "TA").append("当前boss：")
                    .append(oldBoss.getStage()).append("（").append(oldBoss.getLoop())
                    .append("周目），").append(oldBoss.getName()).append("\n")
                    .append(b ? "你" : "TA").append("预约的boss：")
                    .append(list.getNextAppoint(userQq, boss));
        } else {
            sb.append("\n\n排刀列表中").append(b ? "你" : "TA").append("的下一刀：\n")
                    .append(list.getApplyDescribe(apply, user, boss));
        }
        send(qq, sb.toString());
        if (damage >= nowHp) {
            // 提示该王已被击杀
            List<Long> appointList = list.getAppointUserList(oldBoss);
            for (long qq : appointList) {
                changeAllSourceToPrivate(qq);
                send(oldBoss.getStage() + oldBoss.getName() + "已被击杀！");
            }
            // 提示下王已预约
            appointList = list.getAppointUserList(boss);
            for (long qq : appointList) {
                changeAllSourceToPrivate(qq);
                send("你预约的" + boss.getStage() + boss.getName() + "到了！");
            }
        }
        save(boss);
        save(list);
    }

    /**
     * 撤销上一刀.
     */
    private void cancelLastAtk(long userQq) {
        boolean b = userQq == qq;
        User user = getUser(group, userQq);
        Boss boss = getBoss(group);
        BossUserList list = getBossUserList(group);
        List<Atk> atkList = list.getAtkList();
        if (atkList.isEmpty()) {
            send(qq, "还没有人出过刀，无法撤销！");
            return;
        }
        Atk lastAtk = atkList.get(atkList.size() - 1);
        if (lastAtk.getQq() != user.getQq()) {
            send(qq, "最后一刀不是" + (b ? "你" : "TA") + "的刀，无法撤销！");
            return;
        }
        // 回复boss状态，移除该出刀
        boss.addHp(lastAtk.getDamage());
        atkList.remove(lastAtk);
        // 移除所有未出排刀
        list.getApplyList().removeIf(o -> o.getQq() == user.getQq() && !o.isFinished());
        // 移除该出刀对应的排刀
        List<Apply> userApplyList = list.getTodayApplyList(user);
        list.getApplyList().remove(userApplyList.get(userApplyList.size() - 1));
        send(qq, "已撤销" + (b ? "你" : "TA") + "的最后一刀！\n" +
                "已取消" + (b ? "你" : "TA") + "的所有排刀！");
        save(boss);
        save(list);
    }

    /**
     * 上树.
     */
    private void goOnTree(long userQq) {
        boolean b = userQq == qq;
        User user = getUser(group, userQq);
        BossUserList list = getBossUserList(group);
        Apply apply = list.getTodayFirstNotFinishApply(user);
        // 是否有排刀
        if (apply == null) {
            send(qq, (b ? "你" : "TA") + "还未排刀，怎么就上树了鸭？？？");
            return;
        }
        apply.setOnTree(true);
        list.getApplyList().removeIf(o -> o.getQq() == user.getQq() && !o.isOnTree());
        save(list);
        send(qq, (b ? "你" : "TA") + "已经上树，后续排刀已取消。\n请耐心等待大佬救场！");
    }

    /**
     * 查询某个人的信息.
     *
     * @param userQq 要查的人的qq
     */
    private void showMemberInfo(long userQq) {
        User user = getUser(group, userQq);
        BossUserList list = getBossUserList(group);
        String s = getNick(user.getQq()) + "\n" +
                "今日 " + list.getTodayAtkNum(user) + " / 3，" +
                "共计 " + list.getPeriodTheoreticalMaxAtkNum(user) + " / " + list.getPeriodTheoreticalMaxAtkNum() + "\n" +
                (user.isHaveSl() ? "未" : "已") + "使用SL\n" +
                "预约的boss：" + list.getNextAppoint(userQq, getBoss(group));
        StringBuilder sb = new StringBuilder(s);
        for (Stage stage : Stage.toSortedList()) {
            for (int bossIndex = 1; bossIndex <= 5; bossIndex++) {
                if (list.getComAtkNum(user, stage, bossIndex) == 0) {
                    continue;
                }
                sb.append("\n").append(list.getPeriodAtkDescribe(user, stage, bossIndex));
            }
        }
        sb.append("\n综合出刀质量：").append(list.getOverallAtkLevelDescribe(user));
        send(sb.toString());
    }

    /**
     * 综合出刀质量排行.
     */
    private void overallAtkLevel() {
        BossUserList list0 = getBossUserList(group);
        if (list0.getAtkList().isEmpty()) {
            send("还没有人出过刀呢！");
            return;
        }
        List<User> userList = getUserList(group);
        Map<User, Double> map = new HashMap<>(30);
        for (User user : userList) {
            map.put(user, list0.getOverallAtkLevel(user));
        }
        List<Map.Entry<User, Double>> list = new ArrayList<>(map.entrySet());
        list.sort((o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));
        StringBuilder sb = new StringBuilder("综合出刀质量排行\n");
        for (int i = 0; i < list.size(); i++) {
            if (i % 10 != 0) {
                sb.append("\n");
            }
            sb.append(i + 1).append(".").append(getNick(list.get(i).getKey().getQq())).append("\n")
                    .append(list0.getOverallAtkLevelDescribe(list.get(i).getKey()));
            if (i % 10 == 9) {
                send(sb.toString());
                sleep(300);
                sb = new StringBuilder();
            }
        }
        String s = sb.toString();
        if (!"".equals(s)) {
            send(s);
        }
    }

    /**
     * 今日漏刀.
     */
    private void showTodayLackAtkInfo() {
        BossUserList list = getBossUserList(group);
        if (list.isUnionTodayAtkListEmpty()) {
            send("今日还没有人出过刀呢！");
            return;
        }
        List<User> userList = list.getAllUserSortedByTodayAtkNum();
        String unionTodayAtkNum = list.getUnionTodayAtkNum();
        int unionTodayMaxAtkNum = userList.size() * 3;
        if (unionTodayAtkNum.equals(unionTodayMaxAtkNum + "")) {
            send("今日所有人已出完3刀！\n" +
                    "今日行会出刀：" + unionTodayAtkNum + " / " + unionTodayMaxAtkNum);
            return;
        }
        StringBuilder sb = new StringBuilder("今日漏刀排行\n");
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            if (list.getTodayAtkNum(user).equals("3")) {
                break;
            }
            if (i % 10 != 0) {
                sb.append("\n");
            } else if (i != 0) {
                send(sb.toString());
                sleep(300);
                sb = new StringBuilder();
            }
            sb.append(i + 1).append(".").append(getNick(user.getQq())).append("(").append(user.getQq()).append(")\n")
                    .append("今日出刀 ").append(list.getTodayAtkNum(user)).append(" / 3");
        }
        sb.append("\n今日行会出刀：").append(unionTodayAtkNum).append(" / ").append(unionTodayMaxAtkNum);
        send(sb.toString());
    }

    /**
     * 总漏刀.
     */
    private void showAllLackAtkInfo() {
        BossUserList list = getBossUserList(group);
        if (list.getAtkList().isEmpty()) {
            send("还没有人出过刀呢！");
            return;
        }
        List<User> userList = list.getAllUserSortedByAllAtkNum();
        String unionPeriodAtkNum = list.getUnionPeriodAtkNum();
        int unionPeriodTheoreticalMaxAtkNum = list.getUnionPeriodTheoreticalMaxAtkNum();
        int periodTheoreticalMaxAtkNum = list.getPeriodTheoreticalMaxAtkNum();
        if (unionPeriodAtkNum.equals(unionPeriodTheoreticalMaxAtkNum + "")) {
            send("所有人已出完" + periodTheoreticalMaxAtkNum + "刀！\n" +
                    "共计行会出刀：" + unionPeriodAtkNum + " / " + unionPeriodTheoreticalMaxAtkNum);
            return;
        }
        StringBuilder sb = new StringBuilder("总漏刀排行\n");
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            if (list.getPeriodTheoreticalMaxAtkNum(user).equals(periodTheoreticalMaxAtkNum + "")) {
                break;
            }
            if (i % 10 != 0) {
                sb.append("\n");
            } else if (i != 0) {
                send(sb.toString());
                sleep(300);
                sb = new StringBuilder();
            }
            sb.append(i + 1).append(".").append(getNick(user.getQq())).append("(").append(user.getQq()).append(")\n")
                    .append("共计出刀 ").append(list.getPeriodTheoreticalMaxAtkNum(user)).append(" / ")
                    .append(periodTheoreticalMaxAtkNum);
        }
        sb.append("\n共计行会出刀：").append(unionPeriodAtkNum).append(" / ").append(unionPeriodTheoreticalMaxAtkNum);
        send(sb.toString());
    }

    /**
     * 所有王的预估伤害.
     */
    private void showExpectedDam() {
        Boss boss = getBoss(group);
        StringBuilder sb = new StringBuilder("伤害预估值如下：");
        List<Stage> stages = Stage.toSortedList();
        for (Stage stage : stages) {
            sb.append("\n").append(stage).append("：");
            for (int i = 1; i <= 5; i++) {
                sb.append(boss.getExpectedDam(stage, i));
                if (i != 5) {
                    sb.append("，");
                }
            }
        }
        send(qq, sb.toString());
    }

    /**
     * 设置某个王的预估伤害.
     */
    private void setExpectedDam(Stage stage, int bossNum, int expectedDam) {
        Boss boss = getBoss(group);
        boss.setExpectedDam(stage, bossNum, expectedDam);
        save(boss);
        send(qq, "已将" + stage + BOSS_NUM[bossNum - 1] + "王的伤害预估值改为" + expectedDam + "！");
    }

    /**
     * 显示当前rank表.
     */
    private void rankImg(String s) {
        addImg(getRankImg(s));
        send();
    }

    /**
     * 重置本群会战情况，仅保留预估伤害设置.
     */
    private void clear() {
        Boss boss = getBoss(group);
        if (deleteIfExists(getFile(getRootDir(), group + ""))) {
            boss.setLoop(1);
            boss.setIndex(1);
            boss.setNowHp(boss.getMaxHp());
            save(boss);
            send(qq, "已删除本群pcr信息！");
        } else {
            send(qq, "未能删除本群pcr信息，请联系" + AUTHOR_NAME + "(" + AUTHOR_QQ + ")！");
        }
    }
}
