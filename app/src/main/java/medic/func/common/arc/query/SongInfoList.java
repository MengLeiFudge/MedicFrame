package medic.func.common.arc.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MengLeiFudge
 */
public class SongInfoList implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ArrayList<SongInfo> list = new ArrayList<>();// 歌曲信息列表

    /**
     * 不建议在此处初始化.
     * 应使用 {@link Process#updateAll()} 进行初始化.
     */
    SongInfoList() {
    }

    public void add(SongInfo info) {
        int idx = list.indexOf(info);
        if (idx == -1) {
            list.add(info);
        } else {
            list.set(idx, info);
        }
    }

    public List<SongInfo> getListSortByRate() {
        list.sort(SongInfo::compareTo);
        return list;
    }
}
