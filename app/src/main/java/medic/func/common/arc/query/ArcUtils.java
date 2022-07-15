package medic.func.common.arc.query;

import medic.core.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static medic.core.Utils.DEF_STRING;
import static medic.core.Utils.deserialize;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getMd5;
import static medic.core.Utils.getStrFromURL;
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

    static final String USER_AGENT = "menglei";

    /**
     * 从湖精姐姐的 arc api 获取信息
     *
     * @param query  要使用的功能
     * @param params 参数
     * @return 获得的字符串
     */
    public static String getStrFromHJ(String query, Map<String, Object> params) {
        StringBuilder url = new StringBuilder("https://arcapi.cirnobaka.moe/v2/" + query + "?");
        // 是否在键值对前加上连接符 &，第一个键值对不需要，之后都需要
        boolean needConnector = false;
        for (Map.Entry<String, Object> p : params.entrySet()) {
            if (needConnector) {
                url.append("&");
            } else {
                needConnector = true;
            }
            url.append(p.getKey()).append("=").append(p.getValue());
        }
        return getStrFromURL(url.toString(), USER_AGENT);
    }

    /**
     * 玩家基本信息，可以选择是否获取最近游玩情况.
     * <p>
     * ps1：recent_score 可能为 null（没有游玩记录）
     * <p>
     * 返回数据样例如下：
     * <pre>
     * {
     *     "status": 0,
     *     "content": {
     *         "user_id": 114514,
     *         "name": "114514",
     *         "recent_score": {
     *             "song_id": "grievouslady",
     *             "difficulty": 2,
     *             "score": 0,
     *             "shiny_perfect_count": 0,
     *             "perfect_count": 0,
     *             "near_count": 0,
     *             "miss_count": 0,
     *             "clear_type": 0,
     *             "best_clear_type": 0,
     *             "health": 0,
     *             "time_played": 1145141145141,
     *             "modifier": 0,
     *             "rating": 0
     *         },
     *         "character": 0,
     *         "join_date": 1145141145141,
     *         "rating": 0,
     *         "is_skill_sealed": false,
     *         "is_char_uncapped": false,
     *         "is_char_uncapped_override": false,
     *         "is_mutual": false
     *     }
     * }
     * </pre>
     * status可能返回的值以及含义如下：
     * <pre>
     * status    description
     * 0         everything is OK
     * -1        invalid usercode
     * -2        allocate an arc account failed
     * -3        clear friend list failed
     * -4        add friend failed
     * -5        internal error occurred
     * -233      unknown error occurred
     * </pre>
     *
     * @param arcID  arcID，因为有 0 开头的 id，所以使用 String
     * @param recent 是否获取最近游玩情况
     * @return 玩家信息
     */
    public static String getUserInfo(String arcID, boolean recent) {
        Map<String, Object> params = new HashMap<>();
        params.put("usercode", arcID);
        params.put("recent", recent);// 可选
        return getStrFromHJ("userinfo", params);
    }

    /*
{
    "status": 0,
    "content": {
        "song_id": "grievouslady",
        "difficulty": 2,
        "score": 0,
        "shiny_perfect_count": 0,
        "perfect_count": 0,
        "near_count": 0,
        "miss_count": 0,
        "health": 0,
        "modifier": 0,
        "time_played": 1145141145141,
        "best_clear_type": 0,
        "clear_type": 0,
        "character": 0,
        "is_skill_sealed": false,
        "is_char_uncapped": false,
        "rating": 0.0000
    }
}
     */

    /**
     * 玩家单曲最佳记录.
     * status可能返回的值以及含义如下：
     * status	description
     * 0	everything is OK
     * -1	invalid usercode
     * -2	invalid songname
     * -3	invalid difficulty
     * -4	invalid difficulty (map format failed)
     * -5	this song is not recorded in the database
     * -6	too many records
     * -7	internal error
     * -8	this song has no beyond level
     * -9	allocate an arc account failed
     * -10	clear friend list failed
     * -11	add friend failed
     * -12	internal error occurred
     * -13	internal error occurred
     * -14	not played yet
     * -233	unknown error occurred
     *
     * @param arcID      arcID，因为有0开头的id，所以使用String
     * @param songName   歌曲名，支持模糊搜索
     * @param difficulty 难度，0/1/2/3 or pst/prs/ftr/byn or past/present/future/beyond
     * @return 玩家单曲最佳记录
     */
    public static String getUserBest(String arcID, String songName, String difficulty) {
        Map<String, Object> params = new HashMap<>();
        params.put("usercode", arcID);
        params.put("songname", songName);
        params.put("difficulty", difficulty);
        return getStrFromHJ("userbest", params);
    }

    /*
{
    "status": 0,
    "content": {
        "best30_avg": 0.0000,
        "recent10_avg": 0.0000,
        "best30_list": [
            {
                "song_id": "grievouslady",
                "difficulty": 2,
                "score": 0,
                "shiny_perfect_count": 0,
                "perfect_count": 0,
                "near_count": 0,
                "miss_count": 0,
                "health": 0,
                "modifier": 0,
                "time_played": 114514145141,
                "best_clear_type": 0,
                "clear_type": 0,
                "character": 0,
                "is_skill_sealed": false,
                "is_char_uncapped": false,
                "rating": 0.0000
            },
            // more data....
        ]
    }
}
     */

    /**
     * 玩家b30.
     * ps1：隐藏ptt（即灰框）时，recent10_avg = 0
     * ps2：best30_list 有可能少于 30 个（通常出现于新玩家）
     * status可能返回的值以及含义如下：
     * status	description
     * 0	everything is OK
     * -1	invalid usercode
     * -2	allocate an arc account failed
     * -3	clear friend list failed
     * -4	add friend failed
     * -5	internal error occurred
     * -6	not played yet
     * -7	internal error occurred
     * -8	internal error occurred
     * -9	internal error occurred
     * -10	internal error occurred
     * -11	querying best30 failed
     * -12	internal error occurred
     * -13	querying best30 failed
     * -233	unknown error occurred
     *
     * @param arcID arcID，因为有0开头的id，所以使用String
     * @return 玩家b30
     */
    public static String getUserBest30(String arcID) {
        Map<String, Object> params = new HashMap<>();
        params.put("usercode", arcID);
        return getStrFromHJ("userbest30", params);
    }

    /*
{
    "status": 0,
    "content": {
        "id": "ifi",
        "title_localized": {
            "en": "#1f1e33"
        },
        "artist": "かめりあ(EDP)",
        "bpm": "181",
        "bpm_base": 181,
        "set": "vs",
        "audioTimeSec": 163,
        "side": 1,
        "remote_dl": true,
        "world_unlock": false,
        "date": 1590537604,
        "difficulties": [
            {
                "ratingClass": 0,
                "chartDesigner": "夜浪",
                "jacketDesigner": "望月けい",
                "rating": 5,
                "ratingReal": 5.5
            },
            {
                "ratingClass": 1,
                "chartDesigner": "夜浪",
                "jacketDesigner": "望月けい",
                "rating": 9,
                "ratingReal": 9.2
            },
            {
                "ratingClass": 2,
                "chartDesigner": "夜浪 VS 東星 \"Convergence\"",
                "jacketDesigner": "望月けい",
                "rating": 10,
                "ratingReal": 10.9,
                "ratingPlus": true
            }
        ]
    }
}
     */

    /**
     * 单曲信息.
     * ps1：title_localized.ja 可能为 null，但 title_localized.en 一定存在
     * status可能返回的值以及含义如下：
     * status	description
     * 0	everything is OK
     * -1	invalid songname
     * -2	this song is not recorded in the database
     * -3	too many records
     * -233	unknown error occurred
     *
     * @param songName 歌曲名，支持模糊搜索
     * @return 单曲信息
     */
    public static String getSongInfo(String songName) {
        Map<String, Object> params = new HashMap<>();
        params.put("songname", songName);
        return getStrFromHJ("songinfo", params);
    }

    /*
{
    "status": 0,
    "content": {
        "id": "grievouslady"
    }
}
     */

    /**
     * 单曲id.
     * ps1：仅返回单曲id。如果想获取详细信息，请使用 String songInfo(String songName)
     * status可能返回的值以及含义如下：
     * status	description
     * 0	everything is OK
     * -1	invalid songname
     * -2	this song is not recorded in the database
     * -3	too many records
     * -233	unknown error occurred
     *
     * @param songName 歌曲名，支持模糊搜索
     * @return 单曲id
     */
    public static String getSongID(String songName) {
        Map<String, Object> params = new HashMap<>();
        params.put("songname", songName);
        return getStrFromHJ("songalias", params);
    }

    /*
{
    "status": 0,
    "content": {
        "id": "ifi",
        "rating_class": 2,
        "song_info": {
            "id": "ifi",
            "title_localized": {
                "en": "#1f1e33"
            },
            "artist": "かめりあ(EDP)",
            "bpm": "181",
            "bpm_base": 181,
            "set": "vs",
            "audioTimeSec": 163,
            "side": 1,
            "remote_dl": true,
            "world_unlock": false,
            "date": 1590537604,
            "difficulties": [
                {
                    "ratingClass": 0,
                    "chartDesigner": "夜浪",
                    "jacketDesigner": "望月けい",
                    "rating": 5,
                    "ratingReal": 5.5
                },
                {
                    "ratingClass": 1,
                    "chartDesigner": "夜浪",
                    "jacketDesigner": "望月けい",
                    "rating": 9,
                    "ratingReal": 9.2
                },
                {
                    "ratingClass": 2,
                    "chartDesigner": "夜浪 VS 東星 \"Convergence\"",
                    "jacketDesigner": "望月けい",
                    "rating": 10,
                    "ratingReal": 10.9,
                    "ratingPlus": true
                }
            ]
        }
    }
}
     */

    /**
     * 随机选曲.
     * ps1：访问时传入的参数级别是2-23，代表1、1+、2、2+...11、11+
     * ps2：content.song_info 的数据与使用 String songInfo(String songName) 获得的数据是一致的
     * status 可能返回的值以及含义如下：
     * status	description
     * 0	everything is OK
     * -1	invalid range of start
     * -2	invalid range of end
     * -3	internal error
     * -4	internal error
     * -233	unknown error occurred
     *
     * @param minLv          等级下限
     * @param minPlus        下限是否增加0.7
     * @param maxLv          等级上限
     * @param maxPlus        上限是否增加0.7
     * @param returnSongInfo 是否需要返回 songInfo
     * @return 随机选曲
     */
    public static String getRandomSong(int minLv, boolean minPlus,
                                       int maxLv, boolean maxPlus, boolean returnSongInfo) {
        Map<String, Object> params = new HashMap<>();
        int min = minLv * 2 + (minPlus ? 1 : 0);
        int max = maxLv * 2 + (maxPlus ? 1 : 0);
        if (min > max) {
            min ^= max;
            max ^= min;
            min ^= max;
        }
        params.put("start", min);// 没有end情况下，可选
        params.put("end", max);// 可选
        params.put("info", returnSongInfo);// 可选
        return getStrFromHJ("random", params);
    }

    /*
{
    "status": 0,
    "content": {
        "key": "hzfxlxm"
    }
}
     */

    /**
     * 当前 arc connect 码.
     * ps1：网址是 https://lowest.world/connect.
     * status 可能返回的值以及含义如下：
     * status	description
     * 0	everything is OK
     *
     * @return 当前 arc connect 码
     * @deprecated 使用 {@link #getConnectCodeStr()} 代替
     */
    @Deprecated
    public static String getConnectCode() {
        Map<String, Object> params = new HashMap<>();
        return getStrFromHJ("connect", params);
    }

    /**
     * 计算当前 arc connect 码.
     *
     * @return 当前 arc connect 码
     */
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


    /**
     * 获取歌曲名
     *
     * @param songID 歌曲标识，如 ifi
     * @return 歌曲名，如 #1f1e33
     */
    public static String getSongNameByID(String songID) {
        String songName = getString(getSongInfoFile(), songID);
        if (!songName.equals(DEF_STRING)) {
            return songName;
        }
        try {
            JSONObject obj = new JSONObject(getSongInfo(songID));
            songName = obj.getJSONObject("content").getJSONObject("title_localized")
                    .getString("en");
            set(getSongInfoFile(), songID, songName);
            return songName;
        } catch (JSONException e) {
            logError(e);
        }
        return songID;
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
            "光", "对立", "红", "萨菲亚", "忘却",
            "光 & 对立", "对立（Axium）", "对立（Grievous Lady）", "星", "光 & 菲希卡",
            "依莉丝", "爱托", "露娜", "调", "光（Zero）",
            "光（Fracture）", "光（夏）", "对立（夏）", "对立 & 托凛", "彩梦",
            "爱托 & 露娜（冬日）", "梦", "光 & 晴音", "咲弥", "对立 & 中二企鹅（Grievous Lady）",
            "中二企鹅", "榛名", "诺诺", "潘多拉涅墨西斯（MTA-XXX）", "轩辕十四（MDA-21）",
            "群愿", "光（Fantasia）", "对立（Sonata）", "兮娅", "DORO*C",
            "对立（Tempest）", "布丽兰特", "依莉丝（夏）", "咲弥（Etude）", "爱丽丝 & 坦尼尔",
            "露娜 & 美亚", "阿莱乌斯", "希尔", "伊莎贝尔", "未知（请找萌泪添加）",
            "未知（请找萌泪添加）", "未知（请找萌泪添加）", "未知（请找萌泪添加）", "未知（请找萌泪添加）", "未知（请找萌泪添加）",
            "未知（请找萌泪添加）", "未知（请找萌泪添加）", "未知（请找萌泪添加）", "未知（请找萌泪添加）", "未知（请找萌泪添加）",
            "未知（请找萌泪添加）", "未知（请找萌泪添加）", "未知（请找萌泪添加）", "未知（请找萌泪添加）", "未知（请找萌泪添加）",
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

    private static File getSongListFile() {
        return getFile(getRootDir(), "songList.ser");
    }

    static SongList getSongList(boolean unlockAtMethodEnd) {
        SongList songList = deserialize(getSongListFile(), SongList.class, unlockAtMethodEnd);
        if (songList == null) {
            songList = new SongList();
            save(songList);
        }
        return songList;
    }

    static SongList getSongList() {
        return getSongList(false);
    }

    static void save(SongList songList) {
        serialize(songList, getSongListFile());
    }

    static File getSongInfoFile() {
        return getFile(getRootDir(), "songInfo.ser");
    }

}
