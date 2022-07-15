package medic.func.common.arc.werewolf;

import medic.core.Utils;

import java.io.File;

import static medic.core.Utils.deserialize;
import static medic.core.Utils.getFile;
import static medic.core.Utils.serialize;

/**
 * @author MengLeiFudge
 */
public class WerewolfUtils {
    private WerewolfUtils() {
    }

    public enum Role {
        // 狼人
        WEREWOLVES,
        // 狼人
        WOLF,
        WOLF1,
        NVWU,
        LIEREN,
        YUYANJIA,
        YUZHE,

    }


    static File getRootDir() {
        return getFile(Utils.Dir.DATA, "arc_werewolf");
    }

    private static File getUserDir() {
        return getFile(getRootDir(), "user");
    }

    private static File getUserFile(long qq) {
        return getFile(getUserDir(), qq + ".ser");
    }

    static Player getUser(long qq) {
        return deserialize(getUserFile(qq), Player.class);
    }

    static void save(Player player) {
        serialize(player, getUserFile(player.getQq()));
    }

}
