package cn.wildfirechat.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix="im")
@PropertySource(value = "file:config/im.properties", encoding = "UTF-8")
public class IMConfig {
    public String admin_url;
    public String admin_secret;

    public boolean isUse_random_name() {
        return use_random_name;
    }

    public void setUse_random_name(boolean use_random_name) {
        this.use_random_name = use_random_name;
    }

    boolean use_random_name;
    String welcome_for_new_user;
    String welcome_for_back_user;

    boolean new_user_robot_friend;
    String robot_friend_id;
    String robot_welcome;

    String new_user_subscribe_channel_id;
    String back_user_subscribe_channel_id;

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

    public boolean isNew_user_robot_friend() {
        return new_user_robot_friend;
    }

    public void setNew_user_robot_friend(boolean new_user_robot_friend) {
        this.new_user_robot_friend = new_user_robot_friend;
    }

    public String getRobot_friend_id() {
        return robot_friend_id;
    }

    public void setRobot_friend_id(String robot_friend_id) {
        this.robot_friend_id = robot_friend_id;
    }

    public String getRobot_welcome() {
        return robot_welcome;
    }

    public void setRobot_welcome(String robot_welcome) {
        this.robot_welcome = robot_welcome;
    }

    public String getNew_user_subscribe_channel_id() {
        return new_user_subscribe_channel_id;
    }

    public void setNew_user_subscribe_channel_id(String new_user_subscribe_channel_id) {
        this.new_user_subscribe_channel_id = new_user_subscribe_channel_id;
    }

    public String getBack_user_subscribe_channel_id() {
        return back_user_subscribe_channel_id;
    }

    public void setBack_user_subscribe_channel_id(String back_user_subscribe_channel_id) {
        this.back_user_subscribe_channel_id = back_user_subscribe_channel_id;
    }
}
