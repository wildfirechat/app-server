package cn.wildfirechat.app.shiro;


import cn.wildfirechat.app.RestResult;
import cn.wildfirechat.app.jpa.ShiroSession;
import cn.wildfirechat.app.jpa.UserPassword;
import cn.wildfirechat.app.jpa.UserPasswordRepository;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.credential.Sha1CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.crypto.hash.Sha1Hash;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class UserPasswordRealm extends AuthorizingRealm {
    @Autowired
    private UserPasswordRepository userPasswordRepository;

    @PostConstruct
    private void initMatcher() {
        Sha1CredentialsMatcher matcher = new Sha1CredentialsMatcher();
        matcher.setStoredCredentialsHexEncoded(false);
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
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        if (authenticationToken instanceof UsernamePasswordToken) {
            String userId = (String) authenticationToken.getPrincipal();
            Optional<UserPassword> optional = userPasswordRepository.findById(userId);
            if (optional.isPresent()) {
                UserPassword up = optional.get();
                return new SimpleAuthenticationInfo(authenticationToken.getPrincipal(), up.getPassword(), ByteSource.Util.bytes(up.getSalt().getBytes(StandardCharsets.UTF_8)), getName());
            }
        }

        throw new AuthenticationException("没有密码");
    }
}