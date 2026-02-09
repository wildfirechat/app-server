package cn.wildfirechat.app.jpa;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "slide_verify")
public class SlideVerify {
    @Id
    @Column(length = 64)
    private String token;

    @Column
    private int x;

    @Column
    private long timestamp;

    @Column
    private boolean verified;

    public SlideVerify() {
    }

    public SlideVerify(String token, int x, long timestamp) {
        this.token = token;
        this.x = x;
        this.timestamp = timestamp;
        this.verified = false;
    }

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isExpired(int timeoutSeconds) {
        return System.currentTimeMillis() - timestamp > timeoutSeconds * 1000L;
    }
}
