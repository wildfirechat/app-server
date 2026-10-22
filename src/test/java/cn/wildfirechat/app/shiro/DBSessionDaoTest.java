package cn.wildfirechat.app.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * 会话反序列化白名单：已登录会话必须能读回来，白名单外的类型必须被拒绝。
 */
public class DBSessionDaoTest {

    private final DBSessionDao dao = new DBSessionDao();

    private Session roundTrip(Session session) {
        byte[] bytes = ReflectionTestUtils.invokeMethod(dao, "sessionToByte", session);
        return ReflectionTestUtils.invokeMethod(dao, "byteToSession", (Object) bytes);
    }

    @Test
    public void loggedInSessionCanBeRestored() {
        SimpleSession session = new SimpleSession("127.0.0.1");
        session.setId("login");
        session.setAttribute("userId", "u1");
        session.setAttribute("count", 1L);
        session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY,
                new SimplePrincipalCollection("13800000000", "phoneCodeRealm"));
        session.setAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY, Boolean.TRUE);

        Session restored = roundTrip(session);

        assertNotNull("已登录会话必须能反序列化", restored);
        assertEquals("u1", restored.getAttribute("userId"));
        assertEquals(1L, restored.getAttribute("count"));
        assertEquals("13800000000",
                ((SimplePrincipalCollection) restored.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY))
                        .getPrimaryPrincipal());
    }

    @Test
    public void sessionWithUnlistedClassIsRejected() {
        SimpleSession session = new SimpleSession("127.0.0.1");
        session.setId("bad");
        session.setAttribute("other", new TreeMap<String, String>());

        assertNull("白名单外的类型必须被拒绝", roundTrip(session));
    }
}
