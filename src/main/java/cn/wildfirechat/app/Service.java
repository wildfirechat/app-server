package cn.wildfirechat.app;


public interface Service {
    RestResult sendCode(String mobile);
    RestResult login(String mobile, String code, String clientId);
}
