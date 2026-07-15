package cn.wildfirechat.app;

import cn.wildfirechat.app.tools.Utils;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@RestController
public class AudioController {
    private static final Logger LOG = LoggerFactory.getLogger(AudioController.class);

    private static final long MAX_AMR_FILE_SIZE = 50 * 1024 * 1024L;

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

    @PostConstruct
    public void init() {
        cacheDir = new File(cacheDirPath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        allowedHosts = new ArrayList<>();
        allowedHosts.add(getHost(ossGeneralBucketDomain));
        allowedHosts.add(getHost(ossVoiceBucketDomain));
        allowedHosts.add(getHost(ossFileBucketDomain));
        LOG.info("amr2mp3 allowed hosts: {}", allowedHosts);
    }

    @GetMapping("amr2mp3")
    public CompletableFuture<ResponseEntity<InputStreamResource>> amr2mp3(@RequestParam("path") String amrUrl) throws FileNotFoundException {
        // 1. URL 基础校验：协议、域名白名单、后缀
        URL url = validateAmrUrl(amrUrl);
        if (url == null) {
            LOG.warn("amr2mp3 rejected - invalid url: {}", amrUrl);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }

        // 2. 通过 HEAD 预检大小（失败不阻断，由 JAVE 自己处理）
        if (!checkAmrSize(url)) {
            LOG.warn("amr2mp3 rejected - file too large: {}", amrUrl);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }

        // 3. 缓存文件名安全化
        String baseName = amrUrl.substring(amrUrl.lastIndexOf('/') + 1);
        String safeBaseName = Utils.getSafeFileName(baseName);
        if (!safeBaseName.toLowerCase().endsWith(".amr")) {
            LOG.warn("amr2mp3 rejected - unsafe basename: {}", baseName);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }
        String nameWithoutExt = safeBaseName.substring(0, safeBaseName.length() - 4);
        if (StringUtils.isEmpty(nameWithoutExt)) {
            nameWithoutExt = "audio";
        }
        String mp3FileName = nameWithoutExt + ".mp3";
        File mp3File = new File(cacheDir, mp3FileName);

        MediaType mediaType = new MediaType("audio", "mp3");
        if (mp3File.exists()) {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(mp3File));
            return CompletableFuture.completedFuture(ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + mp3File.getName())
                    .contentType(mediaType)
                    .contentLength(mp3File.length())
                    .body(resource));
        }

        return CompletableFuture.supplyAsync(new Supplier<ResponseEntity<InputStreamResource>>() {
            /**
             * Gets a result.
             *
             * @return a result
             */
            @Override
            public ResponseEntity<InputStreamResource> get() {

                try {
                    amr2mp3(url, mp3File);
                    InputStreamResource resource = new InputStreamResource(new FileInputStream(mp3File));
                    return ResponseEntity.ok()
//                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + mp3File.getName())
                            .contentType(mediaType)
                            .contentLength(mp3File.length())
                            .body(resource);
                } catch (MalformedURLException e) {
                    LOG.error("MalformedURLException for amrUrl: {}", amrUrl, e);
                } catch (EncoderException e) {
                    LOG.error("EncoderException", e);
                } catch (FileNotFoundException e) {
                    LOG.error("FileNotFoundException", e);
                }
                return ResponseEntity.status(500).build();
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
        String protocol = url.getProtocol().toLowerCase();
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            return null;
        }
        String host = url.getHost().toLowerCase();
        if (!allowedHosts.contains(host)) {
            return null;
        }
        String path = url.getPath().toLowerCase();
        if (!path.endsWith(".amr")) {
            return null;
        }
        return url;
    }

    private boolean checkAmrSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                long contentLength = conn.getContentLengthLong();
                if (contentLength > 0 && contentLength > MAX_AMR_FILE_SIZE) {
                    LOG.warn("amr2mp3 rejected - content length {} exceeds limit {}", contentLength, MAX_AMR_FILE_SIZE);
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            LOG.warn("amr2mp3 failed to check size for {}: {}", url, e.getMessage());
            // HEAD 失败不阻断，交给 JAVE 处理
            return true;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String getHost(String domain) {
        if (StringUtils.isEmpty(domain)) {
            return "";
        }
        try {
            return new URL(domain).getHost().toLowerCase();
        } catch (MalformedURLException e) {
            return domain.toLowerCase();
        }
    }

    private static void amr2mp3(URL sourceUrl, File target) throws MalformedURLException, EncoderException {
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
        encoder.encode(new MultimediaObject(sourceUrl), target, attrs);

    }

}
