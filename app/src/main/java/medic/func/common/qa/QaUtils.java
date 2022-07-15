package medic.func.common.qa;

import medic.core.Utils;

import java.io.File;

import static medic.core.Utils.deserialize;
import static medic.core.Utils.getFile;
import static medic.core.Utils.serialize;

/**
 * @author MengLeiFudge
 */
public class QaUtils {
    private QaUtils() {
    }

    private static File getQaFile() {
        return getFile(Utils.Dir.DATA, "QA", "qaList.ser");
    }

    static QaList getQaList() {
        QaList list = deserialize(getQaFile(), QaList.class);
        if (list == null) {
            list = new QaList();
        }
        return list;
    }

    static void save(QaList list) {
        serialize(list, getQaFile());
    }
}
