package cn.wildfirechat.app;

import cn.wildfirechat.app.tools.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ws.schild.jave.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

@RestController
public class AudioController {
    private static final Logger LOG = LoggerFactory.getLogger(AudioController.class);

    private static final long MAX_AMR_FILE_SIZE = 50 * 1024 * 1024L;

    /** 外链最多跟随的重定向跳数，每一跳都会重新校验域名。 */
    private static final int MAX_REDIRECTS = 3;

    /** 同时转码的任务数，避免匿名请求把 CPU 打满。 */
    private static final int MAX_CONCURRENT_TRANSCODES = 4;

    @Value("${wfc.audio.cache.dir:/tmp/wfc_audio_cache}")
    String cacheDirPath;

    @Value("${media.bucket_general_domain:}")
    private String ossGeneralBucketDomain;

    @Value("${media.bucket_voice_domain:}")
    private String ossVoiceBucketDomain;

    @Value("${media.bucket_file_domain:}")
    private String ossFileBucketDomain;

    private File cacheDir;
    private List<String> allowedHosts;

    /** 按来源 IP 限频：60 秒内最多 60 次。 */
    private final RateLimiter rateLimiter = new RateLimiter(60, 60);

    private final Semaphore transcodeSlots = new Semaphore(MAX_CONCURRENT_TRANSCODES);

    @PostConstruct
    public void init() {
        cacheDir = new File(cacheDirPath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        allowedHosts = new ArrayList<>();
        addHost(ossGeneralBucketDomain);
        addHost(ossVoiceBucketDomain);
        addHost(ossFileBucketDomain);
        if (allowedHosts.isEmpty()) {
            LOG.warn("amr2mp3 allowlist is empty, all requests will be rejected");
        } else {
            LOG.info("amr2mp3 allowed hosts: {}", allowedHosts);
        }
    }

    private void addHost(String domain) {
        String host = getHost(domain);
        if (!StringUtils.isEmpty(host) && !allowedHosts.contains(host)) {
            allowedHosts.add(host);
        }
    }

    @GetMapping("amr2mp3")
    public CompletableFuture<ResponseEntity<InputStreamResource>> amr2mp3(@RequestParam("path") String amrUrl,
                                                                         HttpServletRequest request) {
        // 1. 基础校验：协议、域名白名单、.amr 后缀
        URL url = validateAmrUrl(amrUrl);
        if (url == null) {
            LOG.warn("amr2mp3 rejected - invalid url: {}", amrUrl);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }

        // 2. 按 IP 限频，避免匿名请求刷转码
        String ip = getIp(request);
        if (!rateLimiter.isGranted(ip)) {
            LOG.warn("amr2mp3 rejected - too frequent, ip: {}", ip);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
        }

        // 3. 缓存键用完整 URL 摘要，避免不同地址同名文件互相覆盖
        File mp3File = new File(cacheDir, cacheKey(amrUrl) + ".mp3");
        MediaType mediaType = new MediaType("audio", "mp3");
        if (mp3File.exists() && mp3File.length() > 0) {
            try {
                return CompletableFuture.completedFuture(mp3Response(mp3File, mediaType));
            } catch (IOException e) {
                LOG.error("read cached mp3 failed: {}", mp3File.getAbsolutePath(), e);
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            if (!transcodeSlots.tryAcquire()) {
                LOG.warn("amr2mp3 rejected - too many concurrent transcodes");
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
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
                transcodeSlots.release();
            }
        });
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
        return !host.isEmpty() && allowedHosts.contains(host);
    }

    /**
     * 打开连接，手动处理重定向：每一跳都重新校验协议和域名，
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

    /** 下载到临时文件；边读边计数，不依赖 Content-Length，最后校验 AMR 文件头。 */
    private File downloadToTemp(URL url) throws IOException {
        HttpURLConnection conn = openFollowingRedirects(url);
        File tmp = File.createTempFile("amr2mp3-", ".amr", cacheDir);
        boolean success = false;
        try {
            String contentType = conn.getContentType();
            if (contentType != null) {
                String ct = contentType.toLowerCase(Locale.ROOT);
                if (!ct.startsWith("audio/") && !ct.startsWith("application/octet-stream")) {
                    throw new IOException("unexpected content type: " + contentType);
                }
            }
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

    private static String cacheKey(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            char[] hex = "0123456789abcdef".toCharArray();
            char[] out = new char[hash.length * 2];
            for (int i = 0; i < hash.length; i++) {
                int b = hash[i] & 0xff;
                out[i * 2] = hex[b >>> 4];
                out[i * 2 + 1] = hex[b & 0x0f];
            }
            return new String(out);
        } catch (Exception e) {
            return Integer.toHexString(url.hashCode());
        }
    }

    private static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (!StringUtils.isEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            return index > 0 ? ip.substring(0, index).trim() : ip.trim();
        }
        return request.getRemoteAddr();
    }

    private String getHost(String domain) {
        if (StringUtils.isEmpty(domain)) {
            return "";
        }
        try {
            return new URL(domain).getHost().toLowerCase(Locale.ROOT);
        } catch (MalformedURLException e) {
            return domain.toLowerCase(Locale.ROOT);
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
