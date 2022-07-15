package medic.func.common.kun;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import medic.core.FuncProcess;
import medic.core.Main;

import static medic.core.Api.MsgSource;
import static medic.core.Api.getAtQQ;
import static medic.core.Api.getNick;
import static medic.core.Api.isAdmin;
import static medic.core.Api.msgSource;
import static medic.core.Api.msgTime;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.ERR_LONG;
import static medic.core.Utils.getDateStr;
import static medic.core.Utils.getFullTimeStr;
import static medic.core.Utils.getRandomDistributionDouble;
import static medic.core.Utils.getRandomDistributionInt;
import static medic.core.Utils.getRandomDouble;
import static medic.core.Utils.getRandomInt;
import static medic.core.Utils.getThisMonday;
import static medic.core.Utils.getTimeStr;
import static medic.core.Utils.milliSecondToStr;
import static medic.func.common.kun.KunUtils.getAdd;
import static medic.func.common.kun.KunUtils.getBoss;
import static medic.func.common.kun.KunUtils.getGrade;
import static medic.func.common.kun.KunUtils.getNowSeason;
import static medic.func.common.kun.KunUtils.getUser;
import static medic.func.common.kun.KunUtils.getUserList;
import static medic.func.common.kun.KunUtils.save;
import static medic.func.common.kun.KunUtils.seasonSettlement;
import static medic.func.common.kun.KunUtils.startNewSeason;
import static medic.func.common.kun.Rank.getListSortedByLevel;
import static medic.func.common.kun.Rank.getListSortedByMoney;

/**
 * @author MengLeiFudge
 */
public class Process extends FuncProcess {
    public Process(Main.Func func) {
        super(func);
        menuList.add("养鲲手册");
    }

    @Override
    public void menu() {
        send("[g]群聊 [p]私聊 [a]仅管理员\n" +
                "[p]养鲲：从无尽之海抓一条奇怪的鲲\n" +
                "[p]属性：查看鲲的属性\n" +
                "[g]查看@xxx：查看别人鲲的属性\n" +
                "[p]洗练[攻防血]+次数：洗练指定属性，可以多个属性一起洗\n" +
                "[g]进击@xxx：互相挑战，共同升级\n" +
                "[p]挑战：挑战全服boss，可得丰厚奖励\n" +
                "[p]boss：查看boss属性" +
                "[gp]等级排行：查看鲲界等级前十\n" +
                "[gp]财富/金钱排行：查看鲲界萌泪币前十"
        );
        sleep(300);
        send("[p]背包：查看自己的所有道具\n" +
                "[p]命名xxx：消耗改名卡将鲲界的鲲改名\n" +
                "[g]赠送+数目@xxx：赠送萌泪币，但是有手续费\n" +
                "[p]商城：查看可以买卖的道具列表\n" +
                "[p]购买/出售+物品名(+数目)：买卖道具\n" +
                "[gp]签到：可得萌泪币，连续签到奖励更多\n" +
                "[p]设置重置时间+小时：自定义摸鲲等功能的每日重置时间\n" +
                "[p]开/关新赛季提示：赛季更新时是否允许bot私聊提示\n" +
                "[a]修改/更改+内容+数目：官方外挂.jpg\n" +
                "当前为S" + getNowSeason() + "赛季！"
        );
    }

    @Override
    public boolean process() {
        if (!textMsg.contains("@")) {
            if (textMsg.matches("养鲲|摸鲲|抓鲲|捕鲲")) {
                if (msgSource == MsgSource.GROUP) {
                    send(qq, "养鲲要私聊我哦！");
                    return true;
                }
                mk();
                return true;
            } else if (textMsg.matches("属性")) {
                if (msgSource == MsgSource.GROUP) {
                    return true;
                }
                attribute();
                return true;
            } else if (textMsg.matches("洗练.+[0-9]+")) {
                if (msgSource == MsgSource.GROUP) {
                    return true;
                }
                boolean[] wash = {textMsg.contains("攻"), textMsg.contains("防"), textMsg.contains("血")};
                int num = Integer.parseInt(textMsg.split("\\D+")[1]);
                washout(wash, num);
                return true;
            } else if (textMsg.matches("挑战")) {
                if (msgSource == MsgSource.GROUP) {
                    return true;
                }
                attackBossInit();
                return true;
            } else if (textMsg.matches("(查看|)[Bb]oss(属性|)")) {
                if (msgSource == MsgSource.GROUP) {
                    return true;
                }
                boss();
                return true;
            } else if (textMsg.matches("等级排行(榜|)")) {
                levelRank();
                return true;
            } else if (textMsg.matches("(财富|萌泪币|金钱)排行(榜|)")) {
                moneyRank();
                return true;
            } else if (textMsg.matches("道具|背包")) {
                if (msgSource == MsgSource.GROUP) {
                    return true;
                }
                bag();
                return true;
            } else if (textMsg.matches("命名.+")) {
                if (msgSource == MsgSource.GROUP) {
                    return true;
                }
                name(textMsg.substring(2));
                return true;
            } else if (textMsg.matches("商城")) {
                if (msgSource == MsgSource.GROUP) {
                    return true;
                }
                send("———— 商城 ————\n"
                        + " 道具    购买    出售\n"
                        + "改名卡：18888 / 15110\n"
                        + "洗练卡：     30 /    24\n"
                        + "挑战券：   100 /     -\n"
                        + "查看卡：   100 /    80\n");
                return true;
            } else if (textMsg.matches("(购买|买|出售|卖).+([0-9]+)?")) {
                if (msgSource == MsgSource.GROUP) {
                    return true;
                }
                boolean buy = textMsg.startsWith("购买") || textMsg.startsWith("买");
                int beginLen = textMsg.startsWith("购买") || textMsg.startsWith("出售") ? 2 : 1;
                int endLen;
                int num;
                if (textMsg.matches(".+[0-9]+")) {
                    String[] data = textMsg.split("\\D+");
                    num = Integer.parseInt(data[data.length - 1]);
                    endLen = data[data.length - 1].length();
                } else {
                    num = 1;
                    endLen = 0;
                }
                String itemName = textMsg.substring(beginLen, textMsg.length() - endLen).trim();
                if (buy) {
                    buy(itemName, num);
                } else {
                    sell(itemName, num);
                }
                return true;
            } else if (textMsg.matches("签到")) {
                dailyAttendance();
                return true;
            } else if (textMsg.matches("设置重置时间 *[0-9]+")) {
                if (msgSource == MsgSource.GROUP) {
                    return true;
                }
                int num = Integer.parseInt(textMsg.split("\\D+")[1]);
                setResetTime(num);
                return true;
            } else if (textMsg.matches("[开关]新赛季提示")) {
                if (msgSource == MsgSource.GROUP) {
                    return true;
                }
                boolean openNewSeasonTip = textMsg.startsWith("开");
                setNewSeasonTip(openNewSeasonTip);
                return true;
            } else if (isAdmin) {
                if (textMsg.matches("(更改|修改).+[0-9]+")) {
                    int num = Integer.parseInt(textMsg.split("\\D+")[1]);
                    String item = textMsg.substring(2, (textMsg.length() - (num + "").length())).trim();
                    change(item, num);
                    return true;
                } else if (textMsg.matches("赠送全部 *[0-9]+")) {
                    int money = Integer.parseInt(textMsg.split("\\D+")[1]);
                    giveMoneyToAll(money);
                }
            }
        } else {
            if (getAtQQ() == ERR_LONG) {
                return false;
            }
            if (textMsg.matches("查看@.+")) {
                otherAttribute();
                return true;
            } else if (textMsg.matches("进击@.+")) {
                attackOthers();
                return true;
            } else if (textMsg.matches("赠送[0-9]+@.+")) {
                giveMoney(Integer.parseInt(textMsg.substring(2, textMsg.indexOf('@'))));
                return true;
            } else if (textMsg.matches("@.+赠送[0-9]+")) {
                giveMoney(Integer.parseInt(textMsg.substring(textMsg.indexOf("赠送") + 2)));
                return true;
            }
        }
        return false;
    }

    private void mk() {
        User user = getUser(qq);
        if (user == null) {
            user = new User(qq);
            save(user);
            send(qq, "你孤身一人去无尽之海，发现一只小鲲崽遗弃在海岸边。\n" +
                    "你看它非常可爱，便给它取名为" + user.getName() +
                    "，决定抚养它成为最强的鲲王。\n" +
                    "只是这无尽之海非常广阔，你还需要抓更多的鲲喂给它，使其慢慢成长....");
            return;
        }
        long targetTime = user.mkTargetTime();
        if (!isAdmin && targetTime != msgTime) {
            send(qq, "下次摸鲲时间为" + getTimeStr(targetTime) +
                    "，还需等待" + milliSecondToStr(msgTime, targetTime, true) + "~");
            return;
        }

        int level = user.getLevel();
        String name = user.getName();
        int money = user.getMoney();
        int event = getRandomInt(0, 99);
        if (event < 10) {
            // 无事发生（0-9）
            send(qq, "你去无尽之海，结果什么都没摸到！\n"
                    + "只好带着" + name + "回去了。");
        } else if (event < 20) {
            // 被偷走钱（10-19）
            int subMoney = (int) getRandomDouble(money * 0.001, money * 0.015);
            subMoney = Math.min(subMoney, level / 200);
            user.setMoney(money - subMoney);
            send(qq, "你去无尽之海，结果什么都没摸到！\n"
                    + "一摸背包，发现萌泪币少了" + subMoney + "！\n"
                    + "你很生气，却不知是谁偷了你的钱，只好带着" + name + "回去了。");
        } else if (event < 30) {
            // 等级降低（20-29）
            int subLevel = getRandomDistributionInt(20, 50 + level / 500);
            user.setLevel(level - subLevel);
            send(qq, "你去无尽之海，忽然一只巨鲲向你咬来！\n"
                    + "这时，" + name + "挺身而出，替你挡下了这一击，"
                    + "自己却受伤，等级降低了" + subLevel + "！\n"
                    + "你立刻离开无尽之海，带着" + name + "回去休养了。");
        } else {
            // 正常摸鲲（30-99）
            int addLevel = event < 36 ?
                    getRandomDistributionInt(100, 250 + level / 100)
                    : getRandomDistributionInt(20, 50 + level / 500);
            int addMoney = event > 33 && event < 40 ?
                    getRandomDistributionInt(addLevel * 10, addLevel * 30)
                    : getRandomDistributionInt(addLevel * 2, addLevel * 6);
            user.setLevel(level + addLevel);
            user.setMoney(money + addMoney);
            if (event < 34) {
                // 鲲等级提高（30-33）
                send(qq, "你去无尽之海，抓了很多很多的鲲！\n"
                        + "你将大部分鲲喂给" + name + "，等级提高" + addLevel + "！\n"
                        + "剩余的鲲都被你卖掉，获得" + addMoney + "枚萌泪币！");
            } else if (event < 36) {
                // 鲲等级、萌泪币提高（34-35）
                send(qq, "你去无尽之海，不仅抓了很多鲲，还捞上来一个大宝箱！\n"
                        + "你将鲲喂给" + name + "，等级提高" + addLevel + "！\n"
                        + "你又打开宝箱，发现里面竟然有" + addMoney + "枚萌泪币！");
            } else if (event < 40) {
                // 萌泪币提高（36-39）
                send(qq, "你去无尽之海，抓了一些鲲，还捞上来一个小宝箱！\n"
                        + "你将鲲喂给" + name + "，等级提高" + addLevel + "！\n"
                        + "你又打开宝箱，发现里面竟然有" + addMoney + "枚萌泪币！");
            } else {
                // 正常（40-99）
                send(qq, "你去无尽之海，抓了一些鲲！\n"
                        + "你将大部分鲲喂给" + name + "，等级提高" + addLevel + "！\n"
                        + "剩余的鲲都被你卖掉，获得" + addMoney + "枚萌泪币！");
            }
        }
        if (user.getLevel() > 120000) {
            startNewSeason();
            seasonSettlement(user);
        }
        save(user);
    }

    private void attribute() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }

        int level = user.getLevel();
        int atk = user.getAtk();
        int def = user.getDef();
        int hp = user.getHp();
        String atkGrade = getGrade(atk, level, 1, 1.5);
        String defGrade = getGrade(def, level, 0.6, 0.9);
        String hpGrade = getGrade(hp, level, 4, 6);
        List<User> list = getListSortedByLevel();
        int myIndex = -1;
        int allNum = list.size();
        for (int i = 0; i < allNum; i++) {
            if (list.get(i).getQq() == qq) {
                myIndex = i + 1;
                break;
            }
        }
        String sendStr = user.getName() + "正在到处游弋。\n"
                + "等级：" + level + "\n"
                + "血量：" + hp + "（" + hpGrade + "）\n"
                + "攻击：" + atk + "（" + atkGrade + "）\n"
                + "防御：" + def + "（" + defGrade + "）\n"
                + "当前排名：" + myIndex + " / " + allNum + "\n";
        if (myIndex == 1) {
            sendStr += "超过了所有人！太强了鸭！\n";
        } else if (myIndex == allNum) {
            sendStr += "谁都没有超过！太惨了鸭！\n";
        } else {
            String s = String.format(Locale.CHINA, "%.2f", (allNum - myIndex) * 100.0 / allNum);
            sendStr += "超过了" + s + "%的人！\n";
        }
        sendStr += "新赛季私聊提示：" + (user.isOpenNewSeasonTip() ? "开启" : "关闭");
        send(qq, sendStr);
        save(user);
    }

    private void otherAttribute() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        User atUser = getUser(getAtQQ());
        if (atUser == null) {
            send(qq, "TA好像还没鲲呢！");
            return;
        }

        int ckk = user.getCkk();
        if (ckk <= 0) {
            send(qq, "TA的鲲似乎被一层迷雾笼罩....\n" + "指令提示：【购买查看卡】");
            return;
        }
        user.setCkk(ckk - 1);
        int level = atUser.getLevel();
        int atk = atUser.getAtk();
        int def = atUser.getDef();
        int hp = atUser.getHp();
        String atkGrade = getGrade(atk, level, 1, 1.5);
        String defGrade = getGrade(def, level, 0.6, 0.9);
        String hpGrade = getGrade(hp, level, 4, 6);
        send(qq, "查看到对方信息如下：\n" +
                atUser.getName() + "\n" +
                "等级：" + level + "\n" +
                "血量：" + hp + "（" + hpGrade + "）\n" +
                "攻击：" + atk + "（" + atkGrade + "）\n" +
                "防御：" + def + "（" + defGrade + "）");
        save(user);
    }

    private void washout(boolean[] type, int oneAttrUseNum) {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        boolean atk = type[0];
        boolean def = type[1];
        boolean hp = type[2];
        if (!atk && !def && !hp) {
            send(qq, user.getName() + "一脸懵逼的看着你，不知道你要干什么。\n" +
                    "指令提示：攻防血至少洗练一项");
            save(user);
            return;
        }
        int level = user.getLevel();
        int xlkNum = user.getXlk();
        int useNum = 0;
        useNum = atk ? useNum + oneAttrUseNum : useNum;
        useNum = def ? useNum + oneAttrUseNum : useNum;
        useNum = hp ? useNum + oneAttrUseNum : useNum;
        if (xlkNum < useNum) {
            send(qq, user.getName() + "也想洗练，但是你好像没有足够的洗练卡了QAQ\n" +
                    "指令提示：该指令至少需要" + useNum + "张洗练卡");
            save(user);
            return;
        }
        user.setXlk(xlkNum - useNum);
        String s = user.getName() + "正在洗练....";
        if (atk) {
            int maxNum = 0;
            for (int i = 0; i < oneAttrUseNum; i++) {
                maxNum = Math.max(maxNum, (int) getRandomDistributionDouble(level * 1.0, level * 1.5));
            }
            s = s + "\n洗练攻击" + oneAttrUseNum + "次，最高" + maxNum;
            user.setAtk(maxNum);
        }
        if (def) {
            int maxNum = 0;
            for (int i = 0; i < oneAttrUseNum; i++) {
                maxNum = Math.max(maxNum, (int) getRandomDistributionDouble(level * 0.6, level * 0.9));
            }
            s = s + "\n洗练防御" + oneAttrUseNum + "次，最高" + maxNum;
            user.setDef(maxNum);
        }
        if (hp) {
            int maxNum = 0;
            for (int i = 0; i < oneAttrUseNum; i++) {
                maxNum = Math.max(maxNum, (int) getRandomDistributionDouble(level * 4.0, level * 6.0));
            }
            s = s + "\n洗练血量" + oneAttrUseNum + "次，最高" + maxNum;
            user.setHp(maxNum);
        }
        send(qq, s);
        save(user);
    }

    private void attackOthers() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        if (getAtQQ() == qq) {
            send(qq, user.getName() + "表示不想打自己，并向你丢了一个白眼！");
            return;
        }
        User atUser = getUser(getAtQQ());
        if (atUser == null) {
            send(qq, "TA好像还没鲲呢！");
            return;
        }
        int myLevel = user.getLevel();
        int atLevel = atUser.getLevel();
        if (atLevel * 1.5 < myLevel) {
            send(qq, user.getName() + "实在是太强了，" +
                    atUser.getName() + "一看这气势，早就远远跑开了！");
            return;
        }
        long targetTime = user.jjTargetTime();
        if (!isAdmin && targetTime != msgTime) {
            send(qq, "下次进击时间为" + getTimeStr(targetTime) +
                    "，还需等待" + milliSecondToStr(msgTime, targetTime, true) + "~");
            return;
        }

        int num1 = getRandomInt(100, 110);
        int num2 = getRandomInt(90, 100);
        int myAtk = user.getAtk() * num1 / 100;
        int atAtk = atUser.getAtk() * num2 / 100;
        int myDef = user.getDef() * num1 / 100;
        int atDef = atUser.getDef() * num2 / 100;
        int myHp = user.getHp() * num1 / 100;
        int atHp = atUser.getHp() * num2 / 100;
        int myAtkTimes = 0;
        int atAtkTimes = 0;
        int myAllDam = 0;
        int atAllDam = 0;
        String myName = user.getName();
        String atName = atUser.getName();
        StringBuilder str = new StringBuilder(myName + " VS " + atName + "\n");
        boolean isWin;
        while (true) {
            int myDam = Math.max(myAtk - atDef, 1);
            myAtkTimes++;
            myAllDam += myDam;
            if (atHp <= myAllDam) {
                str.append(myName).append("攻击").append(myAtkTimes).append("次\n")
                        .append("共造成伤害").append(myAllDam).append("点\n");
                if (myAtkTimes > 1) {
                    str.append(atName).append("反击").append(atAtkTimes).append("次\n")
                            .append("共造成伤害").append(atAllDam).append("点\n");
                }
                str.append("获胜啦！\n");
                isWin = true;
                break;
            }
            int atDam = Math.max(atAtk - myDef, 1);
            atAtkTimes++;
            atAllDam += atDam;
            if (myHp <= atAllDam) {
                str.append(myName).append("攻击").append(myAtkTimes).append("次\n")
                        .append("共造成伤害").append(myAllDam).append("点\n")
                        .append(atName).append("反击").append(atAtkTimes).append("次\n")
                        .append("共造成伤害").append(atAllDam).append("点\n")
                        .append("失败了！\n");
                isWin = false;
                break;
            }
        }
        int myAdd = isWin ? getAdd(myLevel) : (int) (getAdd(myLevel) * 0.7);
        int atAdd = (int) (getAdd(atLevel) * 0.25);
        myLevel += myAdd;
        atLevel += atAdd;
        send(qq, str + "经过磨炼，\n" +
                myName + "等级增加" + myAdd + "！\n" +
                atName + "等级增加" + atAdd + "！");
        user.setLevel(myLevel);
        atUser.setLevel(atLevel);
        if (myLevel > 120000 || atLevel > 120000) {
            startNewSeason();
            seasonSettlement(user);
            seasonSettlement(atUser);
        }
        save(user);
        save(atUser);
    }

    private void attackBossInit() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        int ticket = user.getTzq();
        if (ticket <= 0) {
            send(qq, "你还没挑战券呢！");
            return;
        }
        long targetTime = user.tzTargetTime();
        if (!isAdmin && targetTime != msgTime) {
            send(qq, "下次挑战时间为" + getTimeStr(targetTime) +
                    "，还需等待" + milliSecondToStr(msgTime, targetTime, true) + "~");
            return;
        }
        user.setTzq(ticket - 1);
        Boss boss = getBoss();
        int bossHp = boss.getHp();
        if (bossHp <= 0) {
            boss.newBoss();
            bossHp = boss.getHp();
        }
        int bossAtk = boss.getAtk();
        int bossDef = boss.getDef();
        int myAtk = user.getAtk();
        int myDef = user.getDef();
        int myHp = user.getHp();
        int myAtkTimes = 0;
        int bossAtkTimes = 0;
        int myAllDam = 0;
        int bossAllDam = 0;
        String myName = user.getName();
        String bossName = boss.getName();
        double attributeIncrease;
        if (getRandomDouble(4, 14) < 7) {
            attributeIncrease = getRandomDistributionDouble(4, 10);
            if (attributeIncrease > 7) {
                attributeIncrease = 14 - attributeIncrease;
            }
        } else {
            attributeIncrease = getRandomDistributionDouble(0, 14);
            if (attributeIncrease < 7) {
                attributeIncrease = 14 - attributeIncrease;
            }
        }
        myAtk = (int) (myAtk * attributeIncrease);
        myDef = (int) (myDef * attributeIncrease * 0.1);
        myHp = (int) (myHp * attributeIncrease * 0.5);
        StringBuilder str = new StringBuilder(myName + " VS "
                + bossName + "\n随机加成倍数："
                + String.format(Locale.CHINA, "%.2f", attributeIncrease) + "\n");
        boolean isWin;
        while (true) {
            int myDam = Math.max(myAtk - bossDef, 1);
            myAtkTimes++;
            myAllDam += myDam;
            if (bossHp <= myAllDam) {
                str.append(myName).append("攻击").append(myAtkTimes).append("次\n")
                        .append("共造成伤害").append(myAllDam).append("点\n");
                if (myAtkTimes > 1) {
                    str.append(bossName).append("反击").append(bossAtkTimes).append("次\n")
                            .append("共造成伤害").append(bossAllDam).append("点\n");
                }
                str.append("获胜啦！\n");
                isWin = true;
                break;
            }
            int atDam = Math.max(bossAtk - myDef, 1);
            bossAtkTimes++;
            bossAllDam += atDam;
            if (myHp <= bossAllDam) {
                str.append(myName).append("攻击").append(myAtkTimes).append("次\n")
                        .append("共造成伤害").append(myAllDam).append("点\n")
                        .append(bossName).append("反击").append(bossAtkTimes).append("次\n")
                        .append("共造成伤害").append(bossAllDam).append("点\n")
                        .append("失败了！\n");
                isWin = false;
                break;
            }
        }
        boss.setHp(bossHp - myAllDam);
        int bossLevel = boss.getLevel();
        int getMoney = isWin ? (int) ((long) myAllDam * 2 / bossLevel + 10000)
                : (int) ((long) myAllDam * 2 / bossLevel + 1000);
        user.addMoney(getMoney);
        send(qq, str.append("获得了").append(getMoney).append("枚萌泪币！").toString());
        save(user);
        save(boss);
    }

    private void boss() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        int ckk = user.getCkk();
        if (ckk <= 0) {
            send(qq, "你没有查看卡，无权查看对方信息！");
            return;
        }
        user.setCkk(ckk - 1);
        Boss boss = getBoss();
        int hp = boss.getHp();
        if (hp <= 0) {
            boss.newBoss();
            hp = boss.getHp();
        }
        send(qq, "Boss " + boss.getName() + "\n" +
                "攻击：" + boss.getAtk() + "\n" +
                "防御：" + boss.getDef() + "\n" +
                "剩余血量：" + hp);
        save(user);
    }

    private void levelRank() {
        List<User> list = getListSortedByLevel();
        int size = list.size();
        if (size == 0) {
            send(qq, "当前无人上榜！\n（摸鲲、进击均可上榜）");
            return;
        }
        StringBuilder sb = new StringBuilder();
        int j;
        for (int i = 0; i < Math.min(size, 10); i++) {
            User u = list.get(i);
            j = i % 10;
            if (j != 0) {
                sb.append("\n");
            }
            sb.append(i + 1).append(" Lv.").append(u.getLevel())
                    .append(" ").append(u.getName()).append("\n")
                    .append(getNick(u.getQq())).append("(").append(u.getQq()).append(")");
            if (j == 9) {
                send(sb.toString());
                sleep(300);
                sb = new StringBuilder();
            }
        }
        if (!sb.toString().equals("")) {
            send(sb.toString());
        }
    }

    private void moneyRank() {
        List<User> list = getListSortedByMoney();
        int size = list.size();
        if (size == 0) {
            send(qq, "当前无人上榜！\n（摸鲲、进击均可上榜）");
            return;
        }
        StringBuilder sb = new StringBuilder();
        int j;
        for (int i = 0; i < Math.min(size, 10); i++) {
            User u = list.get(i);
            j = i % 10;
            if (j != 0) {
                sb.append("\n");
            }
            sb.append(i + 1).append(" ").append(u.getMoney())
                    .append(" 枚萌泪币\n")
                    .append(getNick(u.getQq())).append("(").append(u.getQq()).append(")");
            if (j == 9) {
                send(sb.toString());
                sleep(300);
                sb = new StringBuilder();
            }
        }
        if (!sb.toString().equals("")) {
            send(sb.toString());
        }
    }

    private void bag() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        send("改名卡：" + user.getGmk() + "张\n" +
                "洗练卡：" + user.getXlk() + "张\n" +
                "挑战券：" + user.getTzq() + "张\n" +
                "查看卡：" + user.getCkk() + "张\n" +
                "萌泪币：" + user.getMoney() + "枚");
    }

    private void name(String newName) {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        String oldName = user.getName();
        newName = newName.replaceAll("\\s*", "");
        if (newName.length() > 8) {
            send(qq, oldName + "表示这个名字实在是太长了，它根本记不住！\n" +
                    "指令提示：请使用8字符以内的名字");
            return;
        }
        if (newName.equals("")) {
            send(qq, "指令提示：命名xx");
            return;
        }
        int gmk = user.getGmk();
        if (gmk <= 0) {
            send(qq, oldName + "摇了摇头，表示不喜欢这个名字！\n" +
                    "指令提示：【购买改名卡】");
            return;
        }

        user.setGmk(gmk - 1);
        user.setName(newName);
        send(qq, oldName + "高兴地绕着你转了两圈，" +
                "看来它很喜欢这个名字！\n" +
                "以后它就叫" + newName + "啦！");
        save(user);
    }

    private void giveMoney(int giveMoney) {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        User atUser = getUser(getAtQQ());
        if (atUser == null) {
            send(qq, "TA好像还没鲲呢！");
            return;
        }
        int myMoney = user.getMoney();
        if (giveMoney <= 0) {
            giveMoney = Math.min(-giveMoney, myMoney);
        }
        if (giveMoney > myMoney) {
            send(qq, "钱不够！\n你在想peach？");
            return;
        }

        user.subMoney(giveMoney);
        int fee = isAdmin ? 0 : (int) (giveMoney * getRandomDouble(0.05, 0.15));
        int add = giveMoney - fee;
        atUser.addMoney(add);
        send(qq, "收取手续费" + fee + "枚萌泪币，已赠送" + add + "枚萌泪币！");
        save(user);
        save(atUser);
    }

    private void giveMoneyToAll(int money) {
        ArrayList<User> users = (ArrayList<User>) getUserList();
        for (User user : users) {
            user.addMoney(money);
            save(user);
        }
        send("已赠送所有人萌泪币" + money + "枚！");
    }

    private void buy(String itemName, int buyNum) {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        if (buyNum <= 0) {
            send(qq, "购买数量有误！");
            return;
        }
        int money = user.getMoney();
        int price;
        int itemNum;
        switch (itemName) {
            case "改名卡":
                itemNum = user.getGmk();
                if (itemNum + buyNum > 1) {
                    send(qq, "改名卡上限为1！");
                    return;
                }
                price = 18888;
                break;
            case "洗练卡":
                itemNum = user.getXlk();
                if (itemNum + buyNum > 9999) {
                    send(qq, "洗练卡上限为9999！");
                    return;
                }
                price = 30;
                break;
            case "挑战券":
                itemNum = user.getTzq();
                if (itemNum + buyNum > 99) {
                    send(qq, "挑战券上限为99！");
                    return;
                }
                price = 100;
                break;
            case "查看卡":
                itemNum = user.getCkk();
                if (itemNum + buyNum > 99) {
                    send(qq, "查看卡上限为99！");
                    return;
                }
                price = 100;
                break;
            default:
                send(qq, "[" + itemName + "]是神马东西？可以次吗？");
                return;
        }
        if (money < price * buyNum) {
            send(qq, "萌泪币：" + money + "\n不足" + price * buyNum + "，无法购买！");
            return;
        }
        switch (itemName) {
            case "改名卡":
                user.setGmk(itemNum + buyNum);
                break;
            case "洗练卡":
                user.setXlk(itemNum + buyNum);
                break;
            case "挑战券":
                user.setTzq(itemNum + buyNum);
                break;
            case "查看卡":
                user.setCkk(itemNum + buyNum);
                break;
            default:
        }
        user.subMoney(price * buyNum);
        send(qq, "成功购买" + itemName + "×" + buyNum + "！\n" +
                "花费" + price * buyNum + "枚萌泪币！\n" +
                "现有萌泪币：" + user.getMoney() + "枚");
        save(user);
    }

    private void sell(String itemName, int soldNum) {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        if (soldNum <= 0) {
            send(qq, "出售数量有误！");
            return;
        }
        int price;
        int itemNum;
        switch (itemName) {
            case "改名卡":
                price = 15110;
                itemNum = user.getGmk();
                break;
            case "洗练卡":
                price = 24;
                itemNum = user.getXlk();
                break;
            case "查看卡":
                price = 80;
                itemNum = user.getCkk();
                break;
            default:
                send(qq, "[" + itemName + "]是神马东西？可以次吗？");
                return;
        }
        if (itemNum < soldNum) {
            soldNum = itemNum;
        }
        switch (itemName) {
            case "改名卡":
                user.setGmk(itemNum - soldNum);
                break;
            case "洗练卡":
                user.setXlk(itemNum - soldNum);
                break;
            case "查看卡":
                user.setCkk(itemNum - soldNum);
                break;
            default:
        }
        user.addMoney(price * soldNum);
        send(qq, "成功出售" + itemName + "×" + soldNum + "！\n" +
                "获得" + price * soldNum + "枚萌泪币！\n" +
                "现有萌泪币：" + user.getMoney() + "枚");
        save(user);
    }

    private void dailyAttendance() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        String today = getDateStr(msgTime);
        String lastSignInDate = user.getLastSignInDate();
        int allTimes = user.getAllSignInTimes();
        int weekTimes = user.getWeekSignInTimes();
        if (lastSignInDate.equals(today)) {
            send(qq, "共签到" + allTimes + "次\n" +
                    "本周已签到" + weekTimes + "天\n" +
                    "明天再来签到吧！");
            return;
        }
        user.setLastSignInDate(today);
        user.setAllSignInTimes(++allTimes);
        String thisMonday = getThisMonday(msgTime);
        if (thisMonday.compareTo(lastSignInDate) > 0) {
            weekTimes = 1;
            user.setWeekSignInTimes(weekTimes);
        } else {
            user.setWeekSignInTimes(++weekTimes);
        }
        int getMoney;
        switch (weekTimes) {
            case 1:
                getMoney = 666;
                break;
            case 2:
                getMoney = 999;
                break;
            case 3:
                getMoney = 1314;
                break;
            case 4:
                getMoney = 1888;
                break;
            case 5:
                getMoney = 2888;
                break;
            case 6:
                getMoney = 3888;
                break;
            case 7:
                getMoney = 6666;
                break;
            default:
                getMoney = 0;
        }
        user.addMoney(getMoney);
        send(qq, "共签到" + allTimes + "次\n" +
                "本周已签到" + weekTimes + "天\n" +
                "获得" + getMoney + "枚萌泪币！");
        save(user);
    }

    private void setResetTime(int resetTime) {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        if (user.getResetTime() == resetTime) {
            send(qq, "当前重置时间是" + resetTime + "时，无需更改！");
            return;
        }
        long lastSetTime = user.getLastSetTime();
        if (msgTime - lastSetTime < 604800000L) {
            send(qq, "距上一次设置重置时间不足一周！\n请于"
                    + getFullTimeStr(lastSetTime + 604800000L) + "后再试！");
            return;
        }
        user.setResetTime(resetTime);
        user.setLastSetTime(msgTime);
        send(qq, "当前重置时间已更改为" + resetTime + "时！");
        save(user);
    }

    private void setNewSeasonTip(boolean open) {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        user.setOpenNewSeasonTip(open);
        send(qq, "已" + (open ? "打开" : "关闭") + "赛季提示！");
        save(user);
    }

    private void change(String key, int newValue) {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "在无尽之海，也许你会有所发现....\n指令提示：私聊【摸鲲】");
            return;
        }
        switch (key) {
            case "萌泪币":
                user.setMoney(newValue);
                break;
            case "等级":
                user.setLevel(newValue);
                break;
            default:
                send(qq, "错误的修改目标 [" + key + "]");
                return;
        }
        send(qq, "已修改" + key + "为" + newValue + "！");
        save(user);
    }
}
