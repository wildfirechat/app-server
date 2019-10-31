package cn.wildfirechat.app.tools;

import org.springframework.stereotype.Component;

@Component
public class PhoneNumberUserNameGenerator implements UserNameGenerator {
    @Override
    public String getUserName(String phone) {
        return phone;
    }
}
