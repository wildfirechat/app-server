package cn.wildfirechat.app.sms;


import cn.wildfirechat.app.RestResult;

public interface SmsService {
    RestResult.RestCode sendCode(String mobile, String code);
}
