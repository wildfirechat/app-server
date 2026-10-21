package cn.wildfirechat.app.avatar;

import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertNotNull;

public class GroupAvatarUtilTest {

    /**
     * Spring Boot 可执行 jar 里的默认头像 URL 是 jar: 协议。
     * 校验逻辑必须放行内部 jar: 资源，否则会报 "Unavailable avatar url"。
     */
    @Test
    public void openConnectionAllowsInternalJarResource() throws Exception {
        Path jar = Files.createTempFile("avatar-test", ".jar");
        try {
            byte[] png = Files.readAllBytes(Paths.get("src/main/resources/static/avatar/avatar_def.png"));
            try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
                out.putNextEntry(new JarEntry("avatar_def.png"));
                out.write(png);
                out.closeEntry();
            }

            URL url = new URL("jar:" + jar.toUri().toURL() + "!/avatar_def.png");
            Method method = GroupAvatarUtil.class.getDeclaredMethod("openConnectionSafely", URL.class);
            method.setAccessible(true);
            URLConnection connection = (URLConnection) method.invoke(null, url);

            assertNotNull(connection);
            try (InputStream in = connection.getInputStream()) {
                assertNotNull(in);
            }
        } finally {
            Files.deleteIfExists(jar);
        }
    }
}
