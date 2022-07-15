package medic.func.system;

import medic.core.SystemProcess;

import static medic.core.Api.qq;
import static medic.core.Api.send;

/**
 * @author MengLeiFudge
 */
public class ShowChangeAdminInfo extends SystemProcess {
    public ShowChangeAdminInfo(long requestId, long sQQ, String sNick, String info, String result) {
        super(requestId, sQQ, sNick, info, result);
    }

    @Override
    public void process() {
        send(qq + " 到底是成为了管理员还是被取消了管理员呢？\n" +
                "人家也不知道鸭QAQ");
    }
}
