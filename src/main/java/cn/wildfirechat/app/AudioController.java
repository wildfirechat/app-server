package cn.wildfirechat.app;

import cn.wildfirechat.app.tools.RateLimiter;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ws.schild.jave.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
public class AudioController {
    private static final Logger LOG = LoggerFactory.getLogger(AudioController.class);

    private static final long MAX_AMR_FILE_SIZE = 50 * 1024 * 1024L;

    /** 外链最多跟随的重定向跳数，每一跳都会重新校验域名。 */
    private static final int MAX_REDIRECTS = 3;

    /** 同时转码的任务数，避免匿名请求把 CPU 打满。 */
    private static final int MAX_CONCURRENT_TRANSCODES = 4;

    /** 等待转码的任务数上限，超过时返回 429。 */
    private static final int MAX_QUEUED_TRANSCODES = 64;

    @Value("${wfc.audio.cache.dir:/tmp/wfc_audio_cache}")
    String cacheDirPath;

    @Value("${media.bucket_general_domain:}")
    private String ossGeneralBucketDomain;

    @Value("${media.bucket_voice_domain:}")
    private String ossVoiceBucketDomain;

    @Value("${media.bucket_file_domain:}")
    private String ossFileBucketDomain;

    private File cacheDir;

    /** 允许下载的 "主机:端口"。域名没写端口时放行 80 和 443。 */
    private final Set<String> allowedHostPorts = new HashSet<>();

    /** 按用户（未登录时按来源 IP）限频：60 秒内最多 60 次转码。 */
    private final RateLimiter rateLimiter = new RateLimiter(60, 60);

    /** 转码专用线程池：超出并发数的任务排队，不占用公共 ForkJoin 线程池。 */
    private final ThreadPoolExecutor transcodeExecutor = new ThreadPoolExecutor(
            MAX_CONCURRENT_TRANSCODES, MAX_CONCURRENT_TRANSCODES, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUED_TRANSCODES), new CustomizableThreadFactory("amr2mp3-"));

    @PostConstruct
    public void init() {
        cacheDir = new File(cacheDirPath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        addHost(ossGeneralBucketDomain);
        addHost(ossVoiceBucketDomain);
        addHost(ossFileBucketDomain);
        if (allowedHostPorts.isEmpty()) {
            LOG.warn("amr2mp3 allowlist is empty, all requests will be rejected");
        } else {
            LOG.info("amr2mp3 allowed hosts: {}", allowedHostPorts);
        }
    }

    @PreDestroy
    public void destroy() {
        transcodeExecutor.shutdown();
    }

    private void addHost(String domain) {
        if (StringUtils.isEmpty(domain)) {
            return;
        }
        URL url;
        try {
            url = new URL(domain.contains("://") ? domain : "http://" + domain);
        } catch (MalformedURLException e) {
            LOG.warn("amr2mp3 ignore invalid media domain: {}", domain);
            return;
        }
        String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
        if (host.isEmpty()) {
            return;
        }
        if (url.getPort() != -1) {
            allowedHostPorts.add(host + ":" + url.getPort());
        } else {
            allowedHostPorts.add(host + ":80");
            allowedHostPorts.add(host + ":443");
        }
    }

    @GetMapping("amr2mp3")
    public CompletableFuture<ResponseEntity<InputStreamResource>> amr2mp3(@RequestParam("path") String amrUrl,
                                                                         HttpServletRequest request) {
        // 1. 基础校验：协议、域名和端口白名单、.amr 后缀
        URL url = validateAmrUrl(amrUrl);
        if (url == null) {
            LOG.warn("amr2mp3 rejected - invalid url: {}", amrUrl);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }

        // 2. 缓存键只取协议、主机、端口和路径，同一个文件不会因为查询参数或 # 片段不同而重复转码
        File mp3File = new File(cacheDir, cacheKey(url) + ".mp3");
        MediaType mediaType = new MediaType("audio", "mp3");
        if (mp3File.exists() && mp3File.length() > 0) {
            try {
                return CompletableFuture.completedFuture(mp3Response(mp3File, mediaType));
            } catch (IOException e) {
                LOG.error("read cached mp3 failed: {}", mp3File.getAbsolutePath(), e);
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
        }

        // 3. 只对需要转码的请求限频，命中缓存的请求不计数
        String limitKey = rateLimitKey(request);
        if (!rateLimiter.isGranted(limitKey)) {
            LOG.warn("amr2mp3 rejected - too frequent, key: {}", limitKey);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
        }

        try {
            return CompletableFuture.supplyAsync(() -> transcode(url, amrUrl, mp3File, mediaType), transcodeExecutor);
        } catch (RejectedExecutionException e) {
            LOG.warn("amr2mp3 rejected - transcode queue is full");
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
        }
    }

    private ResponseEntity<InputStreamResource> transcode(URL url, String amrUrl, File mp3File, MediaType mediaType) {
        File tmpAmr = null;
        File tmpMp3 = null;
        try {
            // 先自己下载到临时文件，校验大小和文件头，再交给 JAVE 转码本地文件。
            // 这样 ffmpeg 不会直接访问用户给的 URL，重定向也绕不到内网。
            tmpAmr = downloadToTemp(url);
            tmpMp3 = File.createTempFile("amr2mp3-", ".mp3", cacheDir);
            amr2mp3(tmpAmr, tmpMp3);
            Files.move(tmpMp3.toPath(), mp3File.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return mp3Response(mp3File, mediaType);
        } catch (IOException | EncoderException e) {
            LOG.error("amr2mp3 failed for {}", amrUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (tmpAmr != null) {
                tmpAmr.delete();
            }
            if (tmpMp3 != null) {
                tmpMp3.delete();
            }
        }
    }

    private URL validateAmrUrl(String amrUrl) {
        if (StringUtils.isEmpty(amrUrl)) {
            return null;
        }
        URL url;
        try {
            url = new URL(amrUrl);
        } catch (MalformedURLException e) {
            return null;
        }
        if (url.getUserInfo() != null) {
            return null;
        }
        if (!isAllowedHost(url)) {
            return null;
        }
        String path = url.getPath() == null ? "" : url.getPath().toLowerCase(Locale.ROOT);
        if (!path.endsWith(".amr")) {
            return null;
        }
        return url;
    }

    private boolean isAllowedHost(URL url) {
        String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            return false;
        }
        String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
        return !host.isEmpty() && allowedHostPorts.contains(host + ":" + effectivePort(url));
    }

    private static int effectivePort(URL url) {
        return url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
    }

    /**
     * 打开连接，手动处理重定向：每一跳都重新校验协议、域名和端口，
     * 避免白名单域名 302 到内网地址造成 SSRF。
     */
    private HttpURLConnection openFollowingRedirects(URL url) throws IOException {
        URL current = url;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            if (!isAllowedHost(current)) {
                throw new IOException("unsafe redirect target: " + current);
            }
            HttpURLConnection conn = (HttpURLConnection) current.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "audio/*");
            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (StringUtils.isEmpty(location)) {
                    throw new IOException("redirect without location: " + current);
                }
                current = new URL(current, location);
                continue;
            }
            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                throw new IOException("unexpected http status: " + code);
            }
            return conn;
        }
        throw new IOException("too many redirects: " + url);
    }

    /**
     * 下载到临时文件；边读边计数，不依赖 Content-Length，最后校验 AMR 文件头。
     * 不校验 Content-Type：各家对象存储返回的类型不统一（如 binary/octet-stream），以文件头为准。
     */
    private File downloadToTemp(URL url) throws IOException {
        HttpURLConnection conn = openFollowingRedirects(url);
        File tmp = File.createTempFile("amr2mp3-", ".amr", cacheDir);
        boolean success = false;
        try {
            long total = 0;
            byte[] buffer = new byte[8192];
            try (InputStream in = conn.getInputStream();
                 OutputStream out = new FileOutputStream(tmp)) {
                int n;
                while ((n = in.read(buffer)) > 0) {
                    total += n;
                    if (total > MAX_AMR_FILE_SIZE) {
                        throw new IOException("amr file too large");
                    }
                    out.write(buffer, 0, n);
                }
            }
            if (!isAmrFile(tmp)) {
                throw new IOException("not an amr file");
            }
            success = true;
            return tmp;
        } finally {
            conn.disconnect();
            if (!success) {
                tmp.delete();
            }
        }
    }

    private static boolean isAmrFile(File file) {
        byte[] head = new byte[9];
        try (InputStream in = new FileInputStream(file)) {
            int n = in.read(head);
            if (n < 6) {
                return false;
            }
            String magic = new String(head, 0, n, StandardCharsets.US_ASCII);
            return magic.startsWith("#!AMR\n") || magic.startsWith("#!AMR-WB\n");
        } catch (IOException e) {
            return false;
        }
    }

    static String cacheKey(URL url) {
        String normalized = url.getProtocol().toLowerCase(Locale.ROOT) + "://"
                + url.getHost().toLowerCase(Locale.ROOT) + ":" + effectivePort(url) + url.getPath();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
        char[] hex = "0123456789abcdef".toCharArray();
        char[] out = new char[hash.length * 2];
        for (int i = 0; i < hash.length; i++) {
            int b = hash[i] & 0xff;
            out[i * 2] = hex[b >>> 4];
            out[i * 2 + 1] = hex[b & 0x0f];
        }
        return new String(out);
    }

    /** 已登录按用户限频；未登录按来源 IP。 */
    private static String rateLimitKey(HttpServletRequest request) {
        Session session = SecurityUtils.getSubject().getSession(false);
        Object userId = session == null ? null : session.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId;
        }
        return "ip:" + getIp(request);
    }

    /**
     * 只有请求来自本机或内网（即经过反向代理）时才信任 X-Real-IP / X-Forwarded-For，
     * 否则直接用对端地址，避免客户端伪造请求头绕过限频。
     */
    static String getIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String ip = request.getHeader("X-Real-IP");
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (!StringUtils.isEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            return index > 0 ? ip.substring(0, index).trim() : ip.trim();
        }
        return remoteAddr;
    }

    private static boolean isTrustedProxy(String remoteAddr) {
        if (StringUtils.isEmpty(remoteAddr)) {
            return false;
        }
        try {
            // remoteAddr 是 IP 字面量，这里不会触发 DNS 解析
            InetAddress address = InetAddress.getByName(remoteAddr);
            return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static ResponseEntity<InputStreamResource> mp3Response(File file, MediaType mediaType) throws IOException {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.length())
                .body(new InputStreamResource(new FileInputStream(file)));
    }

    private static void amr2mp3(File source, File target) throws EncoderException {
        //Audio Attributes
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libmp3lame");
        audio.setBitRate(128000);
        audio.setChannels(2);
        audio.setSamplingRate(44100);

        //Encoding attributes
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setFormat("mp3");
        attrs.setAudioAttributes(audio);

        //Encode
        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(source), target, attrs);
    }
}
