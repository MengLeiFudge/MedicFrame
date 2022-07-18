package medic.func.common.arc.query;

import lombok.Data;

import java.io.Serializable;

/**
 * @author MengLeiFudge
 */
@Data
public class SongRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private SongInfo songInfo;
    /**
     * 难度级别，0123
     */
    private int difficulty;
    private int score;
    private int shinyPure;
    private int pure;
    private int far;
    private int lost;
    private int clearType;
    private long timePlayed;
    private double songPtt;

    SongRecord(SongInfo songInfo, int difficulty,
               int score, int shinyPure, int pure, int far, int lost,
               int clearType, long timePlayed, double songPtt) {
        this.songInfo = songInfo;
        this.difficulty = difficulty;
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
