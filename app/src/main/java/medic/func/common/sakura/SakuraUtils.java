package medic.func.common.sakura;

import medic.core.Utils;
import medic.func.common.sakura.character.player.Player;

import java.io.File;

import static medic.core.Utils.deserialize;
import static medic.core.Utils.getFile;
import static medic.core.Utils.serialize;

/**
 * @author MengLeiFudge
 */
public class SakuraUtils {
    private SakuraUtils() {
    }

    static File getRootDir() {
        return getFile(Utils.Dir.DATA, "sakuraCity");
    }

    private static File getPlayerDir() {
        return getFile(getRootDir(), "player");
    }

    private static File getPlayerFile(long qq) {
        return getFile(getPlayerDir(), qq + ".ser");
    }

    static Player getPlayer(long qq) {
        return deserialize(getPlayerFile(qq), Player.class);
    }

    static void save(Player player) {
        serialize(player, getPlayerFile(player.getQq()));
    }

}
