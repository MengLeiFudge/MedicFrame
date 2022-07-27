package medic.func.common.reread;

import medic.core.FuncProcess;
import medic.core.Main;

import java.io.File;
import java.text.DecimalFormat;

import static medic.core.Api.addFirstImg;
import static medic.core.Api.addText;
import static medic.core.Api.getAtNum;
import static medic.core.Api.group;
import static medic.core.Api.isAdmin;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.DEF_DOUBLE;
import static medic.core.Utils.Dir;
import static medic.core.Utils.getDouble;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getRandomDouble;
import static medic.core.Utils.set;

/**
 * @author MengLeiFudge
 */
public class Reread extends FuncProcess {
    public Reread(Main.Func func) {
        super(func);
        double d = getDouble(REREAD_FILE, group);
        if (d == DEF_DOUBLE) {
            save();
        } else {
            chance = d;
        }
        menuList.add("复读");
    }

    @Override
    public void menu() {
        send("[g]群聊 [p]私聊 [a]仅管理员\n" +
                "[a][pg]设置复读(概率)+概率(+%)：设置复读概率为指定值的一百倍\n" +
                "栗子：【设置复读2.5】表示本群复读概率将设为2.5%\n" +
                "注意，概率最低为0.01%，最高为50%\n" +
                "复读时，有一半概率倒序复读\n" +
                "本群当前复读概率：" + chance2Str(chance)
        );
    }

    private static final File REREAD_FILE = getFile(Dir.SETTINGS, "reread.txt");

    /**
     * 复读概率，默认值 5%.
     */
    private double chance = 0.05;

    @Override
    public boolean process() {
        // 必须先判断指令是否为修改复读概率，再判断是否执行复读操作
        // 否则复读概率过高将导致修改指令无法执行
        if (isAdmin) {
            if (textMsg.matches("设置复读(概率)? *[0-9]+(\\.[0-9]+)?%?")) {
                String[] data = textMsg.split("[^0-9.]+");
                chance = Math.min(50, Math.max(Double.parseDouble(data[1]), 0.01)) / 100;
                save();
                send("已将本群复读概率设为 " + chance2Str(chance) + "！");
                return true;
            }
        }
        // 超出指定复读概率，则不复读
        if (getRandomDouble(0, 1) > chance) {
            return false;
        }
        // 含有艾特，则不复读
        if (getAtNum() > 0) {
            return false;
        }
        // 含有斜杠或反斜杠，可能是表情或网址，不复读
        if (textMsg.contains("/") || textMsg.contains("\\")) {
            return false;
        }
        // 获取文字复读
        String text;
        if (getRandomDouble(0, 1) < 0.5) {
            // 文字正序输出
            text = textMsg;
        } else {
            // 文字倒序输出
            char[] strChar = textMsg.toCharArray();
            for (int i = 0; i < strChar.length / 2; i++) {
                char c = strChar[i];
                strChar[i] = strChar[strChar.length - 1 - i];
                strChar[strChar.length - 1 - i] = c;
            }
            text = new String(strChar);
        }
        if ("".equals(text)) {
            if (!addFirstImg()) {
                return false;
            }
        } else {
            addFirstImg();
        }
        addText(text);
        send();
        return true;
    }

    private String chance2Str(double chance) {
        return new DecimalFormat("0.000%").format(chance);
    }

    private void save() {
        set(REREAD_FILE, group, chance);
    }
}
