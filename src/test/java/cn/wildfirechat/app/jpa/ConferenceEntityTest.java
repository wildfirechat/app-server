package cn.wildfirechat.app.jpa;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConferenceEntityTest {

    @Test
    public void testDefaultValues() {
        // When
        ConferenceEntity conference = new ConferenceEntity();

        // Then
        assertNull(conference.getId());
        assertNull(conference.getConferenceTitle());
        assertNull(conference.getPassword());
        assertNull(conference.getOwner());
        assertEquals(0, conference.getMaxParticipants());
    }

    @Test
    public void testSettersAndGetters() {
        // Given
        ConferenceEntity conference = new ConferenceEntity();
        long now = System.currentTimeMillis();

        // When
        conference.setId("conf123");
        conference.setConferenceTitle("Test Conference");
        conference.setPassword("123456");
        conference.setPin("7890");
        conference.setOwner("user456");
        conference.setManages("admin1,admin2");
        conference.setStartTime(now);
        conference.setEndTime(now + 3600000);
        conference.setAudience(true);
        conference.setAdvance(false);
        conference.setAllowSwitchMode(true);
        conference.setNoJoinBeforeStart(false);
        conference.setRecording(true);
        conference.setFocus("speaker1");
        conference.setMaxParticipants(100);

        // Then
        assertEquals("conf123", conference.getId());
        assertEquals("Test Conference", conference.getConferenceTitle());
        assertEquals("123456", conference.getPassword());
        assertEquals("7890", conference.getPin());
        assertEquals("user456", conference.getOwner());
        assertEquals("admin1,admin2", conference.getManages());
        assertEquals(now, conference.getStartTime());
        assertEquals(now + 3600000, conference.getEndTime());
        assertTrue(conference.isAudience());
        assertFalse(conference.isAdvance());
        assertTrue(conference.isAllowSwitchMode());
        assertFalse(conference.isNoJoinBeforeStart());
        assertTrue(conference.isRecording());
        assertEquals("speaker1", conference.getFocus());
        assertEquals(100, conference.getMaxParticipants());
    }

    @Test
    public void testBooleanFlags() {
        // Given
        ConferenceEntity conference = new ConferenceEntity();

        // Test all combinations
        conference.setAudience(true);
        assertTrue(conference.isAudience());

        conference.setAdvance(true);
        assertTrue(conference.isAdvance());

        conference.setAllowSwitchMode(true);
        assertTrue(conference.isAllowSwitchMode());

        conference.setNoJoinBeforeStart(true);
        assertTrue(conference.isNoJoinBeforeStart());

        conference.setRecording(true);
        assertTrue(conference.isRecording());

        // Set to false
        conference.setAudience(false);
        assertFalse(conference.isAudience());
    }

    @Test
    public void testMaxParticipantsBoundary() {
        // Given
        ConferenceEntity conference = new ConferenceEntity();

        // When - test boundary values
        conference.setMaxParticipants(0);
        assertEquals(0, conference.getMaxParticipants());

        conference.setMaxParticipants(1);
        assertEquals(1, conference.getMaxParticipants());

        conference.setMaxParticipants(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, conference.getMaxParticipants());
    }
}
