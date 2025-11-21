package cn.wildfirechat.app.shiro;

import org.apache.shiro.authc.AuthenticationToken;

public class LdapToken implements AuthenticationToken {
    final private String phone;
    final private String password;
    final private String ldapUrl;

    public LdapToken(String phone, String password, String ldapUrl) {
        this.phone = phone;
        this.password = password;
        this.ldapUrl = ldapUrl;
    }

    @Override
    public Object getPrincipal() {
        return phone;
    }

    @Override
    public Object getCredentials() {
        return password;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }
}
