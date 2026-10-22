package cn.wildfirechat.app;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AudioControllerTest {

    private AudioController controller;
    private File cacheDir;

    @Before
    public void setUp() throws Exception {
        cacheDir = Files.createTempDirectory("amr2mp3-test").toFile();
        controller = new AudioController();
        ReflectionTestUtils.setField(controller, "cacheDirPath", cacheDir.getAbsolutePath());
        ReflectionTestUtils.setField(controller, "ossGeneralBucketDomain", "http://media.example.com");
        ReflectionTestUtils.setField(controller, "ossVoiceBucketDomain", "http://10.0.0.8:9000");
        ReflectionTestUtils.setField(controller, "ossFileBucketDomain", "");
        controller.init();
    }

    @After
    public void tearDown() {
        controller.destroy();
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        cacheDir.delete();
    }

    private URL validate(String url) {
        return ReflectionTestUtils.invokeMethod(controller, "validateAmrUrl", url);
    }

    @Test
    public void allowlistChecksPort() {
        assertNotNull(validate("http://10.0.0.8:9000/voice/a.amr"));
        assertNull("白名单主机的其他端口必须被拒绝", validate("http://10.0.0.8:6379/a.amr"));
        assertNull(validate("http://10.0.0.8/a.amr"));

        // 域名没写端口时放行 80 和 443
        assertNotNull(validate("http://media.example.com/a.amr"));
        assertNotNull(validate("https://media.example.com/a.amr"));
        assertNull(validate("http://media.example.com:8080/a.amr"));
    }

    @Test
    public void cacheKeyIgnoresQueryFragmentAndHostCase() throws Exception {
        String key = AudioController.cacheKey(new URL("http://media.example.com/voice/a.amr"));
        assertEquals(key, AudioController.cacheKey(new URL("http://media.example.com/voice/a.amr?sign=1")));
        assertEquals(key, AudioController.cacheKey(new URL("http://media.example.com/voice/a.amr#2")));
        assertEquals(key, AudioController.cacheKey(new URL("http://MEDIA.example.com:80/voice/a.amr")));
        assertNotEquals(key, AudioController.cacheKey(new URL("http://media.example.com/voice/b.amr")));
    }

    @Test
    public void cachedResultIsNotRateLimited() throws Exception {
        String amrUrl = "http://media.example.com/voice/a.amr";
        File cached = new File(cacheDir, AudioController.cacheKey(new URL(amrUrl)) + ".mp3");
        Files.write(cached.toPath(), new byte[]{1, 2, 3});

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("8.8.8.8");
        for (int i = 0; i < 100; i++) {
            ResponseEntity<?> response = controller.amr2mp3(amrUrl, request).get();
            assertEquals(200, response.getStatusCodeValue());
        }
    }

    @Test
    public void forwardedHeadersOnlyTrustedFromProxy() {
        MockHttpServletRequest direct = new MockHttpServletRequest();
        direct.setRemoteAddr("8.8.8.8");
        direct.addHeader("X-Forwarded-For", "1.2.3.4");
        assertEquals("公网直连时不能信任转发头", "8.8.8.8", AudioController.getIp(direct));

        MockHttpServletRequest local = new MockHttpServletRequest();
        local.setRemoteAddr("127.0.0.1");
        local.addHeader("X-Real-IP", "1.2.3.4");
        assertEquals("1.2.3.4", AudioController.getIp(local));

        MockHttpServletRequest docker = new MockHttpServletRequest();
        docker.setRemoteAddr("172.17.0.2");
        docker.addHeader("X-Forwarded-For", "5.6.7.8, 10.0.0.1");
        assertEquals("5.6.7.8", AudioController.getIp(docker));
    }
}
