package cn.wildfirechat.app.tools;

import org.junit.Test;

import static org.junit.Assert.*;

public class FixedWindowRateLimiterTest {

    @Test
    public void testAllowWithinLimit() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1000, 3);
        String key = "127.0.0.1";
        assertTrue(limiter.isGranted(key));
        assertTrue(limiter.isGranted(key));
        assertTrue(limiter.isGranted(key));
    }

    @Test
    public void testRejectWhenExceedLimit() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1000, 3);
        String key = "127.0.0.1";
        assertTrue(limiter.isGranted(key));
        assertTrue(limiter.isGranted(key));
        assertTrue(limiter.isGranted(key));
        assertFalse(limiter.isGranted(key));
        assertFalse(limiter.isGranted(key));
    }

    @Test
    public void testDifferentKeysAreIndependent() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1000, 1);
        assertTrue(limiter.isGranted("127.0.0.1"));
        assertFalse(limiter.isGranted("127.0.0.1"));
        assertTrue(limiter.isGranted("192.168.1.1"));
    }

    @Test
    public void testWindowResetsAfterExpire() throws InterruptedException {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(100, 1);
        String key = "127.0.0.1";
        assertTrue(limiter.isGranted(key));
        assertFalse(limiter.isGranted(key));
        Thread.sleep(150);
        assertTrue(limiter.isGranted(key));
    }

    @Test
    public void testEmptyKeyRejected() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1000, 3);
        assertFalse(limiter.isGranted(""));
        assertFalse(limiter.isGranted(null));
    }
}
