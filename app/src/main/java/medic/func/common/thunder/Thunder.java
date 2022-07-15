package medic.func.common.thunder;

import medic.core.FuncProcess;
import medic.core.Main;

import java.io.File;
import java.text.DecimalFormat;

import static medic.core.Api.MsgSource;
import static medic.core.Api.changeAllSourceToPrivate;
import static medic.core.Api.group;
import static medic.core.Api.isAdmin;
import static medic.core.Api.msgSource;
import static medic.core.Api.notAllowTalking;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.DEF_STRING;
import static medic.core.Utils.Dir;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getRandomDouble;
import static medic.core.Utils.getRandomInt;
import static medic.core.Utils.getString;
import static medic.core.Utils.set;

/**
 * @author MengLeiFudge
 */
public class Thunder extends FuncProcess {
    public Thunder(Main.Func func) {
        super(func);
        String s = getString(THUNDER_FILE, group);
        if (DEF_STRING.equals(s)) {
            // 默认概率：5%，默认禁言时间5-20s
            chance = 0.05;
            minSecond = 5;
            maxSecond = 20;
            save();
        } else {
            String[] data = s.split("-");
            chance = Double.parseDouble(data[0]);
            minSecond = Integer.parseInt(data[1]);
            maxSecond = Integer.parseInt(data[2]);
        }
        menuList.add("随机禁言");
    }

    @Override
    public void menu() {
        send("[g]群聊 [p]私聊 [a]仅管理员\n" +
                "[a][pg]设置(随机)禁言概率+概率(+%)：设置随机禁言概率为指定值的一百倍\n" +
                "栗子：【设置禁言概率2.5】表示本群随机禁言概率将设为2.5%\n" +
                "注意，概率最低为0.01%，最高为50%\n" +
                "本群当前随机禁言概率：" + chance2Str(chance) + "\n" +
                "[a][pg]设置(随机)禁言时间+时间下限+时间上限：设置随机禁言时间为指定范围，单位为秒\n" +
                "栗子：【设置禁言时间5 20】表示本群随机禁言时间将设为5s-20s\n" +
                "注意，时间最低为1s，最高为30s\n" +
                "本群当前随机禁言时间：" + minSecond + "s - " + maxSecond + "s"
        );
    }

    private static final File THUNDER_FILE = getFile(Dir.SETTINGS, "thunder.txt");
    private static double chance;
    private static int minSecond;
    private static int maxSecond;

    @Override
    public boolean process() {
        if (msgSource != MsgSource.GROUP) {
            return false;
        }
        if (isAdmin) {
            if (textMsg.matches("设置(随机)?禁言概率 *[0-9]+(\\.[0-9]+)?%?")) {
                String[] data = textMsg.split("[^0-9.]+");
                chance = Math.min(50, Math.max(Double.parseDouble(data[1]), 0.01)) / 100;
                save();
                send("已将本群随机禁言概率设为 " + chance2Str(chance) + "！");
                return true;
            } else if (textMsg.matches("设置(随机)?禁言时间 *[0-9]+ +[0-9]+")) {
                String[] data = textMsg.split("\\D+");
                minSecond = Math.min(30, Math.max(Integer.parseInt(data[1]), 1));
                maxSecond = Math.min(30, Math.max(Integer.parseInt(data[2]), 1));
                if (minSecond > maxSecond) {
                    minSecond ^= maxSecond;
                    maxSecond ^= minSecond;
                    minSecond ^= maxSecond;
                }
                save();
                send("已将本群随机禁言时间设为 " + minSecond + "s - " + maxSecond + "s！");
                return true;
            }
        }
        // 必须先判断指令是否为修改复读概率，再判断是否执行复读操作
        // 否则复读概率过高将导致修改指令无法执行
        if (getRandomDouble(0, 1) < chance) {
            int second = getRandomInt(minSecond, maxSecond);
            notAllowTalking(qq, second);
            changeAllSourceToPrivate(qq);
            send("你被棉花糖的闪电击中，禁言" + second + "s！");
            return true;
        }
        return false;
    }

    private String chance2Str(double chance) {
        return new DecimalFormat("#0.000%").format(chance);
    }

    private void save() {
        set(THUNDER_FILE, group, chance + "-" + minSecond + "-" + maxSecond);
    }
}
