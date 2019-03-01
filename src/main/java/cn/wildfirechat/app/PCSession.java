package cn.wildfirechat.app;

import cn.wildfirechat.app.pojo.SessionOutput;

public class PCSession {
    private String token;
    private String clientId;
    private long createDt;
    private long duration;
    private int status;
    private String confirmedUserId;

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

    public SessionOutput toOutput() {
        return new SessionOutput(token, status, duration - (System.currentTimeMillis() - createDt));
    }
}
