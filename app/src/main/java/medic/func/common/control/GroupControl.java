package medic.func.common.control;

import medic.core.Api.MsgSource;
import medic.core.FuncProcess;
import medic.core.Main;

import static medic.core.Api.allowGroupTalking;
import static medic.core.Api.allowTalking;
import static medic.core.Api.getAtQQ;
import static medic.core.Api.group;
import static medic.core.Api.isAdmin;
import static medic.core.Api.msgSource;
import static medic.core.Api.notAllowGroupTalking;
import static medic.core.Api.notAllowTalking;
import static medic.core.Api.removeMember;
import static medic.core.Api.textMsg;

/**
 * @author MengLeiFudge
 */
public class GroupControl extends FuncProcess {
    public GroupControl(Main.Func func) {
        super(func);
    }

    @Override
    public void menu() {
    }

    @Override
    public boolean process() {
        if (!isAdmin || msgSource != MsgSource.GROUP) {
            return false;
        }
        if (textMsg.matches("(?i)((禁|禁言)?[1-9][0-9]*[smh]?@.*)")) {
            int time = Integer.parseInt(textMsg.split("\\D+")[1]);
            if (textMsg.contains("m@")) {
                notAllowTalking(getAtQQ(), time * 60);
            } else if (textMsg.contains("h@")) {
                notAllowTalking(getAtQQ(), time * 3600);
            } else {
                notAllowTalking(getAtQQ(), time);
            }
            return true;
        } else if (textMsg.matches("(解|解禁)@.*")) {
            allowTalking(getAtQQ());
            return true;
        } else if (textMsg.matches("群禁|群禁言")) {
            notAllowGroupTalking();
            return true;
        } else if (textMsg.matches("解禁|群解禁")) {
            allowGroupTalking();
            return true;
        } else if (textMsg.matches("(踢|踢出)@+")) {
            removeMember(group, getAtQQ());
        }
        return false;
    }
}
