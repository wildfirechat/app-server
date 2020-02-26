package cn.wildfirechat.app.shiro;

import org.apache.shiro.authc.AuthenticationToken;

public class TokenAuthenticationToken implements AuthenticationToken {
    private String token;

    public TokenAuthenticationToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return null;
    }
}
