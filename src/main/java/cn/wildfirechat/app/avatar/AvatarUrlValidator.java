package cn.wildfirechat.app.avatar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 外部图片地址校验。
 *
 * <p>服务端在拉取群成员头像等外部图片前，先确认地址可用：仅接受 http/https、
 * 标准 80/443 端口，且主机不能是本机、内网或保留地址；域名解析失败时不加载。</p>
 *
 * <p>默认不限制图片域名。如需收敛，可配置
 * {@code avatar.url.allowlist=cdn.example.com,img.example.com}（逗号分隔，留空即关闭）。</p>
 */
@Component
public class AvatarUrlValidator {
    private static final Logger LOG = LoggerFactory.getLogger(AvatarUrlValidator.class);

    /** 可选域名白名单，逗号分隔；为空表示不限制域名。 */
    @Value("${avatar.url.allowlist:}")
    private String allowlistConfig;

    private final Set<String> allowedHosts = Collections.synchronizedSet(new HashSet<>());

    /** Spring 使用的无参构造。 */
    public AvatarUrlValidator() {
    }

    /** 便于单元测试直接注入白名单。 */
    AvatarUrlValidator(String... domains) {
        if (domains != null) {
            for (String d : domains) {
                addHost(d);
            }
        }
    }

    @PostConstruct
    void init() {
        if (allowlistConfig != null) {
            for (String d : allowlistConfig.split(",")) {
                addHost(d);
            }
        }
        if (allowedHosts.isEmpty()) {
            LOG.info("avatar url allowlist disabled, only public http(s) urls are allowed");
        } else {
            LOG.info("avatar url allowlist enabled, hosts: {}", allowedHosts);
        }
    }

    private void addHost(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }
        String host;
        try {
            host = new URL(domain.trim()).getHost();
        } catch (MalformedURLException e) {
            host = domain.trim();
        }
        if (host != null) {
            host = host.toLowerCase(Locale.ROOT);
            if (!host.isEmpty()) {
                allowedHosts.add(host);
            }
        }
    }

    /**
     * 校验外部图片地址是否可以加载。
     *
     * @param urlStr 请求体中的 avatarUrl
     * @return true 表示可以加载
     */
    public boolean isAllowed(String urlStr) {
        if (urlStr == null || urlStr.trim().isEmpty()) {
            return false;
        }
        URL url;
        try {
            url = new URL(urlStr.trim());
        } catch (MalformedURLException e) {
            return false;
        }
        if (!isSafePublicUrl(url)) {
            return false;
        }
        // 可选白名单：未配置时不限制域名
        if (!allowedHosts.isEmpty()) {
            String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
            if (!allowedHosts.contains(host)) {
                LOG.warn("avatar url rejected, host not in allowlist: {}", host);
                return false;
            }
        }
        return true;
    }

    /**
     * 静态校验（供 {@link GroupAvatarUtil} 在每次重定向跳转时复用，不依赖 Spring 注入）。
     */
    public static boolean isSafePublicUrl(URL url) {
        if (url == null) {
            return false;
        }
        String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            LOG.warn("avatar url rejected, unsupported protocol: {}", protocol);
            return false;
        }
        if (url.getUserInfo() != null) {
            LOG.warn("avatar url rejected, userinfo not allowed");
            return false;
        }
        int port = url.getPort();
        int defaultPort = "https".equals(protocol) ? 443 : 80;
        if (port != -1 && port != 80 && port != 443 && port != defaultPort) {
            LOG.warn("avatar url rejected, unsupported port: {}", port);
            return false;
        }
        String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
        if (host.isEmpty() || isInternalHostName(host)) {
            LOG.warn("avatar url rejected, internal host name: {}", host);
            return false;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isInternalAddress(address)) {
                    LOG.warn("avatar url rejected, internal address: {} -> {}", host, address.getHostAddress());
                    return false;
                }
            }
        } catch (UnknownHostException e) {
            LOG.warn("avatar url rejected, cannot resolve host: {}", host);
            return false;
        }
        return true;
    }

    private static boolean isInternalHostName(String host) {
        if ("localhost".equals(host)) {
            return true;
        }
        String[] suffixes = {".localhost", ".local", ".internal", ".lan", ".home", ".corp", ".intranet"};
        for (String suffix : suffixes) {
            if (host.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInternalAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xff;
            int b1 = bytes[1] & 0xff;
            // 100.64.0.0/10
            if (b0 == 100 && b1 >= 64 && b1 <= 127) {
                return true;
            }
            // 0.0.0.0/8
            if (b0 == 0) {
                return true;
            }
        } else if (bytes.length == 16) {
            // fc00::/7
            if ((bytes[0] & 0xfe) == 0xfc) {
                return true;
            }
        }
        return false;
    }
}
