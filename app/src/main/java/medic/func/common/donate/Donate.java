package medic.func.common.donate;

import medic.core.FuncProcess;
import medic.core.Main;
import medic.core.Utils;

import static medic.core.Api.addImg;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.getFile;

/**
 * @author MengLeiFudge
 */
public class Donate extends FuncProcess {
    public Donate(Main.Func func) {
        super(func);
    }

    @Override
    public void menu() {
    }

    @Override
    public boolean process() {
        if (textMsg.matches("(/?(?i)donate)|捐献|支持")) {
            addImg(getFile(Utils.Dir.DATA, "zfb.jpg"));
            send();
            return true;
        }
        return false;
    }
}
