package cn.wildfirechat.app.tools;

import java.nio.file.Paths;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String getRandomCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(((int)(Math.random()*100))%10);
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
            e.printStackTrace();
        }
        return UUID.randomUUID().toString();
    }

    public static void main(String[] args) {
        String filename1 = "/aa../../../hello.txt";
        String filename2 = "..\\..\\1.txt";
        System.out.println(getSafeFileName(filename1));
        System.out.println(getSafeFileName(filename2));
    }
}
