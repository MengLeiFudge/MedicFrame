package medic.func.common.arc.query;

import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;

import static medic.core.Utils.logError;

/**
 * @author MengLeiFudge
 */
@Data
public class SongInfo implements Serializable, Comparable<SongInfo> {
    private static final long serialVersionUID = 1L;

    private String song_id;
    private String[] name_en = new String[4];
    private String[] name_jp = new String[4];
    private String[] artist = new String[4];
    private String[] bpm = new String[4];
    private double[] bpm_base = new double[4];
    private String[] set = new String[4];
    private String[] set_friendly = new String[4];
    private int[] time = new int[4];
    private int[] side = new int[4];
    private boolean[] world_unlock = new boolean[4];
    private boolean[] remote_download = new boolean[4];
    private String[] bg = new String[4];
    private long[] date = new long[4];
    private String[] version = new String[4];
    private int[] difficulty = new int[4];
    private int[] rating = new int[4];
    private int[] note = new int[4];
    private String[] chart_designer = new String[4];
    private String[] jacket_designer = new String[4];
    private boolean[] jacket_override = new boolean[4];
    private boolean[] audio_override = new boolean[4];

    /**
     * 指示是否通过content构建成功.
     */
    private boolean isOk = true;

    SongInfo(JSONObject content) {
        try {
            song_id = content.getString("song_id");
            JSONArray difficulties = content.getJSONArray("difficulties");
            for (int i = 0; i < difficulties.length(); i++) {
                JSONObject obj = difficulties.getJSONObject(i);
                name_en[i] = obj.getString("name_en");
                name_jp[i] = obj.getString("name_jp");
                artist[i] = obj.getString("artist");
                bpm[i] = obj.getString("bpm");
                bpm_base[i] = obj.getDouble("bpm_base");
                set[i] = obj.getString("set");
                set_friendly[i] = obj.getString("set_friendly");
                time[i] = obj.getInt("time");
                side[i] = obj.getInt("side");
                world_unlock[i] = obj.getBoolean("world_unlock");
                remote_download[i] = obj.getBoolean("remote_download");
                bg[i] = obj.getString("bg");
                date[i] = obj.getLong("date");
                version[i] = obj.getString("version");
                difficulty[i] = obj.getInt("difficulty");
                rating[i] = obj.getInt("rating");
                note[i] = obj.getInt("note");
                chart_designer[i] = obj.getString("chart_designer");
                jacket_designer[i] = obj.getString("jacket_designer");
                jacket_override[i] = obj.getBoolean("jacket_override");
                audio_override[i] = obj.getBoolean("audio_override");
            }
        } catch (Exception e) {
            logError(e);
            isOk = false;
        }
    }

    @Override
    public int compareTo(SongInfo o) {
        return song_id.compareTo(o.song_id);
    }
}
