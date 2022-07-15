package medic.func.common.kun;

import medic.core.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static medic.core.Api.changeAllSourceToPrivate;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Utils.DEF_INT;
import static medic.core.Utils.deleteIfExists;
import static medic.core.Utils.deserialize;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getInt;
import static medic.core.Utils.getRandomDistributionInt;
import static medic.core.Utils.serialize;
import static medic.core.Utils.set;
import static medic.func.common.kun.Rank.getMaxLevel;

/**
 * @author MengLeiFudge
 */
public class KunUtils {
    private KunUtils() {
    }

    static String getGrade(int attributeValue, int level, double min, double max) {
        if (attributeValue == (int) (level * max)) {
            return "MAX";
        }
        if (attributeValue == (int) (level * min)) {
            return "MIN";
        }
        double cha = level * (max - min);
        double overPart = attributeValue - level * min;

        String str;
        if (overPart < cha / 8) {
            str = "E";
        } else if (overPart < cha * 2 / 8) {
            str = "D";
            overPart -= cha / 8;
        } else if (overPart < cha * 3 / 8) {
            str = "C";
            overPart -= cha * 2 / 8;
        } else if (overPart < cha * 4 / 8) {
            str = "B";
            overPart -= cha * 3 / 8;
        } else if (overPart < cha * 5 / 8) {
            str = "A";
            overPart -= cha * 4 / 8;
        } else if (overPart < cha * 6 / 8) {
            str = "S";
            overPart -= cha * 5 / 8;
        } else if (overPart < cha * 7 / 8) {
            str = "SS";
            overPart -= cha * 6 / 8;
        } else {
            str = "SSS";
            overPart -= cha * 7 / 8;
        }
        cha /= 8;
        if (overPart < cha / 3) {
            return str + "-";
        } else if (overPart < cha * 2 / 3) {
            return str;
        } else {
            return str + "+";
        }
    }

    static int getAdd(int level) {
        int maxLevel = getMaxLevel();
        double fj1 = maxLevel * 0.4;
        double fj2 = maxLevel * 0.65;
        double fj3 = maxLevel * 0.9;
        if (level < fj1) {
            return getRandomDistributionInt(400, 800);
        } else if (level < fj2) {
            return getRandomDistributionInt(200, 400);
        } else if (level < fj3) {
            return getRandomDistributionInt(100, 200);
        } else {
            return getRandomDistributionInt(50, 100);
        }
    }

    /**
     * 赛季结算.
     */
    static void seasonSettlement(User u) {
        int nowSeason = getNowSeason();
        if (u.getSeason() == nowSeason) {
            return;
        }
        int level = u.getLevel();
        int newLevel = 0;
        int addMoney = 0;
        for (int i = u.getSeason(); i < nowSeason; i++) {
            newLevel = getNewLv(level);
            addMoney += level / 3;
        }
        u.setSeason(nowSeason);
        u.setLevel(newLevel);
        u.addMoney(addMoney);
        if (u.isOpenNewSeasonTip()) {
            changeAllSourceToPrivate(u.getQq());
            send(u.getQq(), "S" + nowSeason + "赛季现已开启！\n" +
                    u.getName() + "初始等级为" + newLevel + "\n" +
                    "获得萌泪币" + addMoney + "枚\n快去养鲲吧！");
            changeAllSourceToPrivate(qq);
        }
        save(u);
    }

    /**
     * 赛季结算等级继承.
     * <p>
     * 我也不知道自己写了个啥，反正就是分层，等级越低，转换的比例越高
     *
     * @param level 原先鲲的等级
     * @return 继承后的等级
     */
    static int getNewLv(int level) {
        int newLevel = 0;
        int i = 10;
        for (; i > 1; i--) {
            int q = (10 - i) * (10 - i) * 300 + 3000;
            if (level <= q) {
                break;
            } else {
                newLevel += q * i / 10;
                level -= q;
            }
        }
        return newLevel + level * i / 10;
    }


    private static File getRootDir() {
        return getFile(Utils.Dir.DATA, "kun");
    }

    private static File getNowSeasonFile() {
        return getFile(getRootDir(), "nowSeason.txt");
    }

    static int getNowSeason() {
        int a = getInt(getNowSeasonFile(), 1);
        if (a == DEF_INT) {
            set(getNowSeasonFile(), 1, 1);
            a = 1;
        }
        return a;
    }

    /**
     * 开启新赛季.
     */
    static void startNewSeason() {
        set(getNowSeasonFile(), 1, getNowSeason() + 1);
        deleteIfExists(getBossFile());
    }

    private static File getUserDir() {
        return getFile(getRootDir(), "user");
    }

    private static File getUserFile(long qq) {
        return getFile(getUserDir(), qq + ".ser");
    }

    private static void updateUserWhenDeserialize(User user) {
        if (user == null) {
            return;
        }
        seasonSettlement(user);
        //int level = user.getLevel();
        //user.setAtk(Math.max((int) (level * 1.0), Math.min(user.getAtk(), (int) (level * 1.5))));
        //user.setDef(Math.max((int) (level * 0.6), Math.min(user.getDef(), (int) (level * 0.9))));
        //user.setHp(Math.max((int) (level * 4.0), Math.min(user.getHp(), (int) (level * 6.0))));
        //save(user);
    }

    static User getUser(long qq) {
        User user = deserialize(getUserFile(qq), User.class);
        updateUserWhenDeserialize(user);
        return user;
    }

    static List<User> getUserList() {
        List<User> list = new ArrayList<>();
        File[] userFiles = getUserDir().listFiles();
        if (userFiles == null) {
            return list;
        }
        for (File userFile : userFiles) {
            User user = deserialize(userFile, User.class, true);
            updateUserWhenDeserialize(user);
            if (user != null) {
                list.add(user);
            }
        }
        return list;
    }

    static void save(User user) {
        serialize(user, getFile(getUserDir(), user.getQq() + ".ser"));
    }

    static File getBossFile() {
        return getFile(getRootDir(), "boss.ser");
    }

    static Boss getBoss() {
        Boss boss = deserialize(getBossFile(), Boss.class);
        if (boss == null) {
            boss = new Boss();
        }
        return boss;
    }

    static void save(Boss boss) {
        serialize(boss, getBossFile());
    }

}
