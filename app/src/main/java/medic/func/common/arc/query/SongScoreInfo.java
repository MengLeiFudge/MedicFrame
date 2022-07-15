package medic.func.common.arc.query;

import lombok.Data;

import java.io.Serializable;

/**
 * @author MengLeiFudge
 */
@Data
public class SongScoreInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private SongInfo songInfo;
    private int score;
    private int shinyPure;
    private int pure;
    private int far;
    private int lost;
    private int clearType;
    private long timePlayed;
    private double songPtt;

    SongScoreInfo(SongInfo songInfo,
                  int score, int shinyPure, int pure, int far, int lost,
                  int clearType, long timePlayed, double songPtt) {
        this.songInfo = songInfo;
        this.score = score;
        this.shinyPure = shinyPure;
        this.pure = pure;
        this.far = far;
        this.lost = lost;
        this.clearType = clearType;
        this.timePlayed = timePlayed;
        this.songPtt = songPtt;
    }

    public int getNote() {
        return pure + far + lost;
    }
}
