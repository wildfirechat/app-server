package cn.wildfirechat.app;

import cn.wildfirechat.sdk.AdminConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private IMConfig mIMConfig;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if(StringUtils.isEmpty(mIMConfig.admin_user_id)) {
            mIMConfig.admin_user_id = "admin";
        }
        AdminConfig.initAdmin(mIMConfig.admin_url, mIMConfig.admin_secret);

        System.out.println("========================================");
        System.out.println("=     App Server started successfully  =");
        System.out.println("========================================");
    }
}
