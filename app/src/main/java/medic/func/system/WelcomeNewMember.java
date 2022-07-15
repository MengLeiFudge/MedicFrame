package medic.func.system;

import medic.core.SystemProcess;

import static medic.core.Api.group;
import static medic.core.Api.qq;
import static medic.core.Api.send;

/**
 * @author MengLeiFudge
 */
public class WelcomeNewMember extends SystemProcess {
    public WelcomeNewMember(long requestId, long sQQ, String sNick, String info, String result) {
        super(requestId, sQQ, sNick, info, result);
    }

    @Override
    public void process() {
        alreadyJoined();
    }

    private void alreadyJoined() {
        switch (group + "") {
            case "319567534":
                send(qq, "欢迎新人！\n养鲲请私聊！");
                break;
            case "1121508667":
                send(qq, "欢迎来到萌泪的Arcaea爬梯群！\n" +
                        "请先仔细阅读公告哦~\n看不懂在群里问就好啦QwQ");
                break;
            case "516286670":
                send(qq, "欢迎来到萌泪的测试群！\n" +
                        "此群为萌泪开发机器人的新功能测试群~\n希望各位能积极参与测试哦QwQ");
                break;
            case "1163635014":
                send(qq, "欢迎来到异形工厂交流群！\n" +
                        "有问题请先查看群文件的“萌新必看”文件夹哦~");
                break;
            default:
                send(qq, "欢迎大佬！\ngroupPosition--;");
        }
    }
}
