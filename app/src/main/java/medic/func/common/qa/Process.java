package medic.func.common.qa;

import medic.core.FuncProcess;
import medic.core.Main;

import static medic.core.Api.MsgSource;
import static medic.core.Api.MsgType;
import static medic.core.Api.getAtNick;
import static medic.core.Api.getAtQQ;
import static medic.core.Api.getRobotQQ;
import static medic.core.Api.isAdmin;
import static medic.core.Api.msgSource;
import static medic.core.Api.msgType;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.strFormat;
import static medic.core.Utils.strToRegex;

/**
 * @author MengLeiFudge
 */
public class Process extends FuncProcess {
    public Process(Main.Func func) {
        super(func);
        menuList.add("问答");
        menuList.add("智能问答");
    }

    @Override
    public void menu() {
        send("标识 [g]群聊可用 [p]私聊可用\n" +
                "说明 [a]仅Bot管理员可用\n" +
                "[gp]问题列表：查看所有问题及对应序号\n" +
                "[gp]/问+问题+/答+回答：设置问题，栗子【/问你好/答你好啊】\n" +
                "[gp]查/查看+问题序号：查看该问题的所有回答及对应序号\n" +
                "[gp(a)]删+问题序号+回答序号：删除问题，只能删自己设置的；管理员可以删除任意问答\n" +
                "[a][gp]删+问题序号：删除问题以及该问题所有回答"
        );
    }

    @Override
    public boolean process() {
        if (true) {
            return false;
        }

        // 排除纯图片消息
        if (msgType == MsgType.IMG) {
            return false;
        }
        if (textMsg.matches("问题列表")) {
            //getAllQ();
        } else if (textMsg.matches("/问.+/答.+")) {
            int index = textMsg.indexOf("/答");
            String q = strFormat(textMsg.substring(2, index)).trim();
            String a = textMsg.substring(index + 2).trim();
            //setQA(q, a);
        } else if (textMsg.matches("(查|查看) *[1-9][0-9]*")) {
            int qNum = Integer.parseInt(textMsg.split("\\D+")[1]);
            //getA(qNum);
        } else if (textMsg.matches("(删|删除) *[1-9][0-9]* *[1-9][0-9]*")) {
            String[] data = textMsg.split("\\D+");
            int qNum = Integer.parseInt(data[1]);
            int aNum = Integer.parseInt(data[2]);
            //delA(qNum, aNum);
        } else if (textMsg.matches("(删|删除) *[1-9][0-9]*")) {
            if (!isAdmin) {
                send(qq, "你不是我的管理员，没有权限删除整个问题哦！\n" +
                        "指令提示：【删+问题序号+回答序号】");
            }
            int qNum = Integer.parseInt(textMsg.split("\\D+")[1]);
            //delQ(qNum);
        } else if (msgSource == MsgSource.GROUP) {
            if (!textMsg.contains("@")) {
                // 没有艾特则只在本地问答查找
                //localQA(strFormat(textMsg));
            } else if (getAtQQ() == getRobotQQ()) {
                // 艾特棉花糖，则去掉艾特后先本地问答查找，没有时使用api
                String regex = " *@" + strToRegex(getAtNick()) + " *";
                String q = strFormat(textMsg.replaceAll(regex, ""));
                //if (!localQA(q)) {
                //smartQA(q);
                //}
            }
        } else {
            // 先寻找本地问答，没有再找网上问答
            //if ("".equals(textMsg) && !localQA(strFormat(textMsg))) {
            //smartQA(textMsg);
            //}
        }
        return false;
    }


    /**
     * 显示当前所有问题及其序号.
     *//*
    public void getAllQ() {
        QaList dataBase = getQaList();
        List<Process> list = dataBase.getSortedList();
        if (list.isEmpty()) {
            send("当前本地问答数据库为空！");
            return;
        }
        StringBuilder sb = new StringBuilder("问题列表如下：\n");
        for (int i = 0; i < list.size(); i++) {
            Process base = list.get(i);
            if (i % 10 != 0) {
                sb.append("\n");
            }
            sb.append(i + 1).append(".").append(base.getQuestion());
            if (i % 10 == 9) {
                send(sb.toString());
                sb = new StringBuilder();
            }
        }
        String s = sb.toString();
        if (!s.equals("")) {
            send(s);
        }
    }*/

    /**
     * 设置一个问答.
     *
     * @param question
     * @param answer
     *//*
    public void setQA(String question, String answer) {
        QaList dataBase = getQaList();
        dataBase.add(question, answer, qq);
        save(dataBase);
        send(qq, "已添加该问答！");
    }

    public void getA(int qNum) {
        qNum--;
        QaList dataBase = getQaList();
        List<Process> list = dataBase.getSortedList();
        if (list.isEmpty()) {
            send("当前本地问答数据库为空！");
            return;
        }
        if (qNum >= list.size()) {
            send(qq, "没有找到该问题呢QAQ");
            return;
        }
        Process base = list.get(qNum);
        StringBuilder sb = new StringBuilder("问题：" + base.getQuestion() + "\n回答：\n");
        List<String> answers = base.getAnswerList();
        for (int i = 0; i < answers.size(); i++) {
            if (i % 10 != 0) {
                sb.append("\n");
            }
            sb.append(i + 1).append(".").append(answers.get(i));
            if (i % 10 == 9) {
                send(sb.toString());
                sb = new StringBuilder();
            }
        }
        String s = sb.toString();
        if (!s.equals("")) {
            send(s);
        }
    }

    public void delA(int qNum, int aNum) {
        qNum--;
        aNum--;
        QaList dataBase = getQaList();
        List<Process> list = dataBase.getSortedList();
        if (list.isEmpty()) {
            send("当前本地问答数据库为空！");
            return;
        }
        if (qNum >= list.size()) {
            send(qq, "没有找到该问题呢QAQ");
            return;
        }
        Process base = list.get(qNum);
        if (aNum == 1 && base.getAnswerList().size() == 1) {
            dataBase.remove(qNum);
            return;
        }
        if (base.remove(aNum, qq)) {
            dataBase.set(qNum, base);
            send(qq, "已删除该问题的对应回答！");
            save(dataBase);
        } else {
            send(qq, "没有找到该问题的对应答复呢QAQ");
        }
    }

    public void delQ(int qNum) {
        qNum--;
        QaList dataBase = getQaList();
        List<Process> list = dataBase.getSortedList();
        if (list.isEmpty()) {
            send("当前本地问答数据库为空！");
            return;
        }
        if (qNum >= list.size()) {
            send(qq, "没有找到该问题呢QAQ");
            return;
        }
        dataBase.remove(qNum);
        send(qq, "已删除该问题及其所有回答！");
    }*/


    /**
     * 本地读取问答并回复.
     * 如果有该问答，随机输出一个回答，返回 true;
     * 否则返回 false.
     *
     * @param question
     * @return
     *//*
    public boolean localQA(String question) {
        QaList dataBase = getQaList();
        List<Process> list = dataBase.getSortedList();
        for (Process base : list) {
            if (base.getQuestion().matches(question)) {
                send(base.getRandomAnswer());
                return true;
            }
        }
        return false;
    }

    public void smartQA(String question) {
        if (question.equals("")) {
            send(qq, "找我有什么事吗？QwQ");
            return;
        }
    }*/
}
