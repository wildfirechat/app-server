package cn.wildfirechat.app.shiro;


import cn.wildfirechat.app.jpa.UserPassword;
import cn.wildfirechat.app.jpa.UserPasswordRepository;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.Sha1CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class LdapRealm extends AuthorizingRealm {
    @Autowired
    private UserPasswordRepository userPasswordRepository;

    @PostConstruct
    private void initMatcher() {
        LdapMatcher matcher = new LdapMatcher();
        setCredentialsMatcher(matcher);
    }
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
//        Set<String> stringSet = new HashSet<>();
//        stringSet.add("user:show");
//        stringSet.add("user:admin");
//        info.setStringPermissions(stringSet);
        return info;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        if (token instanceof LdapToken)
            return true;
        return super.supports(token);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        if (authenticationToken instanceof LdapToken) {
            String userId = (String) authenticationToken.getPrincipal();
            return new SimpleAuthenticationInfo(authenticationToken.getPrincipal(), authenticationToken.getCredentials(), getName());
        }
        throw new AuthenticationException("没有密码");
    }
}