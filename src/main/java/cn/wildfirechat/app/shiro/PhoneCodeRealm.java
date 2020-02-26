package cn.wildfirechat.app.shiro;


import cn.wildfirechat.app.RestResult;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class PhoneCodeRealm extends AuthorizingRealm {

    @Autowired
    AuthDataSource authDataSource;

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
        String mobile = (String) authenticationToken.getPrincipal();
        String code = new String((char[]) authenticationToken.getCredentials());
        RestResult.RestCode restCode = authDataSource.verifyCode(mobile, code);
        if (restCode == RestResult.RestCode.SUCCESS) {
            return new SimpleAuthenticationInfo(mobile, code.getBytes(), getName());
        }

        throw new AccountException("密码不正确");
    }
}