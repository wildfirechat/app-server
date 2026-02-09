package cn.wildfirechat.app.jpa;

import org.junit.Test;

import static org.junit.Assert.*;

public class PCSessionTest {

    @Test
    public void testDefaultConstructor() {
        // When
        PCSession session = new PCSession();

        // Then
        assertNull(session.getToken());
        assertNull(session.getClientId());
        assertEquals(0, session.getStatus());
        assertNull(session.getConfirmedUserId());
        assertEquals(0, session.getPlatform());
    }

    @Test
    public void testSettersAndGetters() {
        // Given
        PCSession session = new PCSession();
        long currentTime = System.currentTimeMillis();

        // When
        session.setToken("test-token-123");
        session.setClientId("client-456");
        session.setCreateDt(currentTime);
        session.setDuration(300000);
        session.setStatus(PCSession.PCSessionStatus.Session_Scanned);
        session.setConfirmedUserId("user-789");
        session.setDevice_name("Windows PC");
        session.setPlatform(3);

        // Then
        assertEquals("test-token-123", session.getToken());
        assertEquals("client-456", session.getClientId());
        assertEquals(currentTime, session.getCreateDt());
        assertEquals(300000, session.getDuration());
        assertEquals(PCSession.PCSessionStatus.Session_Scanned, session.getStatus());
        assertEquals("user-789", session.getConfirmedUserId());
        assertEquals("Windows PC", session.getDevice_name());
        assertEquals(3, session.getPlatform());
    }

    @Test
    public void testSessionStatusConstants() {
        assertEquals(0, PCSession.PCSessionStatus.Session_Created);
        assertEquals(1, PCSession.PCSessionStatus.Session_Scanned);
        assertEquals(2, PCSession.PCSessionStatus.Session_Verified);
        assertEquals(3, PCSession.PCSessionStatus.Session_Pre_Verify);
        assertEquals(4, PCSession.PCSessionStatus.Session_Canceled);
    }

    @Test
    public void testStatusTransitions() {
        // Given
        PCSession session = new PCSession();

        // When - simulate status transitions
        session.setStatus(PCSession.PCSessionStatus.Session_Created);
        assertEquals(PCSession.PCSessionStatus.Session_Created, session.getStatus());

        session.setStatus(PCSession.PCSessionStatus.Session_Scanned);
        assertEquals(PCSession.PCSessionStatus.Session_Scanned, session.getStatus());

        session.setStatus(PCSession.PCSessionStatus.Session_Pre_Verify);
        assertEquals(PCSession.PCSessionStatus.Session_Pre_Verify, session.getStatus());

        session.setStatus(PCSession.PCSessionStatus.Session_Verified);
        assertEquals(PCSession.PCSessionStatus.Session_Verified, session.getStatus());

        session.setStatus(PCSession.PCSessionStatus.Session_Canceled);
        assertEquals(PCSession.PCSessionStatus.Session_Canceled, session.getStatus());
    }

    @Test
    public void testPlatformValues() {
        // Given
        PCSession session = new PCSession();

        // When - test different platform values
        int[] platforms = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int platform : platforms) {
            session.setPlatform(platform);
            assertEquals(platform, session.getPlatform());
        }
    }
}
