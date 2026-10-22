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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 外部图片地址校验。
 *
 * <p>服务端在拉取群成员头像等外部图片前，先确认地址可用：仅接受 http/https，端口不限，
 * 允许内网地址（私有化部署的头像通常在内网对象存储上）；拒绝本机回环、链路本地
 * （含云厂商元数据地址）和保留地址。这些地址不会是客户端能访问的正常头像地址。
 * 域名解析失败时不加载。</p>
 *
 * <p>默认不限制图片域名。如需收紧，可配置
 * {@code avatar.url.allowlist=cdn.example.com,http://192.168.1.20:9000}（逗号分隔，留空即关闭）。
 * 配置后只放行名单内的主机，名单内的主机不再做地址检查。条目不写端口时放行 80 和 443。</p>
 */
@Component
public class AvatarUrlValidator {
    private static final Logger LOG = LoggerFactory.getLogger(AvatarUrlValidator.class);

    /** 可选域名白名单，逗号分隔；为空表示不限制域名。 */
    @Value("${avatar.url.allowlist:}")
    private String allowlistConfig;

    /** 白名单 "主机:端口"，只在初始化时写入。 */
    private final Set<String> allowedHostPorts = new HashSet<>();

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
        if (allowedHostPorts.isEmpty()) {
            LOG.info("avatar url allowlist disabled, only public http(s) urls are allowed");
        } else {
            LOG.info("avatar url allowlist enabled, hosts: {}", allowedHostPorts);
        }
    }

    private void addHost(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }
        String value = domain.trim();
        URL url;
        try {
            url = new URL(value.contains("://") ? value : "http://" + value);
        } catch (MalformedURLException e) {
            LOG.warn("avatar url allowlist ignore invalid entry: {}", value);
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
        try {
            return isAllowedUrl(new URL(urlStr.trim()));
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * 校验外部图片地址是否可以加载，{@link GroupAvatarUtil} 在每次重定向跳转时也会调用。
     * 会做 DNS 解析，不要在请求线程上调用。
     */
    public boolean isAllowedUrl(URL url) {
        if (!isHttpUrl(url)) {
            return false;
        }
        if (!allowedHostPorts.isEmpty()) {
            String hostPort = url.getHost().toLowerCase(Locale.ROOT) + ":" + effectivePort(url);
            if (!allowedHostPorts.contains(hostPort)) {
                LOG.warn("avatar url rejected, host not in allowlist: {}", hostPort);
                return false;
            }
            // 运维显式配置的主机，不再做地址检查
            return true;
        }
        return isSafeUrl(url);
    }

    /** 只检查格式：http/https、有主机名、不带用户信息。不做 DNS 解析。 */
    public static boolean isHttpUrl(URL url) {
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
        return url.getHost() != null && !url.getHost().isEmpty();
    }

    /** 默认地址校验：主机解析出的所有地址都不能是本机回环、链路本地或保留地址。 */
    public static boolean isSafeUrl(URL url) {
        if (!isHttpUrl(url)) {
            return false;
        }
        String host = url.getHost().toLowerCase(Locale.ROOT);
        if (isLocalHostName(host)) {
            LOG.warn("avatar url rejected, local host name: {}", host);
            return false;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isBlockedAddress(address)) {
                    LOG.warn("avatar url rejected, blocked address: {} -> {}", host, address.getHostAddress());
                    return false;
                }
            }
        } catch (UnknownHostException e) {
            LOG.warn("avatar url rejected, cannot resolve host: {}", host);
            return false;
        }
        return true;
    }

    private static int effectivePort(URL url) {
        return url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
    }

    /** 内网域名（如 .lan、.corp）解析后按地址判断，这里只拦截指向本机的名字。 */
    private static boolean isLocalHostName(String host) {
        return "localhost".equals(host) || host.endsWith(".localhost");
    }

    /**
     * 不允许访问的地址。内网地址（10/8、172.16/12、192.168/16）不在其中：私有化部署的头像通常在那里。
     */
    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                // 169.254.0.0/16、fe80::/10，含 169.254.169.254 等云厂商元数据地址
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xff;
            int b1 = bytes[1] & 0xff;
            // 100.64.0.0/10，阿里云元数据地址 100.100.100.200 在这个段内
            if (b0 == 100 && b1 >= 64 && b1 <= 127) {
                return true;
            }
            // 0.0.0.0/8
            if (b0 == 0) {
                return true;
            }
            // 198.18.0.0/15
            if (b0 == 198 && (b1 == 18 || b1 == 19)) {
                return true;
            }
            // 240.0.0.0/4（含 255.255.255.255）
            if (b0 >= 240) {
                return true;
            }
        } else if (bytes.length == 16) {
            // fc00::/7，AWS IPv6 元数据地址 fd00:ec2::254 在这个段内；用 IPv6 ULA 存头像的情况很少
            if ((bytes[0] & 0xfe) == 0xfc) {
                return true;
            }
            // 2002::/16（6to4，内嵌 IPv4 地址）
            if ((bytes[0] & 0xff) == 0x20 && (bytes[1] & 0xff) == 0x02) {
                return true;
            }
            // 64:ff9b::/96（NAT64，内嵌 IPv4 地址）
            if ((bytes[0] & 0xff) == 0x00 && (bytes[1] & 0xff) == 0x64
                    && (bytes[2] & 0xff) == 0xff && (bytes[3] & 0xff) == 0x9b) {
                boolean zeros = true;
                for (int i = 4; i < 12; i++) {
                    if (bytes[i] != 0) {
                        zeros = false;
                        break;
                    }
                }
                if (zeros) {
                    return true;
                }
            }
        }
        return false;
    }
}
