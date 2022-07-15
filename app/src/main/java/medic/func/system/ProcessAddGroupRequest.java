package medic.func.system;

import medic.core.SystemProcess;

import static medic.core.Api.addAt;
import static medic.core.Api.addText;
import static medic.core.Api.getNick;
import static medic.core.Api.group;
import static medic.core.Api.joinRequest;
import static medic.core.Api.qq;
import static medic.core.Api.send;

/**
 * @author MengLeiFudge
 */
public class ProcessAddGroupRequest extends SystemProcess {
    public ProcessAddGroupRequest(long requestId, long sQQ, String sNick, String info, String result) {
        super(requestId, sQQ, sNick, info, result);
    }

    @Override
    public void process() {
        processJoinRequest();
    }

    private void processJoinRequest() {
        if (info.matches("问题：.*\\\\n答案：.*")) {
            // 需要回答问题并由管理员审核
            String question = info.substring(3, info.indexOf("\\n"));
            String answer = info.substring(info.indexOf("\\n") + 5);
            // 这里写判断条件
            if (group == 1163635014L && "游戏的中文名是？".equals(question)) {
                if ("异形工厂".equals(answer)) {
                    send(getNick(qq) + "(" + qq + ")想加群哎！\n问题：" + question
                            + "\n回答：" + answer + "\n回答正确，那就让你进群吧！");
                    joinRequest(group, qq, requestId, true);
                } else {
                    addText(getNick(qq) + "(" + qq + ")想加群哎！\n问题：" + question
                            + "\n答案：" + answer + "\n回答错误，召唤");
                    addAt(605738729L);
                    send();
                }
            } else {
                send(getNick(qq) + "(" + qq + ")想加群哎！\n问题：" + question
                        + "\n答案：" + answer + "\n我也不知道对不对，就先不处理啦！");
            }
        } else {
            // 需要验证消息
            send(getNick(qq) + "(" + qq + ")想加群哎！\n验证消息："
                    + ("".equals(info) ? "无" : info) + "\n既然你想来，那就让你进来咯！");
            joinRequest(group, qq, requestId, true);
        }
    }
}
