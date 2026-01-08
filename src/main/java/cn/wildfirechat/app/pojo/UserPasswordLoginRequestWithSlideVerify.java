package cn.wildfirechat.app.pojo;

public class UserPasswordLoginRequestWithSlideVerify {
    private String mobile;
    private String password;
    private String clientId;
    private Integer platform;
    private String slideVerifyToken;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Integer getPlatform() {
        return platform;
    }

    public void setPlatform(Integer platform) {
        this.platform = platform;
    }

    public String getSlideVerifyToken() {
        return slideVerifyToken;
    }

    public void setSlideVerifyToken(String slideVerifyToken) {
        this.slideVerifyToken = slideVerifyToken;
    }
}
