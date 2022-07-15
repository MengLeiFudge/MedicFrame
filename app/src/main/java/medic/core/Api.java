package medic.core;

import android.content.Context;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

import static medic.core.Main.GROUP_OF_LAST_MSG;
import static medic.core.Utils.AUTHOR_QQ;
import static medic.core.Utils.DEF_LONG;
import static medic.core.Utils.DEF_STRING;
import static medic.core.Utils.Dir;
import static medic.core.Utils.ERR_LONG;
import static medic.core.Utils.ERR_STRING;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getLong;
import static medic.core.Utils.getStrFromURL;
import static medic.core.Utils.getString;
import static medic.core.Utils.lock;
import static medic.core.Utils.logError;
import static medic.core.Utils.logInfo;
import static medic.core.Utils.logWarn;
import static medic.core.Utils.set;
import static medic.core.Utils.unicodeToUtf8;
import static medic.core.Utils.unlock;

/**
 * 收发消息的类.
 * <p>
 * 所有方法均通过反射调用 medic 对应程序收发消息。
 * <p>
 * 请务必注意，<b>所有方法作用域均为本条消息</b>，
 * 在不同消息之间，它们不会生效，
 * 故<b>不会出现</b>新 api 覆盖旧 api，从而导致旧消息发送的对象也变化的现象。
 *
 * @author MengLeiFudge
 * @see Main
 */
public class Api {
    private Api() {
    }

    /**
     * 收发消息的实例对象，通过 {@link Main#apiSet(Object)} 获取初值.
     */
    static Object apiObj = null;
    public static String textMsg;
    public static String imgMsg;
    public static String jsonMsg;
    public static String xmlMsg;
    public static MsgType msgType;
    public static long group;
    public static String groupName;
    public static long qq;
    public static String groupNick;
    public static boolean isAdmin;
    public static long msgTime;
    public static int mark;
    public static MsgSource msgSource = MsgSource.NULL;
    /**
     * 待发送的消息，用于在 medic 的日志页面显示.
     */
    private static String temporaryMsg = "";
    private static final File NICK_DIR = getFile(Dir.SETTINGS, "groupNick");


    /**
     * 消息类型.
     */
    public enum MsgType {
        // 未知
        NULL,
        // 文本消息（日常打字）
        TEXT,
        // 图片消息（日常斗图）
        IMG,
        // 文本图片消息（既有文本又有图片）
        TEXT_IMG,
        // JSON 消息（红包消息）
        JSON,
        // XML 消息（通常是分享的链接）
        XML;

        @Override
        public String toString() {
            if (this == TEXT) {
                return "文本消息";
            } else if (this == IMG) {
                return "图片消息";
            } else if (this == TEXT_IMG) {
                return "文本图片消息";
            } else if (this == JSON) {
                return "JSON 消息";
            } else if (this == XML) {
                return "XML 消息";
            } else {
                return "未知类型消息";
            }
        }
    }

    /**
     * 消息来源.
     */
    public enum MsgSource {
        // 未赋值
        NULL,
        // 群消息
        GROUP,
        // 临时消息
        TEMPORARY,
        // 好友消息
        FRIEND;

        @Override
        public String toString() {
            if (this == GROUP) {
                return "群消息";
            } else if (this == TEMPORARY) {
                return "临时消息";
            } else if (this == FRIEND) {
                return "好友消息";
            } else {
                return "未知来源消息";
            }
        }
    }

    /**
     * 指示消息Api是否初始化成功.
     */
    static boolean initApiOk = false;

    /**
     * 初始化消息 Api，并从中获取必要的数据.
     */
    static void initApiBaseInfo(Object obj) {
        if (initApiOk) {
            return;
        }
        try {
            apiObj = obj;
            initApiOk = true;
            textMsg = getText();
            imgMsg = getImg();
            jsonMsg = getJson();
            xmlMsg = getXml();
            group = getGroup();
            groupName = getGroupName();
            qq = getQQ();
            groupNick = getGroupNick();
            msgTime = getMsgTime();
            mark = getMark();
            isAdmin = isAdmin(qq);
            if (!"".equals(jsonMsg)) {
                msgType = MsgType.JSON;
            } else if (!"".equals(xmlMsg)) {
                msgType = MsgType.XML;
            } else {
                boolean haveText = !"".equals(textMsg);
                boolean haveImg = !"".equals(imgMsg);
                if (haveText && haveImg) {
                    msgType = MsgType.TEXT_IMG;
                } else if (haveText) {
                    msgType = MsgType.TEXT;
                } else if (haveImg) {
                    msgType = MsgType.IMG;
                } else {
                    // 表情“快哭了”，或者文件上传到群的消息都属于此类
                    msgType = MsgType.NULL;
                }
            }
        } catch (RuntimeException e) {
            initApiOk = false;
        }
    }

    /**
     * 保存当前消息中所有群昵称.
     * <p>
     * 由于私聊消息群昵称为""且一定不含AT，故本方法仅在当前消息为群消息时有实际意义。
     */
    static void saveCodeAndNick() {
        if (getGroup() != -1) {
            lock(CODE_FILE);
            try {
                set(CODE_FILE, getGroup(), getCode());
            } finally {
                unlock(CODE_FILE);
            }
        }
        if (msgSource != MsgSource.GROUP) {
            return;
        }
        // qq、昵称写入数据库
        File nickFile = getFile(NICK_DIR, group + ".txt");
        lock(nickFile);
        try {
            set(nickFile, qq, groupNick);
            for (int i = 0; i < getAtNum(); i++) {
                long atq = getAtQQ(i);
                // 忽略@全体成员（即atq为0）
                if (atq != 0L) {
                    // todo: 将atqq、at群昵称、群号写入数据库
                    set(nickFile, atq, getAtNick(i));
                }
            }
        } finally {
            unlock(nickFile);
        }
    }

    /**
     * 反射调用 medic 中的方法.
     * <p>
     * 当调用结果为 null，或调用异常时，将返回""；否则返回调用结果。
     *
     * @param methodName 要调用的方法名
     * @param args       参数
     * @return 调用结果
     */
    private static String exec(String methodName, String... args) {
        if (!initApiOk) {
            logError(new UnexpectedStateException("Api 初始化失败，无法使用！"));
            return "";
        }
        try {
            Method method;
            Object result;
            if (args != null && args.length != 0) {
                method = apiObj.getClass().getMethod(methodName, String[].class);
                result = method.invoke(apiObj, (Object) args);
            } else {
                method = apiObj.getClass().getMethod(methodName);
                result = method.invoke(apiObj);
            }
            return result == null ? "" : result.toString();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            logError(e);
            return "";
        }
    }


    /* -- 接收消息 -- */

    /**
     * 返回消息中文本的内容.
     * <p>
     * 返回内容如下：
     * <p>
     * <ul>
     * <li>文本图片消息：文本存在返回文本内容，否则返回""</li>
     * <li>xml 消息：带链接时（即点击卡片可以打开网址），通常返回链接的网址</li>
     * <li>json 消息：略</li>
     * </ul>
     */
    private static String getText() {
        return exec("getTextMsg");
    }

    /**
     * 返回消息中已去掉"-{}"的图片名.
     * <p>
     * 临时消息、好友消息不会触发词条执行，也不能发送图片。
     * <p>
     * 返回内容如下：
     * <p>
     * <ul>
     * <li>纯文本/xml/json：""</li>
     * <li>单张图片：原本内容为"{8541DC6D-108B-448D-B702-3A07794B43CC}.jpg"，
     * 经过处理后返回"8541DC6D108B448DB7023A07794B43CC.jpg"</li>
     * <li>多张图片：原本内容为"{xxx1}.jpg{xxx2}.jpg"，处理方式同单张图片</li>
     * </ul>
     */
    private static String getImg() {
        return exec("getImageMsg").replaceAll("[-{}]", "");
    }

    /**
     * 返回消息中JSON的内容，如果没有则返回"".
     * <p>
     * 据说签到就是 json 格式，但它不会触发词条的执行（即不匹配.*），目前该方法无实际意义。
     */
    private static String getJson() {
        return exec("getJsonMsg");
    }

    /**
     * 返回消息中xml的内容，如果没有则返回"".
     */
    private static String getXml() {
        return exec("getXmlMsg");
    }

    /**
     * 返回当前消息的来源群，同@group.
     * <p>
     * 好友消息返回-1。
     */
    private static long getGroup() {
        return Long.parseLong(exec("getGroup"));
    }

    /**
     * 返回当前消息的来源群名，同@groupName.
     * <p>
     * 消息来源为好友消息时，返回""。
     */
    private static String getGroupName() {
        return exec("getGroupName");
    }

    /**
     * 返回当前消息的发送者，同@uin.
     */
    private static long getQQ() {
        return Long.parseLong(exec("getUin"));
    }

    /**
     * 返回当前消息发送者群昵称，同@nick.
     * <p>
     * 消息来源为私聊消息时，返回""。
     */
    private static String getGroupNick() {
        return exec("getUinName");
    }

    /**
     * 返回消息的发送时间戳，单位毫秒，同@time.
     * <p>
     * 如果想转换为字符串，使用 {@link Utils#timestampToStr} 等相关转换。
     */
    private static long getMsgTime() {
        return Long.parseLong(exec("getTime", "format"));
    }

    /**
     * 返回当前消息标题，同@title.
     *
     * @deprecated 该方法返回的值总为""，没有使用的必要
     */
    @Deprecated
    private static String getTitle() {
        return exec("getTitle");
    }

    /**
     * 返回当前消息code，同@code.
     * <p>
     * 当前消息为群消息/好友消息时，返回值与 {@link #getGroup()} 一致；
     * 当前消息为临时消息时，返回值是特有的值。
     */
    private static long getCode() {
        return Long.parseLong(exec("getCode"));
    }

    /**
     * 返回当前消息的消息标记，用于撤回消息.
     * <p>
     * 消息来源为临时消息/好友消息时，返回 -1。
     * <p>
     * 每个群的消息标记是独立的；对于同一个群，群中每发一条消息则标记 +1。
     *
     * @see #withdrawMsg
     */
    private static int getMark() {
        return Integer.parseInt(exec("getMark"));
    }

    /**
     * 返回当前消息中@的对象个数.
     */
    public static int getAtNum() {
        int num = Integer.parseInt(exec("getAtCnt"));
        return num == -1 ? 0 : num;
    }

    /**
     * 返回当前消息@的第 index+1 个对象的QQ，注意一条消息可以@多个人.
     * <p>
     * 如果没有获取到，返回 {@link Utils#ERR_LONG}。
     */
    public static long getAtQQ(int index) {
        String at = exec("getAt", index + "");
        if ("".equals(at)) {
            return ERR_LONG;
        }
        try {
            return new JSONArray(at).getLong(0);
        } catch (JSONException e) {
            logError(e);
            return ERR_LONG;
        }
    }

    /**
     * 返回当前消息@的第一个对象的QQ.
     * <p>
     * 如果没有获取到，返回 {@link Utils#ERR_LONG}。
     */
    public static long getAtQQ() {
        return getAtQQ(0);
    }

    /**
     * 返回当前消息@的所有对象的QQ.
     *
     * @return 当前消息@的所有对象的QQ.
     */
    public static long[] getAtQQs() {
        long[] atQQs = new long[getAtNum()];
        for (int i = 0; i < atQQs.length; i++) {
            atQQs[i] = getAtQQ(i);
        }
        return atQQs;
    }

    /**
     * 返回当前消息@的第index个对象的昵称，注意一条消息可以@多个人.
     * <p>
     * 如果没有获取到，返回""。
     */
    public static String getAtNick(int index) {
        String at = exec("getAt", index + "");
        if ("".equals(at)) {
            return "";
        }
        try {
            return new JSONArray(at).getString(1);
        } catch (JSONException e) {
            logError(e);
            return "";
        }
    }

    /**
     * 返回当前消息@的第一个对象的昵称.
     * <p>
     * 如果没有获取到，返回""。
     */
    public static String getAtNick() {
        return getAtNick(0);
    }

    /**
     * 返回当前消息@的所有对象的昵称.
     *
     * @return 当前消息@的所有对象的昵称.
     */
    public static String[] getAtNicks() {
        String[] atNicks = new String[getAtNum()];
        for (int i = 0; i < atNicks.length; i++) {
            atNicks[i] = getAtNick(i);
        }
        return atNicks;
    }

    /**
     * 返回上下文消息.
     *
     * @return 上下文消息
     */
    public static Context getContext() {
        if (apiObj == null) {
            logError(new UnexpectedStateException("apiObj 未初始化！"));
            return null;
        }
        try {
            Method method = apiObj.getClass().getMethod("getContext");
            Object result = method.invoke(apiObj);
            return result == null ? null : (Context) result;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            logError(e);
            return null;
        }
    }


    /* -- 发送消息 -- */

    /**
     * 添加@呼叫，不会立即发送，直到调用send.
     * <p>
     * 自带 @，显示的信息是 @ + info
     */
    public static void addAt(long qq, boolean newLine) {
        if (msgSource != MsgSource.GROUP) {
            return;
        }
        String nick = getNick(qq);
        exec("addAt", qq + "", nick);
        temporaryMsg += "@" + nick;
        if (newLine) {
            addText("\n");
        }
    }

    /**
     * 添加@呼叫并换行，不会立即发送，直到调用send.
     */
    public static void addAt(long qq) {
        addAt(qq, true);
    }

    /**
     * 添加对消息发送者的@呼叫，不会立即发送，直到调用send.
     */
    public static void addAt() {
        addAt(qq, true);
    }

    /**
     * 添加@全体，不会立即发送，直到调用send.
     */
    public static void addAtAll(String info) {
        exec("addAt", "0", info);
        temporaryMsg += "@" + info;
    }

    /**
     * 添加@全体，不会立即发送，直到调用send.
     */
    public static void addAtAll() {
        addAtAll("全体成员");
    }

    /**
     * 添加文本，不会立即发送，直到调用send.
     */
    public static void addText(String text) {
        exec("addText", text);
        temporaryMsg += text;
    }

    /**
     * 添加 url/本地 图片，不会立即发送，直到调用send.
     */
    public static void addImg(String pathOrUrl) {
        exec("addImg", pathOrUrl);
        temporaryMsg += "[图片 " + pathOrUrl + "]";
    }

    /**
     * 添加本地图片，不会立即发送，直到调用send.
     */
    public static void addImg(File file) {
        try {
            addImg(file.getCanonicalPath());
        } catch (IOException e) {
            logError(e);
        }
    }

    /**
     * 返回收到的消息中，第一张图片的 url.
     *
     * @return 第一张图片的 url
     */
    public static String getFirstImgUrl() {
        if ("".equals(imgMsg)) {
            throw new UnexpectedStateException("收到的消息不含图片");
        }
        // imageMsg = "9E9AFE870DD7FACAB9D9CDA7365799EB.jpg"
        // imageMsg = "XXX.PNG"
        // imageMsg = "XXX1.jpgXXX2.jpg"
        int index = imgMsg.indexOf('.');
        String imgType = imgMsg.substring(index + 1, index + 4);
        // 图片md5，如"9E9AFE870DD7FACAB9D9CDA7365799EB"
        String img;
        // 带.的后缀名，且有大小写区分，如".jpg"、".png"、".JPEG"
        String type;
        if (imgType.matches("(?i)(jpg|png|gif|mir)")) {
            img = imgMsg.substring(0, index);
            type = imgMsg.substring(index, index + 4);
        } else if (imgType.matches("(?i)(jpe)")) {
            img = imgMsg.substring(0, index);
            type = imgMsg.substring(index, index + 5);
        } else {
            throw new UnexpectedStateException("未知图片格式[" + imgType + "]\n" +
                    "imgMsg[" + imgMsg + "]");
        }
        return "http://gchat.qpic.cn/gchatpic_new/" + qq + "/0-0-" +
                img + "/0?term=2" + type;
    }

    /**
     * 添加图片消息，图片是收到的图片（如果有的话）.
     */
    public static void addFirstImgIfExists() {
        if ("".equals(imgMsg)) {
            return;
        }
        addImg(getFirstImgUrl());
    }

    /**
     * 设置消息发送的群号，仅在发送目标与消息来源不同时调用.
     *
     * @see #getGroup()
     */
    static void setGroup(long group) {
        exec("setId", group + "");
        Api.group = group;
    }

    /**
     * 设置消息发送的群Code，仅在发送目标与消息来源不同时调用.
     *
     * @see #getCode()
     */
    static void setCode(long code) {
        exec("setCode", code + "");
    }

    /**
     * 设置消息发送目标，仅在发送目标与消息来源不同时调用.
     *
     * @see #getQQ()
     */
    static void setQQ(long qq) {
        exec("setUin", qq + "");
        Api.qq = qq;
    }

    /**
     * 存储code的文件， 格式：group=code.
     */
    public static final File CODE_FILE = getFile(Dir.SETTINGS, "code.txt");

    /**
     * 更改消息发送的目标为群消息，仅在当前消息为群消息时生效.
     */
    public static boolean changeGroupToOtherGroup(long group) {
        if (msgSource != MsgSource.GROUP) {
            logError(new UnexpectedStateException("只有群消息才能将消息发送目标设为其他群"));
            return false;
        }
        setGroup(group);
        return true;
    }

    /**
     * 更改消息发送的目标为好友消息.
     *
     * @param qq 消息发送的目标，需为bot好友
     * @deprecated 由于不清楚该qq是否为好友，而设置来源为临时消息时，好友也能收到消息，
     * 故应使用 {@link #changeAllSourceToPrivate(long)} 代替
     */
    @Deprecated
    public static void changeAllSourceToFriend(long qq) {
        setGroup(-1);
        setCode(-1);
        setQQ(qq);
        msgSource = MsgSource.FRIEND;
    }

    /**
     * 更改消息发送的目标为私聊消息.
     */
    public static boolean changeGroupToPrivate() {
        return changeAllSourceToPrivate(qq);
    }

    /**
     * 更改消息发送的目标为私聊消息.
     */
    public static boolean changeAllSourceToPrivate(long qq) {
        long lastGroup = getLong(GROUP_OF_LAST_MSG, qq);
        if (lastGroup == DEF_LONG) {
            //logError(new UnexpectedStateException(getNick(qq) + "(" + qq + ")未发过言！"));
            //return false;
            lastGroup = 319567534L;
            logWarn(new UnexpectedStateException(getNick(qq) + "(" + qq + ")未发过言，使用群319567534代替！"));
        }
        long code = getLong(CODE_FILE, lastGroup + "");
        if (code != DEF_LONG) {
            setCode(code);
            setQQ(qq);
        } else {
            //logError(new UnexpectedStateException("未找到群" + lastGroup + "的code！"));
            //return false;
            setCode(lastGroup);
            setQQ(qq);
            logWarn(new UnexpectedStateException("未找到群" + lastGroup + "的code，使用群号代替！"));
        }
        msgSource = MsgSource.TEMPORARY;
        return true;
    }

    /**
     * 在 medic 的日志界面显示发送的消息内容.
     */
    public static void showMsgToBeSentLog() {
        logInfo("发" + msgSource + "：群 " + group + "，QQ " + qq + "\n" +
                temporaryMsg);
        /*
        if (msgSource == MsgSource.GROUP) {
            logInfo("发" + msgSource + "：群 " + group + "\n" +
                    temporaryMsg);
        } else if (msgSource == MsgSource.TEMPORARY) {
            logInfo("发" + msgSource + "：群 " + group + "，QQ " + qq + "\n" +
                    temporaryMsg);
        } else if (msgSource == MsgSource.FRIEND) {
            logInfo("发" + msgSource + "：QQ " + qq + "\n" +
                    temporaryMsg);
        }
         */
        temporaryMsg = "";
    }

    /**
     * 发送储存的消息.
     */
    public static void send() {
        exec("send");
        showMsgToBeSentLog();
    }

    /**
     * 发送参数中的消息，自动识别 xml、json.
     * <p>
     * 当 msg 为 xml/json 时，相当于调用 {@link #sendXml(String)} 或 {@link #sendJson(String)}；
     * 否则相当于先调用 {@link #addText(String)}，再调用 {@link #send()}。
     *
     * @param msg 机器人要发的消息
     * @deprecated 添加图片后调用该方法，将导致 {@link InvocationTargetException}；
     * 使用 {@link #send(String)} 代替
     */
    @Deprecated
    public static void sendMsg(String msg) {
        // 等价于先 add，再 send
        exec("sendMsg", msg);
        temporaryMsg += msg;
        showMsgToBeSentLog();
    }

    /**
     * 发送参数中的消息，自动识别 xml、json.
     * <p>
     * 当 msg 为 xml/json 时，相当于调用 {@link #sendXml(String)} 或 {@link #sendJson(String)}；
     * 否则相当于先调用 {@link #addText(String)}，再调用 {@link #send()}。
     *
     * @param text 机器人要发的消息
     */
    public static void send(String text) {
        addText(text);
        send();
    }

    public static void send(long qq, String msg) {
        addAt(qq);
        addText(msg);
        send();
    }

    /**
     * 发送参数中的消息，只识别json，其他内容将被无视.
     */
    public static void sendJson(String json) {
        if (!"".equals(temporaryMsg)) {
            throw new UnexpectedStateException("还有消息未发送");
        }
        exec("sendJson", json);
        temporaryMsg += json;
        showMsgToBeSentLog();
    }

    /**
     * 发送参数中的消息，只识别xml，其他内容将被无视.
     */
    public static void sendXml(String xml) {
        if (!"".equals(temporaryMsg)) {
            throw new UnexpectedStateException("还有消息未发送");
        }
        exec("sendXml", xml);
        temporaryMsg += xml;
        showMsgToBeSentLog();
    }

    /**
     * 发送语音消息，参数为直链网址或本地路径.
     */
    public static void sendVoice(String pathOrUrl) {
        exec("sendPtt", pathOrUrl);
        if (msgSource == MsgSource.GROUP) {
            logInfo("发群消息：" + getQQ() + "，群 " + getGroup() + "\n" + pathOrUrl);
        } else if (msgSource == MsgSource.TEMPORARY) {
            logInfo("发临时消息：" + getQQ() + "，群 " + getGroup() + "\n" + pathOrUrl);
        } else if (msgSource == MsgSource.FRIEND) {
            logInfo("发好友消息：" + getQQ() + "，群 " + getGroup() + "\n" + pathOrUrl);
        }
    }

    /**
     * 发送语音消息，参数为直链网址或本地路径，自定义语音时长（单位秒）.
     */
    public static void sendVoice(String pathOrUrl, int second) {
        exec("sendPtt", pathOrUrl, second + "");
        if (msgSource == MsgSource.GROUP) {
            logInfo("发群消息：" + getQQ() + "，群 " + getGroup() + "\n" + pathOrUrl);
        } else if (msgSource == MsgSource.TEMPORARY) {
            logInfo("发临时消息：" + getQQ() + "，群 " + getGroup() + "\n" + pathOrUrl);
        } else if (msgSource == MsgSource.FRIEND) {
            logInfo("发好友消息：" + getQQ() + "，群 " + getGroup() + "\n" + pathOrUrl);
        }
    }

    /**
     * 默认红包标题.
     */
    private static final String DEFAULT_TITLE = "恭喜发财";

    /**
     * 发送专属红包，需要权限：红包支付.
     * <p>
     * 参数为群号，红包标题，总金额（分为单位），领取人QQ（可以多人）。
     */
    private static void sendRedPacket(long group, String title, int cent, long... qq) {
        String yuan = new DecimalFormat("#.00").format(cent / 100.0);
        if (cent > 20000) {
            logError(new UnexpectedStateException("红包金额" + yuan + "r大于200r"));
            return;
        }
        if (cent < qq.length) {
            logError(new UnexpectedStateException("红包金额" + yuan + "r小于" + (0.01 * qq.length) + "r"));
            return;
        }
        String[] array = new String[qq.length + 3];
        array[0] = group + "";
        array[1] = title == null || "".equals(title) ? DEFAULT_TITLE : title;
        array[2] = cent + "";
        for (int i = 3; i < array.length; i++) {
            array[i] = qq[i - 3] + "";
        }
        exec("sendRedPacket", array);
    }

    public static void sendRedPacket(long group, String title, double yuan, long... qq) {
        int cent = (int) (yuan * 100);
        sendRedPacket(group, title, cent, qq);
    }

    public static void sendRedPacket(double yuan, long... qq) {
        sendRedPacket(group, DEFAULT_TITLE, yuan, qq);
    }

    public static void sendRedPacket(String title, double yuan) {
        sendRedPacket(group, title, yuan, qq);
    }

    public static void sendRedPacket(double yuan) {
        sendRedPacket(group, DEFAULT_TITLE, yuan, qq);
    }


    /* -- 系统功能 -- */

    /**
     * 重载词库.
     */
    public static void reload() {
        exec("reload");
    }

    /**
     * 检查是否为机器人管理员（即主人，作用同 medic 主界面管理员）.
     */
    private static boolean isAdmin(long qq) {
        return qq == AUTHOR_QQ || "true".equals(exec("checkAdmin", qq + ""));
    }

    /**
     * 设置机器人管理员（即主人，作用同 medic 主界面管理员）.
     */
    public static void setAdmin(long qq, boolean isAdmin) {
        if (qq == AUTHOR_QQ) {
            isAdmin = true;
        }
        exec("setAdmin", qq + "", isAdmin + "");
    }

    /**
     * 返回本地数据库/API中某个 QQ 对应的昵称.
     *
     * @param qq 要获取昵称的 QQ
     * @return 对应的昵称，本地数据库或api都没有时返回未知昵称
     */
    public static String getNick(long qq) {
        // 从本地数据库的本群获取昵称
        String nick = getString(getFile(NICK_DIR, group + ".txt"), qq);
        if (!DEF_STRING.equals(nick)) {
            return nick;
        }
        // 从本地数据库的其他群获取昵称
        File[] files = getFile(NICK_DIR).listFiles();
        if (files != null) {
            for (File f : files) {
                nick = getString(f, qq);
                if (!DEF_STRING.equals(nick)) {
                    return nick;
                }
            }
        }
        // 从api获取昵称
        nick = getNickFromZhai78(qq);
        if (!ERR_STRING.equals(nick)) {
            return nick;
        }
        nick = getNickFromVvhan(qq);
        if (!ERR_STRING.equals(nick)) {
            return nick;
        }
        return "未知昵称";
    }

    private static String getNickFromZhai78(long qq) {
        // {"nickName":"\u840c\u6cea\u9171\u6700\u53ef\u7231\u5566",
        // "avatar":"http:\/\/q1.qlogo.cn\/g?b=qq&nk=605738729&s=0","status":1,
        // "origin":"\u63a5\u53e3\u6e90\u7801\u83b7\u53d6\u53ca\u514d\u8d39\u8c03\u7528\u5730\u5740www.zhai78.com"}
        String s = getStrFromURL("https://data.zhai78.com/openQqDetail.php?qq=" + qq, null);
        if (ERR_STRING.equals(s)) {
            return ERR_STRING;
        }
        try {
            JSONObject obj = new JSONObject(s);
            if (obj.has("nickName")) {
                return unicodeToUtf8(obj.getString("nickName"));
            } else {
                return ERR_STRING;
            }
        } catch (JSONException e) {
            logWarn(e);
            return ERR_STRING;
        }
    }

    private static String getNickFromVvhan(long qq) {
        // {"success":true,"imgurl":"https://q2.qlogo.cn/headimg_dl?dst_uin=605738729&spec=640",
        // "name":"萌泪酱最可爱啦","qemail":"605738729@qq.com",
        // "qzone":"https://user.qzone.qq.com/605738729"}
        String s = getStrFromURL("https://api.vvhan.com/api/qq?qq=" + qq, null);
        if (ERR_STRING.equals(s)) {
            return ERR_STRING;
        }
        try {
            JSONObject obj = new JSONObject(s);
            if (obj.has("success") && obj.getBoolean("success") && obj.has("name")) {
                return obj.getString("name");
            } else {
                return ERR_STRING;
            }
        } catch (JSONException e) {
            logWarn(e);
            return ERR_STRING;
        }
    }

    /**
     * 返回当前登录账号，同@robot.
     */
    public static long getRobotQQ() {
        return Long.parseLong(exec("getAcct"));
    }

    /**
     * 返回当前设备的信息，需要权限：手机信息.
     * <p>
     * 该方法会返回一个 json 数组，包含系统版本、手机型号等等。
     */
    public static String getMachineCode() {
        return exec("getMachineCode");
    }

    /**
     * 同意/拒绝入群申请，需要登录账号在群内是管理员，此方法仅在内置词条System中有效.
     */
    public static void joinRequest(long group, long member, long requestId, boolean agree) {
        exec("joinRequest", group + "",
                member + "", requestId + "", agree ? "0" : "1");
    }

    /**
     * 删除群成员，需要登录账号在群内是管理员.
     */
    public static void removeMember(long group, long member) {
        exec("deleteMember", group + "", member + "");
    }

    /**
     * 设置群成员名片，需要登录账号在群内是管理员.
     */
    public static void setGroupNick(long member, String groupNick) {
        exec("setMemberCard", getGroup() + "", member + "", groupNick);
    }

    /**
     * 撤回当前消息，需要登录账号在群内是管理员.
     * <p>
     * 撤回特定消息参考 {@link #withdrawMsg(int)}
     */
    public static void withdrawMsg() {
        exec("withDrawMsg");
    }

    /**
     * 撤回消息来源群的特定消息，需要登录账号在群内是管理员.
     *
     * @param mark 消息标记
     * @see #getMark()
     */
    public static void withdrawMsg(int mark) {
        exec("withDrawMsg", mark + "");
    }

    /**
     * 撤回任意群的特定消息，需要登录账号在群内是管理员.
     * <p>
     * 如果想连续撤回多条消息，必须自己保存所有想撤回消息对应的群号和mark。
     *
     * @param group 群号
     * @param mark  消息标记
     * @see #getMark()
     */
    public static void withdrawMsg(long group, int mark) {
        exec("withDrawMsg", group + "", mark + "");
    }

    /**
     * 禁言，需要登录账号在群内是管理员，member为-1表示群禁言，time为0表示解除禁言.
     */
    private static void talk(long member, int time) {
        exec("shotup", getGroup() + "", member + "", time + "");
    }

    /**
     * 禁言群成员，需要登录账号在群内是管理员.
     */
    public static void notAllowTalking(long member, int second) {
        talk(member, second);
    }

    /**
     * 解禁群成员，需要登录账号在群内是管理员.
     */
    public static void allowTalking(long member) {
        talk(member, 0);
    }

    /**
     * 禁言群，需要登录账号在群内是管理员.
     */
    public static void notAllowGroupTalking() {
        talk(-1, 1);
    }

    /**
     * 解禁群，需要登录账号在群内是管理员.
     */
    public static void allowGroupTalking() {
        talk(-1, 0);
    }

    /**
     * 返回所有群的群名、最新公告、群号、code、群员数量.
     */
    private static String getAllGroupInfo() {
        return exec("getTroopList");
    }

    private static class GroupBaseInfo {
        @Getter
        String groupName;
        /**
         * 最新的群公告.
         */
        @Getter
        String announcement;
        @Getter
        long group;
        @Getter
        long code;
        @Getter
        int memberNum;

        GroupBaseInfo(JSONObject obj) {
            try {
                groupName = obj.getString("name");
                announcement = obj.getString("info");
                group = obj.getLong("id");
                code = obj.getLong("code");
                memberNum = obj.getInt("memberCnt");
            } catch (JSONException e) {
                logError(e);
                groupName = "";
                announcement = "";
                group = 0L;
                code = 0L;
                memberNum = 0;
            }
        }
    }

    /**
     * 返回所有群群号.
     */
    public static long[] getGroups() {
        long[] groupArray = new long[1];
        try {
            JSONArray jsonArray = new JSONArray(getAllGroupInfo());
            groupArray = new long[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                GroupBaseInfo group = new GroupBaseInfo(jsonArray.getJSONObject(i));
                groupArray[i] = group.getGroup();
            }
        } catch (JSONException e) {
            logError(e);
        }
        return groupArray;
    }

    /**
     * 返回某个群所有群成员的昵称、群名片、头衔、qq.
     *
     * @deprecated 在群人数较多的情况下，该方法获取信息所需时间较长，且可能只获取部分信息，
     * 例如 2000 人群可能只返回 500 人的数据
     */
    @Deprecated
    private static String getAllGroupMemberInfo(long group) {
        return exec("getTroopMemberList", group + "");
    }

    private static class GroupMemberInfo {
        @Getter
        String groupNick;
        @Getter
        String qqNick;
        /**
         * 群主给予的私人头衔.
         */
        @Getter
        String title;
        @Getter
        long qq;

        GroupMemberInfo(JSONObject obj) {
            try {
                groupNick = obj.getString("card");
                qqNick = obj.getString("nick");
                title = obj.getString("title");
                qq = obj.getLong("uin");
            } catch (JSONException e) {
                logError(e);
                groupNick = "";
                qqNick = "";
                title = "";
                qq = 0L;
            }
        }
    }

    /**
     * 返回某个群所有群成员的qq.
     *
     * @deprecated 原因见 {@link #getAllGroupMemberInfo(long)}
     */
    @Deprecated
    public static long[] getGroupMembers(long group) {
        long[] memberArray = new long[1];
        try {
            JSONObject jsonObject = new JSONObject(getAllGroupMemberInfo(group));
            JSONArray jsonArray = jsonObject.getJSONArray(group + "");
            memberArray = new long[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                GroupMemberInfo member = new GroupMemberInfo(jsonArray.getJSONObject(i));
                memberArray[i] = member.getQq();
            }
        } catch (JSONException e) {
            logError(e);
        }
        return memberArray;
    }

    /**
     * 返回任何群的任意QQ的群名片.
     * <p>
     * 如果群名片为空，或者该QQ不在群中，返回QQ昵称。
     *
     * @deprecated 原因见 {@link #getAllGroupMemberInfo(long)}，
     * 请使用 {@link #getNick(long)} 代替
     */
    @Deprecated
    public static String getGroupNick(long group, long qq) {
        try {
            JSONObject jsonObject = new JSONObject(getAllGroupMemberInfo(group));
            JSONArray jsonArray = jsonObject.getJSONArray(group + "");
            for (int i = 0; i < jsonArray.length(); i++) {
                GroupMemberInfo member = new GroupMemberInfo(jsonArray.getJSONObject(i));
                if (member.getQq() == qq) {
                    String groupNick = member.getGroupNick();
                    return "".equals(groupNick) ? member.getQqNick() : groupNick;
                }
            }
        } catch (JSONException e) {
            logError(e);
        }
        logError(new UnexpectedStateException("QQ" + qq + "不在群" + group + "中"));
        return "";
    }

    /**
     * 返回消息来源群的任意QQ的群名片.
     * <p>
     * 如果群名片为空，或者该QQ不在群中，返回QQ昵称。
     *
     * @deprecated 原因见 {@link #getAllGroupMemberInfo(long)}，
     * 请使用 {@link #getNick(long)} 代替
     */
    @Deprecated
    public static String getGroupNick(long qq) {
        return getGroupNick(group, qq);
    }

    /**
     * 返回QQSkey，此参数可以用于登录QQ空间等.
     */
    public static String getSkey() {
        return exec("getSkey");
    }

    /**
     * 返回QQPSkey，此参数可以用于登录QQ空间等.
     */
    public static String getPSkey() {
        return exec("getPSkey");
    }

    /**
     * 返回ClientKey，此参数可以用于登录QQ空间等.
     */
    public static String getClientKey() {
        return exec("getClientKey").replace(" ", "");
    }
}
