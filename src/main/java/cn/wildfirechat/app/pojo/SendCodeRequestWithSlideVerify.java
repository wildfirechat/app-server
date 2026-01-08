package cn.wildfirechat.app.pojo;

public class SendCodeRequestWithSlideVerify {
    private String mobile;
    private String slideVerifyToken;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getSlideVerifyToken() {
        return slideVerifyToken;
    }

    public void setSlideVerifyToken(String slideVerifyToken) {
        this.slideVerifyToken = slideVerifyToken;
    }
}
