package cn.wildfirechat.app.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "phone_code_record")
public class Record {
    @Id
    @Column(length = 128)
    private String mobile;
    private String code;
    //发送时间，当小于1分钟不允许发送。
    private long timestamp;
    //计算时间段内发送次数的起始时间
    private long startTime;
    //startTime到现在的发送次数
    private int requestCount;

    public Record(String code, String mobile) {
        this.code = code;
        this.mobile = mobile;
        this.timestamp = 0;
        this.startTime = System.currentTimeMillis();
        this.requestCount = 0;
    }

    public Record() {
    }

    public boolean increaseAndCheck() {
        long now = System.currentTimeMillis();
        if (now - startTime > 86400000) {
            reset();
        }
        requestCount++;
        if (requestCount > 10) {
            return false;
        }
        return true;
    }

    public void reset() {
        requestCount = 1;
        startTime = System.currentTimeMillis();
    }

    public int getRequestCount() {
        return requestCount;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMobile() {
        return mobile;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
