package medic.func.common.control;

import medic.core.FuncProcess;
import medic.core.Main;

import static medic.core.Api.MsgSource;
import static medic.core.Api.group;
import static medic.core.Api.isAdmin;
import static medic.core.Api.msgSource;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Main.Func;
import static medic.core.Main.LogFunc;
import static medic.core.Utils.AUTHOR_QQ;
import static medic.core.Utils.setFuncState;

/**
 * @author MengLeiFudge
 */
public class FuncControl extends FuncProcess {
    public FuncControl(Main.Func func) {
        super(func);
    }

    @Override
    public void menu() {
    }

    @Override
    public boolean process() {
        if (textMsg.matches("(开启|关闭)(功能)?[0-9]+")) {
            int funcIndex = Integer.parseInt(textMsg.split("\\D+")[1]);
            changeFuncState(funcIndex, textMsg.startsWith("开"));
            return true;
        }
        return false;
    }

    private void changeFuncState(int funcIndex, boolean open) {
        if (msgSource == MsgSource.GROUP) {
            if (!isAdmin) {
                send("只有Bot管理员才能开关功能哦！");
                return;
            }
            for (Func func : Func.values()) {
                if (func.getIndex() == funcIndex) {
                    setFuncState(group, func, open);
                    send((open ? "已开启" : "已关闭") + func.getName() + "！");
                    return;
                }
            }
        } else {
            if (qq != AUTHOR_QQ) {
                send("只能在群内开关功能哦！");
                return;
            }
            for (LogFunc func : LogFunc.values()) {
                if (func.getIndex() == funcIndex) {
                    setFuncState(func, open);
                    send((open ? "已开启" : "已关闭") + func.getName() + "！");
                    return;
                }
            }
        }
        send("功能序号错误，请检查对应序号！");
    }
}
