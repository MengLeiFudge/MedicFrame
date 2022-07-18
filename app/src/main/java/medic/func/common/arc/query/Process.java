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
import static medic.core.Utils.AUTHOR_NAME;
import static medic.core.Utils.AUTHOR_QQ;
import static medic.core.Utils.ERR_STRING;
import static medic.core.Utils.LESS_THAN_ONE_MINUTE;
import static medic.core.Utils.getFullTimeStr;
import static medic.core.Utils.getRandomInt;
import static medic.core.Utils.logError;
import static medic.core.Utils.logInfo;
import static medic.core.Utils.logWarn;
import static medic.core.Utils.milliSecondToStr;
import static medic.func.common.arc.query.ArcUtils.charStr;
import static medic.func.common.arc.query.ArcUtils.clearStr;
import static medic.func.common.arc.query.ArcUtils.diffFormatStr;
import static medic.func.common.arc.query.ArcUtils.difficulty;
import static medic.func.common.arc.query.ArcUtils.getRandomSong;
import static medic.func.common.arc.query.ArcUtils.getSongInfo;
import static medic.func.common.arc.query.ArcUtils.getSongInfo2;
import static medic.func.common.arc.query.ArcUtils.getSongInfoList;
import static medic.func.common.arc.query.ArcUtils.getSongListJSON;
import static medic.func.common.arc.query.ArcUtils.getUpdate;
import static medic.func.common.arc.query.ArcUtils.getUser;
import static medic.func.common.arc.query.ArcUtils.getUserBest;
import static medic.func.common.arc.query.ArcUtils.getUserBest30;
import static medic.func.common.arc.query.ArcUtils.getUserInfo;
import static medic.func.common.arc.query.ArcUtils.gradeStr;
import static medic.func.common.arc.query.ArcUtils.pttToStr;
import static medic.func.common.arc.query.ArcUtils.rateAndPttToScore;
import static medic.func.common.arc.query.ArcUtils.rateToStr;
import static medic.func.common.arc.query.ArcUtils.save;
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

    SongInfoList songInfoList = null;

    @Override
    public void menu() {
        send("标识 [g]群聊可用 [p]私聊可用\n" +
                "说明 [a]仅Bot管理员可用\n" +
                "[gp]/bind \\ 绑定+arcID/昵称：将你的qq与arc账号绑定\n" +
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
                "[gp]arcapk：获取当前最新apk的版本及下载地址\n" +
                "[gp]获取/查询全部(成绩)：查询所有定数>ptt-3的歌曲成绩\n" +
                "[gp]推分(建议)：获取推分建议"
        );
    }

    @Override
    public boolean process() {
        if (textMsg.matches("(?i)((绑定|/bind) *.+)")) {
            int startIndex = textMsg.startsWith("绑定") ? 2 : 5;
            bind(textMsg.substring(startIndex).trim());
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
        } else if (textMsg.matches("(?i)(arcapk)")) {
            arcApk();
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
                updateAll();
                return true;
            }
        }
        return false;
    }

    public void bind(String arcIDOrName) {
        send("查询信息ing，请耐心等候...");
        try {
            String s = getUserInfo(arcIDOrName);
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

            JSONObject account_info = content.getJSONObject("account_info");
            String code = account_info.getString("code");
            String name = account_info.getString("name");
            int rating = account_info.getInt("rating");// ptt*100，隐藏时为-1
            String ratingStr = rating == -1 ? "隐藏" :
                    new DecimalFormat("0.00").format(rating / 100.0);

            User user = new User(qq, code);
            save(user);

            send(qq, "绑定成功！\n" + name + " (" + code + ") - " + ratingStr);
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
            String s = getUserInfo(user.getArcId(), 1, true);
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

            JSONObject account_info = content.getJSONObject("account_info");
            String name = account_info.getString("name");// 昵称
            int character = account_info.getInt("character");// 角色
            int rating = account_info.getInt("rating");// ptt*100，隐藏时为-1
            String ratingStr = rating == -1 ? "隐藏" :
                    new DecimalFormat("0.00").format(rating / 100.0);
            // 搭档技能是否锁定、搭档是否觉醒、是否为觉醒后又切换到原始态
            boolean isSkillSealed = account_info.getBoolean("is_skill_sealed");
            boolean isCharUncapped = account_info.getBoolean("is_char_uncapped");
            boolean isCharUncappedOverride = account_info.getBoolean("is_char_uncapped_override");

            // 最近游玩记录
            JSONObject recentScore = content.getJSONArray("recent_score").getJSONObject(0);
            String songID = recentScore.getString("song_id");// 歌曲id
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

            JSONObject songinfo = content.getJSONArray("songinfo").getJSONObject(0);
            String songName = songinfo.getString("name_en");

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
            String s = getUserBest(user.getArcId(), querySongName, diffStr, false, true);
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

            JSONObject account_info = content.getJSONObject("account_info");
            String name = account_info.getString("name");// 昵称
            int character = account_info.getInt("character");// 角色
            int rating = account_info.getInt("rating");// ptt*100，隐藏时为-1
            String ratingStr = rating == -1 ? "隐藏" :
                    new DecimalFormat("0.00").format(rating / 100.0);
            // 搭档技能是否锁定、搭档是否觉醒、是否为觉醒后又切换到原始态
            boolean isSkillSealed = account_info.getBoolean("is_skill_sealed");
            boolean isCharUncapped = account_info.getBoolean("is_char_uncapped");
            boolean isCharUncappedOverride = account_info.getBoolean("is_char_uncapped_override");

            JSONObject record = content.getJSONObject("record");
            String songID = record.getString("song_id");// 歌曲id
            int difficulty = record.getInt("difficulty");// 难度，0123
            int score = record.getInt("score");// 分数
            int shinyPure = record.getInt("shiny_perfect_count");// 大Pure
            int pure = record.getInt("perfect_count");// Pure
            int far = record.getInt("near_count");// Far
            int lost = record.getInt("miss_count");// Lost
            int clearType = record.getInt("clear_type");// 完成类型
            long timePlayed = record.getLong("time_played");// 游玩时间
            double songPtt = record.getDouble("rating");// 单曲ptt
            String songRateStr = scoreAndPttToRateStr(songID, difficulty, score, songPtt);// 单曲定数
            String fullTime = getFullTimeStr(timePlayed);
            String timeDiff = milliSecondToStr(timePlayed, System.currentTimeMillis(), false);
            if (!timeDiff.equals(LESS_THAN_ONE_MINUTE)) {
                timeDiff += "前";
            }

            JSONObject songinfo = content.getJSONArray("songinfo").getJSONObject(0);
            String songName = songinfo.getString("name_en");

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
            String s = getUserBest30(user.getArcId(), 0, false, true);
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

            JSONObject account_info = content.getJSONObject("account_info");
            String name = account_info.getString("name");// 昵称
            int character = account_info.getInt("character");// 角色
            int rating = account_info.getInt("rating");// ptt*100，隐藏时为-1
            String ratingStr = rating == -1 ? "隐藏" :
                    new DecimalFormat("0.00").format(rating / 100.0);
            // 搭档技能是否锁定、搭档是否觉醒、是否为觉醒后又切换到原始态
            boolean isSkillSealed = account_info.getBoolean("is_skill_sealed");
            boolean isCharUncapped = account_info.getBoolean("is_char_uncapped");
            boolean isCharUncappedOverride = account_info.getBoolean("is_char_uncapped_override");

            JSONArray b30List = content.getJSONArray("best30_list");// 长度可能为1-30
            int b30Num = b30List.length();
            double b30Avg = content.getDouble("best30_avg");
            String b30AvgStr = new DecimalFormat("0.000").format(b30Avg);
            String r10AvgStr;
            if (rating == -1) {
                r10AvgStr = "未知";
            } else {
                /*double b30Sum = b30Num * b30Avg;
                int r10Num = Math.min(10, b30Num);
                // rating = (b30Sum + r10Sum) / (b30Num + r10Num) * 100
                double r10Sum = rating / 100.0 * (b30Num + r10Num) - b30Sum;
                double r10Avg = r10Sum / r10Num;
                r10AvgStr = new DecimalFormat("0.000").format(r10Avg);*/
                double r10Avg = content.getDouble("recent10_avg");
                r10AvgStr = new DecimalFormat("0.000").format(r10Avg);
            }

            JSONArray b30SongInfo = content.getJSONArray("best30_songinfo");// 长度可能为1-30

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

                JSONObject b30List_i = b30List.getJSONObject(i);
                String songID = b30List_i.getString("song_id");// 歌曲id
                int difficulty = b30List_i.getInt("difficulty");// 难度，0123
                int score = b30List_i.getInt("score");// 分数
                int shinyPure = b30List_i.getInt("shiny_perfect_count");// 大Pure
                int pure = b30List_i.getInt("perfect_count");// Pure
                int far = b30List_i.getInt("near_count");// Far
                int lost = b30List_i.getInt("miss_count");// Lost
                int clearType = b30List_i.getInt("clear_type");// 完成类型
                long timePlayed = b30List_i.getLong("time_played");// 游玩时间
                double songPtt = b30List_i.getDouble("rating");// 单曲ptt
                String songRateStr = scoreAndPttToRateStr(songID, difficulty, score, songPtt);// 单曲定数

                JSONObject b30SongInfo_i = b30SongInfo.getJSONObject(i);
                String songName = b30SongInfo_i.getString("name_en");

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
            SongInfo info = new SongInfo(content);
            if (!info.isOk()) {
                send(qq, "歌曲信息处理出错！\n请联系" + AUTHOR_NAME + "(" + AUTHOR_QQ + ")！");
                return;
            }

            SongInfoList songInfoList = getSongInfoList();
            songInfoList.add(info);
            save(songInfoList);

            send(qq, info.getName_en()[difficulty] + " [" + diffFormatStr[difficulty]
                    + "] [" + info.getRating()[difficulty] / 10.0 + "]\n" +
                    scoreToStr(score) + "  ->  " + scoreAndRateToPttStr(score, info.getRating()[difficulty] / 10.0));
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
            SongInfo info = new SongInfo(content);
            if (!info.isOk()) {
                send(qq, "歌曲信息处理出错！\n请联系" + AUTHOR_NAME + "(" + AUTHOR_QQ + ")！");
                return;
            }

            SongInfoList songInfoList = getSongInfoList();
            songInfoList.add(info);
            save(songInfoList);

            send(qq, "查询结果如下：\n" +
                    info.getName_en()[difficulty] + "\n" +
                    "[" + diffFormatStr[difficulty] + "] " + info.getRating()[difficulty] / 10.0);
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
            SongInfo info = new SongInfo(content);
            if (!info.isOk()) {
                send(qq, "歌曲信息处理出错！\n请联系" + AUTHOR_NAME + "(" + AUTHOR_QQ + ")！");
                return;
            }

            SongInfoList songInfoList = getSongInfoList();
            songInfoList.add(info);
            save(songInfoList);

            String send = "查询结果如下：\n" +
                    info.getName_en()[0] + "\n" +
                    "[" + diffFormatStr[0] + "] " + info.getRating()[0] / 10.0 + "\n" +
                    "[" + diffFormatStr[1] + "] " + info.getRating()[1] / 10.0 + "\n" +
                    "[" + diffFormatStr[2] + "] " + info.getRating()[2] / 10.0;
            if (info.getDifficulty()[3] != 0) {
                if (!info.getName_en()[0].equals(info.getName_en()[3])) {
                    send += "\n" + info.getName_en()[3];
                }
                send += "\n[" + diffFormatStr[3] + "] " + info.getRating()[3] / 10.0;
            }
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
            int difficulty = content.getInt("ratingClass");// 难度级别
            JSONObject songinfo = content.getJSONObject("songinfo");
            String songNameEN = songinfo.getString("name_en");
            double songRealRating = songinfo.getDouble("rating") / 10.0;

            send(qq, "随机选曲结果如下：\n" +
                    songNameEN + " [" + diffFormatStr[difficulty] + "] " + songRealRating);
        } catch (JSONException e) {
            logError(e);
        }
    }

    public void arcApk() {
        send("查询信息ing，请耐心等候...");
        try {
            String s = getUpdate();
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
            String url = content.getString("url");
            String version = content.getString("version");

            send(qq, "Arcaea[" + version + "] 下载地址：\n" + url + "\n" +
                    "您也可以在616.sb下载各个音游！");
        } catch (JSONException e) {
            logError(e);
        }
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
            String s = getUserBest30(user.getArcId(), 0, false, false);
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
        }
        send(qq, s);
        if (msgSource == MsgSource.GROUP) {
            changeGroupToPrivate();
        }

        // step3：获取歌曲信息list
        SongInfoList songInfoList = getSongInfoList(true);
        List<SongInfo> list = songInfoList.getListSortByRate();

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
                for (int i = 0; i < 4; i++) {
                    if (info.getRating()[i] / 10.0 < minPtt) {
                        continue;
                    }
                    try {
                        String str = getUserBest(user.getArcId(), info.getSong_id(), i + "");
                        if (ERR_STRING.equals(str)) {
                            send(qq, "api获取信息超时啦！\n请稍后再试~");
                            return;
                        }
                        JSONObject obj = new JSONObject(str);
                        int status = obj.getInt("status");
                        if (status == -15) {
                            // not played yet
                            return;
                        } else if (status != 0) {
                            logWarn(new UnexpectedStateException(obj.getString("message")));
                            hasException = true;
                            return;
                        }

                        JSONObject record = obj.getJSONObject("content").getJSONObject("record");
                        int score = record.getInt("score");// 分数
                        int shinyPure = record.getInt("shiny_perfect_count");// 大Pure
                        int pure = record.getInt("perfect_count");// Pure
                        int far = record.getInt("near_count");// Far
                        int lost = record.getInt("miss_count");// Lost
                        int clearType = record.getInt("clear_type");// 完成类型
                        long timePlayed = record.getLong("time_played");// 完成类型
                        double songPtt = record.getDouble("rating");// 单曲ptt
                        synchronized (this) {
                            user.addSongRecord(info, i,
                                    score, shinyPure, pure, far, lost,
                                    clearType, timePlayed, songPtt);
                            num++;
                        }
                    } catch (JSONException e) {
                        logError(e);
                        hasException = true;
                    }
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
        save(songInfoList);
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
        SongRecord record = user.getListSortedBySongPtt().get(randomIndex);
        int difficulty = record.getDifficulty();// 难度，0123
        String songName = record.getSongInfo().getName_en()[difficulty];// 歌曲名
        double songRate = record.getSongInfo().getRating()[difficulty] / 10.0;// 单曲定数
        int score = record.getScore();// 分数
        int note = record.getNote();
        int shinyPure = record.getShinyPure();// 大Pure
        int pure = record.getPure();// Pure
        int far = record.getFar();// Far
        int lost = record.getLost();// Lost
        int clearType = record.getClearType();// 完成类型
        long timePlayed = record.getTimePlayed();// 游玩时间
        double songPtt = record.getSongPtt();// 单曲ptt
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

    public void updateAll() {
        send("开始更新！\n请从日志查看更新状态。");
        songInfoList = getSongInfoList();
        try {
            JSONArray songs = getSongListJSON().getJSONArray("songs");
            for (int i = 0; i < songs.length(); i++) {
                String sid = songs.getJSONObject(i).getString("id");
                update(sid);
            }
        } catch (JSONException e) {
            logError(e);
            return;
        }
        save(songInfoList);
        send("更新完毕！");
    }

    public void update(String sid) {
        try {
            String s = getSongInfo2(sid);
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
            SongInfo info = new SongInfo(content);
            if (!info.isOk()) {
                send(qq, "歌曲信息处理出错！\n请联系" + AUTHOR_NAME + "(" + AUTHOR_QQ + ")！");
                return;
            }

            //SongInfoList songInfoList = getSongInfoList();
            songInfoList.add(info);
            //save(songInfoList);

            logInfo(sid + " 已更新");
        } catch (JSONException e) {
            logError(e);
        }
    }
}
