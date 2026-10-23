package cn.wildfirechat.app.robot;

public class RobotProvisionException extends RuntimeException {
    private final int code;

    public RobotProvisionException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
