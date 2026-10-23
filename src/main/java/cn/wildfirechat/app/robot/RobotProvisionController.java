package cn.wildfirechat.app.robot;

import cn.wildfirechat.app.RestResult;
import cn.wildfirechat.app.pojo.RobotProvisionRequest;
import cn.wildfirechat.app.pojo.RobotProvisionResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 登录用户开通/复用 DSH 机器人。
 *
 * 该接口默认受 Shiro `/** = login` 保护；不接受匿名调用。
 */
@RestController
public class RobotProvisionController {
    private static final Logger LOG = LoggerFactory.getLogger(RobotProvisionController.class);

    @Autowired
    private RobotProvisionService provisionService;

    @CrossOrigin
    @PostMapping(value = "/robot/provision", produces = "application/json;charset=UTF-8")
    public Object provision(@RequestBody(required = false) RobotProvisionRequest request) {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        if (userId == null || userId.trim().isEmpty()) {
            return RestResult.error(RestResult.RestCode.ERROR_NOT_LOGIN);
        }
        boolean forceRefresh = request != null && Boolean.TRUE.equals(request.getForceRefresh());
        try {
            RobotProvisionResponse response = provisionService.provision(userId, forceRefresh);
            return RestResult.ok(response);
        } catch (RobotProvisionException e) {
            LOG.warn("robot provision failed for user {}: {}", userId, e.getMessage());
            return RestResult.result(e.getCode(), e.getMessage(), null);
        } catch (Exception e) {
            LOG.error("robot provision error for user {}", userId, e);
            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
        }
    }
}
