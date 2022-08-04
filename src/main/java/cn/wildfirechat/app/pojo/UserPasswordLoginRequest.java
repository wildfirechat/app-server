package cn.wildfirechat.app.pojo;

public class UserPasswordLoginRequest {
    private String mobile;
    private String password;
    private String clientId;
    private Integer platform;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public Integer getPlatform() {
        return platform;
    }

    public void setPlatform(Integer platform) {
        this.platform = platform;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
