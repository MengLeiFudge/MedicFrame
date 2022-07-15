package medic.func.common.arc.werewolf;

import medic.core.FuncProcess;
import medic.core.Main;

import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.func.common.arc.werewolf.WerewolfUtils.save;

/**
 * @author MengLeiFudge
 */
public class Process extends FuncProcess {
    public Process(Main.Func func) {
        super(func);
        menuList.add("狼人杀");
        menuList.add("arc狼人杀");
        menuList.add("arcaea狼人杀");
    }

    @Override
    public void menu() {
        send("标识 [g]群聊可用 [p]私聊可用\n" +
                "说明 [a]仅Bot管理员可用\n" +
                "[g]加入狼人杀：加入arc狼人杀对局\n" +
                "[g]其他指令待定。"
        );
    }

    @Override
    public boolean process() {
        if (textMsg.matches("加入狼人杀")) {
            send(qq, "加入成功");
            return true;
        } else if (textMsg.matches("test")) {
            Player player = WerewolfUtils.getUser(qq);
            if (player == null) {
                player = new Player(qq);
                save(player);
            }
            send(qq, "get player ok, begin sleep");
            sleep(3000);
            save(player);
            send(qq, "save player ok, end sleep");
            return true;
        }
        return false;
    }
}
