package cn.wildfirechat.app.pojo;

public class PhoneCodeLoginRequestWithSlideVerify {
    private String mobile;
    private String code;
    private String clientId;
    private Integer platform;
    private String slideVerifyToken;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
