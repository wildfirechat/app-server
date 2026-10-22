package cn.wildfirechat.app.shiro;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.Cookie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 登录请求不能继承旧 authToken / Cookie，否则 subject.login() 会复用旧 session，
 * 第二个用户会拿到和第一个用户相同的 authToken。
 *
 * <p>DispatcherServlet 映射在 "/"，所以应用内路径就是 servletPath，测试里按这个方式构造请求。</p>
 */
public class ShiroSessionManagerTest {

    private final ShiroSessionManager manager = new ShiroSessionManager();

    private static MockHttpServletRequest request(String contextPath, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath(contextPath);
        request.setServletPath(servletPath);
        request.setRequestURI(contextPath + servletPath);
        return request;
    }

    @Test
    public void loginPathsIgnoreAuthTokenAndCookie() {
        String[] loginPaths = {"/login", "/login_pwd", "/session_login/token-1"};

        for (String path : loginPaths) {
            MockHttpServletRequest request = request("", path);
            request.addHeader("authToken", "old-token");
            request.setCookies(new Cookie("JSESSIONID", "old-cookie"));

            assertNull("登录路径 " + path + " 必须强制新建会话",
                    manager.getSessionId(request, new MockHttpServletResponse()));
        }
    }

    @Test
    public void loginPathWithTrailingSlashAndContextPathStillIgnoresSession() {
        MockHttpServletRequest request = request("/app", "/login_pwd/");
        request.addHeader("authToken", "old-token");

        assertNull(manager.getSessionId(request, new MockHttpServletResponse()));
    }

    @Test
    public void nonCanonicalLoginPathStillIgnoresSession() {
        String[] loginPaths = {"//login_pwd", "/./login", "/login;a=b"};

        for (String path : loginPaths) {
            MockHttpServletRequest request = request("", path);
            request.addHeader("authToken", "old-token");

            assertNull("登录路径 " + path + " 必须强制新建会话",
                    manager.getSessionId(request, new MockHttpServletResponse()));
        }
    }

    @Test
    public void normalRequestStillUsesAuthToken() {
        MockHttpServletRequest request = request("", "/get_group_announcement");
        request.addHeader("authToken", "token-1");

        assertEquals("token-1", manager.getSessionId(request, new MockHttpServletResponse()));
    }

    @Test
    public void normalRequestStillFallsBackToCookie() {
        MockHttpServletRequest request = request("", "/get_group_announcement");
        request.setCookies(new Cookie("JSESSIONID", "cookie-token"));

        assertEquals("cookie-token", manager.getSessionId(request, new MockHttpServletResponse()));
    }
}
