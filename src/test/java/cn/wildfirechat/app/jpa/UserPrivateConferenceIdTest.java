package cn.wildfirechat.app.jpa;

import org.junit.Test;

import static org.junit.Assert.*;

public class UserPrivateConferenceIdTest {

    @Test
    public void testDefaultConstructor() {
        // When
        UserPrivateConferenceId id = new UserPrivateConferenceId();

        // Then
        assertNull(id.getUserId());
        assertNull(id.getConferenceId());
    }

    @Test
    public void testConstructorWithParams() {
        // Given
        String userId = "user123";
        String conferenceId = "conf456";

        // When
        UserPrivateConferenceId id = new UserPrivateConferenceId(userId, conferenceId);

        // Then
        assertEquals(userId, id.getUserId());
        assertEquals(conferenceId, id.getConferenceId());
    }

    @Test
    public void testSettersAndGetters() {
        // Given
        UserPrivateConferenceId id = new UserPrivateConferenceId();

        // When
        id.setUserId("user789");
        id.setConferenceId("conf012");

        // Then
        assertEquals("user789", id.getUserId());
        assertEquals("conf012", id.getConferenceId());
    }

    @Test
    public void testUpdateConferenceId() {
        // Given
        UserPrivateConferenceId id = new UserPrivateConferenceId("user1", "conf1");

        // When
        id.setConferenceId("conf2");

        // Then
        assertEquals("user1", id.getUserId());
        assertEquals("conf2", id.getConferenceId());
    }
}
