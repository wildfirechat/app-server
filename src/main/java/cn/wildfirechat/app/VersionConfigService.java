package cn.wildfirechat.app;

import cn.wildfirechat.app.pojo.VersionCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VersionConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(VersionConfigService.class);

    @Value("${version.config.path:config/version.properties}")
    private String configPath;

    private final ConcurrentHashMap<String, VersionCheckResponse> versionMap = new ConcurrentHashMap<>();
    private long lastModified = 0;

    @PostConstruct
    public void init() {
        loadConfig();
    }

    @Scheduled(fixedRate = 30 * 1000)
    public void reloadIfModified() {
        File file = new File(configPath);
        if (!file.exists()) {
            LOG.warn("Version config file not found: {}", configPath);
            return;
        }
        if (file.lastModified() > lastModified) {
            LOG.info("Version config file modified, reloading...");
            loadConfig();
        }
    }

    private synchronized void loadConfig() {
        File file = new File(configPath);
        if (!file.exists()) {
            LOG.warn("Version config file not found: {}", configPath);
            return;
        }

        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            props.load(reader);
            lastModified = file.lastModified();
        } catch (IOException e) {
            LOG.error("Failed to load version config", e);
            return;
        }

        // Android
        versionMap.put("android", buildVersionInfo(props, "android"));
        // iOS
        versionMap.put("ios", buildVersionInfo(props, "ios"));
        // Harmony
        versionMap.put("harmony", buildVersionInfo(props, "harmony"));

        LOG.info("Version config loaded successfully");
    }

    private VersionCheckResponse buildVersionInfo(Properties props, String platform) {
        VersionCheckResponse info = new VersionCheckResponse();
        info.setLatestVersion(getProp(props, "version." + platform + ".latestVersion", ""));
        info.setBuildNumber(parseInt(getProp(props, "version." + platform + ".buildNumber", "0")));
        info.setMinVersion(getProp(props, "version." + platform + ".minVersion", ""));
        info.setForceUpdate(Boolean.parseBoolean(getProp(props, "version." + platform + ".forceUpdate", "false")));
        info.setTitle(getProp(props, "version." + platform + ".title", ""));
        info.setMessage(getProp(props, "version." + platform + ".message", ""));
        info.setDownloadUrl(getProp(props, "version." + platform + ".downloadUrl", ""));
        info.setRedirectUrl(getProp(props, "version." + platform + ".redirectUrl", ""));
        info.setHarmonyUrl(getProp(props, "version." + platform + ".harmonyUrl", ""));
        return info;
    }

    private String getProp(Properties props, String key, String defaultValue) {
        String value = props.getProperty(key);
        return value != null ? value : defaultValue;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public VersionCheckResponse getVersionInfo(String platform) {
        return versionMap.get(platform);
    }
}
