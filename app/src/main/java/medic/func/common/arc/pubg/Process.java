package medic.func.common.arc.pubg;

import medic.core.FuncProcess;
import medic.core.Img;
import medic.core.Main;
import medic.func.common.arc.query.User;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;

import static medic.core.Api.addImg;
import static medic.core.Api.addText;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.Dir;
import static medic.core.Utils.ERR_STRING;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getFullTimeStr;
import static medic.core.Utils.logError;
import static medic.func.common.arc.query.ArcUtils.diffFormatStr;
import static medic.func.common.arc.query.ArcUtils.getSongNameByID;
import static medic.func.common.arc.query.ArcUtils.getUser;
import static medic.func.common.arc.query.ArcUtils.getUserBest30;
import static medic.func.common.arc.query.ArcUtils.getUserInfo;
import static medic.func.common.arc.query.ArcUtils.pttToStr;
import static medic.func.common.arc.query.ArcUtils.scoreAndPttToRateStr;
import static medic.func.common.arc.query.ArcUtils.scoreToStr;

/**
 * @author MengLeiFudge
 */
public class Process extends FuncProcess {
    public Process(Main.Func func) {
        super(func);
        //menuList.add("吃鸡");
        menuList.add("arc吃鸡");
        menuList.add("arcaea吃鸡");
    }

    @Override
    public void menu() {
        send("标识 [g]群聊可用 [p]私聊可用\n" +
                "说明 [a]仅Bot管理员可用\n" +
                "[g]吃鸡：获取最近游玩的分数情况\n" +
                "[g]吃鸡 b30均值 定数 分数 P 大P F L：查询指定情况下分数，用空格连接每项\n" +
                "[g]加入吃鸡：加入吃鸡对局"
        );
    }

    @Override
    public boolean process() {
        if (textMsg.matches("吃鸡")) {
            lastSongInfo();
            return true;
        } else if (textMsg.matches("吃鸡 [0-9]+(\\.[0-9]+)? [0-9]+(\\.[0-9])? [0-9]+ [0-9]+ [0-9]+ [0-9]+ [0-9]+")) {
            String[] data = textMsg.split(" ");
            double b30Avg = Double.parseDouble(data[1]);
            double songRate = Double.parseDouble(data[2]);
            int x1 = Integer.parseInt(data[3]);
            int x2 = Integer.parseInt(data[4]);
            int x3 = Integer.parseInt(data[5]);
            int x4 = Integer.parseInt(data[6]);
            int x5 = Integer.parseInt(data[7]);
            fullScoreInfo(b30Avg, songRate, x1, x2, x3, x4, x5);
            return true;
        } else if (textMsg.matches("吃鸡 [0-9]+(\\.[0-9]+)? [0-9]+(\\.[0-9])?")) {
            String[] data = textMsg.split(" ");
            pubgImg(Double.parseDouble(data[1]), Double.parseDouble(data[2]));
            return true;
        } else if (textMsg.matches("吃鸡.+")) {
            send(qq, "指令为【吃鸡 b30均值 定数 分数 P 大P Far Lost】\n" +
                    "后三个可以交换位置");
            return true;
        } else if (textMsg.matches("加入吃鸡")) {
            joinPUBG();
            return true;
        }
        return false;
    }

    DecimalFormat df1 = new DecimalFormat("0.0");
    DecimalFormat df2 = new DecimalFormat("0.00");
    DecimalFormat df3 = new DecimalFormat("0.000");
    DecimalFormat df4 = new DecimalFormat("0.0000");

    private void lastSongInfo() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "我又不是神奇海螺，你不绑我怎么查啊！\n" + "tips：绑定 + 9位arcID");
            return;
        }
        send("查询信息ing，请耐心等候...");
        try {
            String s = getUserInfo(user.getArcId(), true);
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj1 = new JSONObject(s);
            if (obj1.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj1.getString("message"));
                return;
            }
            JSONObject content = obj1.getJSONObject("content");
            String name = content.getString("name");// 昵称
            int rating = content.getInt("rating");// ptt*100，隐藏时为-1
            String ratingStr = rating == -1 ? "隐藏" :
                    new DecimalFormat("#0.00").format(rating / 100.0);
            if (!content.has("recent_score")) {
                send(qq, "未查找到你的近期游玩记录，先打一首歌再来查吧！");
                return;
            }
            content = content.getJSONObject("recent_score");
            String songID = content.getString("song_id");// 歌曲id
            String songName = getSongNameByID(songID);// 歌曲名
            int difficulty = content.getInt("difficulty");// 难度，0123
            int score = content.getInt("score");// 分数
            int shinyPure = content.getInt("shiny_perfect_count");// 大Pure
            int pure = content.getInt("perfect_count");// Pure
            int far = content.getInt("near_count");// Far
            int lost = content.getInt("miss_count");// Lost
            int clearType = content.getInt("clear_type");// 完成类型
            long timePlayed = content.getLong("time_played");// 游玩时间
            double songPtt = content.getDouble("rating");// 单曲ptt
            String songRateStr = scoreAndPttToRateStr(songID, difficulty, score, songPtt);// 单曲定数
            double songRate = Double.parseDouble(songRateStr);

            s = getUserBest30(user.getArcId());
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj2 = new JSONObject(s);
            if (obj2.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj2.getString("message"));
                return;
            }
            content = obj2.getJSONObject("content");
            double b30Avg = content.getDouble("best30_avg");

            double score1 = getScore1(b30Avg, songRate, score, shinyPure);
            double score2 = getScore2(shinyPure, pure, far, lost);
            double result = score1 * 0.8 + score2 * 0.2;
            s = name + " (" + user.getArcId() + ") - " + ratingStr + "\n" +
                    "best30 均值：" + df3.format(b30Avg) + "\n" +
                    songName + " [" + diffFormatStr[difficulty] + "] [" + songRateStr + "]\n" +
                    scoreToStr(score) + " -> " + pttToStr(songPtt) + "\n\n" +
                    "基础得分：" + df4.format(score1) + "\n" +
                    "准度得分：" + df4.format(score2) + "\n" +
                    "最终得分：" + df4.format(result);
            send(qq, s);
        } catch (JSONException e) {
            logError(e);
        }
    }

    private void fullScoreInfo(double b30Avg, double songRate, int score,
                               int pure, int shinyPure, int far, int lost) {
        double score1 = getScore1(b30Avg, songRate, score, shinyPure);
        double score2 = getScore2(shinyPure, pure, far, lost);
        double result = score1 * 0.8 + score2 * 0.2;
        send(qq, "best30 均值：" + df3.format(b30Avg) + "\n" +
                "定数：" + df1.format(songRate) + "，分数：" + scoreToStr(score) + "\n" +
                "Pure " + pure + "(+" + shinyPure + ")" + " Far " + far + " Lost " + lost + "\n" +
                "基础得分：" + df4.format(score1) + "\n" +
                "准度得分：" + df4.format(score2) + "\n" +
                "最终得分：" + df4.format(result));
    }

    /**
     * 获取第一部分目标分数为95时，对应的游玩分数.
     */
    private static double get95Score(double b30Avg, double songRate) {
        double diffValue = songRate - b30Avg;
        diffValue = Math.max(-2.5, Math.min(0, diffValue));
        // 不要问我参数怎么来的，matlab拟合告诉我的（
        double fitValue = -15260 * Math.pow(diffValue, 3) - 167800 * Math.pow(diffValue, 2)
                - 597400 * Math.pow(diffValue, 1) + 9289000;
        return Math.max(9500000, fitValue);
    }

    private static double getScore1(double b30Avg, double songRate, int score, int shinyPure) {
        score -= shinyPure;
        double standardScore = 95.0;
        double k = (100.0 - standardScore) / (10000000 - get95Score(b30Avg, songRate));
        double b = 100.0 - k * 10000000;
        if (score < 9800000) {
            // 分数低于980w时，分数损失上升
            double score1 = k * 9800000 + b;
            k = k * 1.5;
            b = score1 - k * 9800000;
            if (score < 9500000) {
                // 分数低于950w时，分数损失上升
                double score2 = k * 9500000 + b;
                k = k * 2;
                b = score2 - k * 9500000;
            }
        }
        return Math.max(0, Math.min(k * score + b, 100));
    }

    private static double getScore2(int shinyPure, int pure, int far, int lost) {
        int note = pure + far + lost;
        double score100 = note * 1.0;
        double score = shinyPure * 1.0 + (pure - shinyPure) * 0.7 + far * 0.3;
        return score / score100 * 100;
    }

    private void pubgImg(double b30Avg, double songRate) {
        // 创建图片，画最外侧边框
        Img img = new Img(600, 400);
        img.setRgbColor(255, 255, 255);
        img.drawRect(0, 0, 600, 400);
        // 画分数曲线
        float x1;
        float y1;
        float x2 = 0;
        float y2 = (float) getScore1(b30Avg, songRate, 9000000, 0) * 3;
        img.setRgbColor(0, 0, 0);
        for (int score = 9004000; score <= 10000000; score += 4000) {
            x1 = x2;
            y1 = y2;
            x2 = (score - 9000000) / 2000.0f;
            y2 = (float) getScore1(b30Avg, songRate, score, 0) * 3;
            img.drawLine(x1 + 50, 400 - (y1 + 50), x2 + 50, 400 - (y2 + 50));
        }
        // 画边框
        img.setRgbColor(0x48, 0x76, 0xff);
        for (float i = 0; i <= 500; i += 50) {
            img.drawLine(i + 50, 50, i + 50, 350);
            img.drawText(((int) i / 5 + 900) + "w", i + 50 - 20, 400f - 30);
        }
        for (float i = 0; i <= 300; i += 30) {
            img.drawLine(50, i + 50, 550, i + 50);
            img.drawText(((int) i / 3) + "", 20, 400f - (i + 45));
        }
        img.setRgbColor(0xcc, 0x32, 0x99);
        File f = getFile(Dir.DATA, "test", getFullTimeStr(System.currentTimeMillis()) + ".jpg");
        img.save(f);
        addText("b30均值: " + df3.format(b30Avg) + "\n" +
                "歌曲定数: " + df1.format(songRate) + "\n");
        addImg(f);
        send();
    }

    private void joinPUBG() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "我又不是神奇海螺，你不绑我怎么查啊！\n" + "tips：绑定 + 9位arcID");
            return;
        }
        send("该功能还未写好！");
    }
}
