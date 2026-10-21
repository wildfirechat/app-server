package cn.wildfirechat.app.avatar;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 外部图片地址校验用例。
 *
 * <p>覆盖常见地址形态：公网 http(s)、本机/内网/保留地址、非 http(s) 协议、非标准端口等。</p>
 *
 * <p>说明：使用 IP 字面量作为主机，避免依赖外部 DNS，保证单元测试可离线、确定性运行。</p>
 */
public class AvatarUrlValidatorTest {

    /** 93.184.216.34 为公网地址字面量，解析不触发 DNS。 */
    private static final String PUBLIC_IP = "93.184.216.34";

    @Test
    public void testNormalPublicImageUrlAccepted() {
        AvatarUrlValidator validator = new AvatarUrlValidator();
        assertTrue(validator.isAllowed("https://" + PUBLIC_IP + "/avatar/a.png"));
        assertTrue(validator.isAllowed("http://" + PUBLIC_IP + "/a.jpg"));
        assertTrue(validator.isAllowed("https://" + PUBLIC_IP + ":443/a.webp"));
        assertTrue(validator.isAllowed("http://" + PUBLIC_IP + ":80/a.gif"));
    }

    @Test
    public void testNonHttpSchemeRejected() {
        AvatarUrlValidator validator = new AvatarUrlValidator();
        assertFalse(validator.isAllowed("file:///tmp/a.png"));
        assertFalse(validator.isAllowed("file:///C:/temp/a.png"));
        assertFalse(validator.isAllowed("ftp://" + PUBLIC_IP + "/a.png"));
        assertFalse(validator.isAllowed("jar:https://" + PUBLIC_IP + "/a.jar!/a.png"));
        assertFalse(validator.isAllowed("gopher://" + PUBLIC_IP + "/a"));
    }

    @Test
    public void testEmptyAndNullRejected() {
        AvatarUrlValidator validator = new AvatarUrlValidator();
        assertFalse(validator.isAllowed(null));
        assertFalse(validator.isAllowed(""));
        assertFalse(validator.isAllowed("   "));
        assertFalse(validator.isAllowed("not a url"));
    }

    @Test
    public void testLocalAddressRejected() {
        AvatarUrlValidator validator = new AvatarUrlValidator();
        assertFalse(validator.isAllowed("http://127.0.0.1/a.png"));
        assertFalse(validator.isAllowed("http://127.0.0.1:8888/a.png"));
        assertFalse(validator.isAllowed("http://localhost/a.png"));
        assertFalse(validator.isAllowed("http://[::1]/a.png"));
    }

    @Test
    public void testReservedAddressRejected() {
        AvatarUrlValidator validator = new AvatarUrlValidator();
        assertFalse(validator.isAllowed("http://169.254.1.1/a.png"));
        assertFalse(validator.isAllowed("http://10.0.0.1/a.png"));
        assertFalse(validator.isAllowed("http://192.168.1.1/a.png"));
        assertFalse(validator.isAllowed("http://172.16.0.1/a.png"));
        assertFalse(validator.isAllowed("http://100.64.0.1/a.png"));
    }

    @Test
    public void testInternalHostNameRejected() {
        AvatarUrlValidator validator = new AvatarUrlValidator();
        assertFalse(validator.isAllowed("http://foo.internal/a.png"));
        assertFalse(validator.isAllowed("http://printer.local/a.png"));
        assertFalse(validator.isAllowed("http://app.lan/a.png"));
    }

    @Test
    public void testNonStandardPortRejected() {
        AvatarUrlValidator validator = new AvatarUrlValidator();
        assertFalse(validator.isAllowed("http://" + PUBLIC_IP + ":22/a.png"));
        assertFalse(validator.isAllowed("http://" + PUBLIC_IP + ":3306/a.png"));
        assertFalse(validator.isAllowed("http://" + PUBLIC_IP + ":8080/a.png"));
    }

    @Test
    public void testUserInfoRejected() {
        AvatarUrlValidator validator = new AvatarUrlValidator();
        assertFalse(validator.isAllowed("http://user:pass@" + PUBLIC_IP + "/a.png"));
    }

    @Test
    public void testOptionalAllowlistStillWorks() {
        // 配置了白名单时，只放行名单内主机
        AvatarUrlValidator validator = new AvatarUrlValidator("https://" + PUBLIC_IP);
        assertTrue(validator.isAllowed("https://" + PUBLIC_IP + "/a.png"));
        assertFalse(validator.isAllowed("https://93.184.216.35/a.png"));
    }
}
