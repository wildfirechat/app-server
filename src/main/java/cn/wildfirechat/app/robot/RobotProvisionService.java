package cn.wildfirechat.app.robot;

import cn.wildfirechat.app.pojo.RobotProvisionResponse;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.InputCreateRobot;
import cn.wildfirechat.pojos.OutputCreateRobot;
import cn.wildfirechat.pojos.OutputRobot;
import cn.wildfirechat.pojos.OutputStringList;
import cn.wildfirechat.sdk.RelationAdmin;
import cn.wildfirechat.sdk.UserAdmin;
import cn.wildfirechat.sdk.model.IMResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 为登录用户开通/复用 DSH 机器人。
 *
 * 不持久化用户↔机器人绑定：每次 provision 都从 IM 服务端
 * `UserAdmin.getUserRobots(userId)` 查询该用户的机器人列表，取第一个；
 * 列表为空才为该用户创建。这样不会全局复用同一个机器人，也不会出现
 * 本地绑定与 IM 服务端不一致。
 */
@Service
public class RobotProvisionService {
    private static final Logger LOG = LoggerFactory.getLogger(RobotProvisionService.class);

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Value("${robot.gateway_url:ws://127.0.0.1:8884/robot/gateway}")
    private String defaultGatewayUrl;

    @Value("${im.robot_api_url:http://localhost}")
    private String defaultImServerUrl;

    private static final String FALLBACK_DISPLAY_NAME = "DSH 助手";

    @Value("${robot.config_version:1}")
    private int baseConfigVersion;

    @Value("${robot.callback_url:}")
    private String configuredCallbackUrl;

    @Value("${ai.provider:openai-compatible}")
    private String aiProvider;

    @Value("${ai.base_url:}")
    private String aiBaseUrl;

    @Value("${ai.api_key:}")
    private String aiApiKey;

    @Value("${ai.model:deepseek-v4-pro}")
    private String aiModel;

    @Value("${ai.reasoning_effort:high}")
    private String aiReasoningEffort;

    @Value("${ai.max_tokens:8192}")
    private int aiMaxTokens;

    @Value("${ai.context_window:131072}")
    private int aiContextWindow;

    @Value("${ai.models:}")
    private String aiModels;

    public RobotProvisionResponse provision(String userId, boolean forceRefresh) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new RobotProvisionException(13, "没有登录");
        }
        String key = userId.intern();
        synchronized (key) {
            // 每次都从 IM 服务端取该用户的机器人列表，不做本地记录。
            RobotCredentials credentials = findFirstOwnedRobot(userId);
            if (credentials == null) {
                credentials = createRobotForUser(userId);
            }
            // 自动把机器人加为好友，客户端可直接在通讯录/单聊里找到它。
            try {
                RelationAdmin.setUserFriend(userId, credentials.robotId, true, null);
            } catch (Exception e) {
                LOG.warn("setUserFriend failed for {} -> {}", userId, credentials.robotId, e);
            }
            LOG.info("provisioned robot {} for user {} (gateway={}, forceRefresh={})",
                credentials.robotId, userId, defaultGatewayUrl, forceRefresh);
            return buildResponse(userId, credentials);
        }
    }

    /**
     * 调 IM 服务查询该用户拥有的机器人，返回第一个可用机器人。
     * 空列表返回 null；查询失败抛异常；机器人不存在/不属于该用户时跳过。
     */
    private RobotCredentials findFirstOwnedRobot(String userId) {
        IMResult<OutputStringList> robots;
        try {
            robots = UserAdmin.getUserRobots(userId);
        } catch (Exception e) {
            LOG.error("getUserRobots failed for {}", userId, e);
            throw new RobotProvisionException(1002, "获取机器人列表失败，请稍后重试");
        }
        if (robots == null || robots.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            LOG.warn("getUserRobots returned {} / {}", robots == null ? "null" : robots.getCode(),
                robots == null ? "null" : robots.getMsg());
            throw new RobotProvisionException(1002, "获取机器人列表失败，请稍后重试");
        }
        OutputStringList list = robots.getResult();
        if (list == null || list.getList() == null || list.getList().isEmpty()) {
            return null;
        }
        for (String robotId : list.getList()) {
            if (robotId == null || robotId.trim().isEmpty()) continue;
            try {
                IMResult<OutputRobot> info = UserAdmin.getRobotInfo(robotId);
                if (info == null || info.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS || info.getResult() == null) {
                    continue;
                }
                OutputRobot robot = info.getResult();
                if (robot.getOwner() == null || !userId.equals(robot.getOwner())) {
                    LOG.warn("robot {} is not owned by {} (owner={}), skip", robotId, userId, robot.getOwner());
                    continue;
                }
                if (robot.getSecret() == null || robot.getSecret().isEmpty()) {
                    LOG.warn("robot {} has no secret, skip", robotId);
                    continue;
                }
                ensureRobotCallback(robot);
                return new RobotCredentials(robot.getUserId() == null ? robotId : robot.getUserId(),
                    robot.getSecret(), robot.getDisplayName(), robot.getPortrait(), userId);
            } catch (Exception e) {
                LOG.warn("getRobotInfo failed for {}; try next", robotId, e);
            }
        }
        return null;
    }

    private RobotCredentials createRobotForUser(String userId) {
        // 参考 BotFather：robot_<userId>_<6位随机数>，并先查 IM 是否已存在。
        RobotCredentials created = tryCreateRobot(userId, generateUniqueRobotId(userId));
        if (created != null) return created;
        // 极小概率：生成/创建竞争失败，再生成一次。
        return tryCreateRobot(userId, generateUniqueRobotId(userId));
    }

    private RobotCredentials tryCreateRobot(String userId, String robotId) {
        String secret = randomSecret();
        String baseName = userDisplayName(userId);
        String displayName = baseName == null || baseName.isEmpty() ? FALLBACK_DISPLAY_NAME : baseName + " 的机器人";
        InputCreateRobot input = new InputCreateRobot();
        input.setUserId(robotId);
        input.setName(robotId);
        input.setDisplayName(displayName);
        input.setOwner(userId);
        input.setSecret(secret);
        input.setExtra("{\"source\":\"wf-pc-dsh\",\"owner\":\"" + userId + "\"}");
        input.setRobotExtra("{\"provider\":\"dsh\"}");
        String callbackUrl = resolveCallbackUrl();
        if (callbackUrl != null) {
            input.setCallback(callbackUrl);
        } else {
            LOG.warn("robot.callback_url is not configured and cannot be derived from gateway_url");
        }

        try {
            IMResult<OutputCreateRobot> created = UserAdmin.createRobot(input);
            if (created != null && created.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS
                && created.getResult() != null) {
                String id = created.getResult().getUserId() == null ? robotId : created.getResult().getUserId();
                String returnedSecret = created.getResult().getSecret();
                return verifyOwned(userId, id, returnedSecret == null || returnedSecret.isEmpty() ? secret : returnedSecret);
            }
            LOG.warn("createRobot returned {} / {}", created == null ? "null" : created.getCode(),
                created == null ? "null" : created.getMsg());
        } catch (Exception e) {
            LOG.warn("createRobot failed for {}; trying getRobotInfo", robotId, e);
        }

        // 已存在/创建失败：回查；只有 owner 匹配才复用
        try {
            IMResult<OutputRobot> existing = UserAdmin.getRobotInfo(robotId);
            if (existing != null && existing.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS
                && existing.getResult() != null) {
                OutputRobot robot = existing.getResult();
                if (userId.equals(robot.getOwner())) {
                    String returnedSecret = robot.getSecret();
                    return new RobotCredentials(robot.getUserId() == null ? robotId : robot.getUserId(),
                        returnedSecret == null || returnedSecret.isEmpty() ? secret : returnedSecret,
                        robot.getDisplayName(), robot.getPortrait(), userId);
                }
                LOG.warn("robot {} exists but owner={} != {}, will not reuse", robotId, robot.getOwner(), userId);
            }
        } catch (Exception e) {
            LOG.error("getRobotInfo failed for {}", robotId, e);
        }
        return null;
    }

    private RobotCredentials verifyOwned(String userId, String robotId, String secret) {
        try {
            IMResult<OutputRobot> info = UserAdmin.getRobotInfo(robotId);
            if (info != null && info.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && info.getResult() != null) {
                OutputRobot robot = info.getResult();
                if (!userId.equals(robot.getOwner())) {
                    LOG.warn("created robot {} owner={} != {}, reject", robotId, robot.getOwner(), userId);
                    return null;
                }
                return new RobotCredentials(robotId, secret, robot.getDisplayName(), robot.getPortrait(), userId);
            }
        } catch (Exception e) {
            LOG.warn("verify created robot failed for {}", robotId, e);
        }
        // 查询失败但创建成功：保守返回，后续 provision 会以列表为准修正。
        return new RobotCredentials(robotId, secret, FALLBACK_DISPLAY_NAME, "", userId);
    }

    /**
     * 机器人回调地址：优先 robot.callback_url；否则从 gateway_url 推导网关 HTTP 地址。
     * 例如 gatewayUrl=ws://gateway:8884/robot/gateway
     *      => http://gateway:8885/robot/recvmsg
     */
    private String resolveCallbackUrl() {
        if (configuredCallbackUrl != null && !configuredCallbackUrl.trim().isEmpty()) {
            return configuredCallbackUrl.trim();
        }
        try {
            URL url = new URL(defaultGatewayUrl);
            String host = url.getHost();
            if (host == null || host.isEmpty()) return null;
            return "http://" + host + ":8885/robot/recvmsg";
        } catch (Exception e) {
            LOG.warn("cannot derive callback URL from gateway_url={}", defaultGatewayUrl, e);
            return null;
        }
    }

    /**
     * 已存在的机器人如果 callback 不是当前网关地址，则通过 createRobot 幂等更新。
     * 保留原 secret/name/displayName 等，只补 callback。
     */
    private void ensureRobotCallback(OutputRobot robot) {
        String callback = resolveCallbackUrl();
        if (callback == null || callback.equals(robot.getCallback())) return;
        try {
            InputCreateRobot update = new InputCreateRobot();
            update.setUserId(robot.getUserId());
            update.setName(robot.getName());
            update.setPassword(robot.getPassword());
            update.setDisplayName(robot.getDisplayName());
            update.setPortrait(robot.getPortrait());
            update.setGender(robot.getGender());
            update.setMobile(robot.getMobile());
            update.setEmail(robot.getEmail());
            update.setAddress(robot.getAddress());
            update.setCompany(robot.getCompany());
            update.setSocial(robot.getSocial());
            update.setExtra(robot.getExtra());
            update.setOwner(robot.getOwner());
            update.setSecret(robot.getSecret());
            update.setCallback(callback);
            update.setRobotExtra(robot.getRobotExtra());
            IMResult<OutputCreateRobot> result = UserAdmin.createRobot(update);
            if (result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                robot.setCallback(callback);
                LOG.info("updated callback for robot {} -> {}", robot.getUserId(), callback);
            } else {
                LOG.warn("update callback failed for robot {}: {}", robot.getUserId(),
                    result == null ? "null" : result.getMsg());
            }
        } catch (Exception e) {
            LOG.warn("update callback failed for robot {}", robot.getUserId(), e);
        }
    }

    private RobotProvisionResponse buildResponse(String userId, RobotCredentials credentials) {
        RobotProvisionResponse response = new RobotProvisionResponse();
        RobotProvisionResponse.Robot robot = new RobotProvisionResponse.Robot();
        robot.setRobotId(credentials.robotId);
        robot.setRobotSecret(credentials.secret);
        robot.setGatewayUrl(defaultGatewayUrl);
        robot.setImServerUrl(defaultImServerUrl);
        robot.setOwner(userId);
        robot.setDisplayName(credentials.displayName == null || credentials.displayName.isEmpty()
            ? FALLBACK_DISPLAY_NAME : credentials.displayName);
        robot.setPortrait(credentials.portrait == null ? "" : credentials.portrait);
        response.setRobot(robot);

        RobotProvisionResponse.Ai ai = new RobotProvisionResponse.Ai();
        ai.setProvider(aiProvider);
        ai.setBaseUrl(aiBaseUrl);
        ai.setApiKey(aiApiKey);
        ai.setModel(aiModel);
        ai.setReasoningEffort(aiReasoningEffort);
        ai.setMaxTokens(aiMaxTokens);
        ai.setContextWindow(aiContextWindow);
        ai.setModels(parseModels(aiModels, aiModel));
        response.setAi(ai);

        RobotProvisionResponse.Features features = new RobotProvisionResponse.Features();
        features.setLine(2);
        features.setWorkspaceMode("multi-group-per-workspace");
        features.setSessionMode("one-session-per-group-per-robot");
        features.setMultiRobot(true);
        response.setFeatures(features);
        response.setConfigVersion(baseConfigVersion);
        response.setUpdatedAt(System.currentTimeMillis());
        return response;
    }

    private List<RobotProvisionResponse.Model> parseModels(String value, String fallback) {
        List<RobotProvisionResponse.Model> models = new ArrayList<>();
        if (value != null && !value.trim().isEmpty()) {
            for (String item : value.split(",")) {
                String part = item.trim();
                if (part.isEmpty()) continue;
                int idx = part.indexOf(':');
                if (idx > 0) {
                    models.add(new RobotProvisionResponse.Model(part.substring(0, idx).trim(), part.substring(idx + 1).trim()));
                } else {
                    models.add(new RobotProvisionResponse.Model(part, part));
                }
            }
        }
        if (models.isEmpty() && fallback != null && !fallback.trim().isEmpty()) {
            models.add(new RobotProvisionResponse.Model(fallback, fallback));
        }
        return models;
    }

    private String userDisplayName(String userId) {
        try {
            IMResult<cn.wildfirechat.pojos.InputOutputUserInfo> result = UserAdmin.getUserByUserId(userId);
            if (result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && result.getResult() != null) {
                String name = result.getResult().getDisplayName();
                if (name == null || name.trim().isEmpty()) name = result.getResult().getName();
                if (name != null && !name.trim().isEmpty()) return name.trim();
            }
        } catch (Exception e) {
            LOG.warn("getUserByUserId failed for {}; fallback display name", userId, e);
        }
        return "";
    }

    private String generateUniqueRobotId(String userId) {
        String baseId = "robot_" + userId + "_";
        for (int i = 0; i < 100; i++) {
            String robotId = baseId + String.format("%06d", random.nextInt(1000000));
            if (!isRobotIdExists(robotId)) return robotId;
        }
        return baseId + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private boolean isRobotIdExists(String robotId) {
        try {
            IMResult<OutputRobot> result = UserAdmin.getRobotInfo(robotId);
            return result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && result.getResult() != null;
        } catch (Exception e) {
            LOG.warn("check robot id failed: {}", robotId, e);
            return false;
        }
    }

    private String randomSecret() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static class RobotCredentials {
        final String robotId;
        final String secret;
        final String displayName;
        final String portrait;
        final String owner;

        RobotCredentials(String robotId, String secret, String displayName, String portrait, String owner) {
            this.robotId = robotId;
            this.secret = secret;
            this.displayName = displayName;
            this.portrait = portrait;
            this.owner = owner;
        }
    }
}
