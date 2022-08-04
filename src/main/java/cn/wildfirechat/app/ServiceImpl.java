package cn.wildfirechat.app;


import cn.wildfirechat.app.jpa.*;
import cn.wildfirechat.app.pojo.*;
import cn.wildfirechat.app.shiro.AuthDataSource;
import cn.wildfirechat.app.shiro.PhoneCodeToken;
import cn.wildfirechat.app.shiro.TokenAuthenticationToken;
import cn.wildfirechat.app.sms.SmsService;
import cn.wildfirechat.app.tools.RateLimiter;
import cn.wildfirechat.app.tools.ShortUUIDGenerator;
import cn.wildfirechat.app.tools.Utils;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.sdk.*;
import cn.wildfirechat.sdk.model.IMResult;
import com.aliyun.oss.*;
import com.aliyun.oss.model.PutObjectRequest;
import com.google.gson.Gson;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.http.HttpProtocol;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.errors.MinioException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.crypto.hash.Sha1Hash;
import org.apache.shiro.subject.Subject;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static cn.wildfirechat.app.RestResult.RestCode.*;
import static cn.wildfirechat.app.jpa.PCSession.PCSessionStatus.*;

@org.springframework.stereotype.Service
public class ServiceImpl implements Service {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);

    @Autowired
    private SmsService smsService;

    @Autowired
    private IMConfig mIMConfig;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserPasswordRepository userPasswordRepository;

    @Value("${sms.super_code}")
    private String superCode;

    @Value("${logs.user_logs_path}")
    private String userLogPath;

    @Autowired
    private ShortUUIDGenerator userNameGenerator;

    @Autowired
    private AuthDataSource authDataSource;

    private RateLimiter rateLimiter;

    @Value("${wfc.compat_pc_quick_login}")
    protected boolean compatPcQuickLogin;

    @Value("${media.server.media_type}")
    private int ossType;

    @Value("${media.server_url}")
    private String ossUrl;

    @Value("${media.access_key}")
    private String ossAccessKey;

    @Value("${media.secret_key}")
    private String ossSecretKey;

    @Value("${media.bucket_general_name}")
    private String ossGeneralBucket;
    @Value("${media.bucket_general_domain}")
    private String ossGeneralBucketDomain;

    @Value("${media.bucket_image_name}")
    private String ossImageBucket;
    @Value("${media.bucket_image_domain}")
    private String ossImageBucketDomain;

    @Value("${media.bucket_voice_name}")
    private String ossVoiceBucket;
    @Value("${media.bucket_voice_domain}")
    private String ossVoiceBucketDomain;

    @Value("${media.bucket_video_name}")
    private String ossVideoBucket;
    @Value("${media.bucket_video_domain}")
    private String ossVideoBucketDomain;


    @Value("${media.bucket_file_name}")
    private String ossFileBucket;
    @Value("${media.bucket_file_domain}")
    private String ossFileBucketDomain;

    @Value("${media.bucket_sticker_name}")
    private String ossStickerBucket;
    @Value("${media.bucket_sticker_domain}")
    private String ossStickerBucketDomain;

    @Value("${media.bucket_moments_name}")
    private String ossMomentsBucket;
    @Value("${media.bucket_moments_domain}")
    private String ossMomentsBucketDomain;

    @Value("${media.bucket_favorite_name}")
    private String ossFavoriteBucket;
    @Value("${media.bucket_favorite_domain}")
    private String ossFavoriteBucketDomain;

    @Value("${local.media.temp_storage}")
    private String ossTempPath;

    private ConcurrentHashMap<String, Boolean> supportPCQuickLoginUsers = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        AdminConfig.initAdmin(mIMConfig.admin_url, mIMConfig.admin_secret);
        rateLimiter = new RateLimiter(60, 200);
    }

    private String getIp() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        String ip = request.getHeader("X-Real-IP");
        if (!StringUtils.isEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.isEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP。
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        } else {
            return request.getRemoteAddr();
        }
    }

    @Override
    public RestResult sendLoginCode(String mobile) {
        String remoteIp = getIp();
        LOG.info("request send sms from {}", remoteIp);

        //判断当前IP发送是否超频。
        //另外 cn.wildfirechat.app.shiro.AuthDataSource.Count 会对用户发送消息限频
        if (!rateLimiter.isGranted(remoteIp)) {
            return RestResult.result(ERROR_SEND_SMS_OVER_FREQUENCY.code, "IP " + remoteIp + " 请求短信超频", null);
        }

        try {
            String code = Utils.getRandomCode(4);
            RestResult.RestCode restCode = authDataSource.insertRecord(mobile, code);

            if (restCode != SUCCESS) {
                return RestResult.error(restCode);
            }


            restCode = smsService.sendCode(mobile, code);
            if (restCode == RestResult.RestCode.SUCCESS) {
                return RestResult.ok(restCode);
            } else {
                authDataSource.clearRecode(mobile);
                return RestResult.error(restCode);
            }
        } catch (Exception e) {
            // json解析错误
            e.printStackTrace();
            authDataSource.clearRecode(mobile);
        }
        return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
    }

    @Override
    public RestResult sendResetCode(String mobile) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        String remoteIp = getIp();
        LOG.info("request send sms from {}", remoteIp);

        //判断当前IP发送是否超频。
        //另外 cn.wildfirechat.app.shiro.AuthDataSource.Count 会对用户发送消息限频
        if (!rateLimiter.isGranted(remoteIp)) {
            return RestResult.result(ERROR_SEND_SMS_OVER_FREQUENCY.code, "IP " + remoteIp + " 请求短信超频", null);
        }

        try {
            String code = Utils.getRandomCode(4);
            RestResult.RestCode restCode = RestResult.RestCode.SUCCESS;//smsService.sendCode(mobile, code);
            if (restCode == RestResult.RestCode.SUCCESS) {
                Optional<UserPassword> optional = userPasswordRepository.findById(userId);
                UserPassword up = optional.orElseGet(() -> new UserPassword(userId, null, null, code));
                up.setResetCode(code);
                userPasswordRepository.save(up);
                return RestResult.ok(restCode);
            } else {
                return RestResult.error(restCode);
            }
        } catch (Exception e) {
            // json解析错误
            e.printStackTrace();
        }
        return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
    }

    @Override
    public RestResult loginWithMobileCode(HttpServletResponse httpResponse, String mobile, String code, String clientId, int platform) {
        Subject subject = SecurityUtils.getSubject();
        // 在认证提交前准备 token（令牌）
        PhoneCodeToken token = new PhoneCodeToken(mobile, code);
        // 执行认证登陆
        try {
            subject.login(token);
        } catch (UnknownAccountException uae) {
            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
        } catch (IncorrectCredentialsException ice) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        } catch (LockedAccountException lae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        } catch (ExcessiveAttemptsException eae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        } catch (AuthenticationException ae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        }
        if (subject.isAuthenticated()) {
            long timeout = subject.getSession().getTimeout();
            LOG.info("Login success " + timeout);
            authDataSource.clearRecode(mobile);
        } else {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        }

        return onLoginSuccess(httpResponse, mobile, clientId, platform, true);
    }

    @Override
    public RestResult loginWithPassword(HttpServletResponse response, String mobile, String password, String clientId, int platform) {
        try {
            IMResult<InputOutputUserInfo> userResult = UserAdmin.getUserByMobile(mobile);
            if (userResult.getErrorCode() == ErrorCode.ERROR_CODE_NOT_EXIST) {
                return RestResult.error(ERROR_NOT_EXIST);
            }
            if (userResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
            }

            Subject subject = SecurityUtils.getSubject();
            // 在认证提交前准备 token（令牌）
            UsernamePasswordToken token = new UsernamePasswordToken(userResult.getResult().getUserId(), password);
            // 执行认证登陆
            try {
                subject.login(token);
            } catch (UnknownAccountException uae) {
                return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
            } catch (IncorrectCredentialsException ice) {
                return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
            } catch (LockedAccountException lae) {
                return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
            } catch (ExcessiveAttemptsException eae) {
                return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
            } catch (AuthenticationException ae) {
                return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
            }
            if (subject.isAuthenticated()) {
                long timeout = subject.getSession().getTimeout();
                LOG.info("Login success " + timeout);
                authDataSource.clearRecode(mobile);
            } else {
                return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
        }

        return onLoginSuccess(response, mobile, clientId, platform, false);
    }

    @Override
    public RestResult changePassword(HttpServletResponse response, String oldPwd, String newPwd) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        Optional<UserPassword> optional = userPasswordRepository.findById(userId);
        if (optional.isPresent()) {
            try {
                if(verifyPassword(optional.get(), oldPwd)) {
                    changePassword(optional.get(), newPwd);
                    return RestResult.ok(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return RestResult.error(ERROR_NOT_EXIST);
        }
        return RestResult.error(ERROR_SERVER_ERROR);
    }

    @Override
    public RestResult resetPassword(HttpServletResponse response, String mobile, String resetCode, String newPwd) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");

        if (!StringUtils.isEmpty(mobile)) {
            try {
                IMResult<InputOutputUserInfo> userResult = UserAdmin.getUserByMobile(mobile);
                if (userResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                    return RestResult.error(ERROR_SERVER_ERROR);
                }
                if (StringUtils.isEmpty(userId)) {
                    userId = userResult.getResult().getUserId();
                } else {
                    if(!userId.equals(userResult.getResult().getUserId())) {
                        //错误。。。。
                        LOG.error("reset password error, user is correct {}, {}", userId, userResult.getResult().getUserId());
                        return RestResult.error(ERROR_SERVER_ERROR);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return RestResult.error(ERROR_SERVER_ERROR);
            }
        }

        Optional<UserPassword> optional = userPasswordRepository.findById(userId);
        if (optional.isPresent()) {
            UserPassword up = optional.get();
            if(resetCode.equals(up.getResetCode())) {
                try {
                    changePassword(up, newPwd);
                    return RestResult.ok(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return RestResult.error(ERROR_SERVER_ERROR);
                }
            } else {
                return RestResult.error(ERROR_CODE_INCORRECT);
            }
        } else {
            return RestResult.error(ERROR_NOT_EXIST);
        }
    }

    private void changePassword(UserPassword up, String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(Sha1Hash.ALGORITHM_NAME);
        digest.reset();
        String salt = UUID.randomUUID().toString();
        digest.update(salt.getBytes(StandardCharsets.UTF_8));
        byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        String hashedPwd = Base64.getEncoder().encodeToString(hashed);
        up.setPassword(hashedPwd);
        up.setSalt(salt);
        userPasswordRepository.save(up);
    }

    private boolean verifyPassword(UserPassword up, String password) throws Exception {
        String salt = up.getSalt();
        MessageDigest digest = MessageDigest.getInstance(Sha1Hash.ALGORITHM_NAME);
        if (salt != null) {
            digest.reset();
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
        }

        byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        String hashedPwd = Base64.getEncoder().encodeToString(hashed);
        return hashedPwd.equals(up.getPassword());
    }

    private RestResult onLoginSuccess(HttpServletResponse httpResponse, String mobile, String clientId, int platform, boolean withResetCode) {
        Subject subject = SecurityUtils.getSubject();
        try {
            //使用电话号码查询用户信息。
            IMResult<InputOutputUserInfo> userResult = UserAdmin.getUserByMobile(mobile);

            //如果用户信息不存在，创建用户
            InputOutputUserInfo user;
            boolean isNewUser = false;
            if (userResult.getErrorCode() == ErrorCode.ERROR_CODE_NOT_EXIST) {
                LOG.info("User not exist, try to create");

                //获取用户名。如果用的是shortUUID生成器，是有极小概率会重复的，所以需要去检查是否已经存在相同的userName。
                //ShortUUIDGenerator内的main函数有测试代码，可以观察一下碰撞的概率，这个重复是理论上的，作者测试了几千万次次都没有产生碰撞。
                //另外由于并发的问题，也有同时生成相同的id并同时去检查的并同时通过的情况，但这种情况概率极低，可以忽略不计。
                String userName;
                int tryCount = 0;
                do {
                    tryCount++;
                    userName = userNameGenerator.getUserName(mobile);
                    if (tryCount > 10) {
                        return RestResult.error(ERROR_SERVER_ERROR);
                    }
                } while (!isUsernameAvailable(userName));


                user = new InputOutputUserInfo();
                user.setName(userName);
                if (mIMConfig.use_random_name) {
                    String displayName = "用户" + (int) (Math.random() * 10000);
                    user.setDisplayName(displayName);
                } else {
                    user.setDisplayName(mobile);
                }
                user.setMobile(mobile);
                IMResult<OutputCreateUser> userIdResult = UserAdmin.createUser(user);
                if (userIdResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    user.setUserId(userIdResult.getResult().getUserId());
                    isNewUser = true;
                } else {
                    LOG.info("Create user failure {}", userIdResult.code);
                    return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
                }


            } else if (userResult.getCode() != 0) {
                LOG.error("Get user failure {}", userResult.code);
                return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
            } else {
                user = userResult.getResult();
            }

            //使用用户id获取token
            IMResult<OutputGetIMTokenData> tokenResult = UserAdmin.getUserToken(user.getUserId(), clientId, platform);
            if (tokenResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                LOG.error("Get user failure {}", tokenResult.code);
                return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
            }

            subject.getSession().setAttribute("userId", user.getUserId());

            //返回用户id，token和是否新建
            LoginResponse response = new LoginResponse();
            response.setUserId(user.getUserId());
            response.setToken(tokenResult.getResult().getToken());
            response.setRegister(isNewUser);
            response.setPortrait(user.getPortrait());
            response.setUserName(user.getName());

            if (withResetCode) {
                String code = Utils.getRandomCode(4);
                Optional<UserPassword> optional = userPasswordRepository.findById(user.getUserId());
                UserPassword up;
                if (optional.isPresent()) {
                    up = optional.get();
                } else {
                    up = new UserPassword(user.getUserId(), null, null);
                }
                up.setResetCode(code);
                userPasswordRepository.save(up);
                response.setResetCode(code);
            }

            if (isNewUser) {
                if (!StringUtils.isEmpty(mIMConfig.welcome_for_new_user)) {
                    sendTextMessage("admin", user.getUserId(), mIMConfig.welcome_for_new_user);
                }

                if (mIMConfig.new_user_robot_friend && !StringUtils.isEmpty(mIMConfig.robot_friend_id)) {
                    ;
                    RelationAdmin.setUserFriend(user.getUserId(), mIMConfig.robot_friend_id, true, null);
                    if (!StringUtils.isEmpty(mIMConfig.robot_welcome)) {
                        sendTextMessage(mIMConfig.robot_friend_id, user.getUserId(), mIMConfig.robot_welcome);
                    }
                }
            } else {
                if (!StringUtils.isEmpty(mIMConfig.welcome_for_back_user)) {
                    sendTextMessage("admin", user.getUserId(), mIMConfig.welcome_for_back_user);
                }
            }

            Object sessionId = subject.getSession().getId();
            httpResponse.setHeader("authToken", sessionId.toString());
            return RestResult.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Exception happens {}", e);
            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
        }
    }
    @Override
    public RestResult sendDestroyCode() {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        try {
            IMResult<InputOutputUserInfo> getUserResult = UserAdmin.getUserByUserId(userId);
            if(getUserResult != null && getUserResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                String mobile = getUserResult.getResult().getMobile();
                if(!StringUtils.isEmpty(mobile)) {
                    return sendLoginCode(mobile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
        }
        return RestResult.error(RestResult.RestCode.ERROR_NOT_EXIST);
    }

    @Override
    public RestResult destroy(HttpServletResponse response, String code) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        try {
            IMResult<InputOutputUserInfo> getUserResult = UserAdmin.getUserByUserId(userId);
            if(getUserResult != null && getUserResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                String mobile = getUserResult.getResult().getMobile();
                if(!StringUtils.isEmpty(mobile)) {
                    if(authDataSource.verifyCode(mobile, code) == SUCCESS) {
                        UserAdmin.destroyUser(userId);
                        authDataSource.clearRecode(mobile);
                        subject.logout();
                        return RestResult.ok(null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
        }
        return RestResult.error(RestResult.RestCode.ERROR_NOT_EXIST);
    }

    private boolean isUsernameAvailable(String username) {
        try {
            IMResult<InputOutputUserInfo> existUser = UserAdmin.getUserByName(username);
            if (existUser.code == ErrorCode.ERROR_CODE_NOT_EXIST.code) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void sendPcLoginRequestMessage(String fromUser, String toUser, int platform, String token) {
        Conversation conversation = new Conversation();
        conversation.setTarget(toUser);
        conversation.setType(ProtoConstants.ConversationType.ConversationType_Private);
        MessagePayload payload = new MessagePayload();
        payload.setType(94);
        if (platform == ProtoConstants.Platform.Platform_WEB) {
            payload.setPushContent("Web端登录请求");
        } else if (platform == ProtoConstants.Platform.Platform_OSX) {
            payload.setPushContent("Mac 端登录请求");
        } else if (platform == ProtoConstants.Platform.Platform_LINUX) {
            payload.setPushContent("Linux 端登录请求");
        } else if (platform == ProtoConstants.Platform.Platform_Windows) {
            payload.setPushContent("Windows 端登录请求");
        } else {
            payload.setPushContent("PC 端登录请求");
        }

        payload.setExpireDuration(60 * 1000);
        payload.setPersistFlag(ProtoConstants.PersistFlag.Not_Persist);
        JSONObject data = new JSONObject();
        data.put("p", platform);
        data.put("t", token);
        payload.setBase64edData(Base64Utils.encodeToString(data.toString().getBytes()));

        try {
            IMResult<SendMessageResult> resultSendMessage = MessageAdmin.sendMessage(fromUser, conversation, payload);
            if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                LOG.info("send message success");
            } else {
                LOG.error("send message error {}", resultSendMessage != null ? resultSendMessage.getErrorCode().code : "unknown");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("send message error {}", e.getLocalizedMessage());
        }

    }

    private void sendTextMessage(String fromUser, String toUser, String text) {
        Conversation conversation = new Conversation();
        conversation.setTarget(toUser);
        conversation.setType(ProtoConstants.ConversationType.ConversationType_Private);
        MessagePayload payload = new MessagePayload();
        payload.setType(1);
        payload.setSearchableContent(text);


        try {
            IMResult<SendMessageResult> resultSendMessage = MessageAdmin.sendMessage(fromUser, conversation, payload);
            if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                LOG.info("send message success");
            } else {
                LOG.error("send message error {}", resultSendMessage != null ? resultSendMessage.getErrorCode().code : "unknown");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("send message error {}", e.getLocalizedMessage());
        }

    }


    @Override
    public RestResult createPcSession(CreateSessionRequest request) {
        String userId = request.getUserId();
        // pc端切换登录用户时，还会带上之前的cookie，通过请求里面是否带有userId来判断是否是切换到新用户
        if (request.getFlag() == 1 && !StringUtils.isEmpty(userId)) {
            Subject subject = SecurityUtils.getSubject();
            userId = (String) subject.getSession().getAttribute("userId");
        }

        if (compatPcQuickLogin) {
            if (userId != null && supportPCQuickLoginUsers.get(userId) == null) {
                userId = null;
            }
        }

        PCSession session = authDataSource.createSession(userId, request.getClientId(), request.getToken(), request.getPlatform());
        if (userId != null) {
            sendPcLoginRequestMessage("admin", userId, request.getPlatform(), session.getToken());
        }
        SessionOutput output = session.toOutput();
        LOG.info("client {} create pc session, key is {}", request.getClientId(), output.getToken());
        return RestResult.ok(output);
    }

    @Override
    public RestResult loginWithSession(String token) {
        Subject subject = SecurityUtils.getSubject();
        // 在认证提交前准备 token（令牌）
        // comment start 如果确定登录不成功，就不通过Shiro尝试登录了
        TokenAuthenticationToken tt = new TokenAuthenticationToken(token);
        PCSession session = authDataSource.getSession(token, false);

        if (session == null) {
            return RestResult.error(ERROR_CODE_EXPIRED);
        } else if (session.getStatus() == Session_Created) {
            return RestResult.error(ERROR_SESSION_NOT_SCANED);
        } else if (session.getStatus() == Session_Scanned) {
            session.setStatus(Session_Pre_Verify);
            authDataSource.saveSession(session);
            LoginResponse response = new LoginResponse();
            try {
                IMResult<InputOutputUserInfo> result = UserAdmin.getUserByUserId(session.getConfirmedUserId());
                if (result.getCode() == 0) {
                    response.setUserName(result.getResult().getDisplayName());
                    response.setPortrait(result.getResult().getPortrait());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return RestResult.result(ERROR_SESSION_NOT_VERIFIED, response);
        } else if (session.getStatus() == Session_Pre_Verify) {
            return RestResult.error(ERROR_SESSION_NOT_VERIFIED);
        } else if (session.getStatus() == Session_Canceled) {
            return RestResult.error(ERROR_SESSION_CANCELED);
        }
        // comment end

        // 执行认证登陆
        // comment start 由于PC端登录之后，可以请求app server创建群公告等。为了保证安全, PC端登录时，也需要在app server创建session。
        try {
            subject.login(tt);
        } catch (UnknownAccountException uae) {
            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
        } catch (IncorrectCredentialsException ice) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        } catch (LockedAccountException lae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        } catch (ExcessiveAttemptsException eae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        } catch (AuthenticationException ae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        }
        if (subject.isAuthenticated()) {
            LOG.info("Login success");
        } else {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        }
        // comment end

        session = authDataSource.getSession(token, true);
        if (session == null) {
            subject.logout();
            return RestResult.error(RestResult.RestCode.ERROR_CODE_EXPIRED);
        }
        subject.getSession().setAttribute("userId", session.getConfirmedUserId());

        try {
            //使用用户id获取token
            IMResult<OutputGetIMTokenData> tokenResult = UserAdmin.getUserToken(session.getConfirmedUserId(), session.getClientId(), session.getPlatform());
            if (tokenResult.getCode() != 0) {
                LOG.error("Get user failure {}", tokenResult.code);
                subject.logout();
                return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
            }
            //返回用户id，token和是否新建
            LoginResponse response = new LoginResponse();
            response.setUserId(session.getConfirmedUserId());
            response.setToken(tokenResult.getResult().getToken());
            return RestResult.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            subject.logout();
            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
        }
    }

    @Override
    public RestResult scanPc(String token) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");

        LOG.info("user {} scan pc, session is {}", userId, token);
        return authDataSource.scanPc(userId, token);
    }

    @Override
    public RestResult confirmPc(ConfirmSessionRequest request) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        if (compatPcQuickLogin) {
            if (request.getQuick_login() > 0) {
                supportPCQuickLoginUsers.put(userId, true);
            } else {
                supportPCQuickLoginUsers.remove(userId);
            }
        }

        LOG.info("user {} confirm pc, session is {}", userId, request.getToken());
        return authDataSource.confirmPc(userId, request.getToken());
    }

    @Override
    public RestResult cancelPc(CancelSessionRequest request) {
        return authDataSource.cancelPc(request.getToken());
    }

    @Override
    public RestResult changeName(String newName) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        try {
            IMResult<InputOutputUserInfo> existUser = UserAdmin.getUserByName(newName);
            if (existUser != null) {
                if (existUser.code == ErrorCode.ERROR_CODE_SUCCESS.code) {
                    if (userId.equals(existUser.getResult().getUserId())) {
                        return RestResult.ok(null);
                    } else {
                        return RestResult.error(ERROR_USER_NAME_ALREADY_EXIST);
                    }
                } else if (existUser.code == ErrorCode.ERROR_CODE_NOT_EXIST.code) {
                    existUser = UserAdmin.getUserByUserId(userId);
                    if (existUser == null || existUser.code != ErrorCode.ERROR_CODE_SUCCESS.code || existUser.getResult() == null) {
                        return RestResult.error(ERROR_SERVER_ERROR);
                    }

                    existUser.getResult().setName(newName);
                    IMResult<OutputCreateUser> createUser = UserAdmin.createUser(existUser.getResult());
                    if (createUser.code == ErrorCode.ERROR_CODE_SUCCESS.code) {
                        return RestResult.ok(null);
                    } else {
                        return RestResult.error(ERROR_SERVER_ERROR);
                    }
                } else {
                    return RestResult.error(ERROR_SERVER_ERROR);
                }
            } else {
                return RestResult.error(ERROR_SERVER_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RestResult.error(ERROR_SERVER_ERROR);
        }
    }

    @Override
    public RestResult complain(String text) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        LOG.error("Complain from user {} where content {}", userId, text);
        sendTextMessage(userId, "cgc8c8VV", text);
        return RestResult.ok(null);
    }

    @Override
    public RestResult getGroupAnnouncement(String groupId) {
        Optional<Announcement> announcement = announcementRepository.findById(groupId);
        if (announcement.isPresent()) {
            GroupAnnouncementPojo pojo = new GroupAnnouncementPojo();
            pojo.groupId = announcement.get().getGroupId();
            pojo.author = announcement.get().getAuthor();
            pojo.text = announcement.get().getAnnouncement();
            pojo.timestamp = announcement.get().getTimestamp();
            return RestResult.ok(pojo);
        } else {
            return RestResult.error(ERROR_GROUP_ANNOUNCEMENT_NOT_EXIST);
        }
    }

    @Override
    public RestResult putGroupAnnouncement(GroupAnnouncementPojo request) {
        if (!StringUtils.isEmpty(request.text)) {
            Subject subject = SecurityUtils.getSubject();
            String userId = (String) subject.getSession().getAttribute("userId");
            boolean isGroupMember = false;
            try {
                IMResult<OutputGroupMemberList> imResult = GroupAdmin.getGroupMembers(request.groupId);
                if (imResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && imResult.getResult() != null && imResult.getResult().getMembers() != null) {
                    for (PojoGroupMember member : imResult.getResult().getMembers()) {
                        if (member.getMember_id().equals(userId)) {
                            if (member.getType() != ProtoConstants.GroupMemberType.GroupMemberType_Removed
                                && member.getType() != ProtoConstants.GroupMemberType.GroupMemberType_Silent) {
                                isGroupMember = true;
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!isGroupMember) {
                return RestResult.error(ERROR_NO_RIGHT);
            }

            Conversation conversation = new Conversation();
            conversation.setTarget(request.groupId);
            conversation.setType(ProtoConstants.ConversationType.ConversationType_Group);
            MessagePayload payload = new MessagePayload();
            payload.setType(1);
            payload.setSearchableContent("@所有人 " + request.text);
            payload.setMentionedType(2);


            try {
                IMResult<SendMessageResult> resultSendMessage = MessageAdmin.sendMessage(request.author, conversation, payload);
                if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    LOG.info("send message success");
                } else {
                    LOG.error("send message error {}", resultSendMessage != null ? resultSendMessage.getErrorCode().code : "unknown");
                    return RestResult.error(ERROR_SERVER_ERROR);
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("send message error {}", e.getLocalizedMessage());
                return RestResult.error(ERROR_SERVER_ERROR);
            }
        }

        Announcement announcement = new Announcement();
        announcement.setGroupId(request.groupId);
        announcement.setAuthor(request.author);
        announcement.setAnnouncement(request.text);
        request.timestamp = System.currentTimeMillis();
        announcement.setTimestamp(request.timestamp);

        announcementRepository.save(announcement);
        return RestResult.ok(request);
    }

    @Override
    public RestResult saveUserLogs(String userId, MultipartFile file) {
        File localFile = new File(userLogPath, userId + "_" + file.getOriginalFilename());

        try {
            file.transferTo(localFile);
        } catch (IOException e) {
            e.printStackTrace();
            return RestResult.error(ERROR_SERVER_ERROR);
        }

        return RestResult.ok(null);
    }

    @Override
    public RestResult addDevice(InputCreateDevice createDevice) {
        try {
            Subject subject = SecurityUtils.getSubject();
            String userId = (String) subject.getSession().getAttribute("userId");

            if (!StringUtils.isEmpty(createDevice.getDeviceId())) {
                IMResult<OutputDevice> outputDeviceIMResult = UserAdmin.getDevice(createDevice.getDeviceId());
                if (outputDeviceIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    if (!createDevice.getOwners().contains(userId)) {
                        return RestResult.error(ERROR_NO_RIGHT);
                    }
                } else if (outputDeviceIMResult.getErrorCode() != ErrorCode.ERROR_CODE_NOT_EXIST) {
                    return RestResult.error(ERROR_SERVER_ERROR);
                }
            }

            IMResult<OutputCreateDevice> result = UserAdmin.createOrUpdateDevice(createDevice);
            if (result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                return RestResult.ok(result.getResult());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResult.error(ERROR_SERVER_ERROR);
    }

    @Override
    public RestResult getDeviceList() {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        try {
            IMResult<OutputDeviceList> imResult = UserAdmin.getUserDevices(userId);
            if (imResult != null && imResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                return RestResult.ok(imResult.getResult().getDevices());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResult.error(ERROR_SERVER_ERROR);
    }


    @Override
    public RestResult delDevice(InputCreateDevice createDevice) {
        try {
            Subject subject = SecurityUtils.getSubject();
            String userId = (String) subject.getSession().getAttribute("userId");

            if (!StringUtils.isEmpty(createDevice.getDeviceId())) {
                IMResult<OutputDevice> outputDeviceIMResult = UserAdmin.getDevice(createDevice.getDeviceId());
                if (outputDeviceIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    if (outputDeviceIMResult.getResult().getOwners().contains(userId)) {
                        createDevice.setExtra(outputDeviceIMResult.getResult().getExtra());
                        outputDeviceIMResult.getResult().getOwners().remove(userId);
                        createDevice.setOwners(outputDeviceIMResult.getResult().getOwners());
                        IMResult<OutputCreateDevice> result = UserAdmin.createOrUpdateDevice(createDevice);
                        if (result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                            return RestResult.ok(result.getResult());
                        } else {
                            return RestResult.error(ERROR_SERVER_ERROR);
                        }
                    } else {
                        return RestResult.error(ERROR_NO_RIGHT);
                    }
                } else {
                    if (outputDeviceIMResult.getErrorCode() != ErrorCode.ERROR_CODE_NOT_EXIST) {
                        return RestResult.error(ERROR_SERVER_ERROR);
                    } else {
                        return RestResult.error(ERROR_NOT_EXIST);
                    }
                }
            } else {
                return RestResult.error(ERROR_INVALID_PARAMETER);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResult.error(ERROR_SERVER_ERROR);
    }

    @Override
    public RestResult sendMessage(SendMessageRequest request) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");

        Conversation conversation = new Conversation();
        conversation.setType(request.type);
        conversation.setTarget(request.target);
        conversation.setLine(request.line);

        MessagePayload payload = new MessagePayload();
        payload.setType(request.content_type);
        payload.setSearchableContent(request.content_searchable);
        payload.setPushContent(request.content_push);
        payload.setPushData(request.content_push_data);
        payload.setContent(request.content);
        payload.setBase64edData(request.content_binary);
        payload.setMediaType(request.content_media_type);
        payload.setRemoteMediaUrl(request.content_remote_url);
        payload.setMentionedType(request.content_mentioned_type);
        payload.setMentionedTarget(request.content_mentioned_targets);
        payload.setExtra(request.content_extra);

        try {
            IMResult<SendMessageResult> imResult = MessageAdmin.sendMessage(userId, conversation, payload);
            if (imResult != null && imResult.getCode() == ErrorCode.ERROR_CODE_SUCCESS.code) {
                return RestResult.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResult.error(ERROR_SERVER_ERROR);
    }

    @Override
    public RestResult uploadMedia(int mediaType, MultipartFile file) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        String uuid = new ShortUUIDGenerator().getUserName(userId);
        String fileName = userId + "-" + System.currentTimeMillis() + "-" + uuid + "-" + file.getOriginalFilename();
        File localFile = new File(ossTempPath, fileName);

        try {
            file.transferTo(localFile);
        } catch (IOException e) {
            e.printStackTrace();
            return RestResult.error(ERROR_SERVER_ERROR);
        }
        /*
        #Media_Type_GENERAL = 0,
#Media_Type_IMAGE = 1,
#Media_Type_VOICE = 2,
#Media_Type_VIDEO = 3,
#Media_Type_FILE = 4,
#Media_Type_PORTRAIT = 5,
#Media_Type_FAVORITE = 6,
#Media_Type_STICKER = 7,
#Media_Type_MOMENTS = 8
         */
        String bucket;
        String bucketDomain;
        switch (mediaType) {
            case 0:
            default:
                bucket = ossGeneralBucket;
                bucketDomain = ossGeneralBucketDomain;
                break;
            case 1:
                bucket = ossImageBucket;
                bucketDomain = ossImageBucketDomain;
                break;
            case 2:
                bucket = ossVoiceBucket;
                bucketDomain = ossVideoBucketDomain;
                break;
            case 3:
                bucket = ossVideoBucket;
                bucketDomain = ossVideoBucketDomain;
                break;
            case 4:
                bucket = ossFileBucket;
                bucketDomain = ossFileBucketDomain;
                break;
            case 7:
                bucket = ossMomentsBucket;
                bucketDomain = ossMomentsBucketDomain;
                break;
            case 8:
                bucket = ossStickerBucket;
                bucketDomain = ossStickerBucketDomain;
                break;
        }

        String url = bucketDomain + "/" + fileName;
        if (ossType == 1) {
            //构造一个带指定 Region 对象的配置类
            Configuration cfg = new Configuration(Region.region0());
            //...其他参数参考类注释
            UploadManager uploadManager = new UploadManager(cfg);
            //...生成上传凭证，然后准备上传

            //如果是Windows情况下，格式是 D:\\qiniu\\test.png
            String localFilePath = localFile.getAbsolutePath();
            //默认不指定key的情况下，以文件内容的hash值作为文件名
            String key = fileName;
            Auth auth = Auth.create(ossAccessKey, ossSecretKey);
            String upToken = auth.uploadToken(bucket);
            try {
                Response response = uploadManager.put(localFilePath, key, upToken);
                //解析上传成功的结果
                DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
                System.out.println(putRet.key);
                System.out.println(putRet.hash);
            } catch (QiniuException ex) {
                Response r = ex.response;
                System.err.println(r.toString());
                try {
                    System.err.println(r.bodyString());
                } catch (QiniuException ex2) {
                    //ignore
                }
                return RestResult.error(ERROR_SERVER_ERROR);
            }
        } else if (ossType == 2) {
            // 创建OSSClient实例。
            OSS ossClient = new OSSClientBuilder().build(ossUrl, ossAccessKey, ossSecretKey);

            // 创建PutObjectRequest对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fileName, localFile);

            // 上传文件。
            try {
                ossClient.putObject(putObjectRequest);
            } catch (OSSException | ClientException e) {
                e.printStackTrace();
                return RestResult.error(ERROR_SERVER_ERROR);
            }
            // 关闭OSSClient。
            ossClient.shutdown();
        } else if (ossType == 3) {
            try {
                // 使用MinIO服务的URL，端口，Access key和Secret key创建一个MinioClient对象
//                MinioClient minioClient = new MinioClient("https://play.min.io", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
                MinioClient minioClient = new MinioClient(ossUrl, ossAccessKey, ossSecretKey);

                // 使用putObject上传一个文件到存储桶中。
//                minioClient.putObject("asiatrip",fileName, localFile.getAbsolutePath(), new PutObjectOptions(PutObjectOptions.MAX_OBJECT_SIZE, PutObjectOptions.MIN_MULTIPART_SIZE));
                minioClient.putObject(bucket, fileName, localFile.getAbsolutePath(), new PutObjectOptions(file.getSize(), 0));
            } catch (MinioException e) {
                System.out.println("Error occurred: " + e);
                return RestResult.error(ERROR_SERVER_ERROR);
            } catch (NoSuchAlgorithmException | IOException | InvalidKeyException e) {
                e.printStackTrace();
                return RestResult.error(ERROR_SERVER_ERROR);
            } catch (Exception e) {
                e.printStackTrace();
                return RestResult.error(ERROR_SERVER_ERROR);
            }
        } else if(ossType == 4) {
            //Todo 需要把文件上传到文件服务器。
        } else if(ossType == 5) {
            COSCredentials cred = new BasicCOSCredentials(ossAccessKey, ossSecretKey);
            ClientConfig clientConfig = new ClientConfig();
            String [] ss = ossUrl.split("\\.");
            if(ss.length > 3) {
                if(!ss[1].equals("accelerate")) {
                    clientConfig.setRegion(new com.qcloud.cos.region.Region(ss[1]));
                } else {
                    clientConfig.setRegion(new com.qcloud.cos.region.Region("ap-shanghai"));
                    try {
                        URL u = new URL(ossUrl);
                        clientConfig.setEndPointSuffix(u.getHost());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        return RestResult.error(ERROR_SERVER_ERROR);
                    }
                }
            }

            clientConfig.setHttpProtocol(HttpProtocol.https);
            COSClient cosClient = new COSClient(cred, clientConfig);

            try {
                cosClient.putObject(bucket, fileName, localFile.getAbsoluteFile());
            } catch (CosClientException e) {
                e.printStackTrace();
                return RestResult.error(ERROR_SERVER_ERROR);
            } finally {
                cosClient.shutdown();
            }
        }

        UploadFileResponse response = new UploadFileResponse();
        response.url = url;
        return RestResult.ok(response);
    }

    @Override
    public RestResult putFavoriteItem(FavoriteItem request) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");

        if(!StringUtils.isEmpty(request.url)){
            try {
                //收藏时需要把对象拷贝到收藏bucket。
                URL mediaURL = new URL(request.url);

                String bucket = null;
                if (mediaURL.getHost().equals(new URL(ossGeneralBucketDomain).getHost())) {
                    bucket = ossGeneralBucket;
                } else if (mediaURL.getHost().equals(new URL(ossImageBucketDomain).getHost())) {
                    bucket = ossImageBucket;
                } else if (mediaURL.getHost().equals(new URL(ossVoiceBucketDomain).getHost())) {
                    bucket = ossVoiceBucket;
                } else if (mediaURL.getHost().equals(new URL(ossVideoBucketDomain).getHost())) {
                    bucket = ossVideoBucket;
                } else if (mediaURL.getHost().equals(new URL(ossFileBucketDomain).getHost())) {
                    bucket = ossFileBucket;
                } else if (mediaURL.getHost().equals(new URL(ossMomentsBucketDomain).getHost())) {
                    bucket = ossMomentsBucket;
                } else if (mediaURL.getHost().equals(new URL(ossStickerBucketDomain).getHost())) {
                    bucket = ossStickerBucket;
                } else if (mediaURL.getHost().equals(new URL(ossFavoriteBucketDomain).getHost())) {
                    //It's already in fav bucket, no need to copy
                    //bucket = ossFavoriteBucket;
                }

                if (bucket != null) {
                    String path = mediaURL.getPath();
                    if (ossType == 1) {
                        Configuration cfg = new Configuration(Region.region0());
                        String fromKey = path.substring(1);
                        Auth auth = Auth.create(ossAccessKey, ossSecretKey);

                        String toBucket = ossFavoriteBucket;
                        String toKey = fromKey;
                        if (!toKey.startsWith(userId)) {
                            toKey = userId + "-" + toKey;
                        }

                        BucketManager bucketManager = new BucketManager(auth, cfg);
                        bucketManager.copy(bucket, fromKey, toBucket, toKey);
                        request.url = ossFavoriteBucketDomain + "/" + fromKey;
                    } else if (ossType == 2) {
                        OSS ossClient = new OSSClient(ossUrl, ossAccessKey, ossSecretKey);
                        path = path.substring(1);
                        String objectName = path;
                        String toKey = path;
                        if (!toKey.startsWith(userId)) {
                            toKey = userId + "-" + toKey;
                        }

                        ossClient.copyObject(bucket, objectName, ossFavoriteBucket, toKey);
                        request.url = ossFavoriteBucketDomain + "/" + toKey;
                        ossClient.shutdown();
                    } else if (ossType == 3) {
                        path = path.substring(bucket.length() + 2);
                        String objectName = path;
                        String toKey = path;
                        if (!toKey.startsWith(userId)) {
                            toKey = userId + "-" + toKey;
                        }
                        MinioClient minioClient = new MinioClient(ossUrl, ossAccessKey, ossSecretKey);
                        minioClient.copyObject(ossFavoriteBucket, toKey, null, null, bucket, objectName, null, null);
                        request.url = ossFavoriteBucketDomain + "/" + toKey;
                    } else if(ossType == 4) {
                        //Todo 需要把收藏的文件保存为永久存储。
                    } else if(ossType == 5) {
                        COSCredentials cred = new BasicCOSCredentials(ossAccessKey, ossSecretKey);
                        ClientConfig clientConfig = new ClientConfig();
                        String [] ss = ossUrl.split("\\.");
                        if(ss.length > 3) {
                            if(!ss[1].equals("accelerate")) {
                                clientConfig.setRegion(new com.qcloud.cos.region.Region(ss[1]));
                            } else {
                                clientConfig.setRegion(new com.qcloud.cos.region.Region("ap-shanghai"));
                                try {
                                    URL u = new URL(ossUrl);
                                    clientConfig.setEndPointSuffix(u.getHost());
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                    return RestResult.error(ERROR_SERVER_ERROR);
                                }
                            }
                        }

                        clientConfig.setHttpProtocol(HttpProtocol.https);
                        COSClient cosClient = new COSClient(cred, clientConfig);

                        path = path.substring(1);
                        String objectName = path;
                        String toKey = path;
                        if (!toKey.startsWith(userId)) {
                            toKey = userId + "-" + toKey;
                        }

                        try {
                            cosClient.copyObject(bucket, objectName, ossFavoriteBucket, toKey);
                            request.url = ossFavoriteBucketDomain + "/" + toKey;
                        } catch (CosClientException e) {
                            e.printStackTrace();
                            return RestResult.error(ERROR_SERVER_ERROR);
                        } finally {
                            cosClient.shutdown();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        request.userId = userId;
        request.timestamp = System.currentTimeMillis();
        favoriteRepository.save(request);
        return RestResult.ok(null);
    }

    @Override
    public RestResult removeFavoriteItems(long id) {
        favoriteRepository.deleteById(id);
        return RestResult.ok(null);
    }

    @Override
    public RestResult getFavoriteItems(long id, int count) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");

        id = id > 0 ? id : Long.MAX_VALUE;
        List<FavoriteItem> favs = favoriteRepository.loadFav(userId, id, count);
        LoadFavoriteResponse response = new LoadFavoriteResponse();
        response.items = favs;
        response.hasMore = favs.size() == count;
        return RestResult.ok(response);
    }

    @Override
    public RestResult getGroupMembersForPortrait(String groupId) {
        try {
            IMResult<OutputGroupMemberList> groupMemberListIMResult = GroupAdmin.getGroupMembers(groupId);
            if(groupMemberListIMResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                LOG.error("getGroupMembersForPortrait failure {},{}", groupMemberListIMResult.getErrorCode().getCode(), groupMemberListIMResult.getErrorCode().getMsg());
                return RestResult.error(ERROR_SERVER_ERROR);
            }
            List<PojoGroupMember> groupMembers = new ArrayList<>();
            for (PojoGroupMember member:groupMemberListIMResult.getResult().getMembers()) {
                if(member.getType() != 4)
                    groupMembers.add(member);
            }

            if (groupMembers.size() > 9) {
                groupMembers.sort((o1, o2) -> {
                    if(o1.getType() == 2)
                        return -1;
                    if(o2.getType() == 2)
                        return 1;
                    if(o1.getType() == 1 && o2.getType() != 1)
                        return -1;
                    if(o2.getType() == 1 && o1.getType() != 1)
                        return 1;
                    return Long.compare(o1.getCreateDt(), o2.getCreateDt());
                });
                groupMembers = groupMembers.subList(0, 9);
            }
            List<UserIdPortraitPojo> mids = new ArrayList<>();
            for (PojoGroupMember member:groupMembers) {
                IMResult<InputOutputUserInfo> userInfoIMResult = UserAdmin.getUserByUserId(member.getMember_id());
                if(userInfoIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    mids.add(new UserIdPortraitPojo(member.getMember_id(), userInfoIMResult.result.getPortrait()));
                } else {
                    mids.add(new UserIdPortraitPojo(member.getMember_id(), ""));
                }
            }
            return RestResult.ok(mids);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("getGroupMembersForPortrait exception", e);
            return RestResult.error(ERROR_SERVER_ERROR);
        }
    }
}
