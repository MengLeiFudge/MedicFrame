package medic.core;

/**
 * {@link Main.Func} 中系统功能都继承于该类.
 * <p>
 * 注意，子类构造函数必须为 {@code public}，否则构造器无法被访问.
 *
 * @author MengLeiFudge
 * @date 20201218
 */
public abstract class SystemProcess implements IProcess {
    protected final long requestId;
    protected final long sQQ;
    protected final String sNick;
    protected final String info;
    protected final String result;

    /**
     * 系统消息构造器.
     *
     * @param requestId 系统消息唯一标识
     * @param sQQ       邀请人/操作者QQ
     * @param sNick     邀请人/操作者昵称
     * @param info      验证消息
     * @param result    处理结果
     */
    protected SystemProcess(long requestId, long sQQ, String sNick, String info, String result) {
        this.requestId = requestId;
        this.sQQ = sQQ;
        this.sNick = sNick;
        this.info = info;
        this.result = result;
    }

    /**
     * 处理系统消息.
     */
    public abstract void process();

    /**
     * 用于线程休眠.
     *
     * @param milliTime 要休眠的时间，ms为单位
     */
    public static void sleep(long milliTime) {
        Utils.sleep(milliTime);
    }
}
