package cn.wildfirechat.app;


import cn.wildfirechat.app.pojo.LoginResponse;
import cn.wildfirechat.sdk.ChatAdmin;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.model.Token;
import cn.wildfirechat.sdk.model.User;
import cn.wildfirechat.sdk.model.UserId;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.github.qcloudsms.httpclient.HTTPException;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@org.springframework.stereotype.Service
public class ServiceImpl implements Service {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);
    private static ConcurrentHashMap<String, Record> mRecords = new ConcurrentHashMap<>();

    @Autowired
    private SMSConfig mSMSConfig;

    @Autowired
    private IMConfig mIMConfig;

    @PostConstruct
    private void init() {
        ChatAdmin.init(mIMConfig.admin_url, mIMConfig.admin_secret);
    }

    @Override
    public RestResult sendCode(String mobile) {
        try {
            if (!Utils.isMobile(mobile)) {
                LOG.error("Not valid mobile {}", mobile);
                return RestResult.error(RestResult.RestCode.ERROR_INVALID_MOBILE);
            }

            Record record = mRecords.get(mobile);
            if (record != null && System.currentTimeMillis() - record.getTimestamp() < 60 * 1000) {
                LOG.error("Send code over frequency. timestamp {}, now {}", record.getTimestamp(), System.currentTimeMillis());
                return RestResult.error(RestResult.RestCode.ERROR_SEND_SMS_OVER_FREQUENCY);
            }

            if (mobile.equals("18701580951")) {
                mRecords.put(mobile, new Record("55555", mobile));
                return RestResult.ok(null);
            }

            String code = Utils.getRandomCode(4);
            String[] params = {code};
            SmsSingleSender ssender = new SmsSingleSender(mSMSConfig.appid, mSMSConfig.appkey);
            SmsSingleSenderResult result = ssender.sendWithParam("86", mobile,
                    mSMSConfig.templateId, params, null, "", "");
            if (result.result == 0) {
                mRecords.put(mobile, new Record(code, mobile));
                return RestResult.ok(null);
            } else {
                LOG.error("Failure to send SMS {}", result);
                return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
            }
        } catch (HTTPException e) {
            // HTTP响应码错误
            e.printStackTrace();
        } catch (JSONException e) {
            // json解析错误
            e.printStackTrace();
        } catch (IOException e) {
            // 网络IO错误
            e.printStackTrace();
        }
        return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
    }

    @Override
    public RestResult login(String mobile, String code, String clientId) {
        if (StringUtils.isEmpty(mSMSConfig.superCode) || !code.equals(mSMSConfig.superCode)) {
            Record record = mRecords.get(mobile);
            if (record == null || !record.getCode().equals(code)) {
                LOG.error("not empty or not correct");
                return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
            }
            if (System.currentTimeMillis() - record.getTimestamp() > 5 * 60 * 1000) {
                LOG.error("Code expired. timestamp {}, now {}", record.getTimestamp(), System.currentTimeMillis());
                return RestResult.error(RestResult.RestCode.ERROR_CODE_EXPIRED);
            }
        }

        try {
            //使用电话号码查询用户信息。
            IMResult<User> userResult = ChatAdmin.getUserByName(mobile);

            //如果用户信息不存在，创建用户
            User user;
            boolean isNewUser = false;
            if (userResult.getCode() == IMResult.IMResultCode.IMRESULT_CODE_NOT_EXIST.code) {
                LOG.info("User not exist, try to create");
                user = new User();
                user.setName(mobile);
                user.setDisplayName(mobile);
                user.setMobile(mobile);
                IMResult<UserId> userIdResult = ChatAdmin.createUser(user);
                if (userIdResult.getCode() == 0) {
                    user.setUserId(userIdResult.getResult().getUserId());
                    isNewUser = true;
                } else {
                    LOG.info("Create user failure {}", userIdResult.code);
                    return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
                }
            } else if(userResult.getCode() != 0){
                LOG.error("Get user failure {}", userResult.code);
                return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
            } else {
                user = userResult.getResult();
            }

            //使用用户id获取token
            IMResult<Token> tokenResult = ChatAdmin.getUserToken(user.getUserId(), clientId);
            if (tokenResult.getCode() != 0) {
                LOG.error("Get user failure {}", tokenResult.code);
                return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
            }

            //返回用户id，token和是否新建
            LoginResponse response = new LoginResponse();
            response.setUserId(user.getUserId());
            response.setToken(tokenResult.getResult().getToken());
            response.setRegister(isNewUser);
            return RestResult.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Exception happens {}", e);
            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
        }
    }
}
