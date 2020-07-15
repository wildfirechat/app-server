package cn.wildfirechat.app;


import cn.wildfirechat.app.jpa.Announcement;
import cn.wildfirechat.app.jpa.AnnouncementRepository;
import cn.wildfirechat.app.model.PCSession;
import cn.wildfirechat.app.pojo.*;
import cn.wildfirechat.app.shiro.AuthDataSource;
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
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static cn.wildfirechat.app.RestResult.RestCode.*;

@org.springframework.stereotype.Service
public class ServiceImpl implements Service {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);

    @Autowired
    private SmsService smsService;

    @Autowired
    private IMConfig mIMConfig;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Value("${sms.super_code}")
    private String superCode;

    @Value("${logs.user_logs_path}")
    private String userLogPath;

    @Autowired
    private ShortUUIDGenerator userNameGenerator;

    @Autowired
    private AuthDataSource authDataSource;

    private RateLimiter rateLimiter;

    @PostConstruct
    private void init() {
        ChatConfig.initAdmin(mIMConfig.admin_url, mIMConfig.admin_secret);
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
    public RestResult sendCode(String mobile) {
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
        } catch (JSONException e) {
            // json解析错误
            e.printStackTrace();
            authDataSource.clearRecode(mobile);
        }
        return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
    }

    @Override
    public RestResult login(String mobile, String code, String clientId, int platform) {
        Subject subject = SecurityUtils.getSubject();
        // 在认证提交前准备 token（令牌）
        UsernamePasswordToken token = new UsernamePasswordToken(mobile, code);
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
        } else {
            token.clear();
            return RestResult.error(RestResult.RestCode.ERROR_CODE_INCORRECT);
        }


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



            } else if(userResult.getCode() != 0){
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

            if (isNewUser) {
                if (!StringUtils.isEmpty(mIMConfig.welcome_for_new_user)) {
                    sendTextMessage("admin", user.getUserId(), mIMConfig.welcome_for_new_user);
                }

                if (mIMConfig.new_user_robot_friend && !StringUtils.isEmpty(mIMConfig.robot_friend_id)) { ;
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

            return RestResult.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Exception happens {}", e);
            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
        }
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
        PCSession session = authDataSource.createSession(request.getClientId(), request.getToken(), request.getPlatform());
        SessionOutput output = session.toOutput();
        return RestResult.ok(output);
    }

    @Override
    public RestResult loginWithSession(String token) {
        Subject subject = SecurityUtils.getSubject();
        // 在认证提交前准备 token（令牌）
        // comment start 如果确定登录不成功，就不通过Shiro尝试登录了
        TokenAuthenticationToken tt = new TokenAuthenticationToken(token);
        PCSession session = authDataSource.getSession(token, false);
        if(session == null){
            return RestResult.error(ERROR_CODE_EXPIRED);
        }else if(session.getStatus() == 0){
            return RestResult.error(ERROR_SESSION_NOT_SCANED);
        }else if (session.getStatus() == 1){
            session.setStatus(3);
            LoginResponse response = new LoginResponse();
            try {
                IMResult<InputOutputUserInfo> result = UserAdmin.getUserByUserId(session.getConfirmedUserId());
                if(result.getCode() == 0){
                    response.setUserName(result.getResult().getDisplayName());
                    response.setPortrait(result.getResult().getPortrait());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return RestResult.result(ERROR_SESSION_NOT_VERIFIED, response);
        }else if(session.getStatus() == 3){
            return RestResult.error(ERROR_SESSION_NOT_VERIFIED);
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
        String userId = (String)subject.getSession().getAttribute("userId");
        return authDataSource.scanPc(userId, token);
    }

    @Override
    public RestResult confirmPc(ConfirmSessionRequest request) {
        return authDataSource.confirmPc(request.getUser_id(), request.getToken());
    }

    @Override
    public RestResult changeName(String newName) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String)subject.getSession().getAttribute("userId");
        try {
            IMResult<InputOutputUserInfo> existUser = UserAdmin.getUserByName(newName);
            if (existUser != null) {
                if (existUser.code == ErrorCode.ERROR_CODE_SUCCESS.code) {
                    if (userId.equals(existUser.getResult().getUserId())) {
                        return RestResult.ok(null);
                    } else {
                        return RestResult.error(ERROR_USER_NAME_ALREADY_EXIST);
                    }
                } else if(existUser.code == ErrorCode.ERROR_CODE_NOT_EXIST.code) {
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
    public RestResult getGroupAnnouncement(String groupId) {
        Optional<Announcement>  announcement = announcementRepository.findById(groupId);
        if (announcement.isPresent()){
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
            String userId = (String)subject.getSession().getAttribute("userId");
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
            String userId = (String)subject.getSession().getAttribute("userId");

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
            if (result!= null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
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
        String userId = (String)subject.getSession().getAttribute("userId");
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
            String userId = (String)subject.getSession().getAttribute("userId");

            if (!StringUtils.isEmpty(createDevice.getDeviceId())) {
                IMResult<OutputDevice> outputDeviceIMResult = UserAdmin.getDevice(createDevice.getDeviceId());
                if (outputDeviceIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    if (outputDeviceIMResult.getResult().getOwners().contains(userId)) {
                        createDevice.setExtra(outputDeviceIMResult.getResult().getExtra());
                        outputDeviceIMResult.getResult().getOwners().remove(userId);
                        createDevice.setOwners(outputDeviceIMResult.getResult().getOwners());
                        IMResult<OutputCreateDevice> result = UserAdmin.createOrUpdateDevice(createDevice);
                        if (result!= null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
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
}
