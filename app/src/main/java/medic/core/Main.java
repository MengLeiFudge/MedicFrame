package medic.core;

import medic.func.common.admin.SetBotAdmin;
import medic.func.common.alarm.OnTimeAlarm;
import medic.func.common.at.FalselyPassOffAt;
import medic.func.common.control.FuncControl;
import medic.func.common.control.GroupControl;
import medic.func.common.donate.Donate;
import medic.func.common.hpic.HPicture;
import medic.func.common.menu.Menu;
import medic.func.common.reread.Reread;
import medic.func.common.thunder.Thunder;
import medic.func.system.ProcessAddGroupRequest;
import medic.func.system.ShowChangeAdminInfo;
import medic.func.system.ShowLeaveGroupInfo;
import medic.func.system.WelcomeNewMember;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static medic.core.Api.MsgSource;
import static medic.core.Api.MsgType;
import static medic.core.Api.group;
import static medic.core.Api.groupNick;
import static medic.core.Api.imgMsg;
import static medic.core.Api.initApi;
import static medic.core.Api.isApiAvailable;
import static medic.core.Api.msgSource;
import static medic.core.Api.msgType;
import static medic.core.Api.qq;
import static medic.core.Api.reload;
import static medic.core.Api.saveCodeAndNick;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.FuncProcess.getProcessed;
import static medic.core.Utils.AUTHOR_NAME;
import static medic.core.Utils.AUTHOR_QQ;
import static medic.core.Utils.DEBUG;
import static medic.core.Utils.DEF_LONG;
import static medic.core.Utils.Dir;
import static medic.core.Utils.createFileIfNotExists;
import static medic.core.Utils.deleteIfExists;
import static medic.core.Utils.fileLockMap;
import static medic.core.Utils.forceCreateNewFile;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getFuncState;
import static medic.core.Utils.getLong;
import static medic.core.Utils.initDirs;
import static medic.core.Utils.isDirsAvailable;
import static medic.core.Utils.lock;
import static medic.core.Utils.logError;
import static medic.core.Utils.logInfo;
import static medic.core.Utils.sendAllErrorToAuthor;
import static medic.core.Utils.set;
import static medic.core.Utils.timestampToStr;
import static medic.core.Utils.unlock;
import static medic.core.Utils.unlockFiles;

/**
 * 该类是处理消息的主入口.
 * <p>
 * 在 hdic.txt 中，可以使用如下方式调用 dex：
 * <p>
 * Lib->dex名称|完整类名|调用方法名(参数1\,参数2\,...).
 * <p>
 * 例如，Lib->classes.dex|medic.core.Main|main(groupMsg) 表示依次反射调用如下方法：
 * <ul>
 * <li>{@link #apiSet(Object)}，参数由 Medic 提供</li>
 * <li>{@link #saveInstance(Object)}，参数由 Medic 提供</li>
 * <li>{@link #main(String...)}，参数为 "groupMsg"</li>
 * </ul>
 * 请务必注意，{@code static}、{@code synchronized} 作用域均为<b>本条消息</b>，
 * 在不同消息之间，它们不会生效。所以本程序将会大量使用 {@code static} 变量、方法。
 * <p>
 * 如果想构建任何真正意义上的 {@code static} 对象，请使用 {@link #saveInstance(Object)}.
 *
 * @author MengLeiFudge
 */
public final class Main {
    private Main() {
    }

    // region 词库版本

    /**
     * 格式为 "yyyy-MMdd-HHmm" 的时间字符串，用于生成版本号.
     *
     * @see #getVersion()
     */
    static final String LAST_MODIFY_DATE = "2022-0727-0900";

    /**
     * 获取以 {@link #LAST_MODIFY_DATE} 计算得到的版本字符串.
     *
     * @return 2020年1月1日0时0分返回0101.26296800
     */
    private static String getVersion() {
        Date date;
        try {
            date = new SimpleDateFormat("yyyy-MMdd-HHmm", Locale.CHINA).parse(LAST_MODIFY_DATE);
        } catch (ParseException e) {
            throw new UnexpectedStateException("DATE(" + LAST_MODIFY_DATE + ")格式错误，应为 yyyy-MMdd-HHmm");
        }
        // 分钟时间戳
        assert date != null;
        int minuteTimespan = (int) (date.getTime() / 60000);
        return new SimpleDateFormat("yyMMdd", Locale.CHINA).format(date) + "." + minuteTimespan;
    }

    // endregion

    // region 功能枚举

    /**
     * log 相关功能枚举（群之间功能状态互通）.
     * <p>
     * 【如非必要，请勿改动】
     */
    public enum LogFunc {
        // 无论哪种 log，都会在 Medic 的日志界面显示
        // 此处表示 log 是否应存储到本地文件
        WRITE_ERROR_LOG(1, "保存Error日志"),
        WRITE_WARN_LOG(2, "保存Warn日志"),
        WRITE_INFO_LOG(3, "保存Info日志");

        private final int index;
        private final String name;

        /**
         * 构造一个日志存储功能.
         *
         * @param index 功能序号
         * @param name  功能名
         */
        LogFunc(int index, String name) {
            this.index = index;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 功能枚举（群之间功能状态不互通）.
     */
    public enum Func {
        // 系统消息对应的功能，priority 为 -1
        PROCESS_ADD_GROUP_REQUEST(-1, 1, "加群处理", ProcessAddGroupRequest.class),
        WELCOME_NEW_MEMBER(-1, 2, "入群欢迎", WelcomeNewMember.class),
        SHOW_LEAVE_GROUP_INFO(-1, 3, "离群提示", ShowLeaveGroupInfo.class),
        SHOW_CHANGE_ADMIN_INFO(-1, 4, "管理变动提示", ShowChangeAdminInfo.class),

        // 非系统消息对应的功能，priority >= 0；index 为 -1 表示不在菜单中显示
        FALSELY_PASS_OFF_AT(0, 11, "假艾特给爷爬", FalselyPassOffAt.class),
        ON_TIME_ALARM(0, 15, "整点报时", OnTimeAlarm.class),
        MENU(1, -1, "菜单", Menu.class),
        FUNC_CONTROL(1, -1, "功能开关", FuncControl.class),
        SET_BOT_ADMIN(1, -1, "设置Bot管理员", SetBotAdmin.class),
        GROUP_CONTROL(1, -1, "群管", GroupControl.class),
        DONATE(1, -1, "捐献", Donate.class),
        REREAD(3, 12, "复读", Reread.class),
        THUNDER(3, 13, "随机禁言", Thunder.class),
        H_PICTURE(5, 16, "色图", HPicture.class),
        KEEP_KUN(5, 21, "养鲲", medic.func.common.kun.Process.class),
        SAKURA_CITY(5, 22, "落樱之都", medic.func.common.sakura.Process.class),
        PCR(5, 23, "PCR会战排刀", medic.func.common.pcr.Process.class),
        ARC_QUERY(5, 24, "Arc查询", medic.func.common.arc.query.Process.class),
        ARC_PUBG(5, 25, "Arc狼人杀", medic.func.common.arc.werewolf.Process.class),
        ARC_WEREWOLF(5, 26, "Arc吃鸡", medic.func.common.arc.pubg.Process.class),
        SEKAI(5, 27, "世界计划", medic.func.common.sekai.Process.class),
        QA(10, 14, "智能问答", medic.func.common.qa.Process.class);

        private final int priority;
        private final int index;
        private final String name;
        private final Class<? extends IProcess> clazz;

        /**
         * 构造一个系统功能 / 非系统功能.
         *
         * @param priority 优先度，越小则优先度越高，同优先度一起执行；
         *                 【系统功能优先度必须为-1】
         * @param index    功能序号，可以随意修改，但【不同功能的序号必须不同】；
         *                 小于等于0表示不会在菜单中显示
         * @param name     功能名，定下后不可修改，否则可能导致功能开关状态错误
         * @param clazz    方法对应的主类，系统功能继承于 {@link SystemProcess}，
         *                 非系统功能继承于 {@link FuncProcess}
         */
        Func(int priority, int index, String name, Class<? extends IProcess> clazz) {
            this.priority = priority;
            this.index = index;
            this.name = name;
            this.clazz = clazz;
        }

        public int getPriority() {
            return priority;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public Class<? extends IProcess> getClazz() {
            return clazz;
        }

        public static Func getFuncByIndex(int index) {
            for (Func f : Func.values()) {
                if (f.index == index) {
                    return f;
                }
            }
            return null;
        }
    }

    // endregion

    // region Medic反射初始化

    /**
     * 初始化收发消息的实例对象，由 Medic 反射调用.
     *
     * @param apiObj 收发消息的实例对象
     */
    public static void apiSet(Object apiObj) {
        initDirs();
        if (isDirsAvailable) {
            RESET_INSTANCE_FILE = getFile(Dir.DATA, "resetInstance.txt");
            HDIC_FILE = getFile(Dir.DIC, "hdic.txt");
            GROUP_OF_LAST_MSG = getFile(Dir.SETTINGS, "groupOfLastMsg.txt");
            SYSTEM_PROCESS_STATE = getFile(Dir.SETTINGS, "systemProcessState.txt");
        }
        initApi(apiObj);
    }

    /**
     * 该文件存在时，应初始化自定义列表.
     *
     * @see #saveInstance(Object)
     */
    public static File RESET_INSTANCE_FILE;

    /**
     * 初始化自定义内容，由 Medic 反射调用.
     * <ul>
     * <li>该方法可构建作用域为所有消息的对象。
     * <li>该方法可用于构建锁，以确保文件读写在高并发下正常运转。
     * </ul>
     * Medic 使用 HashMap 存储 obj，键是类名。
     * <p>
     * 例如，Lib->classes.dex|medic.core.Main|main(groupMsg) 的键就是 "medic.core.Main"。
     * <p>
     * 所以，不同的类名可以获取不同的 obj，它们不会互相干扰。
     *
     * @param obj 之前保存的实例对象
     * @return 想要存储的实例对象
     */
    @SuppressWarnings("unchecked")
    public static Object saveInstance(Object obj) {
        if (!isApiAvailable || !isDirsAvailable) {
            return null;
        }
        // 重复一次以避免任何异常
        for (int i = 0; i < 2; i++) {
            try {
                if (RESET_INSTANCE_FILE != null && RESET_INSTANCE_FILE.isFile()) {
                    deleteIfExists(RESET_INSTANCE_FILE);
                    obj = null;
                    logInfo("检测到初始化文件，重新初始化fileLockMap");
                }
                if (obj instanceof Map<?, ?>) {
                    // 直接强转就行，不需要花里胡哨的方法
                    fileLockMap = (ConcurrentHashMap<File, ReentrantReadWriteLock>) obj;
                    if (DEBUG) {
                        StringBuilder s = new StringBuilder("fileLockMap初始内容如下：");
                        int index = 1;
                        for (Map.Entry<File, ReentrantReadWriteLock> entry : fileLockMap.entrySet()) {
                            s.append("\n").append(index++).append(entry.getKey()).append(", ").append(entry.getValue());
                        }
                        logInfo(s.toString());
                    }
                } else {
                    fileLockMap = new ConcurrentHashMap<>(16);
                    obj = fileLockMap;
                    logInfo("obj 非继承于 Map<?, ?>，初始化fileLockMap");
                }
                return obj;
            } catch (RuntimeException e) {
                obj = null;
                logError("初始化异常，使用空对象重新尝试", e);
            }
        }
        return null;
    }

    // endregion

    // region Medic反射处理消息、词库更新

    /**
     * 程序主入口，由 Medic 反射调用.
     * <p>
     * 消息分为以下三种：
     * <ul>
     * <li>群消息：群收到的消息，对应 hdic.txt 中[词条]的内容
     * <li>私聊消息：未加机器人好友的人，通过群给机器人发的消息为临时消息；
     * 已加机器人好友的人，给机器人发的消息为好友消息（群号为-1）；
     * 对应 hdic.txt 中[词条]有“[临时]”前缀的内容
     * <li>系统消息：入群申请等，对应 hdic.txt 中[模块]的 System
     * </ul>
     * <p>
     * 注意，如果由该线程抛出未捕获的异常，Medic 可以在日志界面显示；
     * 如果是新开线程抛出未捕获的异常，则会造成 Medic 闪退。
     *
     * @param args 由 hdic.txt 中调用 dex 所传入的参数
     */
    public static void main(String... args) {
        if (!isApiAvailable || !isDirsAvailable) {
            return;
        }
        try {
            // 手动创建重置文件
            if (qq == AUTHOR_QQ && "重置实例".equals(textMsg)) {
                send(createFileIfNotExists(RESET_INSTANCE_FILE)
                        ? "重置文件创建成功"
                        : "重置文件创建失败");
                return;
            }
            updateHdic();
            if ("systemMsg".equals(args[0])) {
                msgSource = MsgSource.GROUP;
                processSystemMsg(args);
                return;
            }
            String time = timestampToStr(new Date(), "yyyy/MM/dd HH:mm:ss.SSS") + "\n";
            String msgInfo = "";
            if (!"".equals(textMsg)) {
                msgInfo += "\n" + textMsg;
            }
            if (!"".equals(imgMsg)) {
                msgInfo += "\n" + "[图片 " + imgMsg + "]";
            }
            if ("groupMsg".equals(args[0])) {
                msgSource = MsgSource.GROUP;
                String receiveMsg = "收" + msgSource + " / " + msgType + "\n" +
                        "群 " + group + "，" + groupNick + "(" + qq + ")";
                logInfo(time + receiveMsg + msgInfo);
            } else if ("privateMsg".equals(args[0])) {
                if (group == -1) {
                    msgSource = MsgSource.FRIEND;
                    long lastGroup = getLong(GROUP_OF_LAST_MSG, qq);
                    if (lastGroup == DEF_LONG) {
                        String receiveMsg = "收" + msgSource + " / " + msgType + "\n" +
                                "群 -1(-)，QQ " + qq;
                        logInfo(time + receiveMsg + msgInfo);
                        send("请在有我的群中发一次言！");
                        return;
                    } else {
                        group = lastGroup;
                        String receiveMsg = "收" + msgSource + " / " + msgType + "\n" +
                                "群 -1(" + group + ")，QQ " + qq;
                        logInfo(time + receiveMsg + msgInfo);
                    }
                } else {
                    msgSource = MsgSource.TEMPORARY;
                    String receiveMsg = "收" + msgSource + " / " + msgType + "\n" +
                            "群 " + group + "，QQ " + qq;
                    logInfo(time + receiveMsg + msgInfo);
                }
            } else {
                throw new UnexpectedStateException("第一个参数 [" + args[0] + "] " +
                        "只能是 systemMsg/privateMsg/groupMsg 中的一个");
            }
            set(GROUP_OF_LAST_MSG, qq, group);
            saveCodeAndNick();
            if (msgType == MsgType.TEXT || msgType == MsgType.IMG
                    || msgType == MsgType.TEXT_IMG) {
                processCommonMsg();
            }
        } catch (RuntimeException e) {
            logError(e);
        } finally {
            if (DEBUG) {
                StringBuilder s = new StringBuilder("fileLockMap最终内容如下：");
                int index = 1;
                for (Map.Entry<File, ReentrantReadWriteLock> entry : fileLockMap.entrySet()) {
                    s.append("\n").append(index++).append(entry.getKey()).append(", ").append(entry.getValue());
                }
                logInfo(s.toString());
            }
            unlockFiles();
            sendAllErrorToAuthor();
        }
    }

    private static File HDIC_FILE;

    private static void updateHdic() {
        boolean searchedUpdateBotName = false;
        boolean searchedUpdateAuthor = false;
        boolean searchedUpdateVersion = false;
        boolean needUpdate = false;
        try (BufferedReader br = new BufferedReader(new FileReader(HDIC_FILE))) {
            String s;
            while ((!searchedUpdateBotName || !searchedUpdateAuthor ||
                    !searchedUpdateVersion) && (s = br.readLine()) != null) {
                if (!searchedUpdateBotName && s.startsWith("名称:")) {
                    if (!s.equals("名称:" + AUTHOR_NAME + "Bot")) {
                        logInfo("检测到hdic.txt中[名称]需要更新！");
                        needUpdate = true;
                        break;
                    }
                    searchedUpdateBotName = true;
                } else if (!searchedUpdateAuthor && s.startsWith("作者:")) {
                    if (!s.equals("作者:" + AUTHOR_NAME + "(" + AUTHOR_QQ + ")")) {
                        logInfo("检测到hdic.txt中[作者]需要更新！");
                        needUpdate = true;
                        break;
                    }
                    searchedUpdateAuthor = true;
                } else if (!searchedUpdateVersion && s.startsWith("版本:")) {
                    if (!s.equals("版本:" + getVersion())) {
                        logInfo("检测到hdic.txt中[版本]需要更新！");
                        needUpdate = true;
                        break;
                    }
                    searchedUpdateVersion = true;
                }
            }
        } catch (IOException | UnexpectedStateException e) {
            logError(e);
        }
        if (!needUpdate) {
            return;
        }
        logInfo("正在将新词库信息写入hdic.txt.bak");
        File bak = new File(HDIC_FILE.getAbsolutePath() + ".bak");
        forceCreateNewFile(bak);
        searchedUpdateBotName = false;
        searchedUpdateAuthor = false;
        searchedUpdateVersion = false;
        try (BufferedReader br = new BufferedReader(new FileReader(HDIC_FILE));
             BufferedWriter bw = new BufferedWriter(new FileWriter(bak))) {
            String s;
            boolean firstLine = true;
            while ((s = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                } else {
                    bw.newLine();
                }
                if (!searchedUpdateBotName && s.startsWith("名称:")) {
                    bw.write("名称:" + AUTHOR_NAME + "Bot");
                    searchedUpdateBotName = true;
                } else if (!searchedUpdateAuthor && s.startsWith("作者:")) {
                    bw.write("作者:" + AUTHOR_NAME + "(" + AUTHOR_QQ + ")");
                    searchedUpdateAuthor = true;
                } else if (!searchedUpdateVersion && s.startsWith("版本:")) {
                    bw.write("版本:" + getVersion());
                    searchedUpdateVersion = true;
                } else {
                    bw.write(s);
                }
            }
        } catch (IOException | UnexpectedStateException e) {
            logError(e);
        }
        logInfo("已将新词库信息写入hdic.txt.bak");
        if (!deleteIfExists(HDIC_FILE)) {
            throw new UnexpectedStateException("hdic.txt删除失败");
        }
        logInfo("hdic.txt已删除");
        if (!bak.renameTo(HDIC_FILE)) {
            throw new UnexpectedStateException("hdic.txt.bak重命名失败");
        }
        logInfo("hdic.txt.bak已重命名为hdic.txt");
        reload();
        logInfo("hdic.txt已重载");
    }

    /**
     * 存放上次发言群的文件.
     */
    static File GROUP_OF_LAST_MSG;

    /**
     * 存放系统消息处理情况的文件.
     */
    private static File SYSTEM_PROCESS_STATE;

    /**
     * 处理系统消息.
     * <p>
     * 系统消息无法用 Api->getTextMsg() 等方式获取消息，只能使用 @c0 等获取参数。
     * <p>
     * 系统消息触发时，会收到多条系统消息，该方法也会被调用多次。
     * <p>
     * 系统消息格式为：System requestId qq 昵称 ["功能","信息","结果"]
     * <p>
     * requestId 共16位，前10位表示以秒为单位的时间戳，后6位可能是序号
     * <p>
     * 1.加群申请
     * <ul>
     * <li>qq：主动加群为0，被邀请加群为邀请人qq</li>
     * <li>昵称：主动加群为null，被邀请加群为邀请人昵称</li>
     * <li>功能：加群申请</li>
     * <li>信息：没有加群问题时为验证信息；有加群问题时匹配"问题：.*\\n答案：.*"（java程序里面显示为\\\\）</li>
     * <li>结果：""（表示未处理）、"已同意"、"已拒绝"中的一个</li>
     * </ul>
     * <b>【当一个申请被处理后，会产生一个新的系统消息，它有新的 requestId 以及处理结果，
     * 而原有 requestId 对应的系统消息将被删除，以后触发系统消息时不会再出现】</b>
     * <p>
     * eg1：System 1607910544255155 0 null ["加群申请","大家好，我是1。来自江苏盐城的天蝎座女一枚~",""]
     * <p>
     * eg2：System 1607266917616040 2019884867 摆地摊生活 ["加群申请","","已同意"]
     * <p>
     * 2.退群消息
     * <ul>
     * <li>qq：主动退群为0，被踢出群为操作者qq</li>
     * <li>昵称：主动退群为null，被踢出群为操作者昵称</li>
     * <li>功能：退群消息</li>
     * <li>信息：""</li>
     * <li>结果：""</li>
     * </ul>
     * eg1：System 1607234039068566 0 null ["退群消息","",""]
     * <p>
     * eg2：System 1608215122756115 605738729 萌泪 ["退群消息","",""]
     * <p>
     * 3.管理员设置
     * <ul>
     * <li>qq：群主qq</li>
     * <li>昵称：群主昵称</li>
     * <li>功能：管理员设置</li>
     * <li>信息：""</li>
     * <li>结果：""</li>
     * </ul>
     * eg：System 1608215096805348 605738729 萌泪 ["管理员设置","",""]
     * <p>
     * 无论上管理还是下管理都是一样的，没法判断（鸡肋功能orz）
     *
     * @param args 系统消息所需参数
     */
    private static void processSystemMsg(String[] args) {
        msgSource = MsgSource.GROUP;
        long requestId = Long.parseLong(args[1]);
        long sQQ = Long.parseLong(args[2]);
        String sNick = args[3];
        String[] info = new String[3];
        try {
            JSONArray array = new JSONArray(args[4]);
            info[0] = array.getString(0);
            info[1] = array.getString(1);
            info[2] = array.getString(2);
        } catch (JSONException ignored) {
            // 有时会出现信息不全的情况，忽略
            return;
        }
        // 每次得到新的System消息时，会读取近期所有system消息，该方法会短时间调用多次
        lock(SYSTEM_PROCESS_STATE);
        try {
            if (getLong(SYSTEM_PROCESS_STATE, requestId) != DEF_LONG) {
                return;
            }
            logInfo("收系统消息（" + info[0] + "）：群 " + group + "，QQ " + qq + "\n" +
                    "requestId " + requestId + "\n" +
                    "sQQ " + sQQ + ", sNick " + sNick + "\n" +
                    args[4]);
            boolean process = false;
            switch (info[0]) {
                case "邀请加群":
                    // 别人拉bot进群，需上号处理
                    process = true;
                    break;
                case "加群申请":
                    if ("".equals(info[2])) {
                        process = getFuncState(group, Func.PROCESS_ADD_GROUP_REQUEST);
                        if (process) {
                            new ProcessAddGroupRequest(requestId, sQQ, sNick, info[1], info[2]).process();
                        }
                    } else {
                        process = getFuncState(group, Func.WELCOME_NEW_MEMBER);
                        if (process) {
                            new WelcomeNewMember(requestId, sQQ, sNick, info[1], info[2]).process();
                        }
                    }
                    break;
                case "退群消息":
                    process = getFuncState(group, Func.SHOW_LEAVE_GROUP_INFO);
                    if (process) {
                        new ShowLeaveGroupInfo(requestId, sQQ, sNick, info[1], info[2]).process();
                    }
                    break;
                case "管理员设置":
                    process = getFuncState(group, Func.SHOW_CHANGE_ADMIN_INFO);
                    if (process) {
                        new ShowChangeAdminInfo(requestId, sQQ, sNick, info[1], info[2]).process();
                    }
                    break;
                default:
                    logError(new UnexpectedStateException("未知系统消息类型：" + info[0]));
            }
            if (process) {
                set(SYSTEM_PROCESS_STATE, requestId, 1);
            }
        } catch (RuntimeException e) {
            logError(e);
        } finally {
            unlock(SYSTEM_PROCESS_STATE);
        }
    }

    /**
     * 处理非系统消息.
     * <p>
     * 无论是群消息，还是私聊消息，最终都调用该方法。
     */
    private static void processCommonMsg() {
        // 把非负的优先级添加到 priorityList 中
        Set<Integer> priorityList = new HashSet<>();
        for (Func func : Func.values()) {
            if (func.priority >= 0) {
                priorityList.add(func.priority);
            }
        }
        // 将 priorityList 按照从小到大排序
        Set<Integer> sortPriorityList = new TreeSet<>(Integer::compareTo);
        sortPriorityList.addAll(priorityList);
        // 按优先级顺序依次执行，成功匹配词条则不执行后续优先级的功能
        for (int i : sortPriorityList) {
            for (Func func : Func.values()) {
                // 这里不判断功能是否启用，防止菜单xx失效
                if (func.priority == i) {
                    Class<? extends IProcess> clazz = func.getClazz();
                    try {
                        Constructor<? extends IProcess> constructor = clazz.getConstructor(Func.class);
                        FuncProcess obj = (FuncProcess) constructor.newInstance(func);
                        obj.run();
                    } catch (NoSuchMethodException | IllegalAccessException |
                             InstantiationException | InvocationTargetException e) {
                        logError(e);
                    }
                }
            }
            if (getProcessed()) {
                return;
            }
        }
    }

    // endregion
}
