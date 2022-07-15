package medic.func.common.alarm;

import medic.core.FuncProcess;
import medic.core.Main;
import medic.core.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static medic.core.Api.MsgSource;
import static medic.core.Api.changeGroupToOtherGroup;
import static medic.core.Api.getGroups;
import static medic.core.Api.msgSource;
import static medic.core.Api.send;
import static medic.core.Main.Func;
import static medic.core.Utils.Dir;
import static medic.core.Utils.createFileIfNotExists;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getFuncState;
import static medic.core.Utils.getNextHourTimestamp;
import static medic.core.Utils.lock;
import static medic.core.Utils.timestampToStr;
import static medic.core.Utils.unlock;

/**
 * @author MengLeiFudge
 */
public class OnTimeAlarm extends FuncProcess {
    public OnTimeAlarm(Main.Func func) {
        super(func);
    }

    @Override
    public void menu() {
    }

    @Override
    public boolean process() {
        if (msgSource != MsgSource.GROUP) {
            return false;
        }
        long time = System.currentTimeMillis();
        long nextHour = getNextHourTimestamp(time);
        if (nextHour - time < 10 * 60 * 1000) {
            // 10分钟
            String nowTime = timestampToStr(nextHour, "MM月dd日HH时");
            File f = getFile(Dir.DATA, "整点报时", nowTime + ".txt");
            if (f.exists()) {
                return false;
            }
            lock(f);
            if (f.exists()) {
                return false;
            }
            createFileIfNotExists(f);
            unlock(f);
            ExecutorService singleThreadPool = new ThreadPoolExecutor(
                    1, 1,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(1024),
                    new ThreadPoolExecutor.AbortPolicy());
            singleThreadPool.execute(() -> {
                long[] groups = getGroups();
                List<Long> list = new ArrayList<>();
                for (long g : groups) {
                    if (getFuncState(g, Func.ON_TIME_ALARM)) {
                        list.add(g);
                    }
                }
                while (nextHour - System.currentTimeMillis() > 60000) {
                    sleep(60000);
                }
                while (nextHour - System.currentTimeMillis() > 10000) {
                    sleep(10000);
                }
                while (nextHour - System.currentTimeMillis() > 0) {
                    sleep(100);
                }
                for (long g : list) {
                    changeGroupToOtherGroup(g);
                    send("当前时间：" + nowTime);
                }
                Utils.deleteIfExists(f);
            });
            singleThreadPool.shutdown();
            while (singleThreadPool.isTerminated()) {
                sleep(10000);
            }
        }
        return false;
    }
}
