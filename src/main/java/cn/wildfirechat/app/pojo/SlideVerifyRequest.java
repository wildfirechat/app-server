package cn.wildfirechat.app.pojo;

public class SlideVerifyRequest {
    private String token;
    private int x; // 滑动块的x坐标位置

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }
}
