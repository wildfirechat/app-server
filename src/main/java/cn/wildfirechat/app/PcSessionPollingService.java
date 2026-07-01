package cn.wildfirechat.app;

import cn.wildfirechat.app.jpa.PCSession;
import cn.wildfirechat.app.jpa.PCSessionRepository;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PC 扫码登录长轮询的统一轮询服务。
 * <p>
 * 每个节点维护本节点挂起的 HTTP 长连接（DeferredResult），由一个后台线程每秒批量查询数据库，
 * 根据 pc_session 表的状态变化触发对应的 DeferredResult。多节点部署时，各节点只轮询自己挂起的连接，
 * 手机端扫码更新数据库后，所有节点最终都能通过轮询感知到状态变化。
 */
@org.springframework.stereotype.Service
public class PcSessionPollingService {
    private static final Logger LOG = LoggerFactory.getLogger(PcSessionPollingService.class);

    @Autowired
    private Service mService;

    @Autowired
    private PCSessionRepository pcSessionRepository;

    private static final int TIMEOUT_SECONDS = 50;
    private static final int MAX_PENDING_SIZE = 10000;

    private static final ResponseEntity<RestResult> TIMEOUT_RESPONSE =
            new ResponseEntity<>(RestResult.error(RestResult.RestCode.ERROR_SESSION_EXPIRED), HttpStatus.OK);

    /** 本节点挂起的 PC 登录长轮询，key=token */
    private final ConcurrentHashMap<String, DeferredResult<ResponseEntity>> pendingPcLogin = new ConcurrentHashMap<>();

    public void register(String token, DeferredResult<ResponseEntity> deferredResult) {
        if (pendingPcLogin.size() >= MAX_PENDING_SIZE) {
            LOG.error("pending pc login exceed max size {}", MAX_PENDING_SIZE);
            deferredResult.setResult(ResponseEntity.ok(RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR)));
            return;
        }

        DeferredResult<ResponseEntity> old = pendingPcLogin.put(token, deferredResult);
        if (old != null && !old.isSetOrExpired()) {
            LOG.warn("replace old pending pc login for token {}", token);
            old.setResult(ResponseEntity.ok(RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR)));
        }

        deferredResult.onCompletion(() -> pendingPcLogin.remove(token, deferredResult));
    }

    @Scheduled(fixedRate = 1000)
    public void poll() {
        if (pendingPcLogin.isEmpty()) {
            return;
        }

        List<String> tokens = new ArrayList<>(pendingPcLogin.keySet());

        // 批量查询所有 pending token 的状态，避免对每个 token 单独查库
        Map<String, PCSession> sessionMap;
        try {
            sessionMap = new HashMap<>();
            for (PCSession session : pcSessionRepository.findByTokenIn(tokens)) {
                sessionMap.put(session.getToken(), session);
            }
        } catch (Exception e) {
            LOG.error("batch query pc session error", e);
            sessionMap = new HashMap<>();
        }

        for (String token : tokens) {
            DeferredResult<ResponseEntity> deferredResult = pendingPcLogin.get(token);
            if (deferredResult == null || deferredResult.isSetOrExpired()) {
                pendingPcLogin.remove(token);
                continue;
            }

            PCSession pcSession = sessionMap.get(token);
            if (pcSession == null) {
                // token 已被清理，直接超时
                deferredResult.setResult(TIMEOUT_RESPONSE);
                continue;
            }

            // 状态未变化（仍是 Created），继续等待；只有状态变化时才走完整业务处理
            if (pcSession.getStatus() == PCSession.PCSessionStatus.Session_Created) {
                continue;
            }

            try {
                RestResult restResult = mService.loginWithSession(token);
                int code = restResult.getCode();

                if (code == RestResult.RestCode.ERROR_SESSION_NOT_VERIFIED.code && restResult.getResult() != null) {
                    // 已扫码，PC 端展示用户信息并等待确认
                    deferredResult.setResult(new ResponseEntity<>(restResult, HttpStatus.OK));
                } else if (code == RestResult.RestCode.SUCCESS.code
                        || code == RestResult.RestCode.ERROR_SESSION_EXPIRED.code
                        || code == RestResult.RestCode.ERROR_SERVER_ERROR.code
                        || code == RestResult.RestCode.ERROR_SESSION_CANCELED.code
                        || code == RestResult.RestCode.ERROR_CODE_INCORRECT.code) {
                    // 终态，直接返回
                    ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
                    if (code == RestResult.RestCode.SUCCESS.code) {
                        Subject subject = SecurityUtils.getSubject();
                        builder.header("authToken", subject.getSession().getId().toString());
                    }
                    deferredResult.setResult(builder.body(restResult));
                }
            } catch (Exception e) {
                LOG.error("poll pc session error, token: {}", token, e);
                deferredResult.setResult(new ResponseEntity<>(RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR), HttpStatus.OK));
            }
        }
    }
}
