package medic.func.common.arc.query;

import medic.core.FuncProcess;
import medic.core.Main;
import medic.core.UnexpectedStateException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static medic.core.Api.MsgSource;
import static medic.core.Api.changeGroupToPrivate;
import static medic.core.Api.msgSource;
import static medic.core.Api.msgTime;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.AUTHOR_QQ;
import static medic.core.Utils.ERR_STRING;
import static medic.core.Utils.LESS_THAN_ONE_MINUTE;
import static medic.core.Utils.getFullTimeStr;
import static medic.core.Utils.getRandomInt;
import static medic.core.Utils.logError;
import static medic.core.Utils.logInfo;
import static medic.core.Utils.logWarn;
import static medic.core.Utils.milliSecondToStr;
import static medic.core.Utils.set;
import static medic.func.common.arc.query.ArcUtils.charStr;
import static medic.func.common.arc.query.ArcUtils.clearStr;
import static medic.func.common.arc.query.ArcUtils.diffFormatStr;
import static medic.func.common.arc.query.ArcUtils.difficulty;
import static medic.func.common.arc.query.ArcUtils.getConnectCodeStr;
import static medic.func.common.arc.query.ArcUtils.getRandomSong;
import static medic.func.common.arc.query.ArcUtils.getSongInfo;
import static medic.func.common.arc.query.ArcUtils.getSongInfoFile;
import static medic.func.common.arc.query.ArcUtils.getSongList;
import static medic.func.common.arc.query.ArcUtils.getSongNameByID;
import static medic.func.common.arc.query.ArcUtils.getUser;
import static medic.func.common.arc.query.ArcUtils.getUserBest;
import static medic.func.common.arc.query.ArcUtils.getUserBest30;
import static medic.func.common.arc.query.ArcUtils.getUserInfo;
import static medic.func.common.arc.query.ArcUtils.gradeStr;
import static medic.func.common.arc.query.ArcUtils.pttToStr;
import static medic.func.common.arc.query.ArcUtils.rateAndPttToScore;
import static medic.func.common.arc.query.ArcUtils.rateToStr;
import static medic.func.common.arc.query.ArcUtils.save;
import static medic.func.common.arc.query.ArcUtils.scoreAndPttToRate;
import static medic.func.common.arc.query.ArcUtils.scoreAndPttToRateStr;
import static medic.func.common.arc.query.ArcUtils.scoreAndRateToPtt;
import static medic.func.common.arc.query.ArcUtils.scoreAndRateToPttStr;
import static medic.func.common.arc.query.ArcUtils.scoreToStr;

/**
 * @author MengLeiFudge
 */
public class Process extends FuncProcess {
    public Process(Main.Func func) {
        super(func);
        menuList.add("arc");
        menuList.add("arcaea");
    }

    SongList songList = null;

    @Override
    public void menu() {
        send("标识 [g]群聊可用 [p]私聊可用\n" +
                "说明 [a]仅Bot管理员可用\n" +
                "[gp]/bind \\ 绑定+arcID：将你的qq与arc账号绑定\n" +
                "[gp]/arc \\ 查最近：获取最近一次游玩情况\n" +
                "[gp]查分+歌名(+难度)：查询单曲最佳游玩记录，默认难度ftr\n" +
                "[gp](查)b30：群内返回b30地板，私聊返回完整b30信息\n" +
                "[gp](查)地板：返回b30最低五项\n" +
                "[gp](查)天花板：返回b30最高五项"
        );
        sleep(300);
        send("[gp](计算)ptt+歌名(+难度)+分数：计算单曲指定分数对应的ptt，默认难度ftr\n" +
                "[gp]查定数+歌名(+难度)：查询指定歌曲定数，默认查询所有难度\n" +
                "[gp]随机选曲(+难度下限+难度上限)：随机选曲，可以指定难度上下限，" +
                "难度必须是1、1+、2、2+...11、11+中的某一个\n" +
                "[gp]/conn \\ 连接码：获取当前连接码\n" +
                "[gp]获取/查询全部(成绩)：查询所有定数>ptt-3的歌曲成绩\n" +
                "[gp]推分(建议)：获取推分建议"
        );
    }

    @Override
    public boolean process() {
        if (textMsg.matches("(?i)((绑定|/bind) *[0-9]{9})")) {
            bind(textMsg.substring(textMsg.length() - 9));
            return true;
        } else if (textMsg.matches("(?i)(/arc|查最近)")) {
            recent();
            return true;
        } else if (textMsg.matches("(?i)((查分|查分数).+(pst|prs|ftr|byd)?)")) {
            int index1 = textMsg.startsWith("查分数") ? 3 : 2;
            int len1 = textMsg.matches(".+(?i)(pst|prs|ftr|byd)") ? 3 : 0;
            String querySongName = textMsg.substring(index1, textMsg.length() - len1).trim();
            String diffStr = len1 == 0 ? "ftr" : textMsg.substring(textMsg.length() - 3).toLowerCase();
            queryBest(querySongName, diffStr);
            return true;
        } else if (textMsg.matches("查?[Bb]30")) {
            if (msgSource == MsgSource.GROUP) {
                queryBest30(B30_FLOOR);
            } else {
                queryBest30(B30_FULL);
            }
            return true;
        } else if (textMsg.matches("查?([Bb]30)?地板")) {
            queryBest30(B30_FLOOR);
            return true;
        } else if (textMsg.matches("查?([Bb]30)?(天花板|顶板)")) {
            queryBest30(B30_CEILING);
            return true;
        } else if (textMsg.matches("(?i)((计算)?ptt.+(pst|prs|ftr|byd)? *[0-9]+w?)")) {
            int beginLen = textMsg.startsWith("ptt") ? 3 : 5;
            String[] s1 = textMsg.split("\\D+");
            int score = Integer.parseInt(s1[s1.length - 1]);
            int endLen = (score + "").length();
            if (textMsg.endsWith("W") || textMsg.endsWith("w")) {
                score *= 10000;
                endLen++;
            }
            String s2 = textMsg.substring(beginLen, textMsg.length() - endLen).trim();
            int len1 = s2.matches(".+(?i)(pst|prs|ftr|byd)") ? 3 : 0;
            String querySongName = s2.substring(0, s2.length() - len1).trim();
            String diffStr = len1 == 0 ? "ftr" : s2.substring(s2.length() - 3).toLowerCase();
            calculatePtt(querySongName, difficulty(diffStr), score);
            return true;
        } else if (textMsg.contains("查定数")) {
            if (textMsg.matches("(?i)(查定数.+(pst|prs|ftr|byd))")) {
                String querySongName = textMsg.substring(3, textMsg.length() - 3).trim();
                String diffStr = textMsg.substring(textMsg.length() - 3).toLowerCase();
                queryRating(querySongName, difficulty(diffStr));
                return true;
            } else if (textMsg.matches("查定数.+")) {
                String querySongName = textMsg.substring(3).trim();
                queryRating(querySongName);
                return true;
            }
        } else if (textMsg.contains("随机选曲")) {
            if (textMsg.matches("随机选曲")) {
                randomSong(1, false, 11, true);
                return true;
            } else if (textMsg.matches("随机选曲 *[0-9]+(\\+|) *[0-9]+(\\+|)")) {
                String[] s1 = textMsg.split("\\D+");
                String minLv;
                String maxLv;
                if (s1.length == 2) {
                    String s = s1[1];
                    minLv = s.substring(0, 1);
                    maxLv = s.substring(1);
                } else {
                    minLv = s1[1];
                    maxLv = s1[2];
                }
                boolean minPlus = textMsg.charAt(textMsg.indexOf(minLv) + minLv.length()) == '+';
                boolean maxPlus = textMsg.indexOf(maxLv) + maxLv.length() != textMsg.length();
                randomSong(Integer.parseInt(minLv), minPlus, Integer.parseInt(maxLv), maxPlus);
                return true;
            }
        } else if (textMsg.matches("(?i)(/conn|/connect|连接码|链接码)")) {
            connect();
            return true;
        } else if (textMsg.matches("(获取|查询)(全部|所有)(成绩|)")) {
            queryAll();
            return true;
        } else if (textMsg.matches("清除查询状态")) {
            resetQuerying();
            return true;
        } else if (textMsg.matches("推分(建议|)")) {
            advice();
            return true;
        } else if (qq == AUTHOR_QQ) {
            if (textMsg.matches("更新arc")) {
                update();
                return true;
            }
        }
        return false;
    }

    public void bind(String arcID) {
        send("查询信息ing，请耐心等候...");
        try {
            String s = getUserInfo(arcID, false);
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj = new JSONObject(s);
            if (obj.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj.getString("message"));
                return;
            }

            // 上面status不为0已经表示所有异常情况，所以此处直接存储即可
            User user = new User(qq, arcID);
            save(user);

            JSONObject content = obj.getJSONObject("content");
            String name = content.getString("name");
            int rating = content.getInt("rating");// ptt*100，隐藏时为-1
            String ratingStr = rating == -1 ? "隐藏" :
                    new DecimalFormat("0.00").format(rating / 100.0);

            send(qq, "绑定成功！\n" +
                    name + " (" + arcID + ") - " + ratingStr);
        } catch (JSONException e) {
            logError(e);
        }
    }

    public void recent() {
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
            JSONObject obj = new JSONObject(s);
            if (obj.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj.getString("message"));
                return;
            }

            JSONObject content = obj.getJSONObject("content");
            if (!content.has("recent_score")) {
                send(qq, "未查找到你的近期游玩记录，先打一首歌再来查吧！");
                return;
            }
            String name = content.getString("name");// 昵称
            int character = content.getInt("character");// 角色
            int rating = content.getInt("rating");// ptt*100，隐藏时为-1
            String ratingStr = rating == -1 ? "隐藏" :
                    new DecimalFormat("0.00").format(rating / 100.0);
            // 搭档技能是否锁定、搭档是否觉醒、是否为觉醒后又切换到原始态
            boolean isSkillSealed = content.getBoolean("is_skill_sealed");
            boolean isCharUncapped = content.getBoolean("is_char_uncapped");
            boolean isCharUncappedOverride = content.getBoolean("is_char_uncapped_override");

            // 最近游玩记录
            JSONObject recentScore = content.getJSONObject("recent_score");
            String songID = recentScore.getString("song_id");// 歌曲id
            String songName = getSongNameByID(songID);// 歌曲名
            int difficulty = recentScore.getInt("difficulty");// 难度，0123
            int score = recentScore.getInt("score");// 分数
            int shinyPure = recentScore.getInt("shiny_perfect_count");// 大Pure
            int pure = recentScore.getInt("perfect_count");// Pure
            int far = recentScore.getInt("near_count");// Far
            int lost = recentScore.getInt("miss_count");// Lost
            int clearType = recentScore.getInt("clear_type");// 完成类型
            long timePlayed = recentScore.getLong("time_played");// 游玩时间
            double songPtt = recentScore.getDouble("rating");// 单曲ptt
            String songRateStr = scoreAndPttToRateStr(songID, difficulty, score, songPtt);// 单曲定数
            String fullTime = getFullTimeStr(timePlayed);
            String timeDiff = milliSecondToStr(timePlayed, System.currentTimeMillis(), false);
            if (!timeDiff.equals(LESS_THAN_ONE_MINUTE)) {
                timeDiff += "前";
            }

            SongList songList = getSongList();
            songList.add(songID, songName, difficulty,
                    scoreAndPttToRate(songName, difficulty, score, songPtt));
            save(songList);

            send(qq, name + " (" + user.getArcId() + ") - " + ratingStr + "\n" +
                    "搭档：" + charStr[character] +
                    (isCharUncapped ? (isCharUncappedOverride ? " - 初始" : " - 觉醒") : "") + "\n" +
                    "最近游玩记录：\n" +
                    songName + " [" + diffFormatStr[difficulty] + "] [" + songRateStr + "]\n" +
                    scoreToStr(score) + " -> " + pttToStr(songPtt) + "   "
                    + gradeStr(score) + "/" + clearStr[clearType] + "\n" +
                    "P " + pure + "(+" + shinyPure + ")     F " + far + "     L " + lost + "\n" +
                    fullTime + "\n" + timeDiff
            );
        } catch (JSONException e) {
            logError(e);
        }
    }

    public void queryBest(String querySongName, String diffStr) {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "我又不是神奇海螺，你不绑我怎么查啊！\n" + "tips：绑定 + 9位arcID");
            return;
        }
        send("查询信息ing，请耐心等候...");
        try {
            String s = getUserInfo(user.getArcId(), false);
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj1 = new JSONObject(s);
            if (obj1.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj1.getString("message"));
                return;
            }

            s = getUserBest(user.getArcId(), querySongName, diffStr);
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj2 = new JSONObject(s);
            if (obj2.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj2.getString("message"));
                return;
            }

            JSONObject content = obj1.getJSONObject("content");
            String name = content.getString("name");// 昵称
            int character = content.getInt("character");// 角色
            int rating = content.getInt("rating");// ptt*100，隐藏时为-1
            String ratingStr = rating == -1 ? "隐藏" :
                    new DecimalFormat("0.00").format(rating / 100.0);
            boolean isSkillSealed = content.getBoolean("is_skill_sealed");
            boolean isCharUncapped = content.getBoolean("is_char_uncapped");
            boolean isCharUncappedOverride = content.getBoolean("is_char_uncapped_override");

            content = obj2.getJSONObject("content");
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
            String fullTime = getFullTimeStr(timePlayed);
            String timeDiff = milliSecondToStr(timePlayed, System.currentTimeMillis(), false);
            if (!timeDiff.equals(LESS_THAN_ONE_MINUTE)) {
                timeDiff += "前";
            }

            SongList songList = getSongList();
            songList.add(songID, songName, difficulty,
                    scoreAndPttToRate(songName, difficulty, score, songPtt));
            save(songList);

            send(qq, name + " (" + user.getArcId() + ") - " + ratingStr + "\n" +
                    "搭档：" + charStr[character] +
                    (isCharUncapped ? (isCharUncappedOverride ? " - 初始" : " - 觉醒") : "") + "\n" +
                    "单曲最佳记录：\n" +
                    songName + " [" + diffFormatStr[difficulty] + "] [" + songRateStr + "]\n" +
                    scoreToStr(score) + " -> " + pttToStr(songPtt) + "   "
                    + gradeStr(score) + "/" + clearStr[clearType] + "\n" +
                    "P " + pure + "(+" + shinyPure + ")     F " + far + "     L " + lost + "\n" +
                    fullTime + "\n" + timeDiff
            );
        } catch (JSONException e) {
            logError(e);
        }
    }

    public static final int B30_FULL = 0;
    public static final int B30_FLOOR = 1;
    public static final int B30_CEILING = 2;
    public static final String[] B30_STATE = {"b30", "b30地板", "b30天花板"};

    public void queryBest30(int state) {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "我又不是神奇海螺，你不绑我怎么查啊！\n" + "tips：绑定 + 9位arcID");
            return;
        }
        send("查询信息ing，请耐心等候...");
        try {
            String s = getUserInfo(user.getArcId(), false);
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj1 = new JSONObject(s);
            if (obj1.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj1.getString("message"));
                return;
            }

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

            JSONObject content = obj1.getJSONObject("content");
            String name = content.getString("name");// 昵称
            int character = content.getInt("character");// 角色
            int rating = content.getInt("rating");// ptt*100，隐藏时为-1
            String ratingStr = rating == -1 ? "隐藏" :
                    new DecimalFormat("0.00").format(rating / 100.0);
            boolean isSkillSealed = content.getBoolean("is_skill_sealed");
            boolean isCharUncapped = content.getBoolean("is_char_uncapped");
            boolean isCharUncappedOverride = content.getBoolean("is_char_uncapped_override");

            content = obj2.getJSONObject("content");
            JSONArray b30List = content.getJSONArray("best30_list");// 长度可能为1-30
            int b30Num = b30List.length();
            double b30Avg = content.getDouble("best30_avg");
            String b30AvgStr = new DecimalFormat("0.000").format(b30Avg);
            String r10AvgStr;
            if (rating == -1) {
                r10AvgStr = "未知";
            } else {
                double b30Sum = b30Num * b30Avg;
                int r10Num = Math.min(10, b30Num);
                // rating = (b30Sum + r10Sum) / (b30Num + r10Num) * 100
                double r10Sum = rating / 100.0 * (b30Num + r10Num) - b30Sum;
                double r10Avg = r10Sum / r10Num;
                r10AvgStr = new DecimalFormat("0.000").format(r10Avg);
            }

            int min;
            int max;
            if (state == B30_FULL) {
                min = 0;
                max = b30Num;
            } else if (state == B30_FLOOR) {
                max = b30Num;
                min = Math.max(b30Num - 5, 0);
            } else if (state == B30_CEILING) {
                min = 0;
                max = Math.min(b30Num, 5);
            } else {
                throw new IllegalArgumentException("错误的参数state：" + state);
            }

            StringBuilder sb = new StringBuilder(name + " (" + user.getArcId() + ") - " + ratingStr + "\n" +
                    "搭档：" + charStr[character] +
                    (isCharUncapped ? (isCharUncappedOverride ? " - 初始" : " - 觉醒") : "") + "\n" +
                    B30_STATE[state] + "查询结果如下：\n" +
                    "best30 均值：" + b30AvgStr + "\n" +
                    "recent10 均值：" + r10AvgStr + "\n");
            int times = 0;
            for (int i = min; i < max; i++) {
                if (times == 5) {
                    send(qq, sb.toString());
                    sleep(300);
                    times = 0;
                    sb = new StringBuilder();
                }
                content = b30List.getJSONObject(i);
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

                SongList songList = getSongList();
                songList.add(songID, songName, difficulty,
                        scoreAndPttToRate(songName, difficulty, score, songPtt));
                save(songList);

                if (times % 5 != 0) {
                    sb.append("\n");
                }
                sb.append(songName).append(" [").append(diffFormatStr[difficulty])
                        .append("] [").append(songRateStr).append("]\n")
                        .append(scoreToStr(score)).append(" -> ").append(pttToStr(songPtt)).append("   ")
                        .append(gradeStr(score)).append("/").append(clearStr[clearType]).append("\n")
                        .append("P ").append(pure).append("(+").append(shinyPure).append(")     F ")
                        .append(far).append("     L ").append(lost);
                times++;
            }
            send(qq, sb.toString());
        } catch (JSONException e) {
            logError(e);
        }
    }

    public void calculatePtt(String querySongName, int difficulty, int score) {
        send("查询信息ing，请耐心等候...");
        try {
            String s = getSongInfo(querySongName);
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj = new JSONObject(s);
            if (obj.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj.getString("message"));
                return;
            }

            JSONObject content = obj.getJSONObject("content");
            String songID = content.getString("id");
            String songNameEN = content.getJSONObject("title_localized").getString("en");
            set(getSongInfoFile(), songID, songNameEN);
            double songRate = content.getJSONArray("difficulties")
                    .getJSONObject(difficulty).getDouble("ratingReal");

            SongList songList = getSongList();
            songList.add(songID, songNameEN, difficulty, songRate);
            save(songList);

            send(qq, songNameEN + " [" + diffFormatStr[difficulty]
                    + "] [" + songRate + "]\n" +
                    scoreToStr(score) + "  ->  " + scoreAndRateToPttStr(score, songRate));
        } catch (JSONException e) {
            logError(e);
        }
    }

    public void queryRating(String querySongName, int difficulty) {
        send("查询信息ing，请耐心等候...");
        try {
            String s = getSongInfo(querySongName);
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj = new JSONObject(s);
            if (obj.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj.getString("message"));
                return;
            }

            JSONObject content = obj.getJSONObject("content");
            String songID = content.getString("id");
            String songNameEN = content.getJSONObject("title_localized").getString("en");
            set(getSongInfoFile(), songID, songNameEN);
            double songRealRating = content.getJSONArray("difficulties")
                    .getJSONObject(difficulty).getDouble("ratingReal");

            SongList songList = getSongList();
            songList.add(songID, songNameEN, difficulty, songRealRating);
            save(songList);

            send(qq, "查询结果如下：\n" +
                    songNameEN + "\n" +
                    "[" + diffFormatStr[difficulty] + "] " + songRealRating);
        } catch (JSONException e) {
            logError(e);
        }
    }

    public void queryRating(String querySongName) {
        send("查询信息ing，请耐心等候...");
        try {
            String s = getSongInfo(querySongName);
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj = new JSONObject(s);
            if (obj.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj.getString("message"));
                return;
            }

            JSONObject content = obj.getJSONObject("content");
            String songID = content.getString("id");
            String songNameEN = content.getJSONObject("title_localized").getString("en");
            set(getSongInfoFile(), songID, songNameEN);
            JSONArray arr = content.getJSONArray("difficulties");
            double[] songRealRating = new double[4];
            songRealRating[0] = arr.getJSONObject(0).getDouble("ratingReal");
            songRealRating[1] = arr.getJSONObject(1).getDouble("ratingReal");
            songRealRating[2] = arr.getJSONObject(2).getDouble("ratingReal");

            SongList songList = getSongList();
            songList.add(songID, songNameEN, 0, songRealRating[0]);
            songList.add(songID, songNameEN, 1, songRealRating[1]);
            songList.add(songID, songNameEN, 2, songRealRating[2]);

            String send = "查询结果如下：\n" +
                    songNameEN + "\n" +
                    "[" + diffFormatStr[0] + "] " + songRealRating[0] + "\n" +
                    "[" + diffFormatStr[1] + "] " + songRealRating[1] + "\n" +
                    "[" + diffFormatStr[2] + "] " + songRealRating[2];
            if (arr.length() == 4) {
                songRealRating[3] = arr.getJSONObject(3).getDouble("ratingReal");
                send += "\n[" + diffFormatStr[3] + "] " + songRealRating[3];
                songList.add(songID, songNameEN, 3, songRealRating[3]);
            }
            save(songList);
            send(qq, send);
        } catch (JSONException e) {
            logError(e);
        }
    }

    public void randomSong(int minLv, boolean minPlus, int maxLv, boolean maxPlus) {
        send("查询信息ing，请耐心等候...");
        try {
            String s = getRandomSong(minLv, minPlus, maxLv, maxPlus, true);
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj = new JSONObject(s);
            if (obj.getInt("status") != 0) {
                send(qq, "查询出错！\n" + obj.getString("message"));
                return;
            }

            JSONObject content = obj.getJSONObject("content");
            int difficulty = content.getInt("rating_class");// 难度级别
            content = content.getJSONObject("song_info");
            String songID = content.getString("id");
            String songNameEN = content.getJSONObject("title_localized").getString("en");
            set(getSongInfoFile(), songID, songNameEN);
            content = content.getJSONArray("difficulties").getJSONObject(difficulty);
            double songRealRating = content.getDouble("ratingReal");

            SongList songList = getSongList();
            songList.add(songID, songNameEN, difficulty, songRealRating);
            save(songList);

            send(qq, "随机选曲结果如下：\n" +
                    songNameEN + " [" + diffFormatStr[difficulty] + "] " + songRealRating);
        } catch (JSONException e) {
            logError(e);
        }
    }

    public void connect() {
        send(qq, "当前连接码：" + getConnectCodeStr());
    }

    private boolean query = true;
    private int num = 0;
    private boolean hasException = false;

    /**
     * 获取所有 定数 > ptt - 3 的成绩
     */
    public void queryAll() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "我又不是神奇海螺，你不绑我怎么查啊！\n" + "tips：绑定 + 9位arcID");
            return;
        }
        // step1：防止该指令发送多次时也执行
        if (user.isQuerying()) {
            send(qq, "你现在已在查询了！\n" +
                    "如果上个发送获取全部成绩的指令的时间已过去较久，" +
                    "请发送【清除查询状态】来重置查询状态（仅在确认查询出问题时使用该指令）");
            return;
        }
        user.setQuerying(true);
        save(user);

        // step2：获取b30均值，以此确定要查询的范围
        send(qq, "正在获取你的b30均值...");
        double minPtt;
        String minPttStr;
        try {
            String s = getUserBest30(user.getArcId());
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj = new JSONObject(s);
            if (obj.getInt("status") != 0) {
                send(qq, "查询b30均值失败，请稍后再试！\n" + obj.getString("message"));
                user.setQuerying(false);
                save(user);
                return;
            }
            minPtt = obj.getJSONObject("content").getDouble("best30_avg") - 3;
            minPttStr = new DecimalFormat("0.000").format(minPtt);
        } catch (JSONException e) {
            logError(e);
            user.setQuerying(false);
            save(user);
            return;
        }
        String s = "开始查询定数大于" + minPttStr + "的所有歌曲！\n" +
                "所需时间可能较长，请耐心等候...\n" +
                "查询期间您仍可以使用其他指令！";
        if (msgSource == MsgSource.GROUP) {
            s += "\n\nPS：由于您在群内发送了该指令（该指令建议私聊发送），" +
                    "如果该群允许群私聊，稍后bot将在私聊中发送查询进度；" +
                    "否则，建议您添加bot为好友，同样可以获取查询进度。";
            changeGroupToPrivate();
        }
        send(qq, s);

        // step3：根据b30均值处理list，除掉所有ptt不在预期范围内的歌曲
        SongList songList = getSongList(true);
        List<SongInfo> list = songList.getListSortByRate();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).songRate < minPtt) {
                list.remove(i);
            }
        }

        // step4：开启一个持续发送查询进度的线程
        ExecutorService singleThreadPool = new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024),
                new ThreadPoolExecutor.AbortPolicy());
        singleThreadPool.execute(() -> {
            int lastNum = num;
            long lastSendTime = System.currentTimeMillis();
            while (query) {
                long time = System.currentTimeMillis();
                if (num != lastNum || time - lastSendTime > 20000) {
                    send("已查询成绩数目：" + num);
                    lastNum = num;
                    lastSendTime = time;
                }
                sleep(5000);
            }
        });
        singleThreadPool.shutdown();

        // 清除原有的成绩信息
        user.clearAllSongScoreInfo();

        // step5：开启多个查分线程
        // 注意线程数最大为8，再多将发生api加不上好友等错误，从而使获取的成绩变少
        // 但是，8线程仅在api不繁忙时可以正常运作，平常只有4线程是稳定的，且不能多人同时使用该指令
        ExecutorService pool = new ThreadPoolExecutor(
                4, 4,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024),
                new ThreadPoolExecutor.AbortPolicy());
        for (SongInfo info : list) {
            pool.execute(() -> {
                try {
                    String str = getUserBest(user.getArcId(),
                            info.getSongID(), info.getDifficulty() + "");
                    if (ERR_STRING.equals(str)) {
                        send(qq, "api获取信息超时啦！\n请稍后再试~");
                        return;
                    }
                    JSONObject obj = new JSONObject(str);
                    int status = obj.getInt("status");
                    if (status == -14) {
                        // 没玩过该曲
                        return;
                    } else if (status != 0) {
                        logWarn(new UnexpectedStateException(obj.getString("message")));
                        hasException = true;
                        return;
                    }

                    JSONObject content = obj.getJSONObject("content");
                    int score = content.getInt("score");// 分数
                    int shinyPure = content.getInt("shiny_perfect_count");// 大Pure
                    int pure = content.getInt("perfect_count");// Pure
                    int far = content.getInt("near_count");// Far
                    int lost = content.getInt("miss_count");// Lost
                    int clearType = content.getInt("clear_type");// 完成类型
                    long timePlayed = content.getLong("time_played");// 完成类型
                    double songPtt = content.getDouble("rating");// 单曲ptt
                    synchronized (this) {
                        user.addSongScoreInfo(info,
                                score, shinyPure, pure, far, lost,
                                clearType, timePlayed, songPtt);
                        num++;
                    }
                } catch (JSONException e) {
                    logError(e);
                    hasException = true;
                }
            });
        }
        pool.shutdown();
        while (!pool.isTerminated()) {
            sleep(1000);
        }
        if (hasException) {
            send(qq, "查询的过程中出现了一些bug，获取的成绩个数未达预期目标，请稍后重试！");
        }
        query = false;
        user.setQuerying(false);
        user.setQueryAllScoreTime(System.currentTimeMillis());
        save(user);
        save(songList);
        send(qq, "查询完毕，已获取可用于推分的" + num + "个成绩数据\n" +
                "快使用【推分】来获取推分建议吧！");
    }

    private void resetQuerying() {
        User user = getUser(qq);
        if (user == null) {
            send(qq, "正常情况下您不应知道该指令。开挂了吗？？？");
            return;
        }
        user.setQuerying(false);
        save(user);
        send(qq, "已将查询状态修复，现在可以再次获取全部成绩了。\n" +
                "对您造成的不便深表歉意QAQ");
    }

    public void advice() {
        User user = getUser(qq);
        if (user == null) {
            send("我又不是神奇海螺，你不绑我怎么查啊！\n" + "tips：绑定 + 9位arcID");
            return;
        }
        if (user.listIsEmpty()) {
            send(qq, "请先私聊我【获取/查询全部】！\n" +
                    "PS：尽量私聊，有进度提示，群聊无进度提示但也可以用");
            return;
        }
        long queryTime = user.getQueryAllScoreTime();
        String queryTimeDiff = milliSecondToStr(queryTime, msgTime, false);
        if (!queryTimeDiff.equals(LESS_THAN_ONE_MINUTE)) {
            queryTimeDiff += "前";
        }
        double floorPtt = user.getB30Floor();
        int randomIndex = getRandomInt(0, user.getSize() - 1);
        SongScoreInfo info = user.getListSortedBySongRating().get(randomIndex);
        String songName = info.getSongInfo().getSongName();// 歌曲名
        int difficulty = info.getSongInfo().getDifficulty();// 难度，0123
        double songRate = info.getSongInfo().getSongRate();// 单曲定数
        int score = info.getScore();// 分数
        int note = info.getNote();
        int shinyPure = info.getShinyPure();// 大Pure
        int pure = info.getPure();// Pure
        int far = info.getFar();// Far
        int lost = info.getLost();// Lost
        int clearType = info.getClearType();// 完成类型
        long timePlayed = info.getTimePlayed();// 游玩时间
        double songPtt = info.getSongPtt();// 单曲ptt
        String songRateStr = rateToStr(songRate);
        String songTimeDiff = milliSecondToStr(timePlayed, msgTime, false);
        if (!songTimeDiff.equals(LESS_THAN_ONE_MINUTE)) {
            songTimeDiff += "前";
        }

        double targetScore;
        double perScore = 5000000.0 / note;// 一个far的分数
        int floorScore = rateAndPttToScore(songRate, songPtt, note);
        double minScore = songRate + 2 >= floorPtt ? floorScore - 100000 : 9900000;
        if (score > 10000000) {
            // 原分数pm，随机多1-10个大p的分数
            targetScore = Math.min(score + getRandomInt(1, 10), 10000000 + note);
        } else {
            // 其余情况，随机多1-4个far的分数，且分数过低时加到minScore
            int oldFar = (int) (score / perScore);
            int targetFar = Math.min(oldFar + getRandomInt(1, 4), note * 2);
            targetScore = perScore * targetFar;
            while (targetScore < minScore) {
                targetScore += perScore;
            }
        }
        int showScore = (int) targetScore;// 显示的分数是实际分数取整
        double targetPtt = scoreAndRateToPtt(showScore, songRate);

        send(qq, "成绩个数：" + user.getSize() + "\n" +
                "时间：" + getFullTimeStr(queryTime) + "\n" + queryTimeDiff + "\n" +
                "————————\n" +
                songName + " [" + diffFormatStr[difficulty] + "] [" + songRateStr + "]\n" +
                "单曲最佳记录：\n" +
                scoreToStr(score) + " -> " + pttToStr(songPtt) + "   "
                + gradeStr(score) + "/" + clearStr[clearType] + "\n" +
                "P " + pure + "(+" + shinyPure + ")     F " + far + "     L " + lost + "\n" +
                getFullTimeStr(timePlayed) + "\n" + songTimeDiff + "\n" +
                "推分目标：\n" +
                scoreToStr(showScore) + " -> " + pttToStr(targetPtt)
        );
    }

    public void update() {
        send("开始更新！\n请从日志查看更新状态。");
        songList = getSongList();
        update("ifi");
        update("onefr");
        update("melodyoflove");
        update("aiueoon");
        update("alexandrite");
        update("altale");
        update("amygdata");
        update("anokumene");
        update("antagonism");
        update("antithese");
        update("arcahv");
        update("astraltale");
        update("auxesia");
        update("avantraze");
        update("axiumcrisis");
        update("babaroque");
        update("battlenoone");
        update("bethere");
        update("blacklotus");
        update("blackterritory");
        update("blaster");
        update("blossoms");
        update("blrink");
        update("bookmaker");
        update("brandnewworld");
        update("callmyname");
        update("carminescythe");
        update("chelsea");
        update("chronostasis");
        update("clotho");
        update("conflict");
        update("corpssansorganes");
        update("corruption");
        update("crosssoul");
        update("viyella");
        update("cyaegha");
        update("cyanine");
        update("cyberneciacatharsis");
        update("dandelion");
        update("dantalion");
        update("dataerror");
        update("dement");
        update("diode");
        update("dottodot");
        update("dreadnought");
        update("dreamgoeson");
        update("dreaminattraction");
        update("dropdead");
        update("dxfullmetal");
        update("einherjar");
        update("empireofwinter");
        update("equilibrium");
        update("essenceoftwilight");
        update("etherstrike");
        update("evoltex");
        update("fairytale");
        update("fallensquare");
        update("filament");
        update("flashback");
        update("flyburg");
        update("fractureray");
        update("freefall");
        update("garakuta");
        update("gekka");
        update("genesis");
        update("givemeanightmare");
        update("gloryroad");
        update("goodtek");
        update("grievouslady");
        update("grimheart");
        update("halcyon");
        update("hallofmirrors");
        update("harutopia");
        update("heavenlycaress");
        update("heavensdoor");
        update("hikari");
        update("iconoclast");
        update("ignotus");
        update("ikazuchi");
        update("darakunosono");
        update("impurebird");
        update("infinityheaven");
        update("inkarusi");
        update("hearditsaid");
        update("izana");
        update("journey");
        update("kanagawa");
        update("laqryma");
        update("lethaeus");
        update("libertas");
        update("linearaccelerator");
        update("lostcivilization");
        update("lostdesire");
        update("lucifer");
        update("lumia");
        update("espebranch");
        update("mazenine");
        update("memoryforest");
        update("memoryfactory");
        update("merlin");
        update("metallicpunisher");
        update("mirzam");
        update("modelista");
        update("monochromeprincess");
        update("moonheart");
        update("moonlightofsandcastle");
        update("nexttoyou");
        update("nhelv");
        update("nirvluce");
        update("oblivia");
        update("omakeno");
        update("onelastdrive");
        update("oracle");
        update("ouroboros");
        update("paradise");
        update("particlearts");
        update("partyvinyl");
        update("phantasia");
        update("pragmatism");
        update("purgatorium");
        update("qualia");
        update("quon");
        update("rabbitintheblackroom");
        update("reconstruction");
        update("redandblue");
        update("reinvent");
        update("relentless");
        update("revixy");
        update("ringedgenesis");
        update("rise");
        update("romancewars");
        update("rugie");
        update("saikyostronger");
        update("sayonarahatsukoi");
        update("scarletlance");
        update("senkyou");
        update("shadesoflight");
        update("sheriruth");
        update("silentrush");
        update("singularity");
        update("snowwhite");
        update("solitarydream");
        update("soundwitch");
        update("specta");
        update("stager");
        update("strongholds");
        update("sulfur");
        update("suomi");
        update("supernova");
        update("surrender");
        update("syro");
        update("tempestissimo");
        update("themessage");
        update("tiemedowngently");
        update("tiferet");
        update("trappola");
        update("valhallazero");
        update("vector");
        update("vexaria");
        update("viciousheroism");
        update("vindication");
        update("vividtheory");
        update("worldvanquisher");
        update("worldexecuteme");
        update("yozakurafubuki");
        update("yourvoiceso");
        update("aterlbus");
        update("guardina");
        update("scarletcage");
        update("feelssoright");
        update("faintlight");
        update("teriqma");
        update("mahoroba");
        update("badtek");
        update("maliciousmischance");
        save(songList);
        send("更新完毕！");
    }

    public void update(String song) {
        try {
            String s = getSongInfo(song);
            if (ERR_STRING.equals(s)) {
                send(qq, "api获取信息超时啦！\n请稍后再试~");
                return;
            }
            JSONObject obj = new JSONObject(s);
            int status = obj.getInt("status");
            if (status != 0) {
                return;
            }
            JSONObject content = obj.getJSONObject("content");
            String songID = content.getString("id");
            String songNameEN = content.getJSONObject("title_localized").getString("en");
            set(getSongInfoFile(), songID, songNameEN);
            JSONArray arr = content.getJSONArray("difficulties");
            double[] songRealRating = new double[4];
            songRealRating[0] = arr.getJSONObject(0).getDouble("ratingReal");
            songRealRating[1] = arr.getJSONObject(1).getDouble("ratingReal");
            songRealRating[2] = arr.getJSONObject(2).getDouble("ratingReal");
            songList.add(songID, songNameEN, 0, songRealRating[0]);
            songList.add(songID, songNameEN, 1, songRealRating[1]);
            songList.add(songID, songNameEN, 2, songRealRating[2]);
            if (arr.length() == 4) {
                songRealRating[3] = arr.getJSONObject(3).getDouble("ratingReal");
                songList.add(songID, songNameEN, 3, songRealRating[3]);
            }
            logInfo(song + " 已更新");
        } catch (JSONException e) {
            logError(e);
        }
    }
}
