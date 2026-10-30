package cn.wildfirechat.app.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    public static String getRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
    public static boolean isMobile(String mobile) {
        boolean flag = false;
        try {
            Pattern p = Pattern.compile("^(1[3-9][0-9])\\d{8}$");
            Matcher m = p.matcher(mobile);
            flag = m.matches();
        } catch (Exception e) {
            flag = false;
        }
        return flag;
    }

    public static String getSafeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return UUID.randomUUID().toString();
        }

        // 使用 Paths.get 解析文件名
        try {
            String newName = Paths.get(fileName).getFileName().toString();
            if(!newName.isEmpty()) {
                return newName;
            }
        } catch (Exception e) {
            // 处理解析异常
            LOG.error("Exception", e);
        }
        return UUID.randomUUID().toString();
    }

    public static void main(String[] args) {
        String filename1 = "/aa../../../hello.txt";
        String filename2 = "..\\..\\1.txt";
        LOG.info("{}", getSafeFileName(filename1));
        LOG.info("{}", getSafeFileName(filename2));
    }
}
