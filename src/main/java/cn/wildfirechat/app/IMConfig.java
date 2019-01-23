package cn.wildfirechat.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix="im")
@PropertySource(value = "file:config/im.properties")
public class IMConfig {
    String admin_url;
    String admin_secret;

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
}
