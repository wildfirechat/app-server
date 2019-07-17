package cn.wildfirechat.app;


import cn.wildfirechat.app.pojo.ConfirmSessionRequest;
import cn.wildfirechat.app.pojo.CreateSessionRequest;

public interface SmsService {
    RestResult.RestCode sendCode(String mobile, String code);
}
