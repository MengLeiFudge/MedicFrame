package medic.func.common.pcr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static medic.core.Api.msgTime;
import static medic.core.Utils.Dir;
import static medic.core.Utils.deserialize;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getLong;
import static medic.core.Utils.getZeroHourTimestamp;
import static medic.core.Utils.lock;
import static medic.core.Utils.serialize;
import static medic.core.Utils.set;
import static medic.core.Utils.unlock;

/**
 * @author MengLeiFudge
 */
public class PcrUtils {
    private PcrUtils() {
    }

    static File getRootDir() {
        return getFile(Dir.DATA, "pcr");
    }

    private static File getTimeFile() {
        return getFile(getRootDir(), "time.txt");
    }

    static long getLastFiveHour(long nowTime) {
        long todayFiveHour = getZeroHourTimestamp(nowTime) + 5 * 60 * 60 * 1000;
        if (nowTime < todayFiveHour) {
            return todayFiveHour - 24 * 60 * 60 * 1000;
        } else {
            return todayFiveHour;
        }
    }

    static void updateIfOverFiveHour() {
        lock(getTimeFile());
        try {
            long lastTime = getLong(getTimeFile(), "lastTime");
            if (lastTime == getLastFiveHour(msgTime)) {
                return;
            }
            set(getTimeFile(), "lastTime", getLastFiveHour(msgTime));
            File[] groupDirs = getRootDir().listFiles();
            if (groupDirs == null) {
                return;
            }
            for (File groupDir : groupDirs) {
                if (!groupDir.isDirectory()) {
                    continue;
                }
                File userDir = getUserDir(groupDir);
                if (userDir.isDirectory()) {
                    File[] userFiles = userDir.listFiles();
                    if (userFiles != null) {
                        for (File userFile : userFiles) {
                            User user = deserialize(userFile, User.class);
                            if (user != null) {
                                user.setHaveSl(true);
                                save(user);
                            }
                        }
                    }
                }
                File listFile = getBossUserListFile(groupDir);
                if (listFile.isFile()) {
                    BossUserList list = deserialize(listFile, BossUserList.class);
                    if (list != null) {
                        list.getApplyList().clear();
                        save(list);
                    }
                }
            }
        } finally {
            unlock(getTimeFile());
        }
    }

    private static File getUserDir(long group) {
        return getFile(getRootDir(), group + "", "members");
    }

    private static File getUserDir(File groupDir) {
        return getFile(groupDir, "members");
    }

    private static File getUserFile(long group, long qq) {
        return getFile(getUserDir(group), qq + ".ser");
    }

    static User getUser(long group, long qq) {
        User user = deserialize(getUserFile(group, qq), User.class);
        if (user == null) {
            user = new User(group, qq);
            save(user);
        }
        return user;
    }

    static List<User> getUserList(long group) {
        List<User> list = new ArrayList<>();
        File[] files = getUserDir(group).listFiles();
        if (files != null) {
            for (File f : files) {
                User user = deserialize(f, User.class);
                if (user != null) {
                    list.add(user);
                }
            }
        }
        return list;
    }

    static void save(User user) {
        serialize(user, getUserFile(user.getGroup(), user.getQq()));
    }

    private static File getBossFile(long group) {
        return getFile(getRootDir(), group + "", "boss.ser");
    }

    static Boss getBoss(long group) {
        Boss boss = deserialize(getBossFile(group), Boss.class);
        if (boss == null) {
            boss = new Boss(group);
            save(boss);
        }
        return boss;
    }

    static void save(Boss boss) {
        serialize(boss, getBossFile(boss.getGroup()));
    }

    private static File getBossUserListFile(long group) {
        return getFile(getRootDir(), group + "", "bossUserList.ser");
    }

    private static File getBossUserListFile(File groupDir) {
        return getFile(groupDir, "bossUserList.ser");
    }

    static BossUserList getBossUserList(long group) {
        BossUserList list = deserialize(getBossUserListFile(group), BossUserList.class);
        if (list == null) {
            list = new BossUserList(group);
            save(list);
        }
        return list;
    }

    static void save(BossUserList b) {
        serialize(b, getBossUserListFile(b.getGroup()));
    }

    static File getRankImg(String s) {
        return getFile(getRootDir(), "rank/" + s + ".jpg");
    }
}
