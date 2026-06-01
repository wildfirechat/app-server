package cn.wildfirechat.app.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class ShortUUIDGenerator implements UserNameGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(ShortUUIDGenerator.class);
    public static String[] chars = new String[] { "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z" };

    @Override
    public String getUserName(String phone) {
        return getShortUUID();
    }

    public String getShortUUID() {
        StringBuffer shortBuffer = new StringBuffer();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            shortBuffer.append(chars[x % chars.length]);
        }
        return shortBuffer.toString();
    }
    public static void main(String[] args) {
        Set<String> idSet = new HashSet<>();
        ShortUUIDGenerator generator = new ShortUUIDGenerator();

        int duplatedCount = 0;
        for (int i = 0; i < 1000000; i++) {
            String id = generator.getUserName(null);
            if(!idSet.add(id)) {
                LOG.info("Duplated id of {}", id);
                duplatedCount++;
            }
        }

        LOG.info("Duplated id count is {}", duplatedCount);
    }
}
