package medic.func.common.arc.pubg;

import medic.core.Utils;

import java.io.File;

import static medic.core.Api.group;
import static medic.core.Utils.getFile;

/**
 * @author MengLeiFudge
 */
public class PUBGUtils {
    private PUBGUtils() {
    }

    static File getRootDir() {
        return getFile(Utils.Dir.DATA, "pubg");
    }

    private static File getPUBGFile() {
        return getFile(getRootDir(), group + "", "pubg.ser");
    }
}
