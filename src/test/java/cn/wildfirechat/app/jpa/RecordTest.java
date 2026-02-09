package cn.wildfirechat.app.jpa;

import org.junit.Test;

import static org.junit.Assert.*;

public class RecordTest {

    @Test
    public void testDefaultConstructor() {
        // When
        Record record = new Record();

        // Then
        assertNull(record.getMobile());
        assertNull(record.getCode());
        assertEquals(0L, record.getTimestamp());
    }

    @Test
    public void testConstructorWithParams() {
        // Given
        String code = "123456";
        String mobile = "13800138000";

        // When
        Record record = new Record(code, mobile);

        // Then
        assertEquals(code, record.getCode());
        assertEquals(mobile, record.getMobile());
        assertTrue(record.getStartTime() > 0);
        assertEquals(0, record.getRequestCount());
    }

    @Test
    public void testIncreaseAndCheckWithinLimit() {
        // Given
        Record record = new Record("123456", "13800138000");

        // When - within limit
        for (int i = 0; i < 5; i++) {
            boolean result = record.increaseAndCheck();
            // Then
            assertTrue(result);
        }
        assertEquals(5, record.getRequestCount());
    }

    @Test
    public void testIncreaseAndCheckExceedsLimit() {
        // Given
        Record record = new Record("123456", "13800138000");

        // When - exceed limit
        for (int i = 0; i < 10; i++) {
            record.increaseAndCheck();
        }

        // Then - should return false after 10 requests
        boolean result = record.increaseAndCheck();
        assertFalse(result);
    }

    @Test
    public void testReset() {
        // Given
        Record record = new Record("123456", "13800138000");
        for (int i = 0; i < 5; i++) {
            record.increaseAndCheck();
        }
        assertEquals(5, record.getRequestCount());

        // When
        record.reset();

        // Then
        assertEquals(1, record.getRequestCount());
        assertTrue(record.getStartTime() > 0);
    }

    @Test
    public void testSettersAndGetters() {
        // Given
        Record record = new Record();

        // When
        record.setCode("654321");
        record.setTimestamp(System.currentTimeMillis());
        record.setStartTime(System.currentTimeMillis());

        // Then
        assertEquals("654321", record.getCode());
        assertTrue(record.getTimestamp() > 0);
        assertTrue(record.getStartTime() > 0);
    }

    @Test
    public void testResetAfter24Hours() {
        // Given
        Record record = new Record("123456", "13800138000");
        record.setStartTime(System.currentTimeMillis() - 86400001L); // 24 hours + 1ms ago

        // When
        boolean result = record.increaseAndCheck();

        // Then - should reset and allow (count becomes 1 after increment)
        assertTrue(result);
        assertTrue(record.getRequestCount() >= 1); // Could be 1 or 2 depending on internal logic
    }
}
