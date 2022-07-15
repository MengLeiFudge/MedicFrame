package medic.func.common.admin;

import medic.core.FuncProcess;
import medic.core.Main;

import static medic.core.Api.getAtNicks;
import static medic.core.Api.getAtNum;
import static medic.core.Api.getAtQQs;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.setAdmin;
import static medic.core.Api.textMsg;
import static medic.core.Utils.AUTHOR_QQ;

/**
 * @author MengLeiFudge
 */
public class SetBotAdmin extends FuncProcess {
    public SetBotAdmin(Main.Func func) {
        super(func);
    }

    @Override
    public void menu() {
    }

    @Override
    public boolean process() {
        if (qq == AUTHOR_QQ && getAtNum() != 0) {
            if (textMsg.matches("(增加|设置|设|加)(管理|管理员)@.+")) {
                addBotAdmin();
                return true;
            } else if (textMsg.matches("(删除|取消|删)(管理|管理员)@.+")) {
                subBotAdmin();
                return true;
            }
        }
        return false;
    }

    private void addBotAdmin() {
        StringBuilder sb = new StringBuilder();
        long[] atQqs = getAtQQs();
        String[] atNicks = getAtNicks();
        for (int i = 0; i < getAtNum(); i++) {
            setAdmin(atQqs[i], true);
            sb.append(i == 0 ? "已将 " : "、").append(atNicks[i])
                    .append("(").append(atQqs[i]).append(")");
        }
        sb.append(" 设为Bot管理！");
        send(sb.toString());
    }

    private void subBotAdmin() {
        StringBuilder sb = new StringBuilder();
        long[] atQqs = getAtQQs();
        String[] atNicks = getAtNicks();
        for (int i = 0; i < getAtNum(); i++) {
            setAdmin(atQqs[i], false);
            sb.append(i == 0 ? "已取消 " : "、").append(atNicks[i])
                    .append("(").append(atQqs[i]).append(")");
        }
        sb.append(" 的Bot管理权限！");
        send(sb.toString());
    }
}
