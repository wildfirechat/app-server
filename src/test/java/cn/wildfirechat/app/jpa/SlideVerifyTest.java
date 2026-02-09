package cn.wildfirechat.app.jpa;

import org.junit.Test;

import static org.junit.Assert.*;

public class SlideVerifyTest {

    @Test
    public void testConstructor() {
        // Given
        String token = "test-token";
        int x = 150;
        long timestamp = System.currentTimeMillis();

        // When
        SlideVerify slideVerify = new SlideVerify(token, x, timestamp);

        // Then
        assertEquals(token, slideVerify.getToken());
        assertEquals(x, slideVerify.getX());
        assertEquals(timestamp, slideVerify.getTimestamp());
        assertFalse(slideVerify.isVerified());
    }

    @Test
    public void testSettersAndGetters() {
        // Given
        SlideVerify slideVerify = new SlideVerify();
        String token = "new-token";
        int x = 200;
        long timestamp = 1234567890L;
        boolean verified = true;

        // When
        slideVerify.setToken(token);
        slideVerify.setX(x);
        slideVerify.setTimestamp(timestamp);
        slideVerify.setVerified(verified);

        // Then
        assertEquals(token, slideVerify.getToken());
        assertEquals(x, slideVerify.getX());
        assertEquals(timestamp, slideVerify.getTimestamp());
        assertTrue(slideVerify.isVerified());
    }

    @Test
    public void testIsExpiredNotExpired() {
        // Given
        SlideVerify slideVerify = new SlideVerify("token", 100, System.currentTimeMillis());
        int timeoutSeconds = 300;

        // When
        boolean expired = slideVerify.isExpired(timeoutSeconds);

        // Then
        assertFalse(expired);
    }

    @Test
    public void testIsExpiredJustExpired() {
        // Given
        SlideVerify slideVerify = new SlideVerify("token", 100, System.currentTimeMillis() - 300001); // 300秒零1毫秒前
        int timeoutSeconds = 300;

        // When
        boolean expired = slideVerify.isExpired(timeoutSeconds);

        // Then
        assertTrue(expired);
    }

    @Test
    public void testIsExpiredLongAgo() {
        // Given
        SlideVerify slideVerify = new SlideVerify("token", 100, System.currentTimeMillis() - 600000); // 10分钟前
        int timeoutSeconds = 300;

        // When
        boolean expired = slideVerify.isExpired(timeoutSeconds);

        // Then
        assertTrue(expired);
    }

    @Test
    public void testDefaultConstructor() {
        // When
        SlideVerify slideVerify = new SlideVerify();

        // Then
        assertNull(slideVerify.getToken());
        assertEquals(0, slideVerify.getX());
        assertEquals(0L, slideVerify.getTimestamp());
        assertFalse(slideVerify.isVerified());
    }

    @Test
    public void testVerifiedStateToggle() {
        // Given
        SlideVerify slideVerify = new SlideVerify("token", 100, System.currentTimeMillis());

        // Initially not verified
        assertFalse(slideVerify.isVerified());

        // When - set to verified
        slideVerify.setVerified(true);

        // Then
        assertTrue(slideVerify.isVerified());

        // When - toggle back
        slideVerify.setVerified(false);

        // Then
        assertFalse(slideVerify.isVerified());
    }

    @Test
    public void testBoundaryConditions() {
        // Test with x at boundary values
        SlideVerify slideVerify = new SlideVerify("token", 0, System.currentTimeMillis());
        assertEquals(0, slideVerify.getX());

        slideVerify.setX(1000);
        assertEquals(1000, slideVerify.getX());
    }

    @Test
    public void testTimestampBoundary() {
        // Test with timestamp at boundary
        long currentTime = System.currentTimeMillis();
        SlideVerify slideVerify = new SlideVerify("token", 100, currentTime);

        // Not expired at exactly timeout
        assertFalse(slideVerify.isExpired(0)); // Immediate timeout

        // Expired with negative timeout should always be true
        // (edge case, but testing robustness)
        assertTrue(slideVerify.isExpired(-1));
    }
}
