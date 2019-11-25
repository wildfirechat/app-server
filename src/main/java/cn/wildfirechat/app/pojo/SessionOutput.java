package cn.wildfirechat.app.pojo;

public class SessionOutput {
    private String token;
    private int status;
    private long expired;
    private int platform;
    private String device_name;

    public SessionOutput() {
    }

    public SessionOutput(String token, int status, long expired, String device_name, int platform) {
        this.token = token;
        this.status = status;
        this.expired = expired;
        this.device_name = device_name;
        this.platform = platform;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getExpired() {
        return expired;
    }

    public void setExpired(long expired) {
        this.expired = expired;
    }

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }
}
