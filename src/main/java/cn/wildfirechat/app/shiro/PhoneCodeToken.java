package cn.wildfirechat.app.shiro;

import org.apache.shiro.authc.AuthenticationToken;

public class PhoneCodeToken implements AuthenticationToken {
    final private String phone;
    final private String code;

    public PhoneCodeToken(String phone, String code) {
        this.phone = phone;
        this.code = code;
    }

    @Override
    public Object getPrincipal() {
        return phone;
    }

    @Override
    public Object getCredentials() {
        return code;
    }
}
