package medic.func.common.arc.query;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MengLeiFudge
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private long qq;
    private String arcId = "000000000";
    private final ArrayList<SongScoreInfo> list = new ArrayList<>();
    private long queryAllScoreTime = 0L;
    private boolean isQuerying = false;

    User(long qq, String arcId) {
        this.qq = qq;
        this.arcId = arcId;
    }

    public boolean listIsEmpty() {
        return list.isEmpty();
    }

    public int getSize() {
        return list.size();
    }

    public void addSongScoreInfo(SongInfo info,
                                 int score, int shinyPure, int pure, int far, int lost,
                                 int clearType, long timePlayed, double songRating) {
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            SongScoreInfo oldInfo = list.get(i);
            SongInfo songInfo = oldInfo.getSongInfo();
            if (songInfo.getDifficulty() == info.getDifficulty()
                    && songInfo.getSongID().equals(info.getSongID())) {
                index = i;
            }
        }
        SongScoreInfo newInfo = new SongScoreInfo(info,
                score, shinyPure, pure, far, lost, clearType, timePlayed, songRating);
        if (index == -1) {
            list.add(newInfo);
        } else {
            list.set(index, newInfo);
        }
    }

    public void clearAllSongScoreInfo() {
        list.clear();
    }

    /**
     * 按歌曲定数从高到低排序.
     *
     * @return 排序后的单曲成绩
     */
    public List<SongScoreInfo> getListSortedBySongRealRating() {
        list.sort((o1, o2) -> Double.compare(o2.getSongInfo().getSongRate(), o1.getSongInfo().getSongRate()));
        return list;
    }

    /**
     * 按单曲ptt从高到低排序.
     *
     * @return 排序后的单曲成绩
     */
    public List<SongScoreInfo> getListSortedBySongRating() {
        list.sort((o1, o2) -> Double.compare(o2.getSongPtt(), o1.getSongPtt()));
        return list;
    }

    public double getB30Floor() {
        int index = Math.min(29, list.size() - 1);
        return list.get(index).getSongPtt();
    }
}
