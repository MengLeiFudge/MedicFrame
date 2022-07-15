package medic.func.common.sekai;

import medic.core.FuncProcess;
import medic.core.Main;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.DEF_INT;
import static medic.core.Utils.ERR_STRING;
import static medic.core.Utils.createFileIfNotExists;
import static medic.core.Utils.deleteIfExists;
import static medic.core.Utils.getFullTimeStr;
import static medic.core.Utils.getInt;
import static medic.core.Utils.getStrFromURL;
import static medic.core.Utils.lock;
import static medic.core.Utils.logError;
import static medic.core.Utils.set;
import static medic.core.Utils.unlock;
import static medic.func.common.sekai.SekaiUtils.getAliveDir;
import static medic.func.common.sekai.SekaiUtils.getAliveFile;
import static medic.func.common.sekai.SekaiUtils.getDataFile;
import static medic.func.common.sekai.SekaiUtils.getLastEventFile;

/**
 * @author MengLeiFudge
 */
public class Process extends FuncProcess {
    public Process(Main.Func func) {
        super(func);
        lastEvent = getInt(getLastEventFile(), "lastEvent");
        if (lastEvent == DEF_INT) {
            lastEvent = 1;
            set(getLastEventFile(), "lastEvent", 1);
        }
        menuList.add("sekai");
        menuList.add("世界计划");
    }

    private int lastEvent;

    @Override
    public void menu() {
        send("标识 [g]群聊可用 [p]私聊可用\n" +
                "说明 [a]仅Bot管理员可用\n" +
                "[g]sekai线：获取当前sekai各名次排名\n" +
                "[g]sekai开始+线程数：开启记录sekai分数的线程\n" +
                "[g]sekai结束/终止/停止：停止记录sekai分数"
        );
    }

    @Override
    public boolean process() {
        if (textMsg.matches("(?i)sekai线")) {
            sekaiRank();
            return true;
        } else if (textMsg.matches("(?i)sekai开始[1-8]")) {
            int threadNum = Integer.parseInt(textMsg.split("\\D+")[1]);
            sekaiSaveData(threadNum);
            return true;
        } else if (textMsg.matches("(?i)sekai(结束|终止|停止)")) {
            sekaiStopSaveData();
            return true;
        }
        return false;
    }

    private static final int[] RANK_ALL = {
            100, 200, 300, 400, 500,
            1000, 2000, 3000, 4000, 5000,
            10000, 20000, 30000, 40000, 50000,
            100000
    };

    private void sekaiRank() {
        try {
            JSONObject obj = getLastEvent();
            if (obj == null) {
                send(qq, "当前数据不可用，请稍后再试！");
                return;
            }
            long time = obj.getLong("time");
            StringBuilder sb = new StringBuilder("sekai线 " + getFullTimeStr(time));
            for (int i : RANK_ALL) {
                String thisRank = "rank" + i;
                int score = obj.getJSONArray(thisRank).getJSONObject(0).getInt("score");
                sb.append("\n").append(thisRank).append(" = ").append(score);
            }
            send(sb.toString());
        } catch (JSONException e) {
            logError(e);
        }
    }

    static String pre = "https://bitbucket.org/sekai-world/sekai-event-track/raw/main/event";

    /**
     * 获取json数据.
     *
     * @return 成功获取到数据且数据含有time时，返回最新的数据；否则返回null
     */
    private JSONObject getLastEvent() {
        try {
            String json = getStrFromURL(pre + lastEvent + ".json", null);
            if (ERR_STRING.equals(json)) {
                return null;
            }
            JSONObject obj = new JSONObject(json);
            if (!obj.has("time")) {
                return null;
            }
            long time = obj.getLong("time");
            // 当前json时间差距小于300s，直接使用该数据
            if (System.currentTimeMillis() - time < 300 * 1000) {
                return obj;
            }
            // 时间差距大于300s时，判断下个event是否存在（下个活动开没开）
            json = getStrFromURL(pre + (lastEvent + 1) + ".json", null);
            // 如果下个活动没开
            if (ERR_STRING.equals(json)) {
                return obj;
            }
            // 如果下个活动开了，lastEvent 自增并写入文件
            lastEvent++;
            set(getLastEventFile(), "lastEvent", lastEvent);
            // lastEvent 对应的活动不一定是最新一期活动，递归调用
            return getLastEvent();
        } catch (JSONException e) {
            logError(e);
            return null;
        }
    }

    private static final int[] RANK_ALL2 = {
            20, 30, 40, 50,
            100, 200, 300, 400, 500,
            1000, 2000, 3000, 4000, 5000,
            10000, 20000, 30000, 40000, 50000,
            100000
    };

    private void sekaiSaveData(int threadNum) {
        ExecutorService pool = new ThreadPoolExecutor(
                threadNum, threadNum,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024),
                new ThreadPoolExecutor.AbortPolicy());
        for (int i = 0; i < threadNum; i++) {
            sekaiSaveData0(pool);
            sleep(15000);
        }
        pool.shutdown();
    }

    private void sekaiSaveData0(ExecutorService pool) {
        pool.execute(() -> {
            long startTime = System.currentTimeMillis() / 1000;
            File aliveFile = getAliveFile(startTime);
            createFileIfNotExists(aliveFile);
            send("线程" + startTime + "开始记录数据！");
            while (aliveFile.exists()) {
                try {
                    JSONObject obj = null;
                    for (int i = 0; i < 3 && obj == null; i++) {
                        obj = getLastEvent();
                    }
                    if (obj == null) {
                        send("连续3次获取数据失败！");
                        break;
                    }
                    long time = obj.getLong("time");
                    if (System.currentTimeMillis() - time > 300 * 1000) {
                        send("数据时间距今超过5分钟！");
                        break;
                    }
                    String timeStr = getFullTimeStr(time);
                    File dataFile = getDataFile(lastEvent, timeStr);
                    lock(dataFile);
                    try {
                        if (dataFile.exists()) {
                            sleep(30000);
                            continue;
                        }
                        createFileIfNotExists(dataFile);
                        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataFile))) {
                            bw.write(time + "");
                            bw.newLine();
                            bw.write(timeStr);
                            StringBuilder sb = new StringBuilder();
                            int score = obj.getJSONArray("first10").getJSONObject(9).getInt("score");
                            sb.append("\n").append("rank10").append(" = ").append(score);
                            for (int i : RANK_ALL2) {
                                String thisRank = "rank" + i;
                                score = obj.getJSONArray(thisRank).getJSONObject(0).getInt("score");
                                sb.append("\n").append(thisRank).append(" = ").append(score);
                            }
                            bw.write(sb.toString());
                            bw.newLine();
                        } catch (IOException e) {
                            logError(e);
                            break;
                        }
                    } finally {
                        unlock(dataFile);
                    }
                } catch (JSONException e) {
                    logError(e);
                    break;
                }
            }
            send("线程池" + startTime + "已结束！");
            deleteIfExists(aliveFile);
        });
    }

    private void sekaiStopSaveData() {
        deleteIfExists(getAliveDir());
        send("已终止全部线程！");
    }

}
