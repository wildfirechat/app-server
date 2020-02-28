package cn.wildfirechat.app.model;

public class Record {
    private final String code;
    private final String mobile;
    private final long timestamp;

    public Record(String code, String mobile) {
        this.code = code;
        this.mobile = mobile;
        this.timestamp = System.currentTimeMillis();
    }

    public String getCode() {
        return code;
    }

    public String getMobile() {
        return mobile;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
