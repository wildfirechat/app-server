package cn.wildfirechat.app.avatar;

import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GroupAvatarUtilTest {

    private static URLConnection openConnectionSafely(URL url, Predicate<URL> check) throws Throwable {
        Method method = GroupAvatarUtil.class.getDeclaredMethod("openConnectionSafely", URL.class, Predicate.class);
        method.setAccessible(true);
        try {
            return (URLConnection) method.invoke(null, url, check);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static BufferedImage readImage(byte[] data, int width, int height) throws Throwable {
        Method method = GroupAvatarUtil.class.getDeclaredMethod("readImage", InputStream.class, int.class, int.class);
        method.setAccessible(true);
        try {
            return (BufferedImage) method.invoke(null, new ByteArrayInputStream(data), width, height);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** 启动一个本地 HTTP 服务，/start 返回 302 跳到 location。 */
    private static HttpServer redirectServer(String location) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/start", exchange -> {
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static URL startUrl(HttpServer server) throws IOException {
        return new URL("http://127.0.0.1:" + server.getAddress().getPort() + "/start");
    }

    /**
     * Spring Boot 可执行 jar 里的默认头像 URL 是 jar: 协议。
     * 校验逻辑必须放行内部 jar: 资源，否则会报 "Unavailable avatar url"。
     */
    @Test
    public void openConnectionAllowsInternalJarResource() throws Throwable {
        Path jar = Files.createTempFile("avatar-test", ".jar");
        try {
            byte[] png = Files.readAllBytes(Paths.get("src/main/resources/static/avatar/avatar_def.png"));
            try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
                out.putNextEntry(new JarEntry("avatar_def.png"));
                out.write(png);
                out.closeEntry();
            }

            URL url = new URL("jar:" + jar.toUri().toURL() + "!/avatar_def.png");
            URLConnection connection = openConnectionSafely(url, u -> false);

            assertNotNull(connection);
            try (InputStream in = connection.getInputStream()) {
                assertNotNull(in);
            }
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    /** 外部地址重定向到 file: 必须被拒绝，file/jar 只允许作为首跳的内部资源。 */
    @Test
    public void redirectToFileIsRejected() throws Throwable {
        Path local = Files.createTempFile("avatar-local", ".png");
        HttpServer server = redirectServer(local.toUri().toString());
        try {
            openConnectionSafely(startUrl(server), u -> true);
            fail("重定向到 file: 必须被拒绝");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Unavailable avatar url"));
        } finally {
            server.stop(0);
            Files.deleteIfExists(local);
        }
    }

    /** 重定向后的每一跳都要重新经过校验（例如白名单）。 */
    @Test
    public void redirectTargetIsCheckedAgain() throws Throwable {
        HttpServer server = redirectServer("/blocked");
        try {
            openConnectionSafely(startUrl(server), u -> u.getPath().equals("/start"));
            fail("重定向目标未通过校验时必须被拒绝");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Unavailable avatar url"));
        } finally {
            server.stop(0);
        }
    }

    /** 声明的像素超过上限时，不解码直接拒绝。 */
    @Test
    public void oversizedImageIsRejected() throws Throwable {
        try {
            readImage(png(5000, 5000), 36, 36);
            fail("超过像素上限的图片必须被拒绝");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("too large"));
        }
    }

    /** 正常大图按比例降采样解码，结果不小于目标尺寸。 */
    @Test
    public void largeImageIsSubsampled() throws Throwable {
        BufferedImage image = readImage(png(1000, 1000), 36, 36);

        assertNotNull(image);
        assertTrue(image.getWidth() >= 36 && image.getWidth() < 1000);
    }
}
