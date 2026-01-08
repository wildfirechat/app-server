package cn.wildfirechat.app.pojo;

public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
    private String slideVerifyToken;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getSlideVerifyToken() {
        return slideVerifyToken;
    }

    public void setSlideVerifyToken(String slideVerifyToken) {
        this.slideVerifyToken = slideVerifyToken;
    }
}
