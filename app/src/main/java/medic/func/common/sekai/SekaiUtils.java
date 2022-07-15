package medic.func.common.sekai;

import medic.core.Utils;

import java.io.File;

import static medic.core.Utils.getFile;

/**
 * @author MengLeiFudge
 */
public class SekaiUtils {
    private SekaiUtils() {
    }

    static File getRootDir() {
        return getFile(Utils.Dir.DATA, "sekai");
    }

    static File getLastEventFile() {
        return getFile(getRootDir(), "lastEvent.txt");
    }

    static File getAliveDir() {
        return getFile(getRootDir(), "thread");
    }

    static File getAliveFile(long startTime) {
        return getFile(getAliveDir(), startTime + ".txt");
    }

    static File getDataFile(int lastEvent, String timeStr) {
        return getFile(getRootDir(), "event" + lastEvent, timeStr + ".txt");
    }
}
