package medic.func.common.kun;

import java.util.ArrayList;
import java.util.List;

import static medic.func.common.kun.KunUtils.getUserList;

/**
 * @author MengLeiFudge
 */
public class Rank {
    private Rank() {
    }

    static List<User> getListSortedByLevel() {
        ArrayList<User> list = (ArrayList<User>) getUserList();
        list.sort((o1, o2) -> o2.getLevel() - o1.getLevel());
        return list;
    }

    static List<User> getListSortedByMoney() {
        ArrayList<User> list = (ArrayList<User>) getUserList();
        list.sort((o1, o2) -> o2.getMoney() - o1.getMoney());
        return list;
    }

    static int getMaxLevel() {
        return getListSortedByLevel().get(0).getLevel();
    }

    static int getSize() {
        ArrayList<User> list = (ArrayList<User>) getUserList();
        return list.size();
    }
}
