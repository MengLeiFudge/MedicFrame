package medic.func.common.arc.query;

import medic.core.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import static medic.core.Utils.DEF_STRING;
import static medic.core.Utils.deserialize;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getInfoFromUrl;
import static medic.core.Utils.getMd5;
import static medic.core.Utils.getString;
import static medic.core.Utils.logError;
import static medic.core.Utils.serialize;
import static medic.core.Utils.set;

/**
 * @author MengLeiFudge
 */
public class ArcUtils {
    private ArcUtils() {
    }

    /* -- api接口 -- */

    //WIKI 地址：https://github.com/Arcaea-Infinity/ArcaeaUnlimitedAPI-Wiki

    /**
     * 从 aw 的 arc api 获取信息.
     *
     * @param function  要使用的功能
     * @param urlParams 功能所需参数
     * @return 查询结果
     */
    public static String getStrFromAW(String function, Map<String, String> urlParams) {
        String url = "https://server.awbugl.top/botarcapi/" + function;
        Map<String, String> headerParams = new LinkedHashMap<>();
        headerParams.put("User-Agent", "menglei");
        return getInfoFromUrl(url, urlParams, headerParams);
    }

    /**
     * user/info.
     * <p>
     * 绑定时使用，无具体信息。
     *
     * @param user user name or 9-digit user code
     * @return user/info
     */
    public static String getUserInfo(String user) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user", user);
        return getStrFromAW("user/info", params);
    }

    /**
     * user/info.
     * <p>
     * 需要具体信息（如最近打歌情况等）时使用。
     *
     * @param usercode     9-digit user code
     * @param recent       number, range 0-7. The number of recently played songs expected
     * @param withsonginfo boolean. if true, will reply with songinfo
     * @return user/info
     */
    public static String getUserInfo(String usercode, int recent, boolean withsonginfo) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("usercode", usercode);
        params.put("recent", recent + "");// 可选
        params.put("withsonginfo", withsonginfo + "");// 可选
        return getStrFromAW("user/info", params);
    }

    /**
     * user/best.
     * <p>
     * 查询个人单曲最佳时使用。
     *
     * @param usercode     9-digit user code
     * @param songname     any song name for fuzzy querying
     * @param difficulty   accept format are 0/1/2/3 or pst/prs/ftr/byn or past/present/future/beyond
     * @param withrecent   boolean. if true, will reply with recent_score
     * @param withsonginfo boolean. if true, will reply with songinfo
     * @return user/best
     */
    public static String getUserBest(String usercode, String songname, String difficulty,
                                     boolean withrecent, boolean withsonginfo) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("usercode", usercode);
        params.put("songname", songname);
        params.put("difficulty", difficulty);
        params.put("withrecent", withrecent + "");// 可选
        params.put("withsonginfo", withsonginfo + "");// 可选
        return getStrFromAW("user/best", params);
    }

    /**
     * user/best.
     * <p>
     * 对于推分指令，程序查询个人全部单曲最佳时使用，需要使用歌曲sid。
     *
     * @param usercode   9-digit user code
     * @param songid     sid in Arcaea songlist
     * @param difficulty accept format are 0/1/2/3 or pst/prs/ftr/byn or past/present/future/beyond
     * @return user/best
     */
    public static String getUserBest(String usercode, String songid, String difficulty) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("usercode", usercode);
        params.put("songid", songid);
        params.put("difficulty", difficulty);
        return getStrFromAW("user/best", params);
    }

    /**
     * user/best30.
     * <p>
     * 查询个人单曲最佳前30时使用。
     * <p>
     * ps1：隐藏ptt（即灰框）时，recent10_avg = 0
     * <p>
     * ps2：best30_list 有可能少于 30 个（通常出现于新玩家）
     *
     * @param usercode     9-digit user code
     * @param overflow     number, range 0-10. The number of the overflow records below the best30 minimum
     * @param withrecent   boolean. if true, will reply with recent_score
     * @param withsonginfo boolean. if true, will reply with songinfo
     * @return user/best30
     */
    public static String getUserBest30(String usercode, int overflow, boolean withrecent, boolean withsonginfo) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("usercode", usercode);
        params.put("overflow", overflow + "");// 可选
        params.put("withrecent", withrecent + "");// 可选
        params.put("withsonginfo", withsonginfo + "");// 可选
        return getStrFromAW("user/best30", params);
    }

    /**
     * song/info.
     * <p>
     * 查询单曲信息时使用。
     *
     * @param songname any song name for fuzzy querying
     * @return song/info
     */
    public static String getSongInfo(String songname) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("songname", songname);
        return getStrFromAW("song/info", params);
    }

    /**
     * song/info.
     * <p>
     * 对于推分指令，程序查询单曲信息时使用，需要使用歌曲sid。
     *
     * @param songid sid in Arcaea songlist
     * @return song/info
     */
    public static String getSongInfo2(String songid) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("songid", songid);
        return getStrFromAW("song/info", params);
    }

    // song/alias作用是根据sid或俗称，返回歌曲的所有俗称。没什么用，所以不写了。

    /**
     * song/random.
     * <p>
     * 随机选曲时使用。
     *
     * @param start        range of start (9+ => 9p , 10+ => 10p)
     * @param end          range of end
     * @param withsonginfo boolean. if true, will reply with songinfo
     * @return song/random
     */
    private static String getRandomSong(int start, int end, boolean withsonginfo) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("start", start + "");// 可选
        params.put("end", end + "");// 可选
        params.put("withsonginfo", withsonginfo + "");// 可选
        return getStrFromAW("song/random", params);
    }

    /**
     * song/random.
     * <p>
     * 随机选曲时使用，便于调用。
     *
     * @param minLv        等级下限数字
     * @param minPlus      下限是否带+
     * @param maxLv        等级上限数字
     * @param maxPlus      下限是否带+
     * @param withsonginfo boolean. if true, will reply with songinfo
     * @return song/random
     */
    public static String getRandomSong(int minLv, boolean minPlus,
                                       int maxLv, boolean maxPlus, boolean withsonginfo) {
        int min = minLv * 2 + (minPlus ? 1 : 0);
        int max = maxLv * 2 + (maxPlus ? 1 : 0);
        if (min > max) {
            min ^= max;
            max ^= min;
            min ^= max;
        }
        return getRandomSong(min, max, withsonginfo);
    }

    /**
     * 计算当前 arc connect 码.
     *
     * @return 当前 arc connect 码
     * @deprecated 这个解迷仅在3.0.0出的时候有用，现在已经不需要该解迷了。
     */
    @Deprecated
    public static String getConnectCodeStr() {
        Calendar cal = Calendar.getInstance();
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        String x = cal.get(Calendar.YEAR) + "ori" +
                cal.get(Calendar.MONTH) + "wol" +
                cal.get(Calendar.DAY_OF_MONTH) + "oihs" +
                cal.get(Calendar.DAY_OF_MONTH) + "otas";
        String md5 = getMd5(x);
        // table 长度为 36
        String table = "qwertyuiopasdfghjklzxcvbnm1234567890";
        return new String(new char[]{
                table.charAt(md5.charAt(1) % 36),
                table.charAt(md5.charAt(20) % 36),
                table.charAt(md5.charAt(4) % 36),
                table.charAt(md5.charAt(30) % 36),
                table.charAt(md5.charAt(2) % 36),
                table.charAt(md5.charAt(11) % 36),
                table.charAt(md5.charAt(23) % 36)});
    }

    /**
     * update.
     * <p>
     * 游戏更新时使用，可以获取apk的最新版本号与下载链接。
     *
     * @return update
     */
    public static String getUpdate() {
        return getStrFromAW("update", null);
    }


    /* -- 快捷获取 -- */

    /**
     * 获取单曲定数
     *
     * @param songName   歌名
     * @param difficulty 难度
     * @param score      分数
     * @param rating     分数对应的ptt
     * @return 定数
     */
    static double scoreAndPttToRate(String songName, int difficulty, int score, double rating) {
        if (rating == 0) {
            // 单曲ptt为0时，不能通过计算得到定数，需要查询
            try {
                JSONObject obj = new JSONObject(getSongInfo(songName));
                return obj.getJSONObject("content").getJSONArray("difficulties")
                        .getJSONObject(difficulty).getDouble("ratingReal");
            } catch (JSONException e) {
                logError(e);
            }
            return -1;
        } else {
            // 不为0则可以直接算出定数
            long ret = score > 9800000
                    ? Math.round((rating - (score - 9800000.0) / 200000 - 1) * 10)
                    : Math.round((rating - (score - 9500000.0) / 300000) * 10);
            return ret / 10.0;
        }
    }

    static String rateToStr(double rate) {
        return new DecimalFormat("0.0").format(rate);
    }

    public static String scoreAndPttToRateStr(String songName, int difficulty, int score, double ptt) {
        double rate = scoreAndPttToRate(songName, difficulty, score, ptt);
        return rate == -1 ? "未知" : rateToStr(rate);
    }

    /**
     * 获取单曲ptt
     *
     * @param score 分数
     * @param rate  定数
     * @return 分数对应的ptt
     */
    public static double scoreAndRateToPtt(int score, double rate) {
        if (score > 10000000) {
            return rate + 2;
        } else if (score > 9800000) {
            return rate + (score - 9800000) / 200000.0 + 1;
        } else {
            return Math.max(0, rate + (score - 9500000) / 300000.0);
        }
    }

    public static String pttToStr(double ptt) {
        return new DecimalFormat("#0.000").format(ptt);
    }

    static String scoreAndRateToPttStr(int score, double rate) {
        return pttToStr(scoreAndRateToPtt(score, rate));
    }

    /**
     * 获取分数
     *
     * @param rate 定数
     * @param ptt  ptt
     * @return 对应分数
     */
    public static int rateAndPttToScore(double rate, double ptt, int note) {
        if (ptt >= rate + 2) {
            return 10000000;
        }
        double minScore;
        if (ptt > rate + 1) {
            minScore = 9800000 + (ptt - rate - 1) * 200000;
        } else {
            minScore = 9500000 + (ptt - rate) * 300000;
        }
        if (minScore <= 0) {
            return 0;
        }
        double perScore = 5000000.0 / note;
        double retScore = 10000000;
        while (retScore > minScore) {
            retScore -= perScore;
        }
        retScore += perScore;
        return (int) retScore;
    }

    public static String scoreToStr(int score) {
        return new DecimalFormat("#0,000,000").format(score);
    }

    public static String rateAndPttToScoreStr(double rate, double ptt, int note) {
        return scoreToStr(rateAndPttToScore(rate, ptt, note));
    }

    public static int difficulty(String diffStr) {
        switch (diffStr.toLowerCase()) {
            case "past":
            case "pst":
                return 0;
            case "present":
            case "prs":
                return 1;
            case "future":
            case "ftr":
                return 2;
            case "beyond":
            case "byd":
                return 3;
            default:
                throw new IllegalArgumentException("错误的难度级别：" + diffStr);
        }
    }

    public static String gradeStr(int score) {
        if (score >= 9900000) {
            return "EX+";
        } else if (score >= 9800000) {
            return "EX";
        } else if (score >= 9500000) {
            return "AA";
        } else if (score >= 9200000) {
            return "A";
        } else if (score >= 8900000) {
            return "B";
        } else if (score >= 8600000) {
            return "C";
        } else {
            return "D";
        }
    }

    public static final String[] diffFormatStr = {"PST", "PRS", "FTR", "BYD"};
    public static final String[] clearStr = {"TL", "NC", "FR", "PM", "EC", "HC"};
    public static final String[] charStr = {
            /*00-04*/"光", "对立", "红", "萨菲亚", "忘却",
            /*05-09*/"光 & 对立（Reunion）", "对立（Axium）", "对立（Grievous Lady）", "星", "光 & 菲希卡",
            /*10-14*/"依莉丝", "爱托", "露娜", "调", "光（Zero）",
            /*15-19*/"光（Fracture）", "光（夏日）", "对立（夏日）", "对立 & 托凛", "彩梦",
            /*20-24*/"爱托 & 露娜（冬日）", "柚梅", "光 & 赛依娜", "咲弥", "对立 & 中二企鹅（Grievous Lady）",
            /*25-29*/"中二企鹅", "榛名", "诺诺", "潘多拉涅墨西斯（MTA-XXX）", "轩辕十四（MDA-21）",
            /*30-34*/"群愿", "光（Fantasia）", "对立（Sonata）", "兮娅", "DORO*C",
            /*35-39*/"对立（Tempest）", "布丽兰特", "依莉丝（夏日）", "咲弥（Etude）", "爱丽丝 & 坦尼尔",
            /*40-44*/"露娜 & 美亚", "阿莱乌斯", "希尔", "伊莎贝尔", "迷尔",
            /*45-49*/"拉格兰", "凛可", "奈美", "咲弥 & 伊丽莎白", "莉莉",
            /*50-54*/"群愿（盛夏）", "爱丽丝 & 坦尼尔（Minuet）", "对立（Elegy）", "玛莉嘉", "维塔",
            /*55-59*/"光（Fatalis）", "未知", "未知", "未知", "未知",
            /*60-64*/"未知", "未知", "未知", "未知", "未知",
            /*65-69*/"未知", "未知", "未知", "未知", "未知",
            /*70-74*/"未知", "未知", "未知", "未知", "未知",
            /*75-79*/"未知", "未知", "未知", "未知", "未知",
            /*80-84*/"未知", "未知", "未知", "未知", "未知",
            /*85-89*/"未知", "未知", "未知", "未知", "未知",
            /*90-94*/"未知", "未知", "未知", "未知", "未知",
            /*95-99*/"未知", "未知", "未知", "未知", "白姬",
    };


    /* -- 文件存储 -- */

    static File getRootDir() {
        return getFile(Utils.Dir.DATA, "arcaea");
    }

    private static File getUserDir() {
        return getFile(getRootDir(), "user");
    }

    private static File getUserFile(long qq) {
        return getFile(getUserDir(), qq + ".ser");
    }

    public static User getUser(long qq) {
        return deserialize(getUserFile(qq), User.class);
    }

    static void save(User user) {
        serialize(user, getUserFile(user.getQq()));
    }

    private static File getSongInfoListFile() {
        return getFile(getRootDir(), "songInfoList.ser");
    }

    static SongInfoList getSongInfoList(boolean unlockAtMethodEnd) {
        SongInfoList songInfoList = deserialize(getSongInfoListFile(), SongInfoList.class, unlockAtMethodEnd);
        if (songInfoList == null) {
            songInfoList = new SongInfoList();
            save(songInfoList);
        }
        return songInfoList;
    }

    static SongInfoList getSongInfoList() {
        return getSongInfoList(false);
    }

    static void save(SongInfoList songInfoList) {
        serialize(songInfoList, getSongInfoListFile());
    }

    static JSONObject getSongListJSON() throws JSONException {
        File songlist = getFile(getRootDir(), "songlist");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(songlist))) {
            String s;
            while ((s = br.readLine()) != null) {
                sb.append(s).append("\n");
            }
        } catch (IOException e) {
            return null;
        }
        return new JSONObject(sb.toString());
    }
}
