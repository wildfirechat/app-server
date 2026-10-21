package cn.wildfirechat.app.shiro;


import com.aliyuncs.utils.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Date;

public class ShiroSessionManager extends DefaultWebSessionManager {
    private static final Logger LOG = LoggerFactory.getLogger(ShiroSessionManager.class);

    private static final String AUTHORIZATION = "authToken";

    private static final String REFERENCED_SESSION_ID_SOURCE = "Stateless request";

    // touch 节流：30 分钟内不重复写库。
    // update_time 只用于 90 天清理保活，30 分钟的粒度足够；会话本身不会过期。
    // 如果以后把 globalSessionTimeout 改成有限值，需要保证它大于这个间隔。
    private static final long TOUCH_INTERVAL_MILLIS = 30 * 60 * 1000L;

    public ShiroSessionManager(){
        super();
    }

    @Override
    protected Serializable getSessionId(ServletRequest request, ServletResponse response){
        HttpServletRequest http = WebUtils.toHttp(request);
        String path = http.getRequestURI();
        String contextPath = http.getContextPath();
        if ("/".equals(contextPath)) {
            contextPath = "";
        }
        if (contextPath != null && !contextPath.isEmpty()
                && (path.equals(contextPath) || path.startsWith(contextPath + "/"))) {
            path = path.substring(contextPath.length());
        }

        // 登录请求必须新建会话：不能复用请求里携带的旧 authToken / Cookie。
        // 否则 A 登录后，B 带着 A 的 token 再登录，Shiro 会把 B 的 userId 写进 A 的会话，
        // 两个用户就会拿到同一个 authToken。
        if (isLoginPath(path)) {
            return null;
        }

        String id = http.getHeader(AUTHORIZATION);
        if(StringUtils.isEmpty(id)){
            //如果没有携带id参数则按照父类的方式在cookie进行获取
            LOG.info("super：{}", super.getSessionId(request, response));
            return super.getSessionId(request, response);
        }else{
            //如果请求头中有 authToken 则其值为sessionId
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE,REFERENCED_SESSION_ID_SOURCE);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID,id);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_IS_VALID,Boolean.TRUE);
            return id;
        }
    }

    /**
     * 需要强制换新会话的登录入口。
     *
     * <p>这些接口全部是 {@code anon}，但 ShiroFilter 仍会先解析请求里的 authToken/Cookie；
     * 不在这里跳过的话，{@code subject.login()} 会复用旧 session，导致 session ID 不轮换。
     */
    static boolean isLoginPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return "/login".equals(path)
                || "/login_pwd".equals(path)
                || "/session_login".equals(path)
                || path.startsWith("/session_login/");
    }

    @Override
    public void touch(SessionKey key) {
        Session session = retrieveSession(key);
        if (session == null) {
            return;
        }
        if (session instanceof SimpleSession) {
            SimpleSession ss = (SimpleSession) session;
            Date lastAccessTime = ss.getLastAccessTime();
            if (lastAccessTime != null) {
                long elapsed = System.currentTimeMillis() - lastAccessTime.getTime();
                if (elapsed < TOUCH_INTERVAL_MILLIS) {
                    // 距上次写库不足 30 分钟，直接跳过
                    return;
                }
            }
        }
        session.touch();

        // 会话永不过期时（timeout < 0 或 Long.MAX_VALUE），lastAccessTime 不参与
        // 过期判断，而请求期间 session 的业务属性也不会变化，因此只需要刷新
        // update_time 供清理任务判断活跃度，不必序列化并重写整个 session_data。
        // 如果超时是有限值，仍走全量更新，避免重启/读缓存加载到过旧的
        // lastAccessTime 而误判会话过期。
        long timeout = session.getTimeout();
        boolean neverExpires = timeout < 0 || timeout == Long.MAX_VALUE;
        if (neverExpires && getSessionDAO() instanceof DBSessionDao) {
            try {
                ((DBSessionDao) getSessionDAO()).updateLastAccessTime(session.getId());
            } catch (Exception e) {
                LOG.error("update session access time error, sessionId: {}", session.getId(), e);
            }
            return;
        }
        onChange(session);
    }
}
