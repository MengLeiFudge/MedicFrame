package medic.core;

import java.util.ArrayList;
import java.util.List;

import static medic.core.Api.group;
import static medic.core.Api.textMsg;
import static medic.core.Main.Func;
import static medic.core.Utils.DEBUG;
import static medic.core.Utils.getFuncState;
import static medic.core.Utils.logError;
import static medic.core.Utils.logInfo;

/**
 * {@link Main.Func} 中非系统功能都继承于该类.
 * <p>
 * 注意，子类构造函数必须为 {@code public}，否则构造器无法被访问.
 *
 * @author MengLeiFudge
 * @date 20210412
 */
public abstract class FuncProcess implements IProcess {
    protected final Func func;

    protected final List<String> menuList = new ArrayList<>();

    /**
     * 用于指示所有功能中任意一个词条是否匹配成功.
     */
    private static boolean processed = false;

    static void setProcessed() {
        processed = true;
    }

    static boolean getProcessed() {
        return processed;
    }

    /**
     * 非系统消息构造器.
     *
     * @param func 功能
     */
    protected FuncProcess(Func func) {
        this.func = func;
    }

    /**
     * 判断是否匹配某个功能，匹配则执行该功能.
     */
    void run() {
        try {
            // “菜单11”这样的指令会被视为菜单
            boolean msgIsMenu = textMsg.matches("菜单" + func.getIndex());
            // “养鲲”这样仅含功能名的指令也被视为菜单
            for (String s : menuList) {
                if (textMsg.matches("(?i)(" + s + ")")) {
                    msgIsMenu = true;
                    break;
                }
            }
            boolean isFuncOpen = getFuncState(group, func);
            // 菜单在功能未启用时也可以使用
            if (msgIsMenu) {
                if (DEBUG) {
                    logInfo("匹配【" + func.getName() + "】菜单");
                }
                menu();
                setProcessed();
            } else if (isFuncOpen) {
                if (process()) {
                    if (DEBUG) {
                        logInfo("匹配【" + func.getName() + "】功能");
                    }
                    setProcessed();
                }
            } else if (DEBUG) {
                logInfo("未匹配【" + func.getName() + "】");
            }
        } catch (RuntimeException e) {
            // 诸如数组越界等 RuntimeException，会导致 medic 直接闪退，这里将其捕获
            logError(e);
        }
    }

    /**
     * 菜单.
     */
    public abstract void menu();

    /**
     * 词条匹配.
     * <p>
     * 需要注意的是，一条消息应根据用户权限，<b>至多匹配一个 matches 分支</b>。
     * <p>
     * 因为一条消息对应一个 {@link Api#apiObj}，且会对多个功能进行匹配验证。
     * <p>
     * 若某条消息能匹配多个 matches 分支，且这些分支对应的具体功能
     * 都使用了 {@link Api#addText(String)} 等需要暂存的方法，将极有可能导致发出的消息错乱。
     * <p>
     * 正则表达式部分规则如下，其中标△的较为常用，务必掌握。
     * <p>
     * 常用元字符
     * <p>
     * <ul>
     * <li>.	匹配除换行符以外的任意字符。要匹配 .，请使用 \\.△</li>
     * <li>\	将下一个字符标记为或特殊字符、或原义字符、或向后引用、或八进制转义符；
     *          要匹配 \ 字符，请使用 \\</li>
     * <li>\w	匹配字母或数字或下划线，等价于[A-Za-z0-9_]</li>
     * <li>\s	匹配任意的空白符</li>
     * <li>\d	匹配数字，等价于[0-9]</li>
     * <li>\b	匹配单词的开始或结束</li>
     * <li>^	匹配字符串的开始。要匹配 ^ 字符，请使用 \^</li>
     * <li>$	匹配字符串的结束。要匹配 $ 字符，请使用 \$。</li>
     * </ul>
     * <p>
     * 常用限定符
     * <p>
     * <ul>
     * <li>*	重复零次或更多次。要匹配 * 字符，请使用 \*△</li>
     * <li>+	重复一次或更多次。要匹配 + 字符，请使用 \+。△</li>
     * <li>?	重复零次或一次</li>
     * <li>{n}	重复n次</li>
     * <li>{n,}	重复n次或更多次</li>
     * <li>{n,m}	重复n到m次</li>
     * <li>|	匹配左侧或右侧任意一个。要匹配 |，请使用 \|。△</li>
     * <li>()	标记一个子表达式的开始和结束位置。子表达式可以获取供以后使用。
     *          通常用于优先级的确立。要匹配 (，请使用 \(。△</li>
     * <li>[]	标记一个中括号表达式的开始。通常用于匹配内部任意一个。
     * 			要匹配 [，请使用 \[。△</li>
     * <li>{}	标记限定符表达式的开始。要匹配 {，请使用 \{。</li>
     * <li>(?i) 匹配时忽视大小写。△</li>
     * </ul>
     * <p>
     * 常用反义词
     * <p>
     * <ul>
     * <li>\W	匹配任意不是字母，数字，下划线的字符，等价于[^A-Za-z0-9_]</li>
     * <li>\S	匹配任意不是空白符的字符</li>
     * <li>\D	匹配任意非数字的字符，等价于[^0-9]</li>
     * <li>\B	匹配不是单词开头或结束的位置</li>
     * <li>[^x]	匹配除了x以外的任意字符</li>
     * <li>[^aeiou]	匹配除了aeiou这几个字母以外的任意字符</li>
     * </ul>
     * <p>
     * 示例：
     * <p>
     * <ul>
     * <li>msg.matches("[0-9]")	             匹配任意一个数字</li>
     * <li>msg.matches("[0-9A-Za-z]")	     匹配任意一个数字，或任意一个字母</li>
     * <li>msg.matches("[0-9]*")	         匹配空字符串，或0，或任意正整数</li>
     * <li>msg.matches("[0-9]+")	         匹配0，或任意正整数</li>
     * <li>msg.matches("[0-9]{9}")	         匹配长度为9的任意正整数</li>
     * <li>msg.matches("[Bb]oss")	         匹配Boss，或boss</li>
     * <li>msg.matches("(?i)(boss(属性|))")  匹配boss、Boss属性……</li>
     * </ul>
     *
     * @return 匹配成功返回 true, 否则返回 false.
     */
    public abstract boolean process();

    /**
     * 用于线程休眠.
     *
     * @param milliTime 要休眠的时间，ms为单位
     */
    public static void sleep(long milliTime) {
        Utils.sleep(milliTime);
    }
}
