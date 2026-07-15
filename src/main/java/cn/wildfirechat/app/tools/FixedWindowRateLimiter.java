package cn.wildfirechat.app.tools;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 固定窗口限流器。
 * 每个 key 在指定时间窗口内最多允许 maxRequests 次请求。
 */
public class FixedWindowRateLimiter {
    private static final Logger LOG = LoggerFactory.getLogger(FixedWindowRateLimiter.class);

    private final long windowSizeMillis;
    private final int maxRequests;
    private final Cache<String, AtomicInteger> cache;

    public FixedWindowRateLimiter(long windowSizeMillis, int maxRequests) {
        if (windowSizeMillis <= 0 || maxRequests <= 0) {
            throw new IllegalArgumentException("windowSizeMillis and maxRequests must be positive");
        }
        this.windowSizeMillis = windowSizeMillis;
        this.maxRequests = maxRequests;
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(windowSizeMillis, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 判断指定 key 在当前窗口内是否还可以继续请求。
     *
     * @param key 限流维度，例如客户端 IP
     * @return true 表示允许，false 表示已触发限流
     */
    public boolean isGranted(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        try {
            AtomicInteger counter = cache.get(key, () -> new AtomicInteger(0));
            int count = counter.incrementAndGet();
            if (count > maxRequests) {
                LOG.warn("FixedWindowRateLimiter rejected key {}, count {} exceeds limit {}", key, count, maxRequests);
                return false;
            }
            return true;
        } catch (ExecutionException e) {
            LOG.error("FixedWindowRateLimiter error", e);
            return false;
        }
    }

    public long getWindowSizeMillis() {
        return windowSizeMillis;
    }

    public int getMaxRequests() {
        return maxRequests;
    }
}
