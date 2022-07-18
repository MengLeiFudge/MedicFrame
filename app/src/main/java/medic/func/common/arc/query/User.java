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
    private final ArrayList<SongRecord> list = new ArrayList<>();
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

    public void addSongRecord(SongInfo info, int difficulty,
                              int score, int shinyPure, int pure, int far, int lost,
                              int clearType, long timePlayed, double songRating) {
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            SongRecord previousRecord = list.get(i);
            SongInfo songInfo = previousRecord.getSongInfo();
            if (songInfo.equals(info) && previousRecord.getDifficulty() == difficulty) {
                index = i;
            }
        }
        SongRecord newRecord = new SongRecord(info, difficulty,
                score, shinyPure, pure, far, lost, clearType, timePlayed, songRating);
        if (index == -1) {
            list.add(newRecord);
        } else {
            list.set(index, newRecord);
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
    public List<SongRecord> getListSortedBySongRating() {
        list.sort((o1, o2) -> Double.compare(o2.getSongInfo().getRating()[o2.getDifficulty()],
                o1.getSongInfo().getRating()[o1.getDifficulty()]));
        return list;
    }

    /**
     * 按单曲游玩ptt从高到低排序.
     *
     * @return 排序后的单曲成绩
     */
    public List<SongRecord> getListSortedBySongPtt() {
        list.sort((o1, o2) -> Double.compare(o2.getSongPtt(), o1.getSongPtt()));
        return list;
    }

    public double getB30Floor() {
        int index = Math.min(29, list.size() - 1);
        return list.get(index).getSongPtt();
    }
}
