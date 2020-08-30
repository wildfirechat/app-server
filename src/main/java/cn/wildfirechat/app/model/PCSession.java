package cn.wildfirechat.app.model;

import cn.wildfirechat.app.pojo.SessionOutput;

public class PCSession {
    public interface PCSessionStatus {
        int Session_Created = 0;
        int Session_Scanned = 1;
        int Session_Pre_Verify = 3;
        int Session_Verified = 2;
        int Session_Canceled = 4;
    }

    private String token;
    private String clientId;
    private long createDt;
    private long duration;
    //PCSessionStatus
    private int status;
    private String confirmedUserId;
    private String device_name;
    private int platform;

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getCreateDt() {
        return createDt;
    }

    public void setCreateDt(long createDt) {
        this.createDt = createDt;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getConfirmedUserId() {
        return confirmedUserId;
    }

    public void setConfirmedUserId(String confirmedUserId) {
        this.confirmedUserId = confirmedUserId;
    }

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public SessionOutput toOutput() {
        return new SessionOutput(confirmedUserId, token, status, duration - (System.currentTimeMillis() - createDt), device_name, platform);
    }
}
