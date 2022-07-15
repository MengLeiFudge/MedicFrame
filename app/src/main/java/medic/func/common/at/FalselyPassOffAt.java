package medic.func.common.at;

import medic.core.FuncProcess;
import medic.core.Main;

import static medic.core.Api.MsgSource;
import static medic.core.Api.getAtQQ;
import static medic.core.Api.msgSource;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.ERR_LONG;

/**
 * @author MengLeiFudge
 */
public class FalselyPassOffAt extends FuncProcess {
    public FalselyPassOffAt(Main.Func func) {
        super(func);
    }

    @Override
    public void menu() {
    }

    @Override
    public boolean process() {
        if (msgSource == MsgSource.GROUP && textMsg.contains("@")
                && !textMsg.matches(".+@.+\\..+") && getAtQQ() == ERR_LONG) {
            send(qq, "整天就会复制、粘贴？\n假艾特给爷爬！");
            return true;
        }
        return false;
    }
}
