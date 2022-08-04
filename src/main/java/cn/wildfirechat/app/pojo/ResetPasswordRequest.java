package cn.wildfirechat.app.pojo;

public class ResetPasswordRequest {
    private String mobile;
    private String resetCode;
    private String newPassword;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getResetCode() {
        return resetCode;
    }

    public void setResetCode(String resetCode) {
        this.resetCode = resetCode;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
