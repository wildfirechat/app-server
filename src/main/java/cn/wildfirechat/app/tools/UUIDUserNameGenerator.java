package cn.wildfirechat.app.tools;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UUIDUserNameGenerator implements UserNameGenerator {
    @Override
    public String getUserName(String phone) {
        return "wfid-" + UUID.randomUUID().toString().replaceAll("-", "");
    }
}
