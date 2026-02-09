package cn.wildfirechat.app.jpa;

import org.junit.Test;

import static org.junit.Assert.*;

public class UserConferenceTest {

    @Test
    public void testDefaultConstructor() {
        // When
        UserConference userConference = new UserConference();

        // Then - fields should have default values
        assertNull(userConference.getUserId());
        assertNull(userConference.getConferenceId());
    }

    @Test
    public void testConstructorWithParams() {
        // Given
        String userId = "user123";
        String conferenceId = "conf456";

        // When
        UserConference userConference = new UserConference(userId, conferenceId);

        // Then
        assertEquals(userId, userConference.getUserId());
        assertEquals(conferenceId, userConference.getConferenceId());
    }

    @Test
    public void testSettersAndGetters() {
        // Given
        UserConference userConference = new UserConference();

        // When
        userConference.setUserId("user789");
        userConference.setConferenceId("conf012");

        // Then
        assertEquals("user789", userConference.getUserId());
        assertEquals("conf012", userConference.getConferenceId());
    }

    @Test
    public void testMultipleUserConferences() {
        // Given
        String[] userIds = {"user1", "user2", "user3"};
        String conferenceId = "conf123";

        // When/Then
        for (int i = 0; i < userIds.length; i++) {
            UserConference uc = new UserConference(userIds[i], conferenceId);
            assertEquals(userIds[i], uc.getUserId());
            assertEquals(conferenceId, uc.getConferenceId());
        }
    }
}
