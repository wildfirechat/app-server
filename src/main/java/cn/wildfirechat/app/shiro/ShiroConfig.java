package cn.wildfirechat.app.shiro;


import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {

    @Autowired
    DBSessionDao dbSessionDao;

    @Bean(name = "shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        shiroFilterFactoryBean.setLoginUrl("/login");
        shiroFilterFactoryBean.setUnauthorizedUrl("/notRole");
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();

        // <!-- authc:所有url都必须认证通过才可以访问; anon:所有url都都可以匿名访问-->
        filterChainDefinitionMap.put("/send_code", "anon");
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/pc_session", "anon");

        filterChainDefinitionMap.put("/session_login/**", "anon");
        filterChainDefinitionMap.put("/user/online_event", "anon");
        filterChainDefinitionMap.put("/logs/**", "anon");
        filterChainDefinitionMap.put("/", "anon");

        filterChainDefinitionMap.put("/confirm_pc", "login");
        filterChainDefinitionMap.put("/scan_pc/**", "login");
        filterChainDefinitionMap.put("/put_group_announcement", "login");
        filterChainDefinitionMap.put("/get_group_announcement", "login");
        filterChainDefinitionMap.put("/things/add_device", "login");
        filterChainDefinitionMap.put("/things/list_device", "login");

        //主要这行代码必须放在所有权限设置的最后，不然会导致所有 url 都被拦截 剩余的都需要认证
        filterChainDefinitionMap.put("/**", "login");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        shiroFilterFactoryBean.getFilters().put("login", new JsonAuthLoginFilter());
        return shiroFilterFactoryBean;

    }

    @Bean
    public SecurityManager securityManager() {
        DefaultWebSecurityManager defaultSecurityManager = new DefaultWebSecurityManager();
        defaultSecurityManager.setRealms(Arrays.asList(phoneCodeRealm, scanCodeRealm));
        ShiroSessionManager sessionManager = new ShiroSessionManager();
        sessionManager.setGlobalSessionTimeout(Long.MAX_VALUE);
        sessionManager.setSessionDAO(dbSessionDao);
        defaultSecurityManager.setSessionManager(sessionManager);
        return defaultSecurityManager;
    }



    @Autowired
    private PhoneCodeRealm phoneCodeRealm;

    @Autowired
    private ScanCodeRealm scanCodeRealm;

}