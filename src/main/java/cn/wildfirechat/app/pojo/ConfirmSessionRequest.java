package cn.wildfirechat.app.pojo;

public class ConfirmSessionRequest {
    private String im_token;
    private String token;
    private String user_id;
    private int quick_login;

    //{"token":"22295ee9-d4e3-4fc0-bda3-bcfe008dce08","user_id":"CeDRCRtt","quick_login":true}
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

    public int getQuick_login() {
        return quick_login;
    }

    public void setQuick_login(int quick_login) {
        this.quick_login = quick_login;
    }
}
