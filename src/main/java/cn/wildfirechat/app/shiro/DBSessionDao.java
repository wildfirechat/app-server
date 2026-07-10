package cn.wildfirechat.app.shiro;

import cn.wildfirechat.app.jpa.ShiroSession;
import cn.wildfirechat.app.jpa.ShiroSessionRepository;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class DBSessionDao implements SessionDAO {
    private static final Logger LOG = LoggerFactory.getLogger(DBSessionDao.class);

    /** 缓存条目存活时间 */
    private static final long CACHE_TTL_MINUTES = 60;

    /** 用于脏检查：缓存已持久化的 session 序列化数据，避免无变化时重复写入数据库 */
    private final Cache<Object, byte[]> sessionDataCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .build();
    /** 读缓存：避免同一 session 在短时间内重复读库 */
    private final Cache<Object, Session> sessionReadCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .build();

    @Autowired
    private ShiroSessionRepository shiroSessionRepository;

    @Override
    public Serializable create(Session session) {
        String sessionId = UUID.randomUUID().toString().replaceAll("-", "");
        ((SimpleSession) session).setId(sessionId);
        // 创建时即入库，避免应用重启后 session 丢失
        update(session);
        return sessionId;
    }

    @Override
    public Session readSession(Serializable sessionId) throws UnknownSessionException {
        Session cached = sessionReadCache.getIfPresent(sessionId);
        if (cached != null) {
            return cached;
        }

        ShiroSession shiroSession = shiroSessionRepository.findById((String) sessionId).orElse(null);
        if (shiroSession != null) {
            Session session = byteToSession(shiroSession.getSessionData());
            if (session != null) {
                // 同步到缓存，用于后续脏检查比对
                sessionDataCache.put(sessionId, shiroSession.getSessionData());
                sessionReadCache.put(sessionId, session);
                // 读缓存过期后仍被访问，说明 session 仍在被使用；
                // 刷新 update_time 以免 60 秒 touch 节流导致活跃 session 被清理任务误删
                try {
                    shiroSessionRepository.updateAccessTime((String) sessionId, System.currentTimeMillis());
                } catch (Exception e) {
                    LOG.error("update session access time error, sessionId: {}", sessionId, e);
                }
            }
            return session;
        }
        return null;
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        byte[] currentBytes = sessionToByte(session);
        if (currentBytes == null) {
            return;
        }
        Object sessionId = session.getId();
        byte[] cachedBytes = sessionDataCache.getIfPresent(sessionId);

        // 脏检查：如果数据未变化，直接跳过数据库写入
        if (cachedBytes != null && Arrays.equals(cachedBytes, currentBytes)) {
            return;
        }

        ShiroSession shiroSession = new ShiroSession((String) sessionId, currentBytes);
        shiroSession.setUpdateTime(System.currentTimeMillis());
        shiroSessionRepository.save(shiroSession);
        sessionDataCache.put(sessionId, currentBytes);
        sessionReadCache.put(sessionId, session);
    }

    @Override
    public void delete(Session session) {
        Object sessionId = session.getId();
        sessionDataCache.invalidate(sessionId);
        sessionReadCache.invalidate(sessionId);
        shiroSessionRepository.deleteById((String) sessionId);
    }

    @Override
    public Collection<Session> getActiveSessions() {
        // 原 sessionMap 未被实际维护，直接返回空集合即可
        // 如需准确数据，应从数据库查询
        return java.util.Collections.emptyList();
    }

    // convert session object to byte, then store it to redis
    private byte[] sessionToByte(Session session){
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] bytes = null;
        try {
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(session);
            bytes = bo.toByteArray();
        } catch (IOException e) {
            LOG.error("IOException", e);
        }
        return bytes;
    }

    // restore session
    private Session byteToSession(byte[] bytes){
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
        ObjectInputStream in;
        SimpleSession session = null;
        try {
            in = new ObjectInputStream(bi);
            session = (SimpleSession) in.readObject();
        } catch (ClassNotFoundException e) {
            LOG.error("ClassNotFoundException", e);
        } catch (IOException e) {
            LOG.error("IOException", e);
        }

        return session;
    }
}
