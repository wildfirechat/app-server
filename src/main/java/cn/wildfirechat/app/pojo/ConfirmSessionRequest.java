package cn.wildfirechat.app.pojo;

public class ConfirmSessionRequest {
    private String im_token;
    private String token;
    private String user_id;
    private boolean quick_login;

    public String getIm_token() {
        return im_token;
    }

    public void setIm_token(String im_token) {
        this.im_token = im_token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public boolean isQuickLogin() {
        return quick_login;
    }

    public void setQuickLogin(boolean quickLogin) {
        this.quick_login = quickLogin;
    }
}
