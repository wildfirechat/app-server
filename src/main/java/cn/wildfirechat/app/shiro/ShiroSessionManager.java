package cn.wildfirechat.app.shiro;


import com.aliyuncs.utils.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.Serializable;
import java.util.Date;

public class ShiroSessionManager extends DefaultWebSessionManager {

    private static final String AUTHORIZATION = "authToken";

    private static final String REFERENCED_SESSION_ID_SOURCE = "Stateless request";

    // 控制 touch 频率：60 秒内不重复更新 lastAccessTime
    // 避免每次请求都触发 Session 序列化和数据库写入
    private static final long TOUCH_INTERVAL_MILLIS = 60 * 1000;

    public ShiroSessionManager(){
        super();
    }

    @Override
    protected Serializable getSessionId(ServletRequest request, ServletResponse response){
        String id = WebUtils.toHttp(request).getHeader(AUTHORIZATION);
        if(StringUtils.isEmpty(id)){
            //如果没有携带id参数则按照父类的方式在cookie进行获取
            System.out.println("super："+super.getSessionId(request, response));
            return super.getSessionId(request, response);
        }else{
            //如果请求头中有 authToken 则其值为sessionId
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE,REFERENCED_SESSION_ID_SOURCE);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID,id);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_IS_VALID,Boolean.TRUE);
            return id;
        }
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
                    // 距离上次访问不到 60 秒，跳过 touch，不触发 SessionDAO.update()
                    return;
                }
            }
        }
        session.touch();
        onChange(session);
    }
}
