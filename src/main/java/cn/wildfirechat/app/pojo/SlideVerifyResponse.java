package cn.wildfirechat.app.pojo;

public class SlideVerifyResponse {
    private String token;
    private String backgroundImage;  // base64编码的背景图
    private String sliderImage;      // base64编码的滑块图
    private int y;                   // 滑块在背景图中的y坐标

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getSliderImage() {
        return sliderImage;
    }

    public void setSliderImage(String sliderImage) {
        this.sliderImage = sliderImage;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
