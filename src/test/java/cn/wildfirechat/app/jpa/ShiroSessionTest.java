package cn.wildfirechat.app.jpa;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ShiroSessionTest {

    @Test
    public void testDefaultConstructor() {
        // When
        ShiroSession session = new ShiroSession();

        // Then
        assertNull(session.getSessionId());
        assertNull(session.getSessionData());
    }

    @Test
    public void testConstructorWithParams() {
        // Given
        String sessionId = "session-123";
        byte[] sessionData = "test session data".getBytes();

        // When
        ShiroSession session = new ShiroSession(sessionId, sessionData);

        // Then
        assertEquals(sessionId, session.getSessionId());
        assertArrayEquals(sessionData, session.getSessionData());
    }

    @Test
    public void testSetSessionData() {
        // Given
        ShiroSession session = new ShiroSession();
        byte[] newData = "new session data".getBytes();

        // When
        session.setSessionId("new-session-id");
        session.setSessionData(newData);

        // Then
        assertEquals("new-session-id", session.getSessionId());
        assertArrayEquals(newData, session.getSessionData());
    }

    @Test
    public void testEmptySessionData() {
        // Given
        ShiroSession session = new ShiroSession("empty-session", new byte[0]);

        // Then
        assertNotNull(session.getSessionData());
        assertEquals(0, session.getSessionData().length);
    }

    @Test
    public void testBinarySessionData() {
        // Given
        byte[] binaryData = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};
        ShiroSession session = new ShiroSession("binary-session", binaryData);

        // When
        byte[] retrievedData = session.getSessionData();

        // Then
        assertArrayEquals(binaryData, retrievedData);
        assertEquals(5, retrievedData.length);
    }

    @Test
    public void testLargeSessionData() {
        // Given
        byte[] largeData = new byte[2048];
        Arrays.fill(largeData, (byte) 'A');

        // When
        ShiroSession session = new ShiroSession("large-session", largeData);

        // Then
        assertArrayEquals(largeData, session.getSessionData());
        assertEquals(2048, session.getSessionData().length);
    }
}
