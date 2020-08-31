package cn.wildfirechat.app.pojo;

public class CreateSessionRequest {
    private String token;
    private String device_name;
    private String clientId;
    private int platform;
    // 0，表示pc端为旧版本，不支持快速登录；1，表示pc端为新版本，支持快速登录
    private int flag;

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
}
