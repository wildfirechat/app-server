package cn.wildfirechat.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix="im")
@PropertySource(value = "file:config/im.properties", encoding = "UTF-8")
public class IMConfig {
    String admin_url;
    String admin_secret;

    public boolean isUse_random_name() {
        return use_random_name;
    }

    public void setUse_random_name(boolean use_random_name) {
        this.use_random_name = use_random_name;
    }

    boolean use_random_name;
    String welcome_for_new_user;
    String welcome_for_back_user;

    public String getAdmin_url() {
        return admin_url;
    }

    public void setAdmin_url(String admin_url) {
        this.admin_url = admin_url;
    }

    public String getAdmin_secret() {
        return admin_secret;
    }

    public void setAdmin_secret(String admin_secret) {
        this.admin_secret = admin_secret;
    }

    public String getWelcome_for_new_user() {
        return welcome_for_new_user;
    }

    public void setWelcome_for_new_user(String welcome_for_new_user) {
        this.welcome_for_new_user = welcome_for_new_user;
    }

    public String getWelcome_for_back_user() {
        return welcome_for_back_user;
    }

    public void setWelcome_for_back_user(String welcome_for_back_user) {
        this.welcome_for_back_user = welcome_for_back_user;
    }
}
