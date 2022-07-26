package medic.core;

import android.os.Environment;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.Character.UnicodeBlock;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static medic.core.Api.changeAllSourceToPrivate;
import static medic.core.Api.isApiAvailable;
import static medic.core.Api.send;
import static medic.core.Main.Func;
import static medic.core.Main.LogFunc;
import static medic.core.Main.LogFunc.WRITE_ERROR_LOG;
import static medic.core.Main.LogFunc.WRITE_INFO_LOG;
import static medic.core.Main.LogFunc.WRITE_WARN_LOG;
import static medic.core.Main.getResetInstanceFile;

/**
 * 工具类，用于读取文件、取随机数等操作.
 */
public final class Utils {
    private Utils() {
    }


    /* -- 作者相关 -- */

    /**
     * 作者昵称.
     * <p>
     * 作者账户是唯一的，具有最高权限，可执行增删管理员、开关 Log 等操作.
     */
    public static final String AUTHOR_NAME = "萌泪";
    /**
     * 作者 QQ.
     * <p>
     * 作者账户是唯一的，具有最高权限，可执行增删管理员、开关 Log 等操作.
     */
    public static final long AUTHOR_QQ = 605738729L;
    /**
     * Bot 测试群.
     * <p>
     * 有些特殊功能仅在该群生效，如创建初始化文件等.
     */
    public static final long TEST_GROUP = 516286670L;

    /**
     * 是否显示调试信息.
     */
    static final boolean DEBUG = false;


    /* -- 目录定义 -- */

    /**
     * 内部共享存储空间路径.
     */
    private static File storageDir = null;

    /**
     * 初始化内部共享存储空间路径.
     * <ul>
     * <li>高版本安卓（安卓10及以上）可能无法读写该路径。
     * <li>该目录指向用户目录，而非根目录。
     * </ul>
     */
    @SuppressWarnings("deprecation")
    private static void initStorageDir() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 通常为 /storage/emulated/0
            storageDir = Environment.getExternalStorageDirectory();
        } else {
            logWarn(new UnexpectedStateException("未挂载SD卡，将使用根目录，可能导致读写失败"));
            storageDir = new File("");
        }
    }

    /**
     * 指示各个基础目录.
     * <p>
     * 注意，该类只能使用 showLog
     */
    public enum Dir {
        // medic 基础目录
        DIC("DIC"),
        // 应用数据，如序列化对象、图片等，每个应用应使用自定义子文件夹
        DATA("DIC", "data"),
        // 日志
        LOG("DIC", "log"),
        // 系统配置，如各模块开关情况
        SETTINGS("DIC", "settings"),
        // 数据库
        DATABASE("DIC", "database");

        private final File dir0;

        Dir(String... names) {
            if (storageDir == null) {
                initStorageDir();
            }
            dir0 = FileUtils.getFile(storageDir, names);
            dir0.mkdirs();
        }

        File getFile() {
            return dir0;
        }

        boolean isDirectory() {
            return dir0.isDirectory();
        }
    }

    /**
     * 指示本机存储路径是否可用.
     */
    static boolean isDirsAvailable = false;

    /**
     * 初始化本机存储路径.
     * <p>
     * 如果成功，则 {@link #isDirsAvailable} 置为 {@code true}；否则显示错误日志。
     */
    static void initDirs() {
        try {
            boolean ok = true;
            for (Dir dir : Dir.values()) {
                ok &= dir.isDirectory();
            }
            isDirsAvailable = ok;
        } catch (RuntimeException e) {
            isDirsAvailable = false;
            logError(e);
        }
    }


    /* -- Log 相关 -- */

    enum LogType {
        // 错误，表示不能继续运行的情况
        ERROR,
        // 警告，表示某些不影响运行的特殊情况
        WARN,
        // 信息，表示通常的信息
        INFO;

        /**
         * 仅用于 {@link #showLog(LogType, String)} 中，反射调用的第一个参数.
         *
         * @return log 类型对应的参数字符串
         */
        public String firstLowerChar() {
            if (this == ERROR) {
                return "e";
            } else if (this == WARN) {
                return "w";
            } else {
                return "i";
            }
        }

        /**
         * 返回是否保存该级别日志.
         *
         * @return 是否保存该级别日志
         */
        public boolean writeLog() {
            if (this == ERROR) {
                return getFuncState(WRITE_ERROR_LOG);
            } else if (this == WARN) {
                return getFuncState(WRITE_WARN_LOG);
            } else {
                return getFuncState(WRITE_INFO_LOG);
            }
        }
    }

    /**
     * 暂存要发给作者的 error 信息，使用 set 以去掉重复的 error.
     */
    private static final Set<String> tempErrorSendToAuthor = new HashSet<>();

    static void sendAllErrorToAuthor() {
        if (!tempErrorSendToAuthor.isEmpty()) {
            changeAllSourceToPrivate(AUTHOR_QQ);
            // 最多发送两条，防止发太多导致 bot 冻结
            for (int i = 0; i < tempErrorSendToAuthor.size(); i++) {
                if (i < 3) {
                    for (String s : tempErrorSendToAuthor) {
                        send(Thread.currentThread().getId() + "\n" + s);
                        sleep(500);
                    }
                } else {
                    send(Thread.currentThread().getId() + "\n出现多个错误，请查看日志！");
                    break;
                }
            }
        }
    }

    /**
     * 在 medic 日志界面显示日志.
     *
     * @param type log类型
     * @param info 要记录的信息
     */
    private static void showLog(LogType type, String info) {
        String[] data = {type.firstLowerChar(), info};
        try {
            Method log = Class.forName("app.yashiro.medic.app.dic.Toolkit")
                    .getMethod("log", String[].class);
            log.invoke(null, (Object) data);
        } catch (ClassNotFoundException | NoSuchMethodException
                 | InvocationTargetException | IllegalAccessException e) {
            //ignore
        }
    }

    /**
     * 将日志信息保存在文件中，受设置影响.
     *
     * @param type log类型
     * @param info 要记录的信息
     */
    private static void writeLog(LogType type, String info) {
        if (!isDirsAvailable || !type.writeLog()) {
            return;
        }
        long time = System.currentTimeMillis();
        File f = getFile(Dir.LOG, getDateStr(time) + ".txt");
        if (!createFileIfNotExists(f)) {
            return;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
            bw.write(type + " " + getFullTimeStr(time));
            bw.newLine();
            bw.write(info);
            // 空两行便于阅读
            bw.newLine();
            bw.newLine();
            bw.newLine();
        } catch (IOException e) {
            isDirsAvailable = false;
            logError(e);
        }
    }

    /**
     * 将 extraInfo 与 e 所含的信息拼接.
     * <p>
     * 如果 extraInfo 为 {@code null}，或进行 {@link String#trim()} 处理后为空字符串 ""，
     * extraInfo 将被定义为空；
     * <p>
     * 如果 e 为 {@code null}，或将其转化为具有异常类型、异常描述信息、方法调用情况的
     * 字符串并进行 {@link String#trim()} 处理后为空字符串 ""，
     * exceptionInfo 将被定义为空；
     * <p>
     * 根据 extraInfo 与 exceptionInfo 是否为空，有以下三种情况：
     * <ul>
     * <li>都没有实际意义，返回空字符串 ""
     * <li>仅一个有实际意义，返回二者直接拼接的结果
     * <li>都有实际意义，返回二者用 {@code \n} 拼接的结果
     * </ul>
     *
     * @param extraInfo 额外信息
     * @param e         异常
     * @return 拼接结果
     */
    static String spliceExtraInfoAndException(String extraInfo, Exception e) {
        if (extraInfo == null) {
            extraInfo = "";
        } else {
            extraInfo = extraInfo.trim();
        }
        boolean emptyExtraInfo = "".equals(extraInfo);

        String exceptionInfo;
        if (e == null) {
            exceptionInfo = "";
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            exceptionInfo = baos.toString().trim();
        }
        boolean emptyExceptionInfo = "".equals(exceptionInfo);

        if (emptyExtraInfo && emptyExceptionInfo) {
            return "";
        } else if (!emptyExtraInfo && !emptyExceptionInfo) {
            return extraInfo + "\n" + exceptionInfo;
        } else {
            return extraInfo + exceptionInfo;
        }
    }

    /**
     * 指示是否已发过错误信息.
     */
    private static boolean hasTip = false;

    /**
     * show、write、send Log.
     * <ul>
     * <li>show：在 medic 日志界面显示日志
     * <li>write：根据设置决定是否保存日志到文件
     * <li>send：type 为 {@code LogType.ERROR} 时，将部分日志信息作为消息发出
     * </ul>
     * {@code extraInfo} 和 {@code e} 二者共同决定最终的日志信息，
     * 具体拼接规则见 {@link #spliceExtraInfoAndException(String, Exception)}.
     *
     * @param type      log类型
     * @param extraInfo 额外信息
     * @param e         异常
     */
    static void swsLog(LogType type, String extraInfo, Exception e) {
        String fullInfo = spliceExtraInfoAndException(extraInfo, e);
        showLog(type, fullInfo);
        writeLog(type, fullInfo);
        if (type == LogType.ERROR) {
            // 向消息源提示
            if (!hasTip) {
                send("啊这。。。好像出现了一些错误呢！\n" +
                        "bug已经反馈给" + AUTHOR_NAME + "(" + AUTHOR_QQ + ")啦~\n" +
                        "过段时间应该就会修复的（咕咕咕）");
                hasTip = true;
            }
            // 向作者发送消息，先将其暂存，因为群消息切到私聊就不能切回群
            tempErrorSendToAuthor.add(fullInfo);
        }
    }

    /**
     * 记录并发送一个 Error.
     *
     * @param e 要记录的异常
     */
    public static void logError(Exception e) {
        swsLog(LogType.ERROR, null, e);
    }

    /**
     * 记录并发送一个 Error，可以附加额外信息.
     *
     * @param extraInfo 补充信息，用于定位问题，如方法的参数值等
     * @param e         异常
     */
    public static void logError(String extraInfo, Exception e) {
        swsLog(LogType.ERROR, extraInfo, e);
    }

    /**
     * 记录一个 Warn.
     *
     * @param e 要记录的异常
     */
    public static void logWarn(Exception e) {
        swsLog(LogType.WARN, null, e);
    }

    /**
     * 记录一个 Warn，可以附加额外信息.
     *
     * @param extraInfo 补充信息，用于定位问题，如方法的参数值等
     * @param e         要记录的异常
     */
    public static void logWarn(String extraInfo, Exception e) {
        swsLog(LogType.WARN, extraInfo, e);
    }

    /**
     * 记录一个 Info.
     *
     * @param info 要记录的信息
     */
    public static void logInfo(String info) {
        swsLog(LogType.INFO, info, null);
    }


    /* -- 功能开启、关闭 -- */

    /**
     * 使调用该方法的线程休眠一段时间.
     *
     * @param milliTime 要休眠的时间，单位 ms
     */
    public static void sleep(long milliTime) {
        try {
            Thread.sleep(milliTime);
        } catch (InterruptedException e) {
            logError(e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 表示开启、关闭的中文字符串.
     * <p>
     * 使用 String 而非 int 的原因见 {@link #getString(File, Object)}.
     * <p>
     * 且字符串使用中文，可在 {@link #getFuncStateStr} 中复用.
     */
    public static final String OPEN = "开启";
    public static final String CLOSE = "关闭";

    /**
     * 获取 非Log功能 启用状态，默认关闭.
     * <p>
     * 临时消息正常处理，好友消息使用上一次发言的群号。
     *
     * @param group 群号
     * @param func  某个功能
     * @return 文件中的功能状态字符串为 {@link #OPEN} 时返回 {@code true}，
     * 否则返回 {@code false}
     */
    public static boolean getFuncState(long group, Func func) {
        if (group == -1) {
            logError(new UnexpectedStateException("该方法只能用于群消息，group 不能为 -1"));
            return false;
        }
        if (func.getIndex() <= 0) {
            return true;
        }
        // todo: 使用数据库
        String s = getString(getFile(Dir.SETTINGS, "funcState", group + ".txt"), func.getName());
        if (s.equals(DEF_STRING)) {
            setFuncState(group, func, false);
            return false;
        } else if (s.equals(ERR_STRING)) {
            return false;
        } else {
            return s.equals(OPEN);
        }
    }

    /**
     * 获取 Log功能 启用状态，默认 Error/Warn 开启，Info 关闭.
     *
     * @param func 某个 log 功能
     * @return 文件中的功能状态字符串为 {@link #OPEN} 时返回 {@code true}，
     * 否则返回 {@code false}
     */
    public static boolean getFuncState(LogFunc func) {
        String s = getString(getFile(Dir.SETTINGS, "logState.txt"), func.getName());
        boolean shouldOpen = func != WRITE_INFO_LOG;
        // todo: 使用数据库
        if (s.equals(DEF_STRING)) {
            setFuncState(func, shouldOpen);
            return shouldOpen;
        } else if (s.equals(ERR_STRING)) {
            return shouldOpen;
        } else {
            return s.equals(OPEN);
        }
    }

    /**
     * 获取 非Log功能 启用状态字符串.
     * <p>
     * 临时消息正常处理，好友消息使用上一次发言的群号。
     *
     * @param group 群号
     * @param func  某个功能
     * @return 文件中的功能状态字符串为 {@link #OPEN} 时返回 {@link #OPEN}，
     * 否则返回 {@link #CLOSE}
     */
    public static String getFuncStateStr(long group, Func func) {
        return getFuncState(group, func) ? OPEN : CLOSE;
    }

    /**
     * 获取 Log功能 启用状态字符串.
     *
     * @param func 某个 log 功能
     * @return 文件中的功能状态字符串为 {@link #OPEN} 时返回 {@link #OPEN}，
     * 否则返回 {@link #CLOSE}
     */
    public static String getFuncStateStr(LogFunc func) {
        return getFuncState(func) ? OPEN : CLOSE;
    }

    /**
     * 开启/关闭指定 非Log功能.
     *
     * @param group 群号
     * @param func  功能
     * @param open  开启还是关闭功能
     */
    public static void setFuncState(long group, Func func, boolean open) {
        if (group == -1) {
            logError(new UnexpectedStateException("该方法只能用于群消息，group 不能为 -1"));
            return;
        }
        // todo: 使用数据库
        set(getFile(Dir.SETTINGS, "funcState", group + ".txt"), func.getName(), open ? OPEN : CLOSE);
    }

    /**
     * 开启/关闭指定 Log功能.
     *
     * @param func 功能
     * @param open 开启还是关闭功能
     */
    public static void setFuncState(LogFunc func, boolean open) {
        // todo: 使用数据库
        set(getFile(Dir.SETTINGS, "logState.txt"), func.getName(), open ? OPEN : CLOSE);
    }


    /* -- 文件读写锁 -- */

    /**
     * 读写锁，通过 {@link Main#saveInstance(Object)} 获得初值.
     *
     * @see #getLock(File)
     * @see #getString(File, Object)
     * @see #set(File, Object, Object)
     */
    static Map<File, ReentrantReadWriteLock> fileLockMap = new ConcurrentHashMap<>(16);

    /**
     * 获取指定文件对应的锁.
     * <p>
     * 如果 {@link #fileLockMap} 不含所需的锁，则新增锁并返回；否则直接返回对应锁。
     *
     * @param file 要获取锁的文件
     * @return 获取成功返回锁，否则返回 null
     */
    private static ReentrantReadWriteLock getLock(File file) {
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            logError(e);
            return null;
        }
        ReentrantReadWriteLock lock;
        try {
            if (fileLockMap.containsKey(file)) {
                lock = fileLockMap.get(file);
            } else {
                lock = new ReentrantReadWriteLock();
                fileLockMap.put(file, lock);
            }
            return lock;
        } catch (Exception e) {
            logError(e);
            return null;
        }
    }

    private static void removeLock(File file) {
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            logError(e);
            return;
        }
        ReentrantReadWriteLock lock = getLock(file);
        if (lock == null) {
            return;
        }
        fileLockMap.remove(file, lock);
    }

    public static void lock(File... files) {
        if (files == null || files.length == 0) {
            logError(null, new IllegalArgumentException("files 为 null 或长度为 0"));
            return;
        }
        List<ReentrantReadWriteLock> locks = new ArrayList<>();
        List<Boolean> showInfos = new ArrayList<>();
        for (File file : files) {
            ReentrantReadWriteLock lock = getLock(file);
            if (lock == null) {
                // 如果获取不到锁，则直接终止，且 getLock 已经输出错误信息
                return;
            }
            locks.add(lock);
            showInfos.add(!file.getPath().contains(Dir.SETTINGS.getFile().getPath())
                    && !file.getPath().contains(Dir.DATABASE.getFile().getPath())
                    && !file.getPath().contains(Dir.LOG.getFile().getPath()));
        }
        for (int i = 0; i < locks.size(); i++) {
            boolean showInfo = DEBUG && showInfos.get(i);
            if (showInfo) {
                logInfo("开始锁定 " + files[i].getPath());
            }
            try {
                boolean lockSuccess = locks.get(i).writeLock().tryLock(10, TimeUnit.SECONDS);
                if (lockSuccess && showInfo) {
                    logInfo("锁定成功 " + files[i].getPath());
                } else if (!lockSuccess) {
                    logWarn(new UnexpectedStateException("锁定失败，重新尝试 " + files[i].getPath()));
                    removeLock(files[i]);
                    if (locks.get(i).writeLock().tryLock(10, TimeUnit.SECONDS)) {
                        logInfo("锁定成功 " + files[i].getPath());
                    } else {
                        logError(new UnexpectedStateException("锁定失败 " + files[i].getPath()));
                        // 自动创建重置文件
                        createFileIfNotExists(getResetInstanceFile());
                    }
                }
            } catch (InterruptedException e) {
                logError("锁定错误 " + files[i].getPath(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 解锁指定的文件.
     * <p>
     * 如果目标文件为null，则忽略
     *
     * @param files 要解锁的文件
     */
    public static void unlock(File... files) {
        if (files == null || files.length == 0) {
            logError(null, new IllegalArgumentException("files 为 null 或长度为 0"));
            return;
        }
        List<ReentrantReadWriteLock> locks = new ArrayList<>();
        List<Boolean> showInfos = new ArrayList<>();
        for (File file : files) {
            ReentrantReadWriteLock lock = getLock(file);
            if (lock == null) {
                // 如果获取不到锁，则直接终止，且 getLock 已经输出错误信息
                return;
            }
            locks.add(lock);
            showInfos.add(!file.getPath().contains(Dir.SETTINGS.getFile().getPath())
                    && !file.getPath().contains(Dir.DATABASE.getFile().getPath())
                    && !file.getPath().contains(Dir.LOG.getFile().getPath()));
        }
        for (int i = 0; i < locks.size(); i++) {
            boolean showInfo = DEBUG && showInfos.get(i);
            if (showInfo) {
                logInfo("开始解锁 " + files[i].getPath());
            }
            if (locks.get(i).writeLock().isHeldByCurrentThread()) {
                locks.get(i).writeLock().unlock();
            }
            if (showInfo) {
                logInfo("解锁完成 " + files[i].getPath());
            }
        }
    }


    /* -- 文件读写键值对 -- */

    /**
     * 文件不存在，或文件中不存在键时，将返回默认值.
     */
    public static final String DEF_STRING = "-2147483648";
    public static final int DEF_INT = Integer.parseInt(DEF_STRING);
    public static final long DEF_LONG = Long.parseLong(DEF_STRING);
    public static final double DEF_DOUBLE = Double.parseDouble(DEF_STRING);
    /**
     * 无法从文件中读取键值对，或值的类型不匹配时，将返回错误值.
     */
    public static final String ERR_STRING = "-2147483647";
    public static final int ERR_INT = Integer.parseInt(ERR_STRING);
    public static final long ERR_LONG = Long.parseLong(ERR_STRING);
    public static final double ERR_DOUBLE = Double.parseDouble(ERR_STRING);

    /**
     * 以 UTF_8 编码读取一个字符串.
     * <ul>
     * <li>文件存在，且获取到 {@code key} 对应的值，返回该值
     * <li>文件不存在，或文件存在但无 {@code key}，返回 {@link #DEF_STRING}
     * <li>{@code key} 类型出错返回 {@link #ERR_STRING}
     * </ul>
     *
     * @param file 路径
     * @param key  关键字
     * @return 获取到的字符串
     */
    public static String getString(File file, Object key) {
        if (!(key instanceof String) && !(key instanceof Integer) && !(key instanceof Long)) {
            logError(new IllegalArgumentException("key 必须继承于 String/Integer/Long\npath：" + file));
            return ERR_STRING;
        }
        if (!file.exists()) {
            return DEF_STRING;
        }
        if (file.isDirectory()) {
            logError(new Exception("已存在文件夹\npath：" + file + "\nkey：" + key));
            return ERR_STRING;
        }
        lock(file);
        Properties properties = new Properties();
        try (InputStreamReader input = new InputStreamReader
                (new FileInputStream(file), StandardCharsets.UTF_8)) {
            properties.load(input);
            String ret = properties.getProperty(key + "");
            if (ret == null) {
                ret = DEF_STRING;
            }
            return ret;
        } catch (IOException e) {
            logError("path：" + file + "\nkey：" + key, e);
            return ERR_STRING;
        } finally {
            unlock(file);
        }
    }

    public static int getInt(File file, Object key) {
        try {
            return Integer.parseInt(getString(file, key));
        } catch (NumberFormatException e) {
            logError(e);
            return ERR_INT;
        }
    }

    public static long getLong(File file, Object key) {
        try {
            return Long.parseLong(getString(file, key));
        } catch (NumberFormatException e) {
            logError(e);
            return ERR_LONG;
        }
    }

    public static double getDouble(File file, Object key) {
        try {
            return Double.parseDouble(getString(file, key));
        } catch (NumberFormatException e) {
            logError(e);
            return ERR_DOUBLE;
        }
    }

    /**
     * 以 UTF_8 编码写入一个字符串.
     *
     * @param file 路径
     * @param key  关键字
     * @param val  要写入的值
     */
    public static void set(File file, Object key, Object val) {
        if (!(key instanceof String) && !(key instanceof Integer) && !(key instanceof Long)) {
            logError("写入键值对错误", new Exception(
                    "关键字只能是String、Integer或Long\n" + file));
            return;
        }
        if (!(val instanceof String) && !(val instanceof Integer)
                && !(val instanceof Long) && !(val instanceof Double)) {
            logError("写入键值对错误", new Exception(
                    "值只能是String、Integer、Long或Double\n" + file));
            return;
        }
        if (!createFileIfNotExists(file)) {
            logError(new UnexpectedStateException("文件创建失败\n" +
                    "path：" + file + "\nkey：" + key + "\nval：" + val));
            return;
        }
        if (file.isDirectory()) {
            logError(new Exception(
                    "已存在文件夹\npath：" + file + "\nkey：" + key + "\nval：" + val));
            return;
        }
        lock(file);
        Properties properties = new Properties();
        try (InputStreamReader input = new InputStreamReader
                (new FileInputStream(file), StandardCharsets.UTF_8)) {
            properties.load(input);
        } catch (IOException e) {
            logError("path：" + file + "\nkey：" + key + "\nval：" + val, e);
            unlock(file);
            return;
        }
        properties.setProperty(key + "", val + "");
        try (OutputStreamWriter output = new OutputStreamWriter
                (new FileOutputStream(file), StandardCharsets.UTF_8)) {
            // 注意，new的时候，由于没有append，所以文件已经被清空
            // 如果跟上面的输入流一起建立，输入流就只能读空文件
            properties.store(output, null);
        } catch (IOException e) {
            logError("path：" + file + "\nkey：" + key + "\nval：" + val, e);
        } finally {
            unlock(file);
        }
    }


    /* -- 序列化与反序列化 -- */

    static List<File> lockFiles = new ArrayList<>();

    /**
     * 将对象序列化为文件.
     * 传入的对象不应为空，请在序列化之前进行检查。
     *
     * @param obj  被序列化的对象
     * @param file 序列化文件的存储位置
     */
    public static void serialize(Object obj, File file) {
        if (obj == null) {
            return;
        }
        lock(file);
        try {
            if (!createFileIfNotExists(file)) {
                logError(new UnexpectedStateException("无法创建文件\n" +
                        "path：" + file));
                return;
            }
            if (file.isDirectory() || !file.canWrite()) {
                logError(new UnexpectedStateException("已存在文件夹或文件无法写入\n" +
                        "path：" + file));
                return;
            }
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(obj);
        } catch (IOException e) {
            logError("path：" + file, e);
        } finally {
            unlock(file);
        }
    }

    /**
     * 将文件反序列化为对象.
     *
     * @param file              被反序列化的文件
     * @param destClazz         目标类的class
     * @param unlockAtMethodEnd 反序列化完成时是否释放锁。
     *                          如果仅读取而不写入，则传入 true；如果有写入的可能，则传入 false。
     * @param <T>               目标类
     * @return 反序列化得到的指定类的实例对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(File file, Class<T> destClazz, boolean unlockAtMethodEnd) {
        if (!file.exists()) {
            return null;
        }
        lock(file);
        if (!unlockAtMethodEnd) {
            lockFiles.add(file);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            Class<?> srcClazz = obj.getClass();
            if (srcClazz.equals(destClazz)) {
                return (T) obj;
            } else {
                logError(new UnexpectedStateException("类型不同，无法反序列化\n" +
                        "文件类型：" + srcClazz + "，目标类型：" + destClazz));
                return null;
            }
        } catch (IOException | ClassNotFoundException e) {
            logError("path：" + file, e);
            return null;
        } finally {
            if (unlockAtMethodEnd) {
                unlock(file);
            }
        }
    }

    public static <T> T deserialize(File file, Class<T> destClazz) {
        return deserialize(file, destClazz, false);
    }

    static void unlockFiles() {
        try {
            for (File f : lockFiles) {
                unlock(f);
            }
        } catch (RuntimeException e) {
            // 通常是 IllegalMonitorStateException，正常情况不会出现
            logError(e);
        }
    }

    /* -- 本机文件操作 -- */

    public static File getFile(Dir dir, String... names) {
        return getFile(dir.getFile(), names);
    }

    public static File getFile(File dir, String... names) {
        try {
            return FileUtils.getFile(dir, names).getCanonicalFile();
        } catch (IOException e) {
            logError(e);
            return null;
        }
    }

    /**
     * 文件不存在时，创建该文件.
     * <p>
     * 文件存在时，直接返回 {@code true}；否则返回是否成功创建该文件。
     *
     * @param file 目标文件
     * @return 文件是否已存在
     */
    public static boolean createFileIfNotExists(File file) {
        if (file.isFile()) {
            return true;
        }
        return forceCreateNewFile(file);
    }

    /**
     * 强制创建新文件.
     * <p>
     * 如果文件已存在，则删除后创建新文件；否则直接创建新文件。
     * <p>
     * 仅当新文件创建成功时返回 {@code true}。
     *
     * @param file 目标文件
     * @return 文件是否已存在
     */
    public static boolean forceCreateNewFile(File file) {
        if (file.isDirectory()) {
            logError(new UnexpectedStateException("已存在文件夹 " + file.getPath() + "，无法创建文件"));
            return false;
        }
        deleteIfExists(file);
        try {
            FileUtils.forceMkdirParent(file);
            if (file.createNewFile()) {
                logInfo("文件 " + file.getCanonicalPath() + " 创建成功");
                return true;
            } else {
                logError(new IOException("文件 " + file.getCanonicalPath() + " 创建失败"));
                return false;
            }
        } catch (NullPointerException | IOException e) {
            logError("文件 " + file.getAbsolutePath() + " 创建失败", e);
            return false;
        }
    }

    public static boolean mkdir(File dir) {
        if (dir.isDirectory()) {
            return true;
        }
        try {
            FileUtils.forceMkdir(dir);
            logInfo("文件夹 " + dir.getCanonicalPath() + " 创建成功");
            return true;
        } catch (NullPointerException | IOException e) {
            logError("文件夹 " + dir.getAbsolutePath() + " 创建失败", e);
            return false;
        }
    }

    /**
     * 移动原始文件至目标文件（目标文件不能存在）.
     *
     * @param srcFile  原始文件
     * @param destFile 目标文件
     * @return 移动结果
     */
    public static boolean moveFile(File srcFile, File destFile) {
        try {
            srcFile = srcFile.getCanonicalFile();
            destFile = destFile.getCanonicalFile();
            FileUtils.moveFile(srcFile, destFile);
            return true;
        } catch (IOException e) {
            logError(e);
            return false;
        }
    }

    public static boolean moveFile(String srcFilePath, String destFilePath) {
        return moveFile(new File(srcFilePath), new File(destFilePath));
    }

    /**
     * 移动原始文件/文件夹至目标文件夹或目标文件夹内.
     * <p>
     * {@code moveInside} 为 {@code true} 时，表示移动原始文件/文件夹至目标文件夹内部；
     * 否则表示移动原始文件夹至目标文件夹（目标文件夹不能存在）。
     *
     * @param src        原始文件/文件夹
     * @param destDir    目标文件夹
     * @param moveInside 是否移动至文件夹内部
     * @return 移动结果
     */
    public static boolean moveToDir(File src, File destDir, boolean moveInside) {
        try {
            src = src.getCanonicalFile();
            destDir = destDir.getCanonicalFile();
            if (moveInside) {
                FileUtils.moveToDirectory(src, destDir, true);
            } else {
                FileUtils.moveDirectory(src, destDir);
            }
            return true;
        } catch (IOException e) {
            logError(e);
            return false;
        }
    }

    public static boolean moveToDir(String src, String destDir, boolean moveInside) {
        return moveToDir(new File(src), new File(destDir), moveInside);
    }

    public static boolean copy() {

        //FileUtils.copyDirectory(srcdir, destdir, filter);
        return true;
    }

    /**
     * 重命名文件或文件夹.
     *
     * @param fileOrDir 原文件/文件夹
     * @param newName   新文件名/文件夹名，后缀可加可不加
     * @return 重命名成功返回true，否则返回false
     */
    public static boolean rename(File fileOrDir, String newName) {
        File parent = fileOrDir.getParentFile();
        if (!fileOrDir.isDirectory()) {
            String sourceName = fileOrDir.getName();
            if (sourceName.contains(".") && !newName.contains(".")) {
                newName = newName + "." + sourceName.substring(sourceName.lastIndexOf('.'));
            }
        }
        return fileOrDir.renameTo(new File(parent, newName));
    }

    public static boolean rename(String filePath, String newName) {
        return rename(new File(filePath), newName);
    }

    /**
     * 删除文件或文件夹.
     *
     * @param fileOrDir 想删除的文件/文件夹
     * @return 文件不存在，或文件已被删除时返回true，否则返回false
     */
    public static boolean deleteIfExists(File fileOrDir) {
        if (!fileOrDir.exists()) {
            return true;
        }
        try {
            if (fileOrDir.isDirectory()) {
                FileUtils.deleteDirectory(fileOrDir.getCanonicalFile());
            } else {
                FileUtils.forceDelete(fileOrDir.getCanonicalFile());
            }
            return true;
        } catch (FileNotFoundException e) {
            logWarn(e);
            return true;
        } catch (IOException e) {
            logError(e);
            return false;
        }
    }

    public static boolean deleteIfExists(String filePath) {
        return deleteIfExists(new File(filePath));
    }


    /* -- 随机数、随机长度字符串等 -- */

    private static final Random RANDOM = new Random();

    /**
     * 返回一个均匀分布的随机整数（包括上下限）.
     *
     * @param min 下限
     * @param max 上限
     * @return 随机整数
     */
    public static int getRandomInt(int min, int max) {
        if (min > max) {
            logWarn(new Exception("随机数上下限颠倒：min " + min + ", max " + max));
            min ^= max;
            max ^= min;
            min ^= max;
        }
        // random.nextInt(a)随机生成[0,a)的随机数
        return RANDOM.nextInt(max - min + 1) + min;
    }

    /**
     * 返回一个均匀分布的随机小数（包括上限，不包括下限）.
     *
     * @param min 下限
     * @param max 上限
     * @return 随机小数
     */
    public static double getRandomDouble(double min, double max) {
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }
        // random.nextDouble()随机生成[0,1)的随机数
        return RANDOM.nextDouble() * (max - min) + min;
    }

    public static final double TWENTY_PERCENT = 0.84;
    public static final double TEN_PERCENT = 1.28;
    public static final double FIVE_PERCENT = 1.64;
    public static final double THREE_PERCENT = 1.88;
    public static final double TWO_PERCENT = 2.05;
    public static final double ONE_PERCENT = 2.32;
    public static final double FIVE_PER_THOUSAND = 2.57;
    public static final double THREE_PER_THOUSAND = 2.75;
    public static final double TWO_PER_THOUSAND = 2.88;
    public static final double ONE_PER_THOUSAND = 3.08;

    public static double getDistributionDouble(double x, double miu, double sigma) {
        return 1.0 / (Math.sqrt(2.0 * Math.PI) * sigma) *
                Math.pow(Math.E, -Math.pow(x - miu, 2.0) / (2.0 * Math.pow(sigma, 2.0)));
    }

    public static double getNormalDistributionDouble(double x) {
        return getDistributionDouble(x, 0, 1);
    }

    /**
     * 返回一个正态分布的随机小数（包括上下限）.
     *
     * @param min      下限
     * @param max      上限
     * @param coverage 以标准正态分布表为准，确定正态分布有效范围
     *                 传入Mx.ONE_PERCENT等数值，或者自定义数值（必须为正）
     *                 比如，查表得2.32为0.9898，2.33为0.9901，
     *                 则传入2.32代表取得上限概率约为1%，取得下限概率约为1%
     * @return 随机小数
     */
    public static double getRandomDistributionDouble(double min, double max, double coverage) {
        if (coverage <= 0) {
            return (max + min) / 2;
        }
        double a = RANDOM.nextGaussian();
        if (a > coverage) {
            a = coverage;
        } else if (a < -coverage) {
            a = -coverage;
        }
        a = a / (coverage * 2) + 0.5;// [0,1]
        return (max - min) * a + min;
    }

    public static double getRandomDistributionDouble(double min, double max) {
        return getRandomDistributionDouble(min, max, ONE_PERCENT);
    }

    /**
     * 返回一个正态分布的随机整数（包括上下限）.
     * 利用正态分布小数，扩充范围至[min,max+1)
     *
     * @param min      下限
     * @param max      上限
     * @param coverage 以标准正态分布表为准，确定正态分布有效范围
     *                 传入Mx.ONE_PERCENT等数值，或者自定义数值（必须为正）
     *                 比如，查表得2.32为0.9898，2.33为0.9901，
     *                 则传入2.32代表取得上限概率约为1%，取得下限概率约为1%
     * @return 随机小数
     */
    public static int getRandomDistributionInt(int min, int max, double coverage) {
        double num = getRandomDistributionDouble(min, max + 1.0, coverage);
        return num >= max + 1.0 ? max : (int) num;
    }

    public static int getRandomDistributionInt(int min, int max) {
        return getRandomDistributionInt(min, max, ONE_PERCENT);
    }

    /**
     * 返回一个随机汉字.
     *
     * @return 随机中文字符
     */
    public static char getRandomChineseChar() {
        String str = "";
        byte[] b = new byte[2];
        b[0] = (byte) (176 + Math.abs(RANDOM.nextInt(39)));// 高位
        b[1] = (byte) (161 + Math.abs(RANDOM.nextInt(93)));// 低位
        try {
            str = new String(b, "GBK");
        } catch (UnsupportedEncodingException e) {
            logError(e);
        }
        return str.charAt(0);
    }

    /**
     * 返回指定长度随机中文字符串.
     *
     * @return 指定长度随机中文字符串
     */
    public static String getRandomChineseStr(int len) {
        if (len <= 0) {
            logError(new Exception("错误的长度：" + len));
            return "";
        }
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < len; i++) {
            name.append(getRandomChineseChar());
        }
        return name.toString();
    }

    /**
     * 返回随机长度随机中文字符串.
     *
     * @return 指定长度随机中文字符串
     */
    public static String getRandomChineseStr(int lenMin, int lenMax) {
        if (lenMin <= 0) {
            logError(new Exception("错误的长度：" + lenMin));
            return "";
        }
        if (lenMax <= 0) {
            logError(new Exception("错误的长度：" + lenMax));
            return "";
        }
        return getRandomChineseStr(getRandomInt(lenMin, lenMax));
    }


    /* -- 从网站获取字符串 -- */

    /**
     * 从网站获取字符串，通常是 json 格式数据.
     * <p>
     * 未指定ua时，使用默认ua以解除部分网站对java访问的403限制。
     *
     * @param httpUrl      要获取信息的网站
     * @param urlParams    网址附加参数
     * @param headerParams 请求头附加参数
     * @return 反馈的字符串
     */
    public static String getInfoFromUrl(String httpUrl, Map<String, String> urlParams, Map<String, String> headerParams) {
        String uri = httpUrl;
        try {
            StringBuilder urlSb = new StringBuilder(httpUrl);
            if (urlParams != null) {
                boolean firstParam = true;
                for (Map.Entry<String, String> x : urlParams.entrySet()) {
                    urlSb.append(firstParam ? "?" : "&").append(x.getKey()).append("=")
                            .append(URLEncoder.encode(x.getValue(), "UTF-8"));
                    if (firstParam) {
                        firstParam = false;
                    }
                }
            }
            uri = urlSb.toString();
            logInfo("Http Get Start\n" + uri);
            HttpURLConnection conn = (HttpURLConnection) new URL(uri).openConnection();
            if (headerParams == null) {
                headerParams = new HashMap<>();
            }
            if (!headerParams.containsKey("User-Agent")) {
                // 该ua可以解除部分网站对java访问的403限制
                headerParams.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            }
            for (Map.Entry<String, String> x : headerParams.entrySet()) {
                conn.setRequestProperty(x.getKey(), URLEncoder.encode(x.getValue(), "UTF-8"));
            }
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.connect();
            if (conn.getResponseCode() != 200) {
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line);
            }
            br.close();
            conn.disconnect();
            String getStr = result.toString();
            try {
                //反馈数据格式为JSONObject，格式化后显示
                JSONObject obj = new JSONObject(getStr);
                logInfo("Http Get Success\n" + uri + "\n" + obj.toString(2));
            } catch (JSONException e) {
                try {
                    //反馈数据格式为JSONArray，格式化后显示
                    JSONArray arr = new JSONArray(getStr);
                    logInfo("Http Get Success\n" + uri + "\n" + arr.toString(2));
                } catch (JSONException e1) {
                    //反馈数据格式既不是JSONObject也不是JSONArray，直接显示
                    logInfo("Http Get Success\n" + uri + "\n" + getStr);
                }
            }
            return getStr;
        } catch (IOException e) {
            logInfo("Http Get Failed\n" + uri);
            logWarn(e);
            return ERR_STRING;
        }
    }


    /* -- 时间相关 -- */

    /**
     * 时间戳格式化为日期字符串.
     *
     * @param timestamp 通常是System.currentTimeMillis()
     * @param format    格式标准
     * @return 指定格式的字符串
     */
    public static String timestampToStr(long timestamp, String format) {
        return new SimpleDateFormat(format, Locale.CHINA).format(new Date(timestamp));
    }

    /**
     * 日期实例格式化为日期字符串.
     *
     * @param date   日期实例
     * @param format 格式标准
     * @return 指定格式的字符串
     */
    public static String timestampToStr(Date date, String format) {
        return new SimpleDateFormat(format, Locale.CHINA).format(date);
    }

    /**
     * 时间戳格式化为完整时间字符串.
     *
     * @param timestamp 通常是System.currentTimeMillis()
     * @return 格式为"2020-01-01 00:00:00"的完整时间字符串
     */
    public static String getFullTimeStr(long timestamp) {
        return timestampToStr(timestamp, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 时间戳格式化为年月日字符串.
     *
     * @param timestamp 通常是System.currentTimeMillis()
     * @return 格式为"2020-01-01"的年月日字符串
     */
    public static String getDateStr(long timestamp) {
        return timestampToStr(timestamp, "yyyy-MM-dd");
    }

    /**
     * 时间戳格式化为时分秒字符串.
     *
     * @param timestamp 通常是System.currentTimeMillis()
     * @return 格式为"00:00:00"的时分秒字符串
     */
    public static String getTimeStr(long timestamp) {
        return timestampToStr(timestamp, "HH:mm:ss");
    }

    /**
     * 返回当天零点时间戳.
     *
     * @param timestamp 任意时间戳
     * @return 当天零点时间戳
     */
    public static long getZeroHourTimestamp(long timestamp) {
        return timestamp - (timestamp + TimeZone.getDefault().getRawOffset()) % (24 * 60 * 60 * 1000);
    }

    /**
     * 返回当前小时对应整点的时间戳.
     *
     * @param timestamp 任意时间戳
     * @return 当前小时对应整点的时间戳.
     */
    public static long getThisHourTimestamp(long timestamp) {
        return timestamp - timestamp % (60 * 60 * 1000);
    }

    /**
     * 返回下一小时对应整点的时间戳.
     *
     * @param timestamp 任意时间戳
     * @return 下一小时对应整点的时间戳.
     */
    public static long getNextHourTimestamp(long timestamp) {
        return getThisHourTimestamp(timestamp) + 60 * 60 * 1000;
    }


    /**
     * 返回本周一日期.
     *
     * @param timestamp 通常是System.currentTimeMillis()
     * @return 格式为"2020-01-01"的本周一日期.
     */
    public static String getThisMonday(long timestamp) {
        Calendar calendar = new GregorianCalendar(Locale.CHINA);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setTime(new Date(timestamp));
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
                .format(calendar.getTime());
    }

    /**
     * 返回下周一日期.
     *
     * @param timestamp 通常是System.currentTimeMillis()
     * @return 格式为"2020-01-01"的下周一日期.
     */
    public static String getNextMonday(long timestamp) {
        return getThisMonday(timestamp + 7 * 24 * 60 * 60 * 1000);
    }

    public static final String LESS_THAN_ONE_MINUTE = "小于1分钟";

    /**
     * 将以秒 s 为单位的时间差改成字符串，可选是否展示秒.
     * 如果某部分数字为0，则不会显示该部分。比如传入60，输出为"1分"。
     *
     * @param second     要转换的时间长度，单位s
     * @param showSecond 是否展示秒
     * @return 对应的格式化时间字符串
     */
    public static String secondToStr(int second, boolean showSecond) {
        if (second < 0) {
            logWarn(new Exception("时间差" + second + "应该为正"));
            second = -second;
        }
        if (second == 0) {
            return "0秒";
        }
        if (!showSecond && second < 60) {
            return LESS_THAN_ONE_MINUTE;
        }
        String str = "";
        if (second >= 86400) {
            str = str + (second / 86400) + "天";
            second %= 86400;
        }
        if (second >= 3600) {
            str = str + (second / 3600) + "时";
            second %= 3600;
        }
        if (second >= 60) {
            str = str + (second / 60) + "分";
            second %= 60;
        }
        if (showSecond && second != 0) {
            str = str + second + "秒";
        }
        return str;
    }

    /**
     * 将以毫秒 ms 为单位的时间差改成字符串，可选是否展示秒.
     *
     * @param beginTime  时间开始，单位ms
     * @param endTime    时间截止，单位ms
     * @param showSecond 是否展示秒
     * @return 对应的格式化时间字符串
     */
    public static String milliSecondToStr(long beginTime, long endTime, boolean showSecond) {
        return secondToStr((int) (Math.abs(endTime - beginTime) / 1000), showSecond);
    }

    /**
     * 将以纳秒 ns 为单位的时间差改成字符串，可选是否展示秒.
     *
     * @param beginTime  时间开始，单位ns
     * @param endTime    时间截止，单位ns
     * @param showSecond 是否展示秒
     * @return 对应的格式化时间字符串
     */
    public static String nanoSecondToStr(long beginTime, long endTime, boolean showSecond) {
        return secondToStr((int) (Math.abs(endTime - beginTime) / 1e9), showSecond);
    }


    /* -- 杂项 -- */

    /**
     * 返回斐波那契数列的第 index 项的值.
     *
     * @param index 下标，从1开始
     * @return 斐波那契数列对应下标的值
     */
    public static int getFibonacci(int index) {
        if (index <= 0) {
            logError(new Exception("错误的项数：" + index));
            return -1;
        }
        if (index == 1 || index == 2) {
            return 1;
        } else {
            return getFibonacci(index - 1) + getFibonacci(index - 2);
        }
    }

    /**
     * 计算一个字符串的 MD5.
     *
     * @param s 要计算 MD5 的字符串
     * @return 字符串对应的 MD5
     */
    public static String getMd5(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(s.getBytes());
            byte[] byteArr = md5.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : byteArr) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            logError(e);
        }
        return null;
    }

    /**
     * 转义正则特殊字符 "$()*+.[]?\^{},|".
     *
     * @param s 要转义的字符串
     * @return 正则表达式
     */
    public static String strToRegex(String s) {
        if (s != null && !s.equals("")) {
            String[] fbsArr = {"\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|"};
            for (String key : fbsArr) {
                if (s.contains(key)) {
                    s = s.replace(key, "\\" + key);
                }
            }
        }
        return s;
    }

    /**
     * 标准化字符串.
     *
     * @param s 要标准化的字符串
     * @return 标准化后的字符串
     */
    @Deprecated
    public static String strFormat(String s) {
        if (s != null && !s.equals("")) {
            s = s.replace("！", "!")
                    .replace("【", "[")
                    .replace("】", "]")
                    .replace("，", ",")
                    .replace("。", ".")
                    .replace("？", "?")
                    .replace(" ", "")
                    .replace("\\n", "")
                    .replace("\\\\", "")
                    .replace("/", "")
                    .replace(":", "")
                    .replace("\\*", "")
                    .replace("\\?", "")
                    .replace("\"", "")
                    .replace("<", "")
                    .replace(">", "")
                    .replace("\\|", "");
        }
        return s;
    }

    public static <T> List<T> castObjToList(Object obj, Class<T> clazzT) {
        List<T> list = new ArrayList<>();
        if (!(obj instanceof List<?>)) {
            return list;
        }
        try {
            for (Object o : (List<?>) obj) {
                list.add(clazzT.cast(o));
            }
            return list;
        } catch (RuntimeException e) {
            logError(e);
            return new ArrayList<>();
        }
    }

    public static <K, V> Map<K, V> castObjToMap(Object obj, Class<K> clazzK, Class<V> clazzV) {
        Map<K, V> map = new HashMap<>();
        if (!(obj instanceof Map<?, ?>)) {
            //logInfo("!(obj instanceof Map<?, ?>)");
            return map;
        }
        try {
            //logInfo("Map start, size = " + map.size());
            for (Map.Entry<?, ?> p : ((Map<?, ?>) obj).entrySet()) {
                map.put(clazzK.cast(p.getKey()), clazzV.cast(p.getValue()));
                //logInfo("p.getKey(): " + p.getKey() + "\np.getValue(): " + p.getValue());
            }
            //logInfo("Map end, size = " + map.size());
            return map;
        } catch (RuntimeException e) {
            logError(e);
            return new HashMap<>();
        }
    }

    /**
     * utf-8 转 unicode.
     *
     * @param utf8Str 要转换的 utf-8 字符串
     * @return 转换完毕的 unicode 字符串
     */
    public static String utf8ToUnicode(String utf8Str) {
        char[] myBuffer = utf8Str.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < utf8Str.length(); i++) {
            UnicodeBlock ub = UnicodeBlock.of(myBuffer[i]);
            if (ub == UnicodeBlock.BASIC_LATIN) {
                //英文及数字等
                sb.append(myBuffer[i]);
            } else if (ub == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
                //全角半角字符
                int j = (int) myBuffer[i] - 65248;
                sb.append((char) j);
            } else {
                //汉字
                short s = (short) myBuffer[i];
                String hexS = Integer.toHexString(s);
                String unicode = "\\u" + hexS;
                sb.append(unicode.toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * unicode 转 utf-8.
     *
     * @param unicodeStr 要转换的 unicode 字符串
     * @return 转换完毕的 utf-8 字符串
     */
    public static String unicodeToUtf8(String unicodeStr) {
        char c;
        int len = unicodeStr.length();
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (index < len) {
            c = unicodeStr.charAt(index++);
            if (c == '\\') {
                c = unicodeStr.charAt(index++);
                if (c == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        c = unicodeStr.charAt(index++);
                        if (c >= '0' && c <= '9') {
                            value = (value << 4) + c - '0';
                        } else if (c >= 'a' && c <= 'f') {
                            value = (value << 4) + 10 + c - 'a';
                        } else if (c >= 'A' && c <= 'F') {
                            value = (value << 4) + 10 + c - 'A';
                        } else {
                            throw new IllegalArgumentException("不符合\\uxxxx的格式");
                        }
                    }
                    sb.append((char) value);
                } else {
                    if (c == 't') {
                        c = '\t';
                    } else if (c == 'r') {
                        c = '\r';
                    } else if (c == 'n') {
                        c = '\n';
                    } else if (c == 'f') {
                        c = '\f';
                    }
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
