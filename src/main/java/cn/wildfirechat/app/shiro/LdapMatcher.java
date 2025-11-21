package cn.wildfirechat.app.shiro;

import cn.wildfirechat.app.tools.LdapUser;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Service
public class LdapMatcher implements CredentialsMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        if (token instanceof LdapToken) {
            try {
                LdapToken tt = (LdapToken)token;
                Hashtable<String,String> env = new Hashtable<>();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                env.put(Context.PROVIDER_URL, tt.getLdapUrl());
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, tt.getPrincipal().toString());
                env.put(Context.SECURITY_CREDENTIALS, tt.getCredentials().toString());
                new InitialDirContext(env).close();  // 能 bind 就算成功
                return true;
            } catch (NamingException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }



    /* --------- 验证入口 --------- */
    public static boolean authenticate(String ldapUrl, String dn, String password) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, dn);
            env.put(Context.SECURITY_CREDENTIALS, password);

            /* 成功 bind 立即关闭 */
            new InitialDirContext(env).close();
            return true;
        } catch (AuthenticationException e) {
            // 密码错或账号不存在
            return false;
        } catch (NamingException e) {
            throw new RuntimeException("LDAP 异常", e);
        }
    }

    private static final String LDAP_URL = "ldap://192.168.1.48:389"; // 换成你的 LDAP 地址
    private static final String USER_DN  = "uid=user6,ou=people,dc=wildfirechat,dc=net";

    public static void main(String[] args) {
        boolean ok = authenticate(LDAP_URL, USER_DN, "123456");   // 与条目里明文一致
        System.out.println(ok ? "登录成功" : "用户名或密码错误");
    }
}
