package medic.func.common.menu;

import medic.core.FuncProcess;
import medic.core.Main.Func;
import medic.core.Main.LogFunc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static medic.core.Api.MsgSource;
import static medic.core.Api.group;
import static medic.core.Api.msgSource;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.AUTHOR_QQ;
import static medic.core.Utils.getFuncStateStr;

/**
 * @author MengLeiFudge
 */
public class Menu extends FuncProcess {
    public Menu(Func func) {
        super(func);
    }

    @Override
    public void menu() {
    }

    @Override
    public boolean process() {
        if (textMsg.matches("菜单")) {
            showMenu();
            return true;
        }
        return false;
    }

    private void showMenu() {
        if (msgSource != MsgSource.GROUP) {
            if (qq == AUTHOR_QQ) {
                sortLogFuncThenSend();
            } else {
                send("请在群中发送【菜单】以了解Bot各功能开启情况！");
            }
        } else {
            sortFuncThenSend();
        }
    }

    private void sortLogFuncThenSend() {
        ArrayList<LogFunc> list = new ArrayList<>(Arrays.asList(LogFunc.values()));
        list.sort(Comparator.comparingInt(LogFunc::getIndex));

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (LogFunc func : list) {
            if (!isFirst) {
                sb.append("\n");
            } else {
                isFirst = false;
            }
            sb.append(func.getIndex()).append(".").append(func.getName())
                    .append("：").append(getFuncStateStr(func));
        }
        send(sb.toString());
    }

    private void sortFuncThenSend() {
        ArrayList<Func> list = new ArrayList<>();
        for (Func func : Func.values()) {
            if (func.getIndex() > 0) {
                list.add(func);
            }
        }
        list.sort(Comparator.comparingInt(Func::getIndex));

        StringBuilder sb = new StringBuilder("本群功能开启情况如下：");
        for (Func func : list) {
            sb.append("\n").append(func.getIndex()).append(".").append(func.getName())
                    .append("：").append(getFuncStateStr(group, func));
        }
        sb.append("\ntips：【菜单+功能序号】获得对应功能菜单，如【菜单21】");
        send(sb.toString());
    }
}
