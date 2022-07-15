package medic.func.common.arc.query;

import lombok.Data;

import java.io.Serializable;

/**
 * @author MengLeiFudge
 */
@Data
public class SongInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    String songID;
    String songName;
    int difficulty;
    double songRate;

    SongInfo(String songID, String songName, int difficulty, double songRate) {
        this.songID = songID;
        this.songName = songName;
        this.difficulty = difficulty;
        this.songRate = songRate;
    }
}
