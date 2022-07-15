package medic.func.common.arc.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MengLeiFudge
 */
public class SongList implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ArrayList<SongInfo> list = new ArrayList<>();// 歌曲信息列表

    /**
     * 不建议在此处初始化.
     * 应使用 {@link Process#update()} 进行初始化.
     */
    SongList() {
    }

    public void add(String songID, String songName, int difficulty, double songRealRating) {
        int i = 0;
        for (; i < list.size(); i++) {
            SongInfo info = list.get(i);
            if (info.getDifficulty() == difficulty && info.getSongID().equals(songID)) {
                list.set(i, new SongInfo(songID, songName, difficulty, songRealRating));
                return;
            }
        }
        list.add(new SongInfo(songID, songName, difficulty, songRealRating));
    }

    public List<SongInfo> getListSortByRate() {
        list.sort((o1, o2) -> Double.compare(o2.getSongRate(), o1.getSongRate()));
        return list;
    }
}
