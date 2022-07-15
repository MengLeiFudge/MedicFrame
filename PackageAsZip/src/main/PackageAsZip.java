package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author MengLeiFudge
 */
public class PackageAsZip {
    public static void main(String[] args) {
        File mfDir = new File("../");
        File zipFile = new File("../../MedicFrame" + getVersion(mfDir) + ".zip");
        File[] gitIgnoreFiles = {
                new File("../.idea/.gitignore"),
                new File("../app/.gitignore"),
                new File("../.gitignore"),
                new File(".idea/.gitignore"),
        };
        zip(mfDir, zipFile, gitIgnoreFiles);
    }

    private static String getVersion(File mfDir) {
        String lastModifyDate = "";
        try (BufferedReader br = new BufferedReader(new FileReader(
                new File(mfDir, "app/src/main/java/medic/core/Main.java")))) {
            String s;
            while ((s = br.readLine()) != null) {
                if (s.contains("LAST_MODIFY_DATE")) {
                    lastModifyDate = s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\""));
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("未找到 Main.java 文件，或读取该文件异常");
        }
        if (lastModifyDate.equals("")) {
            throw new IllegalArgumentException("未从 Main.java 文件中获取到日期");
        }
        Date date;
        try {
            date = new SimpleDateFormat("yyyy-MMdd-HHmm", Locale.CHINA).parse(lastModifyDate);
        } catch (ParseException e) {
            throw new IllegalArgumentException("DATE(" + lastModifyDate + ")格式错误，应为 yyyy-MMdd-HHmm");
        }
        // 分钟时间戳
        assert date != null;
        int minuteTimespan = (int) (date.getTime() / 60000);
        return new SimpleDateFormat("yyMMdd", Locale.CHINA).format(date) + "." + minuteTimespan;
    }

    /**
     * @param srcFileOrDir   要压缩的文件
     * @param zipFile        压缩文件存放地方
     * @param gitIgnoreFiles git忽略文件的位置
     */
    private static void zip(File srcFileOrDir, File zipFile, File[] gitIgnoreFiles) {
        List<File> gitIgnoreFileList = new ArrayList<>();
        try {
            srcFileOrDir = srcFileOrDir.getCanonicalFile();
            zipFile = zipFile.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (File gitIgnoreFile : gitIgnoreFiles) {
            if (!gitIgnoreFile.exists()) {
                continue;
            }
            File parentDir = gitIgnoreFile.getParentFile();
            try (BufferedReader br = new BufferedReader(new FileReader(gitIgnoreFile))) {
                String s;
                while ((s = br.readLine()) != null) {
                    if ("".equals(s) || s.startsWith("#")) {
                        continue;
                    }
                    gitIgnoreFileList.add(new File(parentDir, s).getCanonicalFile());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipFile(outputStream, srcFileOrDir, gitIgnoreFileList, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param zos          ZipOutputStream对象
     * @param srcFileOrDir 要压缩的文件或文件夹
     * @param basePath     条目根目录
     */
    private static void zipFile(ZipOutputStream zos, File srcFileOrDir,
                                List<File> gitIgnoreFileList, String basePath) throws IOException {
        if (srcFileOrDir.isDirectory()) {
            for (File f : gitIgnoreFileList) {
                if (srcFileOrDir.getAbsolutePath().equals(f.getAbsolutePath())) {
                    System.out.println("忽略目录：" + f.getAbsolutePath());
                    return;
                }
            }
            basePath = basePath + (basePath.length() == 0 ? "" : "/") + srcFileOrDir.getName();
            System.out.println("zip中的文件夹路径：" + basePath);
            for (File f : Objects.requireNonNull(srcFileOrDir.listFiles())) {
                zipFile(zos, f, gitIgnoreFileList, basePath);
            }
        } else {
            for (File f : gitIgnoreFileList) {
                if (srcFileOrDir.getAbsolutePath().equals(f.getAbsolutePath())) {
                    System.out.println("忽略文件：" + f.getAbsolutePath());
                    return;
                }
            }
            basePath = (basePath.length() == 0 ? "" : basePath + "/") + srcFileOrDir.getName();
            System.out.println("zip中的文件路径：" + basePath);
            zos.putNextEntry(new ZipEntry(basePath));
            try (FileInputStream input = new FileInputStream(srcFileOrDir)) {
                int readLen;
                byte[] buffer = new byte[1024 * 8];
                while ((readLen = input.read(buffer, 0, 1024 * 8)) != -1) {
                    zos.write(buffer, 0, readLen);
                }
            }
        }
    }
}