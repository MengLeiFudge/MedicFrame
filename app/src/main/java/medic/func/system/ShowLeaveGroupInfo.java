package medic.func.system;

import medic.core.SystemProcess;

import static medic.core.Api.getNick;
import static medic.core.Api.qq;
import static medic.core.Api.send;

/**
 * @author MengLeiFudge
 */
public class ShowLeaveGroupInfo extends SystemProcess {
    public ShowLeaveGroupInfo(long requestId, long sQQ, String sNick, String info, String result) {
        super(requestId, sQQ, sNick, info, result);
    }

    @Override
    public void process() {
        send(getNick(qq) + "(" + qq + ")离开了本群QAQ");
    }
}
