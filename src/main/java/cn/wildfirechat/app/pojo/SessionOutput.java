package cn.wildfirechat.app.pojo;

public class SessionOutput {
    private String token;
    private int status;
    private long expired;

    public SessionOutput() {
    }

    public SessionOutput(String token, int status, long expired) {
        this.token = token;
        this.status = status;
        this.expired = expired;
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
}
